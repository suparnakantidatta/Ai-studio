package com.example.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.UpdateManager

@Composable
fun AppUpdateDialog(
  onDismiss: () -> Unit
) {
  val context = LocalContext.current

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(20.dp),
    containerColor = Color.White,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .background(BrandGoldLight, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.SystemUpdate,
            contentDescription = "Update",
            tint = BrandGoldDark,
            modifier = Modifier.size(24.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(text = "App Update Available", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate900)
          Text(text = "Pixel Pathsala v${UpdateManager.LATEST_VERSION}", fontSize = 12.sp, color = BrandPrimary, fontWeight = FontWeight.SemiBold)
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Version Comparison Card
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Slate100,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(text = "Installed Version", fontSize = 11.sp, color = Slate600)
              Text(text = "v${UpdateManager.CURRENT_VERSION}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate800)
            }
            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
            Column(horizontalAlignment = Alignment.End) {
              Text(text = "Latest Release", fontSize = 11.sp, color = BrandGoldDark)
              Text(text = "v${UpdateManager.LATEST_VERSION}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
            }
          }
        }

        Text(text = "What's New in This Version:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)

        UpdateManager.RELEASE_CHANGELOG.forEach { note ->
          Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(vertical = 2.dp)
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = SuccessGreen,
              modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = note, fontSize = 12.sp, color = Slate700, lineHeight = 16.sp)
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Direct GitHub link info
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              clipboard.setPrimaryClip(ClipData.newPlainText("Update URL", UpdateManager.GITHUB_UPDATE_URL))
              Toast.makeText(context, "GitHub APK link copied to clipboard", Toast.LENGTH_SHORT).show()
            }
            .padding(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = "Tap to copy GitHub release download link", fontSize = 11.sp, color = BrandPrimary)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          UpdateManager.downloadAndInstallUpdate(context)
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("confirm_download_update_button")
      ) {
        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Update Now (APK)", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Later", color = Slate600)
      }
    }
  )
}

@Composable
fun AppUpdateBanner(
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = BrandGoldLight),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("app_update_banner")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .background(BrandGold, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = BrandSecondary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(text = "Update Available: v${UpdateManager.LATEST_VERSION}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandGoldDark)
          Text(text = "Tap to download new features from GitHub", fontSize = 11.sp, color = Slate700)
        }
      }

      Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = BrandGoldDark),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
      ) {
        Text("Update", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
      }
    }
  }
}
