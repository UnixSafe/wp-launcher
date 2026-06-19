package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SettingsEntity
import com.example.data.TileEntity
import com.example.repository.AppItem
import com.example.viewmodel.ActiveScreen
import com.example.viewmodel.LauncherViewModel
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StartScreen(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val tiles by viewModel.tiles.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val liveFlipState by viewModel.liveFlipState.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()

    val accentColor = Color(android.graphics.Color.parseColor(settings.accentColorHex))
    val maxSpan = if (settings.useThreeColumns) 6 else 4

    // Dynamic tile packing to arrange SMALL (1x1), MEDIUM (2x2), WIDE (4x2 / 6x2)
    val packedRows = remember(tiles, maxSpan) {
        packTilesToRows(tiles, maxSpan)
    }

    // Determine lock background brushes based on custom wallpaper selection
    val isDark = settings.isDarkTheme
    val backgroundBrush = remember(settings.useWallpaperBackground, settings.wallpaperName, isDark) {
        if (settings.useWallpaperBackground) {
            when (settings.wallpaperName) {
                "Aurora Skies" -> Brush.verticalGradient(
                    colors = listOf(Color(0xFF3F2B96), Color(0xFF1E0B36))
                )
                "Forest Mist" -> Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F2027), Color(0xFF152A30))
                )
                else -> Brush.verticalGradient( // Classic Blue Preset
                    colors = listOf(Color(0xFF001F3F), Color(0xFF001122))
                )
            }
        } else {
            null
        }
    }

    val backgroundModifier = if (settings.useWallpaperBackground && backgroundBrush != null) {
        Modifier.background(backgroundBrush)
    } else {
        Modifier.background(if (isDark) Color.Black else Color.White)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(backgroundModifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header: "début" in thin WP-style title
            Text(
                text = "accueil",
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraLight,
                color = if (settings.isDarkTheme) Color.White else Color.Black,
                modifier = Modifier
                    .padding(start = 20.dp, top = 20.dp, bottom = 12.dp)
                    .combinedClickable(
                        onClick = { if (isEditMode) viewModel.setEditMode(false) },
                        onLongClick = { viewModel.setEditMode(true) }
                    )
            )

            // Edit Mode Alert Banner
            if (isEditMode) {
                Surface(
                    color = accentColor.copy(alpha = 0.2f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Mode Édition • Appuyez sur accueil pour quitter",
                            fontSize = 12.sp,
                            color = if (settings.isDarkTheme) Color.White else Color.Black,
                        )
                        IconButton(
                            onClick = { viewModel.setEditMode(false) },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fermer",
                                tint = if (settings.isDarkTheme) Color.White else Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Scrollable start tile board with perfect geometric square aspect-ratio tile heights
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            ) {
                val totalWidth = maxWidth
                val maxSpanVal = maxSpan
                val gap = 8.dp
                val singleSpanWidth = (totalWidth - gap * (maxSpanVal - 1)) / maxSpanVal
                
                val smallTileHeight = singleSpanWidth
                val mediumTileHeight = (singleSpanWidth * 2) + gap
                val wideTileHeight = mediumTileHeight

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(gap)
                ) {
                    items(packedRows.size) { rowIndex ->
                        val rowItems = packedRows[rowIndex]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(gap)
                        ) {
                            rowItems.forEach { tile ->
                                val span = getSpanForSize(tile.size)
                                val weight = span.toFloat() / maxSpanVal.toFloat()
                                val tileHeight = when (tile.size) {
                                    "SMALL" -> smallTileHeight
                                    "MEDIUM" -> mediumTileHeight
                                    "WIDE" -> wideTileHeight
                                    else -> mediumTileHeight
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(weight)
                                        .height(tileHeight)
                                ) {
                                    PhoneTile(
                                        tile = tile,
                                        settings = settings,
                                        accentColor = accentColor,
                                        isEditMode = isEditMode,
                                        liveFlipState = liveFlipState,
                                        installedApps = installedApps,
                                        onLaunch = { viewModel.launchTile(tile) },
                                        onUnpin = { viewModel.unpinTile(tile) },
                                        onResize = { viewModel.cycleTileSize(tile) },
                                        onMoveUp = { viewModel.moveTileUp(tile) },
                                        onMoveDown = { viewModel.moveTileDown(tile) }
                                    )
                                }
                            }
                            
                            // Fill empty gaps in rows back with transparent filler if necessary
                            val currentSum = rowItems.sumOf { getSpanForSize(it.size) }
                            if (currentSum < maxSpanVal) {
                                Spacer(modifier = Modifier.weight((maxSpanVal - currentSum).toFloat() / maxSpanVal.toFloat()))
                            }
                        }
                    }

                    // Bottom padding
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Floating navigation button to swipes to All Apps
        IconButton(
            onClick = { viewModel.setScreen(ActiveScreen.ALL_APPS) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp)
                .size(48.dp)
                .background(
                    if (settings.isDarkTheme) Color(0xFF222222) else Color(0xFFDDDDDD),
                    CircleShape
                )
                .border(2.dp, if (settings.isDarkTheme) Color.White else Color.Black, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Toutes les applications",
                tint = if (settings.isDarkTheme) Color.White else Color.Black
            )
        }
    }
}

// Live tile wrapper component helper
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhoneTile(
    tile: TileEntity,
    settings: SettingsEntity,
    accentColor: Color,
    isEditMode: Boolean,
    liveFlipState: Int,
    installedApps: List<AppItem>,
    onLaunch: () -> Unit,
    onUnpin: () -> Unit,
    onResize: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val backgroundColor = if (settings.useWallpaperBackground) {
        if (settings.isDarkTheme) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.12f)
    } else if (tile.customAccentColor != null) {
        Color(android.graphics.Color.parseColor(tile.customAccentColor))
    } else {
        accentColor
    }

    // Determine if we should show secondary animated content
    val isFlipped = remember(tile.packageName, liveFlipState) {
        when (tile.packageName) {
            "wp:weather", "wp:photos", "wp:calendar", "wp:cortana" -> (liveFlipState % 2 == 1)
            else -> false
        }
    }

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else if (isEditMode) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "tile_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .padding(2.dp)
            .clip(RoundedCornerShape(0.dp)) // Flat geometric blocks!
            .background(backgroundColor)
            .border(
                width = if (isEditMode) 1.5.dp else 0.dp,
                color = if (isEditMode) Color.White else Color.Transparent
            )
            .pointerInput(isEditMode) {
                detectTapGestures(
                    onPress = {
                        if (!isEditMode) {
                            isPressed = true
                            try {
                                this.tryAwaitRelease()
                            } catch (e: Exception) {
                                // fallback
                            }
                            isPressed = false
                        }
                    },
                    onTap = { if (!isEditMode) onLaunch() },
                    onLongPress = { onResize() }
                )
            }
    ) {
        // Tile Primary content
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = isFlipped,
                transitionSpec = {
                    slideInVertically { height -> height } + fadeIn() togetherWith
                            slideOutVertically { height -> -height } + fadeOut()
                },
                label = "TileFlip"
            ) { flipped ->
                if (flipped) {
                    TileBackState(tile, settings)
                } else {
                    TileFrontState(tile, settings, installedApps)
                }
            }
        }

        // Overlay elements for EDIT MODE
        if (isEditMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                // Unpin button (top right)
                IconButton(
                    onClick = onUnpin,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(24.dp)
                        .background(Color.Black, CircleShape)
                        .border(1.dp, Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PinDrop,
                        contentDescription = "Désépingler",
                        tint = Color.Red,
                        modifier = Modifier.size(12.dp)
                    )
                }

                // Resize button (bottom right)
                IconButton(
                    onClick = onResize,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                        .size(24.dp)
                        .background(Color.Black, CircleShape)
                        .border(1.dp, Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOutMap,
                        contentDescription = "Redimensionner",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }

                // Positional reordering chevrons in the middle
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onMoveUp,
                        modifier = Modifier
                            .size(26.dp)
                            .background(Color.Black.copy(alpha = 0.8f), CircleShape)
                            .border(1.dp, Color.White, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Up",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    IconButton(
                        onClick = onMoveDown,
                        modifier = Modifier
                            .size(26.dp)
                            .background(Color.Black.copy(alpha = 0.8f), CircleShape)
                            .border(1.dp, Color.White, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Down",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

// Side A of Live Tile: Main Icon and title in the bottom-left
@Composable
fun TileFrontState(
    tile: TileEntity,
    settings: SettingsEntity,
    installedApps: List<AppItem>
) {
    val isSmall = tile.size == "SMALL"
    val isWide = tile.size == "WIDE"

    Box(modifier = Modifier.fillMaxSize()) {
        if (tile.packageName.startsWith("wp:")) {
            // Built-in system aesthetic designs
            val vectorIcon = getBuiltInIcon(tile.packageName)
            
            // Draw special tiles visuals
            if (tile.packageName == "wp:clock" && !isSmall) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val timeString = remember {
                        val calendar = Calendar.getInstance()
                        var hrs = calendar.get(Calendar.HOUR_OF_DAY).toString()
                        var mins = calendar.get(Calendar.MINUTE).toString()
                        if (hrs.length == 1) hrs = "0$hrs"
                        if (mins.length == 1) mins = "0$mins"
                        "$hrs:$mins"
                    }
                    Text(
                        text = timeString,
                        fontSize = if (isWide) 48.sp else 32.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.White
                    )
                    Text(
                        text = "Horloge",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (tile.packageName == "wp:photos" && !isSmall) {
                // Draw a beautiful creative simulated dynamic collage
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFE25B26))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Photos",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Vector icon centerpiece
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isSmall) 12.dp else 16.dp)
                ) {
                    Icon(
                        imageVector = vectorIcon,
                        contentDescription = tile.label,
                        tint = Color.White,
                        modifier = Modifier
                            .size(if (isSmall) 26.dp else 36.dp)
                            .align(if (isSmall) Alignment.Center else Alignment.TopStart)
                    )

                    if (!isSmall) {
                        Text(
                            text = tile.label,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.BottomStart)
                        )
                    }
                }
            }
        } else {
            // Android System application icon drawer
            val targetApp = remember(tile.packageName, installedApps) {
                installedApps.find { it.packageName == tile.packageName }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isSmall) 12.dp else 16.dp)
            ) {
                if (targetApp?.icon != null) {
                    Image(
                        bitmap = targetApp.icon.asImageBitmap(),
                        contentDescription = tile.label,
                        modifier = Modifier
                            .size(if (isSmall) 32.dp else 40.dp)
                            .align(if (isSmall) Alignment.Center else Alignment.TopStart)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = tile.label,
                        tint = Color.White,
                        modifier = Modifier
                            .size(if (isSmall) 24.dp else 34.dp)
                            .align(if (isSmall) Alignment.Center else Alignment.TopStart)
                    )
                }

                if (!isSmall) {
                    Text(
                        text = tile.label,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                }
            }
        }

        // Render dynamic Notification badges in bottom-right corner! (like Phone unread count)
        if (tile.unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 12.dp)
                    .background(Color.White.copy(alpha = 0.25f), CircleShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = tile.unreadCount.toString(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Side B of Live Tile: Details flipping slide screen (Météo forecasts, Calendar appointments)
@Composable
fun TileBackState(
    tile: TileEntity,
    settings: SettingsEntity
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            when (tile.packageName) {
                "wp:weather" -> {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = tile.secondaryText ?: "22° Ensoleillé",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp,
                        maxLines = 2
                    )
                }
                "wp:calendar" -> {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = tile.secondaryText ?: "Pas de réunion",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp,
                        maxLines = 2
                    )
                }
                "wp:cortana" -> {
                    Icon(
                        imageVector = Icons.Default.Assistant,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Cortana : Cliquez pour discuter avec Gemini AI !",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 14.sp,
                        maxLines = 2
                    )
                }
                "wp:photos" -> {
                    // Show a beautiful solid quote on the photo side back
                    Icon(
                        imageVector = Icons.Default.Grain,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Windows Phone • Un vent de fraîcheur géométrique",
                        color = Color.White,
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        maxLines = 2,
                        fontWeight = FontWeight.Light
                    )
                }
                else -> {
                    // Fallback to title
                    Text(
                        text = tile.label,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tile.secondaryText ?: "Live Status Active",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// Icon mapper helper
fun getBuiltInIcon(packageName: String): ImageVector {
    return when (packageName) {
        "wp:phone" -> Icons.Default.Call
        "wp:sms" -> Icons.Default.Sms
        "wp:people" -> Icons.Default.People
        "wp:browser" -> Icons.Default.Language
        "wp:store" -> Icons.Default.ShoppingCart
        "wp:clock" -> Icons.Default.Alarm
        "wp:settings" -> Icons.Default.Settings
        "wp:weather" -> Icons.Default.WbSunny
        "wp:photos" -> Icons.Default.PhotoLibrary
        "wp:cortana" -> Icons.Default.Assistant
        "wp:calendar" -> Icons.Default.CalendarToday
        else -> Icons.Default.Apps
    }
}

// Tile Packing Algorithm
fun packTilesToRows(tiles: List<TileEntity>, maxSpan: Int): List<List<TileEntity>> {
    val result = mutableListOf<List<TileEntity>>()
    var currentRow = mutableListOf<TileEntity>()
    var currentSum = 0

    tiles.forEach { tile ->
        val span = getSpanForSize(tile.size)
        // If the tile cannot fit in the remainder of the current row, wrap!
        if (currentSum + span <= maxSpan) {
            currentRow.add(tile)
            currentSum += span
        } else {
            if (currentRow.isNotEmpty()) {
                result.add(currentRow)
            }
            currentRow = mutableListOf(tile)
            currentSum = span
        }
    }
    if (currentRow.isNotEmpty()) {
        result.add(currentRow)
    }
    return result
}

fun getSpanForSize(size: String): Int {
    return when (size) {
        "SMALL" -> 1
        "MEDIUM" -> 2
        "WIDE" -> 4 // Always 4 spans wide to allow beautiful standard grids
        else -> 2
    }
}
