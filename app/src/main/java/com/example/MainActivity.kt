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
              com.example.util.NotificationHelper.syncStudentClassNotification(
                context = applicationContext,
                student = st,
                liveClasses = liveClasses,
                batches = batches
              )
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
    val repository = PathsalaRepository.getInstance(applicationContext)
    repository.currentStudent.value?.let { st ->
      com.example.util.NotificationHelper.syncStudentClassNotification(
        context = applicationContext,
        student = st,
        liveClasses = repository.liveClasses.value,
        batches = repository.batches.value
      )
    }
  }
}
