package com.example.ui.student

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Student
import com.example.data.repository.PathsalaRepository
import com.example.ui.components.CenterLogo
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentMainScreen(
  student: Student,
  repository: PathsalaRepository,
  onLogout: () -> Unit
) {
  var selectedTab by remember { mutableStateOf(0) }
  val isSyncing by repository.isSyncing.collectAsState()
  val liveClasses by repository.liveClasses.collectAsState()

  val hasActiveLiveClass = liveClasses.any {
    it.status == "live" && (it.batchId == "all" || it.batchId == student.batchId)
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            CenterLogo(size = 36)
            Column {
              Text(
                text = student.name,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                color = Color.White
              )
              Text(
                text = "Roll: ${student.rollNo} • ${student.studentClass ?: "Class 10"}",
                fontSize = 11.sp,
                color = Color(0xFFA5B4FC)
              )
            }
          }
        },
        actions = {
          IconButton(
            onClick = { repository.syncWithBackend() },
            enabled = !isSyncing
          ) {
            Icon(
              imageVector = Icons.Default.Sync,
              contentDescription = "Sync",
              tint = if (isSyncing) Color(0xFFFBBF24) else Color.White
            )
          }
          IconButton(onClick = onLogout) {
            Icon(
              imageVector = Icons.Default.Logout,
              contentDescription = "Logout",
              tint = Color(0xFFFCA5A5)
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = Color(0xFF0F172A),
          titleContentColor = Color.White
        )
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars
      ) {
        NavigationBarItem(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
          label = { Text("Fees & Info", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = IndigoPrimary,
            selectedTextColor = IndigoPrimary,
            indicatorColor = Color(0xFFEEF2FF)
          )
        )

        NavigationBarItem(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          icon = {
            BadgedBox(
              badge = {
                if (hasActiveLiveClass) {
                  Badge(containerColor = Color(0xFFE11D48)) {
                    Text("LIVE", fontSize = 9.sp, fontWeight = FontWeight.Black)
                  }
                }
              }
            ) {
              Icon(Icons.Default.Radio, contentDescription = "Live Classes")
            }
          },
          label = { Text("Live Classes", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = if (hasActiveLiveClass) Color(0xFFE11D48) else IndigoPrimary,
            selectedTextColor = if (hasActiveLiveClass) Color(0xFFE11D48) else IndigoPrimary,
            indicatorColor = Color(0xFFEEF2FF)
          )
        )

        NavigationBarItem(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          icon = { Icon(Icons.Default.Assignment, contentDescription = "Exams") },
          label = { Text("Online Tests", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = IndigoPrimary,
            selectedTextColor = IndigoPrimary,
            indicatorColor = Color(0xFFEEF2FF)
          )
        )

        NavigationBarItem(
          selected = selectedTab == 3,
          onClick = { selectedTab = 3 },
          icon = { Icon(Icons.Default.Folder, contentDescription = "Materials") },
          label = { Text("Study Notes", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = IndigoPrimary,
            selectedTextColor = IndigoPrimary,
            indicatorColor = Color(0xFFEEF2FF)
          )
        )
      }
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(MaterialTheme.colorScheme.background)
    ) {
      when (selectedTab) {
        0 -> StudentDashboardTab(student = student, repository = repository)
        1 -> StudentLiveClassesTab(student = student, repository = repository)
        2 -> StudentExamsTab(student = student, repository = repository)
        3 -> StudentMaterialsTab(student = student, repository = repository)
      }
    }
  }
}
