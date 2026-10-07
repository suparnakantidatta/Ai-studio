package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.LocalDataStore

class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
      try {
        val localStore = LocalDataStore(context)
        val sessions = localStore.getLiveClasses().filter { it.status.equals("scheduled", ignoreCase = true) }
        for (session in sessions) {
          if (ClassAlarmManager.isAlarmSet(context, session.id)) {
            ClassAlarmManager.setAlarmForClass(context, session)
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }
}
