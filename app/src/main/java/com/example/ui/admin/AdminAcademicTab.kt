package com.example.ui.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.data.model.Batch
import com.example.data.model.Course
import com.example.data.model.LiveClassSession
import com.example.data.model.StudyMaterial
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminAcademicTab(
  liveClasses: List<LiveClassSession>,
  studyMaterials: List<StudyMaterial>,
  courses: List<Course>,
  batches: List<Batch>,
  exams: List<com.example.data.model.OnlineExam> = emptyList(),
  questions: List<com.example.data.model.ExamQuestion> = emptyList(),
  submissions: List<com.example.data.model.ExamSubmission> = emptyList(),
  onAddClass: (LiveClassSession) -> Unit,
  onUpdateClass: (LiveClassSession) -> Unit,
  onDeleteClass: (String) -> Unit,
  onAddNote: (StudyMaterial) -> Unit,
  onUpdateNote: (StudyMaterial) -> Unit,
  onDeleteNote: (String) -> Unit,
  onTogglePostpone: (String, Boolean, String?) -> Unit = { _, _, _ -> },
  onRescheduleClass: (String, String, String, String, String?) -> Unit = { _, _, _, _, _ -> },
  onSendNotification: (LiveClassSession) -> Unit = {},
  onAddExam: (com.example.data.model.OnlineExam) -> Unit = {},
  onUpdateExam: (com.example.data.model.OnlineExam) -> Unit = {},
  onDeleteExam: (String) -> Unit = {},
  onToggleExamPublish: (String) -> Unit = {},
  onAddQuestion: (com.example.data.model.ExamQuestion) -> Unit = {},
  onDeleteQuestion: (String) -> Unit = {},
  onDeleteSubmission: (String) -> Unit = {}
) {
  var activeSubTab by remember { mutableIntStateOf(0) } // 0: Classes, 1: Notes, 2: Online Exams

  var showAddClassDialog by remember { mutableStateOf(false) }
  var editingClass by remember { mutableStateOf<LiveClassSession?>(null) }
  var deletingClassId by remember { mutableStateOf<String?>(null) }

  var showAddNoteDialog by remember { mutableStateOf(false) }
  var editingNote by remember { mutableStateOf<StudyMaterial?>(null) }
  var deletingNoteId by remember { mutableStateOf<String?>(null) }

  var selectedBatchFilter by remember { mutableStateOf<String?>(null) }
  var postponingSession by remember { mutableStateOf<LiveClassSession?>(null) }
  var notifConfirmationSession by remember { mutableStateOf<LiveClassSession?>(null) }

  var reschedulingSession by remember { mutableStateOf<LiveClassSession?>(null) }
  var rescheduleDateText by remember { mutableStateOf("") }
  var rescheduleStartTimeText by remember { mutableStateOf("") }
  var rescheduleEndTimeText by remember { mutableStateOf("") }
  var rescheduleReasonText by remember { mutableStateOf("") }

  val displayedClasses = remember(liveClasses, selectedBatchFilter) {
    if (selectedBatchFilter == null) {
      liveClasses
    } else {
      liveClasses.filter { it.batchId == selectedBatchFilter }
    }
  }

  val context = LocalContext.current

  Column(modifier = Modifier.fillMaxSize()) {
    Surface(
      color = Color.White,
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FilterChip(
          selected = activeSubTab == 0,
          onClick = { activeSubTab = 0 },
          leadingIcon = {
            Icon(Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(16.dp))
          },
          label = { Text("Scheduled Classes (${liveClasses.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = BrandPrimary,
            selectedLabelColor = Color.White
          ),
          modifier = Modifier.weight(1f)
        )

        FilterChip(
          selected = activeSubTab == 1,
          onClick = { activeSubTab = 1 },
          leadingIcon = {
            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
          },
          label = { Text("Notes (${studyMaterials.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = BrandPrimary,
            selectedLabelColor = Color.White
          ),
          modifier = Modifier.weight(1f)
        )

        FilterChip(
          selected = activeSubTab == 2,
          onClick = { activeSubTab = 2 },
          leadingIcon = {
            Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
          },
          label = { Text("Exams (${exams.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = BrandPrimary,
            selectedLabelColor = Color.White
          ),
          modifier = Modifier.weight(1f)
        )
      }
    }

    if (activeSubTab == 0) {
      // SCHEDULED CLASSES VIEW
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(text = "Scheduled Classes (Database)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate900)
              Text(text = "Real-time records synced directly with Google Sheet database", fontSize = 12.sp, color = Slate600)
            }
            Button(
              onClick = { showAddClassDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add Class", fontSize = 12.sp)
            }
          }
        }

        // Batch Filter Chips
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            FilterChip(
              selected = selectedBatchFilter == null,
              onClick = { selectedBatchFilter = null },
              label = { Text("All Batches (${displayedClasses.size})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
            )
            batches.forEach { b ->
              val count = liveClasses.count { it.batchId == b.id }
              FilterChip(
                selected = selectedBatchFilter == b.id,
                onClick = { selectedBatchFilter = if (selectedBatchFilter == b.id) null else b.id },
                label = { Text("${b.name} ($count)", fontSize = 11.sp) }
              )
            }
          }
        }

        if (displayedClasses.isEmpty()) {
          item {
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = Color.White),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier
                  .padding(24.dp)
                  .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Icon(Icons.Default.EventBusy, contentDescription = null, tint = Slate400, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No scheduled classes in database for this batch.", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Slate700)
                Text("Click '+ Add Class' above to schedule and sync with Google Sheets.", fontSize = 12.sp, color = Slate500)
              }
            }
          }
        } else {
          items(displayedClasses) { session ->
            val course = courses.firstOrNull { it.id == session.courseId }
            val batch = batches.firstOrNull { it.id == session.batchId }

            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(
                containerColor = if (session.isPostponed) Color(0xFFFFFBEB) else Color.White
              ),
              elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(text = session.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate900)
                  Surface(
                    color = if (session.isPostponed) ErrorRedLight else if (session.status == "live") Color(0xFFFEE2E2) else BrandGoldLight,
                    shape = RoundedCornerShape(6.dp)
                  ) {
                    Text(
                      text = if (session.isPostponed) "POSTPONED" else session.status.uppercase(),
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (session.isPostponed) ErrorRed else if (session.status == "live") ErrorRed else BrandGoldDark,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                  }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Date: ${session.scheduledDate} • Timings: ${session.startTime} - ${session.endTime}",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = if (session.isPostponed) ErrorRed else BrandPrimary
                )
                Text(
                  text = "Batch: ${batch?.name ?: "Main Batch"} (${batch?.scheduleDays ?: "Scheduled"}) • Mode: ${session.platform}",
                  fontSize = 11.sp,
                  color = Slate600
                )
                Text(
                  text = "Subject: ${session.subject} • Faculty: ${session.educatorName}",
                  fontSize = 11.sp,
                  color = Slate600
                )

                if (session.isPostponed) {
                  Spacer(modifier = Modifier.height(8.dp))
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEE2E2),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      modifier = Modifier.padding(10.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = "Status: Class postponed by faculty",
                        fontSize = 12.sp,
                        color = ErrorRed,
                        fontWeight = FontWeight.Medium
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Postpone Switch and Send Notification Controls
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = Color(0xFFF8FAFC),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                        text = if (session.isPostponed) "Class Postponed" else "Schedule Active",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (session.isPostponed) ErrorRed else Slate700
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Switch(
                        checked = session.isPostponed,
                        onCheckedChange = { isChecked ->
                          if (isChecked) {
                            postponingSession = session
                          } else {
                            onTogglePostpone(session.id, false, null)
                            Toast.makeText(context, "Class schedule restored to Active", Toast.LENGTH_SHORT).show()
                          }
                        }
                      )
                    }

                    Button(
                      onClick = {
                        onSendNotification(session)
                        notifConfirmationSession = session
                        Toast.makeText(context, "Notification dispatched for ${batch?.name ?: session.title}!", Toast.LENGTH_SHORT).show()
                      },
                      colors = ButtonDefaults.buttonColors(
                        containerColor = if (session.notificationSent) SuccessGreen else BrandPrimary
                      ),
                      shape = RoundedCornerShape(8.dp),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                      Icon(
                        imageVector = if (session.notificationSent) Icons.Default.CheckCircle else Icons.Default.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(if (session.notificationSent) "Notified" else "Notify", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions row
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  if (!session.meetingUrl.isNullOrBlank()) {
                    FilledTonalButton(
                      onClick = {
                        try {
                          val intent = Intent(Intent.ACTION_VIEW, Uri.parse(session.meetingUrl))
                          context.startActivity(intent)
                        } catch (_: Exception) {}
                      },
                      shape = RoundedCornerShape(8.dp),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                      Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(14.dp))
                      Spacer(modifier = Modifier.width(3.dp))
                      Text("Portal", fontSize = 11.sp)
                    }
                  }

                  FilledTonalButton(
                    onClick = {
                      reschedulingSession = session
                      rescheduleDateText = session.scheduledDate
                      rescheduleStartTimeText = session.startTime
                      rescheduleEndTimeText = session.endTime
                      rescheduleReasonText = ""
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = BrandPrimary.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                  ) {
                    Icon(Icons.Default.Schedule, contentDescription = "Reschedule", tint = BrandPrimary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Reschedule", fontSize = 11.sp, color = BrandPrimary, fontWeight = FontWeight.SemiBold)
                  }

                  OutlinedButton(
                    onClick = { editingClass = session },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                  ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Edit", fontSize = 11.sp)
                  }

                  IconButton(
                    onClick = { onDeleteClass(session.id) },
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
    } else if (activeSubTab == 1) {
      // NOTES AS WEB PORTAL VIEW
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(text = "Notes & Study Web Portal", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate900)
              Text(text = "Add notes, PDFs, web portals & learning links", fontSize = 12.sp, color = Slate600)
            }
            Button(
              onClick = { showAddNoteDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add Note / Portal", fontSize = 12.sp)
            }
          }
        }

        if (studyMaterials.isEmpty()) {
          item {
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = Color.White),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier
                  .padding(24.dp)
                  .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Slate400, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No notes or web portal resources added yet.", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Slate700)
                Text("Click '+ Add Note / Portal' above to publish web portal notes for admission batches.", fontSize = 12.sp, color = Slate500)
              }
            }
          }
        } else {
          items(studyMaterials.reversed()) { material ->
            val course = courses.firstOrNull { it.id == material.courseId }
            val batch = batches.firstOrNull { it.id == material.batchId }

            Card(
              shape = RoundedCornerShape(14.dp),
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
                  Text(text = material.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate900)
                  Surface(
                    color = BrandSecondary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                  ) {
                    Text(
                      text = material.fileType.uppercase(),
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = BrandSecondary,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Subject: ${material.subject}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Slate700)
                Text(
                  text = "Batch: ${batch?.name ?: "All Batches"} • Course: ${course?.title ?: "All Courses"}",
                  fontSize = 12.sp,
                  color = BrandPrimary
                )
                if (!material.description.isNullOrBlank()) {
                  Text(text = material.description, fontSize = 12.sp, color = Slate600)
                }
                Text(text = "Date: ${material.uploadedDate.take(10)}", fontSize = 11.sp, color = Slate400)

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  if (material.fileUrl.isNotBlank()) {
                    FilledTonalButton(
                      onClick = {
                        try {
                          val intent = Intent(Intent.ACTION_VIEW, Uri.parse(material.fileUrl))
                          context.startActivity(intent)
                        } catch (_: Exception) {}
                      },
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.weight(1f)
                    ) {
                      Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Open Web Portal", fontSize = 12.sp)
                    }
                  }

                  OutlinedButton(
                    onClick = { editingNote = material },
                    shape = RoundedCornerShape(8.dp)
                  ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", fontSize = 12.sp)
                  }

                  IconButton(
                    onClick = { deletingNoteId = material.id }
                  ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                  }
                }
              }
            }
          }
        }
      }
    } else {
      AdminOnlineExamManager(
        exams = exams,
        questions = questions,
        submissions = submissions,
        courses = courses,
        batches = batches,
        onAddExam = onAddExam,
        onUpdateExam = onUpdateExam,
        onDeleteExam = onDeleteExam,
        onToggleExamPublish = onToggleExamPublish,
        onAddQuestion = onAddQuestion,
        onDeleteQuestion = onDeleteQuestion,
        onDeleteSubmission = onDeleteSubmission
      )
    }
  }

  // DIALOGS
  if (showAddClassDialog) {
    ScheduleClassDialog(
      courses = courses,
      batches = batches,
      existing = null,
      onDismiss = { showAddClassDialog = false },
      onSave = {
        onAddClass(it)
        showAddClassDialog = false
      }
    )
  }

  if (editingClass != null) {
    ScheduleClassDialog(
      courses = courses,
      batches = batches,
      existing = editingClass,
      onDismiss = { editingClass = null },
      onSave = {
        onUpdateClass(it)
        editingClass = null
      }
    )
  }

  if (deletingClassId != null) {
    AlertDialog(
      onDismissRequest = { deletingClassId = null },
      title = { Text("Delete Scheduled Class?") },
      text = { Text("Are you sure you want to delete this scheduled class session? This cannot be undone.") },
      confirmButton = {
        Button(
          onClick = {
            deletingClassId?.let { onDeleteClass(it) }
            deletingClassId = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
        ) {
          Text("Delete", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingClassId = null }) { Text("Cancel") }
      }
    )
  }

  if (postponingSession != null) {
    val session = postponingSession!!
    AlertDialog(
      onDismissRequest = { postponingSession = null },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Postpone Class Session?", fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Text(
          text = "Are you sure you want to mark this class as Postponed for students? The schedule status will be updated to Postponed.",
          fontSize = 14.sp,
          color = Slate600
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onTogglePostpone(session.id, true, null)
            Toast.makeText(context, "Class marked as Postponed", Toast.LENGTH_SHORT).show()
            postponingSession = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
        ) {
          Text("Confirm Postpone", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { postponingSession = null }) { Text("Cancel") }
      }
    )
  }

  if (reschedulingSession != null) {
    val session = reschedulingSession!!
    val batch = batches.firstOrNull { it.id == session.batchId }

    AlertDialog(
      onDismissRequest = { reschedulingSession = null },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Schedule, contentDescription = null, tint = BrandPrimary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Reschedule Class Session", fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Update scheduled date & time for ${session.title} (${batch?.name ?: session.subject}). Updates will sync to database and alert students once.",
            fontSize = 12.sp,
            color = Slate600
          )

          OutlinedTextField(
            value = rescheduleDateText,
            onValueChange = { rescheduleDateText = it },
            label = { Text("New Scheduled Date (YYYY-MM-DD) *") },
            placeholder = { Text("2026-10-27") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = rescheduleStartTimeText,
              onValueChange = { rescheduleStartTimeText = it },
              label = { Text("Start Time *") },
              placeholder = { Text("10:00 AM") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
              value = rescheduleEndTimeText,
              onValueChange = { rescheduleEndTimeText = it },
              label = { Text("End Time *") },
              placeholder = { Text("11:30 AM") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
          }

          OutlinedTextField(
            value = rescheduleReasonText,
            onValueChange = { rescheduleReasonText = it },
            label = { Text("Reschedule Note / Remarks (Optional)") },
            placeholder = { Text("e.g. Center schedule revision") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val dateClean = rescheduleDateText.trim().ifBlank { session.scheduledDate }
            val startClean = rescheduleStartTimeText.trim().ifBlank { session.startTime }
            val endClean = rescheduleEndTimeText.trim().ifBlank { session.endTime }
            onRescheduleClass(session.id, dateClean, startClean, endClean, rescheduleReasonText.trim().ifBlank { null })
            Toast.makeText(context, "Class rescheduled to $dateClean at $startClean", Toast.LENGTH_SHORT).show()
            reschedulingSession = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
        ) {
          Text("Confirm Reschedule", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { reschedulingSession = null }) { Text("Cancel") }
      }
    )
  }

  if (notifConfirmationSession != null) {
    val session = notifConfirmationSession!!
    val batch = batches.firstOrNull { it.id == session.batchId }
    val msg = "📢 Pixel Pathsala Alert: Class for ${batch?.name ?: session.title} on ${session.scheduledDate} (${session.startTime}) is ${if (session.isPostponed) "POSTPONED" else "SCHEDULED (Active)"}. Please check your student portal."

    AlertDialog(
      onDismissRequest = { notifConfirmationSession = null },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = SuccessGreen)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Class Notification Sent", fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "In-app broadcast alert has been sent to all enrolled students for this batch.",
            fontSize = 13.sp,
            color = Slate700
          )
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Slate100,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = msg,
              modifier = Modifier.padding(10.dp),
              fontSize = 11.sp,
              color = Slate800
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val uri = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(msg)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            try {
              context.startActivity(intent)
            } catch (_: Exception) {}
            notifConfirmationSession = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
        ) {
          Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Broadcast on WhatsApp")
        }
      },
      dismissButton = {
        TextButton(onClick = { notifConfirmationSession = null }) { Text("Close") }
      }
    )
  }

  if (showAddNoteDialog) {
    AddNoteWebPortalDialog(
      courses = courses,
      batches = batches,
      existing = null,
      onDismiss = { showAddNoteDialog = false },
      onSave = {
        onAddNote(it)
        showAddNoteDialog = false
      }
    )
  }

  if (editingNote != null) {
    AddNoteWebPortalDialog(
      courses = courses,
      batches = batches,
      existing = editingNote,
      onDismiss = { editingNote = null },
      onSave = {
        onUpdateNote(it)
        editingNote = null
      }
    )
  }

  if (deletingNoteId != null) {
    AlertDialog(
      onDismissRequest = { deletingNoteId = null },
      title = { Text("Delete Note / Web Portal?") },
      text = { Text("Are you sure you want to remove this note/web portal link? Students won't be able to access it.") },
      confirmButton = {
        Button(
          onClick = {
            deletingNoteId?.let { onDeleteNote(it) }
            deletingNoteId = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
        ) {
          Text("Delete", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingNoteId = null }) { Text("Cancel") }
      }
    )
  }
}

