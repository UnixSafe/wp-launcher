package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1, // Always 1
    val accentName: String = "Cobalt",
    val accentColorHex: String = "#0078D7",
    val isDarkTheme: Boolean = true,
    val useThreeColumns: Boolean = true, // WP8.1 3-column start screen
    val showStatusBar: Boolean = true,
    val cortanaName: String = "Cortana",
    val useWallpaperBackground: Boolean = false,
    val wallpaperName: String = "Classic Blue",
    val useCortanaVoice: Boolean = true,
    val isLockScreenEnabled: Boolean = true,
    // Timestamp (ms) of the last time the user dismissed the "set as default launcher"
    // prompt with "plus tard". Used to apply a re-prompt cooldown instead of nagging
    // on every cold start. 0 = never dismissed.
    val defaultPromptDismissedAt: Long = 0,
    // Performance mode: replaces the expensive 3D tile animations (tilt + flip) with cheap
    // 2D equivalents for weak GPUs. Off by default.
    val performanceMode: Boolean = false,
    // Timestamp (ms) of the last dismissal of the "enable performance mode?" prompt. 0 = never.
    val perfModePromptDismissedAt: Long = 0,
    // Windows Phone icon pack: render third-party app tiles as a flat white glyph on the accent
    // tile (Metro look) instead of the colourful real icon.
    val wpStyleAppIcons: Boolean = false
)
