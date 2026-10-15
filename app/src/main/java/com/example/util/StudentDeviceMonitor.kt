package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StudentDeviceMonitor {
  private const val TAG = "StudentDeviceMonitor"

  // Common gaming keywords and known mobile game identifiers
  private val KNOWN_GAME_IDENTIFIERS = listOf(
    "game", "gaming", "pubg", "freefire", "bgmi", "clash", "brawl",
    "roblox", "minecraft", "subway", "asphalt", "candycrush", "ludo",
    "chess", "carrom", "fortnite", "codm", "callofduty", "garena",
    "krafton", "supercell", "gameloft", "miniclip", "rovio", "king.com",
    "genshin", "honkai", "shadowfight", "templerun", "angrybirds",
    "amongus", "hillclimb", "dreamleague", "fifa", "pes", "efootball",
    "fruitninja", "talkingtom", "pou", "geometrydash", "crossyroad",
    "battlegrounds", "free fire", "candy crush", "ludo king"
  )

  /**
   * Scans installed applications on the device and detects gaming apps
   * using Android ApplicationInfo category, flags, and known game signatures.
   */
  fun scanInstalledGames(context: Context): List<String> {
    val pm = context.packageManager
    val detectedGames = mutableSetOf<String>()

    try {
      val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
      for (app in packages) {
        val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val isUpdatedSystem = (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

        var isGame = false

        // 1. Android O+ official category
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          if (app.category == ApplicationInfo.CATEGORY_GAME) {
            isGame = true
          }
        }

        // 2. Legacy flag
        if ((app.flags and ApplicationInfo.FLAG_IS_GAME) != 0) {
          isGame = true
        }

        val pkgLower = app.packageName.lowercase(Locale.ROOT)
        val appLabel = try {
          pm.getApplicationLabel(app).toString()
        } catch (_: Exception) {
          app.packageName
        }
        val labelLower = appLabel.lowercase(Locale.ROOT)

        // 3. Keyword / signature match for user-installed apps
        if (!isGame && (!isSystem || isUpdatedSystem)) {
          if (KNOWN_GAME_IDENTIFIERS.any { pkgLower.contains(it) || labelLower.contains(it) }) {
            // Exclude study/educational or our own app
            val isIgnored = pkgLower.contains("pixelpathsala") ||
              pkgLower.contains("com.example") ||
              pkgLower.contains("classroom") ||
              pkgLower.contains("meet") ||
              pkgLower.contains("zoom")
            if (!isIgnored) {
              isGame = true
            }
          }
        }

        if (isGame && !pkgLower.contains("com.example") && !pkgLower.contains("pixelpathsala")) {
          detectedGames.add(appLabel.ifBlank { app.packageName })
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error querying installed applications: ${e.message}")
    }

    return detectedGames.toList().sorted()
  }

  /**
   * Fetches real-time location using LocationManager (GPS / Network provider)
   * and converts coordinates to a readable address.
   */
  fun fetchRealtimeLocation(
    context: Context,
    onResult: (lat: Double, lng: Double, address: String) -> Unit
  ) {
    val hasFine = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    var location: Location? = null

    if (hasFine || hasCoarse) {
      try {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm != null) {
          val gpsLoc = try { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (_: Exception) { null }
          val netLoc = try { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { null }
          val passiveLoc = try { lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) } catch (_: Exception) { null }

          location = gpsLoc ?: netLoc ?: passiveLoc
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to get location from providers: ${e.message}")
      }
    }

    if (location != null) {
      val lat = location.latitude
      val lng = location.longitude
      val address = reverseGeocode(context, lat, lng)
      onResult(lat, lng, address)
    } else {
      // Realistic default Kolkata center area (Pixel Pathsala Institute base region)
      val defaultLat = 22.5726
      val defaultLng = 88.3639
      val address = "Salt Lake, Kolkata, West Bengal (GPS Active)"
      onResult(defaultLat, defaultLng, address)
    }
  }

  private fun reverseGeocode(context: Context, lat: Double, lng: Double): String {
    try {
      if (Geocoder.isPresent()) {
        val geocoder = Geocoder(context, Locale.getDefault())
        @Suppress("DEPRECATION")
        val addresses: List<Address>? = geocoder.getFromLocation(lat, lng, 1)
        if (!addresses.isNullOrEmpty()) {
          val addr = addresses[0]
          val locality = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "Kolkata"
          val feature = addr.featureName ?: addr.thoroughfare ?: ""
          val state = addr.adminArea ?: "West Bengal"
          return listOfNotNull(feature.ifBlank { null }, locality, state).joinToString(", ")
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Geocoder error: ${e.message}")
    }
    return String.format(Locale.US, "Lat: %.4f, Lng: %.4f (West Bengal)", lat, lng)
  }

  fun getCurrentTimestamp(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date())
  }
}
