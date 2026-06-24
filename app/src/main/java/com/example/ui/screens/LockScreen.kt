package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SettingsEntity
import com.example.viewmodel.LauncherViewModel
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.roundToInt

@Composable
fun LockScreen(
    viewModel: LauncherViewModel,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val tiles by viewModel.tiles.collectAsState()

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val screenHeightPx = with(LocalDensity.current) { screenHeight.toPx() }

    // Offset of the lockscreen panel as it is dragged upwards
    var offsetY by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    // Live date & time: a HOME launcher process is long-lived, so the clock must keep ticking
    // instead of freezing at whatever time the lock screen first composed.
    val frenchDays = listOf("", "dimanche", "lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi")
    val frenchMonths = listOf("janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre")

    var now by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Calendar.getInstance()
            kotlinx.coroutines.delay(10_000)
        }
    }
    val hourText = remember(now) {
        val h = now.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val m = now.get(Calendar.MINUTE).toString().padStart(2, '0')
        "$h:$m"
    }
    val dateFormatted = remember(now) {
        val d = frenchDays.getOrElse(now.get(Calendar.DAY_OF_WEEK)) { "" }
        val m = frenchMonths.getOrElse(now.get(Calendar.MONTH)) { "" }
        "$d ${now.get(Calendar.DAY_OF_MONTH)} $m"
    }

    // Next agenda line sourced from the calendar tile, so it stays consistent with the Start tile.
    val nextEvent = remember(tiles) {
        tiles.find { it.packageName == "wp:calendar" }?.secondaryText ?: "Aucun événement"
    }

    // Unread count simulations from tiles
    val smsCount = remember(tiles) { tiles.find { it.packageName == "wp:sms" }?.unreadCount ?: 0 }
    val callCount = remember(tiles) { tiles.find { it.packageName == "wp:phone" }?.unreadCount ?: 0 }

    // Background selection for Lock Screen
    val lockScreenBrush = remember(settings.wallpaperName) {
        when (settings.wallpaperName) {
            "Aurora Skies" -> Brush.verticalGradient(listOf(Color(0xFF3F2B96), Color(0xFF1E0B36)))
            "Forest Mist" -> Brush.verticalGradient(listOf(Color(0xFF0F2027), Color(0xFF152A30)))
            else -> Brush.verticalGradient(listOf(Color(0xFF001F3F), Color(0xFF001122))) // Cobalt deep sky
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .background(lockScreenBrush)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    // Only allow dragging upwards (negative delta) or reset if dragged down
                    offsetY = (offsetY + delta).coerceAtMost(0f)
                },
                onDragStopped = { velocity ->
                    // If dragged past 25% of the screen or swiped fast upwards, animate unlock
                    if (offsetY < -screenHeightPx * 0.25f || velocity < -1000f) {
                        coroutineScope.launch {
                            animate(
                                initialValue = offsetY,
                                targetValue = -screenHeightPx,
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) { valValue, _ ->
                                offsetY = valValue
                            }
                            onUnlock()
                        }
                    } else {
                        // Spring back to fully locked
                        coroutineScope.launch {
                            animate(
                                initialValue = offsetY,
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) { valValue, _ ->
                                offsetY = valValue
                            }
                        }
                    }
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Instructions hints
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(5.dp)
                            .background(Color.White.copy(alpha = 0.6f), CircleShape)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Glissez vers le haut pour déverrouiller",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }

            // Middle Section: Digital Clock & Date calendar (Strict WP typographic layout)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = hourText,
                    fontSize = 84.sp,
                    fontWeight = FontWeight.ExtraLight,
                    color = Color.White,
                    letterSpacing = (-2).sp,
                    lineHeight = 90.sp
                )
                Text(
                    text = dateFormatted,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Next agenda preview simulation
                Column(
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = "Prochain événement",
                        color = Color(0xFF00D2FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = nextEvent,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Light,
                        maxLines = 1
                    )
                }
            }

            // Bottom Section: Status Glyphs & Counters list
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Signal & WiFi
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )

                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Messages Counter badge (if any simulated unread count exists)
                if (smsCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = smsCount.toString(),
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Missed Calls Counter badge
                if (callCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = callCount.toString(),
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
