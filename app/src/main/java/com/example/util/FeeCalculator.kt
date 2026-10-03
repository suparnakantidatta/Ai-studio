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
  val standardFee: Double, // e.g. 400.0
  val discount: Double, // e.g. 50.0
  val netPayable: Double, // e.g. 350.0
  val isPaid: Boolean,
  val isPending: Boolean,
  val paidAmount: Double,
  val dueAmount: Double,
  val matchingPayment: FeePayment? = null
) {
  // Compatibility getter for existing UI components
  val feeAmount: Double get() = if (discount > 0) netPayable else standardFee
}

data class StudentFeeSummary(
  val student: Student,
  val course: Course?,
  val batch: Batch?,
  val standardMonthlyFee: Double, // e.g. 400.0
  val monthlyDiscount: Double, // e.g. 50.0
  val effectiveMonthlyFee: Double, // e.g. 350.0
  val admissionDate: String, // e.g. "2026-08-16"
  val admissionMonth: String, // e.g. "August 2026"
  val monthsElapsed: List<MonthFeeStatus>, // All months from admission month to current month
  val totalGrossBilled: Double, // standardMonthlyFee * monthsElapsed.size
  val totalDiscountAllowed: Double, // total discount applied across billing
  val totalNetBilled: Double, // totalGrossBilled - totalDiscountAllowed (Net Payable from Admission Month)
  val totalPaid: Double, // already paid amount adjusted as website
  val dueAmount: Double, // maxOf(0.0, totalNetBilled - totalPaid)
  val advanceAmount: Double, // maxOf(0.0, totalPaid - totalNetBilled)
  val paidMonthsCount: Int,
  val dueMonthsCount: Int,
  val unpaidMonths: List<MonthFeeStatus>,
  val payments: List<FeePayment>
) {
  // Compatibility getters for existing UI
  val monthlyFee: Double get() = effectiveMonthlyFee
  val totalBilled: Double get() = totalNetBilled
  val payableFromAdmissionMonth: Double get() = totalNetBilled
  val grossBilledFromAdmissionMonth: Double get() = totalGrossBilled
  val totalDiscount: Double get() = totalDiscountAllowed
  val billedMonthsCount: Int get() = monthsElapsed.size
}

object FeeCalculator {

  private val MONTH_NAMES = arrayOf(
    "", "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
  )

  /**
   * Determine standard monthly fee as per batch and course.
   */
  fun getStandardMonthlyFee(
    student: Student,
    courses: List<Course>,
    batches: List<Batch>
  ): Double {
    val batch = batches.find { it.id == student.batchId }
    val course = courses.find { it.id == student.courseId }
      ?: courses.find { it.id == batch?.courseId }
      ?: courses.firstOrNull()
    return course?.monthlyFee?.takeIf { it > 0 } ?: 400.0
  }

  /**
   * Determine monthly discount for student (override vs standard, or explicit monthly discount).
   */
  fun getMonthlyDiscount(
    student: Student,
    courses: List<Course>,
    batches: List<Batch>
  ): Double {
    val standard = getStandardMonthlyFee(student, courses, batches)
    if (student.monthlyDiscount != null && student.monthlyDiscount > 0) {
      return student.monthlyDiscount
    }
    val override = student.customMonthlyFeeOverride
    if (override != null && override > 0 && override < standard) {
      // If override is a concession value (e.g. 50 or 100), or net monthly fee (e.g. 350)
      return if (override <= standard / 2) override else maxOf(0.0, standard - override)
    }
    return 0.0
  }

  /**
   * Effective monthly fee payable for a student after discount.
   */
  fun getMonthlyFee(
    student: Student,
    courses: List<Course>,
    batches: List<Batch>
  ): Double {
    val standard = getStandardMonthlyFee(student, courses, batches)
    val discount = getMonthlyDiscount(student, courses, batches)
    return maxOf(0.0, standard - discount)
  }