@Composable
fun ScheduleClassDialog(
  courses: List<Course>,
  batches: List<Batch>,
  existing: LiveClassSession?,
  onDismiss: () -> Unit,
  onSave: (LiveClassSession) -> Unit
) {
  var title by remember { mutableStateOf(existing?.title ?: "") }
  var subject by remember { mutableStateOf(existing?.subject ?: "Mathematics") }
  var educator by remember { mutableStateOf(existing?.educatorName ?: "Prof. S. Datta") }
  var selectedCourseId by remember { mutableStateOf(existing?.courseId ?: courses.firstOrNull()?.id ?: "") }
  var selectedBatchId by remember { mutableStateOf(existing?.batchId ?: batches.firstOrNull()?.id ?: "") }
  var scheduledDate by remember {
    mutableStateOf(
      existing?.scheduledDate?.ifBlank {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
      } ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    )
  }
  var startTime by remember { mutableStateOf(existing?.startTime ?: "10:00 AM") }
  var endTime by remember { mutableStateOf(existing?.endTime ?: "11:30 AM") }
  var platform by remember { mutableStateOf(existing?.platform ?: "Google Meet") }
  var meetingUrl by remember { mutableStateOf(existing?.meetingUrl ?: "https://meet.google.com/new") }
  var status by remember { mutableStateOf(existing?.status ?: "scheduled") }
  var errorText by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (existing == null) "Schedule New Class" else "Edit Scheduled Class", fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it; errorText = null },
          label = { Text("Class / Topic Title *") },
          placeholder = { Text("e.g. Calculus Chapter 3 Revision") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = subject,
          onValueChange = { subject = it },
          label = { Text("Subject *") },
          placeholder = { Text("Math, Science, Bengali, English...") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = educator,
          onValueChange = { educator = it },
          label = { Text("Faculty / Educator Name *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Text("Target Admission Batch:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          batches.forEach { b ->
            FilterChip(
              selected = selectedBatchId == b.id,
              onClick = { selectedBatchId = b.id },
              label = { Text(b.name, fontSize = 11.sp) }
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = scheduledDate,
            onValueChange = { scheduledDate = it },
            label = { Text("Date (YYYY-MM-DD)") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = startTime,
            onValueChange = { startTime = it },
            label = { Text("Start Time") },
            placeholder = { Text("10:00 AM") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = endTime,
            onValueChange = { endTime = it },
            label = { Text("End Time") },
            placeholder = { Text("11:30 AM") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = platform,
            onValueChange = { platform = it },
            label = { Text("Platform") },
            placeholder = { Text("Google Meet / Web Portal") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }

        OutlinedTextField(
          value = meetingUrl,
          onValueChange = { meetingUrl = it },
          label = { Text("Meeting / Portal Link *") },
          placeholder = { Text("https://meet.google.com/...") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Text("Status:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("scheduled", "live", "completed").forEach { st ->
            FilterChip(
              selected = status == st,
              onClick = { status = st },
              label = { Text(st.uppercase(), fontSize = 11.sp) }
            )
          }
        }

        if (errorText != null) {
          Text(text = errorText!!, color = ErrorRed, fontSize = 12.sp)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isBlank()) {
            errorText = "Please enter class title"
            return@Button
          }
          val session = LiveClassSession(
            id = existing?.id ?: "class-${System.currentTimeMillis()}",
            title = title.trim(),
            subject = subject.trim(),
            courseId = selectedCourseId,
            batchId = selectedBatchId,
            educatorName = educator.trim(),
            scheduledDate = scheduledDate.trim(),
            startTime = startTime.trim(),
            endTime = endTime.trim(),
            status = status,
            platform = platform.trim(),
            meetingUrl = meetingUrl.trim().ifBlank { "https://meet.google.com/new" }
          )
          onSave(session)
        },
        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
      ) {
        Text(if (existing == null) "Schedule Class" else "Save Changes")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@Composable
fun AddNoteWebPortalDialog(
  courses: List<Course>,
  batches: List<Batch>,
  existing: StudyMaterial?,
  onDismiss: () -> Unit,
  onSave: (StudyMaterial) -> Unit
) {
  var title by remember { mutableStateOf(existing?.title ?: "") }
  var subject by remember { mutableStateOf(existing?.subject ?: "General") }
  var selectedCourseId by remember { mutableStateOf(existing?.courseId ?: courses.firstOrNull()?.id ?: "") }
  var selectedBatchId by remember { mutableStateOf(existing?.batchId ?: batches.firstOrNull()?.id ?: "") }
  var fileType by remember { mutableStateOf(existing?.fileType ?: "Web Portal") }
  var fileUrl by remember { mutableStateOf(existing?.fileUrl ?: "https://") }
  var description by remember { mutableStateOf(existing?.description ?: "") }
  var errorText by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (existing == null) "Add Note as Web Portal" else "Edit Note / Portal", fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it; errorText = null },
          label = { Text("Note / Portal Title *") },
          placeholder = { Text("e.g. Chapter 4 Formulas & Web Notes") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = subject,
          onValueChange = { subject = it },
          label = { Text("Subject *") },
          placeholder = { Text("Mathematics, Physics, Bengali...") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Text("Target Admission Batch:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          batches.forEach { b ->
            FilterChip(
              selected = selectedBatchId == b.id,
              onClick = { selectedBatchId = b.id },
              label = { Text(b.name, fontSize = 11.sp) }
            )
          }
        }

        Text("Resource Type:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("Web Portal", "PDF", "Google Drive", "Document").forEach { ft ->
            FilterChip(
              selected = fileType.equals(ft, ignoreCase = true),
              onClick = { fileType = ft },
              label = { Text(ft, fontSize = 11.sp) }
            )
          }
        }

        OutlinedTextField(
          value = fileUrl,
          onValueChange = { fileUrl = it; errorText = null },
          label = { Text("Web Portal URL / Document Link *") },
          placeholder = { Text("https://portal.pixelpathsala.com/... or Google Drive link") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description / Study Instructions") },
          placeholder = { Text("Read before Monday class...") },
          modifier = Modifier.fillMaxWidth()
        )

        if (errorText != null) {
          Text(text = errorText!!, color = ErrorRed, fontSize = 12.sp)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isBlank()) {
            errorText = "Please enter note title"
            return@Button
          }
          if (fileUrl.isBlank() || fileUrl == "https://") {
            errorText = "Please enter valid web portal or file link"
            return@Button
          }
          val material = StudyMaterial(
            id = existing?.id ?: "mat-${System.currentTimeMillis()}",
            title = title.trim(),
            subject = subject.trim(),
            courseId = selectedCourseId,
            batchId = selectedBatchId,
            fileType = fileType,
            fileUrl = fileUrl.trim(),
            uploadedDate = existing?.uploadedDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
            description = description.trim().ifBlank { null }
          )
          onSave(material)
        },
        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
      ) {
        Text(if (existing == null) "Add Note as Web Portal" else "Save Changes")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}
