package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tiles")
data class TileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val className: String? = null,
    val label: String,
    val size: String = "MEDIUM", // SMALL, MEDIUM, WIDE
    val position: Int = 0,
    val isCustomTile: Boolean = false,
    val customAccentColor: String? = null,
    val unreadCount: Int = 0,
    val secondaryText: String? = null
)
