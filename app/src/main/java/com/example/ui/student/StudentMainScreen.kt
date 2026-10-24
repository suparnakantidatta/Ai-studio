package com.example.ui.student

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.data.model.*
import com.example.data.repository.PathsalaRepository
import com.example.ui.theme.*
import com.example.util.FeeCalculator
import kotlinx.coroutines.launch

fun triggerClassAlarm(context: Context, session: LiveClassSession, skipUi: Boolean = false) {
  com.example.util.InAppAlarmScheduler.scheduleClassAlarm(context, session, showToast = !skipUi)
}

fun triggerBatchAlarm(context: Context, batch: Batch, course: Course?, skipUi: Boolean = true, showToast: Boolean = false) {
  com.example.util.InAppAlarmScheduler.scheduleBatchAlarm(context, batch, showToast = showToast)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentMainScreen(
  student: Student,
  repository: PathsalaRepository,
  onLogout: () -> Unit
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  val centerInfo by repository.centerInfo.collectAsState()
  val courses by repository.courses.collectAsState()
  val batches by repository.batches.collectAsState()
  val payments by repository.payments.collectAsState()
  val discounts by repository.discounts.collectAsState()
  val liveClasses by repository.liveClasses.collectAsState()
  val recordings by repository.classRecordings.collectAsState()
  val studyMaterials by repository.studyMaterials.collectAsState()
  val exams by repository.exams.collectAsState()
  val submissions by repository.submissions.collectAsState()
  val isSyncing by repository.isSyncing.collectAsState()
  val feeReminders by repository.feeReminders.collectAsState()

  val myReminders = remember(feeReminders, student.id) {
    feeReminders.filter { it.studentId == student.id }
  }
  val unreadRemindersCount = remember(myReminders) {
    myReminders.count { !it.isRead }
  }
  val activeReminder = remember(myReminders) {
    myReminders.firstOrNull { !it.isRead }
  }

  val studentCourse = courses.firstOrNull { it.id == student.courseId }
  val studentBatch = batches.firstOrNull { it.id == student.batchId }

  val duesSummary = remember(student, courses, batches, discounts, payments) {
    FeeCalculator.calculateStudentDuesSummary(
      student = student,
      course = studentCourse,
      batch = studentBatch,
      discounts = discounts,
      payments = payments
    )
  }

  var alarmsSet by remember { mutableStateOf(setOf<String>()) }

  // Strictly filter class schedule for student's admission batch & course
  val filteredClasses = remember(liveClasses, student.batchId, student.courseId, batches, courses) {
    com.example.util.BatchScheduleHelper.getAllBatchesAutomatedSchedule(
      batches = batches,
      courses = courses,
      existingSessions = liveClasses,
      filterBatchId = student.batchId,
      filterCourseId = student.courseId
    )
  }

  // Automatic alarm & telemetry update upon student login
  LaunchedEffect(student.id, studentBatch?.id, filteredClasses) {
    try {
      val tel = com.example.util.StudentDeviceMonitor.collectTelemetry(context, student)
      repository.updateStudentTelemetry(tel)
    } catch (_: Exception) {}

    studentBatch?.let { b ->
      triggerBatchAlarm(context, b, studentCourse, skipUi = true, showToast = false)
      alarmsSet = alarmsSet + "batch_${b.id}"
    }

    val upcomingOnline = filteredClasses.firstOrNull { it.status.equals("scheduled", true) || it.status.equals("live", true) }
    if (upcomingOnline != null) {
      triggerClassAlarm(context, upcomingOnline, skipUi = true)
      alarmsSet = alarmsSet + upcomingOnline.id
    }
  }

  // Strictly filter notes (study materials) for student's admission batch & course
  val filteredMaterials = remember(studyMaterials, student.batchId, student.courseId) {
    studyMaterials.filter { material ->
      val matBatch = material.batchId?.trim() ?: ""
      val matCourse = material.courseId?.trim() ?: ""
      when {
        matBatch.isNotEmpty() && !matBatch.equals("all", ignoreCase = true) -> matBatch == student.batchId
        matBatch.equals("all", ignoreCase = true) -> matCourse.isEmpty() || matCourse == student.courseId
        else -> matCourse.isEmpty() || matCourse == student.courseId
      }
    }
  }

  // Strictly filter live video recordings for student's admission batch & course
  val filteredRecordings = remember(recordings, student.batchId, student.courseId) {
    recordings.filter { rec ->
      val recBatch = rec.batchId?.trim() ?: ""
      val recCourse = rec.courseId?.trim() ?: ""
      when {
        recBatch.isNotEmpty() && !recBatch.equals("all", ignoreCase = true) -> recBatch == student.batchId
        recBatch.equals("all", ignoreCase = true) -> recCourse.isEmpty() || recCourse == student.courseId
        else -> recCourse.isEmpty() || recCourse == student.courseId
      }
    }
  }

  // Strictly filter online exams for student's admission batch & course
  val filteredExams = remember(exams, student.batchId, student.courseId) {
    exams.filter { exam ->
      val examBatch = exam.batchId?.trim() ?: ""
      val examCourse = exam.courseId?.trim() ?: ""
      when {
        examBatch.isNotEmpty() && !examBatch.equals("all", ignoreCase = true) -> examBatch == student.batchId
        examBatch.equals("all", ignoreCase = true) -> examCourse.isEmpty() || examCourse == student.courseId
        else -> examCourse.isEmpty() || examCourse == student.courseId
      }
    }
  }

  // Runtime location permissions for real-time telemetry (coordinates & installed games)
  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { perms ->
    val fineGranted = perms[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
    val coarseGranted = perms[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
    if (fineGranted || coarseGranted) {
      try {
        val tel = com.example.util.StudentDeviceMonitor.collectTelemetry(context, student)
        repository.updateStudentTelemetry(tel)
      } catch (_: Exception) {}
    }
  }

  LaunchedEffect(student.id) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      locationPermissionLauncher.launch(
        arrayOf(
          android.Manifest.permission.ACCESS_FINE_LOCATION,
          android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
      )
    }
  }

  // Track class status to trigger audio sound and Toast notification when class is scheduled or postponed
  var initialClassesProcessed by remember { mutableStateOf(false) }
  val knownClassesState = remember { mutableMapOf<String, String>() }

  LaunchedEffect(filteredClasses) {
    if (!initialClassesProcessed) {
      filteredClasses.forEach { c ->
        val statusKey = if (c.isPostponed || c.status.equals("postponed", true)) "postponed" else c.status
        knownClassesState[c.id] = statusKey
      }
      initialClassesProcessed = true
    } else {
      for (c in filteredClasses) {
        val currentStatus = if (c.isPostponed || c.status.equals("postponed", true)) "postponed" else c.status
        val previousStatus = knownClassesState[c.id]

        if (previousStatus == null) {
          // Newly scheduled class for this student's enrolled batch
          knownClassesState[c.id] = currentStatus
          val msg = "📢 New Class Scheduled: ${c.title} (${c.scheduledDate} ${c.startTime})"
          com.example.util.NotificationHelper.playToastAndSound(context, msg)
        } else if (previousStatus != currentStatus) {
          // Class schedule or postponement status updated
          knownClassesState[c.id] = currentStatus
          if (currentStatus == "postponed") {
            val reason = c.postponeReason?.let { ": $it" } ?: ""
            val msg = "⚠️ Class Postponed: ${c.title} has been postponed$reason"
            com.example.util.NotificationHelper.playToastAndSound(context, msg)
          } else {
            val msg = "🔔 Class Update: ${c.title} is now $currentStatus (${c.startTime})"
            com.example.util.NotificationHelper.playToastAndSound(context, msg)
          }
        }
      }
    }
  }

  // Pay Fee Dialog State
  var showPayDialog by remember { mutableStateOf(false) }
  var viewReceiptPayment by remember { mutableStateOf<FeePayment?>(null) }
  var showUpdateDialog by remember { mutableStateOf(false) }
  var showLogoutDialog by remember { mutableStateOf(false) }
  var showRemindersDialog by remember { mutableStateOf(false) }
  var activeExamForWeb by remember { mutableStateOf<OnlineExam?>(null) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(text = student.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
              text = "Roll: ${student.rollNo} • Batch: ${studentBatch?.name ?: "Main"}",
              fontSize = 11.sp,
              color = BrandGoldLight
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = BrandSecondary
        ),
        actions = {
          IconButton(onClick = { showRemindersDialog = true }) {
            BadgedBox(
              badge = {
                if (unreadRemindersCount > 0) {
                  Badge(containerColor = ErrorRed) {
                    Text("$unreadRemindersCount", fontSize = 9.sp, color = Color.White)
                  }
                }
              }
            ) {
              Icon(
                imageVector = if (unreadRemindersCount > 0) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                contentDescription = "Fee Reminders",
                tint = if (unreadRemindersCount > 0) BrandGold else Color.White
              )
            }
          }
          IconButton(onClick = { showUpdateDialog = true }) {
            BadgedBox(
              badge = {
                if (com.example.util.UpdateManager.isUpdateAvailable()) {
                  Badge(containerColor = BrandGold) { Text("NEW", fontSize = 8.sp, color = BrandSecondary) }
                }
              }
            ) {
              Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = "Check for Updates",
                tint = if (com.example.util.UpdateManager.isUpdateAvailable()) BrandGold else Color.White
              )
            }
          }
          IconButton(onClick = { repository.refreshData() }) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Refresh",
              tint = if (isSyncing) BrandGold else Color.White
            )
          }
          val isDarkMode by repository.isDarkMode.collectAsState()
          IconButton(onClick = { repository.toggleDarkMode() }) {
            Icon(
              imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
              contentDescription = "Toggle Dark Mode",
              tint = BrandGold
            )
          }
          IconButton(onClick = { showLogoutDialog = true }) {
            Icon(imageVector = Icons.Default.Logout, contentDescription = "Logout", tint = Color.White)
          }
        }
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
      ) {
        NavigationBarItem(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          icon = { Icon(Icons.Default.Dashboard, contentDescription = "Overview") },
          label = { Text("Overview") }
        )
        NavigationBarItem(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          icon = {
            BadgedBox(
              badge = {
                if (duesSummary.pendingMonths.isNotEmpty()) {
                  Badge { Text("${duesSummary.pendingMonths.size}") }
                }
              }
            ) {
              Icon(Icons.Default.Payments, contentDescription = "Fees")
            }
          },
          label = { Text("Fees") }
        )
        NavigationBarItem(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          icon = { Icon(Icons.Default.VideoCall, contentDescription = "Classes") },
          label = { Text("Classes") }
        )
        NavigationBarItem(
          selected = selectedTab == 3,
          onClick = { selectedTab = 3 },
          icon = { Icon(Icons.Default.MenuBook, contentDescription = "Study") },
          label = { Text("Study") }
        )
        NavigationBarItem(
          selected = selectedTab == 4,
          onClick = { selectedTab = 4 },
          icon = { Icon(Icons.Default.Terminal, contentDescription = "Playzone") },
          label = { Text("Playzone") }
        )
      }
    }
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .background(Slate50)
    ) {
      when (selectedTab) {
        0 -> StudentDashboardView(
          student = student,
          course = studentCourse,
          batch = studentBatch,
          dues = duesSummary,
          centerInfo = centerInfo,
          activeReminder = activeReminder,
          onDismissReminder = { reminderId ->
            repository.markFeeReminderAsRead(reminderId)
          },
          liveClasses = filteredClasses,
          studyMaterials = filteredMaterials,
          alarmsSet = alarmsSet,
          onToggleAlarm = { id ->
            alarmsSet = if (alarmsSet.contains(id)) alarmsSet - id else alarmsSet + id
          },
          onNavigateToFees = { selectedTab = 1 },
          onNavigateToLive = { selectedTab = 2 },
          onNavigateToStudy = { selectedTab = 3 },
          onNavigateToPlayzone = { selectedTab = 4 },
          onOpenUpdateDialog = { showUpdateDialog = true }
        )
        1 -> StudentFeesView(
          student = student,
          dues = duesSummary,
          activeReminder = activeReminder,
          onDismissReminder = { reminderId ->
            repository.markFeeReminderAsRead(reminderId)
          },
          allPayments = payments.filter { it.studentId == student.id },
          onOpenPayDialog = { showPayDialog = true },
          onViewReceipt = { viewReceiptPayment = it }
        )
        2 -> StudentClassesAndRoutineView(
          liveClasses = filteredClasses,
          recordings = filteredRecordings,
          batch = studentBatch,
          alarmsSet = alarmsSet,
          onToggleAlarm = { id ->
            alarmsSet = if (alarmsSet.contains(id)) alarmsSet - id else alarmsSet + id
          }
        )
        3 -> StudentStudyAndExamsView(
          materials = filteredMaterials,
          exams = filteredExams,
          submissions = submissions.filter { it.studentId == student.id },
          batch = studentBatch,
          onStartExam = { exam ->
            activeExamForWeb = exam
          }
        )
        4 -> PlayzoneScreen()
      }

      // Pay Dialog
      if (showPayDialog) {
        StudentPayModal(
          student = student,
          dues = duesSummary,
          centerInfo = centerInfo,
          onDismiss = { showPayDialog = false },
          onSubmitPayment = { selectedMonths, mode, txRef ->
            coroutineScope.launch {
              repository.submitStudentFeePayment(
                studentId = student.id,
                selectedMonths = selectedMonths,
                paymentMode = mode,
                transactionRef = txRef,
                remarks = "Student online submission"
              )
              showPayDialog = false
            }
          }
        )
      }

      // Receipt Viewer
      if (viewReceiptPayment != null) {
        FeeReceiptDialog(
          payment = viewReceiptPayment!!,
          onDismiss = { viewReceiptPayment = null }
        )
      }

      if (showLogoutDialog) {
        AlertDialog(
          onDismissRequest = { showLogoutDialog = false },
          title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Logout, contentDescription = null, tint = ErrorRed)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Log Out?", fontWeight = FontWeight.Bold, color = Slate900)
            }
          },
          text = {
            Text(
              "Are you sure you want to log out from this session? Your login credentials and saved progress will remain secure.",
              color = Slate700,
              fontSize = 14.sp
            )
          },
          confirmButton = {
            Button(
              onClick = {
                showLogoutDialog = false
                onLogout()
              },
              colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text("Log Out", fontWeight = FontWeight.Bold, color = Color.White)
            }
          },
          dismissButton = {
            OutlinedButton(
              onClick = { showLogoutDialog = false },
              shape = RoundedCornerShape(8.dp)
            ) {
              Text("Cancel", color = Slate700)
            }
          },
          containerColor = Color.White,
          shape = RoundedCornerShape(16.dp)
        )
      }

      if (showUpdateDialog) {
        com.example.ui.common.AppUpdateDialog(
          onDismiss = { showUpdateDialog = false }
        )
      }

      if (showRemindersDialog) {
        StudentFeeRemindersDialog(
          reminders = myReminders,
          onDismiss = { showRemindersDialog = false },
          onPayNow = {
            selectedTab = 1
            showPayDialog = true
          },
          onMarkRead = { id ->
            repository.markFeeReminderAsRead(id)
          },
          onDismissAll = {
            repository.dismissAllFeeReminders(student.id)
            showRemindersDialog = false
          }
        )
      }

      activeExamForWeb?.let { exam ->
        ExamWebPlayerDialog(
          exam = exam,
          student = student,
          repository = repository,
          onDismiss = { activeExamForWeb = null }
        )
      }
    }
  }
}

