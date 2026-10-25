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

  const val PERSISTENT_CLASS_NOTIF_ID = 8801

  fun showPersistentClassNotification(
    context: Context,
    title: String,
    scheduledDate: String,
    startTime: String,
    subject: String,
    batchName: String?,
    isPostponed: Boolean,
    postponeReason: String? = null
  ) {
    try {
      createNotificationChannels(context)

      val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("NAVIGATE_TO", "CLASSES")
      }

      val pendingIntent = PendingIntent.getActivity(
        context,
        PERSISTENT_CLASS_NOTIF_ID,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
      )

      val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

      val notifTitle = if (isPostponed) "⚠️ Class Postponed: $title" else "📅 Class Scheduled: $title"
      val notifBody = if (isPostponed) {
        "Notice: ${postponeReason ?: "Session postponed by center faculty"}. Timings: $startTime. Tap for updates."
      } else {
        "Class active on $scheduledDate at $startTime ($subject - ${batchName ?: "Regular"}). Tap to open."
      }

      val builder = NotificationCompat.Builder(context, CHANNEL_CLASS_ALERTS)
        .setSmallIcon(if (isPostponed) android.R.drawable.stat_notify_error else android.R.drawable.ic_dialog_info)
        .setContentTitle(notifTitle)
        .setContentText(notifBody)
        .setStyle(NotificationCompat.BigTextStyle().bigText(notifBody))
        .setColor(if (isPostponed) 0xFFDC2626.toInt() else 0xFF16A34A.toInt())
        .setColorized(true)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setShowWhen(true)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setOngoing(true)
        .setAutoCancel(false)
        .setContentIntent(pendingIntent)

      try {
        val ringtone = RingtoneManager.getRingtone(context, alarmSound)
        ringtone?.play()
      } catch (_: Exception) {}

      val notificationManager = NotificationManagerCompat.from(context)
      try {
        notificationManager.notify(PERSISTENT_CLASS_NOTIF_ID, builder.build())
      } catch (_: SecurityException) {}
    } catch (_: Exception) {}
  }

  fun syncStudentClassNotification(
    context: Context,
    student: com.example.data.model.Student,
    liveClasses: List<com.example.data.model.LiveClassSession>,
    batches: List<com.example.data.model.Batch>
  ) {
    val studentBatch = batches.firstOrNull { it.id == student.batchId }
    val relevantClasses = liveClasses.filter { session ->
      val sessBatch = session.batchId?.trim() ?: ""
      val sessCourse = session.courseId?.trim() ?: ""
      when {
        sessBatch.isNotEmpty() && !sessBatch.equals("all", ignoreCase = true) -> sessBatch == student.batchId
        sessBatch.equals("all", ignoreCase = true) -> sessCourse.isEmpty() || sessCourse == student.courseId
        sessCourse.isNotEmpty() -> sessCourse == student.courseId
        else -> true
      }
    }

    val targetClass = relevantClasses.firstOrNull { it.isPostponed || it.status.equals("postponed", ignoreCase = true) }
      ?: relevantClasses.firstOrNull { it.status.equals("scheduled", ignoreCase = true) || it.status.equals("live", ignoreCase = true) }
      ?: liveClasses.firstOrNull { it.isPostponed || it.status.equals("postponed", ignoreCase = true) }
      ?: liveClasses.firstOrNull { it.status.equals("scheduled", ignoreCase = true) || it.status.equals("live", ignoreCase = true) }

    if (targetClass != null) {
      val isPostponed = targetClass.isPostponed || targetClass.status.equals("postponed", ignoreCase = true)
      showPersistentClassNotification(
        context = context,
        title = targetClass.title,
        scheduledDate = targetClass.scheduledDate,
        startTime = targetClass.startTime,
        subject = targetClass.subject,
        batchName = studentBatch?.name ?: targetClass.academicClass,
        isPostponed = isPostponed,
        postponeReason = targetClass.postponeReason
      )
    } else {
      showPersistentClassNotification(
        context = context,
        title = "Batch: ${studentBatch?.name ?: "Regular Academic"}",
        scheduledDate = studentBatch?.scheduleDays ?: "Regular Timetable",
        startTime = studentBatch?.timings ?: "10:00 AM",
        subject = "Pixel Pathsala Academic Portal",
        batchName = studentBatch?.name,
        isPostponed = false
      )
    }
  }

  fun cancelPersistentClassNotification(context: Context) {
    try {
      val notificationManager = NotificationManagerCompat.from(context)
      notificationManager.cancel(PERSISTENT_CLASS_NOTIF_ID)
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
