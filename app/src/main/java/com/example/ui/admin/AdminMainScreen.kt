package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.PathsalaRepository
import com.example.ui.components.CenterLogo
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(
  repository: PathsalaRepository,
  onLogout: () -> Unit
) {
  var selectedTab by remember { mutableStateOf(0) }
  var showSettingsDialog by remember { mutableStateOf(false) }

  val payments by repository.payments.collectAsState()
  val admissions by repository.admissions.collectAsState()
  val liveClasses by repository.liveClasses.collectAsState()
  val isSyncing by repository.isSyncing.collectAsState()
  val adminUser by repository.currentAdmin.collectAsState()

  val pendingApprovalsCount = payments.filter { it.status == "pending" }.size +
    admissions.filter { it.status == "pending" }.size
  val hasLiveClass = liveClasses.any { it.status == "live" }

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
                text = "Pixel Pathsala ERP",
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                color = Color.White
              )
              Text(
                text = "Admin: ${adminUser?.username ?: "admin"} (${adminUser?.role ?: "Director"})",
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
          IconButton(onClick = { showSettingsDialog = true }) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = "Settings",
              tint = Color(0xFFCBD5E1)
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
          label = { Text("Overview", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
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
                if (pendingApprovalsCount > 0) {
                  Badge(containerColor = AmberPending) {
                    Text("$pendingApprovalsCount", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                  }
                }
              }
            ) {
              Icon(Icons.Default.PendingActions, contentDescription = "Approvals")
            }
          },
          label = { Text("Approvals", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = IndigoPrimary,
            selectedTextColor = IndigoPrimary,
            indicatorColor = Color(0xFFEEF2FF)
          )
        )

        NavigationBarItem(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          icon = { Icon(Icons.Default.People, contentDescription = "Students") },
          label = { Text("Students", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = IndigoPrimary,
            selectedTextColor = IndigoPrimary,
            indicatorColor = Color(0xFFEEF2FF)
          )
        )

        NavigationBarItem(
          selected = selectedTab == 3,
          onClick = { selectedTab = 3 },
          icon = {
            BadgedBox(
              badge = {
                if (hasLiveClass) {
                  Badge(containerColor = Color(0xFFE11D48)) {
                    Text("LIVE", fontSize = 8.sp, fontWeight = FontWeight.Black)
                  }
                }
              }
            ) {
              Icon(Icons.Default.Radio, contentDescription = "Live Class")
            }
          },
          label = { Text("Live Classes", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = if (hasLiveClass) Color(0xFFE11D48) else IndigoPrimary,
            selectedTextColor = if (hasLiveClass) Color(0xFFE11D48) else IndigoPrimary,
            indicatorColor = Color(0xFFEEF2FF)
          )
        )

        NavigationBarItem(
          selected = selectedTab == 4,
          onClick = { selectedTab = 4 },
          icon = { Icon(Icons.Default.CreditCard, contentDescription = "Fees") },
          label = { Text("Fee Ledger", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
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
        0 -> AdminDashboardTab(
          repository = repository,
          onNavigateToApprovals = { selectedTab = 1 },
          onNavigateToStudents = { selectedTab = 2 },
          onNavigateToLive = { selectedTab = 3 },
          onNavigateToFees = { selectedTab = 4 }
        )
        1 -> AdminApprovalsTab(repository = repository)
        2 -> AdminStudentsTab(repository = repository)
        3 -> AdminLiveClassTab(repository = repository)
        4 -> AdminFeeCollectionTab(repository = repository)
      }
    }
  }

  if (showSettingsDialog) {
    AdminSettingsDialog(
      repository = repository,
      onDismiss = { showSettingsDialog = false }
    )
  }
}
