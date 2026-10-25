package com.example.data.sheet

import com.example.data.local.LocalDataStore
import com.example.data.model.*
import com.example.util.FeeCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GoogleSheetSyncService {

  companion object {
    const val WEBHOOK_URL = "https://script.google.com/macros/s/AKfycbz_CEP9stVACI0-6oOiRbJvvfAjn9X1xIVW-m26B2E9Dp4e_aNiyAavFe1jZFAS1jls/exec"
  }

  private val client = OkHttpClient.Builder()
    .connectTimeout(25, TimeUnit.SECONDS)
    .readTimeout(25, TimeUnit.SECONDS)
    .writeTimeout(25, TimeUnit.SECONDS)
    .followRedirects(true)
    .followSslRedirects(true)
    .build()

  suspend fun fetchAllData(): Result<DatabaseDataDto> = withContext(Dispatchers.IO) {
    try {
      val request = Request.Builder()
        .url(WEBHOOK_URL)
        .header("User-Agent", "PixelPathsalaApp/1.0 (Android)")
        .get()
        .build()

      val response = client.newCall(request).execute()
      val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response body"))

      val rootJson = JSONObject(body)
      val dataObj = if (rootJson.has("data")) rootJson.getJSONObject("data") else rootJson

      val dto = parseWebhookData(dataObj)
      Result.success(dto)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun pushNewPayment(payment: FeePayment): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val json = JSONObject().apply {
        put("action", "recordPayment")
        put("receiptNo", payment.receiptNo)
        put("paymentId", payment.id)
        put("studentId", payment.studentId)
        put("studentName", payment.studentName)
        put("amount", payment.finalAmountPaid)
        put("paymentMode", payment.paymentMode)
        put("monthsCovered", payment.monthsCovered.joinToString(", "))
        put("transactionRef", payment.transactionRef ?: "")
        put("status", payment.status)
        put("paymentDate", payment.paymentDate)
        put("approvedBy", payment.approvedBy ?: "")
        put("remarks", payment.remarks ?: "")
      }

      postJson(json)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun pushNewDiscount(discount: Discount): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val json = JSONObject().apply {
        put("action", "addDiscount")
        put("id", discount.id)
        put("title", discount.title)
        put("type", discount.type)
        put("value", discount.value)
        put("scope", discount.scope)
        put("academicClass", discount.academicClass ?: "")
        put("courseId", discount.courseId ?: "")
        put("batchId", discount.batchId ?: "")
        put("studentId", discount.studentId ?: "")
        put("applicableMonthsType", discount.applicableMonthsType)
        put("selectedMonths", discount.selectedMonths?.joinToString(", ") ?: "")
        put("reason", discount.reason ?: "")
        put("active", if (discount.active) "Active" else "Inactive")
      }

      postJson(json)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun updatePaymentApproval(paymentId: String, newStatus: String, approvedBy: String, reason: String?): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val json = JSONObject().apply {
        put("action", "updatePaymentStatus")
        put("paymentId", paymentId)
        put("status", newStatus)
        put("approvedBy", approvedBy)
        put("rejectionReason", reason ?: "")
      }
      postJson(json)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun pushNewStudent(student: Student): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val json = JSONObject().apply {
        put("action", "addStudent")
        put("studentId", student.id)
        put("rollNo", student.rollNo)
        put("name", student.name)
        put("mobile", student.mobile)
        put("aadhaarNo", student.aadhaarNo)
        put("email", student.email ?: "")
        put("guardianName", student.guardianName)
        put("guardianPhone", student.guardianPhone)
        put("address", student.address)
        put("courseId", student.courseId)
        put("batchId", student.batchId)
        put("admissionDate", student.admissionDate)
        put("status", student.status)
        put("customFee", student.customMonthlyFeeOverride ?: "")
      }
      postJson(json)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun pushStudentTelemetry(telemetry: StudentTelemetry): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val json = JSONObject().apply {
        put("action", "recordTelemetry")
        put("studentId", telemetry.studentId)
        put("studentName", telemetry.studentName)
        put("latitude", telemetry.latitude)
        put("longitude", telemetry.longitude)
        put("batteryLevel", telemetry.batteryLevel)
        put("isCharging", telemetry.isCharging)
        put("networkType", telemetry.networkType)
        put("gamesDetected", telemetry.installedGames.joinToString(", "))
        put("focusScore", telemetry.studyFocusScore)
        put("timestamp", telemetry.lastHeartbeat)
      }
      postJson(json)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun pushLiveClass(session: LiveClassSession): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val json = JSONObject().apply {
        put("action", "scheduleClass")
        put("id", session.id)
        put("title", session.title)
        put("subject", session.subject)
        put("courseId", session.courseId ?: "")
        put("batchId", session.batchId ?: "")
        put("educatorName", session.educatorName)
        put("date", session.scheduledDate)
        put("startTime", session.startTime)
        put("endTime", session.endTime)
        put("status", if (session.isPostponed) "postponed" else session.status)
        put("platform", session.platform)
        put("meetingUrl", session.meetingUrl ?: "")
        put("isPostponed", session.isPostponed)
        put("postponeReason", session.postponeReason ?: "")
      }
      postJson(json)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun pushStudyMaterial(material: StudyMaterial): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val json = JSONObject().apply {
        put("action", "addStudyMaterial")
        put("id", material.id)
        put("title", material.title)
        put("subject", material.subject)
        put("courseId", material.courseId ?: "")
        put("batchId", material.batchId ?: "")
        put("fileType", material.fileType)
        put("fileUrl", material.fileUrl)
        put("description", material.description ?: "")
      }
      postJson(json)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun pushExpense(expense: Expense): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val json = JSONObject().apply {
        put("action", "addExpense")
        put("id", expense.id)
        put("title", expense.title)
        put("category", expense.category)
        put("amount", expense.amount)
        put("date", expense.expenseDate)
        put("mode", expense.paymentMode)
        put("approvedBy", expense.approvedBy)
        put("remarks", expense.remarks ?: "")
      }
      postJson(json)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun syncTables(tables: Map<String, JSONArray>): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val tablesObj = JSONObject()
      for ((tableName, tableArray) in tables) {
        tablesObj.put(tableName, tableArray)
      }
      val payload = JSONObject().apply {
        put("action", "sync")
        put("tables", tablesObj)
      }
      postJson(payload)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun syncFeeTransactions(payments: List<FeePayment>): Result<Boolean> = withContext(Dispatchers.IO) {
    val arr = buildFeeTransactionsTable(payments)
    syncTables(mapOf("Fee_Transactions" to arr))
  }

  suspend fun syncStudents(students: List<Student>, courses: List<Course>): Result<Boolean> = withContext(Dispatchers.IO) {
    val arr = buildStudentsTable(students, courses)
    syncTables(mapOf("Students" to arr))
  }

  suspend fun syncDiscounts(discounts: List<Discount>): Result<Boolean> = withContext(Dispatchers.IO) {
    val arr = buildDiscountsTable(discounts)
    syncTables(mapOf("Discounts" to arr))
  }

  suspend fun syncExpenses(expenses: List<Expense>): Result<Boolean> = withContext(Dispatchers.IO) {
    val arr = buildExpensesTable(expenses)
    syncTables(mapOf("Expenses" to arr))
  }

  suspend fun syncLiveClasses(sessions: List<LiveClassSession>): Result<Boolean> = withContext(Dispatchers.IO) {
    val arr = buildLiveClassesTable(sessions)
    syncTables(mapOf("Live_Classes" to arr))
  }

  suspend fun syncAdmissions(admissions: List<AdmissionApplication>): Result<Boolean> = withContext(Dispatchers.IO) {
    val arr = buildAdmissionsTable(admissions)
    syncTables(mapOf("Admissions" to arr))
  }

  suspend fun syncExams(exams: List<OnlineExam>): Result<Boolean> = withContext(Dispatchers.IO) {
    val arr = buildExamsTable(exams)
    syncTables(mapOf("Exams" to arr))
  }

  suspend fun syncSubmissions(submissions: List<ExamSubmission>): Result<Boolean> = withContext(Dispatchers.IO) {
    val arr = buildSubmissionsTable(submissions)
    syncTables(mapOf("Exam_Submissions" to arr))
  }

  fun buildFeeTransactionsTable(payments: List<FeePayment>): JSONArray {
    val arr = JSONArray()
    val header = JSONArray().apply {
      put("Receipt No")
      put("Payment ID")
      put("Student ID")
      put("Student Name")
      put("Amount Paid (₹)")
      put("Payment Mode")
      put("Months Covered")
      put("Transaction Ref / UPI")
      put("Payment Date")
      put("Approval Status")
      put("Approved By")
      put("Rejection Reason")
    }
    arr.put(header)
    for (p in payments) {
      val row = JSONArray().apply {
        put(p.receiptNo)
        put(p.id)
        put(p.studentId)
        put(p.studentName)
        put(p.finalAmountPaid)
        put(p.paymentMode)
        put(p.month.ifBlank { p.monthsCovered.joinToString(", ") })
        put(p.transactionRef ?: "")
        put(p.paymentDate)
        put(p.status)
        put(p.approvedBy ?: "")
        put(p.rejectionReason ?: "")
      }
      arr.put(row)
    }
    return arr
  }

  fun buildStudentsTable(students: List<Student>, courses: List<Course>): JSONArray {
    val arr = JSONArray()
    val header = JSONArray().apply {
      put("Student ID")
      put("Roll No")
      put("Full Name")
      put("Mobile Number")
      put("Aadhaar No")
      put("Email")
      put("Guardian Name")
      put("Guardian Phone")
      put("Residential Address")
      put("Course ID")
      put("Batch ID")
      put("Admission Date")
      put("Status")
      put("Custom Fee Override (₹)")
      put("Monthly Base Fee (₹)")
      put("Total Paid (₹)")
      put("Outstanding Due (₹)")
    }
    arr.put(header)
    for (s in students) {
      val c = courses.firstOrNull { it.id == s.courseId }
      val baseFee = s.customMonthlyFeeOverride ?: c?.monthlyFee ?: 400.0
      val row = JSONArray().apply {
        put(s.id)
        put(s.rollNo)
        put(s.name)
        put(s.mobile)
        put(s.aadhaarNo)
        put(s.email ?: "")
        put(s.guardianName)
        put(s.guardianPhone)
        put(s.address)
        put(s.courseId)
        put(s.batchId)
        put(s.admissionDate)
        put(s.status)
        put(s.customMonthlyFeeOverride ?: "")
        put(baseFee)
        put(s.totalPaidInSheet ?: 0.0)
        put(0.0)
      }
      arr.put(row)
    }
    return arr
  }

  fun buildDiscountsTable(discounts: List<Discount>): JSONArray {
    val arr = JSONArray()
    val header = JSONArray().apply {
      put("Discount ID")
      put("Discount Title")
      put("Discount Type (flat/percentage)")
      put("Value (₹ / %)")
      put("Scope (all/class/course/batch/student)")
      put("Target Academic Class")
      put("Target Course ID")
      put("Target Batch ID")
      put("Target Student ID")
      put("Applicable Months Type")
      put("Selected Months")
      put("Reason / Description")
      put("Active Status")
      put("Created At")
    }
    arr.put(header)
    for (d in discounts) {
      val row = JSONArray().apply {
        put(d.id)
        put(d.title)
        put(d.type)
        put(d.value)
        put(d.scope)
        put(d.academicClass ?: "")
        put(d.courseId ?: "")
        put(d.batchId ?: "")
        put(d.studentId ?: "")
        put(d.applicableMonthsType)
        put(d.selectedMonths?.joinToString(", ") ?: "")
        put(d.reason ?: "")
        put(if (d.active) "Active" else "Inactive")
        put("2026-07-01")
      }
      arr.put(row)
    }
    return arr
  }

  fun buildExpensesTable(expenses: List<Expense>): JSONArray {
    val arr = JSONArray()
    val header = JSONArray().apply {
      put("Expense ID")
      put("Category")
      put("Expense Title")
      put("Amount Paid (₹)")
      put("Payment Date")
      put("Paid To / Vendor")
      put("Payment Mode")
      put("Receipt Reference")
      put("Notes")
    }
    arr.put(header)
    for (e in expenses) {
      val row = JSONArray().apply {
        put(e.id)
        put(e.category)
        put(e.title)
        put(e.amount)
        put(e.expenseDate)
        put(e.approvedBy)
        put(e.paymentMode)
        put(e.id)
        put(e.remarks ?: "")
      }
      arr.put(row)
    }
    return arr
  }

  fun buildLiveClassesTable(sessions: List<LiveClassSession>): JSONArray {
    val arr = JSONArray()
    val header = JSONArray().apply {
      put("Session ID")
      put("Class Title")
      put("Academic Class")
      put("Subject")
      put("Course ID")
      put("Batch ID")
      put("Educator Name")
      put("Date")
      put("Start Time")
      put("End Time")
      put("Status")
      put("Platform")
      put("Meeting Link / Room ID")
      put("Recording Available")
      put("Created At")
    }
    arr.put(header)
    for (s in sessions) {
      val row = JSONArray().apply {
        put(s.id)
        put(s.title)
        put(s.academicClass ?: "Class 11")
        put(s.subject)
        put(s.courseId ?: "")
        put(s.batchId ?: "")
        put(s.educatorName)
        put(s.scheduledDate)
        put(s.startTime)
        put(s.endTime)
        put(if (s.isPostponed) "postponed" else s.status)
        put(s.platform)
        put(s.meetingUrl ?: "")
        put(if (s.isPostponed) (s.postponeReason ?: "Postponed by center administration") else "No")
        put("2026-07-01")
      }
      arr.put(row)
    }
    return arr
  }

  fun buildAdmissionsTable(admissions: List<AdmissionApplication>): JSONArray {
    val arr = JSONArray()
    val header = JSONArray().apply {
      put("Application ID")
      put("Student Name")
      put("Mobile Number")
      put("Aadhaar No")
      put("Email")
      put("Guardian Name")
      put("Guardian Phone")
      put("Target Course ID")
      put("Status")
      put("Applied Date")
      put("Initial Fee Status")
      put("Payment Mode")
      put("Transaction Ref / UPI")
      put("Staff Remarks")
      put("Rejection Reason")
      put("Address")
    }
    arr.put(header)
    for (a in admissions) {
      val row = JSONArray().apply {
        put(a.id)
        put(a.studentName)
        put(a.mobile)
        put(a.aadhaarNo)
        put(a.email ?: "")
        put(a.guardianName)
        put(a.guardianPhone)
        put(a.targetCourseId)
        put(a.status)
        put(a.appliedDate)
        put(a.initialPaymentStatus)
        put(a.initialPaymentMode)
        put(a.initialPaymentRef ?: "")
        put(a.remarks ?: "")
        put(a.rejectionReason ?: "")
        put(a.address)
      }
      arr.put(row)
    }
    return arr
  }

  fun buildExamsTable(exams: List<OnlineExam>): JSONArray {
    val arr = JSONArray()
    val header = JSONArray().apply {
      put("Exam ID")
      put("Exam Code")
      put("Title")
      put("Course ID")
      put("Batch ID")
      put("Academic Class")
      put("Subject")
      put("Duration (Mins)")
      put("Total Marks")
      put("Passing Marks")
      put("Passing %")
      put("Status")
      put("Mode")
      put("Scheduled Date")
    }
    arr.put(header)
    for (e in exams) {
      val row = JSONArray().apply {
        put(e.id)
        put(e.id)
        put(e.title)
        put(e.courseId ?: "")
        put(e.batchId ?: "")
        put("Class 11")
        put(e.subject)
        put(e.durationMinutes)
        put(e.totalMarks)
        put((e.totalMarks * 0.4).toInt())
        put("40%")
        put(e.status)
        put("Online CBT")
        put(e.scheduledDate)
      }
      arr.put(row)
    }
    return arr
  }

  fun buildSubmissionsTable(submissions: List<ExamSubmission>): JSONArray {
    val arr = JSONArray()
    val header = JSONArray().apply {
      put("Submission ID")
      put("Exam ID")
      put("Student ID")
      put("Student Name")
      put("Score")
      put("Total Marks")
      put("Submitted At")
    }
    arr.put(header)
    for (s in submissions) {
      val row = JSONArray().apply {
        put(s.id)
        put(s.examId)
        put(s.studentId)
        put(s.studentName)
        put(s.score)
        put(s.totalMarks)
        put(s.submittedAt)
      }
      arr.put(row)
    }
    return arr
  }

  private fun postJson(json: JSONObject): Result<Boolean> {
    return try {
      val mediaType = "application/json; charset=utf-8".toMediaType()
      val requestBody = json.toString().toRequestBody(mediaType)
      val request = Request.Builder()
        .url(WEBHOOK_URL)
        .header("User-Agent", "PixelPathsalaApp/1.0 (Android)")
        .post(requestBody)
        .build()

      val response = client.newCall(request).execute()
      val bodyStr = response.body?.string() ?: ""
      val ok = response.isSuccessful && !bodyStr.contains("\"error\"")
      Result.success(ok)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private fun parseWebhookData(obj: JSONObject): DatabaseDataDto {
    val centerInfo = parseCenterInfo(obj.optJSONArray("Center_Overview") ?: obj.optJSONArray("Center_Info"))
    val students = parseStudents(obj.optJSONArray("Students"))
    val (courses, batches) = parseCoursesAndBatches(obj.optJSONArray("Courses_Batches"))
    val payments = parsePayments(obj.optJSONArray("Fee_Transactions"))
    val discounts = parseDiscounts(obj.optJSONArray("Discounts"))
    val adminAccounts = parseAdminCredentials(obj.optJSONArray("Admin_Credentials"))
    val educators = parseEducators(obj.optJSONArray("Educators"))
    val admissions = parseAdmissions(obj.optJSONArray("Admissions"))
    val liveClasses = parseLiveClasses(obj.optJSONArray("Live_Classes"))
    val classRecordings = parseClassRecordings(obj.optJSONArray("Class_Recordings"))
    val studyMaterials = parseStudyMaterials(obj.optJSONArray("Study_Materials"))
    val exams = parseExams(obj.optJSONArray("Exams"))
    val questions = parseQuestions(obj.optJSONArray("Question_Bank"))
    val submissions = parseSubmissions(obj.optJSONArray("Exam_Submissions"))
    val expenses = parseExpenses(obj.optJSONArray("Expenses"))

    return DatabaseDataDto(
      centerInfo = centerInfo,
      students = students,
      courses = courses,
      batches = batches,
      educators = educators,
      payments = payments,
      admissions = admissions,
      discounts = discounts,
      liveClasses = liveClasses,
      classRecordings = classRecordings,
      studyMaterials = studyMaterials,
      exams = exams,
      questions = questions,
      examSubmissions = submissions,
      studentTelemetries = emptyList(),
      expenses = expenses,
      adminAccounts = adminAccounts
    )
  }

  private fun parseCenterInfo(arr: JSONArray?): CenterInfo {
    if (arr == null || arr.length() <= 1) return LocalDataStore.INITIAL_CENTER
    val map = mutableMapOf<String, String>()
    for (i in 1 until arr.length()) {
      val row = arr.optJSONArray(i) ?: continue
      val key = optCell(row, 0)
      val value = optCell(row, 1)
      if (key.isNotBlank()) {
        map[key] = value
      }
    }
    val name = map["Institute Name"]?.ifBlank { null } ?: "Pixel Pathsala"
    val tagline = map["Tagline / Motto"]?.ifBlank { null } ?: "Code your dream, decode the future"
    val address = listOfNotNull(
      map["Campus Address"]?.ifBlank { null },
      map["City & State"]?.ifBlank { null }
    ).joinToString(", ").ifBlank { "Raina, Purba Bardhaman, West Bengal" }
    val phone = map["Primary Helpline Phone"]?.ifBlank { null }
      ?: map["Office Desk Phone (Alternate)"]?.ifBlank { null }
      ?: "+91 7908457219"
    val email = map["Official Email"]?.ifBlank { null } ?: "pixelpathsala+edu@gmail.com"
    val upiId = map["UPI ID for Fees"]?.ifBlank { null } ?: "9775708722@apl"
    val upiName = map["UPI Payee Name"]?.ifBlank { null } ?: "Pixel Pathsala Center"
    val qrCodeUrl = map["Center Logo URL"]?.ifBlank { null }
    val websiteUrl = map["Facebook Page URL"]?.ifBlank { null } ?: "https://pixelpathsala.com"

    return CenterInfo(
      id = "center-1",
      name = name,
      tagLine = tagline,
      address = address,
      phone = phone,
      email = email,
      upiId = upiId,
      upiName = upiName,
      qrCodeUrl = qrCodeUrl,
      websiteUrl = websiteUrl
    )
  }

  private fun parseExpenses(arr: JSONArray?): List<Expense> {
    if (arr == null) return emptyList()
    val list = mutableListOf<Expense>()
    for (i in 1 until arr.length()) {
      val row = arr.optJSONArray(i) ?: continue
      val id = optCell(row, 0)
      val title = optCell(row, 1)
      if (id.isBlank() || title.isBlank()) continue
      val category = optCell(row, 2).ifBlank { "General" }
      val amount = optCell(row, 3).toDoubleOrNull() ?: 0.0
      val date = optCell(row, 4)
      val mode = optCell(row, 5).ifBlank { "CASH" }
      val approvedBy = optCell(row, 6).ifBlank { "Admin" }
      val remarks = optCell(row, 7)
      list.add(
        Expense(
          id = id,
          title = title,
          category = category,
          amount = amount,
          expenseDate = date,
          paymentMode = mode,
          approvedBy = approvedBy,
          remarks = remarks
        )
      )
    }
    return list
  }

  private fun optCell(row: JSONArray, index: Int): String {
    if (index >= row.length()) return ""
    val item = row.opt(index) ?: return ""
    return item.toString().trim()
  }

  private fun parseStudents(arr: JSONArray?): List<Student> {
    if (arr == null) return emptyList()
    val list = mutableListOf<Student>()
    for (i in 1 until arr.length()) { // skip header
      val row = arr.optJSONArray(i) ?: continue
      val id = optCell(row, 0)
      if (id.isBlank()) continue
      val rollNo = optCell(row, 1)
      val name = optCell(row, 2)
      val mobile = optCell(row, 3)
      val aadhaar = optCell(row, 4)
      val email = optCell(row, 5)
      val guardianName = optCell(row, 6)
      val guardianPhone = optCell(row, 7)
      val address = optCell(row, 8)
      val courseId = optCell(row, 9)
      val batchId = optCell(row, 10)
      val admissionDate = optCell(row, 11)
      val status = optCell(row, 12).ifBlank { "active" }
      val customFee = optCell(row, 13).toDoubleOrNull()
      val totalPaid = optCell(row, 15).toDoubleOrNull()

      val admMonth = FeeCalculator.parseIsoOrMonthToYearMonth(admissionDate) ?: "2026-07"

      list.add(
        Student(
          id = id,
          rollNo = rollNo,
          name = name,
          mobile = mobile,
          aadhaarNo = aadhaar,
          email = email,
          guardianName = guardianName,
          guardianPhone = guardianPhone,
          address = address,
          courseId = courseId,
          batchId = batchId,
          admissionDate = admissionDate,
          admissionMonth = admMonth,
          status = status,
          customMonthlyFeeOverride = customFee,
          totalPaidInSheet = totalPaid
        )
      )
    }
    return list
  }

  private fun parseCoursesAndBatches(arr: JSONArray?): Pair<List<Course>, List<Batch>> {
    if (arr == null) return Pair(emptyList(), emptyList())
    val courses = mutableListOf<Course>()
    val batches = mutableListOf<Batch>()

    for (i in 1 until arr.length()) {
      val row = arr.optJSONArray(i) ?: continue
      val type = optCell(row, 0).uppercase()
      val id = optCell(row, 1)
      if (id.isBlank()) continue
      val titleOrName = optCell(row, 2)

      if (type == "COURSE") {
        val code = optCell(row, 3)
        val academicClass = optCell(row, 4)
        val category = optCell(row, 5).ifBlank { "General" }
        val monthlyFee = optCell(row, 6).toDoubleOrNull() ?: 400.0
        val desc = optCell(row, 8)
        courses.add(
          Course(
            id = id,
            title = titleOrName,
            code = code,
            academicClass = academicClass,
            category = category,
            monthlyFee = monthlyFee,
            description = desc
          )
        )
      } else if (type == "BATCH") {
        val courseId = optCell(row, 3)
        val timings = optCell(row, 4).ifBlank { "09:00 AM - 11:00 AM" }
        val schedule = optCell(row, 5).ifBlank { "Sat, Sun" }
        val maxCap = optCell(row, 6).toIntOrNull() ?: 30
        val eduId = optCell(row, 7).ifBlank { "edu-1" }
        val mode = optCell(row, 9).ifBlank { "offline" }
        batches.add(
          Batch(
            id = id,
            name = titleOrName,
            courseId = courseId,
            timings = timings,
            scheduleDays = schedule,
            educatorId = eduId,
            mode = mode,
            maxCapacity = maxCap
          )
        )
      }
    }
    return Pair(courses, batches)
  }

  private fun parsePayments(arr: JSONArray?): List<FeePayment> {
    if (arr == null) return emptyList()
    val list = mutableListOf<FeePayment>()
    for (i in 1 until arr.length()) {
      val row = arr.optJSONArray(i) ?: continue
      val receiptNo = optCell(row, 0)
      val paymentId = optCell(row, 1)
      val studentId = optCell(row, 2)
      val studentName = optCell(row, 3)
      val amountPaid = optCell(row, 4).toDoubleOrNull() ?: 0.0
      val mode = optCell(row, 5).ifBlank { "CASH" }
      val monthsCoveredRaw = optCell(row, 6)
      val monthsList = monthsCoveredRaw.split(",").map { it.trim() }.filter { it.isNotBlank() }
      val txRef = optCell(row, 7)
      val paymentDate = optCell(row, 8)
      val status = optCell(row, 9).ifBlank { "approved" }
      val approvedBy = optCell(row, 10)
      val rejectionReason = optCell(row, 11)

      if (paymentId.isNotBlank() || receiptNo.isNotBlank()) {
        list.add(
          FeePayment(
            id = paymentId.ifBlank { "pay-${System.currentTimeMillis()}-$i" },
            receiptNo = receiptNo.ifBlank { "REC-AUTO-$i" },
            studentId = studentId,
            studentName = studentName,
            finalAmountPaid = amountPaid,
            paymentMode = mode,
            month = monthsCoveredRaw,
            monthsCovered = monthsList,
            transactionRef = txRef,
            paymentDate = paymentDate,
            status = status,
            approvedBy = approvedBy,
            rejectionReason = rejectionReason
          )
        )
      }
    }
    return list
  }

  private fun parseDiscounts(arr: JSONArray?): List<Discount> {
    if (arr == null) return emptyList()
    val list = mutableListOf<Discount>()
    for (i in 1 until arr.length()) {
      val row = arr.optJSONArray(i) ?: continue
      val id = optCell(row, 0)
      if (id.isBlank()) continue
      val title = optCell(row, 1)
      val type = optCell(row, 2).lowercase().ifBlank { "flat" }
      val value = optCell(row, 3).toDoubleOrNull() ?: 0.0
      val scope = optCell(row, 4).lowercase().ifBlank { "student" }
      val academicClass = optCell(row, 5).ifBlank { null }
      val courseId = optCell(row, 6).ifBlank { null }
      val batchId = optCell(row, 7).ifBlank { null }
      val studentId = optCell(row, 8).ifBlank { null }
      val monthsType = optCell(row, 9).lowercase().ifBlank { "all_months" }
      val selectedMonthsRaw = optCell(row, 10)
      val selectedMonths = if (selectedMonthsRaw.isNotBlank()) {
        selectedMonthsRaw.split(",").map { it.trim() }
      } else null
      val reason = optCell(row, 11)
      val activeStr = optCell(row, 12)
      val active = !activeStr.equals("inactive", ignoreCase = true)

      list.add(
        Discount(
          id = id,
          title = title,
          type = type,
          value = value,
          scope = scope,
          academicClass = academicClass,
          courseId = courseId,
          batchId = batchId,
          studentId = studentId,
          applicableMonthsType = monthsType,
          selectedMonths = selectedMonths,
          reason = reason,
          active = active
        )
      )
    }
    return list
  }

  private fun parseAdminCredentials(arr: JSONArray?): List<AdminAccount> {
    if (arr == null) return emptyList()
    val list = mutableListOf<AdminAccount>()
    for (i in 1 until arr.length()) {
      val row = arr.optJSONArray(i) ?: continue
      val id = optCell(row, 0)
      val username = optCell(row, 1)
      if (username.isBlank()) continue
      val fullName = optCell(row, 2)
      val role = optCell(row, 3)
      val email = optCell(row, 4)
      val pass = optCell(row, 5)
      val pin = optCell(row, 6)
      val status = optCell(row, 7).ifBlank { "active" }

      list.add(
        AdminAccount(
          id = id.ifBlank { "ADM-001" },
          username = username,
          fullName = fullName.ifBlank { "Administrator" },
          roleTitle = role.ifBlank { "Master Admin" },
          email = email.ifBlank { "admin@pixelpathsala.com" },
          passwordHash = pass,
          securityPin = pin,
          status = status
        )
      )
    }
    return list
  }

  private fun parseEducators(arr: JSONArray?): List<Educator> {
    if (arr == null) return emptyList()
    val list = mutableListOf<Educator>()
    for (i in 1 until arr.length()) {
      val row = arr.optJSONArray(i) ?: continue
      val id = optCell(row, 0)
      val name = optCell(row, 1)
      if (id.isBlank() || name.isBlank()) continue
      list.add(
        Educator(
          id = id,
          name = name,
          designation = optCell(row, 2).ifBlank { "Faculty Member" },
          subject = optCell(row, 3).ifBlank { "Computer Science" },
          phone = optCell(row, 4),
          email = optCell(row, 5),
          qualification = optCell(row, 6).ifBlank { "M.Sc." }
        )
      )
    }
    return list
  }

  private fun parseAdmissions(arr: JSONArray?): List<AdmissionApplication> {
    if (arr == null) return emptyList()
    val list = mutableListOf<AdmissionApplication>()
    for (i in 1 until arr.length()) {
      val row = arr.optJSONArray(i) ?: continue
      val id = optCell(row, 0)
      if (id.isBlank()) continue
      val studentName = optCell(row, 1)
      val mobile = optCell(row, 2)
      val aadhaar = optCell(row, 3)
      val email = optCell(row, 4)
      val guardianName = optCell(row, 5)
      val guardianPhone = optCell(row, 6)
      val targetCourseId = optCell(row, 7)
      val status = optCell(row, 8).ifBlank { "pending" }
      val appliedDate = optCell(row, 9)
      val initStatus = optCell(row, 10).ifBlank { "paid_advance" }
      val mode = optCell(row, 11).ifBlank { "CASH" }
      val txRef = optCell(row, 12)
      val remarks = optCell(row, 13)
      val rejReason = optCell(row, 14)
      val address = optCell(row, 15)

      list.add(
        AdmissionApplication(
          id = id,
          applicationNo = id,
          studentName = studentName,
          mobile = mobile,
          aadhaarNo = aadhaar,
          email = email.ifBlank { null },
          guardianName = guardianName,
          guardianPhone = guardianPhone.ifBlank { mobile },
          targetCourseId = targetCourseId,
          status = status,
          appliedDate = appliedDate,
          initialPaymentStatus = initStatus,
          initialPaymentMode = mode,
          initialPaymentRef = txRef,
          remarks = remarks,
          rejectionReason = rejReason,
          address = address
        )
      )
    }
    return list
  }

  private fun cleanDateOrTime(raw: String): String {
    if (raw.isBlank()) return raw
    if (raw.contains("T") && raw.endsWith("Z")) {
      if (raw.startsWith("1899-") || raw.startsWith("1900-")) {
        return try {
          val timePart = raw.substringAfter("T").substringBefore(".").substringBefore("Z")
          val parts = timePart.split(":")
          val h = parts[0].toIntOrNull() ?: 10
          val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
          val ampm = if (h >= 12) "PM" else "AM"
          val h12 = if (h % 12 == 0) 12 else if (h > 12) h - 12 else h
          String.format(java.util.Locale.US, "%02d:%02d %s", h12, m, ampm)
        } catch (_: Exception) { raw }
      } else {
        return raw.substringBefore("T")
      }
    }
    return raw
  }

  private fun parseLiveClasses(arr: JSONArray?): List<LiveClassSession> {
    if (arr == null) return emptyList()
    val list = mutableListOf<LiveClassSession>()
    for (i in 1 until arr.length()) {
      val row = arr.optJSONArray(i) ?: continue
      val id = optCell(row, 0)
      val title = optCell(row, 1)
      if (id.isBlank() || title.isBlank()) continue
      val rawStatus = optCell(row, 10).ifBlank { "scheduled" }
      val isPostp = rawStatus.equals("postponed", ignoreCase = true)
      val recOrNotice = optCell(row, 13)
      val postponeReason = if (isPostp) {
        if (recOrNotice.isNotBlank() && !recOrNotice.equals("No", ignoreCase = true) && !recOrNotice.equals("Yes", ignoreCase = true)) {
          recOrNotice
        } else {
          "Postponed by center administration"
        }
      } else null

      list.add(
        LiveClassSession(
          id = id,
          title = title,
          academicClass = optCell(row, 2),
          subject = optCell(row, 3).ifBlank { "Computer Science" },
          courseId = optCell(row, 4),
          batchId = optCell(row, 5),
          educatorName = optCell(row, 6).ifBlank { "Faculty" },
          scheduledDate = cleanDateOrTime(optCell(row, 7)),
          startTime = cleanDateOrTime(optCell(row, 8)).ifBlank { "10:00 AM" },
          endTime = cleanDateOrTime(optCell(row, 9)).ifBlank { "11:30 AM" },
          status = rawStatus,
          platform = optCell(row, 11).ifBlank { "Google Meet" },
          meetingUrl = optCell(row, 12).ifBlank { "https://meet.google.com/new" },
          isPostponed = isPostp,
          postponeReason = postponeReason
        )
      )
    }
    return list
  }

  private fun parseClassRecordings(arr: JSONArray?): List<ClassRecording> {
    if (arr == null) return emptyList()
    val list = mutableListOf<ClassRecording>()
    for (i in 1 until arr.length()) {
      val row = arr.optJSONArray(i) ?: continue
      val id = optCell(row, 0)
      val title = optCell(row, 1)
      if (id.isBlank() || title.isBlank()) continue
      val topic = optCell(row, 4)
      val chapter = optCell(row, 5)
      val desc = if (topic.isNotBlank() || chapter.isNotBlank()) "$topic • $chapter" else optCell(row, 13)
      list.add(
        ClassRecording(
          id = id,
          title = title,
          subject = optCell(row, 3).ifBlank { optCell(row, 2) },
          courseId = optCell(row, 6),
          batchId = optCell(row, 7),
          dateRecorded = cleanDateOrTime(optCell(row, 9)),
          durationMinutes = optCell(row, 10).toIntOrNull() ?: 60,
          recordingUrl = optCell(row, 12),
          description = desc
        )
      )
    }
    return list
  }

  private fun parseStudyMaterials(arr: JSONArray?): List<StudyMaterial> {
    if (arr == null) return emptyList()
    val list = mutableListOf<StudyMaterial>()
    for (i in 1 until arr.length()) {
      val row = arr.optJSONArray(i) ?: continue
      val id = optCell(row, 0)
      val title = optCell(row, 1)
      if (id.isBlank() || title.isBlank()) continue
      list.add(
        StudyMaterial(
          id = id,
          title = title,
          subject = optCell(row, 2),
          courseId = optCell(row, 3),
          batchId = optCell(row, 4),
          fileType = optCell(row, 5).ifBlank { "PDF" },
          fileUrl = optCell(row, 8),
          uploadedDate = cleanDateOrTime(optCell(row, 10)),
          description = optCell(row, 12)
        )
      )
    }
    return list
  }

  private fun parseExams(arr: JSONArray?): List<OnlineExam> {
    if (arr == null) return emptyList()
    val list = mutableListOf<OnlineExam>()
    for (i in 1 until arr.length()) {
      val row = arr.optJSONArray(i) ?: continue
      val id = optCell(row, 0)
      if (id.isBlank()) continue
      val title = optCell(row, 2).ifBlank { optCell(row, 1) }
      if (title.isBlank()) continue
      val courseId = optCell(row, 3).ifBlank { null }
      val batchId = optCell(row, 4).ifBlank { null }
      val subject = optCell(row, 6).ifBlank { optCell(row, 2).ifBlank { "General" } }
      val duration = (optCell(row, 7).ifBlank { optCell(row, 5) }).toIntOrNull() ?: 30
      val totalMarks = (optCell(row, 8).ifBlank { optCell(row, 4) }).toIntOrNull() ?: 20
      val status = optCell(row, 11).ifBlank { optCell(row, 7).ifBlank { "published" } }
      val rawDate = optCell(row, 13).ifBlank { optCell(row, 6) }
      val schedDate = cleanDateOrTime(rawDate)
      val qIds = optCell(row, 17).ifBlank { null }

      list.add(
        OnlineExam(
          id = id,
          title = title,
          subject = subject,
          courseId = courseId,
          batchId = batchId,
          totalMarks = totalMarks,
          durationMinutes = duration,
          scheduledDate = schedDate,
          status = status,
          questionIds = qIds
        )
      )
    }
    return list
  }

  private fun parseQuestions(arr: JSONArray?): List<ExamQuestion> {
    if (arr == null) return emptyList()
    val list = mutableListOf<ExamQuestion>()
    for (i in 1 until arr.length()) {
      val row = arr.optJSONArray(i) ?: continue
      val id = optCell(row, 0)
      if (id.isBlank()) continue

      // Support both Google Sheets Question_Bank format:
      // Index 0: Question ID, 1: Course ID, 2: Academic Class, 3: Subject, 4: Topic, 5: Marks,
      // 7: Question Text, 8: Option A, 9: Option B, 10: Option C, 11: Option D, 12: Correct Option Index
      // and legacy format (Index 1: Exam ID, 2: Question Text, 3..6: Options, 7: Correct, 8: Marks)
      val isQuestionBankSchema = row.length() >= 12 && (optCell(row, 7).isNotBlank() || optCell(row, 8).isNotBlank())

      val courseId: String?
      val examId: String
      val qText: String
      val optA: String
      val optB: String
      val optC: String
      val optD: String
      val correctOpt: String
      val marks: Int

      if (isQuestionBankSchema) {
        val courseVal = optCell(row, 1)
        courseId = courseVal.ifBlank { null }
        examId = optCell(row, 14).ifBlank { optCell(row, 4) }
        marks = optCell(row, 5).toIntOrNull() ?: 2
        val rawText = optCell(row, 7)
        val snippet = optCell(row, 13)
        qText = if (snippet.isNotBlank()) "$rawText\n\n$snippet" else rawText
        optA = optCell(row, 8)
        optB = optCell(row, 9)
        optC = optCell(row, 10)
        optD = optCell(row, 11)
        val rawCorrect = optCell(row, 12).trim()
        correctOpt = when (rawCorrect) {
          "0" -> "A"
          "1" -> "B"
          "2" -> "C"
          "3" -> "D"
          else -> if (rawCorrect.isNotBlank()) rawCorrect.uppercase() else "A"
        }
      } else {
        courseId = null
        examId = optCell(row, 1)
        qText = optCell(row, 2)
        optA = optCell(row, 3)
        optB = optCell(row, 4)
        optC = optCell(row, 5)
        optD = optCell(row, 6)
        correctOpt = optCell(row, 7).ifBlank { "A" }.uppercase()
        marks = optCell(row, 8).toIntOrNull() ?: 1
      }

      if (qText.isBlank()) continue
      list.add(
        ExamQuestion(
          id = id,
          examId = examId,
          questionText = qText,
          optionA = optA,
          optionB = optB,
          optionC = optC,
          optionD = optD,
          correctOption = correctOpt,
          marks = marks,
          courseId = courseId
        )
      )
    }
    return list
  }

  private fun parseSubmissions(arr: JSONArray?): List<ExamSubmission> {
    if (arr == null) return emptyList()
    val list = mutableListOf<ExamSubmission>()
    for (i in 1 until arr.length()) {
      val row = arr.optJSONArray(i) ?: continue
      val id = optCell(row, 0)
      if (id.isBlank()) continue
      list.add(
        ExamSubmission(
          id = id,
          examId = optCell(row, 1),
          studentId = optCell(row, 2),
          studentName = optCell(row, 3),
          score = optCell(row, 4).toIntOrNull() ?: 0,
          totalMarks = optCell(row, 5).toIntOrNull() ?: 20,
          submittedAt = optCell(row, 6)
        )
      )
    }
    return list
  }
}
