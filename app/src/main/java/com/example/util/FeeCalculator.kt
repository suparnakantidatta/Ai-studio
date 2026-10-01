package com.example.util

import com.example.data.model.Batch
import com.example.data.model.Course
import com.example.data.model.FeePayment
import com.example.data.model.Student
import java.util.Calendar
import java.util.Locale

data class MonthFeeStatus(
  val monthKey: String, // e.g. "2026-08"
  val monthLabel: String, // e.g. "August 2026"
  val feeAmount: Double, // e.g. 400.0
  val isPaid: Boolean,
  val isPending: Boolean,
  val paidAmount: Double,
  val dueAmount: Double,
  val matchingPayment: FeePayment? = null
)

data class StudentFeeSummary(
  val student: Student,
  val course: Course?,
  val batch: Batch?,
  val monthlyFee: Double,
  val admissionDate: String,
  val monthsElapsed: List<MonthFeeStatus>,
  val totalBilled: Double,
  val totalPaid: Double,
  val dueAmount: Double,
  val advanceAmount: Double,
  val paidMonthsCount: Int,
  val dueMonthsCount: Int,
  val unpaidMonths: List<MonthFeeStatus>,
  val payments: List<FeePayment>
)

object FeeCalculator {

  /**
   * Determine monthly fee payable for a student based on:
   * 1. Student customMonthlyFeeOverride (if set)
   * 2. Course linked to student's batch or student's courseId
   * 3. Fallback: 400.0
   */
  fun getMonthlyFee(
    student: Student,
    courses: List<Course>,
    batches: List<Batch>
  ): Double {
    if (student.customMonthlyFeeOverride != null && student.customMonthlyFeeOverride > 0) {
      return student.customMonthlyFeeOverride
    }
    val batch = batches.find { it.id == student.batchId }
    val course = courses.find { it.id == student.courseId }
      ?: courses.find { it.id == batch?.courseId }
      ?: courses.firstOrNull()
    return course?.monthlyFee?.takeIf { it > 0 } ?: 400.0
  }

  /**
   * Parses year and month from diverse admissionDate formats:
   * "2026-08-20", "2026-08", "20/08/2026", "2026/08/20", etc.
   */
  fun parseAdmissionYearMonth(dateStr: String?): Pair<Int, Int> {
    if (dateStr.isNullOrBlank()) return Pair(2026, 8)
    val clean = dateStr.trim()
    // yyyy-MM-dd or yyyy-MM or yyyy/MM/dd
    if (clean.matches(Regex("""^\d{4}[-/]\d{1,2}([-/]\d{1,2})?.*"""))) {
      val parts = clean.split("-", "/")
      val y = parts[0].toIntOrNull() ?: 2026
      val m = parts[1].toIntOrNull() ?: 8
      return Pair(y, m.coerceIn(1, 12))
    }
    // dd-MM-yyyy or dd/MM/yyyy
    if (clean.matches(Regex("""^\d{1,2}[-/]\d{1,2}[-/]\d{4}.*"""))) {
      val parts = clean.split("-", "/")
      val m = parts[1].toIntOrNull() ?: 8
      val y = parts[2].substring(0, 4).toIntOrNull() ?: 2026
      return Pair(y, m.coerceIn(1, 12))
    }
    return Pair(2026, 8)
  }

  /**
   * Returns ordered list of months from admission date up to current month (inclusive).
   */
  fun getMonthsSinceAdmission(admissionDate: String?): List<Pair<String, String>> {
    val (admYear, admMonth) = parseAdmissionYearMonth(admissionDate)

    val cal = Calendar.getInstance()
    val currentYear = cal.get(Calendar.YEAR)
    val currentMonth = cal.get(Calendar.MONTH) + 1 // 1..12

    val monthNames = arrayOf(
      "", "January", "February", "March", "April", "May", "June",
      "July", "August", "September", "October", "November", "December"
    )

    val list = mutableListOf<Pair<String, String>>()
    var y = admYear
    var m = admMonth

    while (y < currentYear || (y == currentYear && m <= currentMonth)) {
      val key = String.format(Locale.ROOT, "%04d-%02d", y, m)
      val label = "${monthNames[m]} $y"
      list.add(Pair(key, label))
      m++
      if (m > 12) {
        m = 1
        y++
      }
    }

    if (list.isEmpty()) {
      val key = String.format(Locale.ROOT, "%04d-%02d", currentYear, currentMonth)
      val label = "${monthNames[currentMonth]} $currentYear"
      list.add(Pair(key, label))
    }

    return list
  }

