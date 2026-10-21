package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class LocalDataStore(context: Context) {
  private val prefs: SharedPreferences = context.getSharedPreferences("pixel_pathsala_prefs", Context.MODE_PRIVATE)
  private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

  companion object {
    val INITIAL_CENTER = CenterInfo(
      id = "center-1",
      name = "Pixel Pathsala",
      tagLine = "West Bengal Premier Tuition & Coaching Center",
      address = "Raina, Purba Bardhaman, West Bengal",
      phone = "+91 9475934733",
      email = "admin@pixelpathsala.com",
      upiId = "suparnakantidatta@okaxis",
      upiName = "Pixel Pathsala Admin",
      websiteUrl = "https://pixelpathsala.com"
    )

    val INITIAL_ADMIN = AdminAccount(
      id = "ADM-001",
      username = "admin",
      fullName = "Director / Center Administrator",
      roleTitle = "Master Administrator",
      email = "admin@pixelpathsala.com",
      passwordHash = "admin123",
      securityPin = "7722",
      status = "active"
    )
  }

  private inline fun <reified T> getList(key: String, default: List<T>): List<T> {
    val json = prefs.getString(key, null) ?: return default
    return try {
      val type = Types.newParameterizedType(List::class.java, T::class.java)
      val adapter = moshi.adapter<List<T>>(type)
      adapter.fromJson(json) ?: default
    } catch (_: Exception) {
      default
    }
  }

  private inline fun <reified T> putList(key: String, list: List<T>) {
    try {
      val type = Types.newParameterizedType(List::class.java, T::class.java)
      val adapter = moshi.adapter<List<T>>(type)
      val json = adapter.toJson(list)
      prefs.edit().putString(key, json).apply()
    } catch (_: Exception) {}
  }

  private inline fun <reified T> getObject(key: String, default: T): T {
    val json = prefs.getString(key, null) ?: return default
    return try {
      val adapter = moshi.adapter(T::class.java)
      adapter.fromJson(json) ?: default
    } catch (_: Exception) {
      default
    }
  }

  private inline fun <reified T> putObject(key: String, obj: T) {
    try {
      val adapter = moshi.adapter(T::class.java)
      val json = adapter.toJson(obj)
      prefs.edit().putString(key, json).apply()
    } catch (_: Exception) {}
  }

  fun getCenterInfo(): CenterInfo = getObject("center_info", INITIAL_CENTER)
  fun saveCenterInfo(info: CenterInfo) = putObject("center_info", info)

  fun getStudents(): List<Student> = getList("students", emptyList())
  fun saveStudents(list: List<Student>) = putList("students", list)

  fun getCourses(): List<Course> = getList("courses", emptyList())
  fun saveCourses(list: List<Course>) = putList("courses", list)

  fun getBatches(): List<Batch> = getList("batches", emptyList())
  fun saveBatches(list: List<Batch>) = putList("batches", list)

  fun getEducators(): List<Educator> = getList("educators", emptyList())
  fun saveEducators(list: List<Educator>) = putList("educators", list)

  fun getPayments(): List<FeePayment> = getList("payments", emptyList())
  fun savePayments(list: List<FeePayment>) = putList("payments", list)

  fun getAdmissions(): List<AdmissionApplication> = getList("admissions", emptyList())
  fun saveAdmissions(list: List<AdmissionApplication>) = putList("admissions", list)

  fun getDiscounts(): List<Discount> = getList("discounts_list", emptyList())
  fun saveDiscounts(list: List<Discount>) = putList("discounts_list", list)

  fun getLiveClasses(): List<LiveClassSession> = getList("live_classes", emptyList())
  fun saveLiveClasses(list: List<LiveClassSession>) = putList("live_classes", list)

  fun getClassRecordings(): List<ClassRecording> = getList("recordings", emptyList())
  fun saveClassRecordings(list: List<ClassRecording>) = putList("recordings", list)

  fun getStudyMaterials(): List<StudyMaterial> = getList("materials", emptyList())
  fun saveStudyMaterials(list: List<StudyMaterial>) = putList("materials", list)

  fun getExams(): List<OnlineExam> = getList("exams", emptyList())
  fun saveExams(list: List<OnlineExam>) = putList("exams", list)

  fun getQuestions(): List<ExamQuestion> = getList("questions", emptyList())
  fun saveQuestions(list: List<ExamQuestion>) = putList("questions", list)

  fun getExamSubmissions(): List<ExamSubmission> = getList("submissions", emptyList())
  fun saveExamSubmissions(list: List<ExamSubmission>) = putList("submissions", list)

  fun getAdminAccount(): AdminAccount = getObject("admin_account", INITIAL_ADMIN)
  fun saveAdminAccount(admin: AdminAccount) = putObject("admin_account", admin)

  fun getStudentTelemetries(): List<StudentTelemetry> = getList("telemetries", emptyList())
  fun saveStudentTelemetries(list: List<StudentTelemetry>) = putList("telemetries", list)

  fun getExpenses(): List<Expense> = getList("expenses", emptyList())
  fun saveExpenses(list: List<Expense>) = putList("expenses", list)

  fun getLoggedInStudent(): Student? {
    val json = prefs.getString("logged_in_student", null) ?: return null
    return try {
      moshi.adapter(Student::class.java).fromJson(json)
    } catch (_: Exception) {
      null
    }
  }

  fun saveLoggedInStudent(student: Student?) {
    if (student == null) {
      prefs.edit().remove("logged_in_student").apply()
    } else {
      putObject("logged_in_student", student)
    }
  }

  fun getLoggedInAdmin(): AdminAccount? {
    val json = prefs.getString("logged_in_admin", null) ?: return null
    return try {
      moshi.adapter(AdminAccount::class.java).fromJson(json)
    } catch (_: Exception) {
      null
    }
  }

  fun isDarkMode(): Boolean {
    return prefs.getBoolean("app_dark_mode", false)
  }

  fun setDarkMode(enabled: Boolean) {
    prefs.edit().putBoolean("app_dark_mode", enabled).apply()
  }

  fun saveLoggedInAdmin(admin: AdminAccount?) {
    if (admin == null) {
      prefs.edit().remove("logged_in_admin").apply()
    } else {
      putObject("logged_in_admin", admin)
    }
  }

  fun saveAll(dto: DatabaseDataDto) {
    dto.centerInfo?.let { saveCenterInfo(it) }
    dto.students?.let { if (it.isNotEmpty()) saveStudents(it) }
    dto.courses?.let { if (it.isNotEmpty()) saveCourses(it) }
    dto.batches?.let { if (it.isNotEmpty()) saveBatches(it) }
    dto.educators?.let { if (it.isNotEmpty()) saveEducators(it) }
    dto.payments?.let { if (it.isNotEmpty()) savePayments(it) }
    dto.admissions?.let { if (it.isNotEmpty()) saveAdmissions(it) }
    dto.discounts?.let { if (it.isNotEmpty()) saveDiscounts(it) }
    dto.liveClasses?.let { if (it.isNotEmpty()) saveLiveClasses(it) }
    dto.classRecordings?.let { if (it.isNotEmpty()) saveClassRecordings(it) }
    dto.studyMaterials?.let { if (it.isNotEmpty()) saveStudyMaterials(it) }
    dto.exams?.let { if (it.isNotEmpty()) saveExams(it) }
    dto.questions?.let { if (it.isNotEmpty()) saveQuestions(it) }
    dto.examSubmissions?.let { if (it.isNotEmpty()) saveExamSubmissions(it) }
    dto.studentTelemetries?.let { if (it.isNotEmpty()) saveStudentTelemetries(it) }
    dto.expenses?.let { if (it.isNotEmpty()) saveExpenses(it) }
    dto.adminAccounts?.firstOrNull()?.let { saveAdminAccount(it) }
  }
}
