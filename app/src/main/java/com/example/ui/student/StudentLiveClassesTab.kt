package com.example.ui.student

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClassRecording
import com.example.data.model.LiveClassSession
import com.example.data.model.Student
import com.example.data.repository.PathsalaRepository
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@Composable
fun StudentLiveClassesTab(
  student: Student,
  repository: PathsalaRepository
) {
  val context = LocalContext.current
  val liveClasses by repository.liveClasses.collectAsState()
  val recordings by repository.recordings.collectAsState()

  var selectedView by remember { mutableStateOf("live") } // "live" or "recordings"

  // Filter for student's batch, course or all-batch sessions
  val studentLive = liveClasses.filter {
    it.batchId.isBlank() || it.batchId == "all" || it.batchId == student.batchId ||
    it.courseId.isBlank() || it.courseId == "all" || it.courseId == student.courseId
  }
  val activeLive = studentLive.filter { it.status == "live" }
  val upcomingLive = studentLive.filter { it.status == "scheduled" }

  val studentRecordings = recordings.filter {
    it.batchId.isBlank() || it.batchId == "all" || it.batchId == student.batchId ||
    it.courseId.isBlank() || it.courseId == "all" || it.courseId == student.courseId
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Top view switcher
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(Color(0xFFE2E8F0))
          .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selectedView == "live") Color.White else Color.Transparent)
            .clickable { selectedView = "live" }
            .padding(vertical = 8.dp),
          contentAlignment = Alignment.Center
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              Icons.Default.Radio,
              contentDescription = null,
              tint = if (activeLive.isNotEmpty()) RoseLive else IndigoPrimary,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = "Live Sessions (${studentLive.size})",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = if (selectedView == "live") Color(0xFF0F172A) else Color(0xFF64748B)
            )
          }
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selectedView == "recordings") Color.White else Color.Transparent)
            .clickable { selectedView = "recordings" }
            .padding(vertical = 8.dp),
          contentAlignment = Alignment.Center
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(16.dp))
            Text(
              text = "Recorded Vault (${studentRecordings.size})",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = if (selectedView == "recordings") Color(0xFF0F172A) else Color(0xFF64748B)
            )
          }
        }
      }
    }

    if (selectedView == "live") {
      // 1. ACTIVE LIVE NOW HERO CARD
      if (activeLive.isNotEmpty()) {
        item {
          activeLive.forEach { session ->
            Card(
              shape = RoundedCornerShape(24.dp),
              colors = CardDefaults.cardColors(containerColor = Color(0xFF881337)),
              border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFB7185)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp)
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      Box(
                        modifier = Modifier
                          .size(8.dp)
                          .clip(CircleShape)
                          .background(Color(0xFFE11D48))
                      )
                      Text(
                        text = "LIVE CLASS HAPPENING NOW",
                        color = Color(0xFFBE123C),
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                      )
                    }
                  }
                  Text(
                    text = session.platform.replace("_", " ").uppercase(),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                  )
                }

                Text(
                  text = session.title,
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Black,
                  color = Color.White,
                  lineHeight = 22.sp
                )

                Text(
                  text = "${session.subject} • Faculty: ${session.educatorName}",
                  fontSize = 12.sp,
                  color = Color(0xFFFFE4E6),
                  fontWeight = FontWeight.SemiBold
                )

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "Time: ${session.startTime} - ${session.endTime}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    fontFamily = FontFamily.Monospace
                  )

                  Button(
                    onClick = {
                      val url = session.meetingUrl ?: "https://meet.google.com/new"
                      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                      context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                  ) {
                    Icon(Icons.Default.VideoCall, contentDescription = null, tint = Color(0xFFBE123C), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Join Live", color = Color(0xFFBE123C), fontWeight = FontWeight.Black, fontSize = 12.sp)
                  }
                }
              }
            }
          }
        }
      }

      // 2. UPCOMING LIVE SESSIONS
      item {
        SectionHeader(
          title = "Upcoming Scheduled Sessions",
          subtitle = "Calendar of live masterclasses & doubt clinics"
        )
      }

      if (upcomingLive.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.EventAvailable, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(32.dp))
              Text("No upcoming live classes scheduled", fontWeight = FontWeight.Bold, fontSize = 14.sp)
              Text("Your faculty will notify you before the next live broadcast.", fontSize = 12.sp, color = SlateTextSecondary)
            }
          }
        }
      } else {
        items(upcomingLive) { session ->
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
                StatusBadge(status = session.status)
                Text(
                  text = session.scheduledDate,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = SlateTextSecondary,
                  fontFamily = FontFamily.Monospace
                )
              }

              Text(
                text = session.title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
              )

              Text(
                text = "${session.subject} • Faculty: ${session.educatorName}",
                fontSize = 12.sp,
                color = IndigoPrimary,
                fontWeight = FontWeight.Medium
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

                if (!session.meetingUrl.isNullOrBlank()) {
                  OutlinedButton(
                    onClick = {
                      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(session.meetingUrl))
                      context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                  ) {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Class Link", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }
      }
    } else {
      // RECORDED LECTURES VAULT
      item {
        SectionHeader(
          title = "Recorded Video Lectures",
          subtitle = "Revision archives with lecture notes for 24/7 self-paced study"
        )
      }

      if (studentRecordings.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(32.dp))
              Text("No lecture recordings uploaded yet", fontWeight = FontWeight.Bold, fontSize = 14.sp)
              Text("Session recordings will be published after live classes conclude.", fontSize = 12.sp, color = SlateTextSecondary)
            }
          }
        }
      } else {
        items(studentRecordings) { rec ->
          Card(
            shape = RoundedCornerShape(20.dp),
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
                Surface(
                  color = Color(0xFFEEF2FF),
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Text(
                    text = rec.subject.uppercase(),
                    color = IndigoPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(Icons.Default.Schedule, contentDescription = null, tint = SlateTextSecondary, modifier = Modifier.size(12.dp))
                  Text(text = "${rec.durationMinutes} mins", fontSize = 11.sp, color = SlateTextSecondary, fontFamily = FontFamily.Monospace)
                }
              }

              Text(
                text = rec.title,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
              )

              if (!rec.topic.isNullOrBlank()) {
                Text(
                  text = "Topic: ${rec.topic}",
                  fontSize = 12.sp,
                  color = SlateTextSecondary,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "By ${rec.educatorName}",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = SlateTextPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  if (!rec.notesPdfUrl.isNullOrBlank()) {
                    OutlinedButton(
                      onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rec.notesPdfUrl))
                        context.startActivity(intent)
                      },
                      shape = RoundedCornerShape(10.dp),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                      Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFDC2626))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                  }

                  Button(
                    onClick = {
                      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rec.videoUrl))
                      context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                  ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Watch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
