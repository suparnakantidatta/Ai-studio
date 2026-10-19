package com.example.ui.student

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.data.model.*
import com.example.data.repository.PathsalaRepository
import com.example.ui.theme.*
import com.example.util.FeeCalculator
import kotlinx.coroutines.launch

fun triggerClassAlarm(context: Context, session: LiveClassSession) {
  try {
    var hour = 9
    var minute = 0
    val timeStr = session.startTime.trim()
    val isPm = timeStr.contains("PM", ignoreCase = true)
    val isAm = timeStr.contains("AM", ignoreCase = true)
    val cleanTime = timeStr.replace("(?i)am|pm".toRegex(), "").trim()
    val parts = cleanTime.split(":")
    if (parts.isNotEmpty()) {
      hour = parts[0].trim().toIntOrNull() ?: 9
      if (isPm && hour < 12) hour += 12
      if (isAm && hour == 12) hour = 0
    }
    if (parts.size > 1) {
      minute = parts[1].trim().toIntOrNull() ?: 0
    }

    val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
      putExtra(AlarmClock.EXTRA_MESSAGE, "Pixel Pathsala: ${session.title} (${session.subject})")
      putExtra(AlarmClock.EXTRA_HOUR, hour)
      putExtra(AlarmClock.EXTRA_MINUTES, minute)
      putExtra(AlarmClock.EXTRA_SKIP_UI, false)
    }
    context.startActivity(intent)
    Toast.makeText(context, "Alarm requested for ${session.title} at ${session.startTime}", Toast.LENGTH_SHORT).show()
  } catch (e: Exception) {
    try {
      val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
      context.startActivity(intent)
      Toast.makeText(context, "Please set alarm in Clock app for ${session.startTime}", Toast.LENGTH_SHORT).show()
    } catch (e2: Exception) {
      Toast.makeText(context, "Class scheduled at ${session.startTime}", Toast.LENGTH_SHORT).show()
    }
  }
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

  // Batchwise filter state (Default to their admission batch)
  var filterByAdmissionBatchOnly by remember { mutableStateOf(true) }
  var alarmsSet by remember { mutableStateOf(setOf<String>()) }

  val filteredClasses = remember(liveClasses, filterByAdmissionBatchOnly, student.batchId, student.courseId) {
    if (filterByAdmissionBatchOnly) {
      liveClasses.filter {
        it.batchId == student.batchId ||
        (it.batchId.isNullOrBlank() && (it.courseId.isNullOrBlank() || it.courseId == student.courseId))
      }
    } else {
      liveClasses
    }
  }

  val filteredMaterials = remember(studyMaterials, filterByAdmissionBatchOnly, student.batchId, student.courseId) {
    if (filterByAdmissionBatchOnly) {
      studyMaterials.filter {
        it.batchId == student.batchId ||
        (it.batchId.isNullOrBlank() && (it.courseId.isNullOrBlank() || it.courseId == student.courseId))
      }
    } else {
      studyMaterials
    }
  }

  val filteredRecordings = remember(recordings, filterByAdmissionBatchOnly, student.batchId, student.courseId) {
    if (filterByAdmissionBatchOnly) {
      recordings.filter {
        it.batchId == student.batchId ||
        (it.batchId.isNullOrBlank() && (it.courseId.isNullOrBlank() || it.courseId == student.courseId))
      }
    } else {
      recordings
    }
  }

  // Pay Fee Dialog State
  var showPayDialog by remember { mutableStateOf(false) }
  var viewReceiptPayment by remember { mutableStateOf<FeePayment?>(null) }
  var showUpdateDialog by remember { mutableStateOf(false) }

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
          IconButton(onClick = onLogout) {
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
          liveClasses = filteredClasses,
          studyMaterials = filteredMaterials,
          alarmsSet = alarmsSet,
          onToggleAlarm = { id ->
            alarmsSet = if (alarmsSet.contains(id)) alarmsSet - id else alarmsSet + id
          },
          onNavigateToFees = { selectedTab = 1 },
          onNavigateToLive = { selectedTab = 2 },
          onNavigateToStudy = { selectedTab = 3 },
          onOpenUpdateDialog = { showUpdateDialog = true }
        )
        1 -> StudentFeesView(
          student = student,
          dues = duesSummary,
          allPayments = payments.filter { it.studentId == student.id },
          onOpenPayDialog = { showPayDialog = true },
          onViewReceipt = { viewReceiptPayment = it }
        )
        2 -> StudentClassesAndRoutineView(
          liveClasses = filteredClasses,
          recordings = filteredRecordings,
          batch = studentBatch,
          filterByAdmissionBatchOnly = filterByAdmissionBatchOnly,
          onToggleBatchFilter = { filterByAdmissionBatchOnly = !filterByAdmissionBatchOnly },
          alarmsSet = alarmsSet,
          onToggleAlarm = { id ->
            alarmsSet = if (alarmsSet.contains(id)) alarmsSet - id else alarmsSet + id
          }
        )
        3 -> StudentStudyAndExamsView(
          materials = filteredMaterials,
          exams = exams.filter {
            it.courseId.isNullOrBlank() || it.courseId == student.courseId
          },
          submissions = submissions.filter { it.studentId == student.id },
          batch = studentBatch,
          filterByAdmissionBatchOnly = filterByAdmissionBatchOnly,
          onToggleBatchFilter = { filterByAdmissionBatchOnly = !filterByAdmissionBatchOnly }
        )
      }

      // Pay Dialog
      if (showPayDialog) {
        StudentPayModal(
          student = student,
          dues = duesSummary,
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

      if (showUpdateDialog) {
        com.example.ui.common.AppUpdateDialog(
          onDismiss = { showUpdateDialog = false }
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
  liveClasses: List<LiveClassSession>,
  studyMaterials: List<StudyMaterial>,
  alarmsSet: Set<String>,
  onToggleAlarm: (String) -> Unit,
  onNavigateToFees: () -> Unit,
  onNavigateToLive: () -> Unit,
  onNavigateToStudy: () -> Unit,
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
              Icon(Icons.Default.VideoCall, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Scheduled Class", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate900)
            }
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = BrandGoldLight
            ) {
              Text(
                text = nextClass.status.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BrandGoldDark,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(text = nextClass.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate900)
          Text(text = "Subject: ${nextClass.subject} • Faculty: ${nextClass.educatorName}", fontSize = 12.sp, color = Slate600)
          Text(text = "Timings: ${nextClass.startTime} - ${nextClass.endTime} (${nextClass.scheduledDate.ifBlank { "Upcoming" }})", fontSize = 12.sp, color = BrandPrimary, fontWeight = FontWeight.SemiBold)

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
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Notes as Web Portal & Exams", fontSize = 12.sp)
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
        Text(text = "Pixel Pathsala", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate800)
        Text(text = "Raina, Purba Bardhaman, West Bengal", fontSize = 12.sp, color = Slate600)
        Text(text = "Support / Helpdesk: +91 9475934733", fontSize = 12.sp, color = BrandPrimary)
        Text(text = "UPI for Fees: suparnakantidatta@okaxis", fontSize = 12.sp, color = BrandGoldDark, fontWeight = FontWeight.Medium)
      }
    }
  }
}

@Composable
fun StudentFeesView(
  student: Student,
  dues: FeeDuesSummary,
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
              Text(text = "Center UPI ID: suparnakantidatta@okaxis", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
              Text(text = "Name: Pixel Pathsala Admin", fontSize = 11.sp, color = Slate600)
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
      Text(text = "Upcoming Live Classes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
    }

    if (liveClasses.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(text = "No live classes scheduled for today.", modifier = Modifier.padding(16.dp), fontSize = 13.sp, color = Slate600)
        }
      }
    } else {
      items(liveClasses) { session ->
        val isAlarmSet = alarmsSet.contains(session.id)
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
              Text(text = session.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate900)
              Surface(color = BrandGoldLight, shape = RoundedCornerShape(6.dp)) {
                Text(
                  text = session.status.uppercase(),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = BrandGoldDark,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Subject: ${session.subject} • Faculty: ${session.educatorName}", fontSize = 12.sp, color = Slate600)
            Text(text = "Timings: ${session.startTime} - ${session.endTime}", fontSize = 12.sp, color = BrandPrimary, fontWeight = FontWeight.SemiBold)

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
                Text("Join Class", fontSize = 12.sp)
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
  submissions: List<ExamSubmission>
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Text(text = "Assessments & Mock Tests", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
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
  filterByAdmissionBatchOnly: Boolean = true,
  onToggleBatchFilter: () -> Unit = {},
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
          .padding(horizontal = 16.dp, vertical = 6.dp)
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
            label = { Text("Classes & Archives", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
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

        Spacer(modifier = Modifier.height(4.dp))
        FilterChip(
          selected = filterByAdmissionBatchOnly,
          onClick = onToggleBatchFilter,
          leadingIcon = {
            Icon(
              imageVector = if (filterByAdmissionBatchOnly) Icons.Default.FilterAlt else Icons.Default.FilterAltOff,
              contentDescription = null,
              modifier = Modifier.size(14.dp)
            )
          },
          label = {
            Text(
              if (filterByAdmissionBatchOnly) "Admission Batch: ${batch?.name ?: "Enrolled"}" else "Showing: All Batches",
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = BrandGoldLight,
            selectedLabelColor = BrandGoldDark
          )
        )
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
  filterByAdmissionBatchOnly: Boolean = true,
  onToggleBatchFilter: () -> Unit = {}
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
          .padding(horizontal = 16.dp, vertical = 6.dp)
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
            label = { Text("Study & Web Portal (${materials.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
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

        Spacer(modifier = Modifier.height(4.dp))
        FilterChip(
          selected = filterByAdmissionBatchOnly,
          onClick = onToggleBatchFilter,
          leadingIcon = {
            Icon(
              imageVector = if (filterByAdmissionBatchOnly) Icons.Default.FilterAlt else Icons.Default.FilterAltOff,
              contentDescription = null,
              modifier = Modifier.size(14.dp)
            )
          },
          label = {
            Text(
              if (filterByAdmissionBatchOnly) "Admission Batch: ${batch?.name ?: "Enrolled"}" else "Showing: All Batches",
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = BrandGoldLight,
            selectedLabelColor = BrandGoldDark
          )
        )
      }
    }

    if (selectedSubTab == 0) {
      StudentStudyMaterialsView(materials = materials)
    } else {
      StudentExamsView(exams = exams, submissions = submissions)
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
