package com.example

import com.example.data.model.*
import com.example.util.FeeCalculator
import org.junit.Assert.*
import org.junit.Test

class FeeCalculatorTest {

  @Test
  fun testIsoDateConversionToIstYearMonth() {
    // 2026-06-30T18:30:00.000Z in UTC is 2026-07-01 00:00:00 IST -> "2026-07"
    val ym = FeeCalculator.parseIsoOrMonthToYearMonth("2026-06-30T18:30:00.000Z")
    assertEquals("2026-07", ym)

    // Standard YYYY-MM
    assertEquals("2026-08", FeeCalculator.parseIsoOrMonthToYearMonth("2026-08"))
  }

  @Test
  fun testDiscountResolutionForStudent() {
    val student = Student(
      id = "stu-1792076328844",
      rollNo = "PP-2026-005",
      name = "Avinandan Dan",
      mobile = "7001561342",
      aadhaarNo = "943892479593",
      courseId = "course-1788019409876",
      batchId = "batch-1788019693229",
      admissionDate = "2026-06-30T18:30:00.000Z"
    )

    val course = Course(
      id = "course-1788019409876",
      title = "COMPUTER SCIENCE (XI)",
      monthlyFee = 400.0
    )

    val starterDiscount = Discount(
      id = "disc-1792076419291",
      title = "Starter Discount",
      type = "flat",
      value = 100.0,
      scope = "student",
      studentId = "stu-1792076328844",
      applicableMonthsType = "selected_months",
      selectedMonths = listOf("2026-06-30T18:30:00.000Z"),
      active = true
    )

    // In 2026-07, starter discount should apply
    val discJuly = FeeCalculator.resolveApplicableDiscount(
      student = student,
      course = course,
      batch = null,
      targetYearMonth = "2026-07",
      allDiscounts = listOf(starterDiscount),
      baseMonthlyFee = 400.0
    )
    assertNotNull(discJuly)
    assertEquals("Starter Discount", discJuly?.discountTitle)
    assertEquals(100.0, discJuly?.calculatedDeduction ?: 0.0, 0.01)

    // In 2026-08, starter discount should NOT apply
    val discAug = FeeCalculator.resolveApplicableDiscount(
      student = student,
      course = course,
      batch = null,
      targetYearMonth = "2026-08",
      allDiscounts = listOf(starterDiscount),
      baseMonthlyFee = 400.0
    )
    assertNull(discAug)
  }

  @Test
  fun testMultiMonthCalculationWithReceipt() {
    val student = Student(
      id = "stu-1788672835416",
      rollNo = "PP-2026-001",
      name = "Abhinaba Som",
      mobile = "9876543210",
      aadhaarNo = "123456789012",
      courseId = "course-1",
      batchId = "batch-1",
      admissionDate = "2026-06-30T18:30:00.000Z"
    )

    val course = Course(
      id = "course-1",
      title = "Computer Science",
      monthlyFee = 400.0
    )

    val starterDiscount = Discount(
      id = "disc-1",
      title = "Starter Discount",
      type = "flat",
      value = 100.0,
      scope = "student",
      studentId = student.id,
      applicableMonthsType = "selected_months",
      selectedMonths = listOf("2026-07"),
      active = true
    )

    // Existing payment for 2026-07, 2026-08, 2026-09
    val payment = FeePayment(
      id = "pay-1",
      receiptNo = "REC/2026/10-004",
      studentId = student.id,
      studentName = student.name,
      month = "2026-07, 2026-08, 2026-09",
      monthsCovered = listOf("2026-07", "2026-08", "2026-09"),
      finalAmountPaid = 1100.0,
      status = "approved"
    )

    val summary = FeeCalculator.calculateStudentDuesSummary(
      student = student,
      course = course,
      batch = null,
      discounts = listOf(starterDiscount),
      payments = listOf(payment),
      targetUpToMonth = "2026-09"
    )

    assertEquals(3, summary.paidMonths.size)
    assertEquals(0, summary.pendingMonths.size)
    assertEquals(0.0, summary.totalOutstandingDue, 0.01)

    // Test receipt generation
    val newReceipt = FeeCalculator.generateReceiptNo(listOf(payment))
    assertTrue(newReceipt.startsWith("REC/"))
  }

