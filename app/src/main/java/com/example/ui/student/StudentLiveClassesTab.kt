package com.example.ui.student

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import com.example.alarm.ClassAlarmAudioPlayer
import com.example.alarm.ClassAlarmManager
import com.example.alarm.ClassNotificationHelper
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

  val isAlarmRinging by ClassAlarmAudioPlayer.isRinging.collectAsState()
  val activeRingingTitle by ClassAlarmAudioPlayer.activeAlarmSessionTitle.collectAsState()

  var selectedView by remember { mutableStateOf("live") } // "live" or "recordings"

  // Filter for student's batch, course or all-batch sessions
  val studentLive = liveClasses.filter {
    it.batchId.isBlank() || it.batchId == "all" || it.batchId == student.batchId ||
    it.courseId.isBlank() || it.courseId == "all" || it.courseId == student.courseId
  }
  val activeLive = studentLive.filter { it.status == "live" }
  val upcomingLive = studentLive.filter { it.status == "scheduled" }
  val postponedLive = studentLive.filter { it.status == "postponed" }

  val studentRecordings = recordings.filter {
    it.batchId.isBlank() || it.batchId == "all" || it.batchId == student.batchId ||
    it.courseId.isBlank() || it.courseId == "all" || it.courseId == student.courseId
  }

  // Request notifications permission on API 33+ and sync alarms
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      Toast.makeText(context, "Class alarms & notifications enabled!", Toast.LENGTH_SHORT).show()
    }
  }

  LaunchedEffect(Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    ClassNotificationHelper.createNotificationChannels(context)
    ClassAlarmManager.autoScheduleAlarmsForStudent(context, upcomingLive)
    ClassAlarmManager.checkAndNotifyPostponedClasses(context, studentLive)
  }

  // Auto alert if postponed classes change
  LaunchedEffect(studentLive) {
    ClassAlarmManager.checkAndNotifyPostponedClasses(context, studentLive)
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
      // 0. ACTIVE ALARM RINGING HERO ALERT
      if (isAlarmRinging) {
        item {
          Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFEF4444)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(
                    Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                  )
                  Text(
                    text = "⏰ CLASS ALARM RINGING NOW!",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                  )
                }
                Surface(color = Color(0xFFEF4444), shape = RoundedCornerShape(8.dp)) {
                  Text(
                    text = "ON TIME",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              Text(
                text = activeRingingTitle ?: "Your scheduled live classroom is starting now!",
                color = Color(0xFFFEE2E2),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Button(
                  onClick = {
                    ClassAlarmAudioPlayer.stopAlarm(context)
                    Toast.makeText(context, "Alarm stopped", Toast.LENGTH_SHORT).show()
                  },
                  shape = RoundedCornerShape(10.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                  Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF7F1D1D), modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Stop Alarm", color = Color(0xFF7F1D1D), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
              }
            }
          }
        }
      }

      // 0.5. POSTPONED CLASSES NOTICE BANNER
      if (postponedLive.isNotEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  Icons.Default.Warning,
                  contentDescription = null,
                  tint = Color(0xFFD97706),
                  modifier = Modifier.size(22.dp)
                )
                Text(
                  text = "Postponed Classes Notice (${postponedLive.size})",
                  fontWeight = FontWeight.Black,
                  fontSize = 15.sp,
                  color = Color(0xFF92400E)
                )
              }

              postponedLive.forEach { session ->
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = Color.White,
                  border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(
                        text = session.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF78350F)
                      )
                      Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(6.dp)) {
                        Text(
                          text = "POSTPONED",
                          color = Color(0xFFB45309),
                          fontSize = 9.sp,
                          fontWeight = FontWeight.Black,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }
                    }

                    Text(
                      text = "Reason: ${session.postponeReason ?: "Faculty has postponed this session"}",
                      fontSize = 12.sp,
                      color = Color(0xFF92400E)
                    )

                    if (!session.rescheduledDate.isNullOrBlank()) {
                      Text(
                        text = "Rescheduled to: ${session.rescheduledDate} ${session.rescheduledTime ?: ""}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = IndigoPrimary
                      )
                    }

                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.End,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      OutlinedButton(
                        onClick = {
                          ClassAlarmManager.triggerPostponedAlert(context, session)
                          Toast.makeText(context, "Postpone alarm alert sound ringing!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                      ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ring Alarm Sound", fontSize = 11.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }

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

      if (upcomingLive.isNotEmpty()) {
        item {
          Surface(
            color = Color(0xFFEFF6FF),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(Icons.Default.AccessAlarm, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(18.dp))
                Text("Alarms Set for All Live Classes", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
              }

              TextButton(
                onClick = {
                  upcomingLive.firstOrNull()?.let { firstSession ->
                    ClassAlarmManager.triggerTestAlarm(context, firstSession)
                    Toast.makeText(context, "Testing alarm! Ringing on-time sound & notification.", Toast.LENGTH_SHORT).show()
                  }
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
              ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Test Alarm Now", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IndigoPrimary)
              }
            }
          }
        }
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
          var isAlarmOn by remember(session.id) {
            mutableStateOf(ClassAlarmManager.isAlarmSet(context, session.id))
          }

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
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  StatusBadge(status = session.status)
                  Surface(
                    color = if (isAlarmOn) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable {
                      val newState = !isAlarmOn
                      if (newState) {
                        ClassAlarmManager.setAlarmForClass(context, session)
                        Toast.makeText(context, "Alarm set for ${session.startTime} on ${session.scheduledDate}", Toast.LENGTH_SHORT).show()
                      } else {
                        ClassAlarmManager.cancelAlarmForClass(context, session)
                        Toast.makeText(context, "Alarm disabled", Toast.LENGTH_SHORT).show()
                      }
                      isAlarmOn = newState
                    }
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                      Icon(
                        if (isAlarmOn) Icons.Default.AlarmOn else Icons.Default.Alarm,
                        contentDescription = null,
                        tint = if (isAlarmOn) EmeraldSuccess else SlateTextSecondary,
                        modifier = Modifier.size(12.dp)
                      )
                      Text(
                        text = if (isAlarmOn) "Alarm ON" else "Set Alarm",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAlarmOn) EmeraldSuccess else SlateTextSecondary
                      )
                    }
                  }
                }
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

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  OutlinedButton(
                    onClick = {
                      ClassAlarmManager.triggerTestAlarm(context, session)
                      Toast.makeText(context, "Alarm ringing! Notification & Sound activated.", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                  ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(13.dp), tint = IndigoPrimary)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Test Alarm", fontSize = 11.sp)
                  }

                  if (!session.meetingUrl.isNullOrBlank()) {
                    Button(
                      onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(session.meetingUrl))
                        context.startActivity(intent)
                      },
                      shape = RoundedCornerShape(10.dp),
                      colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                      Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(13.dp))
                      Spacer(modifier = Modifier.width(3.dp))
                      Text("Class Link", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
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
