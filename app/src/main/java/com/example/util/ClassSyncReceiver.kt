package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.LocalDataStore
import com.example.data.sheet.GoogleSheetSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ClassSyncReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val sheetService = GoogleSheetSyncService()
        val localStore = LocalDataStore(context)
        val student = localStore.getLoggedInStudent()

        val result = sheetService.fetchAllData()
        result.onSuccess { dto ->
          localStore.saveAll(dto)

          if (student != null) {
            val liveClasses = dto.liveClasses ?: emptyList()
            val batches = dto.batches ?: emptyList()
            val courses = dto.courses ?: emptyList()

            // Post notifications for any scheduled or postponed class
            NotificationHelper.syncStudentClassNotification(
              context = context,
              student = student,
              liveClasses = liveClasses,
              batches = batches,
              courses = courses
            )

            // Setup or cancel 30-min alarms for student's relevant classes
            liveClasses.forEach { session ->
              val isRelevant = BatchScheduleHelper.isClassSessionForStudent(
                session = session,
                student = student,
                courses = courses,
                batches = batches
              )
              if (isRelevant) {
                if (session.isPostponed || session.status.equals("postponed", ignoreCase = true)) {
                  InAppAlarmScheduler.cancelClassAlarm(context, session.id)
                } else if (session.status.equals("scheduled", ignoreCase = true)) {
                  InAppAlarmScheduler.scheduleClassAlarm(context, session, showToast = false)
                }
              }
            }
          }
        }
      } catch (_: Exception) {
      } finally {
        // Schedule next sync in 15 minutes to guarantee background updates when closed
        InAppAlarmScheduler.scheduleNextSyncAlarm(context)
        pendingResult.finish()
      }
    }
  }
}
