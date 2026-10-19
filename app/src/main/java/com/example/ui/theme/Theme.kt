package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandGoldLight,
    onPrimaryContainer = BrandGoldDark,
    secondary = BrandSecondary,
    onSecondary = Color.White,
    tertiary = BrandTertiary,
    onTertiary = Color.White,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    outline = Slate200,
    error = ErrorRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandGold,
    onPrimary = Slate900,
    primaryContainer = BrandSecondary,
    onPrimaryContainer = BrandGoldLight,
    secondary = BrandTertiary,
    onSecondary = Slate900,
    tertiary = BrandPrimary,
    onTertiary = Color.White,
    background = Slate900,
    onBackground = Slate50,
    surface = Slate800,
    onSurface = Slate50,
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Slate400,
    outline = Color(0xFF334155),
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun PixelPathsalaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
