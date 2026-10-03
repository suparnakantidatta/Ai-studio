package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CenterInfo(
  val name: String = "Pixel Pathsala",
  val tagline: String = "Code your dream, decode the future",
  val logoUrl: String? = null,
  val bgUrl: String? = null,
  val address: String = "Raina Main Road, Near Bus Stand",
  val cityState: String = "Raina, Purba Bardhaman (Burdwan), West Bengal - 713424",
  val phonePrimary: String = "+91 9775708722",
  val phoneSecondary: String? = "+91 9832014567",
  val whatsapp: String? = "+91 9775708722",
  val email: String = "pixelpathsala.edu@gmail.com",
  val upiId: String = "9775708722@apl",
  val upiName: String = "Pixel Pathsala Center",
  val regNumber: String? = "PP/WB/2021",
  val establishedYear: String = "2021",
  val admissionOpen: Boolean = true,
  val announcement: String? = "Admissions Open for Session 2026-27! Free Registration & Merit Discounts available on all Batches.",
  val welcomeMessage: String = "Welcome to Pixel Pathsala - Premier Institute for Classwise School & College Syllabus, Competitive Exams, and Interactive Coding & DBMS Training."
)

@JsonClass(generateAdapter = true)
data class Student(
  val id: String,
  val rollNo: String,
  val name: String,
  val studentClass: String? = "Class 10 (Secondary)",
  val mode: String? = "offline",
  val mobile: String,
  val aadhaarNo: String,
  val email: String? = null,
  val guardianName: String = "Parent",
  val guardianPhone: String = "",
  val address: String = "Raina, Purba Bardhaman",
  val courseId: String = "course-1",
  val batchId: String = "batch-1",
  val admissionDate: String = "2026-04-01",
  val admissionMonth: String = "2026-04",
  val status: String = "active", // active, passed_out, dropped
  val notes: String? = null,
  val customMonthlyFeeOverride: Double? = null,
  val monthlyDiscount: Double? = null,
  val discountReason: String? = null,
  val totalPaidInSheet: Double? = null
)

@JsonClass(generateAdapter = true)
data class Course(
  val id: String,
  val title: String,
  val code: String,
  val academicClass: String = "Class 10 (Secondary)",
  val category: String? = "Computer Science & Coding",
  val monthlyFee: Double = 1200.0,
  val description: String = "",
  val syllabusPoints: List<String> = emptyList(),
  val highlights: List<String> = emptyList(),
  val isPopular: Boolean = false,
  val showOnWebsite: Boolean = true
)

@JsonClass(generateAdapter = true)
data class Batch(
  val id: String,
  val name: String,
  val courseId: String,
  val academicClass: String? = "Class 10 (Secondary)",
  val educatorId: String = "edu-1",
  val scheduleDays: List<String> = listOf("Mon", "Wed", "Fri"),
  val timing: String = "05:00 PM - 06:30 PM",
  val roomNumber: String = "Room 101",
  val maxCapacity: Int = 30,
  val isActive: Boolean = true,
  val mode: String = "offline", // offline, online
  val startDate: String = "2026-04-01"
)

@JsonClass(generateAdapter = true)
data class Educator(
  val id: String,
  val name: String,
  val qualification: String = "M.Tech (Computer Science)",
  val subject: String = "Computer Science & Coding",
  val phone: String = "9832104561",
  val email: String? = null,
  val monthlySalary: Double = 30000.0,
  val assignedBatchIds: List<String> = emptyList(),
  val photoUrl: String? = null,
  val experienceYears: Int = 8,
  val joiningDate: String? = "2022-01-15",
  val status: String = "active"
)

