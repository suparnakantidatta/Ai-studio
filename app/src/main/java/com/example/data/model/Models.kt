package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CenterInfo(
  val id: String = "center-1",
  val name: String = "Pixel Pathsala",
  val tagLine: String = "West Bengal Premier Tuition & Coaching Center",
  val address: String = "Raina, Purba Bardhaman, West Bengal",
  val phone: String = "+91 9475934733",
  val email: String = "admin@pixelpathsala.com",
  val upiId: String = "suparnakantidatta@okaxis",
  val upiName: String = "Pixel Pathsala Admin",
  val qrCodeUrl: String? = null,
  val websiteUrl: String = "https://pixelpathsala.com"
)

@JsonClass(generateAdapter = true)
data class Course(
  val id: String,
  val title: String,
  val code: String = "",
  val academicClass: String = "",
  val category: String = "General",
  val monthlyFee: Double = 400.0,
  val description: String? = null
)

@JsonClass(generateAdapter = true)
data class Batch(
  val id: String,
  val name: String,
  val courseId: String,
  val timings: String = "09:00 AM - 11:00 AM",
  val scheduleDays: String = "Sat, Sun",
  val educatorId: String = "edu-1",
  val mode: String = "offline",
  val maxCapacity: Int = 30
)

@JsonClass(generateAdapter = true)
data class Educator(
  val id: String,
  val name: String,
  val designation: String = "Faculty Member",
  val subject: String = "Computer Science",
  val phone: String = "",
  val email: String? = null,
  val qualification: String = "M.Sc., B.Ed"
)

@JsonClass(generateAdapter = true)
data class Student(
  val id: String,
  val rollNo: String,
  val name: String,
  val studentClass: String? = null,
  val mode: String = "offline",
  val mobile: String,
  val aadhaarNo: String,
  val email: String? = null,
  val guardianName: String = "Parent",
  val guardianPhone: String = "",
  val address: String = "",
  val courseId: String,
  val batchId: String,
  val admissionDate: String = "",
  val admissionMonth: String = "",
  val status: String = "active",
  val customMonthlyFeeOverride: Double? = null,
  val monthlyDiscount: Double? = null,
  val discountReason: String? = null,
  val totalPaidInSheet: Double? = null
)

