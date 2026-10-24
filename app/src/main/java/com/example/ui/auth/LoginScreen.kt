package com.example.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AdmissionApplication
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

  // Pending admission dialog state
  var pendingApplicantDialog by remember { mutableStateOf<AdmissionApplication?>(null) }

  // Admin Form State
  var adminUsername by remember { mutableStateOf("") }
  var adminPassword by remember { mutableStateOf("") }
  var adminPasswordVisible by remember { mutableStateOf(false) }
  var adminError by remember { mutableStateOf<String?>(null) }

  val scrollState = rememberScrollState()

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color(0xFF070B14),
            Color(0xFF0D1527),
            Color(0xFF080C18)
          )
        )
      )
  ) {
    // Top bar with Dark mode toggle
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
          .size(38.dp)
      ) {
        Icon(
          imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
          contentDescription = "Toggle Dark Mode",
          tint = BrandGold,
          modifier = Modifier.size(20.dp)
        )
      }
    }

    // Centered Login Card & Input Area
    Box(
      modifier = Modifier
        .fillMaxSize()
        .imePadding()
        .verticalScroll(scrollState)
        .padding(top = 48.dp, bottom = 24.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 480.dp)
          .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        // Logo & Brand Header
        Image(
          painter = painterResource(id = R.drawable.pixel_logo),
          contentDescription = "Pixel Pathsala",
          modifier = Modifier
            .fillMaxWidth(0.68f)
            .height(64.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Coaching ERP & Learning Management System",
          fontSize = 12.sp,
          color = Slate400,
          fontWeight = FontWeight.Medium,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Segmented Switcher (Student Portal vs Admin ERP)
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

        Spacer(modifier = Modifier.height(18.dp))

        // Main Login Card
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(22.dp)),
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
          Column(modifier = Modifier.padding(22.dp)) {

            if (selectedTab == 1) {
              // --- ADMIN ERP FORM ---
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFEEF2FF), CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(20.dp)
                  )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(
                    text = "Admin / Educator ERP",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                  )
                  Text(
                    text = "Restricted management access",
                    fontSize = 12.sp,
                    color = Slate600
                  )
                }
              }

              Spacer(modifier = Modifier.height(16.dp))

              // Admin Error banner
              if (adminError != null) {
                Surface(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                  color = ErrorRedLight,
                  shape = RoundedCornerShape(10.dp),
                  border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f))
                ) {
                  Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Default.Error,
                      contentDescription = null,
                      tint = ErrorRed,
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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
                placeholder = { Text("admin") },
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
                label = { Text("Password or Security PIN") },
                placeholder = { Text("admin123 / 7722") },
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
                  .height(50.dp)
                  .testTag("admin_submit_button")
              ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign In to Admin ERP", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
              }

            } else {
              // --- STUDENT PORTAL FORM ---
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFEFF6FF), CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(20.dp)
                  )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(
                    text = "Student Sign In",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                  )
                  Text(
                    text = "Access batch routine, fees, and exams",
                    fontSize = 12.sp,
                    color = Slate600
                  )
                }
              }

              Spacer(modifier = Modifier.height(14.dp))



              // Student Error banner
              if (studentError != null) {
                Surface(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                  color = ErrorRedLight,
                  shape = RoundedCornerShape(10.dp),
                  border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f))
                ) {
                  Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Default.Error,
                      contentDescription = null,
                      tint = ErrorRed,
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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
                placeholder = { Text("e.g. PP-2026-101 or 9876543210") },
                leadingIcon = {
                  Icon(Icons.Default.Badge, contentDescription = null, tint = BrandPrimary)
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
                placeholder = { Text("Last 4 digits of Aadhaar") },
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

              Spacer(modifier = Modifier.height(18.dp))

              Button(
                onClick = {
                  val trimmedId = studentIdentifier.trim()
                  if (trimmedId.isBlank()) {
                    studentError = "Please enter your Student ID, Roll No, or Mobile"
                    return@Button
                  }

                  // Check if this student is an applicant whose admission is still pending
                  val pendingApp = repository.checkPendingAdmissionStatus(trimmedId)
                  if (pendingApp != null) {
                    pendingApplicantDialog = pendingApp
                    return@Button
                  }

                  val success = repository.loginStudent(trimmedId, studentSecret)
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
                    studentError = "Student record not found or password incorrect. If you recently applied, please wait for center approval."
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(50.dp)
                  .testTag("student_submit_button")
              ) {
                Icon(Icons.Default.School, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign In to Student Portal", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
          text = "Pixel Pathsala • Coaching ERP System",
          fontSize = 12.sp,
          color = Slate500,
          textAlign = TextAlign.Center
        )
      }
    }
  }

  // Pending Admission Warning Dialog
  pendingApplicantDialog?.let { applicant ->
    AlertDialog(
      onDismissRequest = { pendingApplicantDialog = null },
      icon = {
        Icon(
          imageVector = Icons.Default.HourglassTop,
          contentDescription = "Pending Approval",
          tint = Color(0xFFD97706),
          modifier = Modifier.size(36.dp)
        )
      },
      title = {
        Text(
          text = "Admission Pending Approval",
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
          color = Slate900
        )
      },
      text = {
        Column {
          Text(
            text = "Dear ${applicant.studentName},",
            fontWeight = FontWeight.SemiBold,
            color = Slate800,
            fontSize = 14.sp
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Your admission application (Application ID: ${applicant.applicationNo.ifBlank { applicant.id }}) is currently under review by the center administration.",
            fontSize = 13.sp,
            color = Slate600,
            lineHeight = 18.sp
          )
          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            color = Color(0xFFFFFBEB),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "⚠️ Please do NOT attempt to log in until your application is approved. Once approved, you will be allocated your enrolled batch and official Student Roll Number.",
              fontSize = 12.sp,
              color = Color(0xFF92400E),
              modifier = Modifier.padding(10.dp),
              lineHeight = 16.sp
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = { pendingApplicantDialog = null },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
        ) {
          Text("I Understand")
        }
      }
    )
  }
}