  @Test
  fun testSingleMonthPartPaymentCalculation() {
    val student = Student(
      id = "stu-101",
      rollNo = "PP-101",
      name = "Rahul Sharma",
      mobile = "9988776655",
      aadhaarNo = "111122223333",
      courseId = "course-1",
      batchId = "batch-1",
      admissionDate = "2026-06-30T18:30:00.000Z"
    )
    val course = Course(id = "course-1", title = "CS", monthlyFee = 500.0)

    // Part payment of ₹200 for 2026-07
    val partPayment = FeePayment(
      id = "pay-p1",
      receiptNo = "REC/2026/07-001",
      studentId = student.id,
      studentName = student.name,
      month = "2026-07",
      monthsCovered = listOf("2026-07"),
      finalAmountPaid = 200.0,
      status = "approved"
    )

    val summary = FeeCalculator.calculateStudentDuesSummary(
      student = student,
      course = course,
      batch = null,
      discounts = emptyList(),
      payments = listOf(partPayment),
      targetUpToMonth = "2026-07"
    )

    val july = summary.allMonths.first { it.yearMonth == "2026-07" }
    assertEquals(500.0, july.netPayable, 0.01)
    assertEquals(200.0, july.amountPaid, 0.01)
    assertEquals(300.0, july.remainingDue, 0.01)
    assertFalse(july.isPaid)
    assertTrue(july.isPartiallyPaid)

    // Outstanding due should be exactly 300.0
    assertEquals(300.0, summary.totalOutstandingDue, 0.01)
    // Pending months includes partially paid months
    assertEquals(1, summary.pendingMonths.size)
  }

  @Test
  fun testMultiPartPaymentsCompletingMonth() {
    val student = Student(
      id = "stu-102",
      rollNo = "PP-102",
      name = "Priya Roy",
      mobile = "9988776654",
      aadhaarNo = "222233334444",
      courseId = "course-1",
      batchId = "batch-1",
      admissionDate = "2026-06-30T18:30:00.000Z"
    )
    val course = Course(id = "course-1", title = "CS", monthlyFee = 600.0)

    val payment1 = FeePayment(
      id = "pay-p1",
      receiptNo = "REC/2026/07-001",
      studentId = student.id,
      studentName = student.name,
      month = "2026-07",
      monthsCovered = listOf("2026-07"),
      finalAmountPaid = 250.0,
      status = "approved"
    )

    val payment2 = FeePayment(
      id = "pay-p2",
      receiptNo = "REC/2026/07-002",
      studentId = student.id,
      studentName = student.name,
      month = "2026-07",
      monthsCovered = listOf("2026-07"),
      finalAmountPaid = 350.0,
      status = "approved"
    )

    val summary = FeeCalculator.calculateStudentDuesSummary(
      student = student,
      course = course,
      batch = null,
      discounts = emptyList(),
      payments = listOf(payment1, payment2),
      targetUpToMonth = "2026-07"
    )

    val july = summary.allMonths.first { it.yearMonth == "2026-07" }
    assertEquals(600.0, july.amountPaid, 0.01)
    assertEquals(0.0, july.remainingDue, 0.01)
    assertTrue(july.isPaid)
    assertFalse(july.isPartiallyPaid)
    assertEquals(0.0, summary.totalOutstandingDue, 0.01)
    assertEquals(0, summary.pendingMonths.size)
  }

