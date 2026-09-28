package com.example.ui.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import com.example.data.api.ApiClient
import com.example.data.repository.PathsalaRepository
import com.example.ui.theme.*

@Composable
fun AdminSettingsDialog(
  repository: PathsalaRepository,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val currentServerUrl by repository.serverUrl.collectAsState()
  val connectionStatus by repository.connectionStatus.collectAsState()
  val lastSyncTime by repository.lastSyncTime.collectAsState()
  val isSyncing by repository.isSyncing.collectAsState()
  val centerInfo by repository.centerInfo.collectAsState()

  var inputServerUrl by remember { mutableStateOf(currentServerUrl) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp)
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
            Icon(Icons.Default.CloudSync, contentDescription = null, tint = IndigoPrimary)
            Text(
              text = "Backend Server & Sync",
              fontWeight = FontWeight.Black,
              fontSize = 17.sp,
              color = Color(0xFF0F172A)
            )
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        HorizontalDivider(color = SlateBorder)

        // Status Card
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF8FAFC))
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Connection Status", fontSize = 11.sp, color = SlateTextSecondary)
            Text(connectionStatus, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (connectionStatus.contains("Synced")) EmeraldSuccess else Color(0xFFDC2626))
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Last Synced", fontSize = 11.sp, color = SlateTextSecondary)
            Text(lastSyncTime ?: "Never", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Official UPI ID", fontSize = 11.sp, color = SlateTextSecondary)
            Text(centerInfo.upiId, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = IndigoPrimary)
          }
        }

        // Server URL input
        Text(
          text = "Backend API Endpoint URL:",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = SlateTextPrimary
        )

        OutlinedTextField(
          value = inputServerUrl,
          onValueChange = { inputServerUrl = it },
          label = { Text("Server URL") },
          placeholder = { Text(ApiClient.DEFAULT_BASE_URL) },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              inputServerUrl = ApiClient.DEFAULT_BASE_URL
              repository.setServerUrl(ApiClient.DEFAULT_BASE_URL)
              Toast.makeText(context, "Reset to default Cloud Run backend URL", Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text("Default URL", fontSize = 11.sp)
          }

          Button(
            onClick = {
              if (inputServerUrl.isNotBlank()) {
                repository.setServerUrl(inputServerUrl)
                Toast.makeText(context, "Updated Server URL & Syncing...", Toast.LENGTH_SHORT).show()
              }
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
            modifier = Modifier.weight(1f)
          ) {
            Text("Save & Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }

        Button(
          onClick = {
            repository.syncWithBackend()
            Toast.makeText(context, "Full two-way sync initiated...", Toast.LENGTH_SHORT).show()
          },
          enabled = !isSyncing,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
          modifier = Modifier.fillMaxWidth()
        ) {
          if (isSyncing) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Syncing with Cloud Database...", fontSize = 12.sp)
          } else {
            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Trigger Cloud Sync Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
