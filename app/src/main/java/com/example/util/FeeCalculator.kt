package com.example.util

import com.example.data.model.Batch
import com.example.data.model.Course
import com.example.data.model.Discount
import com.example.data.model.FeePayment
import com.example.data.model.Student
import java.util.Calendar
import java.util.Locale

data class MonthFeeStatus(
  val monthKey: String, // e.g. "2026-08"
  val monthLabel: String, // e.g. "August 2026"
  val standardFee: Double, // e.g. 400.0
  val discount: Double, // e.g. 250.0
  val netPayable: Double, // e.g. 150.0
  val isPaid: Boolean,
  val isPending: Boolean,
  val paidAmount: Double,
  val dueAmount: Double,
  val matchingPayment: FeePayment? = null,
  val discountTitle: String? = null
) {
  // Compatibility getter for existing UI components
  val feeAmount: Double get() = if (discount > 0) netPayable else standardFee
}

data class StudentFeeSummary(
  val student: Student,
  val course: Course?,
  val batch: Batch?,
  val standardMonthlyFee: Double, // e.g. 400.0
  val monthlyDiscount: Double, // e.g. 250.0
  val effectiveMonthlyFee: Double, // e.g. 150.0
  val admissionDate: String, // e.g. "2026-08-16"
  val admissionMonth: String, // e.g. "August 2026"
  val currentMonth: String = "", // e.g. "October 2026"
  val monthsElapsed: List<MonthFeeStatus>, // All months from admission month to current month
  val totalGrossBilled: Double, // standardMonthlyFee * monthsElapsed.size
  val totalDiscountAllowed: Double, // total discount applied across billing
  val totalNetBilled: Double, // totalGrossBilled - totalDiscountAllowed (Net Payable from Admission Month)
  val totalPaid: Double, // already paid amount adjusted as website
  val dueAmount: Double, // maxOf(0.0, totalNetBilled - totalPaid) -> Pending Amount
  val advanceAmount: Double, // maxOf(0.0, totalPaid - totalNetBilled)
  val paidMonthsCount: Int,
  val dueMonthsCount: Int,
  val unpaidMonths: List<MonthFeeStatus>,
  val payments: List<FeePayment>,
  val appliedDiscountTitle: String? = null
) {
  // Compatibility getters for existing UI
  val monthlyFee: Double get() = effectiveMonthlyFee
  val totalBilled: Double get() = totalNetBilled
  val payableFromAdmissionMonth: Double get() = totalNetBilled
  val grossBilledFromAdmissionMonth: Double get() = totalGrossBilled
  val totalDiscount: Double get() = totalDiscountAllowed
  val billedMonthsCount: Int get() = monthsElapsed.size
  val pendingAmount: Double get() = dueAmount
  val pendingMonthsText: String get() = if (dueMonthsCount > 0) "$dueMonthsCount Months Pending (From $admissionMonth to $currentMonth)" else "All Cleared Up to $currentMonth"
}

object FeeCalculator {

  private val MONTH_NAMES = arrayOf(
    "", "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
  )