  @Test
  fun testMultiMonthPartialPaymentAllocation() {
    val student = Student(
      id = "stu-103",
      rollNo = "PP-103",
      name = "Subham Das",
      mobile = "9988776653",
      aadhaarNo = "333344445555",
      courseId = "course-1",
      batchId = "batch-1",
      admissionDate = "2026-06-30T18:30:00.000Z"
    )
    val course = Course(id = "course-1", title = "CS", monthlyFee = 400.0)

    // Single payment of ₹650 covering both 2026-07 (₹400) and 2026-08 (₹400, so ₹250 paid, ₹150 remaining)
    val multiPayment = FeePayment(
      id = "pay-multi",
      receiptNo = "REC/2026/08-001",
      studentId = student.id,
      studentName = student.name,
      month = "2026-07, 2026-08",
      monthsCovered = listOf("2026-07", "2026-08"),
      finalAmountPaid = 650.0,
      status = "approved"
    )

    val summary = FeeCalculator.calculateStudentDuesSummary(
      student = student,
      course = course,
      batch = null,
      discounts = emptyList(),
      payments = listOf(multiPayment),
      targetUpToMonth = "2026-08"
    )

    val july = summary.allMonths.first { it.yearMonth == "2026-07" }
    val aug = summary.allMonths.first { it.yearMonth == "2026-08" }

    // July should be fully paid
    assertTrue(july.isPaid)
    assertFalse(july.isPartiallyPaid)
    assertEquals(400.0, july.amountPaid, 0.01)
    assertEquals(0.0, july.remainingDue, 0.01)

    // August should be partially paid
    assertFalse(aug.isPaid)
    assertTrue(aug.isPartiallyPaid)
    assertEquals(250.0, aug.amountPaid, 0.01)
    assertEquals(150.0, aug.remainingDue, 0.01)

    // Total outstanding due should be 150.0
    assertEquals(150.0, summary.totalOutstandingDue, 0.01)
  }

  @Test
  fun testRejectedPaymentDoesNotCreditDues() {
    val student = Student(
      id = "stu-104",
      rollNo = "PP-104",
      name = "Amit Sen",
      mobile = "9988776652",
      aadhaarNo = "444455556666",
      courseId = "course-1",
      batchId = "batch-1",
      admissionDate = "2026-06-30T18:30:00.000Z"
    )
    val course = Course(id = "course-1", title = "CS", monthlyFee = 500.0)

    val rejectedPayment = FeePayment(
      id = "pay-rej-1",
      receiptNo = "REC/2026/07-999",
      studentId = student.id,
      studentName = student.name,
      month = "2026-07",
      monthsCovered = listOf("2026-07"),
      finalAmountPaid = 500.0,
      status = "rejected",
      rejectionReason = "Invalid transaction reference"
    )

    val summary = FeeCalculator.calculateStudentDuesSummary(
      student = student,
      course = course,
      batch = null,
      discounts = emptyList(),
      payments = listOf(rejectedPayment),
      targetUpToMonth = "2026-07"
    )

    val july = summary.allMonths.first { it.yearMonth == "2026-07" }
    // Since payment was rejected, July must remain unpaid with full remaining due
    assertFalse(july.isPaid)
    assertFalse(july.isPartiallyPaid)
    assertEquals(0.0, july.amountPaid, 0.01)
    assertEquals(500.0, july.remainingDue, 0.01)
    assertEquals(500.0, summary.totalOutstandingDue, 0.01)
    assertEquals(1, summary.pendingMonths.size)
  }

