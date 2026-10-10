package com.example.ui.auth

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.PathsalaRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
  repository: PathsalaRepository,
  onStudentLoginSuccess: () -> Unit,
  onAdminLoginSuccess: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var selectedRole by remember { mutableStateOf<String>("student") } // "student" or "admin"

  // Student inputs
  var studentMobile by remember { mutableStateOf("") }
  var studentAadhaar by remember { mutableStateOf("") }
  var studentLoading by remember { mutableStateOf(false) }
  var studentError by remember { mutableStateOf<String?>(null) }

  // Admin inputs
  var adminUsername by remember { mutableStateOf("") }
  var adminPassword by remember { mutableStateOf("") }
  var adminPasswordVisible by remember { mutableStateOf(false) }
  var adminLoading by remember { mutableStateOf(false) }
  var adminError by remember { mutableStateOf<String?>(null) }

  val scrollState = rememberScrollState()

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color(0xFF000000),
            Color(0xFF0F172A),
            Color(0xFF000000)
          )
        )
      )
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .imePadding()
        .verticalScroll(scrollState)
        .padding(horizontal = 24.dp, vertical = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Spacer(modifier = Modifier.height(12.dp))

      // Official Gold Emblem Logo
      Box(
        modifier = Modifier
          .fillMaxWidth(0.85f)
          .height(160.dp)
          .clip(RoundedCornerShape(24.dp))
          .background(Color.Black)
          .border(1.5.dp, Color(0xFFD4AF37).copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
      ) {
        Image(
          painter = painterResource(id = R.drawable.ic_pixel_pathsala_logo),
          contentDescription = "Pixel Pathsala Logo",
          contentScale = ContentScale.Fit,
          modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
        )
      }

      // Role Selection Tabs (Student vs Admin)
      Surface(
        color = Color(0x331E293B),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x40D4AF37)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
        ) {
          // Student Tab
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(14.dp))
              .background(if (selectedRole == "student") IndigoPrimary else Color.Transparent)
              .clickable {
                selectedRole = "student"
                studentError = null
              }
              .padding(vertical = 11.dp),
            contentAlignment = Alignment.Center
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                tint = if (selectedRole == "student") Color.White else Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = "Student Portal",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (selectedRole == "student") Color.White else Color(0xFF94A3B8)
              )
            }
          }

          // Admin Tab
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(14.dp))
              .background(if (selectedRole == "admin") IndigoPrimary else Color.Transparent)
              .clickable {
                selectedRole = "admin"
                adminError = null
              }
              .padding(vertical = 11.dp),
            contentAlignment = Alignment.Center
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = null,
                tint = if (selectedRole == "admin") Color.White else Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = "Admin ERP",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (selectedRole == "admin") Color.White else Color(0xFF94A3B8)
              )
            }
          }
        }
      }

      // Card Container for Login Form
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          if (selectedRole == "student") {
            // STUDENT FORM
            Text(
              text = "Student Sign In",
              fontWeight = FontWeight.Black,
              fontSize = 18.sp,
              color = Color(0xFF0F172A)
            )
            Text(
              text = "Enter your registered mobile number and Aadhaar number (or Roll number) to access your classes, fees & exams.",
              fontSize = 12.sp,
              color = SlateTextSecondary,
              lineHeight = 16.sp
            )

            if (studentError != null) {
              Surface(
                color = Color(0xFFFEF2F2),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                  Text(text = studentError!!, color = Color(0xFFB91C1C), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
              }
            }

            val isStudentFormValid = studentMobile.length == 10 && studentAadhaar.trim().isNotBlank()

            OutlinedTextField(
              value = studentMobile,
              onValueChange = { if (it.length <= 10) studentMobile = it.filter { char -> char.isDigit() } },
              label = { Text("10-Digit Mobile Number") },
              placeholder = { Text("Enter registered mobile number") },
              leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = IndigoPrimary) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
              value = studentAadhaar,
              onValueChange = { studentAadhaar = it },
              label = { Text("Aadhaar (12 digits / last 4) or Roll No *") },
              placeholder = { Text("Enter registered Aadhaar or Roll No") },
              leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = IndigoPrimary) },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth(),
              supportingText = {
                Text(
                  text = "Aadhaar number or Roll number is required to verify student identity.",
                  fontSize = 11.sp,
                  color = if (studentAadhaar.isBlank() && studentMobile.length == 10) Color(0xFFDC2626) else SlateTextSecondary
                )
              }
            )

            Button(
              onClick = {
                if (studentMobile.length < 10) {
                  studentError = "Please enter a valid 10-digit registered mobile number."
                  return@Button
                }
                if (studentAadhaar.trim().isBlank()) {
                  studentError = "Please enter your registered Aadhaar number or Roll number."
                  return@Button
                }
                studentError = null
                studentLoading = true
                coroutineScope.launch {
                  val result = repository.loginStudent(studentMobile, studentAadhaar.trim())
                  studentLoading = false
                  result.onSuccess {
                    Toast.makeText(context, "Welcome back, ${it.name}!", Toast.LENGTH_SHORT).show()
                    onStudentLoginSuccess()
                  }.onFailure {
                    studentError = it.message ?: "Authentication failed. Please verify credentials."
                  }
                }
              },
              enabled = !studentLoading && isStudentFormValid,
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
            ) {
              if (studentLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
              } else {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                  Text("Login to Student Portal", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
              }
            }

          } else {
            // ADMIN FORM
            Text(
              text = "Admin / Educator ERP Sign In",
              fontWeight = FontWeight.Black,
              fontSize = 18.sp,
              color = Color(0xFF0F172A)
            )
            Text(
              text = "Restricted access for Center Directors, Faculty, and Accounts administrators.",
              fontSize = 12.sp,
              color = SlateTextSecondary
            )

            if (adminError != null) {
              Surface(
                color = Color(0xFFFEF2F2),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                  Text(text = adminError!!, color = Color(0xFFB91C1C), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
              }
            }

            OutlinedTextField(
              value = adminUsername,
              onValueChange = { adminUsername = it },
              label = { Text("Username or Email") },
              placeholder = { Text("Enter username or email") },
              leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = IndigoPrimary) },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
              value = adminPassword,
              onValueChange = { adminPassword = it },
              label = { Text("Password") },
              placeholder = { Text("Enter password") },
              leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = IndigoPrimary) },
              trailingIcon = {
                IconButton(onClick = { adminPasswordVisible = !adminPasswordVisible }) {
                  Icon(
                    imageVector = if (adminPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle password visibility"
                  )
                }
              },
              visualTransformation = if (adminPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth()
            )

            Button(
              onClick = {
                adminError = null
                adminLoading = true
                coroutineScope.launch {
                  val result = repository.loginAdmin(adminUsername, adminPassword)
                  adminLoading = false
                  result.onSuccess {
                    Toast.makeText(context, "Welcome, ${it.name}!", Toast.LENGTH_SHORT).show()
                    onAdminLoginSuccess()
                  }.onFailure {
                    adminError = it.message ?: "Invalid admin credentials"
                  }
                }
              },
              enabled = !adminLoading && adminUsername.isNotBlank() && adminPassword.isNotBlank(),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
            ) {
              if (adminLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
              } else {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                  Text("Sign In to Admin ERP", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }

      // Footer branding
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = "Pixel Pathsala • Coaching & ERP System",
          color = Color(0xFF94A3B8),
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium
        )
      }
    }
  }
}
