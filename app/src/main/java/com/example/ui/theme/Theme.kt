package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Vertical space reserved for the Windows Phone status strip when it is shown. */
val WpStatusBarHeight = 26.dp

private fun wpDark(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = Color.White,
    secondary = accent,
    tertiary = accent,
    background = Color.Black,
    surface = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
)

private fun wpLight(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = Color.White,
    secondary = accent,
    tertiary = accent,
    background = Color.White,
    surface = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Windows Phone uses a fixed accent palette + pure black/white, NOT Material You
    // dynamic color. The accent flows in from the user's settings so every Material3
    // component (ripples, switches, etc.) inherits the WP accent instead of purple.
    accentColorHex: String = "#0078D7",
    content: @Composable () -> Unit,
) {
    val accent = remember(accentColorHex) { parseAccent(accentColorHex) }
    val colorScheme = if (darkTheme) wpDark(accent) else wpLight(accent)
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