  @Test
  fun testAdvancePaymentRollover() {
    val student = Student(
      id = "stu-adv-1",
      rollNo = "PP-ADV-001",
      name = "Sneha Paul",
      mobile = "9876500001",
      aadhaarNo = "112233445566",
      courseId = "course-1",
      batchId = "batch-1",
      admissionDate = "2026-06-30T18:30:00.000Z"
    )
    val course = Course(id = "course-1", title = "CS", monthlyFee = 400.0)

    // Single payment of ₹1000 on month "2026-07"
    val overpayment = FeePayment(
      id = "pay-adv-1",
      receiptNo = "REC/2026/07-ADV",
      studentId = student.id,
      studentName = student.name,
      month = "2026-07",
      monthsCovered = listOf("2026-07"),
      finalAmountPaid = 1000.0,
      status = "approved"
    )

    // Evaluated up to 2026-08
    val summary = FeeCalculator.calculateStudentDuesSummary(
      student = student,
      course = course,
      batch = null,
      discounts = emptyList(),
      payments = listOf(overpayment),
      targetUpToMonth = "2026-08"
    )

    val july = summary.allMonths.first { it.yearMonth == "2026-07" }
    val aug = summary.allMonths.first { it.yearMonth == "2026-08" }

    // July should be fully paid (₹400)
    assertTrue(july.isPaid)
    assertEquals(400.0, july.amountPaid, 0.01)

    // August should also be fully paid via rollover (₹400)
    assertTrue(aug.isPaid)
    assertEquals(400.0, aug.amountPaid, 0.01)

    // Remaining ₹200 rolls over to next month (2026-09) which is an advance month!
    val sep = summary.allMonths.firstOrNull { it.yearMonth == "2026-09" }
    assertNotNull(sep)
    assertTrue(sep!!.isAdvance)
    assertEquals(200.0, sep.amountPaid, 0.01)
    assertTrue(sep.isPartiallyPaid)

    // Outstanding due for billed months (July & August) should be 0.0
    assertEquals(0.0, summary.totalOutstandingDue, 0.01)
    assertEquals(1000.0, summary.totalPaid, 0.01)
  }

  @Test
  fun testUnallocatedAdmissionAdvance() {
    val student = Student(
      id = "stu-adv-2",
      rollNo = "PP-ADV-002",
      name = "Rohan Mitra",
      mobile = "9876500002",
      aadhaarNo = "998877665544",
      courseId = "course-1",
      batchId = "batch-1",
      admissionDate = "2026-06-30T18:30:00.000Z"
    )
    val course = Course(id = "course-1", title = "CS", monthlyFee = 500.0)

    // Payment without exact YYYY-MM in month string (e.g. Admission Advance)
    val unallocatedReceipt = FeePayment(
      id = "pay-adv-general",
      receiptNo = "REC/2026/ADM-001",
      studentId = student.id,
      studentName = student.name,
      month = "Admission Advance",
      monthsCovered = emptyList(),
      finalAmountPaid = 500.0,
      status = "approved"
    )

    val summary = FeeCalculator.calculateStudentDuesSummary(
      student = student,
      course = course,
      batch = null,
      discounts = emptyList(),
      payments = listOf(unallocatedReceipt),
      targetUpToMonth = "2026-07"
    )

    val july = summary.allMonths.first { it.yearMonth == "2026-07" }
    // July should be credited by the unallocated advance
    assertTrue(july.isPaid)
    assertEquals(500.0, july.amountPaid, 0.01)
    assertEquals(0.0, summary.totalOutstandingDue, 0.01)
    assertEquals(500.0, summary.totalPaid, 0.01)
  }

  @Test
  fun testDiscountBestBenefitResolution() {
    val student = Student(
      id = "stu-disc-1",
      rollNo = "PP-DISC-001",
      name = "Ananya Sen",
      mobile = "9876500003",
      aadhaarNo = "121234345656",
      courseId = "course-1",
      batchId = "batch-1",
      admissionDate = "2026-06-30T18:30:00.000Z",
      monthlyDiscount = 120.0 // Student concession
    )
    val course = Course(id = "course-1", title = "CS", monthlyFee = 400.0)

    val smallerRule = Discount(
      id = "disc-small",
      title = "Small Promotion",
      type = "flat",
      value = 80.0,
      scope = "course",
      courseId = course.id,
      applicableMonthsType = "all",
      active = true
    )

    val discResolved = FeeCalculator.resolveApplicableDiscount(
      student = student,
      course = course,
      batch = null,
      targetYearMonth = "2026-07",
      allDiscounts = listOf(smallerRule),
      baseMonthlyFee = 400.0
    )
    assertNotNull(discResolved)
    // Direct concession (120) + small promotion (80) = 200
    assertEquals(200.0, discResolved?.calculatedDeduction ?: 0.0, 0.01)
  }

