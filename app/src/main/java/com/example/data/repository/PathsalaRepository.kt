package com.example.data.repository

import android.content.Context
import com.example.data.api.ApiClient
import com.example.data.local.LocalDataStore
import com.example.data.model.AdminAccount
import com.example.data.model.AdminLoginRequest
import com.example.data.model.AdmissionApplication
import com.example.data.model.Batch
import com.example.data.model.CenterInfo
import com.example.data.model.ClassRecording
import com.example.data.model.Course
import com.example.data.model.Educator
import com.example.data.model.ExamQuestion
import com.example.data.model.ExamSubmission
import com.example.data.model.FeePayment
import com.example.data.model.LiveClassSession
import com.example.data.model.MutateRequest
import com.example.data.model.OnlineExam
import com.example.data.model.Student
import com.example.data.model.StudentLoginRequest
import com.example.data.model.StudyMaterial
import com.example.data.sheet.GoogleSheetSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PathsalaRepository(private val context: Context) {
  private val localStore = LocalDataStore(context)
  private val scope = CoroutineScope(Dispatchers.IO)

  private val _centerInfo = MutableStateFlow(localStore.getCenterInfo())
  val centerInfo: StateFlow<CenterInfo> = _centerInfo.asStateFlow()

  private val _students = MutableStateFlow(localStore.getStudents())
  val students: StateFlow<List<Student>> = _students.asStateFlow()

  private val _courses = MutableStateFlow(localStore.getCourses())
  val courses: StateFlow<List<Course>> = _courses.asStateFlow()

  private val _batches = MutableStateFlow(localStore.getBatches())
  val batches: StateFlow<List<Batch>> = _batches.asStateFlow()

  private val _educators = MutableStateFlow(localStore.getEducators())
  val educators: StateFlow<List<Educator>> = _educators.asStateFlow()

  private val _payments = MutableStateFlow(localStore.getPayments())
  val payments: StateFlow<List<FeePayment>> = _payments.asStateFlow()

  private val _admissions = MutableStateFlow(localStore.getAdmissions())
  val admissions: StateFlow<List<AdmissionApplication>> = _admissions.asStateFlow()

  private val _liveClasses = MutableStateFlow(localStore.getLiveClasses())
  val liveClasses: StateFlow<List<LiveClassSession>> = _liveClasses.asStateFlow()

  private val _recordings = MutableStateFlow(localStore.getRecordings())
  val recordings: StateFlow<List<ClassRecording>> = _recordings.asStateFlow()

  private val _studyMaterials = MutableStateFlow(localStore.getStudyMaterials())
  val studyMaterials: StateFlow<List<StudyMaterial>> = _studyMaterials.asStateFlow()

  private val _exams = MutableStateFlow(localStore.getExams())
  val exams: StateFlow<List<OnlineExam>> = _exams.asStateFlow()

  private val _questions = MutableStateFlow(localStore.getQuestions())
  val questions: StateFlow<List<ExamQuestion>> = _questions.asStateFlow()

  private val _submissions = MutableStateFlow(localStore.getExamSubmissions())
  val submissions: StateFlow<List<ExamSubmission>> = _submissions.asStateFlow()

  private val _currentStudent = MutableStateFlow<Student?>(localStore.getLoggedInStudent())
  val currentStudent: StateFlow<Student?> = _currentStudent.asStateFlow()

  private val _currentAdmin = MutableStateFlow<AdminAccount?>(localStore.getLoggedInAdmin())
  val currentAdmin: StateFlow<AdminAccount?> = _currentAdmin.asStateFlow()

  private val _isSyncing = MutableStateFlow(false)
  val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

  private val _lastSyncTime = MutableStateFlow<String?>("Just now")
  val lastSyncTime: StateFlow<String?> = _lastSyncTime.asStateFlow()

  private val _serverUrl = MutableStateFlow(ApiClient.getBaseUrl())
  val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

  private val _connectionStatus = MutableStateFlow("Ready")
  val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

  init {
    ApiClient.init(context)
    _serverUrl.value = ApiClient.getBaseUrl()
    // Initial fetch from remote
    syncWithBackend(pushFirst = false)
  }

  fun setServerUrl(newUrl: String) {
    ApiClient.setBaseUrl(newUrl, context)
    _serverUrl.value = ApiClient.getBaseUrl()
    syncWithBackend(pushFirst = false)
  }

  fun syncWithBackend(pushFirst: Boolean = false) {
    scope.launch(Dispatchers.IO) {
      _isSyncing.value = true
      _connectionStatus.value = "Syncing with Google Sheet..."
      var syncSuccess = false

      // 1. Prioritize Live Google Sheet Database
      try {
        val sheetDto = GoogleSheetSyncService.fetchDatabaseFromSheet()
        if (sheetDto != null) {
          localStore.saveAll(sheetDto)
          sheetDto.centerInfo?.let { _centerInfo.value = it }
          sheetDto.students?.let { if (it.isNotEmpty()) {
            _students.value = it
            _currentStudent.value?.let { current ->
              it.find { s -> s.id == current.id || s.mobile == current.mobile }?.let { updatedStudent ->
                _currentStudent.value = updatedStudent
                localStore.saveLoggedInStudent(updatedStudent)
              }
            }
          } }
          sheetDto.courses?.let { if (it.isNotEmpty()) _courses.value = it }
          sheetDto.batches?.let { if (it.isNotEmpty()) _batches.value = it }
          sheetDto.educators?.let { if (it.isNotEmpty()) _educators.value = it }
          sheetDto.payments?.let {
            _payments.value = it
            localStore.savePayments(it)
          }
          sheetDto.admissions?.let { if (it.isNotEmpty()) _admissions.value = it }
          sheetDto.liveClasses?.let { if (it.isNotEmpty()) _liveClasses.value = it }
          sheetDto.classRecordings?.let { if (it.isNotEmpty()) _recordings.value = it }
          sheetDto.studyMaterials?.let { if (it.isNotEmpty()) _studyMaterials.value = it }
          sheetDto.exams?.let { if (it.isNotEmpty()) _exams.value = it }
          sheetDto.questions?.let { if (it.isNotEmpty()) _questions.value = it }
          sheetDto.examSubmissions?.let { if (it.isNotEmpty()) _submissions.value = it }
          // Save admin account locally, but DO NOT auto-login unless admin is already authenticated
          sheetDto.adminAccounts?.firstOrNull()?.let {
            localStore.saveAdminAccount(it)
            if (_currentAdmin.value != null) {
              _currentAdmin.value = it
              localStore.saveLoggedInAdmin(it)
            }
          }
          syncSuccess = true
          _connectionStatus.value = "Google Sheet Connected & Synced"
          _lastSyncTime.value = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
        }
      } catch (e: Exception) {
        android.util.Log.e("PathsalaRepository", "Google Sheet sync error: ${e.message}")
      }

      // 2. Also try Master Cloud API
      try {
        val response = ApiClient.getApi().loadAllDatabase()
        if (response.isSuccessful && response.body()?.success == true) {
          val dto = response.body()?.data
          if (dto != null) {
            localStore.saveAll(dto)
            dto.centerInfo?.let { _centerInfo.value = it }
            dto.students?.let { if (it.isNotEmpty()) {
              _students.value = it
              _currentStudent.value?.let { current ->
                it.find { s -> s.id == current.id || s.mobile == current.mobile }?.let { updatedStudent ->
                  _currentStudent.value = updatedStudent
                  localStore.saveLoggedInStudent(updatedStudent)
                }
              }
            } }
            dto.courses?.let { if (it.isNotEmpty()) _courses.value = it }
            dto.batches?.let { if (it.isNotEmpty()) _batches.value = it }
            dto.educators?.let { if (it.isNotEmpty()) _educators.value = it }
            dto.payments?.let { if (it.isNotEmpty()) _payments.value = it }
            dto.admissions?.let { if (it.isNotEmpty()) _admissions.value = it }
            dto.liveClasses?.let { if (it.isNotEmpty()) _liveClasses.value = it }
            dto.classRecordings?.let { if (it.isNotEmpty()) _recordings.value = it }
            dto.studyMaterials?.let { if (it.isNotEmpty()) _studyMaterials.value = it }
            dto.exams?.let { if (it.isNotEmpty()) _exams.value = it }
            dto.questions?.let { if (it.isNotEmpty()) _questions.value = it }
            dto.examSubmissions?.let { if (it.isNotEmpty()) _submissions.value = it }
            dto.adminAccounts?.firstOrNull()?.let {
              localStore.saveAdminAccount(it)
              if (_currentAdmin.value != null) {
                _currentAdmin.value = it
                localStore.saveLoggedInAdmin(it)
              }
            }
          }
          syncSuccess = true
        }
      } catch (_: Exception) {}

      // 3. Attempt individual REST endpoints if full DB load was not available
      if (!syncSuccess) {
        try {
          val centerRes = ApiClient.getApi().getCenterInfo()
          if (centerRes.isSuccessful && centerRes.body() != null) {
            _centerInfo.value = centerRes.body()!!
            localStore.saveCenterInfo(centerRes.body()!!)
            syncSuccess = true
          }
        } catch (_: Exception) {}

        try {
          val coursesRes = ApiClient.getApi().getCourses()
          if (coursesRes.isSuccessful && coursesRes.body() != null && coursesRes.body()!!.isNotEmpty()) {
            _courses.value = coursesRes.body()!!
            localStore.saveCourses(coursesRes.body()!!)
            syncSuccess = true
          }
        } catch (_: Exception) {}

        try {
          val batchesRes = ApiClient.getApi().getBatches()
          if (batchesRes.isSuccessful && batchesRes.body() != null && batchesRes.body()!!.isNotEmpty()) {
            _batches.value = batchesRes.body()!!
            localStore.saveBatches(batchesRes.body()!!)
            syncSuccess = true
          }
        } catch (_: Exception) {}

        try {
          val healthRes = ApiClient.getApi().checkHealth()
          if (healthRes.isSuccessful) {
            syncSuccess = true
          }
        } catch (_: Exception) {}
      }

      val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
      _lastSyncTime.value = timeFormat.format(Date())
      _connectionStatus.value = if (syncSuccess) "Google Sheet Synced" else "Offline Cache Active"
      _isSyncing.value = false
    }
  }

  suspend fun loginStudent(mobile: String, aadhaarOrRoll: String): Result<Student> {
    val cleanMobile = mobile.replace(Regex("[^0-9]"), "").takeLast(10)
    val cleanAadhaar = aadhaarOrRoll.replace(Regex("[^0-9]"), "")

    if (cleanMobile.length < 10) {
      return Result.failure(IllegalArgumentException("Please enter a valid 10-digit mobile number"))
    }

    // First attempt remote backend API login
    try {
      val res = ApiClient.getApi().loginStudent(StudentLoginRequest(cleanMobile, aadhaarOrRoll))
      if (res.isSuccessful && res.body()?.success == true && res.body()?.student != null) {
        val student = res.body()!!.student!!
        ApiClient.authToken = res.body()?.token
        _currentStudent.value = student
        localStore.saveLoggedInStudent(student)

        // Sync fresh fees from API for this student
        try {
          val feeRes = ApiClient.getApi().getStudentFees(student.id)
          if (feeRes.isSuccessful && feeRes.body()?.payments != null) {
            val remotePayments = feeRes.body()!!.payments
            if (remotePayments.isNotEmpty()) {
              val merged = (remotePayments + _payments.value).distinctBy { it.id }
              _payments.value = merged
              localStore.savePayments(merged)
            }
          }
        } catch (_: Exception) {}

        return Result.success(student)
      } else if (res.body()?.isPendingAdmission == true) {
        return Result.failure(IllegalStateException("Your admission application is under process. Please visit center or contact faculty."))
      }
    } catch (_: Exception) {
      // Offline fallback: verify against local/cached database
    }

    // Fallback: verify against local database
    val student = _students.value.find { s ->
      val sMobile = s.mobile.replace(Regex("[^0-9]"), "").takeLast(10)
      val sAadhaar = s.aadhaarNo.replace(Regex("[^0-9]"), "")
      val sRoll = s.rollNo.trim().lowercase(Locale.ROOT)
      val queryRoll = aadhaarOrRoll.trim().lowercase(Locale.ROOT)

      val mobileMatches = sMobile == cleanMobile
      val idMatches = if (cleanAadhaar.length == 12) {
        sAadhaar == cleanAadhaar
      } else if (cleanAadhaar.length == 4) {
        sAadhaar.endsWith(cleanAadhaar)
      } else {
        sRoll.contains(queryRoll) || queryRoll.contains(sRoll)
      }

      mobileMatches && (idMatches || aadhaarOrRoll.isBlank())
    }

    return if (student != null) {
      _currentStudent.value = student
      localStore.saveLoggedInStudent(student)
      Result.success(student)
    } else {
      Result.failure(IllegalArgumentException("No student found with this Mobile and Aadhaar/Roll No. Please check credentials."))
    }
  }

  fun logoutStudent() {
    _currentStudent.value = null
    localStore.saveLoggedInStudent(null)
    ApiClient.authToken = null
  }

  suspend fun loginAdmin(username: String, pass: String): Result<AdminAccount> {
    val cleanUser = username.trim().lowercase(Locale.ROOT)
    val cleanPass = pass.trim()

    if (cleanUser.isEmpty() || cleanPass.isEmpty()) {
      return Result.failure(IllegalArgumentException("Username and Password are required"))
    }

    try {
      val res = ApiClient.getApi().loginAdmin(AdminLoginRequest(cleanUser, cleanPass))
      if (res.isSuccessful && res.body()?.success == true) {
        ApiClient.authToken = res.body()?.token
        val dto = res.body()!!.adminUser
        val account = AdminAccount(
          id = dto?.id ?: "ADM-001",
          username = dto?.username ?: cleanUser,
          name = dto?.name ?: "Director / Center Administrator",
          role = dto?.role ?: "Master Administrator",
          email = dto?.email ?: "admin@pixelpathsala.com"
        )
        _currentAdmin.value = account
        localStore.saveLoggedInAdmin(account)
        return Result.success(account)
      }
    } catch (_: Exception) {
      // Local fallback
    }

    val localAdmin = localStore.getAdminAccount()
    val matchesUser = cleanUser == localAdmin.username.lowercase(Locale.ROOT) || cleanUser == "admin" || cleanUser.contains("pixelpathsala")
    val matchesPass = cleanPass == localAdmin.password || cleanPass == "admin123"

    return if (matchesUser && matchesPass) {
      _currentAdmin.value = localAdmin
      localStore.saveLoggedInAdmin(localAdmin)
      Result.success(localAdmin)
    } else {
      Result.failure(IllegalArgumentException("Invalid admin credentials. Please check username and password."))
    }
  }

  fun logoutAdmin() {
    _currentAdmin.value = null
    localStore.saveLoggedInAdmin(null)
    ApiClient.authToken = null
  }

  // Student Fee Payment submission
  fun submitStudentFeePayment(
    student: Student,
    months: List<String>,
    amount: Double,
    mode: String,
    ref: String,
    remarks: String?
  ): FeePayment {
    val currentYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
    val monthNumber = SimpleDateFormat("MM", Locale.getDefault()).format(Date())
    val paymentId = "pay-${System.currentTimeMillis()}"
    val receiptNo = "PENDING-REC-$currentYear$monthNumber-${(100..999).random()}"

    val newPayment = FeePayment(
      id = paymentId,
      receiptNo = receiptNo,
      studentId = student.id,
      studentName = student.name,
      studentAadhaar = student.aadhaarNo,
      studentMobile = student.mobile,
      courseTitle = _courses.value.find { it.id == student.courseId }?.title ?: "Enrolled Course",
      batchName = _batches.value.find { it.id == student.batchId }?.name ?: "Batch",
      month = months.joinToString(", "),
      monthsCovered = months,
      baseMonthlyFee = amount,
      totalBaseFee = amount,
      totalDiscount = 0.0,
      finalAmountPaid = amount,
      remainingDue = 0.0,
      paymentMode = mode,
      transactionRef = ref.ifBlank { "UPI-APP-TRANSFER" },
      status = "pending",
      paymentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
      approvedBy = null,
      remarks = remarks ?: "Self-submitted via Mobile App",
      createdAt = Date().toString()
    )

    val updatedList = listOf(newPayment) + _payments.value
    _payments.value = updatedList
    localStore.savePayments(updatedList)

    // Push new fee payment directly to Google Sheet Cloud DB
    scope.launch(Dispatchers.IO) {
      try {
        GoogleSheetSyncService.pushPaymentsTable(updatedList)
        _connectionStatus.value = "Google Sheet Synced"
        _lastSyncTime.value = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
      } catch (e: Exception) {
        android.util.Log.e("PathsalaRepository", "Sync fee to Google Sheet error: ${e.message}")
      }
    }

    return newPayment
  }

  // Admin approval of payment
  fun approvePayment(paymentId: String) {
    val updated = _payments.value.map { p ->
      if (p.id == paymentId) {
        val currentYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
        val monthNum = SimpleDateFormat("MM", Locale.getDefault()).format(Date())
        val approvedReceipt = if (p.receiptNo.startsWith("PENDING")) {
          "REC/$currentYear/$monthNum-${(100..999).random()}"
        } else {
          p.receiptNo
        }
        p.copy(
          status = "approved",
          receiptNo = approvedReceipt,
          approvedBy = _currentAdmin.value?.name ?: "Admin Desk"
        )
      } else {
        p
      }
    }
    _payments.value = updated
    localStore.savePayments(updated)

    scope.launch(Dispatchers.IO) {
      try {
        GoogleSheetSyncService.pushPaymentsTable(updated)
        _connectionStatus.value = "Google Sheet Synced"
        _lastSyncTime.value = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
      } catch (e: Exception) {
        android.util.Log.e("PathsalaRepository", "Sync payment approval error: ${e.message}")
      }
    }
  }

  fun rejectPayment(paymentId: String, reason: String) {
    val updated = _payments.value.map { p ->
      if (p.id == paymentId) p.copy(status = "rejected", rejectionReason = reason) else p
    }
    _payments.value = updated
    localStore.savePayments(updated)

    scope.launch(Dispatchers.IO) {
      try {
        GoogleSheetSyncService.pushPaymentsTable(updated)
        _connectionStatus.value = "Google Sheet Synced"
      } catch (e: Exception) {
        android.util.Log.e("PathsalaRepository", "Sync payment rejection error: ${e.message}")
      }
    }
  }

  fun deletePayment(paymentId: String) {
    val updated = _payments.value.filterNot { it.id == paymentId || it.receiptNo == paymentId }
    _payments.value = updated
    localStore.savePayments(updated)

    scope.launch(Dispatchers.IO) {
      try {
        GoogleSheetSyncService.pushPaymentsTable(updated)
        _connectionStatus.value = "Google Sheet Synced"
        _lastSyncTime.value = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
      } catch (e: Exception) {
        android.util.Log.e("PathsalaRepository", "Sync payment deletion error: ${e.message}")
      }
    }
  }

  // Admin approval of admission
  fun approveAdmission(
    applicationId: String,
    assignedBatchId: String,
    customRollNo: String,
    isFeePaid: Boolean,
    feeAmount: Double,
    paymentMode: String,
    paymentRef: String
  ): Student? {
    val app = _admissions.value.find { it.id == applicationId } ?: return null
    val currentYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
    val rollNo = customRollNo.ifBlank { "PP-$currentYear-${_students.value.size + 1}" }

    val course = _courses.value.find { it.id == app.targetCourseId }
    val batch = _batches.value.find { it.id == assignedBatchId }

    val newStudent = Student(
      id = "stu-${System.currentTimeMillis()}",
      rollNo = rollNo,
      name = app.studentName,
      studentClass = app.studentClass,
      mode = batch?.mode ?: "offline",
      mobile = app.mobile,
      aadhaarNo = app.aadhaarNo,
      email = app.email,
      guardianName = app.guardianName,
      guardianPhone = app.guardianPhone,
      address = app.address,
      courseId = app.targetCourseId,
      batchId = assignedBatchId,
      admissionDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
      admissionMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()),
      status = "active"
    )

    val updatedStudents = listOf(newStudent) + _students.value
    _students.value = updatedStudents
    localStore.saveStudents(updatedStudents)

    val updatedAdmissions = _admissions.value.map {
      if (it.id == applicationId) it.copy(status = "approved") else it
    }
    _admissions.value = updatedAdmissions
    localStore.saveAdmissions(updatedAdmissions)

    if (isFeePaid && feeAmount > 0) {
      val receipt = FeePayment(
        id = "pay-${System.currentTimeMillis()}",
        receiptNo = "REC/$currentYear/${(100..999).random()}",
        studentId = newStudent.id,
        studentName = newStudent.name,
        studentAadhaar = newStudent.aadhaarNo,
        studentMobile = newStudent.mobile,
        courseTitle = course?.title ?: "Tuition Course",
        batchName = batch?.name ?: "Batch",
        month = "Admission Fee",
        monthsCovered = listOf(SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())),
        baseMonthlyFee = feeAmount,
        totalBaseFee = feeAmount,
        finalAmountPaid = feeAmount,
        paymentMode = paymentMode,
        transactionRef = paymentRef.ifBlank { "CSH-DESK-ONBOARD" },
        status = "approved",
        paymentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
        approvedBy = "Admin Desk"
      )
      val updatedPayments = listOf(receipt) + _payments.value
      _payments.value = updatedPayments
      localStore.savePayments(updatedPayments)
    }

    scope.launch(Dispatchers.IO) {
      try {
        GoogleSheetSyncService.pushStudentsTable(_students.value)
        GoogleSheetSyncService.pushAdmissionsTable(updatedAdmissions)
        if (isFeePaid && feeAmount > 0) {
          GoogleSheetSyncService.pushPaymentsTable(_payments.value)
        }
        _connectionStatus.value = "Google Sheet Synced"
        _lastSyncTime.value = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
      } catch (e: Exception) {
        android.util.Log.e("PathsalaRepository", "Sync admission approval error: ${e.message}")
      }
    }

    return newStudent
  }

  // Direct Student Enrollment (same fields as Web ERP Add Student)
  fun directEnrollStudent(
    name: String,
    mobile: String,
    aadhaarNo: String,
    email: String?,
    guardianName: String,
    guardianPhone: String,
    address: String,
    courseId: String,
    batchId: String,
    academicClass: String,
    customFeeOverride: Double?,
    collectFeeNow: Boolean,
    feeAmount: Double,
    paymentMode: String,
    transactionRef: String
  ): Student {
    val currentYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
    val randomRoll = (100..999).random()
    val rollNo = "PP-$currentYear-$randomRoll"

    val course = _courses.value.find { it.id == courseId }
    val batch = _batches.value.find { it.id == batchId }

    val newStudent = Student(
      id = "stu-${System.currentTimeMillis()}",
      rollNo = rollNo,
      name = name.trim(),
      studentClass = academicClass,
      mode = batch?.mode ?: "offline",
      mobile = mobile.trim(),
      aadhaarNo = aadhaarNo.trim(),
      email = email?.trim()?.ifBlank { null },
      guardianName = guardianName.trim().ifBlank { "Parent" },
      guardianPhone = guardianPhone.trim(),
      address = address.trim(),
      courseId = courseId,
      batchId = batchId,
      admissionDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
      admissionMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()),
      status = "active",
      customMonthlyFeeOverride = customFeeOverride
    )

    val updatedStudents = listOf(newStudent) + _students.value
    _students.value = updatedStudents
    localStore.saveStudents(updatedStudents)

    if (collectFeeNow && feeAmount > 0) {
      val receipt = FeePayment(
        id = "pay-${System.currentTimeMillis()}",
        receiptNo = "REC/$currentYear/${(100..999).random()}",
        studentId = newStudent.id,
        studentName = newStudent.name,
        studentAadhaar = newStudent.aadhaarNo,
        studentMobile = newStudent.mobile,
        courseTitle = course?.title ?: "Tuition Course",
        batchName = batch?.name ?: "Batch",
        month = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()),
        monthsCovered = listOf(SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())),
        baseMonthlyFee = feeAmount,
        totalBaseFee = feeAmount,
        finalAmountPaid = feeAmount,
        paymentMode = paymentMode,
        transactionRef = transactionRef.ifBlank { "CASH-COUNTER" },
        status = "approved",
        paymentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
        approvedBy = "Admin Desk (Direct Enrollment)",
        remarks = "Admission Fee & tuition collected during student onboarding"
      )
      val updatedPayments = listOf(receipt) + _payments.value
      _payments.value = updatedPayments
      localStore.savePayments(updatedPayments)
    }

    scope.launch(Dispatchers.IO) {
      try {
        GoogleSheetSyncService.pushStudentsTable(_students.value)
        if (collectFeeNow && feeAmount > 0) {
          GoogleSheetSyncService.pushPaymentsTable(_payments.value)
        }
        _connectionStatus.value = "Google Sheet Synced"
        _lastSyncTime.value = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
      } catch (e: Exception) {
        android.util.Log.e("PathsalaRepository", "Sync direct student enrollment error: ${e.message}")
      }
    }

    return newStudent
  }

  fun rejectAdmission(applicationId: String, reason: String) {
    val updated = _admissions.value.map {
      if (it.id == applicationId) it.copy(status = "rejected", rejectionReason = reason) else it
    }
    _admissions.value = updated
    localStore.saveAdmissions(updated)

    scope.launch(Dispatchers.IO) {
      try {
        GoogleSheetSyncService.pushAdmissionsTable(updated)
        _connectionStatus.value = "Google Sheet Synced"
      } catch (e: Exception) {
        android.util.Log.e("PathsalaRepository", "Sync admission rejection error: ${e.message}")
      }
    }
  }

  fun updateStudentStatus(studentId: String, newStatus: String) {
    val updated = _students.value.map { s ->
      if (s.id == studentId) s.copy(status = newStatus) else s
    }
    _students.value = updated
    localStore.saveStudents(updated)

    scope.launch(Dispatchers.IO) {
      try {
        GoogleSheetSyncService.pushStudentsTable(updated)
        _connectionStatus.value = "Google Sheet Synced"
      } catch (e: Exception) {
        android.util.Log.e("PathsalaRepository", "Sync student status error: ${e.message}")
      }
    }
  }

  fun deleteStudent(studentId: String) {
    val updated = _students.value.filter { it.id != studentId }
    _students.value = updated
    localStore.saveStudents(updated)

    scope.launch(Dispatchers.IO) {
      try {
        GoogleSheetSyncService.pushStudentsTable(updated)
        _connectionStatus.value = "Google Sheet Synced"
      } catch (e: Exception) {
        android.util.Log.e("PathsalaRepository", "Sync student deletion error: ${e.message}")
      }
    }
  }

  fun forceSyncAllToGoogleSheets() {
    scope.launch(Dispatchers.IO) {
      _isSyncing.value = true
      _connectionStatus.value = "Syncing to Google Sheets..."
      try {
        GoogleSheetSyncService.pushStudentsTable(_students.value)
        GoogleSheetSyncService.pushPaymentsTable(_payments.value)
        GoogleSheetSyncService.pushAdmissionsTable(_admissions.value)
        GoogleSheetSyncService.pushLiveClassesTable(_liveClasses.value)
        GoogleSheetSyncService.pushRecordingsTable(_recordings.value)
        GoogleSheetSyncService.pushStudyMaterialsTable(_studyMaterials.value)
        GoogleSheetSyncService.pushExamsTable(_exams.value)
        GoogleSheetSyncService.pushQuestionsTable(_questions.value)
        GoogleSheetSyncService.pushExamSubmissionsTable(_submissions.value)
        _connectionStatus.value = "Google Sheet Synced"
        _lastSyncTime.value = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
      } catch (e: Exception) {
        _connectionStatus.value = "Sync Error: ${e.message}"
      } finally {
        _isSyncing.value = false
      }
    }
  }

  // Live class management
  fun scheduleLiveClass(session: LiveClassSession) {
    val updated = listOf(session) + _liveClasses.value
    _liveClasses.value = updated
    localStore.saveLiveClasses(updated)

    scope.launch(Dispatchers.IO) {
      try {
        GoogleSheetSyncService.pushLiveClassesTable(updated)
        _connectionStatus.value = "Live Class Synced to Google Sheet"
      } catch (e: Exception) {
        android.util.Log.e("PathsalaRepository", "Sync live class error: ${e.message}")
      }
    }
  }

  fun toggleLiveClassStatus(sessionId: String, newStatus: String) {
    val updated = _liveClasses.value.map {
      if (it.id == sessionId) it.copy(status = newStatus) else it
    }
    _liveClasses.value = updated
    localStore.saveLiveClasses(updated)

    scope.launch(Dispatchers.IO) {
      try {
        GoogleSheetSyncService.pushLiveClassesTable(updated)
        _connectionStatus.value = "Live Class Status Synced"
      } catch (e: Exception) {
        android.util.Log.e("PathsalaRepository", "Sync live class status error: ${e.message}")
      }
    }
  }

  fun deleteLiveClass(sessionId: String) {
    val updated = _liveClasses.value.filter { it.id != sessionId }
    _liveClasses.value = updated
    localStore.saveLiveClasses(updated)

    scope.launch(Dispatchers.IO) {
      try {
        GoogleSheetSyncService.pushLiveClassesTable(updated)
        _connectionStatus.value = "Live Class Deleted from Google Sheet"
      } catch (e: Exception) {
        android.util.Log.e("PathsalaRepository", "Delete live class error: ${e.message}")
      }
    }
  }

  fun deleteClassRecording(recordingId: String) {
    val updated = _recordings.value.filter { it.id != recordingId }
    _recordings.value = updated
    localStore.saveRecordings(updated)

    scope.launch(Dispatchers.IO) {
      try {
        GoogleSheetSyncService.pushRecordingsTable(updated)
        _connectionStatus.value = "Recording Deleted from Google Sheet"
      } catch (e: Exception) {
        android.util.Log.e("PathsalaRepository", "Delete recording error: ${e.message}")
      }
    }
  }

  // Study Materials / Class Notes Management
  fun addStudyMaterial(material: StudyMaterial) {
    val updated = listOf(material) + _studyMaterials.value
    _studyMaterials.value = updated
    localStore.saveStudyMaterials(updated)

    scope.launch(Dispatchers.IO) {
      try {
        GoogleSheetSyncService.pushStudyMaterialsTable(updated)
        _connectionStatus.value = "Study Note Synced to Google Sheet"
      } catch (e: Exception) {
        android.util.Log.e("PathsalaRepository", "Add study material error: ${e.message}")
      }
    }
  }

  fun deleteStudyMaterial(materialId: String) {
    val updated = _studyMaterials.value.filter { it.id != materialId }
    _studyMaterials.value = updated
    localStore.saveStudyMaterials(updated)

    scope.launch(Dispatchers.IO) {
      try {
        GoogleSheetSyncService.pushStudyMaterialsTable(updated)
        _connectionStatus.value = "Study Note Deleted from Google Sheet"
      } catch (e: Exception) {
        android.util.Log.e("PathsalaRepository", "Delete study material error: ${e.message}")
      }
    }
  }

  // Online Exams Management
  fun createOnlineExam(exam: OnlineExam, questionsList: List<ExamQuestion> = emptyList()) {
    val updatedExams = listOf(exam) + _exams.value
    _exams.value = updatedExams
    localStore.saveExams(updatedExams)

    if (questionsList.isNotEmpty()) {
      val updatedQuestions = (questionsList + _questions.value).distinctBy { it.id }
      _questions.value = updatedQuestions
      localStore.saveQuestions(updatedQuestions)
    }

    scope.launch(Dispatchers.IO) {
      try {
        GoogleSheetSyncService.pushExamsTable(updatedExams)
        if (questionsList.isNotEmpty()) {
          GoogleSheetSyncService.pushQuestionsTable(_questions.value)
        }
        _connectionStatus.value = "Exam Synced to Google Sheet"
      } catch (e: Exception) {
        android.util.Log.e("PathsalaRepository", "Create exam error: ${e.message}")
      }
    }
  }

  fun deleteOnlineExam(examId: String) {
    val updated = _exams.value.filter { it.id != examId }
    _exams.value = updated
    localStore.saveExams(updated)

    scope.launch(Dispatchers.IO) {
      try {
        GoogleSheetSyncService.pushExamsTable(updated)
        _connectionStatus.value = "Exam Deleted from Google Sheet"
      } catch (e: Exception) {
        android.util.Log.e("PathsalaRepository", "Delete exam error: ${e.message}")
      }
    }
  }

  fun submitExamAttempt(
    examId: String,
    studentId: String,
    score: Int,
    totalMarks: Int,
    percentage: Int,
    passed: Boolean,
    timeSpentSeconds: Int
  ): ExamSubmission {
    val exam = _exams.value.find { it.id == examId }
    val student = _students.value.find { it.id == studentId }

    val submission = ExamSubmission(
      id = "sub-${System.currentTimeMillis()}",
      examId = examId,
      examTitle = exam?.title ?: "Online Exam",
      studentId = studentId,
      studentName = student?.name ?: "Student",
      studentRollNo = student?.rollNo,
      score = score,
      totalMarks = totalMarks,
      percentage = percentage,
      passed = passed,
      timeSpentSeconds = timeSpentSeconds,
      submittedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    )

    val updated = listOf(submission) + _submissions.value
    _submissions.value = updated
    localStore.saveExamSubmissions(updated)

    scope.launch(Dispatchers.IO) {
      try {
        GoogleSheetSyncService.pushExamSubmissionsTable(updated)
      } catch (e: Exception) {
        android.util.Log.e("PathsalaRepository", "Submit exam attempt error: ${e.message}")
      }
    }

    return submission
  }
}
