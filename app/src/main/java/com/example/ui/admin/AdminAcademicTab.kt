package com.example.ui.admin

import android.content.Intent
import android.net.Uri
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
  onAddClass: (LiveClassSession) -> Unit,
  onUpdateClass: (LiveClassSession) -> Unit,
  onDeleteClass: (String) -> Unit,
  onAddNote: (StudyMaterial) -> Unit,
  onUpdateNote: (StudyMaterial) -> Unit,
  onDeleteNote: (String) -> Unit
) {
  var activeSubTab by remember { mutableIntStateOf(0) } // 0: Scheduled Classes, 1: Notes as Web Portal

  var showAddClassDialog by remember { mutableStateOf(false) }
  var editingClass by remember { mutableStateOf<LiveClassSession?>(null) }
  var deletingClassId by remember { mutableStateOf<String?>(null) }

  var showAddNoteDialog by remember { mutableStateOf(false) }
  var editingNote by remember { mutableStateOf<StudyMaterial?>(null) }
  var deletingNoteId by remember { mutableStateOf<String?>(null) }

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
          label = { Text("Notes & Web Portal (${studyMaterials.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
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
              Text(text = "Scheduled Classes", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate900)
              Text(text = "Manage live sessions, timetable & portal links", fontSize = 12.sp, color = Slate600)
            }
            Button(
              onClick = { showAddClassDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Schedule Class", fontSize = 12.sp)
            }
          }
        }

        if (liveClasses.isEmpty()) {
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
                Text("No classes scheduled yet.", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Slate700)
                Text("Click '+ Schedule Class' above to schedule a class with meeting/portal link.", fontSize = 12.sp, color = Slate500)
              }
            }
          }
        } else {
          items(liveClasses.reversed()) { session ->
            val course = courses.firstOrNull { it.id == session.courseId }
            val batch = batches.firstOrNull { it.id == session.batchId }

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
                  Text(text = session.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
                  Surface(
                    color = if (session.status == "live") ErrorRedLight else BrandGoldLight,
                    shape = RoundedCornerShape(6.dp)
                  ) {
                    Text(
                      text = session.status.uppercase(),
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (session.status == "live") ErrorRed else BrandGoldDark,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Subject: ${session.subject} • Faculty: ${session.educatorName}",
                  fontSize = 12.sp,
                  color = Slate700,
                  fontWeight = FontWeight.Medium
                )
                Text(
                  text = "Target Batch: ${batch?.name ?: "All Admission Batches"} • Course: ${course?.title ?: "All Courses"}",
                  fontSize = 12.sp,
                  color = BrandPrimary
                )
                Text(
                  text = "Timings: ${session.startTime} - ${session.endTime} (${session.scheduledDate.ifBlank { "Regular" }})",
                  fontSize = 12.sp,
                  color = Slate600
                )
                if (!session.meetingUrl.isNullOrBlank()) {
                  Text(
                    text = "Portal Link: ${session.meetingUrl}",
                    fontSize = 11.sp,
                    color = Slate500,
                    maxLines = 1
                  )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                      modifier = Modifier.weight(1f)
                    ) {
                      Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Launch Portal", fontSize = 12.sp)
                    }
                  }

                  OutlinedButton(
                    onClick = { editingClass = session },
                    shape = RoundedCornerShape(8.dp)
                  ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", fontSize = 12.sp)
                  }

                  IconButton(
                    onClick = { deletingClassId = session.id }
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
