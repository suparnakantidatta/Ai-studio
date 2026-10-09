package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.LiveClassSession
import java.util.Calendar

object ClassAlarmManager {
  private const val PREFS_NAME = "live_class_alarms_prefs"
  private const val KEY_PREFIX_ALARM = "alarm_enabled_"
  private const val KEY_PREFIX_POSTPONED_NOTIFIED = "notified_postponed_"

  /**
   * Robust parser for class date ("2026-10-10", "10-10-2026", "10/10/2026") and time ("11:00", "09:00 AM", "13:00", "07:00 PM").
   */
  fun parseClassDateTime(dateStr: String, timeStr: String): Calendar? {
    try {
      val cal = Calendar.getInstance()
      val cleanDate = dateStr.trim().replace("\"", "").replace("'", "")
      val dateParts = cleanDate.split("-", "/", "T", " ")
      if (dateParts.size >= 3) {
        val p0 = dateParts[0].toIntOrNull() ?: return null
        val p1 = dateParts[1].toIntOrNull() ?: return null
        val p2 = dateParts[2].take(4).toIntOrNull() ?: return null

        val y: Int
        val m: Int
        val d: Int
        if (p0 >= 2000) {
          // YYYY-MM-DD
          y = p0
          m = p1
          d = p2
        } else if (p2 >= 2000) {
          // DD-MM-YYYY or MM-DD-YYYY
          y = p2
          if (p0 > 12) {
            d = p0
            m = p1
          } else if (p1 > 12) {
            d = p1
            m = p0
          } else {
            // Standard Indian / UK format DD-MM-YYYY
            d = p0
            m = p1
          }
        } else {
          y = cal.get(Calendar.YEAR)
          m = p1
          d = p0
        }

        cal.set(Calendar.YEAR, y)
        cal.set(Calendar.MONTH, (m - 1).coerceIn(0, 11))
        cal.set(Calendar.DAY_OF_MONTH, d.coerceIn(1, 31))
      } else {
        return null
      }

      val cleanTime = timeStr.trim().uppercase()
      val isPm = cleanTime.contains("PM")
      val isAm = cleanTime.contains("AM")
      val digitsPart = cleanTime.replace("AM", "").replace("PM", "").trim()
      val timeParts = digitsPart.split(":")
      if (timeParts.isNotEmpty()) {
        var hour = timeParts[0].toIntOrNull() ?: 9
        val minute = if (timeParts.size > 1) timeParts[1].toIntOrNull() ?: 0 else 0
        if (isPm && hour < 12) hour += 12
        if (isAm && hour == 12) hour = 0
        cal.set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        cal.set(Calendar.MINUTE, minute.coerceIn(0, 59))
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return null
  }

  /**
   * Sets an exact alarm for the live class session as scheduled in the database.
   */
  fun setAlarmForClass(context: Context, session: LiveClassSession): Boolean {
    val cal = parseClassDateTime(session.scheduledDate, session.startTime) ?: return false
    val triggerMillis = cal.timeInMillis
    val now = System.currentTimeMillis()

    // If class was scheduled in the past (ended more than 10 mins ago), do NOT trigger alarm
    if (triggerMillis < now - 10 * 60 * 1000L) {
      saveAlarmState(context, session.id, false)
      return false
    }

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
    val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
      action = ClassAlarmReceiver.ACTION_TRIGGER_CLASS_ALARM
      putExtra(ClassAlarmReceiver.EXTRA_SESSION_ID, session.id)
      putExtra(ClassAlarmReceiver.EXTRA_SESSION_TITLE, session.title)
      putExtra(ClassAlarmReceiver.EXTRA_SESSION_ACADEMIC_CLASS, session.academicClass ?: session.targetClass ?: "Class 12")
      putExtra(ClassAlarmReceiver.EXTRA_SESSION_SUBJECT, session.subject)
      putExtra(ClassAlarmReceiver.EXTRA_EDUCATOR_NAME, session.educatorName)
      putExtra(ClassAlarmReceiver.EXTRA_SCHEDULED_DATE, session.scheduledDate)
      putExtra(ClassAlarmReceiver.EXTRA_START_TIME, session.startTime)
      putExtra(ClassAlarmReceiver.EXTRA_END_TIME, session.endTime)
      putExtra(ClassAlarmReceiver.EXTRA_PLATFORM, session.platform)
      putExtra(ClassAlarmReceiver.EXTRA_MEETING_URL, session.meetingUrl)
      putExtra(ClassAlarmReceiver.EXTRA_BATCH_ID, session.batchId)
      putExtra(ClassAlarmReceiver.EXTRA_COURSE_ID, session.courseId)
    }

    val requestCode = (session.id.hashCode() and 0x7FFFFFFF)
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    try {
      if (triggerMillis > now) {
        // Schedule exact alarm for when class starts as per database timetable
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } else {
          alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
      } else {
        // Class is right now (within active 10-minute live window)
        triggerTestAlarm(context, session)
      }

      saveAlarmState(context, session.id, true)
      return true
    } catch (e: Exception) {
      e.printStackTrace()
      return false
    }
  }

  /**
   * Cancels the alarm for the session.
   */
  fun cancelAlarmForClass(context: Context, session: LiveClassSession) {
    try {
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
      val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
        action = ClassAlarmReceiver.ACTION_TRIGGER_CLASS_ALARM
      }
      val requestCode = (session.id.hashCode() and 0x7FFFFFFF)
      val pendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      alarmManager?.cancel(pendingIntent)
      saveAlarmState(context, session.id, false)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  /**
   * Checks whether the user has toggled alarm ON for this session.
   */
  fun isAlarmSet(context: Context, sessionId: String): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_PREFIX_ALARM + sessionId, false)
  }

