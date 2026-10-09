package com.example.ui.student

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.ClassAlarmAudioPlayer
import com.example.alarm.ClassAlarmManager
import com.example.alarm.ClassNotificationHelper
import com.example.data.model.Student
import com.example.data.repository.PathsalaRepository
import com.example.ui.components.CenterLogo
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentMainScreen(
  student: Student,
  repository: PathsalaRepository,
  onLogout: () -> Unit
) {
  val context = LocalContext.current
  val activity = context as? ComponentActivity
  var selectedTab by remember { mutableStateOf(0) }
  var showLogoutDialog by remember { mutableStateOf(false) }
  val isSyncing by repository.isSyncing.collectAsState()
  val liveClasses by repository.liveClasses.collectAsState()
  val courses by repository.courses.collectAsState()
  val batches by repository.batches.collectAsState()
  val isAlarmRinging by ClassAlarmAudioPlayer.isRinging.collectAsState()
  val activeAlarmTitle by ClassAlarmAudioPlayer.activeAlarmSessionTitle.collectAsState()

  val studentCourse = courses.find { it.id == student.courseId }
  val studentBatch = batches.find { it.id == student.batchId }
  val displayClass = studentCourse?.academicClass?.takeIf { it.isNotBlank() }
    ?: student.studentClass?.takeIf { it.isNotBlank() && !it.startsWith("Class 10 (Secondary)") }
    ?: studentCourse?.title?.takeIf { it.isNotBlank() }
    ?: "Class 12"

  // Permission launcher for Notifications on Android 13+
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { _ -> }

  LaunchedEffect(Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    ClassNotificationHelper.createNotificationChannels(context)
  }

  // Filter live classes targeted to this student
  val studentLive = remember(liveClasses, student) {
    liveClasses.filter {
      (it.batchId == "all" || it.batchId.isBlank() || it.batchId == student.batchId) &&
      (it.courseId == "all" || it.courseId.isBlank() || it.courseId == student.courseId)
    }
  }

  val scheduledLive = remember(studentLive) {
    studentLive.filter { it.status.equals("scheduled", ignoreCase = true) }
  }

  // On student login or data sync: automatically set alarms on time and notify for postponed classes
  LaunchedEffect(student.id, studentLive) {
    ClassAlarmManager.autoScheduleAlarmsForStudent(context, scheduledLive)
    ClassAlarmManager.checkAndNotifyPostponedClasses(context, studentLive)
  }

  // Prevent back key from logging out: return to Tab 0 or minimize app
  BackHandler {
    if (selectedTab != 0) {
      selectedTab = 0
    } else {
      activity?.moveTaskToBack(true)
    }
  }

  val hasActiveLiveClass = liveClasses.any {
    it.status == "live" && (it.batchId == "all" || it.batchId == student.batchId)
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            CenterLogo(size = 36)
            Column {
              Text(
                text = student.name,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                color = Color.White
              )
              Text(
                text = "Roll: ${student.rollNo} • $displayClass",
                fontSize = 11.sp,
                color = Color(0xFFA5B4FC)
              )
            }
          }
        },
        actions = {
          IconButton(
            onClick = { repository.syncWithBackend() },
            enabled = !isSyncing
          ) {
            Icon(
              imageVector = Icons.Default.Sync,
              contentDescription = "Sync",
              tint = if (isSyncing) Color(0xFFFBBF24) else Color.White
            )
          }
          IconButton(onClick = { showLogoutDialog = true }) {
            Icon(
              imageVector = Icons.Default.Logout,
              contentDescription = "Logout",
              tint = Color(0xFFFCA5A5)
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = Color(0xFF0F172A),
          titleContentColor = Color.White
        )
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars
      ) {
        NavigationBarItem(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
          label = { Text("Fees & Info", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = IndigoPrimary,
            selectedTextColor = IndigoPrimary,
            indicatorColor = Color(0xFFEEF2FF)
          )
        )

        NavigationBarItem(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          icon = {
            BadgedBox(
              badge = {
                if (hasActiveLiveClass) {
                  Badge(containerColor = Color(0xFFE11D48)) {
                    Text("LIVE", fontSize = 9.sp, fontWeight = FontWeight.Black)
                  }
                }
              }
            ) {
              Icon(Icons.Default.Radio, contentDescription = "Live Classes")
            }
          },
          label = { Text("Live Classes", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = if (hasActiveLiveClass) Color(0xFFE11D48) else IndigoPrimary,
            selectedTextColor = if (hasActiveLiveClass) Color(0xFFE11D48) else IndigoPrimary,
            indicatorColor = Color(0xFFEEF2FF)
          )
        )

        NavigationBarItem(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          icon = { Icon(Icons.Default.Assignment, contentDescription = "Exams") },
          label = { Text("Online Tests", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = IndigoPrimary,
            selectedTextColor = IndigoPrimary,
            indicatorColor = Color(0xFFEEF2FF)
          )
        )

        NavigationBarItem(
          selected = selectedTab == 3,
          onClick = { selectedTab = 3 },
          icon = { Icon(Icons.Default.Folder, contentDescription = "Materials") },
          label = { Text("Study Notes", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = IndigoPrimary,
            selectedTextColor = IndigoPrimary,
            indicatorColor = Color(0xFFEEF2FF)
          )
        )
      }
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(MaterialTheme.colorScheme.background)
    ) {
      if (isAlarmRinging) {
        Surface(
          color = Color(0xFFDC2626),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = "Alarm Ringing",
                tint = Color.White
              )
              Column {
                Text(
                  text = "⏰ CLASS ALARM RINGING NOW!",
                  fontWeight = FontWeight.Black,
                  fontSize = 12.sp,
                  color = Color.White
                )
                Text(
                  text = activeAlarmTitle ?: "Live class is starting now!",
                  fontSize = 11.sp,
                  color = Color(0xFFFEE2E2),
                  maxLines = 1
                )
              }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Button(
                onClick = { selectedTab = 1 },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text("View Class", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, fontSize = 11.sp)
              }
              OutlinedButton(
                onClick = { ClassAlarmAudioPlayer.stopAlarm(context) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text("Stop", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
              }
            }
          }
        }
      }

      Box(modifier = Modifier.weight(1f)) {
        when (selectedTab) {
          0 -> StudentDashboardTab(student = student, repository = repository)
          1 -> StudentLiveClassesTab(student = student, repository = repository)
          2 -> StudentExamsTab(student = student, repository = repository)
          3 -> StudentMaterialsTab(student = student, repository = repository)
        }
      }
    }
  }

  if (showLogoutDialog) {
    AlertDialog(
      onDismissRequest = { showLogoutDialog = false },
      title = { Text("Log Out?", fontWeight = FontWeight.Bold) },
      text = { Text("Are you sure you want to log out of your student account?") },
      confirmButton = {
        Button(
          onClick = {
            showLogoutDialog = false
            onLogout()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
        ) {
          Text("Log Out", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { showLogoutDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}