  /**
   * Parses year and month from any admission date / month string:
   * e.g. "2026-10-27T18:30:00.000Z", "2026-08-16", "2026-08", "August 2026", "28/10/2026"
   */
  fun parseAdmissionYearMonth(dateStr: String?): Pair<Int, Int> {
    if (dateStr.isNullOrBlank()) return Pair(2026, 8)
    val clean = dateStr.trim().replace("\"", "").replace("'", "")

    // Check for named month strings like "August 2026" or "October 2026"
    for (i in 1..12) {
      val name = MONTH_NAMES[i]
      if (clean.contains(name, ignoreCase = true) || clean.contains(name.take(3), ignoreCase = true)) {
        val yMatch = Regex("""\b(20\d\d)\b""").find(clean)
        val y = yMatch?.value?.toIntOrNull() ?: 2026
        return Pair(y, i)
      }
    }

    // Check ISO or yyyy-MM-dd / yyyy-MM format (e.g. 2026-10-27T18:30:00.000Z or 2026-08-16)
    if (clean.matches(Regex("""^\d{4}[-/]\d{1,2}.*"""))) {
      val parts = clean.split("-", "/", "T")
      val y = parts[0].toIntOrNull() ?: 2026
      val m = parts[1].toIntOrNull() ?: 8
      return Pair(y, m.coerceIn(1, 12))
    }

    // Check dd-MM-yyyy or dd/MM/yyyy format (e.g. 16-08-2026)
    if (clean.matches(Regex("""^\d{1,2}[-/]\d{1,2}[-/]\d{4}.*"""))) {
      val parts = clean.split("-", "/")
      val m = parts[1].toIntOrNull() ?: 8
      val y = parts[2].substring(0, 4).toIntOrNull() ?: 2026
      return Pair(y, m.coerceIn(1, 12))
    }

    return Pair(2026, 8)
  }

  /**
   * Returns ordered list of months from admission month up to current month (inclusive).
   */
  fun getMonthsSinceAdmission(admissionDate: String?): List<Pair<String, String>> {
    val (admYear, admMonth) = parseAdmissionYearMonth(admissionDate)

    val cal = Calendar.getInstance()
    val currentYear = cal.get(Calendar.YEAR)
    val currentMonth = cal.get(Calendar.MONTH) + 1 // 1..12

    val list = mutableListOf<Pair<String, String>>()
    var y = admYear
    var m = admMonth

    while (y < currentYear || (y == currentYear && m <= currentMonth)) {
      val key = String.format(Locale.ROOT, "%04d-%02d", y, m)
      val label = "${MONTH_NAMES[m]} $y"
      list.add(Pair(key, label))
      m++
      if (m > 12) {
        m = 1
        y++
      }
    }

    if (list.isEmpty()) {
      val safeMonth = admMonth.coerceIn(1, 12)
      val key = String.format(Locale.ROOT, "%04d-%02d", admYear, safeMonth)
      val label = "${MONTH_NAMES[safeMonth]} $admYear"
      list.add(Pair(key, label))
    }

    return list
  }

