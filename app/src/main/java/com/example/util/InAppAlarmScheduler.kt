package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.example.data.model.LiveClassSession
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object InAppAlarmScheduler {

  fun scheduleClassAlarm(context: Context, session: LiveClassSession, showToast: Boolean = false) {
    try {
      if (session.isPostponed || session.status.equals("postponed", ignoreCase = true)) {
        cancelClassAlarm(context, session.id)
        return
      }

      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

      val classStartTimeMillis = computeClassStartTimeMillis(session.scheduledDate, session.startTime)
      val now = System.currentTimeMillis()

      // Class already finished in the past
      if (classStartTimeMillis <= now) {
        return
      }

      // Ring ONLY 30 minutes before class start
      val targetAlarmMillis = classStartTimeMillis - 30 * 60 * 1000L

      val triggerMillis = if (targetAlarmMillis > now) {
        targetAlarmMillis
      } else {
        // Class is starting in less than 30 minutes: alert promptly in 3 seconds
        now + 3000L
      }

      val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
        putExtra("title", session.title)
        putExtra("subject", session.subject)
        putExtra("time", session.startTime)
        putExtra("sessionId", session.id)
      }

      val requestCode = (session.id.hashCode() and 0x7FFFFFFF)
      val pendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
      )

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
      } else {
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
      }

      if (showToast) {
        val minDiff = ((classStartTimeMillis - now) / 60000).toInt()
        Toast.makeText(context, "Alarm set: Rings 30 min before class (${session.startTime})", Toast.LENGTH_SHORT).show()
      }
    } catch (_: Exception) {}
  }

  fun cancelClassAlarm(context: Context, sessionId: String) {
    try {
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
      val intent = Intent(context, ClassAlarmReceiver::class.java)
      val requestCode = (sessionId.hashCode() and 0x7FFFFFFF)
      val pendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
      )
      alarmManager.cancel(pendingIntent)
    } catch (_: Exception) {}
  }

  private fun computeClassStartTimeMillis(dateStr: String, timeStr: String): Long {
    val cal = Calendar.getInstance()
    val timeParts = parseTimeString(timeStr)

    // Parse date if provided
    var dateSet = false
    val trimmedDate = dateStr.trim()
    if (trimmedDate.isNotEmpty()) {
      val cleanDate = if (trimmedDate.contains("T")) trimmedDate.substringBefore("T") else trimmedDate
      val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
        SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
      )
      for (df in dateFormats) {
        try {
          val parsed = df.parse(cleanDate)
          if (parsed != null) {
            val dateCal = Calendar.getInstance().apply { time = parsed }
            cal.set(Calendar.YEAR, dateCal.get(Calendar.YEAR))
            cal.set(Calendar.MONTH, dateCal.get(Calendar.MONTH))
            cal.set(Calendar.DAY_OF_MONTH, dateCal.get(Calendar.DAY_OF_MONTH))
            dateSet = true
            break
          }
        } catch (_: Exception) {}
      }
    }

    cal.set(Calendar.HOUR_OF_DAY, timeParts.first)
    cal.set(Calendar.MINUTE, timeParts.second)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)

    // If no specific calendar date was given and time today has already elapsed, assume next day session
    if (!dateSet && cal.timeInMillis <= System.currentTimeMillis()) {
      cal.add(Calendar.DAY_OF_YEAR, 1)
    }

    return cal.timeInMillis
  }

  private fun parseTimeString(timeStr: String): Pair<Int, Int> {
    var hour = 9
    var minute = 0
    val isPm = timeStr.contains("PM", ignoreCase = true)
    val isAm = timeStr.contains("AM", ignoreCase = true)
    val clean = timeStr.replace("(?i)am|pm".toRegex(), "").trim()
    val parts = clean.split(":", " ")
    if (parts.isNotEmpty()) {
      hour = parts[0].trim().toIntOrNull() ?: 9
      if (isPm && hour < 12) hour += 12
      if (isAm && hour == 12) hour = 0
    }
    if (parts.size > 1) {
      minute = parts[1].trim().toIntOrNull() ?: 0
    }
    return Pair(hour, minute)
  }
}