  /**
   * Calculates comprehensive fee summary for a student:
   * - Payable amount as per batch and course
   * - Due amount calculated from date of admission
   * - Already paid amount adjusted as website
   */
  fun calculateStudentFeeSummary(
    student: Student,
    courses: List<Course>,
    batches: List<Batch>,
    allPayments: List<FeePayment>
  ): StudentFeeSummary {
    val batch = batches.find { it.id == student.batchId }
    val course = courses.find { it.id == student.courseId }
      ?: courses.find { it.id == batch?.courseId }
      ?: courses.firstOrNull()

    val monthlyFee = getMonthlyFee(student, courses, batches)
    val admissionDate = student.admissionDate.ifBlank { "2026-08-01" }

    // Student approved and pending payments
    val studentPayments = allPayments.filter { p ->
      p.studentId == student.id || (p.studentName.isNotBlank() && p.studentName.equals(student.name, ignoreCase = true))
    }
    val approvedPayments = studentPayments.filter { it.status.equals("approved", ignoreCase = true) }
    val pendingPayments = studentPayments.filter { it.status.equals("pending", ignoreCase = true) }

    val totalPaid = approvedPayments.sumOf { it.finalAmountPaid }

    val rawMonths = getMonthsSinceAdmission(admissionDate)
    val totalBilled = rawMonths.size * monthlyFee
    val dueAmount = maxOf(0.0, totalBilled - totalPaid)
    val advanceAmount = maxOf(0.0, totalPaid - totalBilled)

    // Adjust paid amount across months chronologically (as website does)
    var remainingCredit = totalPaid
    val monthStatuses = mutableListOf<MonthFeeStatus>()

    for ((monthKey, monthLabel) in rawMonths) {
      val explicitPayment = approvedPayments.find { p ->
        p.monthsCovered.any { it.equals(monthKey, ignoreCase = true) || it.contains(monthLabel, ignoreCase = true) } ||
        p.month.equals(monthKey, ignoreCase = true) || p.month.contains(monthLabel, ignoreCase = true)
      } ?: approvedPayments.firstOrNull()

      val pendingPayment = pendingPayments.find { p ->
        p.monthsCovered.any { it.equals(monthKey, ignoreCase = true) || it.contains(monthLabel, ignoreCase = true) } ||
        p.month.equals(monthKey, ignoreCase = true) || p.month.contains(monthLabel, ignoreCase = true)
      }

      val isCoveredByCredit = remainingCredit >= monthlyFee
      val isPartiallyCovered = remainingCredit > 0 && remainingCredit < monthlyFee
      val coveredAmount = when {
        isCoveredByCredit -> monthlyFee
        isPartiallyCovered -> remainingCredit
        else -> 0.0
      }
      val monthDue = monthlyFee - coveredAmount
      remainingCredit = maxOf(0.0, remainingCredit - monthlyFee)

      monthStatuses.add(
        MonthFeeStatus(
          monthKey = monthKey,
          monthLabel = monthLabel,
          feeAmount = monthlyFee,
          isPaid = isCoveredByCredit || coveredAmount >= monthlyFee,
          isPending = pendingPayment != null,
          paidAmount = coveredAmount,
          dueAmount = monthDue,
          matchingPayment = if (isCoveredByCredit || coveredAmount > 0) explicitPayment else null
        )
      )
    }

    val paidCount = monthStatuses.count { it.isPaid }
    val unpaidList = monthStatuses.filter { !it.isPaid }

    return StudentFeeSummary(
      student = student,
      course = course,
      batch = batch,
      monthlyFee = monthlyFee,
      admissionDate = admissionDate,
      monthsElapsed = monthStatuses,
      totalBilled = totalBilled,
      totalPaid = totalPaid,
      dueAmount = dueAmount,
      advanceAmount = advanceAmount,
      paidMonthsCount = paidCount,
      dueMonthsCount = unpaidList.size,
      unpaidMonths = unpaidList,
      payments = studentPayments
    )
  }
}
