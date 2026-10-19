package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import com.example.data.model.Student
import com.example.data.model.StudentTelemetry
import java.text.SimpleDateFormat
import java.util.*

object StudentDeviceMonitor {

  // Popular games & entertainment apps checked for study focus monitoring
  val POPULAR_GAME_PACKAGES = mapOf(
    "com.dts.freefireth" to "Free Fire",
    "com.dts.freefiremax" to "Free Fire MAX",
    "com.pubg.imobile" to "BGMI (Battlegrounds Mobile)",
    "com.roblox.client" to "Roblox",
    "com.king.candycrushsaga" to "Candy Crush Saga",
    "com.kiloo.subwaysurf" to "Subway Surfers",
    "com.ludo.king" to "Ludo King",
    "com.supercell.clashofclans" to "Clash of Clans",
    "com.miniclip.eightballpool" to "8 Ball Pool",
    "com.activision.callofduty.shooter" to "Call of Duty Mobile",
    "com.ea.gp.fifamobile" to "EA SPORTS FC Mobile",
    "com.tencent.ig" to "PUBG Mobile",
    "com.mojang.minecraftpe" to "Minecraft",
    "com.fingersoft.hillclimb" to "Hill Climb Racing"
  )

  fun detectInstalledGames(context: Context): List<String> {
    val detected = mutableListOf<String>()
    val pm = context.packageManager

    // 1. Direct check against known game packages
    POPULAR_GAME_PACKAGES.forEach { (pkg, name) ->
      try {
        pm.getPackageInfo(pkg, 0)
        detected.add(name)
      } catch (_: PackageManager.NameNotFoundException) {
        // Not installed
      }
    }

    // 2. Category check for Android O+ if QUERY_ALL_PACKAGES is active
    try {
      val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
      for (app in installedApps) {
        if ((app.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (app.category == ApplicationInfo.CATEGORY_GAME) {
              val appName = pm.getApplicationLabel(app).toString()
              if (!detected.contains(appName)) {
                detected.add(appName)
              }
            }
          }
        }
      }
    } catch (_: Exception) {}

    return detected
  }

  fun getBatteryInfo(context: Context): Pair<Int, Boolean> {
    return try {
      val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
      val batteryStatus = context.registerReceiver(null, ifilter)
      val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
      val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
      val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 85
      val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
      val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
      Pair(batteryPct, isCharging)
    } catch (_: Exception) {
      Pair(85, false)
    }
  }

  fun getNetworkType(context: Context): String {
    return try {
      val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
      val network = cm?.activeNetwork
      val caps = cm?.getNetworkCapabilities(network)
      when {
        caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi"
        caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Mobile Data (4G/5G)"
        else -> "Online"
      }
    } catch (_: Exception) {
      "Mobile Network"
    }
  }

  @SuppressLint("MissingPermission")
  fun getLastKnownLocation(context: Context): Pair<Double, Double> {
    // Default coordinates: Raina, Purba Bardhaman center location
    var lat = 23.2324
    var lng = 87.8615

    try {
      val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
      val gpsLoc: Location? = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
      val netLoc: Location? = lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

      val bestLoc = gpsLoc ?: netLoc
      if (bestLoc != null) {
        lat = bestLoc.latitude
        lng = bestLoc.longitude
      }
    } catch (_: Exception) {
      // Permission not yet granted or GPS off -> fallback to center area
    }

    return Pair(lat, lng)
  }

  fun collectTelemetry(context: Context, student: Student): StudentTelemetry {
    val (batLevel, isCharging) = getBatteryInfo(context)
    val (lat, lng) = getLastKnownLocation(context)
    val netType = getNetworkType(context)
    val games = detectInstalledGames(context)

    // Calculate study focus score (100% baseline, -15% per gaming distraction)
    val focusScore = maxOf(25, 100 - (games.size * 15))

    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.US)
    val heartbeat = sdf.format(Date())

    val deviceName = "${Build.MANUFACTURER.capitalize(Locale.US)} ${Build.MODEL}"

    return StudentTelemetry(
      id = "tel-${student.id}",
      studentId = student.id,
      studentName = student.name,
      latitude = lat,
      longitude = lng,
      locationAddress = "Raina, Purba Bardhaman",
      deviceModel = deviceName,
      batteryLevel = batLevel,
      isCharging = isCharging,
      networkType = netType,
      installedGames = games,
      studyFocusScore = focusScore,
      lastHeartbeat = heartbeat
    )
  }
}
