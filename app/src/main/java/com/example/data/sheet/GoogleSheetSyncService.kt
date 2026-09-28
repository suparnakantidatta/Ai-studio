package com.example.data.sheet

import android.util.Log
import com.example.data.model.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.StringReader
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
    .connectTimeout(20, TimeUnit.SECONDS)
    .readTimeout(25, TimeUnit.SECONDS)
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
    val students = parseStudentsTable(dataObj.optJSONArray("Students"))
    val (courses, batches) = parseCoursesBatchesTable(dataObj.optJSONArray("Courses_Batches"))
    val payments = parsePaymentsTable(dataObj.optJSONArray("Fee_Transactions"))
    val admissions = parseAdmissionsTable(dataObj.optJSONArray("Admissions"))
    val educators = parseEducatorsTable(dataObj.optJSONArray("Educators"))
    val centerInfo = parseCenterOverviewTable(dataObj.optJSONArray("Center_Overview"))
    val adminAccounts = parseAdminsTable(dataObj.optJSONArray("Admin_Credentials"))

    return DatabaseDataDto(
      centerInfo = centerInfo,
      students = students,
      courses = courses,
      batches = batches,
      payments = payments,
      admissions = admissions,
      educators = educators,
      adminAccounts = adminAccounts
    )
  }

  private fun parseStudentsTable(array: JSONArray?): List<Student> {
    val list = mutableListOf<Student>()
    if (array == null || array.length() <= 1) return list

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
      val admissionDate = optCell(row, 11).take(10)
      val status = optCell(row, 12).ifBlank { "active" }

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
          status = status
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
      val paymentId = optCell(row, 1).ifBlank { "pay-$i" }
      val studentId = optCell(row, 2)
      val studentName = optCell(row, 3)
      val amount = optCell(row, 4).toDoubleOrNull() ?: 0.0
      val mode = optCell(row, 5).ifBlank { "CASH" }
      val month = optCell(row, 6).take(7)
      val ref = optCell(row, 7)
      val date = optCell(row, 8).take(10)
      val status = optCell(row, 9).ifBlank { "approved" }
      val approvedBy = optCell(row, 10).ifBlank { "Admin" }
      val remarks = optCell(row, 11)

      list.add(
        FeePayment(
          id = paymentId,
          receiptNo = receiptNo,
          studentId = studentId,
          studentName = studentName,
          baseMonthlyFee = amount,
          totalBaseFee = amount,
          finalAmountPaid = amount,
          paymentMode = mode,
          month = month,
          monthsCovered = listOf(month),
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
    val students = fetchCsvRows("Students", GIDS["Students"] ?: "1572610885")?.let { rows ->
      val arr = JSONArray()
      rows.forEach { r -> arr.put(JSONArray(r)) }
      parseStudentsTable(arr)
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

    return DatabaseDataDto(
      centerInfo = centerInfo,
      students = students,
      courses = courses,
      batches = batches,
      payments = payments,
      educators = educators,
      adminAccounts = admins
    )
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
        tokens.add(sb.toString().trim())
        sb.clear()
      } else {
        sb.append(ch)
      }
    }
    tokens.add(sb.toString().trim())
    return tokens
  }

  private fun optCell(row: JSONArray, index: Int): String {
    if (index >= row.length()) return ""
    val item = row.opt(index) ?: return ""
    return item.toString().trim()
  }
}
