package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

object ClassNotificationHelper {
  const val CHANNEL_CLASS_ALARM_ID = "channel_class_alarm"
  const val CHANNEL_POSTPONED_ALERT_ID = "channel_postponed_alert"
  const val CHANNEL_SCHEDULED_ID = "channel_scheduled_class"

  fun createNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

      val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

      val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

      // 1. Channel for Live Class On-Time Alarm
      val alarmChannel = NotificationChannel(
        CHANNEL_CLASS_ALARM_ID,
        "Live Class On-Time Alarms",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Ringing alarms and notifications when a scheduled live class starts"
        enableVibration(true)
        vibrationPattern = longArrayOf(0, 800, 400, 800, 400, 800)
        setSound(alarmSound, audioAttributes)
        lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
      }

      // 2. Channel for Postponed Class Alerts
      val postponeChannel = NotificationChannel(
        CHANNEL_POSTPONED_ALERT_ID,
        "Postponed Class Alerts",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "High-priority notifications and alerts when faculty postpones a scheduled class"
        enableVibration(true)
        vibrationPattern = longArrayOf(0, 500, 200, 500)
        setSound(alarmSound, audioAttributes)
        lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
      }

      // 3. Channel for Upcoming Class Reminders
      val scheduledChannel = NotificationChannel(
        CHANNEL_SCHEDULED_ID,
        "Scheduled Class Reminders",
        NotificationManager.IMPORTANCE_DEFAULT
      ).apply {
        description = "Advance reminders for upcoming scheduled live classes"
      }

      notificationManager.createNotificationChannels(listOf(alarmChannel, postponeChannel, scheduledChannel))
    }
  }

  /**
   * Shows a heads-up alarm notification when a scheduled class starts on time.
   */
  fun showLiveClassAlarmNotification(
    context: Context,
    sessionId: String,
    title: String,
    subject: String,
    educator: String,
    meetingUrl: String?
  ) {
    createNotificationChannels(context)
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notifId = (sessionId.hashCode() and 0x7FFFFFFF)

    // Stop Alarm action PendingIntent
    val dismissIntent = Intent(context, ClassAlarmReceiver::class.java).apply {
      action = ClassAlarmReceiver.ACTION_DISMISS_ALARM
      putExtra(ClassAlarmReceiver.EXTRA_SESSION_ID, sessionId)
      putExtra(ClassAlarmReceiver.EXTRA_NOTIFICATION_ID, notifId)
    }
    val dismissPendingIntent = PendingIntent.getBroadcast(
      context,
      notifId + 10,
      dismissIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Join Class / Open App PendingIntent
    val openIntent = if (!meetingUrl.isNullOrBlank()) {
      Intent(Intent.ACTION_VIEW, Uri.parse(meetingUrl)).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
    } else {
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
    }
    val openPendingIntent = PendingIntent.getActivity(
      context,
      notifId + 20,
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_CLASS_ALARM_ID)
      .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
      .setContentTitle("⏰ LIVE CLASS ALARM: $title")
      .setContentText("$subject • Faculty: $educator is starting now!")
      .setStyle(
        NotificationCompat.BigTextStyle()
          .bigText("⏰ Your scheduled class '$title' is starting right now!\nSubject: $subject\nFaculty: $educator\nTap 'Join Class' to enter meeting.")
      )
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_ALARM)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setAutoCancel(true)
      .setContentIntent(openPendingIntent)
      .addAction(android.R.drawable.ic_media_play, "Join Class", openPendingIntent)
      .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Alarm", dismissPendingIntent)
      .setFullScreenIntent(openPendingIntent, true)
      .build()

    notificationManager.notify(notifId, notification)
  }

  /**
   * Shows a high-priority alert notification when a class has been postponed.
   */
  fun showClassPostponedNotification(
    context: Context,
    sessionId: String,
    title: String,
    reason: String?,
    rescheduledDate: String?,
    rescheduledTime: String?
  ) {
    createNotificationChannels(context)
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notifId = ((sessionId + "_postponed").hashCode() and 0x7FFFFFFF)

    val openIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val openPendingIntent = PendingIntent.getActivity(
      context,
      notifId + 30,
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val reasonText = reason?.ifBlank { null } ?: "Postponed by Faculty"
    val rescheduleText = if (!rescheduledDate.isNullOrBlank()) {
      "\nRescheduled to: $rescheduledDate ${rescheduledTime ?: ""}"
    } else ""

    val notification = NotificationCompat.Builder(context, CHANNEL_POSTPONED_ALERT_ID)
      .setSmallIcon(android.R.drawable.ic_dialog_alert)
      .setContentTitle("⚠️ CLASS POSTPONED: $title")
      .setContentText("$reasonText$rescheduleText")
      .setStyle(
        NotificationCompat.BigTextStyle()
          .bigText("⚠️ NOTICE: The scheduled live class '$title' has been postponed.\nReason: $reasonText$rescheduleText\nPlease check your updated student timetable.")
      )
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_REMINDER)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setAutoCancel(true)
      .setContentIntent(openPendingIntent)
      .build()

    notificationManager.notify(notifId, notification)
  }

  fun dismissNotification(context: Context, notificationId: Int) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancel(notificationId)
  }
}
