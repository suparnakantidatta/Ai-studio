package com.example.ui.admin

import android.content.Intent
import android.net.Uri
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
import com.example.data.model.*
import com.example.data.repository.PathsalaRepository
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@Composable
fun AdminLiveClassTab(
  repository: PathsalaRepository,
  initialSubTab: Int = 0
) {
  val context = LocalContext.current
  val liveClasses by repository.liveClasses.collectAsState()
  val recordings by repository.recordings.collectAsState()
  val exams by repository.exams.collectAsState()
  val studyMaterials by repository.studyMaterials.collectAsState()
  val courses by repository.courses.collectAsState()
  val batches by repository.batches.collectAsState()

  var selectedSubTab by remember { mutableIntStateOf(initialSubTab) }

  // Dialog triggers
  var showScheduleDialog by remember { mutableStateOf(false) }
  var showCreateExamDialog by remember { mutableStateOf(false) }
  var showAddMaterialDialog by remember { mutableStateOf(false) }

  // Delete confirmation targets
  var sessionToDelete by remember { mutableStateOf<LiveClassSession?>(null) }
  var recordingToDelete by remember { mutableStateOf<ClassRecording?>(null) }
  var examToDelete by remember { mutableStateOf<OnlineExam?>(null) }
  var materialToDelete by remember { mutableStateOf<StudyMaterial?>(null) }

  // Live Class form states
  var sessionTitle by remember { mutableStateOf("") }
  var sessionSubject by remember { mutableStateOf("Computer Science & Coding") }
  var sessionClass by remember { mutableStateOf("Class 10 (Secondary)") }
  var sessionDate by remember { mutableStateOf("2026-09-29") }
  var sessionStartTime by remember { mutableStateOf("05:00 PM") }
  var sessionEndTime by remember { mutableStateOf("06:30 PM") }
  var sessionEducator by remember { mutableStateOf("Er. Suparna Kanti Datta") }
  var sessionMeetingUrl by remember { mutableStateOf("https://meet.google.com/new") }

  // Exam form states
  var examCode by remember { mutableStateOf("EXAM-${(100..999).random()}") }
  var examTitle by remember { mutableStateOf("") }
  var examSubject by remember { mutableStateOf("Computer Science") }
  var examClass by remember { mutableStateOf("Class 10 (Secondary)") }
  var examDuration by remember { mutableStateOf("30") }
  var examTotalMarks by remember { mutableStateOf("20") }
  var examPassingMarks by remember { mutableStateOf("8") }
  var examDate by remember { mutableStateOf("2026-09-29") }
  var examQuestionText by remember { mutableStateOf("") }
  var examOptA by remember { mutableStateOf("") }
  var examOptB by remember { mutableStateOf("") }
  var examOptC by remember { mutableStateOf("") }
  var examOptD by remember { mutableStateOf("") }

  // Material form states
  var matTitle by remember { mutableStateOf("") }
  var matSubject by remember { mutableStateOf("Computer Science") }
  var matType by remember { mutableStateOf("pdf") }
  var matUrl by remember { mutableStateOf("") }
  var matDesc by remember { mutableStateOf("") }

  Column(modifier = Modifier.fillMaxSize()) {
    // Top Sub-Tabs Navigation
    TabRow(
      selectedTabIndex = selectedSubTab,
      containerColor = MaterialTheme.colorScheme.surface,
      contentColor = IndigoPrimary
    ) {
      Tab(
        selected = selectedSubTab == 0,
        onClick = { selectedSubTab = 0 },
        text = { Text("Live Classes (${liveClasses.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
        icon = { Icon(Icons.Default.Radio, contentDescription = null, modifier = Modifier.size(16.dp)) }
      )
      Tab(
        selected = selectedSubTab == 1,
        onClick = { selectedSubTab = 1 },
        text = { Text("Exams (${exams.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
        icon = { Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(16.dp)) }
      )
      Tab(
        selected = selectedSubTab == 2,
        onClick = { selectedSubTab = 2 },
        text = { Text("Class Notes (${studyMaterials.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
        icon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) }
      )
    }

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .weight(1f),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      when (selectedSubTab) {
        // ==================== SUB-TAB 0: LIVE CLASSES & RECORDINGS ====================
        0 -> {
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(text = "Live Classroom & Recordings", fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(text = "Broadcast sessions & lecture video archives", fontSize = 12.sp, color = SlateTextSecondary)
              }

              Button(
                onClick = { showScheduleDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoseLive)
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Schedule Live", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }

          item {
            SectionHeader(title = "Live Broadcast Sessions (${liveClasses.size})")
          }

          if (liveClasses.isEmpty()) {
            item {
              Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
              ) {
                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                  Text("No live classes scheduled. Click 'Schedule Live' to create one.", fontSize = 13.sp, color = SlateTextSecondary)
                }
              }
            }
          } else {
            items(liveClasses) { session ->
              val isLive = session.status == "live"
              Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                  containerColor = if (isLive) Color(0xFFFFF1F2) else MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                  if (isLive) 2.dp else 1.dp,
                  if (isLive) Color(0xFFFB7185) else SlateBorder
                ),
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
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      StatusBadge(status = session.status)
                      Surface(color = Color(0xFFF1F5F9), shape = RoundedCornerShape(6.dp)) {
                        Text(
                          text = session.academicClass ?: "All Classes",
                          color = SlateTextSecondary,
                          fontSize = 10.sp,
                          fontWeight = FontWeight.Bold,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }
                    }

                    IconButton(
                      onClick = { sessionToDelete = session },
                      modifier = Modifier.size(32.dp)
                    ) {
                      Icon(Icons.Default.Delete, contentDescription = "Delete Live Class", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    }
                  }

                  Text(
                    text = session.title,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                  )

                  Text(
                    text = "${session.subject} • Faculty: ${session.educatorName}",
                    fontSize = 12.sp,
                    color = IndigoPrimary,
                    fontWeight = FontWeight.SemiBold
                  )

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "${session.scheduledDate} • ${session.startTime} - ${session.endTime}",
                      fontSize = 11.sp,
                      color = SlateTextSecondary,
                      fontFamily = FontFamily.Monospace
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                      if (session.status == "scheduled") {
                        Button(
                          onClick = {
                            repository.toggleLiveClassStatus(session.id, "live")
                            Toast.makeText(context, "Class is now LIVE!", Toast.LENGTH_SHORT).show()
                          },
                          shape = RoundedCornerShape(10.dp),
                          colors = ButtonDefaults.buttonColors(containerColor = RoseLive),
                          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                          Icon(Icons.Default.Radio, contentDescription = null, modifier = Modifier.size(14.dp))
                          Spacer(modifier = Modifier.width(4.dp))
                          Text("Go Live", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                      } else if (session.status == "live") {
                        Button(
                          onClick = {
                            repository.toggleLiveClassStatus(session.id, "completed")
                            Toast.makeText(context, "Class ended and marked completed.", Toast.LENGTH_SHORT).show()
                          },
                          shape = RoundedCornerShape(10.dp),
                          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                          Text("End Class", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                      }

                      if (!session.meetingUrl.isNullOrBlank()) {
                        OutlinedButton(
                          onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(session.meetingUrl))
                            context.startActivity(intent)
                          },
                          shape = RoundedCornerShape(10.dp),
                          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                          Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          // Recordings Section
          item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
              title = "Lecture Video Archives (${recordings.size})",
              subtitle = "On-demand recordings available for students"
            )
          }

          if (recordings.isEmpty()) {
            item {
              Text("No video recordings stored in database.", fontSize = 12.sp, color = SlateTextSecondary)
            }
          } else {
            items(recordings) { rec ->
              Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = rec.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "${rec.subject} • ${rec.durationMinutes} mins • ${rec.educatorName}", fontSize = 11.sp, color = SlateTextSecondary)
                  }

                  Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                      onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rec.videoUrl))
                        context.startActivity(intent)
                      }
                    ) {
                      Icon(Icons.Default.PlayCircle, contentDescription = "Play", tint = IndigoPrimary)
                    }

                    IconButton(
                      onClick = { recordingToDelete = rec },
                      modifier = Modifier.size(32.dp)
                    ) {
                      Icon(Icons.Default.Delete, contentDescription = "Delete Recording", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    }
                  }
                }
              }
            }
          }
        }

        // ==================== SUB-TAB 1: ONLINE EXAMS ====================
        1 -> {
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(text = "Online Examinations (${exams.size})", fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(text = "Manage online test papers, scoring & schedules", fontSize = 12.sp, color = SlateTextSecondary)
              }

              Button(
                onClick = { showCreateExamDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Create Exam", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }

          if (exams.isEmpty()) {
            item {
              Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
              ) {
                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                  Text("No exams found in database. Tap 'Create Exam' to schedule one.", fontSize = 13.sp, color = SlateTextSecondary)
                }
              }
            }
          } else {
            items(exams) { exam ->
              Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(
                  modifier = Modifier.padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      Surface(color = Color(0xFFEEF2FF), shape = RoundedCornerShape(6.dp)) {
                        Text(
                          text = exam.examCode,
                          color = IndigoPrimary,
                          fontSize = 11.sp,
                          fontWeight = FontWeight.Bold,
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                      }
                      StatusBadge(status = exam.status)
                    }

                    IconButton(
                      onClick = { examToDelete = exam },
                      modifier = Modifier.size(32.dp)
                    ) {
                      Icon(Icons.Default.Delete, contentDescription = "Delete Exam", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    }
                  }

                  Text(
                    text = exam.title,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                  )

                  Text(
                    text = "${exam.subject} • ${exam.academicClass ?: "All Classes"}",
                    fontSize = 12.sp,
                    color = SlateTextSecondary
                  )

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "Duration: ${exam.durationMinutes} mins | Total: ${exam.totalMarks} M | Pass: ${exam.passingMarks} M",
                      fontSize = 11.sp,
                      color = IndigoPrimary,
                      fontWeight = FontWeight.SemiBold
                    )
                    Text(
                      text = exam.scheduledDate,
                      fontSize = 11.sp,
                      color = SlateTextSecondary,
                      fontFamily = FontFamily.Monospace
                    )
                  }
                }
              }
            }
          }
        }

        // ==================== SUB-TAB 2: CLASS NOTES & STUDY MATERIALS ====================
        2 -> {
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(text = "Class Notes & Materials (${studyMaterials.size})", fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(text = "Syllabus guides, formula sheets & assignments", fontSize = 12.sp, color = SlateTextSecondary)
              }

              Button(
                onClick = { showAddMaterialDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Notes", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
              }
            }
          }

          if (studyMaterials.isEmpty()) {
            item {
              Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
              ) {
                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                  Text("No class notes found in database. Tap 'Add Notes' to upload.", fontSize = 13.sp, color = SlateTextSecondary)
                }
              }
            }
          } else {
            items(studyMaterials) { mat ->
              Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      Surface(color = Color(0xFFE0F2FE), shape = RoundedCornerShape(6.dp)) {
                        Text(
                          text = mat.fileType.uppercase(),
                          color = Color(0xFF0369A1),
                          fontSize = 9.sp,
                          fontWeight = FontWeight.Black,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }
                      Text(
                        text = mat.subject ?: "General",
                        fontSize = 11.sp,
                        color = SlateTextSecondary,
                        fontWeight = FontWeight.SemiBold
                      )
                    }

                    Text(
                      text = mat.title,
                      fontWeight = FontWeight.Bold,
                      fontSize = 15.sp,
                      color = MaterialTheme.colorScheme.onSurface
                    )

                    if (!mat.description.isNullOrBlank()) {
                      Text(
                        text = mat.description,
                        fontSize = 12.sp,
                        color = SlateTextSecondary,
                        maxLines = 2
                      )
                    }

                    Text(
                      text = "Uploaded: ${mat.uploadedDate} by ${mat.uploadedBy}",
                      fontSize = 10.sp,
                      color = SlateTextSecondary
                    )
                  }

                  Row(verticalAlignment = Alignment.CenterVertically) {
                    if (mat.fileUrl.isNotBlank()) {
                      IconButton(
                        onClick = {
                          try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mat.fileUrl))
                            context.startActivity(intent)
                          } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open URL", Toast.LENGTH_SHORT).show()
                          }
                        }
                      ) {
                        Icon(Icons.Default.Download, contentDescription = "Download", tint = IndigoPrimary)
                      }
                    }

                    IconButton(
                      onClick = { materialToDelete = mat },
                      modifier = Modifier.size(32.dp)
                    ) {
                      Icon(Icons.Default.Delete, contentDescription = "Delete Note", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // ==================== DELETE CONFIRMATION DIALOGS ====================

  sessionToDelete?.let { session ->
    AlertDialog(
      onDismissRequest = { sessionToDelete = null },
      title = { Text("Delete Live Class?", fontWeight = FontWeight.Bold) },
      text = { Text("Are you sure you want to delete '${session.title}'? This will remove it from the system and Google Sheets database.") },
      confirmButton = {
        Button(
          onClick = {
            repository.deleteLiveClass(session.id)
            sessionToDelete = null
            Toast.makeText(context, "Live class deleted and synced to Google Sheets", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
        ) {
          Text("Delete", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { sessionToDelete = null }) {
          Text("Cancel")
        }
      }
    )
  }

  recordingToDelete?.let { rec ->
    AlertDialog(
      onDismissRequest = { recordingToDelete = null },
      title = { Text("Delete Video Recording?", fontWeight = FontWeight.Bold) },
      text = { Text("Are you sure you want to delete recording '${rec.title}' from the database?") },
      confirmButton = {
        Button(
          onClick = {
            repository.deleteClassRecording(rec.id)
            recordingToDelete = null
            Toast.makeText(context, "Recording deleted and synced to Google Sheets", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
        ) {
          Text("Delete", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { recordingToDelete = null }) {
          Text("Cancel")
        }
      }
    )
  }

  examToDelete?.let { exam ->
    AlertDialog(
      onDismissRequest = { examToDelete = null },
      title = { Text("Delete Online Exam?", fontWeight = FontWeight.Bold) },
      text = { Text("Are you sure you want to delete '${exam.title}' (${exam.examCode})? This will remove it from student portals and Google Sheets.") },
      confirmButton = {
        Button(
          onClick = {
            repository.deleteOnlineExam(exam.id)
            examToDelete = null
            Toast.makeText(context, "Exam deleted and synced to Google Sheets", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
        ) {
          Text("Delete", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { examToDelete = null }) {
          Text("Cancel")
        }
      }
    )
  }

  materialToDelete?.let { mat ->
    AlertDialog(
      onDismissRequest = { materialToDelete = null },
      title = { Text("Delete Study Material?", fontWeight = FontWeight.Bold) },
      text = { Text("Are you sure you want to delete '${mat.title}'? This will remove it from the Google Sheets database.") },
      confirmButton = {
        Button(
          onClick = {
            repository.deleteStudyMaterial(mat.id)
            materialToDelete = null
            Toast.makeText(context, "Study note deleted and synced to Google Sheets", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
        ) {
          Text("Delete", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { materialToDelete = null }) {
          Text("Cancel")
        }
      }
    )
  }

  // ==================== SCHEDULE LIVE CLASS DIALOG ====================
  if (showScheduleDialog) {
    Dialog(onDismissRequest = { showScheduleDialog = false }) {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.padding(16.dp)
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text("Schedule Live Class", fontWeight = FontWeight.Black, fontSize = 17.sp)

          OutlinedTextField(
            value = sessionTitle,
            onValueChange = { sessionTitle = it },
            label = { Text("Session Title *") },
            placeholder = { Text("e.g. Python OOP & Algorithms") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = sessionSubject,
            onValueChange = { sessionSubject = it },
            label = { Text("Subject / Domain *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = sessionEducator,
            onValueChange = { sessionEducator = it },
            label = { Text("Educator Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = sessionMeetingUrl,
            onValueChange = { sessionMeetingUrl = it },
            label = { Text("Google Meet or Stream URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
          ) {
            TextButton(onClick = { showScheduleDialog = false }) {
              Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = {
                if (sessionTitle.isNotBlank()) {
                  val newSession = LiveClassSession(
                    id = "live-${System.currentTimeMillis()}",
                    title = sessionTitle,
                    academicClass = sessionClass,
                    targetClass = sessionClass,
                    subject = sessionSubject,
                    educatorName = sessionEducator,
                    scheduledDate = sessionDate,
                    startTime = sessionStartTime,
                    endTime = sessionEndTime,
                    status = "scheduled",
                    platform = "google_meet",
                    meetingUrl = sessionMeetingUrl
                  )
                  repository.scheduleLiveClass(newSession)
                  showScheduleDialog = false
                  Toast.makeText(context, "Live class scheduled & saved to Google Sheets!", Toast.LENGTH_SHORT).show()
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = RoseLive)
            ) {
              Text("Schedule")
            }
          }
        }
      }
    }
  }

  // ==================== CREATE EXAM DIALOG ====================
  if (showCreateExamDialog) {
    Dialog(onDismissRequest = { showCreateExamDialog = false }) {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.padding(16.dp)
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text("Create Online Exam", fontWeight = FontWeight.Black, fontSize = 17.sp)

          OutlinedTextField(
            value = examTitle,
            onValueChange = { examTitle = it },
            label = { Text("Exam Title *") },
            placeholder = { Text("e.g. Computer Science Term 1") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = examSubject,
              onValueChange = { examSubject = it },
              label = { Text("Subject") },
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = examDuration,
              onValueChange = { examDuration = it },
              label = { Text("Duration (mins)") },
              modifier = Modifier.weight(1f)
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = examTotalMarks,
              onValueChange = { examTotalMarks = it },
              label = { Text("Total Marks") },
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = examPassingMarks,
              onValueChange = { examPassingMarks = it },
              label = { Text("Passing Marks") },
              modifier = Modifier.weight(1f)
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
          ) {
            TextButton(onClick = { showCreateExamDialog = false }) {
              Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = {
                if (examTitle.isNotBlank()) {
                  val newExamId = "exam-${System.currentTimeMillis()}"
                  val newExam = OnlineExam(
                    id = newExamId,
                    examCode = examCode,
                    title = examTitle,
                    courseId = "all",
                    batchId = "all",
                    academicClass = examClass,
                    subject = examSubject,
                    durationMinutes = examDuration.toIntOrNull() ?: 30,
                    totalMarks = examTotalMarks.toIntOrNull() ?: 20,
                    passingMarks = examPassingMarks.toIntOrNull() ?: 8,
                    passingPercentage = 40,
                    status = "active",
                    mode = "online",
                    scheduledDate = examDate,
                    startTime = "09:00 AM",
                    endTime = "09:00 PM",
                    instructions = "Read each question carefully. You may navigate between questions. Click Submit Exam when finished.",
                    isPublished = true
                  )
                  repository.createOnlineExam(newExam)
                  showCreateExamDialog = false
                  Toast.makeText(context, "Exam created & synced to Google Sheets!", Toast.LENGTH_SHORT).show()
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
            ) {
              Text("Create Exam")
            }
          }
        }
      }
    }
  }

  // ==================== ADD STUDY MATERIAL DIALOG ====================
  if (showAddMaterialDialog) {
    Dialog(onDismissRequest = { showAddMaterialDialog = false }) {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.padding(16.dp)
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text("Add Class Notes / Material", fontWeight = FontWeight.Black, fontSize = 17.sp)

          OutlinedTextField(
            value = matTitle,
            onValueChange = { matTitle = it },
            label = { Text("Note Title *") },
            placeholder = { Text("e.g. Chapter 1 Formula Sheet") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = matSubject,
            onValueChange = { matSubject = it },
            label = { Text("Subject / Module") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = matUrl,
            onValueChange = { matUrl = it },
            label = { Text("Resource URL / Link *") },
            placeholder = { Text("https://drive.google.com/...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = matDesc,
            onValueChange = { matDesc = it },
            label = { Text("Brief Description") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
          ) {
            TextButton(onClick = { showAddMaterialDialog = false }) {
              Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = {
                if (matTitle.isNotBlank()) {
                  val newMat = StudyMaterial(
                    id = "mat-${System.currentTimeMillis()}",
                    title = matTitle,
                    subject = matSubject,
                    courseId = "all",
                    batchId = "all",
                    fileType = if (matUrl.contains("drive.google") || matUrl.endsWith(".pdf")) "pdf" else "link",
                    fileName = matTitle,
                    fileSize = "Cloud Resource",
                    fileUrl = matUrl.ifBlank { "https://pixelpathsala.com" },
                    uploadedBy = "Admin - Faculty Desk",
                    uploadedDate = "2026-09-29",
                    downloadCount = 0,
                    description = matDesc
                  )
                  repository.addStudyMaterial(newMat)
                  showAddMaterialDialog = false
                  Toast.makeText(context, "Study note added & synced to Google Sheets!", Toast.LENGTH_SHORT).show()
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
            ) {
              Text("Add Note", color = Color.White)
            }
          }
        }
      }
    }
  }
}
