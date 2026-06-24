package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.parseAccent
import com.example.viewmodel.ActiveScreen
import com.example.viewmodel.LauncherViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ActionCenter(
    viewModel: LauncherViewModel,
    isOpen: Boolean,
    onClose: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val incomingNotification by viewModel.incomingNotification.collectAsState()

    // Mock toggle states
    var isWifiOn by remember { mutableStateOf(true) }
    var isBluetoothOn by remember { mutableStateOf(false) }
    var isAirplaneMode by remember { mutableStateOf(false) }
    var isLocationOn by remember { mutableStateOf(true) }

    val accentColor = parseAccent(settings.accentColorHex)
    val isDark = settings.isDarkTheme

    // Sliding Top Sheet
    AnimatedVisibility(
        visible = isOpen,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it }),
        label = "ActionCenterSlide"
    ) {
        Surface(
            color = if (isDark) Color(0xFB141414) else Color(0xFBFAFAFA),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clickable { /* Block clicks falling behind */ }
                .pointerInput(Unit) {
                    var accumulatedDrag = 0f
                    detectVerticalDragGestures(
                        onDragStart = { accumulatedDrag = 0f },
                        onVerticalDrag = { change, dragAmount ->
                            accumulatedDrag += dragAmount
                            change.consume()
                        },
                        // Decide dismissal only on release (was firing repeatedly mid-drag,
                        // snapping the sheet shut while the finger was still down).
                        onDragEnd = {
                            if (accumulatedDrag < -60f) onClose()
                            accumulatedDrag = 0f
                        },
                        onDragCancel = { accumulatedDrag = 0f }
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(20.dp)
            ) {
                // Header (WP "notification center" style date/status bar info)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CENTRE D'ACTIONS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Vendredi 19 Juin",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Thin,
                            color = if (isDark) Color.White else Color.Black
                        )
                    }

                    // Done / Close indicator
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(36.dp)
                            .background(if (isDark) Color(0xFF222222) else Color(0xFFEEEEEE), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Fermer",
                            tint = if (isDark) Color.White else Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // WP 4-Toggle Action Buttons (Grid)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionToggleBlock(
                        label = "Wi-Fi",
                        isOn = isWifiOn,
                        onIcon = Icons.Default.Wifi,
                        offIcon = Icons.Default.WifiOff,
                        accentColor = accentColor,
                        isDark = isDark,
                        onClick = { isWifiOn = !isWifiOn },
                        modifier = Modifier.weight(1f)
                    )

                    ActionToggleBlock(
                        label = "Bluetooth",
                        isOn = isBluetoothOn,
                        onIcon = Icons.Default.Bluetooth,
                        offIcon = Icons.Default.BluetoothDisabled,
                        accentColor = accentColor,
                        isDark = isDark,
                        onClick = { isBluetoothOn = !isBluetoothOn },
                        modifier = Modifier.weight(1f)
                    )

                    ActionToggleBlock(
                        label = "Avion",
                        isOn = isAirplaneMode,
                        onIcon = Icons.Default.AirplaneTicket,
                        offIcon = Icons.Default.AirplanemodeInactive,
                        accentColor = accentColor,
                        isDark = isDark,
                        onClick = { isAirplaneMode = !isAirplaneMode },
                        modifier = Modifier.weight(1f)
                    )

                    ActionToggleBlock(
                        label = "Position",
                        isOn = isLocationOn,
                        onIcon = Icons.Default.LocationOn,
                        offIcon = Icons.Default.LocationOff,
                        accentColor = accentColor,
                        isDark = isDark,
                        onClick = { isLocationOn = !isLocationOn },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // "All settings" link
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onClose()
                            viewModel.setScreen(ActiveScreen.SETTINGS)
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOUS LES PARAMÈTRES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Divider(
                    color = if (isDark) Color(0xFF222222) else Color(0xFFE0E0E0),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Notification Board Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "notifications",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Light,
                        color = if (isDark) Color.White else Color.Black
                    )

                    Text(
                        text = "tout effacer",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.clickable {
                            viewModel.dismissNotificationBanner()
                            viewModel.resetMockNotifications()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Alerts List & Simulators
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (incomingNotification != null) {
                        item {
                            NotificationAlertItem(
                                title = incomingNotification!!.substringBefore(" • "),
                                message = incomingNotification!!.substringAfter(" • "),
                                accentColor = accentColor,
                                isDark = isDark,
                                onDismiss = { viewModel.dismissNotificationBanner() }
                            )
                        }
                    } else {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Aucune nouvelle notification",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // SIMULATION CONTROL DECK
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "simuler des notifications (test de tuiles)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.simulateNotificationIncoming("Téléphone", "Appel manqué d'un numéro masqué")
                                },
                                shape = RoundedCornerShape(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Appel Manqué", fontSize = 11.sp, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    viewModel.simulateNotificationIncoming("Messages", "Nouveau SMS : Salut, tu as vu le launcher ?")
                                },
                                shape = RoundedCornerShape(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Nouveau SMS", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }

                // Sleek visual grab handle pill at the bottom context of the ActionCenter
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(60.dp)
                        .height(5.dp)
                        .background(
                            color = if (isDark) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(2.5.dp)
                        )
                        .clickable { onClose() }
                )
            }
        }
    }
}

@Composable
fun ActionToggleBlock(
    label: String,
    isOn: Boolean,
    onIcon: ImageVector,
    offIcon: ImageVector,
    accentColor: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(0.dp))
            .background(if (isOn) accentColor else if (isDark) Color(0xFF1F1F1F) else Color(0xFFE5E5E5))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = if (isOn) onIcon else offIcon,
                contentDescription = label,
                tint = if (isOn || isDark) Color.White else Color.Black,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = label,
                fontSize = 11.sp,
                color = if (isOn || isDark) Color.White else Color.Black,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun NotificationAlertItem(
    title: String,
    message: String,
    accentColor: Color,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    Surface(
        color = if (isDark) Color(0xFF1D1D1D) else Color(0xFFEEEEEE),
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual accent vertical key lines
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .background(accentColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = if (isDark) Color.White else Color.Black,
                    maxLines = 2,
                    lineHeight = 16.sp
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Effacer",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
