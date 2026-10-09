package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.PathsalaRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Pixel Pathsala", appName)
  }

  @Test
  fun `student login with valid credentials succeeds`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = PathsalaRepository(context)

    // Using seed student: Rohan Sharma (mobile: 9876543210, aadhaar: 453289012345)
    val result = repository.loginStudent("9876543210", "453289012345")
    assertTrue("Student login should succeed", result.isSuccess)
    val student = result.getOrNull()
    assertNotNull(student)
    assertEquals("Rohan Sharma", student?.name)
    assertEquals("PP-2026-001", student?.rollNo)
  }

  @Test
  fun `admin login with master credentials succeeds`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = PathsalaRepository(context)

    val result = repository.loginAdmin("admin", "admin123")
    assertTrue("Admin login should succeed", result.isSuccess)
    val admin = result.getOrNull()
    assertNotNull(admin)
    assertEquals("admin", admin?.username)
  }

  @Test
  fun `submit student fee payment records pending receipt`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = PathsalaRepository(context)

    val student = repository.students.value.first()
    val initialPaymentCount = repository.payments.value.size

    val payment = repository.submitStudentFeePayment(
      student = student,
      months = listOf("2026-07"),
      amount = 1200.0,
      mode = "UPI",
      ref = "UPI-TEST-123456",
      remarks = "Test Payment"
    )

    assertNotNull(payment)
    assertEquals("pending", payment.status)
    assertEquals(initialPaymentCount + 1, repository.payments.value.size)
  }

  @Test
  fun `test webhook post directly`() {
    val success = com.example.data.sheet.GoogleSheetSyncService.postToWebhook("{\"action\":\"ping\"}")
    assertTrue(success)
  }

  @Test
  fun `session persists across app restarts until explicit logout`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo1 = PathsalaRepository(context)

    // Clear any previous session
    repo1.logoutStudent()
    repo1.logoutAdmin()

    // 1. Log in student
    val loginResult = repo1.loginStudent("9876543210", "453289012345")
    assertTrue(loginResult.isSuccess)
    assertEquals("Rohan Sharma", repo1.currentStudent.value?.name)

    // 2. Simulate app restart (creating a new repository instance as occurs when app restarts)
    val repo2 = PathsalaRepository(context)
    assertNotNull("Session should persist on restart", repo2.currentStudent.value)
    assertEquals("Rohan Sharma", repo2.currentStudent.value?.name)

    // 3. Explicit logout clears session
    repo2.logoutStudent()
    val repo3 = PathsalaRepository(context)
    org.junit.Assert.assertNull("Session should be null after explicit logout", repo3.currentStudent.value)
  }

  @Test
  fun `delete payment removes receipt from ledger`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = PathsalaRepository(context)

    val student = repository.students.value.first()
    val newPay = repository.submitStudentFeePayment(
      student = student,
      months = listOf("April 2026"),
      amount = 1200.0,
      mode = "CASH",
      ref = "TEST-REF-DEL",
      remarks = "Delete Test"
    )
    val countBefore = repository.payments.value.size
    assertTrue(repository.payments.value.any { it.id == newPay.id })

    repository.deletePayment(newPay.id)
    assertEquals(countBefore - 1, repository.payments.value.size)
    org.junit.Assert.assertFalse(repository.payments.value.any { it.id == newPay.id })
  }

  @Test
  fun `fee calculator computes payable as per batch course and dues from admission date adjusted with payments`() {
    val student = com.example.data.model.Student(
      id = "stu-test-calc",
      rollNo = "PP-2026-999",
      name = "Subhadip Roy",
      mobile = "9876543210",
      aadhaarNo = "123456789012",
      courseId = "course-xi",
      batchId = "batch-morning",
      admissionDate = "2026-08-15" // August 2026
    )

    val course = com.example.data.model.Course(
      id = "course-xi",
      title = "Computer Science (XI)",
      code = "COMS-011",
      monthlyFee = 400.0
    )

    val batch = com.example.data.model.Batch(
      id = "batch-morning",
      name = "Morning Batch",
      courseId = "course-xi"
    )

    // Case 1: No payments made
    val summaryNoPay = com.example.util.FeeCalculator.calculateStudentFeeSummary(
      student = student,
      courses = listOf(course),
      batches = listOf(batch),
      allPayments = emptyList()
    )
    assertEquals(400.0, summaryNoPay.monthlyFee, 0.01)
    // From August 2026 to October 2026 = 3 months
    assertTrue(summaryNoPay.monthsElapsed.size >= 3)
    val expectedBilled = summaryNoPay.monthsElapsed.size * 400.0
    assertEquals(expectedBilled, summaryNoPay.totalBilled, 0.01)
    assertEquals(expectedBilled, summaryNoPay.dueAmount, 0.01)
    assertEquals(0.0, summaryNoPay.totalPaid, 0.01)

    // Case 2: One payment of ₹400 made
    val payment1 = com.example.data.model.FeePayment(
      id = "pay-test-1",
      receiptNo = "REC/2026/08-001",
      studentId = student.id,
      studentName = student.name,
      finalAmountPaid = 400.0,
      paymentMode = "UPI",
      month = "August 2026",
      status = "approved"
    )

    val summaryWithPay = com.example.util.FeeCalculator.calculateStudentFeeSummary(
      student = student,
      courses = listOf(course),
      batches = listOf(batch),
      allPayments = listOf(payment1)
    )
    assertEquals(400.0, summaryWithPay.totalPaid, 0.01)
    assertEquals(expectedBilled - 400.0, summaryWithPay.dueAmount, 0.01)
    assertTrue(summaryWithPay.monthsElapsed.first().isPaid)
  }

  @Test
  fun `discount is calculated and payable from admission month is net of discount`() {
    val studentWithDiscount = com.example.data.model.Student(
      id = "stu-disc-1",
      rollNo = "PP-2026-111",
      name = "Amitava Roy",
      mobile = "9876500000",
      aadhaarNo = "111122223333",
      courseId = "course-xi",
      batchId = "batch-morning",
      admissionDate = "2026-08-15", // August 2026
      monthlyDiscount = 50.0,
      discountReason = "Merit Concession"
    )

    val course = com.example.data.model.Course(
      id = "course-xi",
      title = "Computer Science (XI)",
      code = "COMS-011",
      monthlyFee = 400.0
    )

    val batch = com.example.data.model.Batch(
      id = "batch-morning",
      name = "Morning Batch",
      courseId = "course-xi"
    )

    val summary = com.example.util.FeeCalculator.calculateStudentFeeSummary(
      student = studentWithDiscount,
      courses = listOf(course),
      batches = listOf(batch),
      allPayments = emptyList()
    )

    assertEquals(400.0, summary.standardMonthlyFee, 0.01)
    assertEquals(50.0, summary.monthlyDiscount, 0.01)
    assertEquals(350.0, summary.effectiveMonthlyFee, 0.01)
    assertEquals(350.0, summary.monthlyFee, 0.01)

    // Number of months from August 2026 to October 2026 = 3 months
    val mos = summary.monthsElapsed.size
    assertTrue("At least 3 billing months from August 2026", mos >= 3)

    val expectedGross = mos * 400.0
    val expectedDiscount = mos * 50.0
    val expectedNetPayable = expectedGross - expectedDiscount

    assertEquals(expectedGross, summary.totalGrossBilled, 0.01)
    assertEquals(expectedGross, summary.grossBilledFromAdmissionMonth, 0.01)
    assertEquals(expectedDiscount, summary.totalDiscountAllowed, 0.01)
    assertEquals(expectedNetPayable, summary.payableFromAdmissionMonth, 0.01)
    assertEquals(expectedNetPayable, summary.dueAmount, 0.01)

    // Verify each month's status reflects discount
    summary.monthsElapsed.forEach { m ->
      assertEquals(400.0, m.standardFee, 0.01)
      assertEquals(50.0, m.discount, 0.01)
      assertEquals(350.0, m.netPayable, 0.01)
    }
  }

  @Test
  fun `due calculation strictly starts from student admission date and does not bill one month before`() {
    val studentAdmittedSeptember = com.example.data.model.Student(
      id = "stu-test-sep",
      rollNo = "PP-2026-593",
      name = "Test Student",
      mobile = "1234567890",
      aadhaarNo = "123456789012",
      courseId = "course-xi",
      batchId = "batch-morning",
      admissionDate = "2026-09-01" // Admitted in September
    )

    // Even if an application inquiry occurred in August, fee billing must start strictly at admission date (September)
    val admissionApp = com.example.data.model.AdmissionApplication(
      id = "adm-1788976622086",
      studentName = "Test Student",
      mobile = "1234567890",
      aadhaarNo = "123456789012",
      targetCourseId = "course-xi",
      appliedDate = "2026-08-21T18:30:00.000Z",
      status = "approved"
    )

    val course = com.example.data.model.Course(
      id = "course-xi",
      title = "Computer Science (XI)",
      code = "COMS-011",
      monthlyFee = 400.0
    )

    val batch = com.example.data.model.Batch(
      id = "batch-morning",
      name = "Morning Batch",
      courseId = "course-xi"
    )

    val summary = com.example.util.FeeCalculator.calculateStudentFeeSummary(
      student = studentAdmittedSeptember,
      courses = listOf(course),
      batches = listOf(batch),
      allPayments = emptyList(),
      admissions = listOf(admissionApp)
    )

    // Must resolve to September 2026 (admission date), NOT August (one month before admission)
    assertEquals("September 2026", summary.admissionMonth)
    // First billed month must be September, never August
    assertEquals("September 2026", summary.monthsElapsed.first().monthLabel)
    assertEquals("2026-09", summary.monthsElapsed.first().monthKey)
  }

  @Test
  fun `class alarm date parser correctly handles ISO and Indian date formats for future live classes`() {
    val calIso = com.example.alarm.ClassAlarmManager.parseClassDateTime("2026-10-10", "11:00")
    assertNotNull(calIso)
    assertEquals(2026, calIso!!.get(java.util.Calendar.YEAR))
    assertEquals(9, calIso.get(java.util.Calendar.MONTH)) // October is 9 in Calendar
    assertEquals(10, calIso.get(java.util.Calendar.DAY_OF_MONTH))
    assertEquals(11, calIso.get(java.util.Calendar.HOUR_OF_DAY))

    val calIndian = com.example.alarm.ClassAlarmManager.parseClassDateTime("10-10-2026", "09:00 AM")
    assertNotNull(calIndian)
    assertEquals(2026, calIndian!!.get(java.util.Calendar.YEAR))
    assertEquals(9, calIndian.get(java.util.Calendar.MONTH))
    assertEquals(10, calIndian.get(java.util.Calendar.DAY_OF_MONTH))
    assertEquals(9, calIndian.get(java.util.Calendar.HOUR_OF_DAY))
  }

  @Test
  fun `custom fee override of 50 is treated as monthly discount concession`() {
    val studentWithOverride = com.example.data.model.Student(
      id = "stu-override",
      rollNo = "PP-2026-999",
      name = "Subhasis Mondal",
      mobile = "9876543210",
      aadhaarNo = "999988887777",
      courseId = "course-xi",
      batchId = "batch-morning",
      admissionDate = "2026-08-01",
      customMonthlyFeeOverride = 50.0 // Entered 50 as concession amount
    )

    val course = com.example.data.model.Course(
      id = "course-xi",
      title = "Computer Science (XI)",
      code = "COMS-011",
      monthlyFee = 400.0
    )

    val batch = com.example.data.model.Batch(
      id = "batch-morning",
      name = "Morning Batch",
      courseId = "course-xi"
    )

    val discount = com.example.util.FeeCalculator.getMonthlyDiscount(studentWithOverride, listOf(course), listOf(batch))
    val netFee = com.example.util.FeeCalculator.getMonthlyFee(studentWithOverride, listOf(course), listOf(batch))

    assertEquals(50.0, discount, 0.01)
    assertEquals(350.0, netFee, 0.01)

    val summary = com.example.util.FeeCalculator.calculateStudentFeeSummary(
      student = studentWithOverride,
      courses = listOf(course),
      batches = listOf(batch),
      allPayments = emptyList()
    )

    assertEquals(50.0, summary.monthlyDiscount, 0.01)
    assertEquals(350.0, summary.effectiveMonthlyFee, 0.01)
    val mos = summary.monthsElapsed.size
    assertEquals(mos * 350.0, summary.payableFromAdmissionMonth, 0.01)
  }

  @Test
  fun `database discount table rules calculate pending payment and discounts accurately from admission date to current month`() {
    val student = com.example.data.model.Student(
      id = "stu-1788672835416",
      rollNo = "PP-2026-255",
      name = "Abhinaba Som",
      mobile = "9647750688",
      aadhaarNo = "3056 3793 9454",
      courseId = "course-1788019409876",
      batchId = "batch-1788019693229",
      admissionDate = "2026-08-15"
    )

    val course = com.example.data.model.Course(
      id = "course-1788019409876",
      title = "COMPUTER SCIENCE (XI)",
      code = "COMS-011",
      monthlyFee = 400.0
    )

    val batch = com.example.data.model.Batch(
      id = "batch-1788019693229",
      name = "MORNING BATCH (COMS-011)",
      courseId = "course-1788019409876"
    )

    val dbDiscounts = listOf(
      com.example.data.model.Discount(
        id = "disc-all",
        title = "Center-Wide Concession (Assigned in All Options)",
        type = "flat",
        value = 250.0,
        scope = "all",
        applicableMonthsType = "all_months",
        active = true,
        reason = "Universal academic fee concession assigned to all options"
      )
    )

    val admissionPayment = com.example.data.model.FeePayment(
      id = "pay-adm-1",
      receiptNo = "REC/2026/ADM-255",
      studentId = student.id,
      studentName = student.name,
      month = "August 2026",
      monthsCovered = listOf("2026-08"),
      baseMonthlyFee = 400.0,
      totalBaseFee = 400.0,
      totalDiscount = 250.0,
      finalAmountPaid = 150.0,
      paymentMode = "UPI",
      status = "approved",
      paymentDate = "2026-08-15"
    )

    val summary = com.example.util.FeeCalculator.calculateStudentFeeSummary(
      student = student,
      courses = listOf(course),
      batches = listOf(batch),
      allPayments = listOf(admissionPayment),
      discounts = dbDiscounts
    )

    assertEquals(400.0, summary.standardMonthlyFee, 0.01)
    assertEquals(250.0, summary.monthlyDiscount, 0.01)
    assertEquals(150.0, summary.effectiveMonthlyFee, 0.01)
    assertEquals("August 2026", summary.admissionMonth)
    assertTrue("At least 3 billing months elapsed (August, September, October)", summary.monthsElapsed.size >= 3)

    val monthsCount = summary.monthsElapsed.size
    assertEquals(monthsCount * 400.0, summary.totalGrossBilled, 0.01)
    assertEquals(monthsCount * 250.0, summary.totalDiscountAllowed, 0.01)
    assertEquals(monthsCount * 150.0, summary.totalNetBilled, 0.01)
    assertEquals(150.0, summary.totalPaid, 0.01)

    val expectedDue = (monthsCount - 1) * 150.0
    assertEquals(expectedDue, summary.dueAmount, 0.01)
    assertEquals(monthsCount - 1, summary.dueMonthsCount)

    assertTrue(summary.monthsElapsed.first().isPaid)
    assertTrue(!summary.monthsElapsed[1].isPaid)
  }
}
