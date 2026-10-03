package com.example.data.sheet

import android.util.Log
import com.example.data.model.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.StringReader
import java.util.Locale
import java.util.concurrent.TimeUnit

object GoogleSheetSyncService {
  private const val TAG = "GoogleSheetSyncService"

  const val SPREADSHEET_ID = "1HVB5lNLyCIh6Uw0KmSAvaUnlQjfJMroHzAKy1a-4_O4"
  const val WEBHOOK_URL = "https://script.google.com/macros/s/AKfycbz_CEP9stVACI0-6oOiRbJvvfAjn9X1xIVW-m26B2E9Dp4e_aNiyAavFe1jZFAS1jls/exec"

  // GIDs for direct CSV fallback
  val GIDS = mapOf(
    "Center_Overview" to "25117137",
    "Admin_Credentials" to "371655954",
    "Students" to "1572610885",
    "Courses_Batches" to "563739523",
    "Fee_Transactions" to "154871224",
    "Admissions" to "120081180",
    "Educators" to "1520472103",
    "Live_Classes" to "1629684060",
    "Class_Recordings" to "863738719",
    "Study_Materials" to "161082531",
    "Exams" to "1954552249",
    "Question_Bank" to "1111711455",
    "Exam_Submissions" to "1708781557"
  )

  private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(45, TimeUnit.SECONDS)
    .writeTimeout(45, TimeUnit.SECONDS)
    .followRedirects(true)
    .followSslRedirects(true)
    .build()

  /**
   * Fetches full database from Google Sheet (via Webhook or direct CSV fallback)
   */
  fun fetchDatabaseFromSheet(): DatabaseDataDto? {
    // 1. Try Webhook first
    try {
      val request = Request.Builder()
        .url(WEBHOOK_URL)
        .header("User-Agent", "Mozilla/5.0 PixelPathsala/1.0")
        .build()

      val response = client.newCall(request).execute()
      if (response.isSuccessful) {
        val bodyStr = response.body?.string()
        if (!bodyStr.isNullOrBlank() && bodyStr.trim().startsWith("{")) {
          val json = JSONObject(bodyStr)
          val dataObj = json.optJSONObject("data")
          if (dataObj != null) {
            val dto = parseWebhookData(dataObj)
            if (dto.students?.isNotEmpty() == true || dto.courses?.isNotEmpty() == true) {
              Log.d(TAG, "Successfully synced from Google Sheet Webhook!")
              return dto
            }
          }
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Webhook sync failed, trying direct Google Sheet CSV fallback: ${e.message}")
    }

    // 2. Direct Google Sheet CSV Fallback
    try {
      return fetchViaDirectCsv()
    } catch (e: Exception) {
      Log.e(TAG, "Direct CSV fallback failed: ${e.message}", e)
    }

    return null
  }

  private fun parseWebhookData(dataObj: JSONObject): DatabaseDataDto {
    val admissions = parseAdmissionsTable(dataObj.optJSONArray("Admissions"))
    val students = parseStudentsTable(dataObj.optJSONArray("Students"), admissions)
    val (courses, batches) = parseCoursesBatchesTable(dataObj.optJSONArray("Courses_Batches"))
    val payments = parsePaymentsTable(dataObj.optJSONArray("Fee_Transactions"))
    val educators = parseEducatorsTable(dataObj.optJSONArray("Educators"))
    val centerInfo = parseCenterOverviewTable(dataObj.optJSONArray("Center_Overview"))
    val adminAccounts = parseAdminsTable(dataObj.optJSONArray("Admin_Credentials"))
    val liveClasses = parseLiveClassesTable(dataObj.optJSONArray("Live_Classes"))
    val classRecordings = parseClassRecordingsTable(dataObj.optJSONArray("Class_Recordings"))
    val studyMaterials = parseStudyMaterialsTable(dataObj.optJSONArray("Study_Materials"))
    val exams = parseExamsTable(dataObj.optJSONArray("Exams"))
    val questions = parseQuestionsTable(dataObj.optJSONArray("Question_Bank"))
    val submissions = parseSubmissionsTable(dataObj.optJSONArray("Exam_Submissions"))

    return DatabaseDataDto(
      centerInfo = centerInfo,
      students = students,
      courses = courses,
      batches = batches,
      payments = payments,
      admissions = admissions,
      educators = educators,
      adminAccounts = adminAccounts,
      liveClasses = liveClasses,
      classRecordings = classRecordings,
      studyMaterials = studyMaterials,
      exams = exams,
      questions = questions,
      examSubmissions = submissions
    )
  }

  private fun parseStudentsTable(
    array: JSONArray?,
    admissionsList: List<AdmissionApplication>? = null
  ): List<Student> {
    val list = mutableListOf<Student>()
    if (array == null || array.length() <= 1) return list

    // Check header indices if available in row 0
    val headerRow = array.optJSONArray(0)
    var customFeeCol = 13
    var discountCol = -1
    var admDateCol = 11
    var baseFeeCol = 14
    var totalPaidCol = 15

    if (headerRow != null) {
      for (c in 0 until headerRow.length()) {
        val h = headerRow.optString(c, "").lowercase().trim()
        if (h.contains("discount") || h.contains("concession")) discountCol = c
        if (h.contains("custom") || h.contains("override")) customFeeCol = c
        if (h.contains("admission") || h.contains("joining")) admDateCol = c
        if (h.contains("base fee") || h.contains("monthly base")) baseFeeCol = c
        if (h.contains("total paid") || h.contains("amount paid")) totalPaidCol = c
      }
    }

    for (i in 1 until array.length()) {
      val row = array.optJSONArray(i) ?: continue
      val id = optCell(row, 0).ifBlank { "stu-$i" }
      val rollNo = optCell(row, 1).ifBlank { "PP-2026-$i" }
      val name = optCell(row, 2).ifBlank { "Student $i" }
      val mobile = optCell(row, 3)
      val aadhaar = optCell(row, 4)
      val email = optCell(row, 5)
      val guardianName = optCell(row, 6)
      val guardianPhone = optCell(row, 7)
      val address = optCell(row, 8)
      val courseId = optCell(row, 9)
      val batchId = optCell(row, 10)
      val rawDate = optCell(row, admDateCol)
      var admissionDate = if (rawDate.contains("T")) rawDate.substringBefore("T") else rawDate.take(10).ifBlank { "2026-08-01" }

      // Check if admissions table has an earlier applied date for this student
      if (admissionsList != null) {
        val matchingApp = admissionsList.find { app ->
          (app.studentName.isNotBlank() && app.studentName.equals(name, ignoreCase = true)) ||
          (app.mobile.isNotBlank() && app.mobile == mobile) ||
          (app.aadhaarNo.isNotBlank() && app.aadhaarNo == aadhaar)
        }
        if (matchingApp != null && matchingApp.appliedDate.isNotBlank()) {
          val appDate = if (matchingApp.appliedDate.contains("T")) matchingApp.appliedDate.substringBefore("T") else matchingApp.appliedDate.take(10)
          // If student date is late October or blank or after applied date, prefer applied date
          if (admissionDate.startsWith("2026-10") && appDate.startsWith("2026-08")) {
            admissionDate = appDate
          }
        }
      }

      val admissionMonth = admissionDate.take(7).ifBlank { "2026-08" }
      val status = optCell(row, 12).ifBlank { "active" }
      val customFee = optCell(row, customFeeCol).replace("₹", "").replace(",", "").trim().toDoubleOrNull()
      val baseFee = if (baseFeeCol != -1) optCell(row, baseFeeCol).replace("₹", "").replace(",", "").trim().toDoubleOrNull() else null
      val totalPaidInSheet = if (totalPaidCol != -1) optCell(row, totalPaidCol).replace("₹", "").replace(",", "").trim().toDoubleOrNull() else null

      val explicitDiscount = if (discountCol != -1) {
        optCell(row, discountCol).replace("₹", "").replace(",", "").trim().toDoubleOrNull()
      } else null

      val standardFeeVal = baseFee ?: 400.0
      val monthlyDiscount = explicitDiscount
        ?: if (customFee != null && customFee > 0 && customFee < standardFeeVal) {
          if (customFee <= standardFeeVal / 2) customFee else (standardFeeVal - customFee)
        } else null

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
          admissionMonth = admissionMonth,
          status = status,
          customMonthlyFeeOverride = customFee,
          monthlyDiscount = monthlyDiscount,
          totalPaidInSheet = totalPaidInSheet
        )
      )
    }
    return list
  }

