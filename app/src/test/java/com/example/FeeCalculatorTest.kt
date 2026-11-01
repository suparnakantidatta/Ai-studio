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
}
