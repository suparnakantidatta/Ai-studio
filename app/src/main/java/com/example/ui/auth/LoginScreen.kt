package com.example.ui.auth

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.PathsalaRepository
import com.example.ui.theme.*

@Composable
fun LoginScreen(
  repository: PathsalaRepository,
  onStudentLoginSuccess: () -> Unit,
  onAdminLoginSuccess: () -> Unit
) {
  var selectedTab by remember { mutableIntStateOf(0) } // 0: Student, 1: Admin
  val isSyncing by repository.isSyncing.collectAsState()

  // Student Form State
  var studentIdentifier by remember { mutableStateOf("") }
  var studentSecret by remember { mutableStateOf("") }
  var studentError by remember { mutableStateOf<String?>(null) }

  // Admin Form State
  var adminUsername by remember { mutableStateOf("") }
  var adminPassword by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var adminError by remember { mutableStateOf<String?>(null) }
  var showUpdateDialog by remember { mutableStateOf(false) }

  val scrollState = rememberScrollState()

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Slate50)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
    ) {
      if (com.example.util.UpdateManager.isUpdateAvailable()) {
        com.example.ui.common.AppUpdateBanner(
          onClick = { showUpdateDialog = true },
          modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 36.dp, bottom = 0.dp)
        )
      }
      // Header Banner
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(
            Brush.verticalGradient(
              colors = listOf(BrandSecondary, BrandPrimary)
            )
          )
          .padding(top = 48.dp, bottom = 32.dp, start = 24.dp, end = 24.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Box(
            modifier = Modifier
              .size(68.dp)
              .background(BrandGold, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.School,
              contentDescription = "Logo",
              tint = BrandSecondary,
              modifier = Modifier.size(38.dp)
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Text(
            text = "Pixel Pathsala",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )

          Text(
            text = "Tuition & Coaching ERP Management",
            fontSize = 13.sp,
            color = BrandGoldLight
          )
        }
      }

      // Card Container
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .offset(y = (-16).dp)
          .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
      ) {
        Column(modifier = Modifier.padding(20.dp)) {

          // Role Switcher Tab
          TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Slate100,
            contentColor = BrandPrimary,
            indicator = {},
            divider = {},
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .padding(4.dp)
          ) {
            Tab(
              selected = selectedTab == 0,
              onClick = { selectedTab = 0; studentError = null },
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (selectedTab == 0) Color.White else Color.Transparent)
                .testTag("tab_student_login")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Person,
                  contentDescription = null,
                  tint = if (selectedTab == 0) BrandPrimary else Slate600,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Student Portal",
                  fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                  color = if (selectedTab == 0) BrandPrimary else Slate600,
                  fontSize = 14.sp
                )
              }
            }

            Tab(
              selected = selectedTab == 1,
              onClick = { selectedTab = 1; adminError = null },
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (selectedTab == 1) Color.White else Color.Transparent)
                .testTag("tab_admin_login")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.AdminPanelSettings,
                  contentDescription = null,
                  tint = if (selectedTab == 1) BrandPrimary else Slate600,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Admin Desk",
                  fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                  color = if (selectedTab == 1) BrandPrimary else Slate600,
                  fontSize = 14.sp
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(20.dp))

          if (selectedTab == 0) {
            // Student Login Form
            Text(
              text = "Student Sign In",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = Slate900
            )
            Text(
              text = "Enter your Student ID, Roll No, or registered Mobile number",
              fontSize = 12.sp,
              color = Slate600
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
              value = studentIdentifier,
              onValueChange = { studentIdentifier = it; studentError = null },
              label = { Text("Student ID / Roll / Mobile") },
              leadingIcon = {
                Icon(Icons.Default.Badge, contentDescription = null, tint = BrandPrimary)
              },
              placeholder = { Text("e.g. PP-2026-005 or 7001561342") },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("student_id_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
              value = studentSecret,
              onValueChange = { studentSecret = it; studentError = null },
              label = { Text("Passcode (Optional / Roll No)") },
              leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = BrandPrimary)
              },
              placeholder = { Text("Leave blank or enter roll number") },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("student_secret_input")
            )

            if (studentError != null) {
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = studentError!!,
                color = ErrorRed,
                fontSize = 12.sp
              )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
              onClick = {
                if (studentIdentifier.isBlank()) {
                  studentError = "Please enter your Student ID or Roll No"
                  return@Button
                }
                val success = repository.loginStudent(studentIdentifier, studentSecret)
                if (success) {
                  onStudentLoginSuccess()
                } else {
                  studentError = "Student not found in center records. Check details or sync."
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("student_submit_button")
            ) {
              Text("Enter Student Portal", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }

          } else {
            // Admin Login Form
            Text(
              text = "Administrator Login",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = Slate900
            )
            Text(
              text = "Director & Faculty Management Access",
              fontSize = 12.sp,
              color = Slate600
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
              value = adminUsername,
              onValueChange = { adminUsername = it; adminError = null },
              label = { Text("Username / Email") },
              leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null, tint = BrandPrimary)
              },
              placeholder = { Text("admin") },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_username_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
              value = adminPassword,
              onValueChange = { adminPassword = it; adminError = null },
              label = { Text("Password or Security PIN") },
              leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = BrandPrimary)
              },
              trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                  Icon(
                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Toggle password"
                  )
                }
              },
              visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
              placeholder = { Text("admin123 or PIN 7722") },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_password_input")
            )

            if (adminError != null) {
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = adminError!!,
                color = ErrorRed,
                fontSize = 12.sp
              )
            }

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
                  adminError = "Invalid administrator credentials. (Default: admin / admin123 or PIN 7722)"
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = BrandGoldDark),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("admin_submit_button")
            ) {
              Text("Sign In as Administrator", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
          }

          Spacer(modifier = Modifier.height(16.dp))
          HorizontalDivider(color = Slate200)
          Spacer(modifier = Modifier.height(12.dp))

          // Cloud Sync button
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.CloudSync,
                contentDescription = null,
                tint = if (isSyncing) BrandGold else BrandPrimary,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (isSyncing) "Syncing with Google Sheets..." else "Backend: Google Sheet ERP",
                fontSize = 12.sp,
                color = Slate600
              )
            }

            TextButton(onClick = { repository.refreshData() }) {
              Text("Refresh Data", fontSize = 12.sp, color = BrandPrimary, fontWeight = FontWeight.Bold)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
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

      Spacer(modifier = Modifier.height(32.dp))
    }

    if (showUpdateDialog) {
      com.example.ui.common.AppUpdateDialog(
        onDismiss = { showUpdateDialog = false }
      )
    }
  }
}
