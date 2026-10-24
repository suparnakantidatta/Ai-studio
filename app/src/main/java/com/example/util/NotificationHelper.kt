package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {

  const val CHANNEL_FEE_REMINDERS = "pixel_pathsala_fee_reminders"
  const val CHANNEL_NAME = "Fee Due Reminders"
  const val CHANNEL_DESC = "Notifications for student pending tuition fees & payment dues"

  const val CHANNEL_CLASS_ALERTS = "pixel_pathsala_class_alerts"
  const val CHANNEL_CLASS_NAME = "Class & Routine In-App Alarms"
  const val CHANNEL_CLASS_DESC = "Automated in-app reminders and alarms for scheduled classes"

  fun createNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

      val feeChannel = NotificationChannel(
        CHANNEL_FEE_REMINDERS,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = CHANNEL_DESC
        enableVibration(true)
        enableLights(true)
      }

      val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

      val classChannel = NotificationChannel(
        CHANNEL_CLASS_ALERTS,
        CHANNEL_CLASS_NAME,
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = CHANNEL_CLASS_DESC
        enableVibration(true)
        enableLights(true)
        setSound(alarmSound, null)
      }

      manager?.createNotificationChannel(feeChannel)
      manager?.createNotificationChannel(classChannel)
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
      createNotificationChannels(context)

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
      } catch (_: SecurityException) {}
    } catch (_: Exception) {}
  }

  fun sendClassAlarmSystemNotification(
    context: Context,
    title: String,
    subject: String,
    time: String,
    notificationId: Int = (System.currentTimeMillis() % 100000).toInt()
  ) {
    try {
      createNotificationChannels(context)

      val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("NAVIGATE_TO", "CLASSES")
      }

      val pendingIntent = PendingIntent.getActivity(
        context,
        notificationId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
      )

      val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

      val contentMsg = "Class is starting now ($time). Subject: $subject. Tap to attend session."

      val builder = NotificationCompat.Builder(context, CHANNEL_CLASS_ALERTS)
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle("🔔 Class Alarm: $title")
        .setContentText(contentMsg)
        .setStyle(NotificationCompat.BigTextStyle().bigText(contentMsg))
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setSound(alarmSound)
        .setVibrate(longArrayOf(0, 500, 250, 500, 250, 500))
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)

      val notificationManager = NotificationManagerCompat.from(context)
      try {
        notificationManager.notify(notificationId, builder.build())
      } catch (_: SecurityException) {}
    } catch (_: Exception) {}
  }

  fun playToastAndSound(context: Context, message: String) {
    try {
      val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
      val ringtone = RingtoneManager.getRingtone(context, notificationUri)
      ringtone?.play()
    } catch (_: Exception) {}
    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
  }
}