  data class DiscountResolution(
    val amount: Double,
    val discount: Discount? = null,
    val title: String? = null
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
   * Resolves the best applicable discount for a student based on:
   * 1. Student explicit discount / custom fee override
   * 2. Database discount table matching rules:
   *    - Active status
   *    - Target scope (student ID, batch ID, course ID, academic class, or 'all')
   *    - Applicable months (all_months, or selected_months matching monthKey)
   *    - Type: percentage vs flat amount
   */
  fun resolveDiscount(
    student: Student,
    courses: List<Course>,
    batches: List<Batch>,
    discounts: List<Discount> = emptyList(),
    monthKey: String? = null,
    monthLabel: String? = null
  ): DiscountResolution {
    val batch = batches.find { it.id == student.batchId }
    val course = courses.find { it.id == student.courseId }
      ?: courses.find { it.id == batch?.courseId }
      ?: courses.firstOrNull()
    val standardFee = course?.monthlyFee?.takeIf { it > 0 } ?: 400.0

    // 1. Explicit student-level discount from student record
    var explicitDiscount = 0.0
    var explicitTitle: String? = null

    if (student.monthlyDiscount != null && student.monthlyDiscount > 0) {
      explicitDiscount = student.monthlyDiscount
      explicitTitle = student.discountReason ?: "Student Concession"
    } else if (student.customMonthlyFeeOverride != null && student.customMonthlyFeeOverride > 0 && student.customMonthlyFeeOverride < standardFee) {
      val override = student.customMonthlyFeeOverride
      explicitDiscount = if (override <= standardFee / 2) override else maxOf(0.0, standardFee - override)
      explicitTitle = student.discountReason ?: "Custom Fee Concession"
    }

    // 2. Matching discounts from database discount table
    var bestDbDiscount: Discount? = null
    var bestDbAmount = 0.0

    for (d in discounts) {
      if (!d.active) continue

      // Scope match
      val scopeMatches = when (d.scope.lowercase().trim()) {
        "student" -> !d.studentId.isNullOrBlank() && d.studentId == student.id
        "batch" -> !d.batchId.isNullOrBlank() && d.batchId == student.batchId
        "course" -> !d.courseId.isNullOrBlank() && (d.courseId == student.courseId || d.courseId == course?.id)
        "class" -> !d.academicClass.isNullOrBlank() && (
          d.academicClass.equals(student.studentClass, ignoreCase = true) ||
          d.academicClass.equals(course?.academicClass, ignoreCase = true)
        )
        "all" -> true
        else -> d.scope.isBlank() || d.scope.equals("universal", ignoreCase = true)
      }
      if (!scopeMatches) continue

      // Month match
      val monthMatches = when {
        d.applicableMonthsType.equals("selected_months", ignoreCase = true) && !d.selectedMonths.isNullOrEmpty() -> {
          if (monthKey == null && monthLabel == null) true
          else d.selectedMonths.any { sm ->
            (monthKey != null && sm.contains(monthKey, ignoreCase = true)) ||
            (monthLabel != null && sm.contains(monthLabel, ignoreCase = true)) ||
            (monthKey != null && monthKey.contains(sm, ignoreCase = true))
          }
        }
        else -> true // "all_months"
      }
      if (!monthMatches) continue

      // Calculate discount amount
      val discValue = when (d.type.lowercase().trim()) {
        "percentage" -> (standardFee * d.value) / 100.0
        else -> d.value // "flat"
      }

      if (discValue > bestDbAmount) {
        bestDbAmount = discValue
        bestDbDiscount = d
      }
    }

    return if (bestDbAmount >= explicitDiscount && bestDbDiscount != null) {
      DiscountResolution(
        amount = minOf(standardFee, bestDbAmount),
        discount = bestDbDiscount,
        title = bestDbDiscount.title
      )
    } else {
      DiscountResolution(
        amount = minOf(standardFee, explicitDiscount),
        discount = null,
        title = explicitTitle
      )
    }
  }

  /**
   * Determine monthly discount for student (override vs standard, explicit monthly discount, or database discount table).
   */
  fun getMonthlyDiscount(
    student: Student,
    courses: List<Course>,
    batches: List<Batch>,
    discounts: List<Discount> = emptyList()
  ): Double {
    return resolveDiscount(student, courses, batches, discounts).amount
  }

  /**
   * Effective monthly fee payable for a student after discount.
   */
  fun getMonthlyFee(
    student: Student,
    courses: List<Course>,
    batches: List<Batch>,
    discounts: List<Discount> = emptyList()
  ): Double {
    val standard = getStandardMonthlyFee(student, courses, batches)
    val discount = getMonthlyDiscount(student, courses, batches, discounts)
    return maxOf(0.0, standard - discount)
  }

  /**
   * Parses year and month from any admission date / month string:
   * e.g. "2026-10-27T18:30:00.000Z", "2026-08-16", "2026-08", "August 2026", "28/10/2026"
   */
  fun parseAdmissionYearMonth(dateStr: String?): Pair<Int, Int> {
    if (dateStr.isNullOrBlank()) {
      val now = Calendar.getInstance()
      return Pair(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
    }
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
      val parts = clean.split("-", "/", "T", " ")
      val y = parts[0].toIntOrNull() ?: 2026
      val m = parts[1].toIntOrNull() ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)
      return Pair(y, m.coerceIn(1, 12))
    }

    // Check dd-MM-yyyy or dd/MM/yyyy format (e.g. 16-08-2026 or 01/03/2026)
    if (clean.matches(Regex("""^\d{1,2}[-/]\d{1,2}[-/]\d{4}.*"""))) {
      val parts = clean.split("-", "/", " ")
      val p0 = parts[0].toIntOrNull() ?: 1
      val p1 = parts[1].toIntOrNull() ?: 1
      val y = parts[2].substring(0, 4).toIntOrNull() ?: 2026
      val m = if (p0 > 12) p1 else if (p1 > 12) p0 else p1 // standard dd-MM-yyyy
      return Pair(y, m.coerceIn(1, 12))
    }

    val now = Calendar.getInstance()
    return Pair(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
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
   * - Discounts calculated using database discount table & student concession rules
   * - Net payable amount calculated from date of admission to current date month
   * - Pending amount and unpaid months calculated chronologically
   */
  fun calculateStudentFeeSummary(
    student: Student,
    courses: List<Course>,
    batches: List<Batch>,
    allPayments: List<FeePayment>,
    admissions: List<com.example.data.model.AdmissionApplication> = emptyList(),
    discounts: List<Discount> = emptyList()
  ): StudentFeeSummary {
    val batch = batches.find { it.id == student.batchId }
    val course = courses.find { it.id == student.courseId }
      ?: courses.find { it.id == batch?.courseId }
      ?: courses.firstOrNull()

    val standardMonthlyFee = getStandardMonthlyFee(student, courses, batches)
    val defaultDiscountRes = resolveDiscount(student, courses, batches, discounts)
    val monthlyDiscount = defaultDiscountRes.amount
    val effectiveMonthlyFee = maxOf(0.0, standardMonthlyFee - monthlyDiscount)

    // Check if admissions table has matching application for this student
    val matchingApp = admissions.find { app ->
      (app.studentName.isNotBlank() && app.studentName.equals(student.name, ignoreCase = true)) ||
      (app.mobile.isNotBlank() && app.mobile == student.mobile) ||
      (app.aadhaarNo.isNotBlank() && app.aadhaarNo == student.aadhaarNo)
    }

    // Strictly calculate pending amount depending on student's actual admission date in the database
    val studentDate = student.admissionDate.ifBlank { student.admissionMonth.ifBlank { "" } }
    val effectiveAdmissionStr = studentDate.ifBlank { matchingApp?.appliedDate ?: "2026-08-01" }

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

    val rawMonths = getMonthsSinceAdmission(effectiveAdmissionStr)
    val currentMonthLabel = rawMonths.lastOrNull()?.second ?: run {
      val cal = Calendar.getInstance()
      "${MONTH_NAMES[cal.get(Calendar.MONTH) + 1]} ${cal.get(Calendar.YEAR)}"
    }

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

      val monthDiscRes = resolveDiscount(student, courses, batches, discounts, monthKey, monthLabel)
      val monthStandardFee = standardMonthlyFee
      val monthDiscount = monthDiscRes.amount
      val monthNetPayable = maxOf(0.0, monthStandardFee - monthDiscount)

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
          matchingPayment = if (isCoveredByCredit || coveredAmount > 0) explicitPayment else null,
          discountTitle = monthDiscRes.title
        )
      )
    }

    val totalGrossBilled = monthStatuses.sumOf { it.standardFee }
    val baseDiscountAllowed = monthStatuses.sumOf { it.discount }
    val receiptExtraDiscounts = approvedPayments.sumOf { p ->
      val coveredCount = maxOf(1, p.monthsCovered.size)
      maxOf(0.0, p.totalDiscount - (coveredCount * monthlyDiscount))
    }
    val totalDiscountAllowed = baseDiscountAllowed + receiptExtraDiscounts
    val totalNetBilled = maxOf(0.0, totalGrossBilled - totalDiscountAllowed)

    val dueAmount = maxOf(0.0, totalNetBilled - totalPaid)
    val advanceAmount = maxOf(0.0, totalPaid - totalNetBilled)

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
      currentMonth = currentMonthLabel,
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
      payments = studentPayments,
      appliedDiscountTitle = defaultDiscountRes.title
    )
  }
}