  private fun parseCoursesBatchesTable(array: JSONArray?): Pair<List<Course>, List<Batch>> {
    val courses = mutableListOf<Course>()
    val batches = mutableListOf<Batch>()
    if (array == null || array.length() <= 1) return Pair(courses, batches)

    for (i in 1 until array.length()) {
      val row = array.optJSONArray(i) ?: continue
      val recordType = optCell(row, 0).uppercase()
      val id = optCell(row, 1)

      if (recordType == "COURSE") {
        val title = optCell(row, 2)
        val code = optCell(row, 3)
        val academicClass = optCell(row, 4)
        val category = optCell(row, 5)
        val fee = optCell(row, 6).toDoubleOrNull() ?: 400.0
        val isPopular = optCell(row, 7).contains("pop", ignoreCase = true) || fee > 0
        val description = optCell(row, 8)

        courses.add(
          Course(
            id = id,
            title = title,
            code = code,
            academicClass = academicClass,
            category = category,
            monthlyFee = fee,
            description = description,
            isPopular = isPopular
          )
        )
      } else if (recordType == "BATCH") {
        val name = optCell(row, 2)
        val courseId = optCell(row, 3)
        val timings = optCell(row, 4)
        val scheduleDays = optCell(row, 5)
        val maxCapacity = optCell(row, 6).toIntOrNull() ?: 30
        val educatorId = optCell(row, 7)
        val room = optCell(row, 8)
        val mode = optCell(row, 9).ifBlank { "offline" }

        batches.add(
          Batch(
            id = id,
            name = name,
            courseId = courseId,
            timing = timings,
            scheduleDays = if (scheduleDays.isNotBlank()) scheduleDays.split(",").map { it.trim() } else listOf("Sat", "Sun"),
            maxCapacity = maxCapacity,
            educatorId = educatorId,
            roomNumber = room.ifBlank { "Room 101" },
            mode = mode
          )
        )
      }
    }
    return Pair(courses, batches)
  }

  private fun parsePaymentsTable(array: JSONArray?): List<FeePayment> {
    val list = mutableListOf<FeePayment>()
    if (array == null || array.length() <= 1) return list

    for (i in 1 until array.length()) {
      val row = array.optJSONArray(i) ?: continue
      val receiptNo = optCell(row, 0)
      val paymentId = optCell(row, 1).ifBlank { receiptNo.ifBlank { "pay-$i" } }
      val studentId = optCell(row, 2)
      val studentName = optCell(row, 3)
      val amountStr = optCell(row, 4).replace("₹", "").replace(",", "").trim()
      val amount = amountStr.toDoubleOrNull() ?: 0.0
      val mode = optCell(row, 5).ifBlank { "CASH" }
      val monthsRaw = optCell(row, 6).ifBlank { "Current Session" }
      val monthsList = if (monthsRaw.contains(",")) monthsRaw.split(",").map { it.trim() } else listOf(monthsRaw)
      val month = monthsList.firstOrNull() ?: monthsRaw
      val ref = optCell(row, 7)
      val date = optCell(row, 8).ifBlank { "2026-10-01" }
      val rawStatus = optCell(row, 9).trim().lowercase(Locale.ROOT)
      val status = if (rawStatus.contains("approve")) "approved" else if (rawStatus.contains("reject")) "rejected" else if (rawStatus.contains("pending")) "pending" else "approved"
      val approvedBy = optCell(row, 10).ifBlank { "Admin" }
      val remarks = optCell(row, 11)

      if (receiptNo.isBlank() && studentName.isBlank() && amount == 0.0) continue

      list.add(
        FeePayment(
          id = paymentId,
          receiptNo = receiptNo.ifBlank { "REC-AUTO-$i" },
          studentId = studentId,
          studentName = studentName,
          baseMonthlyFee = amount,
          totalBaseFee = amount,
          finalAmountPaid = amount,
          paymentMode = mode,
          month = month,
          monthsCovered = monthsList,
          transactionRef = ref,
          paymentDate = date,
          status = status,
          approvedBy = approvedBy,
          remarks = remarks
        )
      )
    }
    return list
  }