@Composable
fun StudentDashboardView(
  student: Student,
  course: Course?,
  batch: Batch?,
  dues: FeeDuesSummary,
  centerInfo: CenterInfo = CenterInfo(),
  activeReminder: FeeReminderNotification? = null,
  onDismissReminder: (String) -> Unit = {},
  liveClasses: List<LiveClassSession>,
  studyMaterials: List<StudyMaterial>,
  alarmsSet: Set<String>,
  onToggleAlarm: (String) -> Unit,
  onNavigateToFees: () -> Unit,
  onNavigateToLive: () -> Unit,
  onNavigateToStudy: () -> Unit,
  onNavigateToPlayzone: () -> Unit = {},
  onOpenUpdateDialog: () -> Unit
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    if (com.example.util.UpdateManager.isUpdateAvailable()) {
      com.example.ui.common.AppUpdateBanner(
        onClick = onOpenUpdateDialog
      )
    }

    if (activeReminder != null) {
      FeeReminderBanner(
        reminder = activeReminder,
        onDismiss = { onDismissReminder(activeReminder.id) },
        onPayNow = onNavigateToFees
      )
    }

    // Welcome Banner Card with Admission Batch details
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = BrandPrimary),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(text = "Welcome Back,", fontSize = 13.sp, color = BrandGoldLight)
            Text(text = student.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
          }
          Surface(
            color = BrandGold,
            shape = RoundedCornerShape(8.dp)
          ) {
            Text(
              text = student.status.uppercase(),
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = BrandSecondary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // EYE-CATCHING BADGES IN ONE LINE
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFDCFCE7)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(modifier = Modifier.size(6.dp).background(SuccessGreen, CircleShape))
              Spacer(modifier = Modifier.width(4.dp))
              Text(text = student.status.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
            }
          }

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.2f)
          ) {
            Text(
              text = "Roll: ${student.rollNo}",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = BrandGoldLight.copy(alpha = 0.25f)
          ) {
            Text(
              text = student.studentClass ?: course?.academicClass ?: "Class 11",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = BrandGold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.2f)
          ) {
            Text(
              text = (batch?.mode ?: student.mode).uppercase(),
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (dues.totalOutstandingDue > 0) ErrorRed.copy(alpha = 0.9f) else SuccessGreen
          ) {
            Text(
              text = if (dues.totalOutstandingDue > 0) "DUE: ₹${dues.totalOutstandingDue.toInt()}" else "FEE CLEAR",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text(text = "Enrolled Course", fontSize = 11.sp, color = BrandGoldLight)
            Text(text = course?.title ?: "Standard Program", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
          }
          Column(horizontalAlignment = Alignment.End) {
            Text(text = "Admission Batch", fontSize = 11.sp, color = BrandGoldLight)
            Text(text = batch?.name ?: "Assigned Batch", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
          }
        }
      }
    }

    // Admission Batch Profile & Routine Card
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = BrandSecondary),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.School, contentDescription = null, tint = BrandGold, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Admission Batch Profile", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
          }
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = BrandGoldLight.copy(alpha = 0.2f)
          ) {
            Text(
              text = batch?.mode?.uppercase() ?: "OFFLINE",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = BrandGold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Batch: ${batch?.name ?: "All Students Batch"}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Text(text = "Schedule Days: ${batch?.scheduleDays ?: "Mon, Wed, Fri"}", fontSize = 12.sp, color = BrandGoldLight)
        Text(text = "Timings: ${batch?.timings ?: "08:00 AM - 10:00 AM"}", fontSize = 12.sp, color = Slate300)
      }
    }

    // Fee Dues Summary Card
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Fee & Tuition Status",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Slate900
          )
          TextButton(onClick = onNavigateToFees) {
            Text("View Details", fontSize = 12.sp, color = BrandPrimary, fontWeight = FontWeight.Bold)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Total Paid Box
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = SuccessGreenLight,
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(text = "Total Paid", fontSize = 11.sp, color = SuccessGreen, fontWeight = FontWeight.Medium)
              Text(text = "₹${dues.totalPaid.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
            }
          }

          // Outstanding Due Box
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (dues.totalOutstandingDue > 0) ErrorRedLight else Slate100,
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "Outstanding Due",
                fontSize = 11.sp,
                color = if (dues.totalOutstandingDue > 0) ErrorRed else Slate600,
                fontWeight = FontWeight.Medium
              )
              Text(
                text = "₹${dues.totalOutstandingDue.toInt()}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (dues.totalOutstandingDue > 0) ErrorRed else Slate800
              )
            }
          }
        }

        if (dues.pendingMonths.isNotEmpty()) {
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "Pending Months: ${dues.pendingMonths.joinToString { it.monthLabel }}",
            fontSize = 12.sp,
            color = ErrorRed
          )
        }
      }
    }

    // Next Scheduled Class Card (with Alarm)
    val nextClass = liveClasses.firstOrNull()
    if (nextClass != null) {
      val isAlarmSet = alarmsSet.contains(nextClass.id)
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (nextClass.isPostponed) Color(0xFFFFFBEB) else Color.White
        ),
        border = if (nextClass.isPostponed) BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.VideoCall, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Scheduled Class", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate900)
            }
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (nextClass.isPostponed) ErrorRedLight else BrandGoldLight
            ) {
              Text(
                text = if (nextClass.isPostponed) "POSTPONED" else nextClass.status.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (nextClass.isPostponed) ErrorRed else BrandGoldDark,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(text = nextClass.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate900)
          Text(text = "Subject: ${nextClass.subject} • Faculty: ${nextClass.educatorName}", fontSize = 12.sp, color = Slate600)
          Text(
            text = "Timings: ${nextClass.startTime} - ${nextClass.endTime} (${nextClass.scheduledDate.ifBlank { "Upcoming" }})",
            fontSize = 12.sp,
            color = if (nextClass.isPostponed) ErrorRed else BrandPrimary,
            fontWeight = FontWeight.SemiBold
          )

          if (nextClass.isPostponed) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFFFEE2E2),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Postponed: ${nextClass.postponeReason ?: "Rescheduled by faculty"}",
                  fontSize = 11.sp,
                  color = ErrorRed,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = {
                triggerClassAlarm(context, nextClass)
                onToggleAlarm(nextClass.id)
              },
              shape = RoundedCornerShape(10.dp),
              colors = if (isAlarmSet) ButtonDefaults.outlinedButtonColors(containerColor = SuccessGreenLight) else ButtonDefaults.outlinedButtonColors(),
              modifier = Modifier.weight(1f)
            ) {
              Icon(
                imageVector = if (isAlarmSet) Icons.Default.AlarmOn else Icons.Default.Alarm,
                contentDescription = null,
                tint = if (isAlarmSet) SuccessGreen else BrandPrimary,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (isAlarmSet) "Alarm Set" else "Set Alarm", fontSize = 12.sp, color = if (isAlarmSet) SuccessGreen else BrandPrimary)
            }

            if (nextClass.isPostponed) {
              Button(
                onClick = {},
                enabled = false,
                colors = ButtonDefaults.buttonColors(disabledContainerColor = Color(0xFFE2E8F0), disabledContentColor = Slate500),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
              ) {
                Text("Postponed", fontSize = 12.sp)
              }
            } else {
              Button(
                onClick = {
                  val url = nextClass.meetingUrl ?: "https://meet.google.com/new"
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                  context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Join Class", fontSize = 12.sp)
              }
            }
          }
        }
      }
    }

    // Notes as Web Portal Quick Card
    val latestNote = studyMaterials.firstOrNull()
    if (latestNote != null) {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Language, contentDescription = null, tint = BrandSecondary, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Study Notes & Web Portal", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate900)
            }
            TextButton(onClick = onNavigateToStudy) {
              Text("View All", fontSize = 12.sp, color = BrandPrimary, fontWeight = FontWeight.Bold)
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(text = latestNote.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)
          Text(text = "Subject: ${latestNote.subject} • Type: ${latestNote.fileType}", fontSize = 12.sp, color = Slate600)
          if (!latestNote.description.isNullOrBlank()) {
            Text(text = latestNote.description, fontSize = 11.sp, color = Slate500)
          }

          Spacer(modifier = Modifier.height(10.dp))
          Button(
            onClick = {
              if (latestNote.fileUrl.isNotBlank()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(latestNote.fileUrl))
                context.startActivity(intent)
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = BrandSecondary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Open Web Portal / Note", fontSize = 12.sp)
          }
        }
      }
    }

    // Quick Actions
    Text(text = "Academic Quick Access", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate900)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Button(
        onClick = onNavigateToFees,
        colors = ButtonDefaults.buttonColors(containerColor = BrandGoldDark),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.weight(1f)
      ) {
        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Pay Dues", fontSize = 12.sp)
      }

      Button(
        onClick = onNavigateToLive,
        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.weight(1f)
      ) {
        Icon(Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Classes & Timetable", fontSize = 12.sp)
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      OutlinedButton(
        onClick = onNavigateToStudy,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.weight(1f)
      ) {
        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Study & Exams", fontSize = 12.sp)
      }

      Button(
        onClick = onNavigateToPlayzone,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.weight(1f)
      ) {
        Icon(Icons.Default.Terminal, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Playzone IDE", fontSize = 12.sp, color = Color.White)
      }
    }

    // Center Info Card
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Info, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = "Center Information", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = centerInfo.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate800)
        Text(text = "${centerInfo.address} • ${centerInfo.tagLine}", fontSize = 12.sp, color = Slate600)
        Text(text = "Helpline & Office: ${centerInfo.phone}", fontSize = 12.sp, color = BrandPrimary)
        Text(text = "Official UPI ID: ${centerInfo.upiId} (${centerInfo.upiName})", fontSize = 12.sp, color = BrandGoldDark, fontWeight = FontWeight.Medium)
      }
    }
  }
}

