package com.example.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ClassAlarmAudioPlayer {
  private var activeRingtone: Ringtone? = null
  private val _isRinging = MutableStateFlow(false)
  val isRinging: StateFlow<Boolean> = _isRinging.asStateFlow()

  private val _activeAlarmSessionTitle = MutableStateFlow<String?>(null)
  val activeAlarmSessionTitle: StateFlow<String?> = _activeAlarmSessionTitle.asStateFlow()

  /**
   * Plays alarm ringtone and triggers vibration pattern.
   */
  fun playAlarm(context: Context, sessionTitle: String? = null) {
    stopAlarm(context)
    _activeAlarmSessionTitle.value = sessionTitle
    _isRinging.value = true

    try {
      // 1. Vibrate device
      val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
      vibrator?.let { v ->
        val pattern = longArrayOf(0, 800, 400, 800, 400, 800)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          v.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
          @Suppress("DEPRECATION")
          v.vibrate(pattern, -1)
        }
      }

      // 2. Play Alarm Ringtone
      val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

      val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        ringtone.audioAttributes = AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_ALARM)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build()
      }
      activeRingtone = ringtone
      ringtone.play()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  /**
   * Plays alert tone specifically for class postponement notice.
   */
  fun playPostponeAlert(context: Context, sessionTitle: String? = null) {
    _activeAlarmSessionTitle.value = sessionTitle
    _isRinging.value = true

    try {
      val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
      vibrator?.let { v ->
        val pattern = longArrayOf(0, 500, 200, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          v.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
          @Suppress("DEPRECATION")
          v.vibrate(pattern, -1)
        }
      }

      val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

      val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        ringtone.audioAttributes = AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build()
      }
      activeRingtone = ringtone
      ringtone.play()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  /**
   * Stops the ringtone and vibration.
   */
  fun stopAlarm(context: Context) {
    try {
      activeRingtone?.stop()
      activeRingtone = null
      val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
      vibrator?.cancel()
    } catch (e: Exception) {
      e.printStackTrace()
    } finally {
      _isRinging.value = false
      _activeAlarmSessionTitle.value = null
    }
  }
}