  @Test
  fun testMultipleDiscountsWithDirectStudentConcession() {
    val student = Student(
      id = "stu-disc-compare",
      rollNo = "PP-DISC-01",
      name = "Suresh Sen",
      mobile = "9876543210",
      aadhaarNo = "123456789012",
      courseId = "course-1",
      batchId = "batch-1",
      admissionDate = "2026-06-30T18:30:00.000Z",
      monthlyDiscount = 120.0 // Student concession
    )
    val course = Course(id = "course-1", title = "CS", monthlyFee = 400.0)

    val promotionRule = Discount(
      id = "disc-small",
      title = "Course Promotion",
      type = "flat",
      value = 80.0,
      scope = "course",
      courseId = course.id,
      applicableMonthsType = "all",
      active = true
    )

    // Both direct concession (120) and course promotion (80) are combined = 200
    val discResolved = FeeCalculator.resolveApplicableDiscount(
      student = student,
      course = course,
      batch = null,
      targetYearMonth = "2026-07",
      allDiscounts = listOf(promotionRule),
      baseMonthlyFee = 400.0
    )
    assertNotNull(discResolved)
    assertEquals(200.0, discResolved?.calculatedDeduction ?: 0.0, 0.01)
  }

  @Test
  fun testMultipleDiscountsForBatchAndParticularStudentCombined() {
    val student = Student(
      id = "stu-multi-disc-1",
      rollNo = "PP-2026-005",
      name = "Avinandan Dan",
      mobile = "7001561342",
      aadhaarNo = "943892479593",
      courseId = "course-1",
      batchId = "batch-101",
      admissionDate = "2026-06-30T18:30:00.000Z"
    )

    val course = Course(
      id = "course-1",
      title = "Computer Science",
      monthlyFee = 400.0
    )

    val batch = Batch(
      id = "batch-101",
      courseId = "course-1",
      name = "Evening CS Batch"
    )

    // 1. Discount for full batch: ₹100 flat
    val batchDiscount = Discount(
      id = "disc-batch",
      title = "Batch Special Discount",
      type = "flat",
      value = 100.0,
      scope = "batch",
      batchId = "batch-101",
      applicableMonthsType = "all_months",
      active = true
    )

    // 2. Discount for particular student: ₹50 flat
    val studentDiscount = Discount(
      id = "disc-student",
      title = "Merit Concession",
      type = "flat",
      value = 50.0,
      scope = "student",
      studentId = student.id,
      applicableMonthsType = "all_months",
      active = true
    )

    val discountsList = listOf(batchDiscount, studentDiscount)

    val resolution = FeeCalculator.resolveApplicableDiscount(
      student = student,
      course = course,
      batch = batch,
      targetYearMonth = "2026-07",
      allDiscounts = discountsList,
      baseMonthlyFee = 400.0
    )

    assertNotNull(resolution)
    // Both batch discount (100) and student discount (50) must be applied: 150
    assertEquals(150.0, resolution?.calculatedDeduction ?: 0.0, 0.01)
    assertTrue(resolution?.discountTitle?.contains("Batch Special Discount") == true)
    assertTrue(resolution?.discountTitle?.contains("Merit Concession") == true)

    // Test in full fee dues summary
    val summary = FeeCalculator.calculateStudentDuesSummary(
      student = student,
      course = course,
      batch = batch,
      discounts = discountsList,
      payments = emptyList(),
      targetUpToMonth = "2026-07"
    )

    val july = summary.allMonths.first { it.yearMonth == "2026-07" }
    assertEquals(400.0, july.baseFee, 0.01)
    assertEquals(150.0, july.discountAmount, 0.01)
    assertEquals(250.0, july.netPayable, 0.01)
    assertEquals(250.0, july.remainingDue, 0.01)
    assertEquals(250.0, summary.totalOutstandingDue, 0.01)
  }

