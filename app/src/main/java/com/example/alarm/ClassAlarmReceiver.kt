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
    const val EXTRA_SESSION_ACADEMIC_CLASS = "extra_session_academic_class"
    const val EXTRA_SESSION_SUBJECT = "extra_session_subject"
    const val EXTRA_EDUCATOR_NAME = "extra_educator_name"
    const val EXTRA_SCHEDULED_DATE = "extra_scheduled_date"
    const val EXTRA_START_TIME = "extra_start_time"
    const val EXTRA_END_TIME = "extra_end_time"
    const val EXTRA_PLATFORM = "extra_platform"
    const val EXTRA_MEETING_URL = "extra_meeting_url"
    const val EXTRA_BATCH_ID = "extra_batch_id"
    const val EXTRA_COURSE_ID = "extra_course_id"
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
        val academicClass = intent.getStringExtra(EXTRA_SESSION_ACADEMIC_CLASS) ?: "Class 12"
        val subject = intent.getStringExtra(EXTRA_SESSION_SUBJECT) ?: "Academic Subject"
        val educator = intent.getStringExtra(EXTRA_EDUCATOR_NAME) ?: "Faculty Desk"
        val scheduledDate = intent.getStringExtra(EXTRA_SCHEDULED_DATE)
        val startTime = intent.getStringExtra(EXTRA_START_TIME)
        val endTime = intent.getStringExtra(EXTRA_END_TIME)
        val platform = intent.getStringExtra(EXTRA_PLATFORM)
        val meetingUrl = intent.getStringExtra(EXTRA_MEETING_URL)
        val batchId = intent.getStringExtra(EXTRA_BATCH_ID)

        // 1. Play ringing alarm & vibration with descriptive title
        val alarmTitle = "$title ($academicClass) • ${startTime ?: ""}"
        ClassAlarmAudioPlayer.playAlarm(context, alarmTitle)

        // 2. Display high-priority heads-up notification with action to join or stop
        ClassNotificationHelper.showLiveClassAlarmNotification(
          context = context,
          sessionId = sessionId,
          title = title,
          academicClass = academicClass,
          subject = subject,
          educator = educator,
          scheduledDate = scheduledDate,
          startTime = startTime,
          endTime = endTime,
          platform = platform,
          meetingUrl = meetingUrl,
          batchName = batchId
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
