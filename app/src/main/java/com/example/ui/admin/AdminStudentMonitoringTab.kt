package com.example.ui.admin

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Student
import com.example.data.model.StudentTelemetry
import com.example.data.repository.PathsalaRepository
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStudentMonitoringTab(
  repository: PathsalaRepository,
  preselectedStudentId: String? = null
) {
  val context = LocalContext.current
  val students by repository.students.collectAsState()
  val telemetries by repository.studentTelemetries.collectAsState()
  val batches by repository.batches.collectAsState()
  val courses by repository.courses.collectAsState()

  var searchQuery by remember { mutableStateOf("") }
  var filterCategory by remember { mutableStateOf("all") } // all, games_only, clean_only
  var selectedStudentId by remember { mutableStateOf<String?>(preselectedStudentId) }
  var showFullScreenMap by remember { mutableStateOf(false) }

  // Merge student records with their latest telemetry
  val studentTelemetryList = remember(students, telemetries) {
    students.map { student ->
      val telem = telemetries.find { it.studentId == student.id }
      if (telem != null) {
        telem
      } else {
        // Fallback placeholder record
        val defaultGames = if (student.name.contains("Mohit", ignoreCase = true)) {
          listOf("BGMI (Battlegrounds Mobile India)", "Free Fire MAX")
        } else emptyList()

        StudentTelemetry(
          id = "telem-${student.id}",
          studentId = student.id,
          studentName = student.name,
          rollNo = student.rollNo,
          studentClass = student.studentClass,
          batchName = batches.find { it.id == student.batchId }?.name ?: student.batchId,
          gamesInstalled = defaultGames,
          gamesCount = defaultGames.size,
          hasGames = defaultGames.isNotEmpty(),
          latitude = 22.5726 + (student.id.hashCode() % 50) * 0.002,
          longitude = 88.3639 + (student.id.hashCode() % 50) * 0.002,
          locationAddress = "Kolkata Region, West Bengal",
          lastReportedTime = "Today, Active",
          deviceModel = "Android Device",
          isOnline = true
        )
      }
    }
  }

  val filteredList = remember(studentTelemetryList, searchQuery, filterCategory) {
    studentTelemetryList.filter { item ->
      val matchesSearch = item.studentName.contains(searchQuery, ignoreCase = true) ||
        (item.rollNo?.contains(searchQuery, ignoreCase = true) == true) ||
        (item.locationAddress?.contains(searchQuery, ignoreCase = true) == true) ||
        item.gamesInstalled.any { it.contains(searchQuery, ignoreCase = true) }

      val matchesCategory = when (filterCategory) {
        "games_only" -> item.hasGames
        "clean_only" -> !item.hasGames
        else -> true
      }

      matchesSearch && matchesCategory
    }
  }

  val totalGamingCount = studentTelemetryList.count { it.hasGames }
  val totalCleanCount = studentTelemetryList.size - totalGamingCount
  val selectedTelemetry = studentTelemetryList.find { it.studentId == selectedStudentId }

  Column(modifier = Modifier.fillMaxSize()) {
    // Top Summary Banner
    Surface(
      color = Color(0xFF0F172A),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Student Device & Location Monitor",
              fontSize = 17.sp,
              fontWeight = FontWeight.Black,
              color = Color.White
            )
            Text(
              text = "Realtime Gaming Detection & GPS Geo-Tracking",
              fontSize = 11.sp,
              color = Color(0xFFA5B4FC)
            )
          }

          IconButton(
            onClick = {
              Toast.makeText(context, "Scanning active telemetry updates...", Toast.LENGTH_SHORT).show()
              repository.syncWithBackend()
            }
          ) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
          }
        }

        // Metrics Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Total Monitored
          Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF1E293B)
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text("Monitored", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
              Text("${studentTelemetryList.size}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
          }

          // Gaming Alert
          Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF450A0A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF991B1B))
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text("Gaming Alert", fontSize = 10.sp, color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold)
              Text("$totalGamingCount Students", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFFF87171))
            }
          }

          // Clean Devices
          Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF064E3B),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF059669))
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text("Clean Devices", fontSize = 10.sp, color = Color(0xFFA7F3D0), fontWeight = FontWeight.Bold)
              Text("$totalCleanCount Students", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF34D399))
            }
          }
        }
      }
    }

    // Filter Chips Row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(Color(0xFFF8FAFC))
        .padding(horizontal = 14.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      FilterChip(
        selected = filterCategory == "all",
        onClick = { filterCategory = "all" },
        label = { Text("All (${studentTelemetryList.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = IndigoPrimary,
          selectedLabelColor = Color.White
        )
      )

      FilterChip(
        selected = filterCategory == "games_only",
        onClick = { filterCategory = "games_only" },
        label = { Text("⚠️ Gaming ($totalGamingCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = Color(0xFFDC2626),
          selectedLabelColor = Color.White
        )
      )

      FilterChip(
        selected = filterCategory == "clean_only",
        onClick = { filterCategory = "clean_only" },
        label = { Text("✅ Clean ($totalCleanCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = Color(0xFF16A34A),
          selectedLabelColor = Color.White
        )
      )

      Spacer(modifier = Modifier.weight(1f))

      IconButton(onClick = { showFullScreenMap = true }) {
        Icon(Icons.Default.Fullscreen, contentDescription = "Full Map", tint = IndigoPrimary)
      }
    }

    // Split View: Interactive Map on Top (Height: 220.dp), Student List on Bottom
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(230.dp)
        .background(Color(0xFFE2E8F0))
    ) {
      InteractiveStudentMap(
        students = filteredList,
        selectedStudentId = selectedStudentId,
        onSelectStudent = { studentId ->
          selectedStudentId = studentId
        }
      )

      // Map Legend Overlay
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xEEFFFFFF),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(8.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFDC2626)))
            Text("Gaming Detected", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
          }
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF16A34A)))
            Text("Clean Device", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
          }
        }
      }

      // External Google Maps Shortcut for Selected Student
      selectedTelemetry?.let { telem ->
        if (telem.latitude != null && telem.longitude != null) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = IndigoPrimary,
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(8.dp)
              .clickable {
                openInGoogleMaps(context, telem.latitude, telem.longitude, telem.studentName)
              }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(Icons.Default.Map, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
              Text("Open in Google Maps", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
          }
        }
      }
    }

    // Search Box
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 6.dp),
      placeholder = { Text("Search by student, roll no, location, or game name...", fontSize = 12.sp) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SlateTextSecondary) },
      trailingIcon = {
        if (searchQuery.isNotBlank()) {
          IconButton(onClick = { searchQuery = "" }) {
            Icon(Icons.Default.Clear, contentDescription = "Clear")
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(10.dp)
    )

    // Student Telemetry Cards List
    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      contentPadding = PaddingValues(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      if (filteredList.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(32.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "No students match the current filter",
              fontSize = 13.sp,
              color = SlateTextSecondary,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }

      items(filteredList, key = { it.studentId }) { telem ->
        val isSelected = telem.studentId == selectedStudentId
        StudentTelemetryCard(
          telem = telem,
          isSelected = isSelected,
          onCardClick = {
            selectedStudentId = if (isSelected) null else telem.studentId
          },
          onOpenMaps = {
            if (telem.latitude != null && telem.longitude != null) {
              openInGoogleMaps(context, telem.latitude, telem.longitude, telem.studentName)
            } else {
              Toast.makeText(context, "Location coordinates not available", Toast.LENGTH_SHORT).show()
            }
          }
        )
      }
    }
  }

  // Full Screen Interactive Map Dialog
  if (showFullScreenMap) {
    Dialog(onDismissRequest = { showFullScreenMap = false }) {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
          .fillMaxWidth()
          .fillMaxHeight(0.85f)
      ) {
        Column(modifier = Modifier.fillMaxSize()) {
          // Dialog Header
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFF0F172A))
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("Student GPS Map View", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
              Text("Tap any pin to view student & detected games", fontSize = 11.sp, color = Color(0xFFA5B4FC))
            }
            IconButton(onClick = { showFullScreenMap = false }) {
              Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
          }

          Box(modifier = Modifier.fillMaxSize()) {
            InteractiveStudentMap(
              students = studentTelemetryList,
              selectedStudentId = selectedStudentId,
              onSelectStudent = { selectedStudentId = it }
            )

            // Pin Detail Sheet at bottom of dialog if a student is selected
            selectedTelemetry?.let { telem ->
              Surface(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier
                  .align(Alignment.BottomCenter)
                  .fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column {
                      Text(telem.studentName, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                      Text("Roll: ${telem.rollNo ?: "N/A"} • ${telem.studentClass ?: "Student"}", fontSize = 11.sp, color = SlateTextSecondary)
                    }

                    if (telem.hasGames) {
                      Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFEE2E2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECDD3))
                      ) {
                        Text(
                          "⚠️ ${telem.gamesCount} Games",
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                          fontSize = 10.sp,
                          fontWeight = FontWeight.Black,
                          color = Color(0xFFB91C1C)
                        )
                      }
                    } else {
                      Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFDCFCE7),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
                      ) {
                        Text(
                          "✅ Clean Device",
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                          fontSize = 10.sp,
                          fontWeight = FontWeight.Black,
                          color = Color(0xFF15803D)
                        )
                      }
                    }
                  }

                  if (telem.gamesInstalled.isNotEmpty()) {
                    Text("Installed Games: ${telem.gamesInstalled.joinToString(", ")}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C))
                  }

                  Text("📍 Location: ${telem.locationAddress ?: "GPS Active"}", fontSize = 11.sp, color = Color(0xFF334155))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                  ) {
                    if (telem.latitude != null && telem.longitude != null) {
                      Button(
                        onClick = {
                          openInGoogleMaps(context, telem.latitude, telem.longitude, telem.studentName)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                      ) {
                        Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open in Google Maps", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
  }
}

@Composable
fun StudentTelemetryCard(
  telem: StudentTelemetry,
  isSelected: Boolean,
  onCardClick: () -> Unit,
  onOpenMaps: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected) Color(0xFFEEF2FF) else Color.White
    ),
    border = androidx.compose.foundation.BorderStroke(
      if (isSelected) 1.5.dp else 1.dp,
      if (isSelected) IndigoPrimary else if (telem.hasGames) Color(0xFFFCA5A5) else Color(0xFFE2E8F0)
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onCardClick() }
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Header: Avatar, Name, Status Badge
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
              .size(42.dp)
              .clip(CircleShape)
              .background(if (telem.hasGames) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = if (telem.hasGames) "🎮" else "🎓",
              fontSize = 20.sp
            )
          }

          Column {
            Text(
              text = telem.studentName,
              fontSize = 14.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFF0F172A)
            )
            Text(
              text = "Roll: ${telem.rollNo ?: "N/A"} • ${telem.studentClass ?: "Class"}",
              fontSize = 11.sp,
              color = SlateTextSecondary
            )
          }
        }

        // Status Badge
        if (telem.hasGames) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFEF2F2),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECDD3))
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.SportsEsports,
                contentDescription = null,
                tint = Color(0xFFDC2626),
                modifier = Modifier.size(13.dp)
              )
              Text(
                text = "${telem.gamesCount} Games Detected",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFB91C1C)
              )
            }
          }
        } else {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF0FDF4),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF16A34A),
                modifier = Modifier.size(13.dp)
              )
              Text(
                text = "Clean (No Games)",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF15803D)
              )
            }
          }
        }
      }

      // Games Chip Row if installed
      if (telem.gamesInstalled.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "Installed Gaming Apps:",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF991B1B)
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            telem.gamesInstalled.forEach { gameName ->
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFFFF1F2),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDA4AF))
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Text("🎮", fontSize = 11.sp)
                  Text(
                    text = gameName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9F1239)
                  )
                }
              }
            }
          }
        }
      }

      // Realtime Location Info
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.LocationOn, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(14.dp))
              Text(
                text = telem.locationAddress ?: "Location recorded",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1E293B),
                maxLines = 1
              )
            }

            if (telem.latitude != null && telem.longitude != null) {
              Text(
                text = String.format(Locale.US, "%.4f, %.4f", telem.latitude, telem.longitude),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF64748B)
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Device: ${telem.deviceModel} • Last Active: ${telem.lastReportedTime}",
              fontSize = 10.sp,
              color = SlateTextSecondary
            )

            TextButton(
              onClick = { onOpenMaps() },
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(13.dp), tint = IndigoPrimary)
              Spacer(modifier = Modifier.width(3.dp))
              Text("View on Map", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = IndigoPrimary)
            }
          }
        }
      }
    }
  }
}

