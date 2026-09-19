package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = MilitaryAccent,
    onPrimary = MilitaryPrimaryDark,
    primaryContainer = MilitaryPrimary,
    onPrimaryContainer = MilitaryLightGreen,
    secondary = MilitaryLightGreen,
    onSecondary = MilitaryPrimaryDark,
    background = Color(0xFF0F1713),
    surface = Color(0xFF16231C),
    onBackground = Color(0xFFE4E9E5),
    onSurface = Color(0xFFE4E9E5),
    outline = Color(0xFF2C3E34),
    error = MilitaryDanger
  )

private val LightColorScheme =
  lightColorScheme(
    primary = MilitaryPrimary,
    onPrimary = Color.White,
    primaryContainer = MilitaryLightGreen,
    onPrimaryContainer = MilitaryPrimaryDark,
    secondary = MilitaryAccent,
    onSecondary = Color.White,
    background = MilitaryBg,
    surface = MilitaryCard,
    onBackground = MilitaryText,
    onSurface = MilitaryText,
    outline = MilitaryBorder,
    error = MilitaryDanger
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Preserve distinctive military theme
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