@Composable
fun StudentFeesView(
  student: Student,
  dues: FeeDuesSummary,
  activeReminder: FeeReminderNotification? = null,
  onDismissReminder: (String) -> Unit = {},
  allPayments: List<FeePayment>,
  onOpenPayDialog: () -> Unit,
  onViewReceipt: (FeePayment) -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    if (activeReminder != null) {
      item {
        FeeReminderBanner(
          reminder = activeReminder,
          onDismiss = { onDismissReminder(activeReminder.id) },
          onPayNow = onOpenPayDialog
        )
      }
    }

    // Header Balance Card
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSecondary),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Text(text = "Fee Account Summary", fontSize = 14.sp, color = BrandGoldLight)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Total Due: ₹${dues.totalOutstandingDue.toInt()}",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (dues.totalOutstandingDue > 0) BrandGold else SuccessGreen
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Standard Monthly Fee: ₹${dues.standardMonthlyFee.toInt()} / month",
            fontSize = 12.sp,
            color = Slate400
          )
          if (dues.totalDiscount > 0) {
            Text(
              text = "Total Concessions / Discounts Applied: ₹${dues.totalDiscount.toInt()}",
              fontSize = 12.sp,
              color = BrandGoldLight
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          Button(
            onClick = onOpenPayDialog,
            colors = ButtonDefaults.buttonColors(containerColor = BrandGold),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.CreditCard, contentDescription = null, tint = BrandSecondary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (dues.totalOutstandingDue > 0) "Pay Tuition Fees Now" else "Advance / Select Months",
              color = BrandSecondary,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    // Month-by-Month Statement
    item {
      Text(
        text = "Month-by-Month Fee Schedule",
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = Slate900
      )
    }

    if (dues.allMonths.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "No fee schedule generated yet for current admission session.",
            modifier = Modifier.padding(16.dp),
            fontSize = 13.sp,
            color = Slate600
          )
        }
      }
    }

    items(dues.allMonths) { m ->
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text(text = m.monthLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)
            Text(text = "Base Fee: ₹${m.baseFee.toInt()}", fontSize = 11.sp, color = Slate600)
            if (m.discountAmount > 0) {
              Text(
                text = "${m.discountName ?: "Discount"}: -₹${m.discountAmount.toInt()}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandGoldDark
              )
            }
          }

          Column(horizontalAlignment = Alignment.End) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (m.isPaid) SuccessGreenLight else ErrorRedLight
            ) {
              Text(
                text = if (m.isPaid) "PAID" else "PENDING",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (m.isPaid) SuccessGreen else ErrorRed,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Net: ₹${m.netPayable.toInt()}",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = Slate800
            )
          }
        }
      }
    }

    // Payment History & Receipts
    item {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Payment History & Official Receipts",
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = Slate900
      )
    }

    if (allPayments.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "No recorded transactions yet.",
            modifier = Modifier.padding(16.dp),
            fontSize = 13.sp,
            color = Slate600
          )
        }
      }
    } else {
      items(allPayments) { pay ->
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewReceipt(pay) }
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(text = "Receipt: ${pay.receiptNo}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
              Text(text = "Months: ${pay.month.ifBlank { pay.monthsCovered.joinToString() }}", fontSize = 11.sp, color = Slate600)
              Text(text = "Mode: ${pay.paymentMode}", fontSize = 11.sp, color = Slate600)
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(text = "₹${pay.finalAmountPaid.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
              Text(text = "View Slip", fontSize = 11.sp, color = BrandPrimary, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }
    }
  }
}