@JsonClass(generateAdapter = true)
data class FeePayment(
  val id: String,
  val receiptNo: String,
  val studentId: String,
  val studentName: String,
  val studentAadhaar: String = "",
  val studentMobile: String = "",
  val courseTitle: String = "Academic Course",
  val batchName: String = "Active Batch",
  val month: String = "April 2026",
  val monthsCovered: List<String> = listOf("2026-04"),
  val baseMonthlyFee: Double = 1200.0,
  val totalBaseFee: Double = 1200.0,
  val totalDiscount: Double = 0.0,
  val discountBreakdown: String? = null,
  val finalAmountPaid: Double = 1200.0,
  val remainingDue: Double = 0.0,
  val paymentMode: String = "UPI", // UPI, CASH, BANK_TRANSFER
  val transactionRef: String? = null,
  val status: String = "approved", // approved, pending, rejected
  val rejectionReason: String? = null,
  val paymentDate: String = "2026-04-05",
  val approvedBy: String? = "Admin",
  val remarks: String? = null,
  val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class Expense(
  val id: String,
  val category: String = "General",
  val title: String,
  val amount: Double,
  val date: String,
  val paidTo: String = "Vendor",
  val paymentMode: String = "UPI",
  val receiptNo: String? = null,
  val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class AdmissionApplication(
  val id: String,
  val applicationNo: String = "",
  val studentName: String,
  val studentClass: String? = "Class 10 (Secondary)",
  val mobile: String,
  val aadhaarNo: String,
  val email: String? = null,
  val guardianName: String = "Parent",
  val guardianPhone: String = "",
  val address: String = "Raina, Purba Bardhaman",
  val targetCourseId: String = "course-1",
  val preferredBatchId: String? = null,
  val status: String = "pending", // pending, approved, rejected
  val appliedDate: String = "2026-08-15",
  val initialPaymentStatus: String = "paid_advance",
  val initialPaymentMode: String? = "UPI",
  val initialPaymentRef: String? = null,
  val initialPaymentAmount: Double? = null,
  val remarks: String? = null,
  val rejectionReason: String? = null
)

@JsonClass(generateAdapter = true)
data class Discount(
  val id: String,
  val title: String,
  val type: String = "flat", // flat, percentage
  val value: Double = 200.0,
  val scope: String = "batch", // all, class, course, batch, student
  val academicClass: String? = null,
  val courseId: String? = null,
  val batchId: String? = null,
  val studentId: String? = null,
  val applicableMonthsType: String = "all_months",
  val selectedMonths: List<String>? = null,
  val active: Boolean = true,
  val reason: String? = null
)

@JsonClass(generateAdapter = true)
data class StudyMaterial(
  val id: String,
  val title: String,
  val description: String? = null,
  val batchId: String = "all",
  val courseId: String = "all",
  val subject: String? = "Computer Science",
  val fileType: String = "pdf", // pdf, doc, zip, link
  val fileUrl: String = "",
  val fileName: String = "Document.pdf",
  val fileSize: String? = "1.5 MB",
  val uploadedBy: String = "Faculty",
  val uploadedDate: String = "2026-04-05",
  val downloadCount: Int = 10
)

@JsonClass(generateAdapter = true)
data class ExamQuestion(
  val id: String,
  val courseId: String = "all",
  val academicClass: String? = "Class 10",
  val subject: String = "Programming",
  val topic: String = "Core Concepts",
  val marks: Int = 2,
  val difficulty: String = "medium",
  val questionText: String,
  val options: List<String> = emptyList(),
  val correctOptionIndex: Int = 0,
  val codeSnippet: String? = null,
  val explanation: String? = null
)

@JsonClass(generateAdapter = true)
data class OnlineExam(
  val id: String,
  val examCode: String = "EX-101",
  val title: String,
  val courseId: String = "all",
  val batchId: String = "all",
  val academicClass: String? = "Class 10",
  val subject: String = "Computer Science",
  val durationMinutes: Int = 30,
  val totalMarks: Int = 20,
  val passingMarks: Int = 8,
  val passingPercentage: Int = 40,
  val status: String = "active", // scheduled, active, completed
  val mode: String = "online",
  val scheduledDate: String = "2026-08-30",
  val startTime: String = "10:00 AM",
  val endTime: String = "08:00 PM",
  val questionIds: List<String> = emptyList(),
  val instructions: String? = "Read each question carefully.",
  val isPublished: Boolean = true
)

@JsonClass(generateAdapter = true)
data class ExamSubmission(
  val id: String,
  val examId: String,
  val examTitle: String,
  val studentId: String,
  val studentName: String,
  val studentRollNo: String? = null,
  val score: Int = 0,
  val totalMarks: Int = 20,
  val percentage: Int = 0,
  val passed: Boolean = false,
  val timeSpentSeconds: Int = 0,
  val submittedAt: String = ""
)

@JsonClass(generateAdapter = true)
data class LiveClassSession(
  val id: String,
  val title: String,
  val academicClass: String? = "Class 10 (Secondary)",
  val targetClass: String? = "Class 10 (Secondary)",
  val subject: String = "Computer Science",
  val courseId: String = "course-1",
  val batchId: String = "batch-1",
  val educatorName: String = "Prof. Subhasish Mukherjee",
  val scheduledDate: String = "2026-09-01",
  val startTime: String = "05:00 PM",
  val endTime: String = "06:30 PM",
  val status: String = "scheduled", // scheduled, live, completed
  val platform: String = "google_meet", // google_meet, zoom, in_app
  val meetingUrl: String? = "https://meet.google.com/new",
  val isRecordingAvailable: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ClassRecording(
  val id: String,
  val title: String,
  val academicClass: String? = "Class 10 (Secondary)",
  val targetClass: String? = "Class 10 (Secondary)",
  val subject: String = "Computer Science",
  val topic: String? = "Python Functions & Logic",
  val chapter: String? = "Module 1",
  val courseId: String = "course-1",
  val batchId: String = "batch-1",
  val educatorName: String = "Prof. Subhasish Mukherjee",
  val recordedDate: String = "2026-08-25",
  val durationMinutes: Int = 60,
  val videoSourceType: String = "youtube", // youtube, drive, mp4
  val videoUrl: String = "https://www.youtube.com/watch?v=kUMe1FH4CHE",
  val notesPdfUrl: String? = null,
  val viewCount: Int = 25
)

@JsonClass(generateAdapter = true)
data class AdminAccount(
  val id: String = "ADM-001",
  val username: String = "admin",
  val password: String = "admin123",
  val name: String = "Director / Center Administrator",
  val role: String = "Master Administrator",
  val email: String = "admin@pixelpathsala.com",
  val phone: String = "+91 9775708722",
  val securityPin: String = "7722",
  val status: String = "active",
  val lastLogin: String? = null,
  val updatedAt: String? = null
)

// API Request/Response Transfer Objects
@JsonClass(generateAdapter = true)
data class AdminLoginRequest(
  val username: String,
  val password: String
)

@JsonClass(generateAdapter = true)
data class AdminUserDto(
  val id: String,
  val username: String,
  val name: String,
  val role: String,
  val email: String,
  val phone: String? = null,
  val lastLogin: String? = null
)

@JsonClass(generateAdapter = true)
data class AdminLoginResponse(
  val success: Boolean,
  val token: String? = null,
  val message: String? = null,
  val adminUser: AdminUserDto? = null
)

@JsonClass(generateAdapter = true)
data class StudentLoginRequest(
  val mobile: String,
  val aadhaarNo: String
)

@JsonClass(generateAdapter = true)
data class StudentLoginResponse(
  val success: Boolean,
  val token: String? = null,
  val message: String? = null,
  val student: Student? = null,
  val isPendingAdmission: Boolean? = null,
  val application: AdmissionApplication? = null
)

@JsonClass(generateAdapter = true)
data class DatabaseDataDto(
  val centerInfo: CenterInfo? = null,
  val students: List<Student>? = null,
  val admissions: List<AdmissionApplication>? = null,
  val payments: List<FeePayment>? = null,
  val courses: List<Course>? = null,
  val batches: List<Batch>? = null,
  val educators: List<Educator>? = null,
  val expenses: List<Expense>? = null,
  val discounts: List<Discount>? = null,
  val adminAccounts: List<AdminAccount>? = null,
  val studyMaterials: List<StudyMaterial>? = null,
  val questions: List<ExamQuestion>? = null,
  val exams: List<OnlineExam>? = null,
  val examSubmissions: List<ExamSubmission>? = null,
  val liveClasses: List<LiveClassSession>? = null,
  val classRecordings: List<ClassRecording>? = null
)

@JsonClass(generateAdapter = true)
data class LoadAllResponse(
  val success: Boolean,
  val fromLiveGoogleSheet: Boolean? = null,
  val fromCache: Boolean? = null,
  val data: DatabaseDataDto? = null,
  val counts: Map<String, Int>? = null,
  val lastSynced: String? = null,
  val message: String? = null
)

@JsonClass(generateAdapter = true)
data class BaseApiResponse(
  val success: Boolean,
  val message: String? = null,
  val error: String? = null
)

@JsonClass(generateAdapter = true)
data class MutateRequest(
  val action: String,
  val payload: Map<String, Any?> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class PayFeeRequest(
  val studentId: String,
  val amount: Double,
  val month: String,
  val paymentMode: String = "UPI",
  val transactionRef: String? = null
)

@JsonClass(generateAdapter = true)
data class AdmissionApplyRequest(
  val name: String,
  val mobile: String,
  val guardianName: String? = null,
  val courseId: String? = null,
  val aadhaarNo: String? = null,
  val address: String? = null,
  val remarks: String? = null
)

@JsonClass(generateAdapter = true)
data class AdminDashboardStats(
  val totalStudents: Int = 0,
  val activeBatches: Int = 0,
  val totalCollected: Double = 0.0,
  val pendingApprovals: Int = 0,
  val pendingAdmissions: Int = 0
)

@JsonClass(generateAdapter = true)
data class StudentFeesResponse(
  val success: Boolean = true,
  val totalPaid: Double = 0.0,
  val totalDue: Double = 0.0,
  val payments: List<FeePayment> = emptyList(),
  val dueMonths: List<String> = emptyList()
)
