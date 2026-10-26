package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.data.repository.PathsalaRepository
import com.example.ui.admin.AdminMainScreen
import com.example.ui.auth.LoginScreen
import com.example.ui.splash.OpeningLogoScreen
import com.example.ui.student.StudentMainScreen
import com.example.ui.theme.PixelPathsalaTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
      val processName = android.app.Application.getProcessName()
      if (packageName != processName) {
        try {
          android.webkit.WebView.setDataDirectorySuffix(processName)
        } catch (_: Exception) {}
      }
    }

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
      if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
        requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
      }
    }

    val repository = PathsalaRepository.getInstance(applicationContext)

    // Eagerly restore notification on launch if student already logged in
    repository.currentStudent.value?.let { st ->
      com.example.util.NotificationHelper.syncStudentClassNotification(
        context = applicationContext,
        student = st,
        liveClasses = repository.liveClasses.value,
        batches = repository.batches.value
      )
    }

    setContent {
      val isDarkMode by repository.isDarkMode.collectAsState()
      PixelPathsalaTheme(darkTheme = isDarkMode) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          var showSplash by remember { mutableStateOf(true) }
          val currentStudent by repository.currentStudent.collectAsState()
          val currentAdmin by repository.currentAdmin.collectAsState()

          val liveClasses by repository.liveClasses.collectAsState()
          val batches by repository.batches.collectAsState()

          LaunchedEffect(currentStudent, liveClasses, batches) {
            val st = currentStudent
            if (st != null) {
              // Notify once if there is a new scheduled or postponed class
              com.example.util.NotificationHelper.syncStudentClassNotification(
                context = applicationContext,
                student = st,
                liveClasses = liveClasses,
                batches = batches
              )

              // Automatically maintain 30-min alarms for student's explicitly scheduled classes
              liveClasses.forEach { session ->
                val sessBatch = session.batchId?.trim() ?: ""
                val sessCourse = session.courseId?.trim() ?: ""
                val isRelevant = when {
                  sessBatch.isNotEmpty() && !sessBatch.equals("all", ignoreCase = true) -> sessBatch == st.batchId
                  sessBatch.equals("all", ignoreCase = true) -> sessCourse.isEmpty() || sessCourse == st.courseId
                  sessCourse.isNotEmpty() -> sessCourse == st.courseId
                  else -> true
                }
                if (isRelevant) {
                  if (session.isPostponed || session.status.equals("postponed", ignoreCase = true)) {
                    com.example.util.InAppAlarmScheduler.cancelClassAlarm(applicationContext, session.id)
                  } else if (session.status.equals("scheduled", ignoreCase = true)) {
                    com.example.util.InAppAlarmScheduler.scheduleClassAlarm(applicationContext, session, showToast = false)
                  }
                }
              }
            }
          }

          if (showSplash) {
            OpeningLogoScreen(
              onSplashFinished = { showSplash = false }
            )
          } else if (currentStudent != null) {
            BackHandler {
              // Keep login persistent on back press, do not logout!
              moveTaskToBack(true)
            }
            StudentMainScreen(
              student = currentStudent!!,
              repository = repository,
              onLogout = {
                com.example.util.NotificationHelper.cancelPersistentClassNotification(applicationContext)
                repository.logoutStudent()
              }
            )
          } else if (currentAdmin != null) {
            BackHandler {
              // Keep login persistent on back press, do not logout!
              moveTaskToBack(true)
            }
            AdminMainScreen(
              admin = currentAdmin!!,
              repository = repository,
              onLogout = { repository.logoutAdmin() }
            )
          } else {
            LoginScreen(
              repository = repository,
              onStudentLoginSuccess = { /* StateFlow updates */ },
              onAdminLoginSuccess = { /* StateFlow updates */ }
            )
          }
        }
      }
    }
  }

  override fun onStop() {
    super.onStop()
  }
}