  private fun parseAdmissionsTable(array: JSONArray?): List<AdmissionApplication> {
    val list = mutableListOf<AdmissionApplication>()
    if (array == null || array.length() <= 1) return list

    for (i in 1 until array.length()) {
      val row = array.optJSONArray(i) ?: continue
      val id = optCell(row, 0).ifBlank { "adm-$i" }
      val name = optCell(row, 1)
      val mobile = optCell(row, 2)
      val aadhaar = optCell(row, 3)
      val email = optCell(row, 4)
      val guardianName = optCell(row, 5)
      val guardianPhone = optCell(row, 6)
      val courseId = optCell(row, 7)
      val status = optCell(row, 8).ifBlank { "pending" }
      val appliedDate = optCell(row, 9).take(10)
      val initialFeeStatus = optCell(row, 10).ifBlank { "paid_advance" }
      val paymentMode = optCell(row, 11).ifBlank { "UPI" }
      val paymentRef = optCell(row, 12).ifBlank { null }
      val remarks = optCell(row, 13).ifBlank { null }
      val rejReason = optCell(row, 14).ifBlank { null }
      val address = optCell(row, 15)

      list.add(
        AdmissionApplication(
          id = id,
          applicationNo = id,
          studentName = name,
          mobile = mobile,
          aadhaarNo = aadhaar,
          email = email,
          guardianName = guardianName,
          guardianPhone = guardianPhone,
          targetCourseId = courseId,
          status = status,
          appliedDate = appliedDate,
          initialPaymentStatus = initialFeeStatus,
          initialPaymentMode = paymentMode,
          initialPaymentRef = paymentRef,
          remarks = remarks,
          rejectionReason = rejReason,
          address = address
        )
      )
    }
    return list
  }

  private fun parseEducatorsTable(array: JSONArray?): List<Educator> {
    val list = mutableListOf<Educator>()
    if (array == null || array.length() <= 1) return list

    for (i in 1 until array.length()) {
      val row = array.optJSONArray(i) ?: continue
      val id = optCell(row, 0).ifBlank { "edu-$i" }
      val name = optCell(row, 1)
      val qualification = optCell(row, 2)
      val subject = optCell(row, 3)
      val phone = optCell(row, 4)
      val email = optCell(row, 5)
      val salary = optCell(row, 6).toDoubleOrNull() ?: 25000.0
      val exp = optCell(row, 7).toIntOrNull() ?: 10
      val status = optCell(row, 8).ifBlank { "active" }
      val photoUrl = optCell(row, 10)

      list.add(
        Educator(
          id = id,
          name = name,
          qualification = qualification,
          subject = subject,
          phone = phone,
          email = email,
          monthlySalary = salary,
          experienceYears = exp,
          status = status,
          photoUrl = photoUrl
        )
      )
    }
    return list
  }

  private fun parseCenterOverviewTable(array: JSONArray?): CenterInfo {
    var name = "Pixel Pathsala"
    var tagline = "Code your dream, decode the future"
    var address = "Raina"
    var cityState = "Purba Bardhaman, West Bengal"
    var phone = "+91 7908457219"
    var whatsapp = "+91 9775708722"
    var email = "pixelpathsala+edu@gmail.com"
    var upiId = "9775708722@apl"
    var upiName = "Pixel Pathsala Center"
    var announcement = "Admissions Open for Session 2026-27!"

    if (array != null && array.length() > 1) {
      for (i in 1 until array.length()) {
        val row = array.optJSONArray(i) ?: continue
        val key = optCell(row, 0).lowercase()
        val value = optCell(row, 1)

        when {
          key.contains("institute name") -> name = value
          key.contains("tagline") -> tagline = value
          key.contains("campus address") -> address = value
          key.contains("city") -> cityState = value
          key.contains("primary helpline") -> phone = value
          key.contains("whatsapp") -> whatsapp = value
          key.contains("official email") -> email = value
          key.contains("upi id") -> upiId = value
          key.contains("upi payee") -> upiName = value
          key.contains("announcement") -> announcement = value
        }
      }
    }

    return CenterInfo(
      name = name,
      tagline = tagline,
      address = address,
      cityState = cityState,
      phonePrimary = phone,
      whatsapp = whatsapp,
      email = email,
      upiId = upiId,
      upiName = upiName,
      announcement = announcement
    )
  }

  private fun parseAdminsTable(array: JSONArray?): List<AdminAccount> {
    val list = mutableListOf<AdminAccount>()
    if (array == null || array.length() <= 1) return list

    for (i in 1 until array.length()) {
      val row = array.optJSONArray(i) ?: continue
      val id = optCell(row, 0).ifBlank { "ADM-001" }
      val username = optCell(row, 1).ifBlank { "admin" }
      val name = optCell(row, 2).ifBlank { "Director / Center Administrator" }
      val role = optCell(row, 3).ifBlank { "Master Administrator" }
      val email = optCell(row, 4).ifBlank { "admin@pixelpathsala.com" }
      val password = optCell(row, 5).ifBlank { "admin123" }
      val pin = optCell(row, 6).ifBlank { "7722" }

      list.add(
        AdminAccount(
          id = id,
          username = username,
          name = name,
          role = role,
          email = email,
          password = password,
          securityPin = pin
        )
      )
    }
    return list
  }