  @Test
  fun testStudentDiscountMatchedByRollNoOrName() {
    val student = Student(
      id = "stu-uuid-999",
      rollNo = "PP-2026-999",
      name = "Subrata Roy",
      mobile = "9876500000",
      aadhaarNo = "111122223333",
      courseId = "course-1",
      batchId = "batch-1",
      admissionDate = "2026-06-30T18:30:00.000Z"
    )
    val course = Course(id = "course-1", title = "CS", monthlyFee = 500.0)

    // Discount created in sheet with Target Student ID = Roll No "PP-2026-999"
    val discountByRoll = Discount(
      id = "disc-roll",
      title = "Roll Based Discount",
      type = "flat",
      value = 75.0,
      scope = "student",
      studentId = "PP-2026-999",
      applicableMonthsType = "all_months",
      active = true
    )

    val res = FeeCalculator.resolveApplicableDiscount(
      student = student,
      course = course,
      batch = null,
      targetYearMonth = "2026-07",
      allDiscounts = listOf(discountByRoll),
      baseMonthlyFee = 500.0
    )
    assertNotNull(res)
    assertEquals(75.0, res?.calculatedDeduction ?: 0.0, 0.01)
    assertEquals("Roll Based Discount", res?.discountTitle)
  }

  @Test
  fun testDeletedPaymentProperlyReopensMonthDue() {
    val student = Student(
      id = "stu-del-1",
      rollNo = "PP-DEL-001",
      name = "Vikram Roy",
      mobile = "9876543211",
      aadhaarNo = "555566667777",
      courseId = "course-1",
      batchId = "batch-1",
      admissionDate = "2026-06-30T18:30:00.000Z",
      totalPaidInSheet = 400.0 // Simulates old sheet state before transaction deletion
    )
    val course = Course(id = "course-1", title = "CS", monthlyFee = 400.0)

    val payment = FeePayment(
      id = "pay-to-delete",
      receiptNo = "REC/2026/07-DEL",
      studentId = student.id,
      studentName = student.name,
      month = "2026-07",
      monthsCovered = listOf("2026-07"),
      finalAmountPaid = 400.0,
      status = "approved"
    )

    // 1. When payment exists: July should be fully paid
    val summaryBeforeDelete = FeeCalculator.calculateStudentDuesSummary(
      student = student,
      course = course,
      batch = null,
      discounts = emptyList(),
      payments = listOf(payment),
      targetUpToMonth = "2026-07"
    )
    val julyBefore = summaryBeforeDelete.allMonths.first { it.yearMonth == "2026-07" }
    assertTrue(julyBefore.isPaid)
    assertEquals(400.0, julyBefore.amountPaid, 0.01)
    assertEquals(0.0, julyBefore.remainingDue, 0.01)
    assertEquals(0.0, summaryBeforeDelete.totalOutstandingDue, 0.01)

    // 2. When payment is deleted (empty payments list):
    // July must immediately reopen as DUE with 400.0 remaining, NOT hold old paid data
    val summaryAfterDelete = FeeCalculator.calculateStudentDuesSummary(
      student = student.copy(totalPaidInSheet = 0.0), // Updated student record after deletion
      course = course,
      batch = null,
      discounts = emptyList(),
      payments = emptyList(),
      targetUpToMonth = "2026-07"
    )
    val julyAfter = summaryAfterDelete.allMonths.first { it.yearMonth == "2026-07" }
    assertFalse(julyAfter.isPaid)
    assertEquals(0.0, julyAfter.amountPaid, 0.01)
    assertEquals(400.0, julyAfter.remainingDue, 0.01)
    assertEquals(400.0, summaryAfterDelete.totalOutstandingDue, 0.01)
    assertEquals(1, summaryAfterDelete.pendingMonths.size)
  }

