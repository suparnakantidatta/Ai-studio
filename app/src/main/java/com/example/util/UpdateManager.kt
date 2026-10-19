package com.example.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast

object UpdateManager {
  const val GITHUB_UPDATE_URL = "https://github.com/suparnakantidatta/Ai-studio/releases/latest/download/Pixel_pathsala_1.0.0.1.apk"
  const val LATEST_VERSION = "1.0.0.1"
  const val CURRENT_VERSION = "1.0.0"
  const val APK_FILENAME = "Pixel_pathsala_1.0.0.1.apk"

  val RELEASE_CHANGELOG = listOf(
    "Enhanced Multi-Month Fee Collection Desk for Administrators",
    "Real-time Discount Rules deduction and automated receipt generation",
    "Seamless Google Sheets bidirectional synchronization",
    "Student Portal UPI reference entry and digital receipts viewer",
    "Optimized class schedule and exam submissions display"
  )

  fun isUpdateAvailable(): Boolean {
    // Current is 1.0.0, latest is 1.0.0.1
    return LATEST_VERSION != CURRENT_VERSION
  }

  fun downloadAndInstallUpdate(context: Context) {
    try {
      // 1. Queue with Android DownloadManager for notification & download
      val request = DownloadManager.Request(Uri.parse(GITHUB_UPDATE_URL)).apply {
        setTitle("Pixel Pathsala v$LATEST_VERSION Update")
        setDescription("Downloading latest APK from GitHub Releases...")
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, APK_FILENAME)
        setMimeType("application/vnd.android.package-archive")
        setAllowedOverMetered(true)
        setAllowedOverRoaming(true)
      }

      val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
      downloadManager?.enqueue(request)

      Toast.makeText(
        context,
        "Download started. Check notifications or Downloads folder to install.",
        Toast.LENGTH_LONG
      ).show()

      // 2. Also offer to open in browser for immediate download fallback
      val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_UPDATE_URL)).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      context.startActivity(browserIntent)
    } catch (_: Exception) {
      // Fallback: Open directly in browser
      try {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_UPDATE_URL)).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(browserIntent)
      } catch (e: Exception) {
        Toast.makeText(context, "Could not open download link: ${e.message}", Toast.LENGTH_SHORT).show()
      }
    }
  }
}