  private fun fetchViaDirectCsv(): DatabaseDataDto {
    val admissions = fetchCsvRows("Admissions", GIDS["Admissions"] ?: "120081180")?.let { rows ->
      val arr = JSONArray()
      rows.forEach { r -> arr.put(JSONArray(r)) }
      parseAdmissionsTable(arr)
    } ?: emptyList()

    val students = fetchCsvRows("Students", GIDS["Students"] ?: "1572610885")?.let { rows ->
      val arr = JSONArray()
      rows.forEach { r -> arr.put(JSONArray(r)) }
      parseStudentsTable(arr, admissions)
    } ?: emptyList()

    val (courses, batches) = fetchCsvRows("Courses_Batches", GIDS["Courses_Batches"] ?: "563739523")?.let { rows ->
      val arr = JSONArray()
      rows.forEach { r -> arr.put(JSONArray(r)) }
      parseCoursesBatchesTable(arr)
    } ?: Pair(emptyList(), emptyList())

    val payments = fetchCsvRows("Fee_Transactions", GIDS["Fee_Transactions"] ?: "154871224")?.let { rows ->
      val arr = JSONArray()
      rows.forEach { r -> arr.put(JSONArray(r)) }
      parsePaymentsTable(arr)
    } ?: emptyList()

    val educators = fetchCsvRows("Educators", GIDS["Educators"] ?: "1520472103")?.let { rows ->
      val arr = JSONArray()
      rows.forEach { r -> arr.put(JSONArray(r)) }
      parseEducatorsTable(arr)
    } ?: emptyList()

    val centerInfo = fetchCsvRows("Center_Overview", GIDS["Center_Overview"] ?: "25117137")?.let { rows ->
      val arr = JSONArray()
      rows.forEach { r -> arr.put(JSONArray(r)) }
      parseCenterOverviewTable(arr)
    }

    val admins = fetchCsvRows("Admin_Credentials", GIDS["Admin_Credentials"] ?: "371655954")?.let { rows ->
      val arr = JSONArray()
      rows.forEach { r -> arr.put(JSONArray(r)) }
      parseAdminsTable(arr)
    } ?: emptyList()

    val liveClasses = fetchCsvRows("Live_Classes", GIDS["Live_Classes"] ?: "1629684060")?.let { rows ->
      val arr = JSONArray()
      rows.forEach { r -> arr.put(JSONArray(r)) }
      parseLiveClassesTable(arr)
    } ?: emptyList()

    val recordings = fetchCsvRows("Class_Recordings", GIDS["Class_Recordings"] ?: "863738719")?.let { rows ->
      val arr = JSONArray()
      rows.forEach { r -> arr.put(JSONArray(r)) }
      parseClassRecordingsTable(arr)
    } ?: emptyList()

    val studyMaterials = fetchCsvRows("Study_Materials", GIDS["Study_Materials"] ?: "161082531")?.let { rows ->
      val arr = JSONArray()
      rows.forEach { r -> arr.put(JSONArray(r)) }
      parseStudyMaterialsTable(arr)
    } ?: emptyList()

    val exams = fetchCsvRows("Exams", GIDS["Exams"] ?: "1954552249")?.let { rows ->
      val arr = JSONArray()
      rows.forEach { r -> arr.put(JSONArray(r)) }
      parseExamsTable(arr)
    } ?: emptyList()

    val questions = fetchCsvRows("Question_Bank", GIDS["Question_Bank"] ?: "1111711455")?.let { rows ->
      val arr = JSONArray()
      rows.forEach { r -> arr.put(JSONArray(r)) }
      parseQuestionsTable(arr)
    } ?: emptyList()

    val submissions = fetchCsvRows("Exam_Submissions", GIDS["Exam_Submissions"] ?: "1708781557")?.let { rows ->
      val arr = JSONArray()
      rows.forEach { r -> arr.put(JSONArray(r)) }
      parseSubmissionsTable(arr)
    } ?: emptyList()

    return DatabaseDataDto(
      centerInfo = centerInfo,
      students = students,
      courses = courses,
      batches = batches,
      payments = payments,
      admissions = admissions,
      educators = educators,
      adminAccounts = admins,
      liveClasses = liveClasses,
      classRecordings = recordings,
      studyMaterials = studyMaterials,
      exams = exams,
      questions = questions,
      examSubmissions = submissions
    )
  }

  private fun parseLiveClassesTable(array: JSONArray?): List<LiveClassSession> {
    val list = mutableListOf<LiveClassSession>()
    if (array == null || array.length() <= 1) return list

    for (i in 1 until array.length()) {
      val row = array.optJSONArray(i) ?: continue
      val id = optCell(row, 0).ifBlank { "live-$i" }
      val title = optCell(row, 1)
      if (title.isBlank()) continue
      val academicClass = optCell(row, 2).ifBlank { "All Classes" }
      val subject = optCell(row, 3).ifBlank { "General" }
      val courseId = optCell(row, 4).ifBlank { "all" }
      val batchId = optCell(row, 5).ifBlank { "all" }
      val educator = optCell(row, 6).ifBlank { "Faculty Desk" }
      val date = optCell(row, 7).ifBlank { "2026-09-29" }
      val startTime = optCell(row, 8).ifBlank { "10:00 AM" }
      val endTime = optCell(row, 9).ifBlank { "11:30 AM" }
      val status = optCell(row, 10).ifBlank { "scheduled" }
      val platform = optCell(row, 11).ifBlank { "google_meet" }
      val meetingUrl = optCell(row, 12).ifBlank { "https://meet.google.com/new" }
      val recordingAvailable = optCell(row, 13).contains("yes", ignoreCase = true)

      list.add(
        LiveClassSession(
          id = id,
          title = title,
          academicClass = academicClass,
          targetClass = academicClass,
          subject = subject,
          courseId = courseId,
          batchId = batchId,
          educatorName = educator,
          scheduledDate = date,
          startTime = startTime,
          endTime = endTime,
          status = status,
          platform = platform,
          meetingUrl = meetingUrl,
          isRecordingAvailable = recordingAvailable
        )
      )
    }
    return list
  }

