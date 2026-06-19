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
    val isLockScreenEnabled: Boolean = true
)