  fun saveAlarmState(context: Context, sessionId: String, enabled: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_PREFIX_ALARM + sessionId, enabled).apply()
  }

  /**
   * Immediately triggers alarm (sound, vibration, notification) for live testing or on-time trigger.
   */
  fun triggerTestAlarm(context: Context, session: LiveClassSession) {
    val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
      action = ClassAlarmReceiver.ACTION_TRIGGER_CLASS_ALARM
      putExtra(ClassAlarmReceiver.EXTRA_SESSION_ID, session.id)
      putExtra(ClassAlarmReceiver.EXTRA_SESSION_TITLE, session.title)
      putExtra(ClassAlarmReceiver.EXTRA_SESSION_ACADEMIC_CLASS, session.academicClass ?: session.targetClass ?: "Class 12")
      putExtra(ClassAlarmReceiver.EXTRA_SESSION_SUBJECT, session.subject)
      putExtra(ClassAlarmReceiver.EXTRA_EDUCATOR_NAME, session.educatorName)
      putExtra(ClassAlarmReceiver.EXTRA_SCHEDULED_DATE, session.scheduledDate)
      putExtra(ClassAlarmReceiver.EXTRA_START_TIME, session.startTime)
      putExtra(ClassAlarmReceiver.EXTRA_END_TIME, session.endTime)
      putExtra(ClassAlarmReceiver.EXTRA_PLATFORM, session.platform)
      putExtra(ClassAlarmReceiver.EXTRA_MEETING_URL, session.meetingUrl)
      putExtra(ClassAlarmReceiver.EXTRA_BATCH_ID, session.batchId)
      putExtra(ClassAlarmReceiver.EXTRA_COURSE_ID, session.courseId)
    }
    context.sendBroadcast(intent)
  }

  /**
   * Triggers alert notification & sound for a postponed class.
   */
  fun triggerPostponedAlert(context: Context, session: LiveClassSession) {
    val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
      action = ClassAlarmReceiver.ACTION_TRIGGER_POSTPONED_ALERT
      putExtra(ClassAlarmReceiver.EXTRA_SESSION_ID, session.id)
      putExtra(ClassAlarmReceiver.EXTRA_SESSION_TITLE, session.title)
      putExtra(ClassAlarmReceiver.EXTRA_POSTPONE_REASON, session.postponeReason ?: "Faculty has postponed this class.")
      putExtra(ClassAlarmReceiver.EXTRA_RESCHEDULED_DATE, session.rescheduledDate)
      putExtra(ClassAlarmReceiver.EXTRA_RESCHEDULED_TIME, session.rescheduledTime)
    }
    context.sendBroadcast(intent)
  }

  /**
   * Detects newly postponed classes and triggers notification & alarm sound.
   */
  fun checkAndNotifyPostponedClasses(context: Context, sessions: List<LiveClassSession>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val postponed = sessions.filter { it.status.equals("postponed", ignoreCase = true) }

    for (s in postponed) {
      val key = KEY_PREFIX_POSTPONED_NOTIFIED + s.id
      val alreadyNotified = prefs.getBoolean(key, false)
      if (!alreadyNotified) {
        triggerPostponedAlert(context, s)
        prefs.edit().putBoolean(key, true).apply()
      }
    }
  }

  /**
   * Automatically sets alarms for upcoming scheduled classes.
   */
  fun autoScheduleAlarmsForStudent(context: Context, sessions: List<LiveClassSession>) {
    val scheduled = sessions.filter { it.status.equals("scheduled", ignoreCase = true) }
    for (s in scheduled) {
      val isAlreadyExplicitlyDisabled = !isAlarmSet(context, s.id) && context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).contains(KEY_PREFIX_ALARM + s.id)
      if (!isAlreadyExplicitlyDisabled) {
        setAlarmForClass(context, s)
      }
    }
  }
}