  private fun parseClassRecordingsTable(array: JSONArray?): List<ClassRecording> {
    val list = mutableListOf<ClassRecording>()
    if (array == null || array.length() <= 1) return list

    for (i in 1 until array.length()) {
      val row = array.optJSONArray(i) ?: continue
      val id = optCell(row, 0).ifBlank { "rec-$i" }
      val title = optCell(row, 1)
      if (title.isBlank()) continue
      val academicClass = optCell(row, 2).ifBlank { "Class 10 (Secondary)" }
      val subject = optCell(row, 3).ifBlank { "Computer Science" }
      val topic = optCell(row, 4)
      val chapter = optCell(row, 5)
      val courseId = optCell(row, 6).ifBlank { "all" }
      val batchId = optCell(row, 7).ifBlank { "all" }
      val educator = optCell(row, 8).ifBlank { "Faculty Desk" }
      val date = optCell(row, 9)
      val duration = optCell(row, 10).toIntOrNull() ?: 60
      val sourceType = optCell(row, 11).ifBlank { "youtube" }
      val videoUrl = optCell(row, 12).ifBlank { "https://www.youtube.com" }
      val notesPdf = optCell(row, 13).ifBlank { null }
      val views = optCell(row, 14).toIntOrNull() ?: 15

      list.add(
        ClassRecording(
          id = id,
          title = title,
          academicClass = academicClass,
          targetClass = academicClass,
          subject = subject,
          topic = topic,
          chapter = chapter,
          courseId = courseId,
          batchId = batchId,
          educatorName = educator,
          recordedDate = date,
          durationMinutes = duration,
          videoSourceType = sourceType,
          videoUrl = videoUrl,
          notesPdfUrl = notesPdf,
          viewCount = views
        )
      )
    }
    return list
  }

  private fun parseStudyMaterialsTable(array: JSONArray?): List<StudyMaterial> {
    val list = mutableListOf<StudyMaterial>()
    if (array == null || array.length() <= 1) return list

    for (i in 1 until array.length()) {
      val row = array.optJSONArray(i) ?: continue
      val id = optCell(row, 0).ifBlank { "mat-$i" }
      val title = optCell(row, 1)
      if (title.isBlank()) continue
      val subject = optCell(row, 2).ifBlank { "General" }
      val courseId = optCell(row, 3).ifBlank { "all" }
      val batchId = optCell(row, 4).ifBlank { "all" }
      val fileType = optCell(row, 5).ifBlank { "pdf" }
      val fileName = optCell(row, 6).ifBlank { title }
      val fileSize = optCell(row, 7).ifBlank { "Cloud File" }
      val fileUrl = optCell(row, 8)
      val uploadedBy = optCell(row, 9).ifBlank { "Faculty Desk" }
      val uploadedDate = optCell(row, 10).ifBlank { "2026-09-29" }
      val downloadCount = optCell(row, 11).toIntOrNull() ?: 0
      val description = optCell(row, 12)

      list.add(
        StudyMaterial(
          id = id,
          title = title,
          subject = subject,
          courseId = courseId,
          batchId = batchId,
          fileType = fileType,
          fileName = fileName,
          fileSize = fileSize,
          fileUrl = fileUrl,
          uploadedBy = uploadedBy,
          uploadedDate = uploadedDate,
          downloadCount = downloadCount,
          description = description
        )
      )
    }
    return list
  }

  private fun parseExamsTable(array: JSONArray?): List<OnlineExam> {
    val list = mutableListOf<OnlineExam>()
    if (array == null || array.length() <= 1) return list

    for (i in 1 until array.length()) {
      val row = array.optJSONArray(i) ?: continue
      val id = optCell(row, 0).ifBlank { "exam-$i" }
      val code = optCell(row, 1).ifBlank { "EX-$i" }
      val title = optCell(row, 2)
      if (title.isBlank()) continue
      val courseId = optCell(row, 3).ifBlank { "all" }
      val batchId = optCell(row, 4).ifBlank { "all" }
      val academicClass = optCell(row, 5).ifBlank { "Class 10" }
      val subject = optCell(row, 6).ifBlank { "General" }
      val duration = optCell(row, 7).toIntOrNull() ?: 30
      val totalMarks = optCell(row, 8).toIntOrNull() ?: 20
      val passingMarks = optCell(row, 9).toIntOrNull() ?: 8
      val passingPct = optCell(row, 10).toIntOrNull() ?: 40
      val status = optCell(row, 11).ifBlank { "active" }
      val mode = optCell(row, 12).ifBlank { "online" }
      val scheduledDate = optCell(row, 13).ifBlank { "2026-09-29" }
      val startTime = optCell(row, 14).ifBlank { "09:00 AM" }
      val endTime = optCell(row, 15).ifBlank { "09:00 PM" }
      val questionIdsStr = optCell(row, 17)
      val questionIds = if (questionIdsStr.isNotBlank()) questionIdsStr.split(",").map { it.trim() } else emptyList()
      val instructions = optCell(row, 18).ifBlank { "Read each question carefully." }
      val isPublished = optCell(row, 19).isBlank() || optCell(row, 19).contains("yes", ignoreCase = true) || optCell(row, 19) == "true"

      list.add(
        OnlineExam(
          id = id,
          examCode = code,
          title = title,
          courseId = courseId,
          batchId = batchId,
          academicClass = academicClass,
          subject = subject,
          durationMinutes = duration,
          totalMarks = totalMarks,
          passingMarks = passingMarks,
          passingPercentage = passingPct,
          status = status,
          mode = mode,
          scheduledDate = scheduledDate,
          startTime = startTime,
          endTime = endTime,
          questionIds = questionIds,
          instructions = instructions,
          isPublished = isPublished
        )
      )
    }
    return list
  }

  private fun parseQuestionsTable(array: JSONArray?): List<ExamQuestion> {
    val list = mutableListOf<ExamQuestion>()
    if (array == null || array.length() <= 1) return list

    for (i in 1 until array.length()) {
      val row = array.optJSONArray(i) ?: continue
      val id = optCell(row, 0).ifBlank { "q-$i" }
      val courseId = optCell(row, 1).ifBlank { "all" }
      val academicClass = optCell(row, 2)
      val subject = optCell(row, 3).ifBlank { "General" }
      val topic = optCell(row, 4).ifBlank { "Fundamentals" }
      val marks = optCell(row, 5).toIntOrNull() ?: 2
      val difficulty = optCell(row, 6).ifBlank { "medium" }
      val text = optCell(row, 7)
      if (text.isBlank()) continue
      val optA = optCell(row, 8)
      val optB = optCell(row, 9)
      val optC = optCell(row, 10)
      val optD = optCell(row, 11)
      val options = listOf(optA, optB, optC, optD).filter { it.isNotBlank() }
      val correctIdx = optCell(row, 12).toIntOrNull() ?: 0
      val codeSnippet = optCell(row, 13).ifBlank { null }
      val explanation = optCell(row, 14).ifBlank { null }

      list.add(
        ExamQuestion(
          id = id,
          courseId = courseId,
          academicClass = academicClass,
          subject = subject,
          topic = topic,
          marks = marks,
          difficulty = difficulty,
          questionText = text,
          options = options,
          correctOptionIndex = correctIdx,
          codeSnippet = codeSnippet,
          explanation = explanation
        )
      )
    }
    return list
  }

