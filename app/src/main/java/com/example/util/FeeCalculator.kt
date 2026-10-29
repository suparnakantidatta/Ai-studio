package com.example.util

import com.example.data.model.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

object FeeCalculator {

  private val IST_TIMEZONE = TimeZone.getTimeZone("Asia/Kolkata")

  fun getCurrentDate(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
      timeZone = IST_TIMEZONE
    }
    return sdf.format(Date())
  }

  fun parseIsoOrDateToCleanDate(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val trimmed = raw.trim()

    // 1. If already in YYYY-MM-DD format
    if (trimmed.matches(Regex("""^\d{4}-\d{2}-\d{2}$"""))) {
      return trimmed
    }

    // 2. If in ISO 8601 UTC timestamp format e.g. "2026-10-27T18:30:00.000Z"
    if (trimmed.contains("T")) {
      if (trimmed.startsWith("1899-") || trimmed.startsWith("1900-")) {
        return try {
          val timePart = trimmed.substringAfter("T").substringBefore(".").substringBefore("Z")
          val parts = timePart.split(":")
          val h = parts[0].toIntOrNull() ?: 10
          val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
          val ampm = if (h >= 12) "PM" else "AM"
          val h12 = if (h % 12 == 0) 12 else if (h > 12) h - 12 else h
          String.format(Locale.US, "%02d:%02d %s", h12, m, ampm)
        } catch (_: Exception) { trimmed }
      }
      try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
          timeZone = TimeZone.getTimeZone("UTC")
        }
        val clean = trimmed.split(".")[0].replace("Z", "")
        val date = sdf.parse(clean)
        if (date != null) {
          val outSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = IST_TIMEZONE
          }
          return outSdf.format(date)
        }
      } catch (_: Exception) {
        return trimmed.substringBefore("T")
      }
    }

    // 3. Try DD/MM/YYYY or DD-MM-YYYY
    try {
      val slashParts = trimmed.split("/", "-")
      if (slashParts.size == 3) {
        val y = if (slashParts[0].length == 4) slashParts[0] else slashParts[2]
        val m = slashParts[1].padStart(2, '0')
        val d = if (slashParts[0].length == 4) slashParts[2].padStart(2, '0') else slashParts[0].padStart(2, '0')
        return "$y-$m-$d"
      }
    } catch (_: Exception) {}

    return trimmed
  }

  fun parseIsoOrMonthToYearMonth(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val trimmed = raw.trim()

    // 1. If already in YYYY-MM format
    if (trimmed.matches(Regex("""^\d{4}-\d{2}$"""))) {
      return trimmed
    }

    // 2. If in YYYY-MM-DD format
    if (trimmed.matches(Regex("""^\d{4}-\d{2}-\d{2}$"""))) {
      return trimmed.substring(0, 7)
    }

    // 3. If in ISO 8601 UTC timestamp format e.g. "2026-06-30T18:30:00.000Z"
    if (trimmed.contains("T")) {
      try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
          timeZone = TimeZone.getTimeZone("UTC")
        }
        val clean = trimmed.split(".")[0].replace("Z", "")
        val date = sdf.parse(clean)
        if (date != null) {
          val outSdf = SimpleDateFormat("yyyy-MM", Locale.US).apply {
            timeZone = IST_TIMEZONE
          }
          return outSdf.format(date)
        }
      } catch (_: Exception) {}
    }

    // 4. Try DD/MM/YYYY or DD-MM-YYYY
    try {
      val slashParts = trimmed.split("/", "-")
      if (slashParts.size == 3) {
        val y = if (slashParts[0].length == 4) slashParts[0] else slashParts[2]
        val m = if (slashParts[0].length == 4) slashParts[1] else slashParts[1]
        val paddedM = m.padStart(2, '0')
        return "$y-$paddedM"
      }
    } catch (_: Exception) {}

    return null
  }

  fun formatYearMonthToDisplay(yearMonth: String): String {
    return try {
      val sdfIn = SimpleDateFormat("yyyy-MM", Locale.US)
      val date = sdfIn.parse(yearMonth) ?: return yearMonth
      val sdfOut = SimpleDateFormat("MMMM yyyy", Locale.US)
      sdfOut.format(date)
    } catch (_: Exception) {
      yearMonth
    }
  }

  fun getMonthListBetween(startYearMonth: String, endYearMonth: String): List<String> {
    val result = mutableListOf<String>()
    try {
      val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
      val cal = Calendar.getInstance().apply {
        time = sdf.parse(startYearMonth) ?: Date()
      }
      val endCal = Calendar.getInstance().apply {
        time = sdf.parse(endYearMonth) ?: Date()
      }

      while (!cal.after(endCal)) {
        result.add(sdf.format(cal.time))
        cal.add(Calendar.MONTH, 1)
      }
    } catch (_: Exception) {
      if (startYearMonth.isNotBlank()) result.add(startYearMonth)
    }
    return result
  }

  fun getCurrentYearMonth(): String {
    val sdf = SimpleDateFormat("yyyy-MM", Locale.US).apply {
      timeZone = IST_TIMEZONE
    }
    return sdf.format(Date())
  }

  fun resolveApplicableDiscount(
    student: Student,
    course: Course?,
    batch: Batch?,
    targetYearMonth: String,
    allDiscounts: List<Discount>,
    baseMonthlyFee: Double
  ): DiscountResolution? {
    // If student has explicit monthlyDiscount set on student record
    if (student.monthlyDiscount != null && student.monthlyDiscount > 0) {
      return DiscountResolution(
        discountId = "student-direct",
        discountTitle = student.discountReason ?: "Student Concession",
        discountType = "flat",
        value = student.monthlyDiscount,
        calculatedDeduction = minOf(student.monthlyDiscount, baseMonthlyFee)
      )
    }

    val activeDiscounts = allDiscounts.filter { it.active }

    // Find all matching discount rules
    val matching = activeDiscounts.filter { disc ->
      // Scope match
      val scopeMatches = when (disc.scope.lowercase()) {
        "student" -> disc.studentId.isNullOrBlank() || disc.studentId == student.id
        "batch" -> disc.batchId.isNullOrBlank() || disc.batchId == student.batchId
        "course" -> disc.courseId.isNullOrBlank() || disc.courseId == student.courseId
        "class" -> {
          val stClass = student.studentClass ?: course?.academicClass ?: ""
          disc.academicClass.isNullOrBlank() || disc.academicClass.equals(stClass, ignoreCase = true)
        }
        "all" -> true
        else -> false
      }
      if (!scopeMatches) return@filter false

      // Months match
      when (disc.applicableMonthsType.lowercase()) {
        "all_months" -> true
        "selected_months", "single_month" -> {
          val sel = disc.selectedMonths ?: emptyList()
          sel.any { rawM ->
            val norm = parseIsoOrMonthToYearMonth(rawM)
            norm == targetYearMonth
          }
        }
        else -> true
      }
    }

    if (matching.isEmpty()) return null

    // Pick highest discount deduction
    val candidate = matching.maxByOrNull { disc ->
      if (disc.type.equals("percentage", ignoreCase = true)) {
        (baseMonthlyFee * disc.value) / 100.0
      } else {
        disc.value
      }
    } ?: return null

    val deduction = if (candidate.type.equals("percentage", ignoreCase = true)) {
      (baseMonthlyFee * candidate.value) / 100.0
    } else {
      candidate.value
    }

    return DiscountResolution(
      discountId = candidate.id,
      discountTitle = candidate.title,
      discountType = candidate.type,
      value = candidate.value,
      calculatedDeduction = minOf(deduction, baseMonthlyFee)
    )
  }

  fun calculateStudentDuesSummary(
    student: Student,
    course: Course?,
    batch: Batch?,
    discounts: List<Discount>,
    payments: List<FeePayment>,
    targetUpToMonth: String = getCurrentYearMonth()
  ): FeeDuesSummary {
    val admMonth = parseIsoOrMonthToYearMonth(student.admissionMonth.ifBlank { student.admissionDate })
      ?: "2026-07"

    val baseMonthlyFee = student.customMonthlyFeeOverride
      ?: course?.monthlyFee
      ?: 400.0

    // Ensure months range is from admission month to targetUpToMonth (at least admission month)
    val startMonth = if (admMonth > targetUpToMonth) targetUpToMonth else admMonth
    val months = getMonthListBetween(startMonth, maxOf(admMonth, targetUpToMonth))

    // Approved payments for this student
    val approvedPayments = payments.filter {
      it.studentId == student.id && it.status.equals("approved", ignoreCase = true)
    }

    // Map which months have been paid
    val paidMonthsSet = mutableSetOf<String>()
    approvedPayments.forEach { p ->
      // If monthsCovered is populated
      if (p.monthsCovered.isNotEmpty()) {
        p.monthsCovered.forEach { m ->
          parseIsoOrMonthToYearMonth(m)?.let { paidMonthsSet.add(it) }
        }
      }
      // Or month field has comma-separated months
      if (p.month.isNotBlank()) {
        p.month.split(",").forEach { m ->
          parseIsoOrMonthToYearMonth(m)?.let { paidMonthsSet.add(it) }
        }
      }
    }

    val monthDetails = months.map { ym ->
      val discountRes = resolveApplicableDiscount(
        student = student,
        course = course,
        batch = batch,
        targetYearMonth = ym,
        allDiscounts = discounts,
        baseMonthlyFee = baseMonthlyFee
      )

      val discAmt = discountRes?.calculatedDeduction ?: 0.0
      val net = max(0.0, baseMonthlyFee - discAmt)
      val isPaid = paidMonthsSet.contains(ym)
      val paidAmt = if (isPaid) net else 0.0
      val due = if (isPaid) 0.0 else net

      MonthFeeDetail(
        yearMonth = ym,
        monthLabel = formatYearMonthToDisplay(ym),
        baseFee = baseMonthlyFee,
        discountAmount = discAmt,
        discountName = discountRes?.discountTitle,
        netPayable = net,
        amountPaid = paidAmt,
        remainingDue = due,
        isPaid = isPaid
      )
    }

    val pending = monthDetails.filter { !it.isPaid }
    val paid = monthDetails.filter { it.isPaid }

    val totalBase = monthDetails.sumOf { it.baseFee }
    val totalDisc = monthDetails.sumOf { it.discountAmount }
    val totalNet = monthDetails.sumOf { it.netPayable }
    val totalPaidAmt = monthDetails.sumOf { it.amountPaid }
    val totalDue = pending.sumOf { it.netPayable }

    return FeeDuesSummary(
      studentId = student.id,
      studentName = student.name,
      courseTitle = course?.title ?: "Standard Course",
      batchName = batch?.name ?: "Main Batch",
      admissionMonth = admMonth,
      standardMonthlyFee = baseMonthlyFee,
      totalBaseFee = totalBase,
      totalDiscount = totalDisc,
      totalNetPayable = totalNet,
      totalPaid = totalPaidAmt,
      totalOutstandingDue = totalDue,
      pendingMonths = pending,
      paidMonths = paid,
      allMonths = monthDetails
    )
  }

  fun generateReceiptNo(existingPayments: List<FeePayment>): String {
    val cal = Calendar.getInstance().apply { timeZone = IST_TIMEZONE }
    val year = cal.get(Calendar.YEAR)
    val month = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
    val prefix = "REC/$year/$month-"

    val currentMonthPayments = existingPayments.filter { it.receiptNo.startsWith(prefix) }
    val nextSeq = currentMonthPayments.size + 1
    return "$prefix${nextSeq.toString().padStart(3, '0')}"
  }
}
