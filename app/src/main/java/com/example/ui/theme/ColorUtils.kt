package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/** Cobalt — the default Windows Phone accent, used as a safe fallback. */
val WpDefaultAccent = Color(0xFF0078D7)

/**
 * Parse a stored hex string into a Compose [Color] without ever throwing.
 * [android.graphics.Color.parseColor] throws on an empty/blank/malformed value;
 * a persisted accent must never be able to crash the whole UI, so we fall back
 * to the Cobalt default instead.
 */
fun parseAccent(hex: String?): Color = try {
    Color(android.graphics.Color.parseColor(hex?.takeIf { it.isNotBlank() } ?: "#0078D7"))
} catch (e: IllegalArgumentException) {
    WpDefaultAccent
}