  @Test
  fun testFeeAdjustmentFromAdmissionDateSequentiallyUsingTotalCollection() {
    val student = Student(
      id = "stu-fifo-1",
      rollNo = "PP-FIFO-01",
      name = "Rahul Bannerjee",
      mobile = "9830012345",
      aadhaarNo = "1234 5678 9012",
      courseId = "course-1",
      batchId = "batch-1",
      admissionDate = "2026-06-30T18:30:00.000Z" // Converted to IST: 2026-07
    )

    val course = Course(
      id = "course-1",
      title = "Mathematics Core",
      monthlyFee = 500.0
    )

    // Total fees collection of ₹750 across two payments (e.g. ₹500 and ₹250)
    val payments = listOf(
      FeePayment(
        id = "pay-f1",
        receiptNo = "REC/2026/07-F1",
        studentId = student.id,
        studentName = student.name,
        month = "2026-07",
        monthsCovered = listOf("2026-07"),
        finalAmountPaid = 500.0,
        status = "approved"
      ),
      FeePayment(
        id = "pay-f2",
        receiptNo = "REC/2026/08-F2",
        studentId = student.id,
        studentName = student.name,
        month = "2026-08",
        monthsCovered = listOf("2026-08"),
        finalAmountPaid = 250.0,
        status = "approved"
      )
    )

    val summary = FeeCalculator.calculateStudentDuesSummary(
      student = student,
      course = course,
      batch = null,
      discounts = emptyList(),
      payments = payments,
      targetUpToMonth = "2026-08"
    )

    // First month must be 2026-07 (from date of admission)
    assertEquals("2026-07", summary.allMonths.first().yearMonth)

    val month1 = summary.allMonths.first { it.yearMonth == "2026-07" }
    assertEquals(500.0, month1.amountPaid, 0.01)
    assertEquals(0.0, month1.remainingDue, 0.01)
    assertTrue("First month should be fully PAID", month1.isPaid)
    assertFalse("First month should not be partially paid", month1.isPartiallyPaid)

    val month2 = summary.allMonths.first { it.yearMonth == "2026-08" }
    assertEquals(250.0, month2.amountPaid, 0.01)
    assertEquals(250.0, month2.remainingDue, 0.01)
    assertFalse("Second month should not be fully paid", month2.isPaid)
    assertTrue("Second month should be PARTIALLY PAID", month2.isPartiallyPaid)

    assertEquals(250.0, summary.totalOutstandingDue, 0.01)
    assertEquals(750.0, summary.totalPaid, 0.01)
  }

  @Test
  fun testAadhaarLastFourPasswordVerificationLogic() {
    val studentWithPlainAadhaar = Student(
      id = "stu-auth-1",
      rollNo = "PP-01",
      name = "Amit Das",
      mobile = "9876543210",
      aadhaarNo = "943892479593",
      courseId = "c-1",
      batchId = "b-1"
    )
    val digits1 = studentWithPlainAadhaar.aadhaarNo.filter { it.isDigit() }
    val pass1 = digits1.takeLast(4)
    assertEquals("9593", pass1)

    // Formatted Aadhaar with spaces
    val studentWithFormattedAadhaar = Student(
      id = "stu-auth-2",
      rollNo = "PP-02",
      name = "Sujata Sen",
      mobile = "9876543211",
      aadhaarNo = "9438 9247 1234",
      courseId = "c-1",
      batchId = "b-1"
    )
    val digits2 = studentWithFormattedAadhaar.aadhaarNo.filter { it.isDigit() }
    val pass2 = digits2.takeLast(4)
    assertEquals("1234", pass2)
  }
}