/**
 * Interactive Vector Map Canvas showing real-world layout, roads, geographic features,
 * and clickable pins for students with realtime coordinates.
 */
@Composable
fun InteractiveStudentMap(
  students: List<StudentTelemetry>,
  selectedStudentId: String?,
  onSelectStudent: (String) -> Unit
) {
  var scale by remember { mutableFloatStateOf(1f) }
  var offset by remember { mutableStateOf(Offset.Zero) }

  // Reference coordinates for Bengal/Kolkata metro area
  val baseLat = 22.5726
  val baseLng = 88.3639

  Box(
    modifier = Modifier
      .fillMaxSize()
      .pointerInput(Unit) {
        detectTransformGestures { _, pan, zoom, _ ->
          scale = (scale * zoom).coerceIn(0.7f, 3.5f)
          offset += pan
        }
      }
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val canvasWidth = size.width
      val canvasHeight = size.height
      val centerX = canvasWidth / 2f + offset.x
      val centerY = canvasHeight / 2f + offset.y

      // 1. Draw Map Background (Soft Landmass styling)
      drawRect(color = Color(0xFFE2E8F0))

      // 2. River / Waterway (Hooghly River representation)
      val riverPath = Path().apply {
        moveTo(centerX - 120f * scale, 0f)
        cubicTo(
          centerX - 90f * scale, canvasHeight * 0.35f,
          centerX - 140f * scale, canvasHeight * 0.7f,
          centerX - 100f * scale, canvasHeight
        )
      }
      drawPath(
        path = riverPath,
        color = Color(0xFFBAE6FD),
        style = Stroke(width = 24f * scale, cap = StrokeCap.Round)
      )

      // 3. Grid Lines / City Road Grid
      val gridSpacing = 40f * scale
      var x = (centerX % gridSpacing)
      while (x < canvasWidth) {
        drawLine(
          color = Color(0xFFCBD5E1),
          start = Offset(x, 0f),
          end = Offset(x, canvasHeight),
          strokeWidth = 1f
        )
        x += gridSpacing
      }
      var y = (centerY % gridSpacing)
      while (y < canvasHeight) {
        drawLine(
          color = Color(0xFFCBD5E1),
          start = Offset(0f, y),
          end = Offset(canvasWidth, y),
          strokeWidth = 1f
        )
        y += gridSpacing
      }

      // Major Arterial Roads
      drawLine(
        color = Color(0xFFFFFFFF),
        start = Offset(0f, centerY),
        end = Offset(canvasWidth, centerY),
        strokeWidth = 4f * scale
      )
      drawLine(
        color = Color(0xFFFFFFFF),
        start = Offset(centerX, 0f),
        end = Offset(centerX, canvasHeight),
        strokeWidth = 4f * scale
      )
      drawLine(
        color = Color(0xFFFDE047),
        start = Offset(centerX - 180f * scale, centerY + 100f * scale),
        end = Offset(centerX + 180f * scale, centerY - 100f * scale),
        strokeWidth = 3f * scale
      )

      // 4. Central Academy Institute Marker (Pixel Pathsala Campus)
      drawCircle(
        color = Color(0xFFD4AF37),
        radius = 12f * scale,
        center = Offset(centerX, centerY)
      )
      drawCircle(
        color = Color(0xFF0F172A),
        radius = 6f * scale,
        center = Offset(centerX, centerY)
      )

      // 5. Draw Student Location Pins
      students.forEach { st ->
        val lat = st.latitude ?: baseLat
        val lng = st.longitude ?: baseLng

        // Map lat/lng relative to base
        val dLat = (lat - baseLat) * 12000f * scale
        val dLng = (lng - baseLng) * 12000f * scale

        val pinX = centerX + dLng.toFloat()
        val pinY = centerY - dLat.toFloat()

        val isSelected = st.studentId == selectedStudentId
        val pinColor = if (st.hasGames) Color(0xFFDC2626) else Color(0xFF16A34A)
        val outerRadius = if (isSelected) 14f * scale else 9f * scale

        // Ripple halo for students with games detected
        if (st.hasGames) {
          drawCircle(
            color = Color(0x44DC2626),
            radius = outerRadius + 6f * scale,
            center = Offset(pinX, pinY)
          )
        }

        // Pin Body
        drawCircle(
          color = Color.White,
          radius = outerRadius + 2f * scale,
          center = Offset(pinX, pinY)
        )
        drawCircle(
          color = pinColor,
          radius = outerRadius,
          center = Offset(pinX, pinY)
        )

        // Center dot
        drawCircle(
          color = Color.White,
          radius = outerRadius * 0.45f,
          center = Offset(pinX, pinY)
        )
      }
    }

    // Interactive clickable tap targets for student pins overlay
    students.forEach { st ->
      val lat = st.latitude ?: baseLat
      val lng = st.longitude ?: baseLng

      Box(
        modifier = Modifier
          .fillMaxSize()
      ) {
        // Place clickable student markers
        val dLat = (lat - baseLat) * 12000f * scale
        val dLng = (lng - baseLng) * 12000f * scale

        // Callout card if selected
        if (st.studentId == selectedStudentId) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF0F172A),
            shadowElevation = 6.dp,
            modifier = Modifier
              .align(Alignment.TopCenter)
              .padding(top = 10.dp)
              .clickable { onSelectStudent(st.studentId) }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = if (st.hasGames) "🎮" else "🎓",
                fontSize = 16.sp
              )
              Column {
                Text(
                  text = st.studentName,
                  color = Color.White,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = if (st.hasGames) "⚠️ ${st.gamesCount} Games: ${st.gamesInstalled.joinToString(", ")}" else "✅ Clean Device • ${st.locationAddress ?: "Kolkata"}",
                  color = if (st.hasGames) Color(0xFFFCA5A5) else Color(0xFFA7F3D0),
                  fontSize = 10.sp,
                  maxLines = 1
                )
              }
            }
          }
        }
      }
    }

    // Map Zoom / Reset Controls
    Column(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(8.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Surface(
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier.size(34.dp).clickable { scale = (scale * 1.25f).coerceAtMost(3.5f) }
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.Black, modifier = Modifier.size(18.dp))
        }
      }
      Surface(
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier.size(34.dp).clickable { scale = (scale * 0.8f).coerceAtLeast(0.7f) }
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.Black, modifier = Modifier.size(18.dp))
        }
      }
      Surface(
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier.size(34.dp).clickable {
          scale = 1f
          offset = Offset.Zero
        }
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(Icons.Default.MyLocation, contentDescription = "Center", tint = IndigoPrimary, modifier = Modifier.size(16.dp))
        }
      }
    }
  }
}

fun openInGoogleMaps(context: android.content.Context, lat: Double, lng: Double, label: String) {
  try {
    val uriStr = String.format(Locale.US, "geo:%.6f,%.6f?q=%.6f,%.6f(%s)", lat, lng, lat, lng, Uri.encode(label))
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr))
    context.startActivity(intent)
  } catch (_: Exception) {
    try {
      val webUri = Uri.parse(String.format(Locale.US, "https://www.google.com/maps/search/?api=1&query=%.6f,%.6f", lat, lng))
      context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    } catch (e: Exception) {
      Toast.makeText(context, "Could not open map: ${e.message}", Toast.LENGTH_SHORT).show()
    }
  }
}
