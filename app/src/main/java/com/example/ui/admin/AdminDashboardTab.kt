package com.example.ui.admin

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FeePayment
import com.example.data.repository.PathsalaRepository
import com.example.ui.components.MetricCard
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@Composable
fun AdminDashboardTab(
  repository: PathsalaRepository,
  onNavigateToApprovals: () -> Unit,
  onNavigateToFees: () -> Unit,
  onNavigateToLive: () -> Unit,
  onNavigateToStudents: () -> Unit
) {
  val students by repository.students.collectAsState()
  val payments by repository.payments.collectAsState()
  val admissions by repository.admissions.collectAsState()
  val batches by repository.batches.collectAsState()
  val liveClasses by repository.liveClasses.collectAsState()
  val centerInfo by repository.centerInfo.collectAsState()

  val activeStudents = students.filter { it.status == "active" }
  val pendingPayments = payments.filter { it.status == "pending" }
  val pendingAdmissions = admissions.filter { it.status == "pending" }
  val approvedPayments = payments.filter { it.status == "approved" }
  val totalCollected = approvedPayments.sumOf { it.finalAmountPaid }

  val activeLiveClasses = liveClasses.filter { it.status == "live" }

  var selectedReceipt by remember { mutableStateOf<FeePayment?>(null) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Top KPI Metric Cards Grid
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        MetricCard(
          title = "Enrolled Students",
          value = "${activeStudents.size}",
          subtitle = "${students.size} Total Registered",
          icon = Icons.Default.People,
          iconTint = IndigoPrimary,
          modifier = Modifier.weight(1f)
        )
        MetricCard(
          title = "Fee Revenue",
          value = "₹${(totalCollected / 1000).toInt()}k",
          subtitle = "${approvedPayments.size} Receipts Cleared",
          icon = Icons.Default.CurrencyRupee,
          iconTint = EmeraldSuccess,
          modifier = Modifier.weight(1f)
        )
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        MetricCard(
          title = "Pending Approvals",
          value = "${pendingPayments.size + pendingAdmissions.size}",
          subtitle = "${pendingPayments.size} Fees • ${pendingAdmissions.size} Admissions",
          icon = Icons.Default.PendingActions,
          iconTint = if (pendingPayments.isNotEmpty() || pendingAdmissions.isNotEmpty()) AmberPending else EmeraldSuccess,
          modifier = Modifier.weight(1f)
        )
        MetricCard(
          title = "Active Batches",
          value = "${batches.size}",
          subtitle = "${activeLiveClasses.size} Live Now",
          icon = Icons.Default.Layers,
          iconTint = CyanAccent,
          modifier = Modifier.weight(1f)
        )
      }
    }

    // Pending Action Alerts
    if (pendingPayments.isNotEmpty() || pendingAdmissions.isNotEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AmberPending)
                Text(
                  text = "Pending Approvals Required",
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = Color(0xFF92400E)
                )
              }
              Button(
                onClick = onNavigateToApprovals,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AmberPending),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
              ) {
                Text("Review Now", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
              }
            }

            Text(
              text = "${pendingPayments.size} student fee payments awaiting UTR verification and ${pendingAdmissions.size} online admission applications ready for onboarding.",
              fontSize = 12.sp,
              color = Color(0xFFB45309),
              lineHeight = 16.sp
            )
          }
        }
      }
    }

    // Live Stream Active Banner
    if (activeLiveClasses.isNotEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF881337)),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFB7185)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE11D48))
                )
                Text("LIVE BROADCAST ACTIVE", color = Color(0xFFFECDD3), fontWeight = FontWeight.Black, fontSize = 11.sp)
              }
              Text(
                text = activeLiveClasses[0].title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 2.dp)
              )
            }

            Button(
              onClick = onNavigateToLive,
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color.White),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Text("Manage", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBE123C))
            }
          }
        }
      }
    }

    // Quick Actions
    item {
      SectionHeader(title = "Quick Actions")
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        QuickActionButton(
          label = "Collect Fee",
          icon = Icons.Default.AddCard,
          onClick = onNavigateToFees,
          modifier = Modifier.weight(1f)
        )
        QuickActionButton(
          label = "Add Student",
          icon = Icons.Default.PersonAdd,
          onClick = onNavigateToStudents,
          modifier = Modifier.weight(1f)
        )
        QuickActionButton(
          label = "Live Class",
          icon = Icons.Default.VideoCall,
          onClick = onNavigateToLive,
          modifier = Modifier.weight(1f)
        )
      }
    }

    // Recent Fee Transactions
    item {
      SectionHeader(
        title = "Recent Fee Receipts",
        subtitle = "Latest transactions across all student batches",
        actionText = "View All",
        onActionClick = onNavigateToFees
      )
    }

    val recentPayments = payments.take(6)
    if (recentPayments.isEmpty()) {
      item {
        Text("No fee transactions logged yet.", fontSize = 12.sp, color = SlateTextSecondary)
      }
    } else {
      items(recentPayments) { pay ->
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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = pay.studentName,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "${pay.receiptNo} • ${pay.month}",
                fontSize = 11.sp,
                color = SlateTextSecondary,
                fontFamily = FontFamily.Monospace
              )
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "₹${pay.finalAmountPaid.toInt()}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace
              )
              if (pay.status == "approved") {
                Text(
                  text = "Print Receipt",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = IndigoPrimary,
                  modifier = Modifier.clickable { selectedReceipt = pay }
                )
              } else {
                StatusBadge(status = pay.status)
              }
            }
          }
        }
      }
    }
  }

  selectedReceipt?.let { pay ->
    ReceiptDialog(
      payment = pay,
      centerName = centerInfo.name,
      onDismiss = { selectedReceipt = null }
    )
  }
}

@Composable
fun QuickActionButton(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
    modifier = modifier.clickable { onClick() }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(Color(0xFFEEF2FF)),
        contentAlignment = Alignment.Center
      ) {
        Icon(imageVector = icon, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(18.dp))
      }
      Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = SlateTextPrimary
      )
    }
  }
}