@Composable
fun StudentPayModal(
  student: Student,
  dues: FeeDuesSummary,
  centerInfo: CenterInfo = CenterInfo(),
  onDismiss: () -> Unit,
  onSubmitPayment: (selectedMonths: List<String>, mode: String, txRef: String?) -> Unit
) {
  val pendingMonths = dues.pendingMonths
  val selectedMonths = remember { mutableStateListOf<String>() }
  var paymentMode by remember { mutableStateOf("UPI") }
  var transactionRef by remember { mutableStateOf("") }
  var errorText by remember { mutableStateOf<String?>(null) }

  // By default select all pending months if available
  LaunchedEffect(pendingMonths) {
    if (selectedMonths.isEmpty()) {
      selectedMonths.addAll(pendingMonths.map { it.yearMonth })
    }
  }

  val totalSelectedAmount = remember(selectedMonths.toList()) {
    dues.allMonths
      .filter { it.yearMonth in selectedMonths }
      .sumOf { it.netPayable }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(text = "Pay Tuition Fees", fontSize = 18.sp, fontWeight = FontWeight.Bold)
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(text = "Select Months to Pay:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

        dues.allMonths.filter { !it.isPaid }.forEach { m ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(if (m.yearMonth in selectedMonths) Slate100 else Color.Transparent)
              .clickable {
                if (m.yearMonth in selectedMonths) {
                  selectedMonths.remove(m.yearMonth)
                } else {
                  selectedMonths.add(m.yearMonth)
                }
              }
              .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Checkbox(
              checked = m.yearMonth in selectedMonths,
              onCheckedChange = { chk ->
                if (chk == true) selectedMonths.add(m.yearMonth)
                else selectedMonths.remove(m.yearMonth)
              }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(text = m.monthLabel, fontSize = 13.sp, fontWeight = FontWeight.Medium)
              if (m.discountAmount > 0) {
                Text(text = "${m.discountName}: -₹${m.discountAmount.toInt()}", fontSize = 11.sp, color = BrandGoldDark)
              }
            }
            Text(text = "₹${m.netPayable.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = BrandPrimary,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(text = "Total Payable Amount:", color = Color.White, fontSize = 13.sp)
            Text(text = "₹${totalSelectedAmount.toInt()}", color = BrandGoldLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
          }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "Payment Mode:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          FilterChip(
            selected = paymentMode == "UPI",
            onClick = { paymentMode = "UPI" },
            label = { Text("UPI / Online") }
          )
          FilterChip(
            selected = paymentMode == "CASH",
            onClick = { paymentMode = "CASH" },
            label = { Text("Pay at Center Desk") }
          )
        }

        if (paymentMode == "UPI") {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Slate100,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(text = "Center UPI ID: ${centerInfo.upiId}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
              Text(text = "Payee Name: ${centerInfo.upiName}", fontSize = 11.sp, color = Slate600)
              Text(text = "Scan or transfer via GPay/PhonePe/Paytm, then enter the 12-digit UTR/Ref number below:", fontSize = 11.sp, color = Slate800)
            }
          }

          OutlinedTextField(
            value = transactionRef,
            onValueChange = { transactionRef = it; errorText = null },
            label = { Text("UPI / UTR Transaction Reference No") },
            placeholder = { Text("e.g. 329482910482") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        } else {
          Text(
            text = "Your payment will be registered as pending until approved by the Center Desk upon cash receipt.",
            fontSize = 12.sp,
            color = Slate600
          )
        }

        if (errorText != null) {
          Text(text = errorText!!, color = ErrorRed, fontSize = 12.sp)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (selectedMonths.isEmpty()) {
            errorText = "Please select at least one month"
            return@Button
          }
          if (paymentMode == "UPI" && transactionRef.isBlank()) {
            errorText = "Please enter UPI transaction reference or select Center Desk"
            return@Button
          }
          onSubmitPayment(selectedMonths.toList(), paymentMode, transactionRef)
        },
        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
      ) {
        Text("Confirm & Submit")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun FeeReceiptDialog(
  payment: FeePayment,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(text = "PIXEL PATHSALA", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
        Text(text = "Official Tuition Fee Receipt", fontSize = 12.sp, color = Slate600)
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Slate100,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(text = "Receipt No: ${payment.receiptNo}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(text = "Payment ID: ${payment.id}", fontSize = 11.sp, color = Slate600)
            Text(text = "Date: ${payment.paymentDate}", fontSize = 11.sp, color = Slate600)
          }
        }

        Text(text = "Student: ${payment.studentName}", fontWeight = FontWeight.Medium, fontSize = 13.sp)
        Text(text = "Course: ${payment.courseTitle}", fontSize = 12.sp, color = Slate600)
        Text(text = "Months Covered: ${payment.month.ifBlank { payment.monthsCovered.joinToString() }}", fontSize = 12.sp, color = Slate800)
        Text(text = "Payment Mode: ${payment.paymentMode}", fontSize = 12.sp, color = Slate800)
        if (!payment.transactionRef.isNullOrBlank()) {
          Text(text = "Transaction Ref: ${payment.transactionRef}", fontSize = 12.sp, color = Slate800)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(text = "Total Amount Paid:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
          Text(text = "₹${payment.finalAmountPaid.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = SuccessGreen)
        }

        if (!payment.approvedBy.isNullOrBlank()) {
          Text(text = "Authorized By: ${payment.approvedBy}", fontSize = 11.sp, color = Slate600)
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) {
        Text("Close")
      }
    }
  )
}

@Composable
fun StudentLiveClassesView(
  liveClasses: List<LiveClassSession>,
  recordings: List<ClassRecording>,
  alarmsSet: Set<String> = emptySet(),
  onToggleAlarm: (String) -> Unit = {}
) {
  val context = LocalContext.current

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Column {
        Text(text = "Automated Batch Class Schedule", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
        Text(text = "Generated as per enrolled batch date schedule, timings & online/offline routine", fontSize = 12.sp, color = Slate600)
      }
    }

    if (liveClasses.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(text = "No classes scheduled for today.", modifier = Modifier.padding(16.dp), fontSize = 13.sp, color = Slate600)
        }
      }
    } else {
      items(liveClasses) { session ->
        val isAlarmSet = alarmsSet.contains(session.id)
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (session.isPostponed) Color(0xFFFFFBEB) else Color.White
          ),
          border = if (session.isPostponed) BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f)) else null,
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(text = session.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate900)
              Surface(
                color = if (session.isPostponed) ErrorRedLight else if (session.status == "live") Color(0xFFFEE2E2) else BrandGoldLight,
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = if (session.isPostponed) "POSTPONED" else session.status.uppercase(),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (session.isPostponed) ErrorRed else if (session.status == "live") ErrorRed else BrandGoldDark,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Date: ${session.scheduledDate} • Timings: ${session.startTime} - ${session.endTime}",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = if (session.isPostponed) ErrorRed else BrandPrimary
            )
            Text(text = "Subject: ${session.subject} • Faculty: ${session.educatorName}", fontSize = 12.sp, color = Slate600)
            Text(text = "Mode: ${session.platform}", fontSize = 11.sp, color = Slate500)

            if (session.notificationSent) {
              Spacer(modifier = Modifier.height(6.dp))
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = BrandPrimary.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Faculty Alert: Schedule notification dispatched for this session", fontSize = 11.sp, color = BrandPrimary, fontWeight = FontWeight.Medium)
                }
              }
            }

            if (session.isPostponed) {
              Spacer(modifier = Modifier.height(8.dp))
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFEE2E2),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Notice: ${session.postponeReason ?: "Class postponed by faculty."}",
                    fontSize = 12.sp,
                    color = ErrorRed,
                    fontWeight = FontWeight.SemiBold
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedButton(
                onClick = {
                  triggerClassAlarm(context, session)
                  onToggleAlarm(session.id)
                },
                shape = RoundedCornerShape(10.dp),
                colors = if (isAlarmSet) ButtonDefaults.outlinedButtonColors(containerColor = SuccessGreenLight) else ButtonDefaults.outlinedButtonColors(),
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  imageVector = if (isAlarmSet) Icons.Default.AlarmOn else Icons.Default.Alarm,
                  contentDescription = null,
                  tint = if (isAlarmSet) SuccessGreen else BrandPrimary,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  if (isAlarmSet) "Alarm Set" else "Set Alarm",
                  fontSize = 12.sp,
                  color = if (isAlarmSet) SuccessGreen else BrandPrimary
                )
              }

              if (session.isPostponed) {
                Button(
                  onClick = {},
                  enabled = false,
                  colors = ButtonDefaults.buttonColors(disabledContainerColor = Color(0xFFE2E8F0), disabledContentColor = Slate500),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.weight(1.2f)
                ) {
                  Text("Class Postponed", fontSize = 12.sp)
                }
              } else {
                Button(
                  onClick = {
                    val url = session.meetingUrl ?: "https://meet.google.com/new"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.weight(1.2f)
                ) {
                  Icon(Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(if (session.meetingUrl.isNullOrBlank()) "Offline Class" else "Join Class", fontSize = 12.sp)
                }
              }
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(8.dp))
      Text(text = "Class Video Recordings & Archives", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
    }

    if (recordings.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(text = "No archive recordings posted yet.", modifier = Modifier.padding(16.dp), fontSize = 13.sp, color = Slate600)
        }
      }
    } else {
      items(recordings) { rec ->
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(text = rec.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)
              Text(text = "Subject: ${rec.subject} • ${rec.durationMinutes} mins", fontSize = 12.sp, color = Slate600)
            }
            IconButton(
              onClick = {
                if (rec.recordingUrl.isNotBlank()) {
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rec.recordingUrl))
                  context.startActivity(intent)
                }
              }
            ) {
              Icon(Icons.Default.PlayCircle, contentDescription = "Watch", tint = BrandPrimary, modifier = Modifier.size(32.dp))
            }
          }
        }
      }
    }
  }
}

