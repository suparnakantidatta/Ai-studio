package com.example.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.PathsalaRepository
import com.example.ui.theme.*
import com.example.util.StudentDeviceMonitor

@Composable
fun LoginScreen(
  repository: PathsalaRepository,
  onStudentLoginSuccess: () -> Unit,
  onAdminLoginSuccess: () -> Unit
) {
  val context = LocalContext.current
  val isDarkMode by repository.isDarkMode.collectAsState()
  var selectedTab by remember { mutableIntStateOf(0) } // 0: Student, 1: Admin

  // Student Form State
  var studentIdentifier by remember { mutableStateOf("") }
  var studentSecret by remember { mutableStateOf("") }
  var studentPasswordVisible by remember { mutableStateOf(false) }
  var studentError by remember { mutableStateOf<String?>(null) }

  // Admin Form State
  var adminUsername by remember { mutableStateOf("") }
  var adminPassword by remember { mutableStateOf("") }
  var adminPasswordVisible by remember { mutableStateOf(false) }
  var adminError by remember { mutableStateOf<String?>(null) }
  var showUpdateDialog by remember { mutableStateOf(false) }

  val scrollState = rememberScrollState()

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF0A0F1D))
  ) {
    // Dark mode toggle in top bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.End,
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = { repository.toggleDarkMode() },
        modifier = Modifier
          .background(Color(0xFF1E293B), CircleShape)
          .size(36.dp)
      ) {
        Icon(
          imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
          contentDescription = "Toggle Dark Mode",
          tint = BrandGold,
          modifier = Modifier.size(20.dp)
        )
      }
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(horizontal = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(modifier = Modifier.height(48.dp))

      if (com.example.util.UpdateManager.isUpdateAvailable()) {
        com.example.ui.common.AppUpdateBanner(
          onClick = { showUpdateDialog = true },
          modifier = Modifier.padding(bottom = 16.dp)
        )
      }

      // Top Logo Header matching screenshot
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .height(180.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF030712),
        border = BorderStroke(1.5.dp, BrandGold.copy(alpha = 0.7f))
      ) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            // Monitor Icon with Gold P
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color.Black,
              border = BorderStroke(1.5.dp, BrandGold),
              modifier = Modifier.size(68.dp, 50.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = "P",
                  fontSize = 24.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = BrandGold
                )
              }
            }
            // Stand base
            Box(
              modifier = Modifier
                .width(28.dp)
                .height(4.dp)
                .background(BrandGold, RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = "PIXEL PATHSHALA",
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              color = BrandGold,
              letterSpacing = 1.5.sp
            )
            Text(
              text = "CODE YOUR DREAM, DECODE THE FUTURE",
              fontSize = 9.sp,
              fontWeight = FontWeight.Medium,
              color = BrandGoldLight.copy(alpha = 0.9f),
              letterSpacing = 0.8.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Segmented Switcher matching screenshot
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF131D31),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
        ) {
          Surface(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .clickable {
                selectedTab = 0
                studentError = null
              }
              .testTag("tab_student_login"),
            shape = RoundedCornerShape(12.dp),
            color = if (selectedTab == 0) BrandPrimary else Color.Transparent
          ) {
            Row(
              modifier = Modifier.padding(vertical = 12.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                tint = if (selectedTab == 0) Color.White else Slate400,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Student Portal",
                fontSize = 14.sp,
                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                color = if (selectedTab == 0) Color.White else Slate400
              )
            }
          }

          Surface(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .clickable {
                selectedTab = 1
                adminError = null
              }
              .testTag("tab_admin_login"),
            shape = RoundedCornerShape(12.dp),
            color = if (selectedTab == 1) BrandPrimary else Color.Transparent
          ) {
            Row(
              modifier = Modifier.padding(vertical = 12.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = null,
                tint = if (selectedTab == 1) Color.White else Slate400,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Admin ERP",
                fontSize = 14.sp,
                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                color = if (selectedTab == 1) Color.White else Slate400
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Main Form Card matching screenshot
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
        color = Color.White
      ) {
        Column(modifier = Modifier.padding(22.dp)) {

          if (selectedTab == 1) {
            // Admin Form
            Text(
              text = "Admin / Educator ERP Sign In",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = Slate900
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Restricted access for Center Directors, Faculty, and Accounts administrators.",
              fontSize = 12.sp,
              color = Slate600,
              lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Error banner if any
            if (adminError != null) {
              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = 16.dp),
                color = ErrorRedLight,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(20.dp)
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = adminError!!,
                    fontSize = 12.sp,
                    color = ErrorRed,
                    fontWeight = FontWeight.Medium
                  )
                }
              }
            }

            OutlinedTextField(
              value = adminUsername,
              onValueChange = { adminUsername = it; adminError = null },
              label = { Text("Username or Email") },
              leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null, tint = BrandPrimary)
              },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_username_input"),
              shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
              value = adminPassword,
              onValueChange = { adminPassword = it; adminError = null },
              label = { Text("Password") },
              leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = BrandPrimary)
              },
              trailingIcon = {
                IconButton(onClick = { adminPasswordVisible = !adminPasswordVisible }) {
                  Icon(
                    imageVector = if (adminPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Toggle password"
                  )
                }
              },
              visualTransformation = if (adminPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_password_input"),
              shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
              onClick = {
                if (adminUsername.isBlank() || adminPassword.isBlank()) {
                  adminError = "Please enter admin username and password / PIN"
                  return@Button
                }
                val success = repository.loginAdmin(adminUsername, adminPassword)
                if (success) {
                  onAdminLoginSuccess()
                } else {
                  adminError = "Invalid admin credentials. Please check username and password."
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("admin_submit_button")
            ) {
              Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Sign In to Admin ERP", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

          } else {
            // Student Form
            Text(
              text = "Student Portal Sign In",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = Slate900
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Access your classes, fee ledger, exams, and study materials.",
              fontSize = 12.sp,
              color = Slate600,
              lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Error banner if any
            if (studentError != null) {
              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = 16.dp),
                color = ErrorRedLight,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(20.dp)
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = studentError!!,
                    fontSize = 12.sp,
                    color = ErrorRed,
                    fontWeight = FontWeight.Medium
                  )
                }
              }
            }

            OutlinedTextField(
              value = studentIdentifier,
              onValueChange = { studentIdentifier = it; studentError = null },
              label = { Text("Roll Number or Mobile") },
              leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null, tint = BrandPrimary)
              },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("student_id_input"),
              shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
              value = studentSecret,
              onValueChange = { studentSecret = it; studentError = null },
              label = { Text("Password") },
              leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = BrandPrimary)
              },
              trailingIcon = {
                IconButton(onClick = { studentPasswordVisible = !studentPasswordVisible }) {
                  Icon(
                    imageVector = if (studentPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Toggle password"
                  )
                }
              },
              visualTransformation = if (studentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
              supportingText = {
                Text(
                  text = "Password is the last 4 digits of your Aadhaar number",
                  fontSize = 11.sp,
                  color = Slate600
                )
              },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("student_secret_input"),
              shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
              onClick = {
                if (studentIdentifier.isBlank()) {
                  studentError = "Please enter your Student ID or Roll No"
                  return@Button
                }
                val success = repository.loginStudent(studentIdentifier, studentSecret)
                if (success) {
                  val loggedIn = repository.currentStudent.value
                  if (loggedIn != null) {
                    try {
                      val tel = StudentDeviceMonitor.collectTelemetry(context, loggedIn)
                      repository.updateStudentTelemetry(tel)
                    } catch (_: Exception) {}
                  }
                  onStudentLoginSuccess()
                } else {
                  studentError = "Student not found in center records. Check details or roll number."
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("student_submit_button")
            ) {
              Icon(Icons.Default.School, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Sign In to Student Portal", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
          }

          Spacer(modifier = Modifier.height(20.dp))
          HorizontalDivider(color = Slate200)
          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showUpdateDialog = true },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = Slate600, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Pixel Pathsala v${com.example.util.UpdateManager.CURRENT_VERSION} • Check for Updates",
              fontSize = 11.sp,
              color = Slate600
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      Text(
        text = "Pixel Pathsala • Coaching & ERP System",
        fontSize = 12.sp,
        color = Slate400,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(32.dp))
    }

    if (showUpdateDialog) {
      com.example.ui.common.AppUpdateDialog(
        onDismiss = { showUpdateDialog = false }
      )
    }
  }
}
