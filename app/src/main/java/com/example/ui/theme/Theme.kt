package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
  primary = IndigoPrimary,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFEEF2FF),
  onPrimaryContainer = IndigoPrimaryVariant,
  secondary = SlateDarkHeader,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFF1F5F9),
  onSecondaryContainer = SlateDarkHeader,
  tertiary = EmeraldSuccess,
  onTertiary = Color.White,
  background = SlateBackground,
  onBackground = SlateTextPrimary,
  surface = SlateSurface,
  onSurface = SlateTextPrimary,
  surfaceVariant = Color(0xFFF1F5F9),
  onSurfaceVariant = SlateTextSecondary,
  outline = SlateBorder
)

private val DarkColorScheme = darkColorScheme(
  primary = IndigoSecondary,
  onPrimary = Color.White,
  primaryContainer = IndigoPrimaryVariant,
  onPrimaryContainer = Color(0xFFEEF2FF),
  secondary = Color(0xFF94A3B8),
  onSecondary = SlateDarker,
  secondaryContainer = SlateDarkHeader,
  onSecondaryContainer = Color.White,
  tertiary = EmeraldSuccess,
  onTertiary = Color.White,
  background = SlateDarker,
  onBackground = Color(0xFFF8FAFC),
  surface = SlateDarkHeader,
  onSurface = Color(0xFFF8FAFC),
  surfaceVariant = Color(0xFF334155),
  onSurfaceVariant = Color(0xFFCBD5E1),
  outline = Color(0xFF475569)
)

@Composable
fun PixelPathsalaTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      window.statusBarColor = Color(0xFF1E293B).toArgb()
      WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