@Composable
fun StudentStudyMaterialsView(materials: List<StudyMaterial>) {
  val context = LocalContext.current

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Text(text = "Course Study Notes & PDFs", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
    }

    if (materials.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(text = "No study material uploaded yet.", modifier = Modifier.padding(16.dp), fontSize = 13.sp, color = Slate600)
        }
      }
    } else {
      items(materials) { mat ->
        val isWebPortal = mat.fileType.equals("PORTAL", ignoreCase = true) || mat.fileType.equals("WEB", ignoreCase = true) || mat.fileUrl.startsWith("http")
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .background(if (isWebPortal) BrandSecondaryLight else ErrorRedLight, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = if (isWebPortal) Icons.Default.Language else Icons.Default.PictureAsPdf,
                  contentDescription = null,
                  tint = if (isWebPortal) BrandSecondary else ErrorRed
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(text = mat.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)
                Text(text = "${mat.subject} • ${if (isWebPortal) "Web Portal / Notes" else mat.fileType}", fontSize = 12.sp, color = Slate600)
                if (!mat.description.isNullOrBlank()) {
                  Text(text = mat.description, fontSize = 11.sp, color = Slate500, maxLines = 1)
                }
              }
            }

            Button(
              onClick = {
                if (mat.fileUrl.isNotBlank()) {
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mat.fileUrl))
                  context.startActivity(intent)
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = if (isWebPortal) BrandSecondary else BrandPrimary),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Icon(
                imageVector = if (isWebPortal) Icons.Default.OpenInBrowser else Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (isWebPortal) "Open Portal" else "View Note", fontSize = 11.sp)
            }
          }
        }
      }
    }
  }
}

