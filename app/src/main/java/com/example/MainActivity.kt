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
    val repository = PathsalaRepository.getInstance(applicationContext)

    setContent {
      PixelPathsalaTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          var showSplash by remember { mutableStateOf(true) }
          val currentStudent by repository.currentStudent.collectAsState()
          val currentAdmin by repository.currentAdmin.collectAsState()

          if (showSplash) {
            OpeningLogoScreen(
              onSplashFinished = { showSplash = false }
            )
          } else if (currentStudent != null) {
            BackHandler {
              repository.logoutStudent()
            }
            StudentMainScreen(
              student = currentStudent!!,
              repository = repository,
              onLogout = { repository.logoutStudent() }
            )
          } else if (currentAdmin != null) {
            BackHandler {
              repository.logoutAdmin()
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
}