@JsonClass(generateAdapter = true)
data class FeePayment(
  val id: String,
  val receiptNo: String,
  val studentId: String,
  val studentName: String,
  val studentAadhaar: String = "",
  val studentMobile: String = "",
  val courseTitle: String = "Course",
  val batchName: String = "Batch",
  val month: String = "",
  val monthsCovered: List<String> = emptyList(),
  val baseMonthlyFee: Double = 0.0,
  val totalBaseFee: Double = 0.0,
  val totalDiscount: Double = 0.0,
  val discountBreakdown: String? = null,
  val finalAmountPaid: Double,
  val remainingDue: Double = 0.0,
  val paymentMode: String = "CASH", // CASH, UPI, ONLINE
  val transactionRef: String? = null,
  val status: String = "approved", // approved, pending, rejected
  val paymentDate: String = "",
  val approvedBy: String? = null,
  val remarks: String? = null,
  val rejectionReason: String? = null,
  val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AdmissionApplication(
  val id: String,
  val applicationNo: String = "",
  val studentName: String,
  val mobile: String,
  val aadhaarNo: String,
  val email: String? = null,
  val guardianName: String,
  val guardianPhone: String,
  val targetCourseId: String,
  val status: String = "pending", // pending, approved, rejected
  val appliedDate: String = "",
  val initialPaymentStatus: String = "paid_advance",
  val initialPaymentAmount: Double? = null,
  val initialPaymentMode: String? = "CASH",
  val initialPaymentRef: String? = null,
  val remarks: String? = null,
  val rejectionReason: String? = null,
  val address: String = ""
)

@JsonClass(generateAdapter = true)
data class Discount(
  val id: String,
  val title: String,
  val type: String = "flat", // flat, percentage
  val value: Double = 0.0,
  val scope: String = "student", // all, class, course, batch, student
  val academicClass: String? = null,
  val courseId: String? = null,
  val batchId: String? = null,
  val studentId: String? = null,
  val applicableMonthsType: String = "all_months", // all_months, selected_months, single_month
  val selectedMonths: List<String>? = null,
  val active: Boolean = true,
  val reason: String? = null
)

@JsonClass(generateAdapter = true)
data class LiveClassSession(
  val id: String,
  val title: String,
  val subject: String = "General",
  val academicClass: String? = null,
  val courseId: String? = null,
  val batchId: String? = null,
  val educatorName: String = "Faculty",
  val scheduledDate: String = "",
  val startTime: String = "10:00 AM",
  val endTime: String = "12:00 PM",
  val status: String = "scheduled", // scheduled, live, completed, postponed
  val platform: String = "Google Meet",
  val meetingUrl: String? = "https://meet.google.com/new",
  val isPostponed: Boolean = false,
  val postponeReason: String? = null,
  val notificationSent: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ClassRecording(
  val id: String,
  val title: String,
  val subject: String = "General",
  val courseId: String? = null,
  val batchId: String? = null,
  val recordingUrl: String = "",
  val durationMinutes: Int = 60,
  val dateRecorded: String = "",
  val description: String? = null
)

@JsonClass(generateAdapter = true)
data class StudyMaterial(
  val id: String,
  val title: String,
  val subject: String = "General",
  val courseId: String? = null,
  val batchId: String? = null,
  val fileUrl: String = "",
  val fileType: String = "PDF",
  val uploadedDate: String = "",
  val description: String? = null
)

@JsonClass(generateAdapter = true)
data class ExamQuestion(
  val id: String,
  val examId: String,
  val questionText: String,
  val optionA: String,
  val optionB: String,
  val optionC: String,
  val optionD: String,
  val correctOption: String = "A",
  val marks: Int = 1
)

@JsonClass(generateAdapter = true)
data class OnlineExam(
  val id: String,
  val title: String,
  val subject: String = "General",
  val courseId: String? = null,
  val totalMarks: Int = 20,
  val durationMinutes: Int = 30,
  val scheduledDate: String = "",
  val status: String = "published"
)

@JsonClass(generateAdapter = true)
data class ExamSubmission(
  val id: String,
  val examId: String,
  val studentId: String,
  val studentName: String,
  val score: Int,
  val totalMarks: Int,
  val submittedAt: String = ""
)

@JsonClass(generateAdapter = true)
data class AdminAccount(
  val id: String = "ADM-001",
  val username: String = "admin",
  val fullName: String = "Director / Center Administrator",
  val roleTitle: String = "Master Administrator",
  val email: String = "admin@pixelpathsala.com",
  val passwordHash: String = "admin123",
  val securityPin: String = "7722",
  val status: String = "active"
)

@JsonClass(generateAdapter = true)
data class StudentTelemetry(
  val id: String,
  val studentId: String,
  val studentName: String,
  val latitude: Double = 23.2324,
  val longitude: Double = 87.8615,
  val locationAddress: String = "Raina, Purba Bardhaman",
  val deviceModel: String = "",
  val appVersion: String = "1.0",
  val batteryLevel: Int = 100,
  val isCharging: Boolean = false,
  val networkType: String = "WIFI",
  val installedGames: List<String> = emptyList(),
  val studyFocusScore: Int = 100,
  val lastHeartbeat: String = ""
)

@JsonClass(generateAdapter = true)
data class Expense(
  val id: String,
  val title: String,
  val category: String = "Utilities",
  val amount: Double = 0.0,
  val expenseDate: String = "",
  val paymentMode: String = "CASH",
  val approvedBy: String = "Admin",
  val remarks: String? = null
)

@JsonClass(generateAdapter = true)
data class FeeReminderNotification(
  val id: String,
  val studentId: String,
  val studentName: String,
  val dueAmount: Double,
  val pendingMonths: String,
  val message: String,
  val timestamp: String,
  val isRead: Boolean = false
)

@JsonClass(generateAdapter = true)
data class DatabaseDataDto(
  val centerInfo: CenterInfo? = null,
  val students: List<Student>? = null,
  val courses: List<Course>? = null,
  val batches: List<Batch>? = null,
  val educators: List<Educator>? = null,
  val payments: List<FeePayment>? = null,
  val admissions: List<AdmissionApplication>? = null,
  val discounts: List<Discount>? = null,
  val liveClasses: List<LiveClassSession>? = null,
  val classRecordings: List<ClassRecording>? = null,
  val studyMaterials: List<StudyMaterial>? = null,
  val exams: List<OnlineExam>? = null,
  val questions: List<ExamQuestion>? = null,
  val examSubmissions: List<ExamSubmission>? = null,
  val studentTelemetries: List<StudentTelemetry>? = null,
  val expenses: List<Expense>? = null,
  val adminAccounts: List<AdminAccount>? = null
)

// Computed Fee Models
data class MonthFeeDetail(
  val yearMonth: String, // e.g. "2026-07"
  val monthLabel: String, // e.g. "July 2026"
  val baseFee: Double,
  val discountAmount: Double,
  val discountName: String?,
  val netPayable: Double,
  val amountPaid: Double,
  val remainingDue: Double,
  val isPaid: Boolean
)

data class FeeDuesSummary(
  val studentId: String,
  val studentName: String,
  val courseTitle: String,
  val batchName: String,
  val admissionMonth: String,
  val standardMonthlyFee: Double,
  val totalBaseFee: Double,
  val totalDiscount: Double,
  val totalNetPayable: Double,
  val totalPaid: Double,
  val totalOutstandingDue: Double,
  val pendingMonths: List<MonthFeeDetail>,
  val paidMonths: List<MonthFeeDetail>,
  val allMonths: List<MonthFeeDetail>
)

data class DiscountResolution(
  val discountId: String,
  val discountTitle: String,
  val discountType: String,
  val value: Double,
  val calculatedDeduction: Double
)
