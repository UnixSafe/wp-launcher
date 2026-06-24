package com.example.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.parseAccent
import com.example.viewmodel.ActiveScreen
import com.example.viewmodel.LauncherViewModel

data class ColorAccentItem(val name: String, val hex: String)

/** Authentic Windows Phone / Windows 10 Mobile Metro toggle: a flat pill track that fills with
 *  the accent when on (white thumb on the right), or a thin bordered transparent track when off
 *  (foreground thumb on the left). Replaces the Material3 Switch which looks like an Android toggle. */
@Composable
fun WpToggle(
    checked: Boolean,
    onToggle: () -> Unit,
    accentColor: Color,
    isDark: Boolean
) {
    val trackW = 46.dp
    val trackH = 20.dp
    val thumbSize = 12.dp
    val pad = 4.dp
    val fg = if (isDark) Color.White else Color.Black
    val thumbX by animateDpAsState(
        targetValue = if (checked) trackW - thumbSize - pad else pad,
        label = "wp_toggle_thumb"
    )
    Box(
        modifier = Modifier
            .size(width = trackW, height = trackH)
            .clip(RoundedCornerShape(trackH / 2))
            .background(if (checked) accentColor else Color.Transparent)
            .border(
                width = 2.dp,
                color = if (checked) Color.Transparent else fg.copy(alpha = 0.55f),
                shape = RoundedCornerShape(trackH / 2)
            )
            .clickable { onToggle() },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbX)
                .size(thumbSize)
                .clip(CircleShape)
                .background(if (checked) Color.White else fg.copy(alpha = 0.85f))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val isDark = settings.isDarkTheme
    val accentColor = parseAccent(settings.accentColorHex)

    // List of standard beautiful Windows Phone accents
    val accents = listOf(
        ColorAccentItem("Cobalt", "#0078D7"),
        ColorAccentItem("Crimson", "#E81123"),
        ColorAccentItem("Emerald", "#107C41"),
        ColorAccentItem("Mango", "#F7630C"),
        ColorAccentItem("Lime", "#76B900"),
        ColorAccentItem("Magenta", "#D13438"),
        ColorAccentItem("Violet", "#B4009E"),
        ColorAccentItem("Teal", "#00B7C3"),
        ColorAccentItem("Steel", "#7A7574"),
        ColorAccentItem("Amber", "#FF8C00"),
        ColorAccentItem("Mauve", "#744DA9"),
        ColorAccentItem("Indigo", "#5C2D91")
    )

    var tempCortanaName by remember(settings.cortanaName) { mutableStateOf(settings.cortanaName) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) Color.Black else Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header: Back arrow and "paramètres"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.setScreen(ActiveScreen.START) },
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (isDark) Color(0xFF1D1B20) else Color(0xFFF3F0F4), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Retour",
                        tint = if (isDark) Color.White else Color.Black
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "paramètres",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraLight,
                    color = if (isDark) Color.White else Color.Black
                )
            }

            // Single honest section header (WP Settings is a flat scrolling list, not a pivot).
            Text(
                text = "système",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // THEME TOGGLE
                item {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "Arrière-plan",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleTheme() }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isDark) "Sombre" else "Clair",
                                fontSize = 16.sp,
                                color = if (isDark) Color.White else Color.Black
                            )
                            WpToggle(
                                checked = isDark,
                                onToggle = { viewModel.toggleTheme() },
                                accentColor = accentColor,
                                isDark = isDark
                            )
                        }
                    }
                }

                // GRID COLUMN TOGGLE
                item {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "Taille de l'écran d'accueil",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleColumns() }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (settings.useThreeColumns) "3 Colonnes (WP 8.1)" else "2 Colonnes (WP 8)",
                                fontSize = 16.sp,
                                color = if (isDark) Color.White else Color.Black
                            )
                            WpToggle(
                                checked = settings.useThreeColumns,
                                onToggle = { viewModel.toggleColumns() },
                                accentColor = accentColor,
                                isDark = isDark
                            )
                        }
                    }
                }

                // STATUS BAR TOGGLE
                item {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "Barre d'état du launcher",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleStatusBar() }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (settings.showStatusBar) "Afficher l'heure & stats" else "Masquer la barre d'état",
                                fontSize = 16.sp,
                                color = if (isDark) Color.White else Color.Black
                            )
                            WpToggle(
                                checked = settings.showStatusBar,
                                onToggle = { viewModel.toggleStatusBar() },
                                accentColor = accentColor,
                                isDark = isDark
                            )
                        }
                    }
                }

                // WALLPAPER TOGGLE
                item {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "Mosaïque d'arrière-plan (Parallax)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleWallpaper() }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (settings.useWallpaperBackground) "Activée - Tuiles transparentes" else "Désactivée - Tuiles pleines",
                                fontSize = 16.sp,
                                color = if (isDark) Color.White else Color.Black
                            )
                            WpToggle(
                                checked = settings.useWallpaperBackground,
                                onToggle = { viewModel.toggleWallpaper() },
                                accentColor = accentColor,
                                isDark = isDark
                            )
                        }
                    }
                }

                // WALLPAPER PRESET SELECTION
                if (settings.useWallpaperBackground) {
                    item {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = "Choix de l'Arrière-plan mosaïque",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Classic Blue", "Aurora Skies", "Forest Mist").forEach { name ->
                                    val isSelected = settings.wallpaperName == name
                                    val presetColor = when (name) {
                                        "Aurora Skies" -> Color(0xFF3F2B96)
                                        "Forest Mist" -> Color(0xFF1B3D34)
                                        else -> Color(0xFF003366)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .background(presetColor)
                                            .border(
                                                width = if (isSelected) 2.dp else 0.dp,
                                                color = if (isSelected) (if (isDark) Color.White else Color.Black) else Color.Transparent
                                            )
                                            .clickable { viewModel.updateWallpaperName(name) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = name,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // SIGNATURE LOCK SCREEN TOGGLE
                item {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "Écran de verrouillage Windows Phone",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleLockScreen() }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (settings.isLockScreenEnabled) "Activé (Glisser vers le haut)" else "Désactivé (Démarrage direct)",
                                fontSize = 16.sp,
                                color = if (isDark) Color.White else Color.Black
                            )
                            WpToggle(
                                checked = settings.isLockScreenEnabled,
                                onToggle = { viewModel.toggleLockScreen() },
                                accentColor = accentColor,
                                isDark = isDark
                            )
                        }
                    }
                }

                // CORTANA SPEECH VOICE TOGGLE
                item {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "Synthèse vocale de l'assistante (TTS)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleCortanaVoice() }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (settings.useCortanaVoice) "Voix active" else "Voix inactive",
                                fontSize = 16.sp,
                                color = if (isDark) Color.White else Color.Black
                            )
                            WpToggle(
                                checked = settings.useCortanaVoice,
                                onToggle = { viewModel.toggleCortanaVoice() },
                                accentColor = accentColor,
                                isDark = isDark
                            )
                        }
                    }
                }

                // ASSISTANT NICKNAME
                item {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "Nom de l'assistante AI",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = tempCortanaName,
                            onValueChange = {
                                tempCortanaName = it
                                viewModel.updateCortanaNickname(it)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(0.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = if (isDark) Color.White else Color.Black,
                                unfocusedTextColor = if (isDark) Color.White else Color.Black,
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // ACCENT COLOR PICKER
                item {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "Couleur d'accentuation actuelle : ${settings.accentName}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // 3x4 Matrix color picker
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (rowIndex in 0 until 4) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (colIndex in 0 until 3) {
                                        val itemIndex = rowIndex * 3 + colIndex
                                        if (itemIndex < accents.size) {
                                            val currentAccent = accents[itemIndex]
                                            val currentAccentColor = parseAccent(currentAccent.hex)
                                            val isSelected = settings.accentName == currentAccent.name

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                                    .background(currentAccentColor)
                                                    .border(
                                                        width = if (isSelected) 3.dp else 0.dp,
                                                        color = if (isSelected) (if (isDark) Color.White else Color.Black) else Color.Transparent
                                                    )
                                                    .clickable {
                                                        viewModel.updateAccent(currentAccent.name, currentAccent.hex)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = currentAccent.name,
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // DIAGNOSIS & MAINTENANCE DECK
                item {
                    Divider(
                        color = if (isDark) Color(0xFF222222) else Color(0xFFE0E0E0),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    Text(
                        text = "maintenance du launcher",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Reset Default layout button
                    Button(
                        onClick = { viewModel.forceDefaultLayout() },
                        shape = RoundedCornerShape(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Restore, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RESTAURER LA DISPOSITION WP D'ORIGINE", fontSize = 11.sp, color = Color.White)
                    }

                    // Flush Notifications button
                    OutlinedButton(
                        onClick = { viewModel.resetMockNotifications() },
                        shape = RoundedCornerShape(0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDark) Color.White else Color.Black),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RÉINITIALISER LES VISUELS DE TUILES", fontSize = 11.sp)
                    }
                }

                // Footer credits
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "WP Launcher v1.0.0 • AI Studio Build",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Light,
                            modifier = Modifier.padding(bottom = 60.dp)
                        )
                    }
                }
            }
        }
    }
}