  /**
   * Calculates comprehensive fee summary for a student:
   * - Standard payable amount as per batch and course
   * - Discounts calculated (student-level monthly discount + receipt-level discounts)
   * - Net payable amount calculated from date of admission
   * - Already paid amount adjusted chronologically as website
   */
  fun calculateStudentFeeSummary(
    student: Student,
    courses: List<Course>,
    batches: List<Batch>,
    allPayments: List<FeePayment>,
    admissions: List<com.example.data.model.AdmissionApplication> = emptyList()
  ): StudentFeeSummary {
    val batch = batches.find { it.id == student.batchId }
    val course = courses.find { it.id == student.courseId }
      ?: courses.find { it.id == batch?.courseId }
      ?: courses.firstOrNull()

    val standardMonthlyFee = getStandardMonthlyFee(student, courses, batches)
    val monthlyDiscount = getMonthlyDiscount(student, courses, batches)
    val effectiveMonthlyFee = maxOf(0.0, standardMonthlyFee - monthlyDiscount)

    // Check if admissions table has an earlier applied date for this student
    val matchingApp = admissions.find { app ->
      (app.studentName.isNotBlank() && app.studentName.equals(student.name, ignoreCase = true)) ||
      (app.mobile.isNotBlank() && app.mobile == student.mobile) ||
      (app.aadhaarNo.isNotBlank() && app.aadhaarNo == student.aadhaarNo)
    }
    val appDate = matchingApp?.appliedDate?.takeIf { it.isNotBlank() }
    val studentDate = student.admissionDate.ifBlank { student.admissionMonth.ifBlank { "" } }

    val effectiveAdmissionStr = when {
      appDate != null && studentDate.isNotBlank() -> {
        val (appY, appM) = parseAdmissionYearMonth(appDate)
        val (stuY, stuM) = parseAdmissionYearMonth(studentDate)
        if (appY < stuY || (appY == stuY && appM < stuM)) {
          if (appDate.contains("T")) appDate.substringBefore("T") else appDate.take(10)
        } else {
          studentDate
        }
      }
      studentDate.isNotBlank() -> studentDate
      appDate != null -> if (appDate.contains("T")) appDate.substringBefore("T") else appDate.take(10)
      else -> "2026-08-01"
    }

    val (admYear, admMonth) = parseAdmissionYearMonth(effectiveAdmissionStr)
    val admissionMonthLabel = "${MONTH_NAMES[admMonth]} $admYear"

    // Filter student payments (both counter receipts and online app payments)
    val studentPayments = allPayments.filter { p ->
      p.studentId == student.id || (p.studentName.isNotBlank() && p.studentName.equals(student.name, ignoreCase = true))
    }
    val baseApprovedPayments = studentPayments.filter { it.status.equals("approved", ignoreCase = true) }
    val pendingPayments = studentPayments.filter { it.status.equals("pending", ignoreCase = true) }

    // Synthesize admission payment if application was marked as paid_advance and no payments exist in payments table
    val approvedPayments = if (baseApprovedPayments.isEmpty() && matchingApp != null && matchingApp.initialPaymentStatus.contains("paid", ignoreCase = true)) {
      val admAmount = matchingApp.initialPaymentAmount ?: effectiveMonthlyFee
      listOf(
        FeePayment(
          id = "pay-adm-${matchingApp.id}",
          receiptNo = "REC/${admYear}/ADM-${student.rollNo.takeLast(4).ifBlank { "001" }}",
          studentId = student.id,
          studentName = student.name,
          studentAadhaar = student.aadhaarNo,
          studentMobile = student.mobile,
          courseTitle = course?.title ?: "Academic Course",
          batchName = batch?.name ?: "Active Batch",
          month = admissionMonthLabel,
          monthsCovered = listOf(String.format(Locale.ROOT, "%04d-%02d", admYear, admMonth)),
          baseMonthlyFee = standardMonthlyFee,
          totalBaseFee = standardMonthlyFee,
          totalDiscount = monthlyDiscount,
          finalAmountPaid = admAmount,
          paymentMode = matchingApp.initialPaymentMode ?: "UPI",
          transactionRef = matchingApp.initialPaymentRef ?: "UPI-ADM-ADVANCE",
          status = "approved",
          paymentDate = effectiveAdmissionStr,
          approvedBy = "Admin (Admission Advance)",
          remarks = "Initial fee paid at admission"
        )
      )
    } else {
      baseApprovedPayments
    }

    val paymentsSum = approvedPayments.sumOf { it.finalAmountPaid }
    val sheetPaid = student.totalPaidInSheet ?: 0.0
    val totalPaid = maxOf(paymentsSum, sheetPaid)

    val receiptLevelDiscounts = approvedPayments.sumOf { it.totalDiscount }

    val rawMonths = getMonthsSinceAdmission(effectiveAdmissionStr)
    val totalGrossBilled = rawMonths.size * standardMonthlyFee
    val studentMonthlyDiscountsTotal = rawMonths.size * monthlyDiscount

    // Total discount allowed: student monthly discounts for each elapsed month plus any additional receipt concessions
    val totalDiscountAllowed = if (monthlyDiscount > 0) {
      val receiptExtraDiscounts = approvedPayments.sumOf { p ->
        val coveredCount = maxOf(1, p.monthsCovered.size)
        maxOf(0.0, p.totalDiscount - (coveredCount * monthlyDiscount))
      }
      studentMonthlyDiscountsTotal + receiptExtraDiscounts
    } else {
      receiptLevelDiscounts
    }
    val totalNetBilled = maxOf(0.0, totalGrossBilled - totalDiscountAllowed)

    val dueAmount = maxOf(0.0, totalNetBilled - totalPaid)
    val advanceAmount = maxOf(0.0, totalPaid - totalNetBilled)

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

      val monthStandardFee = standardMonthlyFee
      val monthDiscount = monthlyDiscount
      val monthNetPayable = effectiveMonthlyFee

      val isCoveredByCredit = remainingCredit >= monthNetPayable
      val isPartiallyCovered = remainingCredit > 0 && remainingCredit < monthNetPayable
      val coveredAmount = when {
        isCoveredByCredit -> monthNetPayable
        isPartiallyCovered -> remainingCredit
        else -> 0.0
      }
      val monthDue = maxOf(0.0, monthNetPayable - coveredAmount)
      remainingCredit = maxOf(0.0, remainingCredit - monthNetPayable)

      monthStatuses.add(
        MonthFeeStatus(
          monthKey = monthKey,
          monthLabel = monthLabel,
          standardFee = monthStandardFee,
          discount = monthDiscount,
          netPayable = monthNetPayable,
          isPaid = isCoveredByCredit || coveredAmount >= monthNetPayable,
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
      standardMonthlyFee = standardMonthlyFee,
      monthlyDiscount = monthlyDiscount,
      effectiveMonthlyFee = effectiveMonthlyFee,
      admissionDate = effectiveAdmissionStr,
      admissionMonth = admissionMonthLabel,
      monthsElapsed = monthStatuses,
      totalGrossBilled = totalGrossBilled,
      totalDiscountAllowed = totalDiscountAllowed,
      totalNetBilled = totalNetBilled,
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
