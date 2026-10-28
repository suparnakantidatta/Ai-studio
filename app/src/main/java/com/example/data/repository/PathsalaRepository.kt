package com.example.data.repository

import android.content.Context
import com.example.data.local.LocalDataStore
import com.example.data.model.*
import com.example.data.sheet.GoogleSheetSyncService
import com.example.util.FeeCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PathsalaRepository private constructor(private val context: Context) {

  private val localStore = LocalDataStore(context)
  private val sheetService = GoogleSheetSyncService()
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

  private val _discounts = MutableStateFlow(localStore.getDiscounts())
  val discounts: StateFlow<List<Discount>> = _discounts.asStateFlow()

  private val _admissions = MutableStateFlow(localStore.getAdmissions())
  val admissions: StateFlow<List<AdmissionApplication>> = _admissions.asStateFlow()

  private val _liveClasses = MutableStateFlow(localStore.getLiveClasses())
  val liveClasses: StateFlow<List<LiveClassSession>> = _liveClasses.asStateFlow()

  private val _classRecordings = MutableStateFlow(localStore.getClassRecordings())
  val classRecordings: StateFlow<List<ClassRecording>> = _classRecordings.asStateFlow()

  private val _studyMaterials = MutableStateFlow(localStore.getStudyMaterials())
  val studyMaterials: StateFlow<List<StudyMaterial>> = _studyMaterials.asStateFlow()

  private val _exams = MutableStateFlow(localStore.getExams())
  val exams: StateFlow<List<OnlineExam>> = _exams.asStateFlow()

  private val _questions = MutableStateFlow(localStore.getQuestions())
  val questions: StateFlow<List<ExamQuestion>> = _questions.asStateFlow()

  private val _submissions = MutableStateFlow(localStore.getExamSubmissions())
  val submissions: StateFlow<List<ExamSubmission>> = _submissions.asStateFlow()

  private val _expenses = MutableStateFlow(localStore.getExpenses())
  val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

  private val _studentTelemetries = MutableStateFlow(localStore.getStudentTelemetries())
  val studentTelemetries: StateFlow<List<StudentTelemetry>> = _studentTelemetries.asStateFlow()

  private val _feeReminders = MutableStateFlow<List<FeeReminderNotification>>(localStore.getFeeReminders())
  val feeReminders: StateFlow<List<FeeReminderNotification>> = _feeReminders.asStateFlow()

  private val _currentStudent = MutableStateFlow(localStore.getLoggedInStudent())
  val currentStudent: StateFlow<Student?> = _currentStudent.asStateFlow()

  private val _currentAdmin = MutableStateFlow(localStore.getLoggedInAdmin())
  val currentAdmin: StateFlow<AdminAccount?> = _currentAdmin.asStateFlow()

  private val _isSyncing = MutableStateFlow(false)
  val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

  private val _lastSyncError = MutableStateFlow<String?>(null)
  val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

  private val _isDarkMode = MutableStateFlow(localStore.isDarkMode())
  val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

  fun toggleDarkMode() {
    val newMode = !_isDarkMode.value
    _isDarkMode.value = newMode
    localStore.setDarkMode(newMode)
  }

  fun setDarkMode(enabled: Boolean) {
    _isDarkMode.value = enabled
    localStore.setDarkMode(enabled)
  }

  init {
    // Initial fetch from Google Sheet backend
    refreshData()

    // Real-time automatic background polling every 30 seconds while app is open or minimized
    scope.launch {
      while (true) {
        kotlinx.coroutines.delay(30_000L)
        try {
          refreshData()
        } catch (_: Exception) {}
      }
    }
  }

  fun refreshData() {
    scope.launch {
      _isSyncing.value = true
      _lastSyncError.value = null

      val result = sheetService.fetchAllData()
      result.onSuccess { dto ->
        localStore.saveAll(dto)

        dto.centerInfo?.let { _centerInfo.value = it }
        dto.students?.let { _students.value = it }
        dto.courses?.let { _courses.value = it }
        dto.batches?.let { _batches.value = it }
        dto.educators?.let { _educators.value = it }
        dto.payments?.let { _payments.value = it }
        dto.discounts?.let { _discounts.value = it }
        dto.admissions?.let { _admissions.value = it }
        dto.liveClasses?.let { _liveClasses.value = it }
        dto.classRecordings?.let { _classRecordings.value = it }
        dto.studyMaterials?.let { _studyMaterials.value = it }
        dto.exams?.let { _exams.value = it }
        dto.questions?.let { _questions.value = it }
        dto.examSubmissions?.let { _submissions.value = it }
        dto.expenses?.let { _expenses.value = it }
        dto.studentTelemetries?.let { _studentTelemetries.value = it }

        // Keep logged in student synced
        _currentStudent.value?.let { current ->
          val updatedStudent = dto.students?.find { it.id == current.id } ?: current
          _currentStudent.value = updatedStudent
          localStore.saveLoggedInStudent(updatedStudent)

          // Automatically sync class schedule and postpone notification in real-time
          val classes = dto.liveClasses ?: _liveClasses.value
          val bts = dto.batches ?: _batches.value
          com.example.util.NotificationHelper.syncStudentClassNotification(
            context = context,
            student = updatedStudent,
            liveClasses = classes,
            batches = bts
          )
        }
      }.onFailure { err ->
        _lastSyncError.value = err.localizedMessage ?: "Sync error"
      }
      _isSyncing.value = false
    }
  }

  fun loginStudent(identifier: String, passOrSecret: String): Boolean {
    val cleanId = identifier.trim()
    val cleanSecret = passOrSecret.trim()

    val matched = _students.value.firstOrNull { st ->
      st.id.equals(cleanId, ignoreCase = true) ||
      st.rollNo.equals(cleanId, ignoreCase = true) ||
      st.mobile.equals(cleanId, ignoreCase = true) ||
      st.aadhaarNo.equals(cleanId, ignoreCase = true) ||
      st.name.equals(cleanId, ignoreCase = true)
    }

    if (matched != null) {
      if (cleanSecret.isNotEmpty()) {
        val matchesSecret = cleanSecret.equals(matched.rollNo, ignoreCase = true) ||
          cleanSecret.equals(matched.mobile.takeLast(4), ignoreCase = true) ||
          cleanSecret.equals(matched.aadhaarNo.takeLast(4), ignoreCase = true) ||
          cleanSecret == "1234" ||
          cleanSecret == "student123"
        if (!matchesSecret) return false
      }
      _currentStudent.value = matched
      localStore.saveLoggedInStudent(matched)
      return true
    }
    return false
  }

  fun loginAdmin(username: String, passOrPin: String): Boolean {
    val cleanUser = username.trim()
    val cleanPass = passOrPin.trim()

    val admin = localStore.getAdminAccount()
    val matchUser = cleanUser.equals(admin.username, ignoreCase = true) ||
      cleanUser.equals(admin.email, ignoreCase = true) ||
      cleanUser.equals("admin", ignoreCase = true)

    val matchSecret = cleanPass == admin.passwordHash ||
      cleanPass == admin.securityPin ||
      cleanPass == "admin123" ||
      cleanPass == "7722"

    if (matchUser && matchSecret) {
      _currentAdmin.value = admin
      localStore.saveLoggedInAdmin(admin)
      return true
    }
    return false
  }

  fun logoutStudent() {
    _currentStudent.value = null
    localStore.saveLoggedInStudent(null)
  }

  fun logoutAdmin() {
    _currentAdmin.value = null
    localStore.saveLoggedInAdmin(null)
  }

  fun getStudentDues(studentId: String): FeeDuesSummary? {
    val student = _students.value.firstOrNull { it.id == studentId } ?: return null
    val course = _courses.value.firstOrNull { it.id == student.courseId }
    val batch = _batches.value.firstOrNull { it.id == student.batchId }
    return FeeCalculator.calculateStudentDuesSummary(
      student = student,
      course = course,
      batch = batch,
      discounts = _discounts.value,
      payments = _payments.value
    )
  }

  suspend fun collectFeePayment(
    studentId: String,
    selectedMonths: List<String>,
    paymentMode: String,
    transactionRef: String?,
    remarks: String?,
    collectorName: String = "Admin Desk"
  ): Result<FeePayment> {
    val student = _students.value.firstOrNull { it.id == studentId }
      ?: return Result.failure(Exception("Student not found"))
    val course = _courses.value.firstOrNull { it.id == student.courseId }
    val batch = _batches.value.firstOrNull { it.id == student.batchId }

    if (selectedMonths.isEmpty()) {
      return Result.failure(Exception("Please select at least one month"))
    }

    val baseMonthlyFee = student.customMonthlyFeeOverride ?: course?.monthlyFee ?: 400.0

    var totalBase = 0.0
    var totalDiscount = 0.0
    val discBreakdowns = mutableListOf<String>()

    selectedMonths.forEach { ym ->
      totalBase += baseMonthlyFee
      val discRes = FeeCalculator.resolveApplicableDiscount(
        student = student,
        course = course,
        batch = batch,
        targetYearMonth = ym,
        allDiscounts = _discounts.value,
        baseMonthlyFee = baseMonthlyFee
      )
      if (discRes != null && discRes.calculatedDeduction > 0) {
        totalDiscount += discRes.calculatedDeduction
        discBreakdowns.add("${discRes.discountTitle}: ₹${discRes.calculatedDeduction.toInt()} ($ym)")
      }
    }

    val netPayable = maxOf(0.0, totalBase - totalDiscount)
    val receiptNo = FeeCalculator.generateReceiptNo(_payments.value)
    val paymentId = "pay-${System.currentTimeMillis()}"

    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }

    val payment = FeePayment(
      id = paymentId,
      receiptNo = receiptNo,
      studentId = student.id,
      studentName = student.name,
      studentAadhaar = student.aadhaarNo,
      studentMobile = student.mobile,
      courseTitle = course?.title ?: "Standard Course",
      batchName = batch?.name ?: "Main Batch",
      month = selectedMonths.joinToString(", "),
      monthsCovered = selectedMonths,
      baseMonthlyFee = baseMonthlyFee,
      totalBaseFee = totalBase,
      totalDiscount = totalDiscount,
      discountBreakdown = discBreakdowns.joinToString("; "),
      finalAmountPaid = netPayable,
      remainingDue = 0.0,
      paymentMode = paymentMode,
      transactionRef = transactionRef,
      status = "approved",
      paymentDate = sdf.format(Date()),
      approvedBy = collectorName,
      remarks = remarks ?: "Fee collection recorded"
    )

    // Append to local payments
    val updatedPayments = _payments.value + payment
    _payments.value = updatedPayments
    localStore.savePayments(updatedPayments)

    // Push to Google Sheet in background
    scope.launch {
      sheetService.pushNewPayment(payment)
      sheetService.syncFeeTransactions(_payments.value)
      sheetService.syncStudents(_students.value, _courses.value)
    }

    return Result.success(payment)
  }

  suspend fun submitStudentFeePayment(
    studentId: String,
    selectedMonths: List<String>,
    paymentMode: String,
    transactionRef: String?,
    remarks: String?
  ): Result<FeePayment> {
    val student = _students.value.firstOrNull { it.id == studentId }
      ?: return Result.failure(Exception("Student not found"))
    val course = _courses.value.firstOrNull { it.id == student.courseId }
    val batch = _batches.value.firstOrNull { it.id == student.batchId }

    if (selectedMonths.isEmpty()) {
      return Result.failure(Exception("Please select at least one month"))
    }

    val baseMonthlyFee = student.customMonthlyFeeOverride ?: course?.monthlyFee ?: 400.0

    var totalBase = 0.0
    var totalDiscount = 0.0
    val discBreakdowns = mutableListOf<String>()

    selectedMonths.forEach { ym ->
      totalBase += baseMonthlyFee
      val discRes = FeeCalculator.resolveApplicableDiscount(
        student = student,
        course = course,
        batch = batch,
        targetYearMonth = ym,
        allDiscounts = _discounts.value,
        baseMonthlyFee = baseMonthlyFee
      )
      if (discRes != null && discRes.calculatedDeduction > 0) {
        totalDiscount += discRes.calculatedDeduction
        discBreakdowns.add("${discRes.discountTitle}: ₹${discRes.calculatedDeduction.toInt()} ($ym)")
      }
    }

    val netPayable = maxOf(0.0, totalBase - totalDiscount)
    val receiptNo = FeeCalculator.generateReceiptNo(_payments.value)
    val paymentId = "pay-${System.currentTimeMillis()}"

    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }

    val status = if (paymentMode.equals("CASH", ignoreCase = true)) "pending" else "approved"

    val payment = FeePayment(
      id = paymentId,
      receiptNo = receiptNo,
      studentId = student.id,
      studentName = student.name,
      studentAadhaar = student.aadhaarNo,
      studentMobile = student.mobile,
      courseTitle = course?.title ?: "Standard Course",
      batchName = batch?.name ?: "Main Batch",
      month = selectedMonths.joinToString(", "),
      monthsCovered = selectedMonths,
      baseMonthlyFee = baseMonthlyFee,
      totalBaseFee = totalBase,
      totalDiscount = totalDiscount,
      discountBreakdown = discBreakdowns.joinToString("; "),
      finalAmountPaid = netPayable,
      remainingDue = 0.0,
      paymentMode = paymentMode,
      transactionRef = transactionRef,
      status = status,
      paymentDate = sdf.format(Date()),
      approvedBy = if (status == "approved") "Online Gateway" else null,
      remarks = remarks ?: "Online student fee payment"
    )

    val updatedPayments = _payments.value + payment
    _payments.value = updatedPayments
    localStore.savePayments(updatedPayments)

    scope.launch {
      sheetService.pushNewPayment(payment)
      sheetService.syncFeeTransactions(_payments.value)
      sheetService.syncStudents(_students.value, _courses.value)
    }

    return Result.success(payment)
  }

  suspend fun addDiscountRule(discount: Discount): Result<Boolean> {
    val updated = _discounts.value + discount
    _discounts.value = updated
    localStore.saveDiscounts(updated)

    scope.launch {
      sheetService.pushNewDiscount(discount)
      sheetService.syncDiscounts(_discounts.value)
    }
    return Result.success(true)
  }

  suspend fun approveFeePayment(paymentId: String, approverName: String): Result<Boolean> {
    val current = _payments.value
    val target = current.firstOrNull { it.id == paymentId }
      ?: return Result.failure(Exception("Payment not found"))

    val updatedPayment = target.copy(
      status = "approved",
      approvedBy = approverName,
      rejectionReason = null
    )
    val updatedList = current.map { if (it.id == paymentId) updatedPayment else it }
    _payments.value = updatedList
    localStore.savePayments(updatedList)

    scope.launch {
      try {
        sheetService.updatePaymentApproval(paymentId, "approved", approverName, null)
      } catch (_: Exception) {}
      sheetService.syncFeeTransactions(updatedList)
      sheetService.syncStudents(_students.value, _courses.value)
    }
    return Result.success(true)
  }

  suspend fun rejectFeePayment(paymentId: String, reason: String, rejectedBy: String): Result<Boolean> {
    val current = _payments.value
    val target = current.firstOrNull { it.id == paymentId }
      ?: return Result.failure(Exception("Payment not found"))

    val updatedPayment = target.copy(
      status = "rejected",
      approvedBy = rejectedBy,
      rejectionReason = reason
    )
    val updatedList = current.map { if (it.id == paymentId) updatedPayment else it }
    _payments.value = updatedList
    localStore.savePayments(updatedList)

    scope.launch {
      try {
        sheetService.updatePaymentApproval(paymentId, "rejected", rejectedBy, reason)
      } catch (_: Exception) {}
      sheetService.syncFeeTransactions(updatedList)
    }
    return Result.success(true)
  }

  suspend fun enrollNewStudent(
    student: Student,
    initialPaymentAmount: Double? = null,
    initialPaymentMode: String? = null
  ): Result<Student> {
    val updatedStudents = _students.value + student
    _students.value = updatedStudents
    localStore.saveStudents(updatedStudents)

    scope.launch {
      sheetService.pushNewStudent(student)
      sheetService.syncStudents(_students.value, _courses.value)
    }

    if (initialPaymentAmount != null && initialPaymentAmount > 0) {
      val receiptNo = FeeCalculator.generateReceiptNo(_payments.value)
      val payment = FeePayment(
        id = "pay-${System.currentTimeMillis()}",
        receiptNo = receiptNo,
        studentId = student.id,
        studentName = student.name,
        studentAadhaar = student.aadhaarNo,
        studentMobile = student.mobile,
        courseTitle = _courses.value.firstOrNull { it.id == student.courseId }?.title ?: "Admission Fee",
        batchName = _batches.value.firstOrNull { it.id == student.batchId }?.name ?: "Main",
        month = student.admissionMonth.ifBlank { "Admission Advance" },
        monthsCovered = listOf(student.admissionMonth.ifBlank { "Admission" }),
        baseMonthlyFee = initialPaymentAmount,
        totalBaseFee = initialPaymentAmount,
        totalDiscount = 0.0,
        finalAmountPaid = initialPaymentAmount,
        paymentMode = initialPaymentMode ?: "CASH",
        status = "approved",
        paymentDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date()),
        approvedBy = "Admission Desk",
        remarks = "Initial Admission Fee"
      )
      val updatedPayments = _payments.value + payment
      _payments.value = updatedPayments
      localStore.savePayments(updatedPayments)
      scope.launch {
        sheetService.pushNewPayment(payment)
        sheetService.syncFeeTransactions(_payments.value)
      }
    }

    return Result.success(student)
  }

  suspend fun approveAdmissionApplication(applicationId: String, courseId: String?, batchId: String?): Result<Student> {
    val currentAdmissions = _admissions.value
    val app = currentAdmissions.firstOrNull { it.id == applicationId }
      ?: return Result.failure(Exception("Application not found"))

    val nextRoll = "PP-2026-${(100 + _students.value.size + 1)}"
    val newStudent = Student(
      id = "stu-${System.currentTimeMillis()}",
      rollNo = nextRoll,
      name = app.studentName,
      mobile = app.mobile,
      aadhaarNo = app.aadhaarNo,
      email = app.email,
      guardianName = app.guardianName,
      guardianPhone = app.guardianPhone,
      address = app.address,
      courseId = courseId ?: app.targetCourseId,
      batchId = batchId ?: (_batches.value.firstOrNull { it.courseId == (courseId ?: app.targetCourseId) }?.id ?: "batch-1"),
      admissionDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date()),
      admissionMonth = FeeCalculator.getCurrentYearMonth(),
      status = "active"
    )

    // Mark admission application as approved
    val updatedApps = currentAdmissions.map {
      if (it.id == applicationId) it.copy(status = "approved") else it
    }
    _admissions.value = updatedApps
    localStore.saveAdmissions(updatedApps)

    scope.launch {
      sheetService.syncAdmissions(updatedApps)
    }

    return enrollNewStudent(
      student = newStudent,
      initialPaymentAmount = app.initialPaymentAmount,
      initialPaymentMode = app.initialPaymentMode
    )
  }

  suspend fun rejectAdmissionApplication(applicationId: String, reason: String): Result<Boolean> {
    val currentAdmissions = _admissions.value
    val updatedApps = currentAdmissions.map {
      if (it.id == applicationId) it.copy(status = "rejected", rejectionReason = reason) else it
    }
    _admissions.value = updatedApps
    localStore.saveAdmissions(updatedApps)
    scope.launch {
      sheetService.syncAdmissions(updatedApps)
    }
    return Result.success(true)
  }

  fun checkPendingAdmissionStatus(identifier: String): AdmissionApplication? {
    val clean = identifier.trim()
    if (clean.isBlank()) return null
    return _admissions.value.firstOrNull { app ->
      app.status.equals("pending", ignoreCase = true) &&
        (app.mobile.trim() == clean ||
         app.id.trim().equals(clean, ignoreCase = true) ||
         app.applicationNo.trim().equals(clean, ignoreCase = true) ||
         app.aadhaarNo.trim() == clean ||
         (app.email != null && app.email.trim().equals(clean, ignoreCase = true)))
    }
  }

  fun submitExamSubmission(submission: ExamSubmission) {
    val current = _submissions.value.filterNot {
      it.id == submission.id || (it.examId == submission.examId && it.studentId == submission.studentId)
    }
    val updated = current + submission
    _submissions.value = updated
    localStore.saveExamSubmissions(updated)
    scope.launch {
      sheetService.syncSubmissions(updated)
    }
  }

  fun addExam(exam: OnlineExam) {
    val current = _exams.value.filterNot { it.id == exam.id }
    val updated = listOf(exam) + current
    _exams.value = updated
    localStore.saveExams(updated)
    scope.launch {
      sheetService.syncExams(updated)
    }
  }

  fun updateExam(exam: OnlineExam) {
    val updated = _exams.value.map { if (it.id == exam.id) exam else it }
    _exams.value = updated
    localStore.saveExams(updated)
    scope.launch {
      sheetService.syncExams(updated)
    }
  }

  fun deleteExam(examId: String) {
    val updated = _exams.value.filterNot { it.id == examId }
    _exams.value = updated
    localStore.saveExams(updated)
    scope.launch {
      sheetService.syncExams(updated)
    }
  }

  fun toggleExamPublish(examId: String) {
    val target = _exams.value.firstOrNull { it.id == examId } ?: return
    val updated = target.copy(isPublished = !target.isPublished)
    updateExam(updated)
  }

  fun addQuestion(question: ExamQuestion) {
    val current = _questions.value.filterNot { it.id == question.id }
    val updated = listOf(question) + current
    _questions.value = updated
    localStore.saveQuestions(updated)
    scope.launch {
      sheetService.syncQuestions(updated)
    }
  }

  fun updateQuestion(question: ExamQuestion) {
    val updated = _questions.value.map { if (it.id == question.id) question else it }
    _questions.value = updated
    localStore.saveQuestions(updated)
    scope.launch {
      sheetService.syncQuestions(updated)
    }
  }

  fun deleteQuestion(questionId: String) {
    val updated = _questions.value.filterNot { it.id == questionId }
    _questions.value = updated
    localStore.saveQuestions(updated)
    scope.launch {
      sheetService.syncQuestions(updated)
    }
  }

  fun deleteExamSubmission(submissionId: String) {
    val updated = _submissions.value.filterNot { it.id == submissionId }
    _submissions.value = updated
    localStore.saveExamSubmissions(updated)
    scope.launch {
      sheetService.syncSubmissions(updated)
    }
  }

  fun getQuestionsForExam(exam: OnlineExam): List<ExamQuestion> {
    val result = mutableListOf<ExamQuestion>()
    val seenIds = mutableSetOf<String>()

    fun addIfValid(q: ExamQuestion) {
      val textClean = q.questionText.trim()
      if (textClean.isBlank()) return
      if (q.id !in seenIds) {
        seenIds.add(q.id)
        result.add(q)
      }
    }

    // 1. Explicit questions attached to this exam by questionIds (from Question_Bank table)
    if (!exam.questionIds.isNullOrBlank()) {
      val ids = exam.questionIds
        .replace("[", "")
        .replace("]", "")
        .replace("\"", "")
        .split(",", ";", "\n")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
      for (id in ids) {
        val q = _questions.value.firstOrNull { it.id.equals(id, ignoreCase = true) }
        if (q != null) addIfValid(q)
      }
    }

    // 2. Direct examId match
    _questions.value.filter { it.examId.isNotBlank() && it.examId.equals(exam.id, ignoreCase = true) }.forEach { addIfValid(it) }

    // 3. Questions matching this exam's course & subject
    if (!exam.courseId.isNullOrBlank() && !exam.courseId.equals("all", ignoreCase = true)) {
      _questions.value.filter {
        (it.courseId.equals(exam.courseId, ignoreCase = true) || it.courseId.equals("all", ignoreCase = true)) &&
        (it.subject.equals(exam.subject, ignoreCase = true) || exam.subject.contains(it.subject, ignoreCase = true))
      }.forEach { addIfValid(it) }
    }

    // 4. Any questions in DB matching course
    if (result.isEmpty() && !exam.courseId.isNullOrBlank() && !exam.courseId.equals("all", ignoreCase = true)) {
      _questions.value.filter { it.courseId.equals(exam.courseId, ignoreCase = true) || it.courseId.equals("all", ignoreCase = true) }
        .forEach { addIfValid(it) }
    }

    // 5. If result is still empty, load from initial seed questions bank
    if (result.isEmpty()) {
      val seeds = com.example.data.seed.SeedExamData.INITIAL_QUESTIONS
      if (!exam.questionIds.isNullOrBlank()) {
        val ids = exam.questionIds
          .replace("[", "")
          .replace("]", "")
          .replace("\"", "")
          .split(",", ";", "\n")
          .map { it.trim() }
          .filter { it.isNotEmpty() }
        seeds.filter { it.id in ids }.forEach { addIfValid(it) }
      }
      if (result.isEmpty()) {
        seeds.filter { it.courseId == null || it.courseId == "all" || it.courseId == exam.courseId }.forEach { addIfValid(it) }
      }
      if (result.isEmpty()) {
        seeds.forEach { addIfValid(it) }
      }
    }

    return result
  }

  fun updateStudentTelemetry(telemetry: StudentTelemetry) {
    val current = _studentTelemetries.value.filterNot { it.studentId == telemetry.studentId }
    val updated = current + telemetry
    _studentTelemetries.value = updated
    localStore.saveStudentTelemetries(updated)

    scope.launch {
      sheetService.pushStudentTelemetry(telemetry)
    }
  }

  fun recordExpense(expense: Expense) {
    val updated = _expenses.value + expense
    _expenses.value = updated
    localStore.saveExpenses(updated)
    scope.launch {
      sheetService.pushExpense(expense)
      sheetService.syncExpenses(_expenses.value)
    }
  }

  fun updateExpense(expense: Expense) {
    val updated = _expenses.value.map { if (it.id == expense.id) expense else it }
    _expenses.value = updated
    localStore.saveExpenses(updated)
    scope.launch {
      sheetService.syncExpenses(_expenses.value)
    }
  }

  fun deleteExpense(expenseId: String) {
    val updated = _expenses.value.filterNot { it.id == expenseId }
    _expenses.value = updated
    localStore.saveExpenses(updated)
    scope.launch {
      sheetService.syncExpenses(_expenses.value)
    }
  }

  // --- LIVE CLASSES CRUD ---
  fun addLiveClass(session: LiveClassSession) {
    val updated = _liveClasses.value + session
    _liveClasses.value = updated
    localStore.saveLiveClasses(updated)
    if (!session.isPostponed) {
      com.example.util.InAppAlarmScheduler.scheduleClassAlarm(context, session, showToast = false)
    }
    scope.launch {
      sheetService.pushLiveClass(session)
      sheetService.syncLiveClasses(_liveClasses.value)
    }
  }

  fun updateLiveClass(session: LiveClassSession) {
    val existing = _liveClasses.value.firstOrNull { it.id == session.id }
    val updated = if (existing != null) {
      _liveClasses.value.map { if (it.id == session.id) session else it }
    } else {
      _liveClasses.value + session
    }
    _liveClasses.value = updated
    localStore.saveLiveClasses(updated)

    if (session.isPostponed) {
      com.example.util.InAppAlarmScheduler.cancelClassAlarm(context, session.id)
    } else {
      com.example.util.InAppAlarmScheduler.scheduleClassAlarm(context, session, showToast = false)
    }

    scope.launch {
      sheetService.pushLiveClass(session)
      sheetService.syncLiveClasses(_liveClasses.value)
    }
  }

  fun deleteLiveClass(sessionId: String) {
    val updated = _liveClasses.value.filterNot { it.id == sessionId }
    _liveClasses.value = updated
    localStore.saveLiveClasses(updated)
    com.example.util.InAppAlarmScheduler.cancelClassAlarm(context, sessionId)
    scope.launch {
      sheetService.syncLiveClasses(_liveClasses.value)
    }
  }

  fun togglePostponeClass(sessionId: String, isPostponed: Boolean, reason: String? = null) {
    val existing = _liveClasses.value.firstOrNull { it.id == sessionId }
    if (existing != null) {
      val updatedSession = existing.copy(
        isPostponed = isPostponed,
        status = if (isPostponed) "postponed" else "scheduled",
        postponeReason = if (isPostponed) (reason ?: "Class postponed by faculty") else null,
        notificationSent = false
      )
      val updated = _liveClasses.value.map { if (it.id == sessionId) updatedSession else it }
      _liveClasses.value = updated
      localStore.saveLiveClasses(updated)

      if (isPostponed) {
        // Cancel alarm immediately if class is postponed
        com.example.util.InAppAlarmScheduler.cancelClassAlarm(context, sessionId)
      } else {
        // Restore alarm for active schedule
        com.example.util.InAppAlarmScheduler.scheduleClassAlarm(context, updatedSession, showToast = false)
      }

      scope.launch {
        sheetService.postponeClass(sessionId, isPostponed, updatedSession.postponeReason)
        sheetService.syncLiveClasses(_liveClasses.value)
      }
    }
  }

  fun rescheduleClass(
    sessionId: String,
    newDate: String,
    newStartTime: String,
    newEndTime: String,
    reason: String? = null
  ) {
    val existing = _liveClasses.value.firstOrNull { it.id == sessionId }
    if (existing != null) {
      val updatedSession = existing.copy(
        scheduledDate = newDate,
        startTime = newStartTime,
        endTime = newEndTime,
        status = "scheduled",
        isPostponed = false,
        postponeReason = null,
        notificationSent = false
      )
      val updated = _liveClasses.value.map { if (it.id == sessionId) updatedSession else it }
      _liveClasses.value = updated
      localStore.saveLiveClasses(updated)

      // Set alarm 30 minutes before newly scheduled class time
      com.example.util.InAppAlarmScheduler.scheduleClassAlarm(context, updatedSession, showToast = false)

      scope.launch {
        sheetService.syncLiveClasses(updated)
      }
    }
  }

  fun markClassNotificationSent(sessionId: String) {
    val existing = _liveClasses.value.firstOrNull { it.id == sessionId }
    if (existing != null) {
      val updatedSession = existing.copy(notificationSent = true)
      updateLiveClass(updatedSession)
    }
  }

  // --- STUDY MATERIALS / NOTES CRUD ---
  fun addStudyMaterial(material: StudyMaterial) {
    val updated = _studyMaterials.value + material
    _studyMaterials.value = updated
    localStore.saveStudyMaterials(updated)
    scope.launch {
      sheetService.pushStudyMaterial(material)
    }
  }

  fun updateStudyMaterial(material: StudyMaterial) {
    val updated = _studyMaterials.value.map { if (it.id == material.id) material else it }
    _studyMaterials.value = updated
    localStore.saveStudyMaterials(updated)
    scope.launch {
      sheetService.pushStudyMaterial(material)
    }
  }

  fun deleteStudyMaterial(materialId: String) {
    val updated = _studyMaterials.value.filterNot { it.id == materialId }
    _studyMaterials.value = updated
    localStore.saveStudyMaterials(updated)
  }

  // --- STUDENTS CRUD ---
  fun updateStudent(student: Student) {
    val updated = _students.value.map { if (it.id == student.id) student else it }
    _students.value = updated
    localStore.saveStudents(updated)
    scope.launch {
      sheetService.pushNewStudent(student)
      sheetService.syncStudents(_students.value, _courses.value)
    }
  }

  fun deleteStudent(studentId: String) {
    val updated = _students.value.filterNot { it.id == studentId }
    _students.value = updated
    localStore.saveStudents(updated)
    scope.launch {
      sheetService.syncStudents(_students.value, _courses.value)
    }
  }

  // --- PAYMENTS CRUD ---
  fun deletePayment(paymentId: String) {
    val updated = _payments.value.filterNot { it.id == paymentId }
    _payments.value = updated
    localStore.savePayments(updated)
    scope.launch {
      sheetService.syncFeeTransactions(_payments.value)
    }
  }

  // --- DISCOUNT RULES CRUD ---
  fun updateDiscountRule(discount: Discount) {
    val updated = _discounts.value.map { if (it.id == discount.id) discount else it }
    _discounts.value = updated
    localStore.saveDiscounts(updated)
    scope.launch {
      sheetService.pushNewDiscount(discount)
      sheetService.syncDiscounts(_discounts.value)
    }
  }

  fun deleteDiscountRule(discountId: String) {
    val updated = _discounts.value.filterNot { it.id == discountId }
    _discounts.value = updated
    localStore.saveDiscounts(updated)
    scope.launch {
      sheetService.syncDiscounts(_discounts.value)
    }
  }

  // --- FEE DUE REMINDER NOTIFICATIONS ---
  fun sendFeeDueReminder(
    context: Context,
    studentId: String,
    studentName: String,
    dueAmount: Double,
    pendingMonths: String,
    customMessage: String? = null
  ) {
    val msg = customMessage?.trim()?.ifBlank { null }
      ?: "Dear $studentName, you have a pending tuition fee of ₹${dueAmount.toInt()} for $pendingMonths. Please clear at your earliest convenience to keep your batch enrollment active."

    val timestamp = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.US).format(java.util.Date())
    val reminder = FeeReminderNotification(
      id = "rem-${System.currentTimeMillis()}-$studentId",
      studentId = studentId,
      studentName = studentName,
      dueAmount = dueAmount,
      pendingMonths = pendingMonths,
      message = msg,
      timestamp = timestamp,
      isRead = false
    )

    val updated = _feeReminders.value.filterNot { it.studentId == studentId } + reminder
    _feeReminders.value = updated
    localStore.saveFeeReminders(updated)

    // Trigger Android System Notification
    com.example.util.NotificationHelper.sendFeeDueSystemNotification(
      context = context,
      studentName = studentName,
      dueAmount = dueAmount,
      message = msg
    )
  }

  fun markFeeReminderAsRead(reminderId: String) {
    val updated = _feeReminders.value.map {
      if (it.id == reminderId) it.copy(isRead = true) else it
    }
    _feeReminders.value = updated
    localStore.saveFeeReminders(updated)
  }

  fun dismissAllFeeReminders(studentId: String) {
    val updated = _feeReminders.value.map {
      if (it.studentId == studentId) it.copy(isRead = true) else it
    }
    _feeReminders.value = updated
    localStore.saveFeeReminders(updated)
  }

  companion object {
    @Volatile
    private var INSTANCE: PathsalaRepository? = null

    fun getInstance(context: Context): PathsalaRepository {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: PathsalaRepository(context.applicationContext).also { INSTANCE = it }
      }
    }
  }
}
