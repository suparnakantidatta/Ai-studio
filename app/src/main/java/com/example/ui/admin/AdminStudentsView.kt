package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Batch
import com.example.data.model.Course
import com.example.data.model.Student
import com.example.data.model.StudentTelemetry
import com.example.ui.theme.*

@Composable
fun AdminStudentsView(
  students: List<Student>,
  telemetries: List<StudentTelemetry>,
  courses: List<Course>,
  batches: List<Batch>,
  onOpenNewAdmission: () -> Unit,
  onEditStudent: (Student) -> Unit,
  onDeleteStudent: (String) -> Unit
) {
  var query by remember { mutableStateOf("") }
  var statusFilter by remember { mutableStateOf("all") } // all, active, alumni, tracker

  val filteredStudents = remember(query, statusFilter, students) {
    var list = if (statusFilter == "active") {
      students.filter { it.status.equals("active", ignoreCase = true) }
    } else if (statusFilter == "alumni") {
      students.filter { it.status.equals("alumni", ignoreCase = true) }
    } else {
      students
    }

    if (query.isNotBlank()) {
      list = list.filter {
        it.name.contains(query, ignoreCase = true) ||
        it.rollNo.contains(query, ignoreCase = true) ||
        it.mobile.contains(query) ||
        it.aadhaarNo.contains(query)
      }
    }
    list
  }

  if (statusFilter == "tracker") {
    // Live Tracker & Games view
    Column(modifier = Modifier.fillMaxSize()) {
      Surface(
        color = Color.White,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Student Device & Gaming Telemetry",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Slate900
          )
          OutlinedButton(
            onClick = { statusFilter = "all" },
            shape = RoundedCornerShape(8.dp)
          ) {
            Text("Back to Directory", fontSize = 12.sp)
          }
        }
      }
      AdminTelemetryTrackerTab(students = students, telemetries = telemetries)
    }
  } else {
    // Directory View matching Screenshot 2
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Header matching Screenshot 2
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Student Directory",
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold,
              color = Slate900
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "${students.size} Students Enrolled",
              fontSize = 12.sp,
              color = Slate600
            )
          }

          Button(
            onClick = onOpenNewAdmission,
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Enroll Student", fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      // Search Box matching Screenshot 2
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            OutlinedTextField(
              value = query,
              onValueChange = { query = it },
              placeholder = { Text("Search by Name, Roll No, Mobile, Aadhaar...") },
              leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate600) },
              trailingIcon = {
                if (query.isNotEmpty()) {
                  IconButton(onClick = { query = "" }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                  }
                }
              },
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips matching Screenshot 2
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              FilterChip(
                selected = statusFilter == "all",
                onClick = { statusFilter = "all" },
                label = { Text("All (${students.size})", fontSize = 12.sp) },
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Slate200,
                  selectedLabelColor = Slate900
                )
              )

              FilterChip(
                selected = statusFilter == "active",
                onClick = { statusFilter = "active" },
                label = { Text("Active", fontSize = 12.sp) },
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Slate200,
                  selectedLabelColor = Slate900
                )
              )

              FilterChip(
                selected = statusFilter == "alumni",
                onClick = { statusFilter = "alumni" },
                label = { Text("Alumni", fontSize = 12.sp) },
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Slate200,
                  selectedLabelColor = Slate900
                )
              )

              FilterChip(
                selected = false,
                onClick = { statusFilter = "tracker" },
                leadingIcon = {
                  Icon(Icons.Default.Smartphone, contentDescription = null, modifier = Modifier.size(14.dp))
                },
                label = { Text("Device Tracker", fontSize = 12.sp) },
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                  containerColor = BrandGoldLight.copy(alpha = 0.5f),
                  labelColor = BrandGoldDark
                )
              )
            }
          }
        }
      }

      // Student Cards matching Screenshot 2
      items(filteredStudents) { st ->
        val course = courses.firstOrNull { it.id == st.courseId }
        val batch = batches.firstOrNull { it.id == st.batchId }
        val initials = st.name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")

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
                // Initials Circle
                Box(
                  modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFFE0E7FF), CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = initials.ifBlank { "ST" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary
                  )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                  Text(
                    text = st.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                  )
                  Text(
                    text = "Roll: ${st.rollNo} • Class: ${st.studentClass ?: "Class 11"}",
                    fontSize = 11.sp,
                    color = Slate600
                  )
                }
              }

              Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (st.status.equals("active", true)) Color(0xFFDCFCE7) else Slate100,
                border = BorderStroke(1.dp, if (st.status.equals("active", true)) SuccessGreen.copy(alpha = 0.5f) else Slate300)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(12.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = st.status.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (st.status.equals("active", true)) SuccessGreen else Slate700
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges in ONE LINE and catchy
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (st.status.equals("active", true)) Color(0xFFDCFCE7) else Slate100,
                border = BorderStroke(1.dp, if (st.status.equals("active", true)) SuccessGreen.copy(alpha = 0.4f) else Slate300)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .size(6.dp)
                      .background(if (st.status.equals("active", true)) SuccessGreen else Slate500, CircleShape)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = st.status.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (st.status.equals("active", true)) SuccessGreen else Slate700
                  )
                }
              }

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = BrandPrimary.copy(alpha = 0.1f)
              ) {
                Text(
                  text = "Roll: ${st.rollNo}",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = BrandPrimary,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = BrandGold.copy(alpha = 0.15f)
              ) {
                Text(
                  text = st.studentClass ?: course?.academicClass ?: "Class 11",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = BrandGoldDark,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE0E7FF)
              ) {
                Text(
                  text = (st.mode.ifBlank { batch?.mode ?: "offline" }).uppercase(),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF3730A3),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }

              val netRate = ((course?.monthlyFee ?: 400.0) - (st.monthlyDiscount ?: 0.0)).toInt()
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFEF3C7)
              ) {
                Text(
                  text = "₹$netRate/mo",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF92400E),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Info Box Container matching Screenshot 2
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFFF8FAFC),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = "Course / Batch",
                  fontSize = 10.sp,
                  color = Slate500
                )
                Text(
                  text = "${course?.title ?: "COMPUTER SCIENCE (XI)"} (${batch?.name ?: "MORNING BATCH (COMS-011)"})",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = Slate900
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = "Rate: ₹${course?.monthlyFee?.toInt() ?: 400}/mo (-₹${st.monthlyDiscount?.toInt() ?: 100} discount) = ₹${((course?.monthlyFee ?: 400.0) - (st.monthlyDiscount ?: 0.0)).toInt()}/mo net",
                    fontSize = 11.sp,
                    color = BrandPrimary,
                    fontWeight = FontWeight.SemiBold
                  )
                  Text(
                    text = st.mobile,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                  )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = "Aadhaar: ${st.aadhaarNo}",
                    fontSize = 11.sp,
                    color = Slate600
                  )
                  Text(
                    text = "Adm: ${st.admissionDate.ifBlank { "2026-07-01" }}",
                    fontSize = 10.sp,
                    color = Slate500
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons row matching Screenshot 2
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedButton(
                onClick = { onEditStudent(st) },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
              ) {
                Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit Fee/Adm", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                  onClick = { statusFilter = "tracker" },
                  contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                  Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Tracker", fontSize = 11.sp, color = BrandPrimary, fontWeight = FontWeight.Bold)
                }

                IconButton(
                  onClick = { onDeleteStudent(st.id) },
                  modifier = Modifier.size(36.dp)
                ) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed, modifier = Modifier.size(18.dp))
                }
              }
            }
          }
        }
      }
    }
  }
}
