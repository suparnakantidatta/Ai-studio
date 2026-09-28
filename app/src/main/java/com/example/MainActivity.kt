package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.data.repository.PathsalaRepository
import com.example.ui.admin.AdminMainScreen
import com.example.ui.admin.AdminSettingsDialog
import com.example.ui.auth.LoginScreen
import com.example.ui.student.StudentMainScreen
import com.example.ui.theme.PixelPathsalaTheme

sealed class AppScreen {
  data object Login : AppScreen()
  data object StudentPortal : AppScreen()
  data object AdminErp : AppScreen()
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val repository = PathsalaRepository(applicationContext)

    setContent {
      PixelPathsalaTheme {
        var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Login) }
        var showServerConfigDialog by remember { mutableStateOf(false) }

        val currentStudent by repository.currentStudent.collectAsState()
        val currentAdmin by repository.currentAdmin.collectAsState()

        // Sync screen with session state
        LaunchedEffect(currentStudent, currentAdmin) {
          if (currentStudent != null && currentScreen == AppScreen.Login) {
            currentScreen = AppScreen.StudentPortal
          } else if (currentAdmin != null && currentScreen == AppScreen.Login) {
            currentScreen = AppScreen.AdminErp
          }
        }

        Surface(modifier = Modifier.fillMaxSize()) {
          when (currentScreen) {
            AppScreen.Login -> {
              LoginScreen(
                repository = repository,
                onStudentLoginSuccess = {
                  currentScreen = AppScreen.StudentPortal
                },
                onAdminLoginSuccess = {
                  currentScreen = AppScreen.AdminErp
                },
                onOpenServerConfig = {
                  showServerConfigDialog = true
                }
              )
            }

            AppScreen.StudentPortal -> {
              BackHandler {
                repository.logoutStudent()
                currentScreen = AppScreen.Login
              }
              val student = currentStudent
              if (student != null) {
                StudentMainScreen(
                  student = student,
                  repository = repository,
                  onLogout = {
                    repository.logoutStudent()
                    currentScreen = AppScreen.Login
                  }
                )
              } else {
                currentScreen = AppScreen.Login
              }
            }

            AppScreen.AdminErp -> {
              BackHandler {
                repository.logoutAdmin()
                currentScreen = AppScreen.Login
              }
              AdminMainScreen(
                repository = repository,
                onLogout = {
                  repository.logoutAdmin()
                  currentScreen = AppScreen.Login
                }
              )
            }
          }
        }

        if (showServerConfigDialog) {
          AdminSettingsDialog(
            repository = repository,
            onDismiss = { showServerConfigDialog = false }
          )
        }
      }
    }
  }
}
