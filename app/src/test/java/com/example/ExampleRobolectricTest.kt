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
}