  private fun parseSubmissionsTable(array: JSONArray?): List<ExamSubmission> {
    val list = mutableListOf<ExamSubmission>()
    if (array == null || array.length() <= 1) return list

    for (i in 1 until array.length()) {
      val row = array.optJSONArray(i) ?: continue
      val id = optCell(row, 0).ifBlank { "sub-$i" }
      val examId = optCell(row, 1)
      val examTitle = optCell(row, 2)
      val studentId = optCell(row, 3)
      val studentName = optCell(row, 4)
      val rollNo = optCell(row, 5)
      val score = optCell(row, 9).toIntOrNull() ?: 0
      val total = optCell(row, 10).toIntOrNull() ?: 20
      val pct = optCell(row, 11).toIntOrNull() ?: 0
      val passed = optCell(row, 12).contains("pass", ignoreCase = true)
      val timeSpent = optCell(row, 13).toIntOrNull() ?: 0
      val submittedAt = optCell(row, 14)

      list.add(
        ExamSubmission(
          id = id,
          examId = examId,
          examTitle = examTitle,
          studentId = studentId,
          studentName = studentName,
          studentRollNo = rollNo,
          score = score,
          totalMarks = total,
          percentage = pct,
          passed = passed,
          timeSpentSeconds = timeSpent,
          submittedAt = submittedAt
        )
      )
    }
    return list
  }

  private fun fetchCsvRows(name: String, gid: String): List<List<String>>? {
    val url = "https://docs.google.com/spreadsheets/d/$SPREADSHEET_ID/export?format=csv&gid=$gid"
    val req = Request.Builder()
      .url(url)
      .header("User-Agent", "Mozilla/5.0 PixelPathsala/1.0")
      .build()

    val res = client.newCall(req).execute()
    if (!res.isSuccessful) return null
    val csv = res.body?.string() ?: return null

    val rows = mutableListOf<List<String>>()
    val reader = BufferedReader(StringReader(csv))
    var line: String?
    while (reader.readLine().also { line = it } != null) {
      val parsedLine = parseCsvLine(line!!)
      rows.add(parsedLine)
    }
    return rows
  }

  private fun parseCsvLine(line: String): List<String> {
    val tokens = mutableListOf<String>()
    var inQuotes = false
    val sb = StringBuilder()
    for (ch in line) {
      if (ch == '\"') {
        inQuotes = !inQuotes
      } else if (ch == ',' && !inQuotes) {
        tokens.add(sb.toString().trim().removeSurrounding("\""))
        sb.clear()
      } else {
        sb.append(ch)
      }
    }
    tokens.add(sb.toString().trim().removeSurrounding("\""))
    return tokens
  }

  private fun optCell(row: JSONArray, index: Int): String {
    if (index >= row.length()) return ""
    val item = row.opt(index) ?: return ""
    return item.toString().trim()
  }

  // ==========================================
  // WRITE OPERATIONS TO GOOGLE SHEETS
  // ==========================================

  fun postToWebhook(jsonPayload: String): Boolean {
    return try {
      val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
      val body = jsonPayload.toRequestBody(mediaType)
      val request = Request.Builder()
        .url(WEBHOOK_URL)
        .header("User-Agent", "Mozilla/5.0 PixelPathsala/1.0")
        .post(body)
        .build()

      val response = client.newCall(request).execute()
      val resString = response.body?.string() ?: ""
      println("DEBUG postToWebhook code: ${response.code}, body: $resString")
      Log.d(TAG, "postToWebhook response code: ${response.code}, body: $resString")
      response.isSuccessful || resString.contains("\"success\":true")
    } catch (e: Exception) {
      println("DEBUG postToWebhook exception: ${e.message}")
      e.printStackTrace()
      Log.e(TAG, "postToWebhook failed: ${e.message}", e)
      false
    }
  }

  fun appendStudent(student: Student): Boolean {
    val row = listOf(
      student.id,
      student.rollNo,
      student.name,
      student.mobile,
      student.aadhaarNo,
      student.email ?: "",
      student.guardianName,
      student.guardianPhone,
      student.address,
      student.courseId,
      student.batchId,
      student.admissionDate,
      student.status,
      student.customMonthlyFeeOverride?.toString() ?: "",
      "",
      "",
      ""
    )
    val json = JSONObject()
    json.put("action", "addStudent")
    val rowArr = JSONArray()
    row.forEach { rowArr.put(it) }
    json.put("row", rowArr)
    return postToWebhook(json.toString())
  }

  fun appendPayment(payment: FeePayment): Boolean {
    val row = listOf(
      payment.receiptNo,
      payment.id,
      payment.studentId,
      payment.studentName,
      payment.finalAmountPaid,
      payment.paymentMode,
      payment.monthsCovered.joinToString(","),
      payment.transactionRef,
      payment.paymentDate,
      payment.status,
      payment.approvedBy,
      payment.remarks ?: payment.rejectionReason ?: ""
    )
    val json = JSONObject()
    json.put("action", "addPayment")
    val rowArr = JSONArray()
    row.forEach { rowArr.put(it) }
    json.put("row", rowArr)
    return postToWebhook(json.toString())
  }

  fun appendAdmission(app: AdmissionApplication): Boolean {
    val row = listOf(
      app.id,
      app.studentName,
      app.mobile,
      app.aadhaarNo,
      app.email ?: "",
      app.guardianName,
      app.guardianPhone,
      app.targetCourseId,
      app.status,
      app.appliedDate,
      app.initialPaymentStatus,
      app.initialPaymentMode ?: "CASH",
      app.initialPaymentRef ?: "",
      app.remarks ?: "",
      app.rejectionReason ?: "",
      app.address
    )
    val json = JSONObject()
    json.put("action", "addAdmission")
    val rowArr = JSONArray()
    row.forEach { rowArr.put(it) }
    json.put("row", rowArr)
    return postToWebhook(json.toString())
  }

