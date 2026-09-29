package com.example.ui.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
  var newClass by remember { mutableStateOf("Class 11") }
  var newMobile by remember { mutableStateOf("") }
  var newAadhaar by remember { mutableStateOf("") }
  var newEmail by remember { mutableStateOf("") }
  var newGuardianName by remember { mutableStateOf("") }
  var newGuardianPhone by remember { mutableStateOf("") }
  var newAddress by remember { mutableStateOf("Raina, Purba Bardhaman") }
  var newCourseId by remember { mutableStateOf(courses.firstOrNull()?.id ?: "course-1788019409876") }
  var newBatchId by remember { mutableStateOf(batches.firstOrNull()?.id ?: "batch-1788019693229") }
  var newDeliveryMode by remember { mutableStateOf("offline") }
  var recordFeeAtAdmission by remember { mutableStateOf(true) }
  var admissionFeeAmount by remember { mutableStateOf("400") }
  var customFeeOverrideText by remember { mutableStateOf("") }
  var paymentModeChoice by remember { mutableStateOf("CASH") }
  var txnRefText by remember { mutableStateOf("CASH-COUNTER") }

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
                  repository.updateStudentStatus(student.id, newStatus)
                  Toast.makeText(context, "${student.name} marked as $newStatus & synced to Google Sheet", Toast.LENGTH_SHORT).show()
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
                  repository.deleteStudent(student.id)
                  Toast.makeText(context, "Student deleted and removed from Google Sheet", Toast.LENGTH_SHORT).show()
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

  // Add Student Dialog (matching Web ERP & Google Sheet schema)
  if (showAddDialog) {
    Dialog(
      onDismissRequest = { showAddDialog = false },
      properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
          .fillMaxWidth(0.95f)
          .fillMaxHeight(0.92f)
          .padding(8.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
        ) {
          // Header
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("Enroll New Student", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF0F172A))
              Text("Connected with Google Sheet & Center ERP", fontSize = 11.sp, color = SlateTextSecondary)
            }
            IconButton(onClick = { showAddDialog = false }) {
              Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateTextSecondary)
            }
          }

          HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = SlateBorder)

          // Scrollable Form Body
          val dialogScroll = rememberScrollState()
          Column(
            modifier = Modifier
              .weight(1f)
              .verticalScroll(dialogScroll),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            // Section 1: Academic & Batch Allotment
            Text("1. ACADEMIC & BATCH ALLOTMENT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IndigoPrimary)

            // Course Dropdown
            var courseDropdownExpanded by remember { mutableStateOf(false) }
            val selectedCourse = courses.find { it.id == newCourseId } ?: courses.firstOrNull()
            Column {
              Text("Target Course *", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = SlateTextPrimary)
              Spacer(modifier = Modifier.height(4.dp))
              Box {
                OutlinedCard(
                  onClick = { courseDropdownExpanded = true },
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column {
                      Text(selectedCourse?.title ?: "Select Course", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                      Text("${selectedCourse?.code ?: ""} • Standard Fee: ₹${selectedCourse?.monthlyFee?.toInt() ?: 400}/month", fontSize = 11.sp, color = SlateTextSecondary)
                    }
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                  }
                }
                DropdownMenu(
                  expanded = courseDropdownExpanded,
                  onDismissRequest = { courseDropdownExpanded = false }
                ) {
                  courses.forEach { course ->
                    DropdownMenuItem(
                      text = {
                        Column {
                          Text(course.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                          Text("${course.code} • ₹${course.monthlyFee.toInt()}/mo", fontSize = 11.sp, color = SlateTextSecondary)
                        }
                      },
                      onClick = {
                        newCourseId = course.id
                        val firstBatch = batches.find { it.courseId == course.id }
                        if (firstBatch != null) newBatchId = firstBatch.id
                        newClass = course.academicClass
                        admissionFeeAmount = course.monthlyFee.toInt().toString()
                        courseDropdownExpanded = false
                      }
                    )
                  }
                }
              }
            }

            // Batch Dropdown (filtered by selected course)
            val filteredBatches = batches.filter { it.courseId == newCourseId }
            val selectedBatch = filteredBatches.find { it.id == newBatchId } ?: filteredBatches.firstOrNull() ?: batches.find { it.id == newBatchId }
            var batchDropdownExpanded by remember { mutableStateOf(false) }
            Column {
              Text("Assigned Batch *", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = SlateTextPrimary)
              Spacer(modifier = Modifier.height(4.dp))
              Box {
                OutlinedCard(
                  onClick = { batchDropdownExpanded = true },
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column {
                      Text(selectedBatch?.name ?: "Select Batch", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                      Text("${selectedBatch?.timing ?: ""} • ${selectedBatch?.scheduleDays?.joinToString() ?: ""}", fontSize = 11.sp, color = SlateTextSecondary)
                    }
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                  }
                }
                DropdownMenu(
                  expanded = batchDropdownExpanded,
                  onDismissRequest = { batchDropdownExpanded = false }
                ) {
                  val listToShow = if (filteredBatches.isNotEmpty()) filteredBatches else batches
                  listToShow.forEach { batch ->
                    DropdownMenuItem(
                      text = {
                        Column {
                          Text(batch.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                          Text("${batch.timing} • ${batch.scheduleDays.joinToString()} (${batch.mode})", fontSize = 11.sp, color = SlateTextSecondary)
                        }
                      },
                      onClick = {
                        newBatchId = batch.id
                        newDeliveryMode = batch.mode
                        batchDropdownExpanded = false
                      }
                    )
                  }
                }
              }
            }

            // Academic Class & Delivery Mode
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("Academic Class", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  listOf("Class 11", "Class 12").forEach { cls ->
                    FilterChip(
                      selected = newClass.contains(cls),
                      onClick = { newClass = cls },
                      label = { Text(cls, fontSize = 11.sp) },
                      shape = RoundedCornerShape(8.dp)
                    )
                  }
                }
              }

              Column(modifier = Modifier.weight(1f)) {
                Text("Delivery Mode", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  listOf("offline" to "Classroom", "online" to "Live").forEach { (modeVal, modeLabel) ->
                    FilterChip(
                      selected = newDeliveryMode == modeVal,
                      onClick = { newDeliveryMode = modeVal },
                      label = { Text(modeLabel, fontSize = 11.sp) },
                      shape = RoundedCornerShape(8.dp)
                    )
                  }
                }
              }
            }

            HorizontalDivider(color = SlateBorder)

            // Section 2: Student Personal Details
            Text("2. STUDENT PERSONAL DETAILS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IndigoPrimary)

            OutlinedTextField(
              value = newName,
              onValueChange = { newName = it },
              label = { Text("Full Name *") },
              placeholder = { Text("e.g. Abhinaba Som") },
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedTextField(
                value = newMobile,
                onValueChange = { if (it.length <= 10) newMobile = it.filter { c -> c.isDigit() } },
                label = { Text("Mobile (10 digits) *") },
                placeholder = { Text("9647750688") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
              )

              OutlinedTextField(
                value = newAadhaar,
                onValueChange = { newAadhaar = it },
                label = { Text("Aadhaar (12 digits) *") },
                placeholder = { Text("3056 3793 9454") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
              )
            }

            OutlinedTextField(
              value = newEmail,
              onValueChange = { newEmail = it },
              label = { Text("Email Address (Optional)") },
              placeholder = { Text("somabhinaba@gmail.com") },
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
              value = newAddress,
              onValueChange = { newAddress = it },
              label = { Text("Residential Address *") },
              placeholder = { Text("Narayanpur, Bokra, Purba Bardhaman") },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(color = SlateBorder)

            // Section 3: Guardian Details
            Text("3. GUARDIAN / PARENT DETAILS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IndigoPrimary)

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedTextField(
                value = newGuardianName,
                onValueChange = { newGuardianName = it },
                label = { Text("Guardian Name *") },
                placeholder = { Text("Suprabhat Som") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
              )

              OutlinedTextField(
                value = newGuardianPhone,
                onValueChange = { if (it.length <= 10) newGuardianPhone = it.filter { c -> c.isDigit() } },
                label = { Text("Guardian Phone *") },
                placeholder = { Text("9647750688") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
              )
            }

            HorizontalDivider(color = SlateBorder)

            // Section 4: Fees & Initial Payment
            Text("4. FEES & ADMISSION RECEIPT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IndigoPrimary)

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text("Standard Monthly Fee", fontSize = 10.sp, color = SlateTextSecondary)
                  Text("₹${selectedCourse?.monthlyFee?.toInt() ?: 400}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = IndigoPrimary)
                }
              }

              OutlinedTextField(
                value = customFeeOverrideText,
                onValueChange = { customFeeOverrideText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Fee Override (₹)") },
                placeholder = { Text("Custom fee if any") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.3f)
              )
            }

            // Collect initial fee toggle
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = if (recordFeeAtAdmission) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
              border = androidx.compose.foundation.BorderStroke(1.dp, if (recordFeeAtAdmission) Color(0xFF86EFAC) else SlateBorder),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column {
                    Text("Collect Initial Fee at Admission", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                    Text("Auto-generate official receipt immediately", fontSize = 11.sp, color = SlateTextSecondary)
                  }
                  Switch(
                    checked = recordFeeAtAdmission,
                    onCheckedChange = { recordFeeAtAdmission = it }
                  )
                }

                if (recordFeeAtAdmission) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    OutlinedTextField(
                      value = admissionFeeAmount,
                      onValueChange = { admissionFeeAmount = it.filter { c -> c.isDigit() } },
                      label = { Text("Amount Paid (₹)") },
                      singleLine = true,
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier.weight(1f)
                    )

                    Column(modifier = Modifier.weight(1.2f)) {
                      Text("Payment Mode", fontSize = 11.sp, color = SlateTextSecondary)
                      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("CASH", "UPI").forEach { mode ->
                          FilterChip(
                            selected = paymentModeChoice == mode,
                            onClick = {
                              paymentModeChoice = mode
                              txnRefText = if (mode == "CASH") "CASH-COUNTER" else ""
                            },
                            label = { Text(mode, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(6.dp)
                          )
                        }
                      }
                    }
                  }

                  if (paymentModeChoice == "UPI") {
                    OutlinedTextField(
                      value = txnRefText,
                      onValueChange = { txnRefText = it },
                      label = { Text("UPI Transaction ID / Ref") },
                      placeholder = { Text("e.g. 9775708722@apl / UPI Ref") },
                      singleLine = true,
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier.fillMaxWidth()
                    )
                  }
                }
              }
            }
          }

          HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SlateBorder)

          // Actions
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
          ) {
            TextButton(onClick = { showAddDialog = false }) {
              Text("Cancel", fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
              onClick = {
                if (newName.isBlank() || newMobile.isBlank() || newAadhaar.isBlank()) {
                  Toast.makeText(context, "Please fill Name, Mobile and Aadhaar", Toast.LENGTH_SHORT).show()
                } else {
                  val fee = admissionFeeAmount.toDoubleOrNull() ?: 400.0
                  val customFee = customFeeOverrideText.toDoubleOrNull()
                  repository.directEnrollStudent(
                    name = newName,
                    mobile = newMobile,
                    aadhaarNo = newAadhaar,
                    email = if (newEmail.isNotBlank()) newEmail else null,
                    guardianName = newGuardianName,
                    guardianPhone = newGuardianPhone,
                    address = newAddress,
                    courseId = newCourseId,
                    batchId = newBatchId,
                    academicClass = newClass,
                    customFeeOverride = customFee,
                    collectFeeNow = recordFeeAtAdmission,
                    feeAmount = fee,
                    paymentMode = paymentModeChoice,
                    transactionRef = txnRefText
                  )
                  showAddDialog = false
                  Toast.makeText(context, "Student $newName enrolled & added to directory!", Toast.LENGTH_SHORT).show()
                  // reset fields
                  newName = ""
                  newMobile = ""
                  newAadhaar = ""
                  newGuardianName = ""
                  newGuardianPhone = ""
                }
              },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
            ) {
              Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Save & Enroll Student", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          }
        }
      }
    }
  }
}
