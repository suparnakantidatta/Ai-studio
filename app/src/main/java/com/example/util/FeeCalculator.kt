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

  private val MONTH_NAMES_MAP = mapOf(
    "jan" to "01", "january" to "01",
    "feb" to "02", "february" to "02",
    "mar" to "03", "march" to "03",
    "apr" to "04", "april" to "04",
    "may" to "05",
    "jun" to "06", "june" to "06",
    "jul" to "07", "july" to "07",
    "aug" to "08", "august" to "08",
    "sep" to "09", "sept" to "09", "september" to "09",
    "oct" to "10", "october" to "10",
    "nov" to "11", "november" to "11",
    "dec" to "12", "december" to "12"
  )

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

    // 4. Try DD/MM/YYYY or DD-MM-YYYY or YYYY/MM/DD
    try {
      val slashParts = trimmed.split("/", "-")
      if (slashParts.size == 3) {
        val y = if (slashParts[0].length == 4) slashParts[0] else slashParts[2]
        val m = if (slashParts[0].length == 4) slashParts[1] else slashParts[1]
        val paddedM = m.padStart(2, '0')
        return "$y-$paddedM"
      } else if (slashParts.size == 2) {
        if (slashParts[0].length == 4 && slashParts[1].toIntOrNull() in 1..12) {
          return "${slashParts[0]}-${slashParts[1].padStart(2, '0')}"
        }
        if (slashParts[1].length == 4 && slashParts[0].toIntOrNull() in 1..12) {
          return "${slashParts[1]}-${slashParts[0].padStart(2, '0')}"
        }
      }
    } catch (_: Exception) {}

    // 5. Named month parsing (e.g. "July 2026", "Jul 2026", "July, 2026", "July")
    try {
      val lower = trimmed.lowercase()
      for ((name, num) in MONTH_NAMES_MAP) {
        if (Regex("""\b$name\b""").containsMatchIn(lower)) {
          val yearMatch = Regex("""\b(20\d\d)\b""").find(trimmed)
          val year = yearMatch?.value ?: "2026"
          return "$year-$num"
        }
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
    var studentDirectResolution: DiscountResolution? = null
    if (student.monthlyDiscount != null && student.monthlyDiscount > 0) {
      studentDirectResolution = DiscountResolution(
        discountId = "student-direct",
        discountTitle = student.discountReason?.ifBlank { "Student Concession" } ?: "Student Concession",
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

    val bestRule = matching.maxByOrNull { disc ->
      if (disc.type.equals("percentage", ignoreCase = true)) {
        (baseMonthlyFee * disc.value) / 100.0
      } else {
        disc.value
      }
    }

    val ruleResolution = bestRule?.let { candidate ->
      val deduction = if (candidate.type.equals("percentage", ignoreCase = true)) {
        (baseMonthlyFee * candidate.value) / 100.0
      } else {
        candidate.value
      }
      DiscountResolution(
        discountId = candidate.id,
        discountTitle = candidate.title,
        discountType = candidate.type,
        value = candidate.value,
        calculatedDeduction = minOf(deduction, baseMonthlyFee)
      )
    }

    return when {
      studentDirectResolution != null && ruleResolution != null -> {
        if (studentDirectResolution.calculatedDeduction >= ruleResolution.calculatedDeduction) {
          studentDirectResolution
        } else {
          ruleResolution
        }
      }
      studentDirectResolution != null -> studentDirectResolution
      ruleResolution != null -> ruleResolution
      else -> null
    }
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

    val billingCutoffMonth = if (targetUpToMonth.isNotBlank()) targetUpToMonth else getCurrentYearMonth()

    // Filter approved payments for this student
    val approvedPayments = payments.filter {
      it.studentId == student.id && it.status.equals("approved", ignoreCase = true)
    }

    // Collect all valid Year-Months referenced in approved payments
    val paymentYearMonths = mutableSetOf<String>()
    approvedPayments.forEach { p ->
      if (p.monthsCovered.isNotEmpty()) {
        p.monthsCovered.forEach { m -> parseIsoOrMonthToYearMonth(m)?.let { paymentYearMonths.add(it) } }
      }
      if (p.month.isNotBlank()) {
        p.month.split(",").forEach { m -> parseIsoOrMonthToYearMonth(m)?.let { paymentYearMonths.add(it) } }
      }
    }

    // Determine horizon: from startMonth to endMonth (covering admission, billing cutoff, all payments, plus next 3 advance months)
    val earliestPaid = paymentYearMonths.minOrNull()
    val startMonth = listOfNotNull(admMonth, earliestPaid, billingCutoffMonth).minOrNull() ?: admMonth

    // Calculate advance horizon (at least 3 upcoming future months beyond billing cutoff)
    val advanceCal = Calendar.getInstance().apply {
      timeZone = IST_TIMEZONE
      val curSdf = SimpleDateFormat("yyyy-MM", Locale.US)
      time = curSdf.parse(billingCutoffMonth) ?: Date()
      add(Calendar.MONTH, 3)
    }
    val advanceHorizonMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(advanceCal.time)

    val latestPaid = paymentYearMonths.maxOrNull()
    val endMonth = listOfNotNull(admMonth, billingCutoffMonth, latestPaid, advanceHorizonMonth).maxOrNull() ?: billingCutoffMonth

    val months = getMonthListBetween(startMonth, endMonth)

    // Pre-calculate net payable and discounts for all months
    val monthNetPayableMap = mutableMapOf<String, Double>()
    val monthDiscountResMap = mutableMapOf<String, DiscountResolution?>()
    months.forEach { ym ->
      val discountRes = resolveApplicableDiscount(
        student = student,
        course = course,
        batch = batch,
        targetYearMonth = ym,
        allDiscounts = discounts,
        baseMonthlyFee = baseMonthlyFee
      )
      monthDiscountResMap[ym] = discountRes
      val discAmt = discountRes?.calculatedDeduction ?: 0.0
      monthNetPayableMap[ym] = max(0.0, baseMonthlyFee - discAmt)
    }

    // Accumulate payments across months chronologically
    val monthPaidAmountMap = mutableMapOf<String, Double>()
    months.forEach { ym -> monthPaidAmountMap[ym] = 0.0 }
    var unallocatedAdvance = 0.0

    // Sort approved payments chronologically
    val sortedApprovedPayments = approvedPayments.sortedBy { it.paymentDate.ifBlank { it.createdAt ?: "" } }

    sortedApprovedPayments.forEach { p ->
      val pMonths = mutableListOf<String>()
      if (p.monthsCovered.isNotEmpty()) {
        p.monthsCovered.forEach { m -> parseIsoOrMonthToYearMonth(m)?.let { pMonths.add(it) } }
      }
      if (pMonths.isEmpty() && p.month.isNotBlank()) {
        p.month.split(",").forEach { m -> parseIsoOrMonthToYearMonth(m)?.let { pMonths.add(it) } }
      }

      if (pMonths.isNotEmpty()) {
        var remainingPayment = p.finalAmountPaid

        // Step 1: Allocate up to net payable for explicitly selected months
        for (ym in pMonths) {
          if (remainingPayment <= 0.0) break
          val net = monthNetPayableMap[ym] ?: baseMonthlyFee
          val alreadyPaid = monthPaidAmountMap[ym] ?: 0.0
          val needed = max(0.0, net - alreadyPaid)
          if (needed > 0.0) {
            val allocate = minOf(remainingPayment, needed)
            monthPaidAmountMap[ym] = alreadyPaid + allocate
            remainingPayment -= allocate
          }
        }

        // Step 2: If excess payment exists, roll it over to subsequent unpaid months in sequence
        if (remainingPayment > 0.0) {
          val lastSpecified = pMonths.last()
          val lastIdx = months.indexOf(lastSpecified)
          val subsequent = if (lastIdx >= 0) months.subList(lastIdx + 1, months.size) else months
          for (nextM in subsequent) {
            if (remainingPayment <= 0.0) break
            val net = monthNetPayableMap[nextM] ?: baseMonthlyFee
            val already = monthPaidAmountMap[nextM] ?: 0.0
            val needed = max(0.0, net - already)
            if (needed > 0.0) {
              val alloc = minOf(remainingPayment, needed)
              monthPaidAmountMap[nextM] = already + alloc
              remainingPayment -= alloc
            }
          }
          if (remainingPayment > 0.0) {
            unallocatedAdvance += remainingPayment
          }
        }
      } else {
        // Payment with no specific month (e.g. Admission Advance, general deposit):
        // Allocate to earliest unpaid months starting from admission month
        var remaining = p.finalAmountPaid
        for (ym in months) {
          if (remaining <= 0.0) break
          val net = monthNetPayableMap[ym] ?: baseMonthlyFee
          val already = monthPaidAmountMap[ym] ?: 0.0
          val needed = max(0.0, net - already)
          if (needed > 0.0) {
            val alloc = minOf(remaining, needed)
            monthPaidAmountMap[ym] = already + alloc
            remaining -= alloc
          }
        }
        if (remaining > 0.0) {
          unallocatedAdvance += remaining
        }
      }
    }

    // Fallback to totalPaidInSheet ONLY if there are no itemized payment records for this student
    if (approvedPayments.isEmpty() && student.totalPaidInSheet != null && student.totalPaidInSheet > 0.0) {
      var extraFromSheet = student.totalPaidInSheet
      for (ym in months) {
        if (extraFromSheet <= 0.0) break
        val net = monthNetPayableMap[ym] ?: baseMonthlyFee
        val already = monthPaidAmountMap[ym] ?: 0.0
        val needed = max(0.0, net - already)
        if (needed > 0.0) {
          val alloc = minOf(extraFromSheet, needed)
          monthPaidAmountMap[ym] = already + alloc
          extraFromSheet -= alloc
        }
      }
      if (extraFromSheet > 0.0) {
        unallocatedAdvance += extraFromSheet
      }
    }

    val monthDetails = months.map { ym ->
      val discountRes = monthDiscountResMap[ym]
      val discAmt = discountRes?.calculatedDeduction ?: 0.0
      val net = monthNetPayableMap[ym] ?: max(0.0, baseMonthlyFee - discAmt)
      val paidSoFar = monthPaidAmountMap[ym] ?: 0.0
      val isFullyPaid = paidSoFar >= (net - 0.01)
      val remaining = if (isFullyPaid) 0.0 else max(0.0, net - paidSoFar)
      val isPartPaid = paidSoFar > 0.0 && !isFullyPaid
      val isFuture = ym > billingCutoffMonth

      MonthFeeDetail(
        yearMonth = ym,
        monthLabel = formatYearMonthToDisplay(ym),
        baseFee = baseMonthlyFee,
        discountAmount = discAmt,
        discountName = discountRes?.discountTitle,
        netPayable = net,
        amountPaid = paidSoFar,
        remainingDue = remaining,
        isPaid = isFullyPaid,
        isPartiallyPaid = isPartPaid,
        isAdvance = isFuture
      )
    }

    // Billed months: months up to billing cutoff month
    val billedMonths = monthDetails.filter { !it.isAdvance }
    // Advance months: future months beyond billing cutoff
    val advanceMonths = monthDetails.filter { it.isAdvance }

    val paidMonthsList = monthDetails.filter { it.isPaid }
    val advancePaidMonths = advanceMonths.filter { it.isPaid }

    // Pending months are unpaid/part-paid months in the billed cycle
    val pendingMonthsList = billedMonths.filter { !it.isPaid }

    val totalBase = billedMonths.sumOf { it.baseFee }
    val totalDisc = billedMonths.sumOf { it.discountAmount }
    val totalNet = billedMonths.sumOf { it.netPayable }

    val actualTotalPaid = maxOf(
      approvedPayments.sumOf { it.finalAmountPaid },
      monthDetails.sumOf { it.amountPaid } + unallocatedAdvance
    )

    val rawBilledDue = pendingMonthsList.sumOf { it.remainingDue }
    val totalDue = maxOf(0.0, rawBilledDue - unallocatedAdvance)
    val excessUnallocated = maxOf(0.0, unallocatedAdvance - rawBilledDue)
    val netAdvanceBalance = advanceMonths.sumOf { it.amountPaid } + excessUnallocated

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
      totalPaid = actualTotalPaid,
      totalOutstandingDue = totalDue,
      pendingMonths = pendingMonthsList,
      paidMonths = paidMonthsList,
      allMonths = monthDetails,
      advanceBalance = netAdvanceBalance,
      advanceMonthsCount = advancePaidMonths.size
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
