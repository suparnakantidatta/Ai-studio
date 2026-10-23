package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.example.data.model.Batch
import com.example.data.model.LiveClassSession
import java.util.Calendar

object InAppAlarmScheduler {

  fun scheduleClassAlarm(context: Context, session: LiveClassSession, showToast: Boolean = false) {
    try {
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
      val timeParts = parseTimeString(session.startTime)

      val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, timeParts.first)
        set(Calendar.MINUTE, timeParts.second)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= System.currentTimeMillis()) {
          add(Calendar.DAY_OF_YEAR, 1)
        }
      }

      val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
        putExtra("title", session.title)
        putExtra("subject", session.subject)
        putExtra("time", session.startTime)
      }

      val requestCode = (session.id.hashCode() and 0x7FFFFFFF)
      val pendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
      )

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
      } else {
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
      }

      if (showToast) {
        Toast.makeText(context, "Class alarm set for ${session.startTime} in Pathsala ERP", Toast.LENGTH_SHORT).show()
      }
    } catch (_: Exception) {}
  }

  fun scheduleBatchAlarm(context: Context, batch: Batch, showToast: Boolean = false) {
    try {
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
      val timeParts = parseTimeString(batch.timings)

      val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, timeParts.first)
        set(Calendar.MINUTE, timeParts.second)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= System.currentTimeMillis()) {
          add(Calendar.DAY_OF_YEAR, 1)
        }
      }

      val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
        putExtra("title", "Batch Routine: ${batch.name}")
        putExtra("subject", batch.scheduleDays)
        putExtra("time", batch.timings)
      }

      val requestCode = (("batch_" + batch.id).hashCode() and 0x7FFFFFFF)
      val pendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
      )

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
      } else {
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
      }

      if (showToast) {
        Toast.makeText(context, "Routine reminder set for ${batch.name} (${batch.timings}) in Pathsala app", Toast.LENGTH_SHORT).show()
      }
    } catch (_: Exception) {}
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
