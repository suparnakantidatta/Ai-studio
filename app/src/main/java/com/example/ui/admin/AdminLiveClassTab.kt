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
import com.example.data.model.ClassRecording
import com.example.data.model.LiveClassSession
import com.example.data.repository.PathsalaRepository
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@Composable
fun AdminLiveClassTab(
  repository: PathsalaRepository
) {
  val context = LocalContext.current
  val liveClasses by repository.liveClasses.collectAsState()
  val recordings by repository.recordings.collectAsState()
  val batches by repository.batches.collectAsState()
  val courses by repository.courses.collectAsState()

  var showScheduleDialog by remember { mutableStateOf(false) }

  // Schedule dialog inputs
  var sessionTitle by remember { mutableStateOf("") }
  var sessionSubject by remember { mutableStateOf("Computer Science & Coding") }
  var sessionClass by remember { mutableStateOf("Class 10 (Secondary)") }
  var sessionDate by remember { mutableStateOf("2026-09-05") }
  var sessionStartTime by remember { mutableStateOf("05:00 PM") }
  var sessionEndTime by remember { mutableStateOf("06:30 PM") }
  var sessionEducator by remember { mutableStateOf("Prof. Subhasish Mukherjee") }
  var sessionMeetingUrl by remember { mutableStateOf("https://meet.google.com/new") }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(text = "Live Classroom & Recordings", fontWeight = FontWeight.Black, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
          Text(text = "Broadcast live streams and manage video archives", fontSize = 12.sp, color = SlateTextSecondary)
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

    // Sessions List
    item {
      SectionHeader(title = "Live Broadcast Sessions (${liveClasses.size})")
    }

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
              Surface(
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = session.academicClass ?: "All Classes",
                  color = SlateTextSecondary,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }

            Text(
              text = session.scheduledDate,
              fontSize = 11.sp,
              fontFamily = FontFamily.Monospace,
              color = SlateTextSecondary,
              fontWeight = FontWeight.Bold
            )
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
              text = "${session.startTime} - ${session.endTime}",
              fontSize = 12.sp,
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

    // Recordings Vault
    item {
      Spacer(modifier = Modifier.height(8.dp))
      SectionHeader(
        title = "Lecture Video Archives (${recordings.size})",
        subtitle = "On-demand recordings available for students"
      )
    }

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

          IconButton(
            onClick = {
              val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rec.videoUrl))
              context.startActivity(intent)
            }
          ) {
            Icon(Icons.Default.PlayCircle, contentDescription = "Play", tint = IndigoPrimary)
          }
        }
      }
    }
  }

  // Schedule Live Class Dialog
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
                  Toast.makeText(context, "Live class scheduled!", Toast.LENGTH_SHORT).show()
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
}
