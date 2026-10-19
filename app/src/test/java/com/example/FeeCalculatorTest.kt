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
}
