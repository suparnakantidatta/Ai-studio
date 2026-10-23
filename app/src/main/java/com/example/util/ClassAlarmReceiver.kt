package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ClassAlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val title = intent.getStringExtra("title") ?: "Pixel Pathsala Class Alert"
    val subject = intent.getStringExtra("subject") ?: "Scheduled Routine"
    val time = intent.getStringExtra("time") ?: ""

    NotificationHelper.sendClassAlarmSystemNotification(context, title, subject, time)
  }
}
