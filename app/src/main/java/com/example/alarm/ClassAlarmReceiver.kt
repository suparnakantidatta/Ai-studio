package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ClassAlarmReceiver : BroadcastReceiver() {

  companion object {
    const val ACTION_TRIGGER_CLASS_ALARM = "com.example.alarm.ACTION_TRIGGER_CLASS_ALARM"
    const val ACTION_DISMISS_ALARM = "com.example.alarm.ACTION_DISMISS_ALARM"
    const val ACTION_TRIGGER_POSTPONED_ALERT = "com.example.alarm.ACTION_TRIGGER_POSTPONED_ALERT"

    const val EXTRA_SESSION_ID = "extra_session_id"
    const val EXTRA_SESSION_TITLE = "extra_session_title"
    const val EXTRA_SESSION_SUBJECT = "extra_session_subject"
    const val EXTRA_EDUCATOR_NAME = "extra_educator_name"
    const val EXTRA_MEETING_URL = "extra_meeting_url"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_POSTPONE_REASON = "extra_postpone_reason"
    const val EXTRA_RESCHEDULED_DATE = "extra_rescheduled_date"
    const val EXTRA_RESCHEDULED_TIME = "extra_rescheduled_time"
  }

  override fun onReceive(context: Context, intent: Intent?) {
    if (intent == null) return

    when (intent.action) {
      ACTION_TRIGGER_CLASS_ALARM -> {
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: "session-1"
        val title = intent.getStringExtra(EXTRA_SESSION_TITLE) ?: "Scheduled Live Class"
        val subject = intent.getStringExtra(EXTRA_SESSION_SUBJECT) ?: "Academic Subject"
        val educator = intent.getStringExtra(EXTRA_EDUCATOR_NAME) ?: "Faculty Desk"
        val meetingUrl = intent.getStringExtra(EXTRA_MEETING_URL)

        // 1. Play ringing alarm & vibration
        ClassAlarmAudioPlayer.playAlarm(context, title)

        // 2. Display high-priority heads-up notification with action to join or stop
        ClassNotificationHelper.showLiveClassAlarmNotification(
          context = context,
          sessionId = sessionId,
          title = title,
          subject = subject,
          educator = educator,
          meetingUrl = meetingUrl
        )
      }

      ACTION_DISMISS_ALARM -> {
        // Stop the ringing alarm
        ClassAlarmAudioPlayer.stopAlarm(context)
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (notifId != -1) {
          ClassNotificationHelper.dismissNotification(context, notifId)
        }
      }

      ACTION_TRIGGER_POSTPONED_ALERT -> {
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: "session-1"
        val title = intent.getStringExtra(EXTRA_SESSION_TITLE) ?: "Live Class"
        val reason = intent.getStringExtra(EXTRA_POSTPONE_REASON)
        val reschedDate = intent.getStringExtra(EXTRA_RESCHEDULED_DATE)
        val reschedTime = intent.getStringExtra(EXTRA_RESCHEDULED_TIME)

        // 1. Play alert sound & vibration
        ClassAlarmAudioPlayer.playPostponeAlert(context, title)

        // 2. Display postponed class notification
        ClassNotificationHelper.showClassPostponedNotification(
          context = context,
          sessionId = sessionId,
          title = title,
          reason = reason,
          rescheduledDate = reschedDate,
          rescheduledTime = reschedTime
        )
      }
    }
  }
}
