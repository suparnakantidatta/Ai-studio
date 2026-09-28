package com.example.ui.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Student
import com.example.data.repository.PathsalaRepository
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@Composable
fun AdminStudentsTab(
  repository: PathsalaRepository
) {
  val context = LocalContext.current
  val students by repository.students.collectAsState()
  val courses by repository.courses.collectAsState()
  val batches by repository.batches.collectAsState()

  var searchQuery by remember { mutableStateOf("") }
  var filterStatus by remember { mutableStateOf("all") }
  var showAddDialog by remember { mutableStateOf(false) }

  // Add student form inputs
  var newName by remember { mutableStateOf("") }
  var newClass by remember { mutableStateOf("Class 10 (Secondary)") }
  var newMobile by remember { mutableStateOf("") }
  var newAadhaar by remember { mutableStateOf("") }
  var newGuardianName by remember { mutableStateOf("") }
  var newGuardianPhone by remember { mutableStateOf("") }
  var newAddress by remember { mutableStateOf("Raina, Purba Bardhaman") }
  var newCourseId by remember { mutableStateOf(courses.firstOrNull()?.id ?: "course-1") }
  var newBatchId by remember { mutableStateOf(batches.firstOrNull()?.id ?: "batch-1") }
  var newDeliveryMode by remember { mutableStateOf("offline") }
  var recordFeeAtAdmission by remember { mutableStateOf(true) }
  var admissionFeeAmount by remember { mutableStateOf("1200") }

  val filteredStudents = students.filter { s ->
    val matchesSearch = s.name.contains(searchQuery, ignoreCase = true) ||
      s.rollNo.contains(searchQuery, ignoreCase = true) ||
      s.mobile.contains(searchQuery) ||
      s.aadhaarNo.contains(searchQuery)
    val matchesStatus = filterStatus == "all" || s.status == filterStatus
    matchesSearch && matchesStatus
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Top Bar with Add Button
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(text = "Student Directory", fontWeight = FontWeight.Black, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
          Text(text = "${students.size} Students Enrolled", fontSize = 12.sp, color = SlateTextSecondary)
        }

        Button(
          onClick = { showAddDialog = true },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Enroll Student", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    // Search & Filter
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by Name, Roll No, Mobile, Aadhaar...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SlateTextSecondary) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = filterStatus == "all",
              onClick = { filterStatus = "all" },
              label = { Text("All (${students.size})", fontSize = 11.sp) },
              shape = RoundedCornerShape(8.dp)
            )
            FilterChip(
              selected = filterStatus == "active",
              onClick = { filterStatus = "active" },
              label = { Text("Active", fontSize = 11.sp) },
              shape = RoundedCornerShape(8.dp)
            )
            FilterChip(
              selected = filterStatus == "passed_out",
              onClick = { filterStatus = "passed_out" },
              label = { Text("Alumni", fontSize = 11.sp) },
              shape = RoundedCornerShape(8.dp)
            )
          }
        }
      }
    }

    // Student Roster Items
    items(filteredStudents) { student ->
      val course = courses.find { it.id == student.courseId }
      val batch = batches.find { it.id == student.batchId }

      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(Color(0xFFEEF2FF)),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = student.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                  fontWeight = FontWeight.Black,
                  fontSize = 14.sp,
                  color = IndigoPrimary
                )
              }
              Column {
                Text(
                  text = student.name,
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Roll: ${student.rollNo} • Class: ${student.studentClass ?: "Class 10"}",
                  fontSize = 11.sp,
                  color = SlateTextSecondary,
                  fontFamily = FontFamily.Monospace
                )
              }
            }

            StatusBadge(status = student.status)
          }

          // Metadata row
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xFFF8FAFC))
              .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(text = "Course / Batch", fontSize = 10.sp, color = SlateTextSecondary)
              Text(
                text = "${course?.title ?: "Course"} (${batch?.name ?: "Batch"})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SlateTextPrimary
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(text = "Contact Mobile", fontSize = 10.sp, color = SlateTextSecondary)
              Text(text = student.mobile, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
          }

          // Actions
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Aadhaar: ${student.aadhaarNo}",
              fontSize = 11.sp,
              color = SlateTextSecondary,
              fontFamily = FontFamily.Monospace
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              TextButton(
                onClick = {
                  val newStatus = if (student.status == "active") "passed_out" else "active"
                  // update status
                  Toast.makeText(context, "Student status updated", Toast.LENGTH_SHORT).show()
                }
              ) {
                Text(
                  text = if (student.status == "active") "Mark Alumni" else "Mark Active",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = IndigoPrimary
                )
              }

              IconButton(
                onClick = {
                  // delete confirmation
                  Toast.makeText(context, "Deleting student...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
              }
            }
          }
        }
      }
    }
  }

  // Add Student Dialog
  if (showAddDialog) {
    Dialog(onDismissRequest = { showAddDialog = false }) {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.padding(16.dp)
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text("Enroll New Student", fontWeight = FontWeight.Black, fontSize = 17.sp)

          OutlinedTextField(
            value = newName,
            onValueChange = { newName = it },
            label = { Text("Full Name *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = newMobile,
            onValueChange = { if (it.length <= 10) newMobile = it.filter { c -> c.isDigit() } },
            label = { Text("Mobile Number *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = newAadhaar,
            onValueChange = { newAadhaar = it },
            label = { Text("Aadhaar Number *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = newGuardianName,
            onValueChange = { newGuardianName = it },
            label = { Text("Guardian Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          // Delivery Mode (Offline / Online Live)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (newDeliveryMode == "offline") IndigoPrimary else Color(0xFFF1F5F9))
                .clickable { newDeliveryMode = "offline" }
                .padding(vertical = 8.dp),
              contentAlignment = Alignment.Center
            ) {
              Text("Offline Classroom", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (newDeliveryMode == "offline") Color.White else Color(0xFF475569))
            }
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (newDeliveryMode == "online") CyanAccent else Color(0xFFF1F5F9))
                .clickable { newDeliveryMode = "online" }
                .padding(vertical = 8.dp),
              contentAlignment = Alignment.Center
            ) {
              Text("Online Live", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (newDeliveryMode == "online") Color.White else Color(0xFF475569))
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
          ) {
            TextButton(onClick = { showAddDialog = false }) {
              Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = {
                if (newName.isNotBlank() && newMobile.isNotBlank()) {
                  // Enroll student
                  showAddDialog = false
                  Toast.makeText(context, "Student $newName enrolled successfully!", Toast.LENGTH_SHORT).show()
                } else {
                  Toast.makeText(context, "Please fill Name and Mobile", Toast.LENGTH_SHORT).show()
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
            ) {
              Text("Save & Enroll")
            }
          }
        }
      }
    }
  }
}