  fun pushStudentsTable(students: List<Student>): Boolean {
    val headers = listOf(
      "Student ID", "Roll No", "Full Name", "Mobile Number", "Aadhaar No", "Email",
      "Guardian Name", "Guardian Phone", "Residential Address", "Course ID", "Batch ID",
      "Admission Date", "Status", "Custom Fee Override (₹)", "Monthly Base Fee (₹)",
      "Total Paid (₹)", "Outstanding Due (₹)"
    )
    val rows = mutableListOf<List<Any?>>()
    rows.add(headers)
    for (s in students) {
      rows.add(
        listOf(
          s.id,
          s.rollNo,
          s.name,
          s.mobile,
          s.aadhaarNo,
          s.email ?: "",
          s.guardianName,
          s.guardianPhone,
          s.address,
          s.courseId,
          s.batchId,
          s.admissionDate,
          s.status,
          s.customMonthlyFeeOverride?.toString() ?: "",
          "",
          "",
          ""
        )
      )
    }

    val json = JSONObject()
    json.put("action", "saveAll")
    val tables = JSONObject()
    val arr = JSONArray()
    for (row in rows) {
      val rowArr = JSONArray()
      row.forEach { rowArr.put(it ?: "") }
      arr.put(rowArr)
    }
    tables.put("Students", arr)
    json.put("tables", tables)

    return postToWebhook(json.toString())
  }

  fun pushPaymentsTable(payments: List<FeePayment>): Boolean {
    val headers = listOf(
      "Receipt No", "Payment ID", "Student ID", "Student Name", "Amount Paid (₹)",
      "Payment Mode", "Months Covered", "Transaction Ref / UPI", "Payment Date",
      "Approval Status", "Approved By", "Rejection Reason"
    )
    val rows = mutableListOf<List<Any?>>()
    rows.add(headers)
    for (p in payments) {
      rows.add(
        listOf(
          p.receiptNo,
          p.id,
          p.studentId,
          p.studentName,
          p.finalAmountPaid,
          p.paymentMode,
          p.monthsCovered.joinToString(","),
          p.transactionRef,
          p.paymentDate,
          p.status,
          p.approvedBy,
          p.remarks ?: p.rejectionReason ?: ""
        )
      )
    }

    val json = JSONObject()
    json.put("action", "saveAll")
    val tables = JSONObject()
    val arr = JSONArray()
    for (row in rows) {
      val rowArr = JSONArray()
      row.forEach { rowArr.put(it ?: "") }
      arr.put(rowArr)
    }
    tables.put("Fee_Transactions", arr)
    json.put("tables", tables)

    return postToWebhook(json.toString())
  }

  fun pushAdmissionsTable(admissions: List<AdmissionApplication>): Boolean {
    val headers = listOf(
      "Application ID", "Student Name", "Mobile Number", "Aadhaar No", "Email",
      "Guardian Name", "Guardian Phone", "Target Course ID", "Status", "Applied Date",
      "Initial Fee Status", "Payment Mode", "Transaction Ref / UPI", "Staff Remarks",
      "Rejection Reason", "Address"
    )
    val rows = mutableListOf<List<Any?>>()
    rows.add(headers)
    for (a in admissions) {
      rows.add(
        listOf(
          a.id,
          a.studentName,
          a.mobile,
          a.aadhaarNo,
          a.email ?: "",
          a.guardianName,
          a.guardianPhone,
          a.targetCourseId,
          a.status,
          a.appliedDate,
          a.initialPaymentStatus,
          a.initialPaymentMode ?: "CASH",
          a.initialPaymentRef ?: "",
          a.remarks ?: "",
          a.rejectionReason ?: "",
          a.address
        )
      )
    }

    val json = JSONObject()
    json.put("action", "saveAll")
    val tables = JSONObject()
    val arr = JSONArray()
    for (row in rows) {
      val rowArr = JSONArray()
      row.forEach { rowArr.put(it ?: "") }
      arr.put(rowArr)
    }
    tables.put("Admissions", arr)
    json.put("tables", tables)

    return postToWebhook(json.toString())
  }

  fun pushLiveClassesTable(sessions: List<LiveClassSession>): Boolean {
    val headers = listOf(
      "Session ID", "Class Title", "Academic Class", "Subject", "Course ID",
      "Batch ID", "Educator Name", "Date", "Start Time", "End Time",
      "Status", "Platform", "Meeting Link / Room ID", "Recording Available", "Created At"
    )
    val rows = mutableListOf<List<Any?>>()
    rows.add(headers)
    for (s in sessions) {
      rows.add(
        listOf(
          s.id,
          s.title,
          s.academicClass ?: s.targetClass ?: "All Classes",
          s.subject,
          s.courseId,
          s.batchId,
          s.educatorName,
          s.scheduledDate,
          s.startTime,
          s.endTime,
          s.status,
          s.platform,
          s.meetingUrl ?: "https://meet.google.com/new",
          if (s.isRecordingAvailable) "Yes" else "No",
          s.scheduledDate
        )
      )
    }

    val json = JSONObject()
    json.put("action", "saveAll")
    val tables = JSONObject()
    val arr = JSONArray()
    for (row in rows) {
      val rowArr = JSONArray()
      row.forEach { rowArr.put(it ?: "") }
      arr.put(rowArr)
    }
    tables.put("Live_Classes", arr)
    json.put("tables", tables)

    return postToWebhook(json.toString())
  }

  fun pushRecordingsTable(recordings: List<ClassRecording>): Boolean {
    val headers = listOf(
      "Recording ID", "Title", "Academic Class", "Subject", "Topic",
      "Chapter", "Course ID", "Batch ID", "Educator Name", "Recorded Date",
      "Duration (Mins)", "Source Type", "Video URL", "Notes PDF URL", "View Count", "Created At"
    )
    val rows = mutableListOf<List<Any?>>()
    rows.add(headers)
    for (r in recordings) {
      rows.add(
        listOf(
          r.id,
          r.title,
          r.academicClass ?: r.targetClass ?: "All Classes",
          r.subject,
          r.topic ?: "",
          r.chapter ?: "",
          r.courseId,
          r.batchId,
          r.educatorName,
          r.recordedDate,
          r.durationMinutes,
          r.videoSourceType,
          r.videoUrl,
          r.notesPdfUrl ?: "",
          r.viewCount,
          r.recordedDate
        )
      )
    }

    val json = JSONObject()
    json.put("action", "saveAll")
    val tables = JSONObject()
    val arr = JSONArray()
    for (row in rows) {
      val rowArr = JSONArray()
      row.forEach { rowArr.put(it ?: "") }
      arr.put(rowArr)
    }
    tables.put("Class_Recordings", arr)
    json.put("tables", tables)

    return postToWebhook(json.toString())
  }

