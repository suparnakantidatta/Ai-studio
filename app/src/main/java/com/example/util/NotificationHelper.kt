package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {

  const val CHANNEL_FEE_REMINDERS = "pixel_pathsala_fee_reminders"
  const val CHANNEL_NAME = "Fee Due Reminders"
  const val CHANNEL_DESC = "Notifications for student pending tuition fees & payment dues"

  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_FEE_REMINDERS,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = CHANNEL_DESC
        enableVibration(true)
        enableLights(true)
      }
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      manager?.createNotificationChannel(channel)
    }
  }

  fun sendFeeDueSystemNotification(
    context: Context,
    studentName: String,
    dueAmount: Double,
    message: String,
    notificationId: Int = (System.currentTimeMillis() % 100000).toInt()
  ) {
    try {
      createNotificationChannel(context)

      val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("NAVIGATE_TO", "FEES")
      }

      val pendingIntent = PendingIntent.getActivity(
        context,
        notificationId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
      )

      val builder = NotificationCompat.Builder(context, CHANNEL_FEE_REMINDERS)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Fee Due Reminder: ₹${dueAmount.toInt()} Pending")
        .setContentText(message)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)

      val notificationManager = NotificationManagerCompat.from(context)
      try {
        notificationManager.notify(notificationId, builder.build())
      } catch (_: SecurityException) {
        // Ignored if POST_NOTIFICATIONS runtime permission not granted yet on Android 13+
      }
    } catch (_: Exception) {}
  }
}
