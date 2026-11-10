package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HyperRushColorScheme = darkColorScheme(
  primary = NeonCyan,
  onPrimary = Color.Black,
  primaryContainer = CardNavy,
  onPrimaryContainer = NeonCyan,
  secondary = NeonOrange,
  onSecondary = Color.Black,
  secondaryContainer = Color(0xFF2E1906),
  onSecondaryContainer = NeonOrange,
  tertiary = NeonYellow,
  onTertiary = Color.Black,
  background = DarkNavyBg,
  onBackground = Color.White,
  surface = CardNavy,
  onSurface = Color.White,
  surfaceVariant = Color(0xFF1E293B),
  onSurfaceVariant = Color(0xFFCBD5E1),
  outline = BorderCyan,
  error = DangerRed,
  onError = Color.White
)

@Composable
fun HyperRushTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = HyperRushColorScheme,
    typography = Typography,
    content = content
  )
}