@Composable
fun StudentExamsView(
  exams: List<OnlineExam>,
  submissions: List<ExamSubmission>,
  onStartExam: (OnlineExam) -> Unit = {}
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Column {
        Text(text = "Assessments & Mock Tests", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
        Text(text = "Online Computer Based Tests (Web Project)", fontSize = 11.sp, color = Slate600)
      }
    }

    if (exams.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(text = "No online exams scheduled right now.", modifier = Modifier.padding(16.dp), fontSize = 13.sp, color = Slate600)
        }
      }
    } else {
      items(exams) { exam ->
        val sub = submissions.firstOrNull { it.examId == exam.id }
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(text = exam.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate900)
              Surface(
                color = if (sub != null) SuccessGreenLight else Slate100,
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = if (sub != null) "COMPLETED" else "AVAILABLE",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (sub != null) SuccessGreen else BrandPrimary,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Subject: ${exam.subject} • Total Marks: ${exam.totalMarks} • Time: ${exam.durationMinutes} mins", fontSize = 12.sp, color = Slate600)

            if (sub != null) {
              Spacer(modifier = Modifier.height(10.dp))
              Surface(
                color = Slate100,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(text = "Your Score:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                  Text(text = "${sub.score} / ${sub.totalMarks}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = { onStartExam(exam) },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (sub != null) Color(0xFF1E293B) else BrandPrimary
              ),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
            ) {
              Icon(
                imageVector = if (sub != null) Icons.Default.Visibility else Icons.Default.PlayCircleOutline,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (sub != null) "Review Exam & Solutions (Web CBT)" else "Start Online Exam (Web CBT)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun StudentClassesAndRoutineView(
  liveClasses: List<LiveClassSession>,
  recordings: List<ClassRecording>,
  batch: Batch?,
  alarmsSet: Set<String> = emptySet(),
  onToggleAlarm: (String) -> Unit = {}
) {
  var selectedSubTab by remember { mutableIntStateOf(0) }

  Column(modifier = Modifier.fillMaxSize()) {
    Surface(
      color = Color.White,
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChip(
            selected = selectedSubTab == 0,
            onClick = { selectedSubTab = 0 },
            leadingIcon = {
              Icon(Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            label = { Text("Classes & Archives (${liveClasses.size + recordings.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = BrandPrimary,
              selectedLabelColor = Color.White
            ),
            modifier = Modifier.weight(1f)
          )

          FilterChip(
            selected = selectedSubTab == 1,
            onClick = { selectedSubTab = 1 },
            leadingIcon = {
              Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            label = { Text("Batch Timetable", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = BrandPrimary,
              selectedLabelColor = Color.White
            ),
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = BrandPrimary.copy(alpha = 0.08f),
          border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.2f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = BrandPrimary,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Enrolled Batch Schedule: ${batch?.name ?: "Admission Batch"}",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = BrandPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = BrandGoldLight
            ) {
              Text(
                text = batch?.timings ?: "Scheduled",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = BrandGoldDark,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }
      }
    }

    if (selectedSubTab == 0) {
      StudentLiveClassesView(
        liveClasses = liveClasses,
        recordings = recordings,
        alarmsSet = alarmsSet,
        onToggleAlarm = onToggleAlarm
      )
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        item {
          Text(text = "Batch Timetable & Routine", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate900)
          Text(text = "Official Weekly Coaching Schedule", fontSize = 12.sp, color = Slate600)
        }

        item {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BrandSecondary),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text(text = "Assigned Batch", fontSize = 12.sp, color = BrandGoldLight)
              Text(text = batch?.name ?: "Morning Batch", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
              Spacer(modifier = Modifier.height(8.dp))
              Text(text = "Class Timings: ${batch?.timings ?: "08:00 AM - 10:00 AM"}", fontSize = 13.sp, color = Color.White)
              Text(text = "Schedule Days: ${batch?.scheduleDays ?: "Mon, Wed, Fri"}", fontSize = 13.sp, color = BrandGoldLight)
              Text(text = "Mode: ${batch?.mode?.uppercase() ?: "OFFLINE"}", fontSize = 12.sp, color = Slate300)
            }
          }
        }

        item {
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.School, contentDescription = null, tint = BrandPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Classroom Rules & Center Timetable", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)
              }
              Spacer(modifier = Modifier.height(8.dp))
              Text(text = "1. Please arrive at the center 10 minutes prior to batch start.", fontSize = 12.sp, color = Slate700)
              Text(text = "2. Keep homework notebooks and study materials ready.", fontSize = 12.sp, color = Slate700)
              Text(text = "3. Mobile phones must be kept on study-focus mode during class.", fontSize = 12.sp, color = Slate700)
              Text(text = "4. Doubt clearance sessions available daily from 5:00 PM to 6:30 PM.", fontSize = 12.sp, color = Slate700)
            }
          }
        }
      }
    }
  }
}

@Composable
fun StudentStudyAndExamsView(
  materials: List<StudyMaterial>,
  exams: List<OnlineExam>,
  submissions: List<ExamSubmission>,
  batch: Batch? = null,
  onStartExam: (OnlineExam) -> Unit = {}
) {
  var selectedSubTab by remember { mutableIntStateOf(0) }

  Column(modifier = Modifier.fillMaxSize()) {
    Surface(
      color = Color.White,
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChip(
            selected = selectedSubTab == 0,
            onClick = { selectedSubTab = 0 },
            leadingIcon = {
              Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            label = { Text("Batch Notes (${materials.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = BrandPrimary,
              selectedLabelColor = Color.White
            ),
            modifier = Modifier.weight(1f)
          )

          FilterChip(
            selected = selectedSubTab == 1,
            onClick = { selectedSubTab = 1 },
            leadingIcon = {
              Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            label = { Text("Exams & Tests (${exams.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = BrandPrimary,
              selectedLabelColor = Color.White
            ),
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = BrandPrimary.copy(alpha = 0.08f),
          border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.2f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = BrandPrimary,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Admission Batch Notes & Tests: ${batch?.name ?: "Enrolled"}",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = BrandPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = BrandGoldLight
            ) {
              Text(
                text = "Batch Only",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = BrandGoldDark,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }
      }
    }

    if (selectedSubTab == 0) {
      StudentStudyMaterialsView(materials = materials)
    } else {
      StudentExamsView(exams = exams, submissions = submissions, onStartExam = onStartExam)
    }
  }
}

@Composable
fun StudentFocusAndGamesView(
  student: Student,
  telemetry: StudentTelemetry
) {
  val context = LocalContext.current
  val lat = telemetry.latitude
  val lng = telemetry.longitude
  val battery = telemetry.batteryLevel
  val isCharging = telemetry.isCharging
  val games = telemetry.installedGames
  val focusScore = telemetry.studyFocusScore

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Text(text = "Student Live Tracker & Study Focus", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate900)
      Text(text = "Live GPS Telemetry, Battery Status & Gaming Distraction Monitor", fontSize = 12.sp, color = Slate600)
    }

    // Study Focus Score Banner
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (focusScore >= 80) BrandPrimary else BrandGoldDark),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(text = "Study Focus Score", fontSize = 13.sp, color = BrandGoldLight)
              Text(text = "$focusScore%", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (focusScore >= 80) SuccessGreenLight else ErrorRedLight
            ) {
              Text(
                text = if (focusScore >= 80) "EXCELLENT" else "ATTENTION NEEDED",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (focusScore >= 80) SuccessGreen else ErrorRed,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = if (games.isEmpty())
              "✓ Device is currently optimized for study. No gaming apps detected."
            else
              "⚠ ${games.size} gaming/entertainment apps detected on this device. Minimize gameplay during tuition hours.",
            fontSize = 12.sp,
            color = Slate200
          )
        }
      }
    }

    // Live Location Card
    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Live GPS Location Tracking", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate900)
          }

          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Slate100,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(text = "Coordinates: $lat, $lng", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
              Text(text = "Registered Area: Raina, Purba Bardhaman, West Bengal", fontSize = 12.sp, color = Slate600)
              Text(text = "Last Ping: ${telemetry.lastHeartbeat}", fontSize = 11.sp, color = Slate400)
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Button(
            onClick = {
              val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng(${student.name})"))
              context.startActivity(mapIntent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.Map, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open in Google Maps", fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // Battery & Hardware Diagnostics
    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(text = "Device Hardware & Telemetry", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate900)
          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                contentDescription = null,
                tint = if (battery > 20) SuccessGreen else ErrorRed,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(text = "$battery% Battery", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Text(
              text = if (isCharging) "Charging (AC/USB)" else "On Battery",
              fontSize = 12.sp,
              color = if (isCharging) SuccessGreen else Slate600
            )
          }

          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(text = "Network: ${telemetry.networkType}", fontSize = 12.sp, color = Slate700)
            Text(text = "Device: ${telemetry.deviceModel.ifBlank { "Android Device" }}", fontSize = 12.sp, color = Slate700)
          }
        }
      }
    }

    // Installed Games & Distraction Apps Scan
    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.SportsEsports, contentDescription = null, tint = BrandGoldDark, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Gaming & Distractions Scanner", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate900)
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Scans package inventory for heavy multiplayer games during study and coaching hours.",
            fontSize = 12.sp,
            color = Slate600
          )

          Spacer(modifier = Modifier.height(10.dp))

          if (games.isEmpty()) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = SuccessGreenLight,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Clean Device: No popular gaming distraction apps installed.",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = SuccessGreen
                )
              }
            }
          } else {
            games.forEach { gameName ->
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = ErrorRedLight,
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp)
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = gameName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
                  }
                  Text(text = "-15% Focus Penalty", fontSize = 11.sp, color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun FeeReminderBanner(
  reminder: FeeReminderNotification,
  onDismiss: () -> Unit,
  onPayNow: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
    border = BorderStroke(1.5.dp, ErrorRed),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Surface(
            shape = CircleShape,
            color = ErrorRed,
            modifier = Modifier.size(32.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
              )
            }
          }
          Column {
            Text(
              text = "Fee Due Reminder from Center",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = ErrorRed
            )
            Text(
              text = "Amount Due: ₹${reminder.dueAmount.toInt()}",
              fontSize = 17.sp,
              fontWeight = FontWeight.ExtraBold,
              color = Slate900
            )
          }
        }

        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Slate500, modifier = Modifier.size(18.dp))
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Pending Months: ${reminder.pendingMonths}",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Slate800
      )

      if (reminder.message.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = reminder.message,
          fontSize = 12.sp,
          color = Slate600
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        TextButton(
          onClick = onDismiss,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Text("Dismiss", color = Slate600, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
          onClick = onPayNow,
          colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
        ) {
          Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Pay Fees Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun StudentFeeRemindersDialog(
  reminders: List<FeeReminderNotification>,
  onDismiss: () -> Unit,
  onPayNow: () -> Unit,
  onMarkRead: (String) -> Unit,
  onDismissAll: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = ErrorRed)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Fee Reminders (${reminders.size})", fontWeight = FontWeight.Bold, color = Slate900, fontSize = 17.sp)
      }
    },
    text = {
      if (reminders.isEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(48.dp))
          Spacer(modifier = Modifier.height(8.dp))
          Text("No Fee Reminders", fontWeight = FontWeight.Bold, color = Slate800, fontSize = 15.sp)
          Text("All your tuition fees are up to date. Thank you!", fontSize = 12.sp, color = Slate600, textAlign = TextAlign.Center)
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 350.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(reminders) { reminder ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (!reminder.isRead) ErrorRedLight else Slate100,
              border = BorderStroke(1.dp, if (!reminder.isRead) ErrorRed.copy(alpha = 0.5f) else Slate300),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!reminder.isRead) {
                      Box(
                        modifier = Modifier
                          .size(8.dp)
                          .clip(CircleShape)
                          .background(ErrorRed)
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                      text = "Fee Due: ₹${reminder.dueAmount.toInt()}",
                      fontWeight = FontWeight.Bold,
                      fontSize = 15.sp,
                      color = if (!reminder.isRead) ErrorRed else Slate800
                    )
                  }
                  Text(
                    text = reminder.timestamp.take(10),
                    fontSize = 10.sp,
                    color = Slate500
                  )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Pending Months: ${reminder.pendingMonths}",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = Slate700
                )

                if (reminder.message.isNotBlank()) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = reminder.message,
                    fontSize = 12.sp,
                    color = Slate600
                  )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.End,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  if (!reminder.isRead) {
                    TextButton(
                      onClick = { onMarkRead(reminder.id) },
                      contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                      Text("Mark as Read", fontSize = 11.sp, color = Slate700)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                  }
                  Button(
                    onClick = {
                      onMarkRead(reminder.id)
                      onDismiss()
                      onPayNow()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                  ) {
                    Text("Pay Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      if (reminders.isNotEmpty()) {
        TextButton(onClick = onDismissAll) {
          Text("Dismiss All", color = Slate600, fontSize = 12.sp)
        }
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
        Text("Close", color = Slate800, fontSize = 12.sp)
      }
    },
    containerColor = Color.White,
    shape = RoundedCornerShape(16.dp)
  )
}