  fun pushStudyMaterialsTable(materials: List<StudyMaterial>): Boolean {
    val headers = listOf(
      "Material ID", "Title", "Subject", "Course ID", "Batch ID",
      "File Type", "File Name", "File Size", "File URL / Link", "Uploaded By",
      "Uploaded Date", "Download Count", "Description"
    )
    val rows = mutableListOf<List<Any?>>()
    rows.add(headers)
    for (m in materials) {
      rows.add(
        listOf(
          m.id,
          m.title,
          m.subject ?: "General",
          m.courseId,
          m.batchId,
          m.fileType,
          m.fileName,
          m.fileSize ?: "Cloud File",
          m.fileUrl,
          m.uploadedBy,
          m.uploadedDate,
          m.downloadCount,
          m.description ?: ""
        )
      )
    }

    val json = JSONObject()
    json.put("action", "saveAll")
    val tables = JSONObject()
    val arr = JSONArray()
    for (row in rows) {
      val rowArr = JSONArray()
      row.forEach { rowArr.put(it ?: "") }
      arr.put(rowArr)
    }
    tables.put("Study_Materials", arr)
    json.put("tables", tables)

    return postToWebhook(json.toString())
  }

  fun pushExamsTable(exams: List<OnlineExam>): Boolean {
    val headers = listOf(
      "Exam ID", "Exam Code", "Title", "Course ID", "Batch ID",
      "Academic Class", "Subject", "Duration (Mins)", "Total Marks", "Passing Marks",
      "Passing %", "Status", "Mode", "Scheduled Date", "Start Time",
      "End Time", "Question Count", "Question IDs", "Instructions", "Published", "Created At"
    )
    val rows = mutableListOf<List<Any?>>()
    rows.add(headers)
    for (e in exams) {
      rows.add(
        listOf(
          e.id,
          e.examCode,
          e.title,
          e.courseId,
          e.batchId,
          e.academicClass ?: "Class 10",
          e.subject,
          e.durationMinutes,
          e.totalMarks,
          e.passingMarks,
          e.passingPercentage,
          e.status,
          e.mode,
          e.scheduledDate,
          e.startTime,
          e.endTime,
          e.questionIds.size,
          e.questionIds.joinToString(","),
          e.instructions ?: "Read each question carefully.",
          if (e.isPublished) "Yes" else "No",
          e.scheduledDate
        )
      )
    }

    val json = JSONObject()
    json.put("action", "saveAll")
    val tables = JSONObject()
    val arr = JSONArray()
    for (row in rows) {
      val rowArr = JSONArray()
      row.forEach { rowArr.put(it ?: "") }
      arr.put(rowArr)
    }
    tables.put("Exams", arr)
    json.put("tables", tables)

    return postToWebhook(json.toString())
  }

  fun pushQuestionsTable(questions: List<ExamQuestion>): Boolean {
    val headers = listOf(
      "Question ID", "Course ID", "Academic Class", "Subject", "Topic",
      "Marks", "Difficulty", "Question Text", "Option A", "Option B",
      "Option C", "Option D", "Correct Option Index", "Code Snippet", "Explanation", "Created At"
    )
    val rows = mutableListOf<List<Any?>>()
    rows.add(headers)
    for (q in questions) {
      val optA = q.options.getOrNull(0) ?: ""
      val optB = q.options.getOrNull(1) ?: ""
      val optC = q.options.getOrNull(2) ?: ""
      val optD = q.options.getOrNull(3) ?: ""
      rows.add(
        listOf(
          q.id,
          q.courseId,
          q.academicClass ?: "",
          q.subject,
          q.topic,
          q.marks,
          q.difficulty,
          q.questionText,
          optA,
          optB,
          optC,
          optD,
          q.correctOptionIndex,
          q.codeSnippet ?: "",
          q.explanation ?: "",
          "2026-09-29"
        )
      )
    }

    val json = JSONObject()
    json.put("action", "saveAll")
    val tables = JSONObject()
    val arr = JSONArray()
    for (row in rows) {
      val rowArr = JSONArray()
      row.forEach { rowArr.put(it ?: "") }
      arr.put(rowArr)
    }
    tables.put("Question_Bank", arr)
    json.put("tables", tables)

    return postToWebhook(json.toString())
  }

  fun pushExamSubmissionsTable(submissions: List<ExamSubmission>): Boolean {
    val headers = listOf(
      "Submission ID", "Exam ID", "Exam Title", "Student ID", "Student Name",
      "Roll No", "Aadhaar No", "Course ID", "Batch ID", "Score Obtained",
      "Total Marks", "Percentage (%)", "Result Status", "Time Spent (Secs)", "Submitted At", "Answers Summary"
    )
    val rows = mutableListOf<List<Any?>>()
    rows.add(headers)
    for (sub in submissions) {
      rows.add(
        listOf(
          sub.id,
          sub.examId,
          sub.examTitle,
          sub.studentId,
          sub.studentName,
          sub.studentRollNo ?: "",
          "",
          "",
          "",
          sub.score,
          sub.totalMarks,
          sub.percentage,
          if (sub.passed) "PASSED" else "NEEDS_IMPROVEMENT",
          sub.timeSpentSeconds,
          sub.submittedAt,
          ""
        )
      )
    }

    val json = JSONObject()
    json.put("action", "saveAll")
    val tables = JSONObject()
    val arr = JSONArray()
    for (row in rows) {
      val rowArr = JSONArray()
      row.forEach { rowArr.put(it ?: "") }
      arr.put(rowArr)
    }
    tables.put("Exam_Submissions", arr)
    json.put("tables", tables)

    return postToWebhook(json.toString())
  }
}
