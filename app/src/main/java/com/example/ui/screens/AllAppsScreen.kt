package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.SettingsEntity
import com.example.repository.AppItem
import com.example.ui.theme.parseAccent
import com.example.viewmodel.ActiveScreen
import com.example.viewmodel.LauncherViewModel
import kotlinx.coroutines.launch
import java.text.Normalizer

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AllAppsScreen(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val accentColor = parseAccent(settings.accentColorHex)
    val isDark = settings.isDarkTheme
    val isSearching = searchQuery.isNotBlank()

    // Filter apps based on search
    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }
    }

    // Group apps alphabetically (accent-folded so É/Ç/À file under their base letter).
    val groupedApps = remember(filteredApps) {
        filteredApps.groupBy { bucketOf(it.label) }
    }

    // List of active letters present
    val activeLetters = remember(groupedApps) {
        groupedApps.keys
    }

    // Jump List visibility
    var showJumpList by rememberSaveable { mutableStateOf(false) }

    // Floating scroll controller
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Map each group letter to its scroll index
    val letterToScrollIndexMap = remember(groupedApps) {
        val map = mutableMapOf<Char, Int>()
        var accumulatedIndex = 0
        
        // Loop through grouped apps sorted alphabetically
        val alphabet = ('A'..'Z').toList() + '#'
        for (char in alphabet) {
            val items = groupedApps[char]
            if (items != null) {
                map[char] = accumulatedIndex
                // Each group has: 1 header block + N child app listitems
                accumulatedIndex += 1 + items.size
            }
        }
        map
    }

    // Selected App for Context menu
    var selectedAppForPin by remember { mutableStateOf<AppItem?>(null) }

    // Back closes the inner-most overlay first (jump list / pin dialog) before the screen.
    BackHandler(enabled = showJumpList) { showJumpList = false }
    BackHandler(enabled = selectedAppForPin != null) { selectedAppForPin = null }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) Color.Black else Color.White)
            .pointerInput(Unit) {
                // WP: swipe RIGHT to slide back to the Start screen.
                var dx = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dx = 0f },
                    onDragEnd = {
                        if (dx > 60f) viewModel.setScreen(ActiveScreen.START)
                        dx = 0f
                    },
                    onDragCancel = { dx = 0f }
                ) { _, amount -> dx += amount }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Row: Back arrow and "applications"
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
                    text = "applications",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraLight,
                    color = if (isDark) Color.White else Color.Black
                )
            }

            // WP-styled search bar
            Surface(
                color = if (isDark) Color(0xFF151515) else Color(0xFFEEEEEE),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isDark) Color.Gray else Color.DarkGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Rechercher...", color = Color.Gray, fontSize = 14.sp) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = if (isDark) Color.White else Color.Black,
                            unfocusedTextColor = if (isDark) Color.White else Color.Black,
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.updateSearchQuery("") },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Effacer",
                                tint = if (isDark) Color.Gray else Color.DarkGray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            if (filteredApps.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucune application correspondante",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                // Scrollable Application List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    val sortedAlphabet = ('A'..'Z').toList() + '#'
                    
                    sortedAlphabet.forEach { char ->
                        val items = groupedApps[char]
                        if (items != null) {
                            // Section Alphabet Header (hidden while searching -> flat filtered list)
                            if (!isSearching) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .padding(vertical = 12.dp)
                                            .size(42.dp)
                                            .background(accentColor)
                                            .clickable(enabled = !isSearching) { showJumpList = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = char.toString(),
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Section Apps List
                            items(items.size) { appIndex ->
                                val app = items[appIndex]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { viewModel.launchAppItem(app) },
                                            onLongClick = { selectedAppForPin = app }
                                        )
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (app.icon != null) {
                                        Image(
                                            bitmap = app.icon.asImageBitmap(),
                                            contentDescription = app.label,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(0.dp))
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .background(if (isDark) Color(0xFF222222) else Color(0xFFDDDDDD)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = app.label.firstOrNull()?.uppercase() ?: "?",
                                                color = if (isDark) Color.White else Color.Black,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Text(
                                        text = app.label,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Light,
                                        color = if (isDark) Color.White else Color.Black,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        }

        // --- FULL JUMP LIST OVERLAY (Grille de lettres) ---
        if (showJumpList) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable { showJumpList = false } // Dismiss on background click
                    .systemBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "choisir une lettre",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraLight,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // A-Z + # Grid representation
                    val itemsList = ('A'..'Z').toList() + '#'
                    val columns = 4
                    val chunkSize = (itemsList.size + columns - 1) / columns

                    for (row in 0 until chunkSize) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (col in 0 until columns) {
                                val itemIndex = row * columns + col
                                if (itemIndex < itemsList.size) {
                                    val letter = itemsList[itemIndex]
                                    val hasApps = activeLetters.contains(letter)
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .background(if (hasApps) accentColor else Color(0xFF222222))
                                            .clickable(enabled = hasApps) {
                                                showJumpList = false
                                                val targetScrollIndex = letterToScrollIndexMap[letter]
                                                if (targetScrollIndex != null) {
                                                    coroutineScope.launch {
                                                        listState.animateScrollToItem(targetScrollIndex)
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = letter.toString(),
                                            color = if (hasApps) Color.White else Color.DarkGray,
                                            fontSize = 20.sp,
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

        // --- CONTEXT MENU DIALOG ---
        if (selectedAppForPin != null) {
            val app = selectedAppForPin!!
            AlertDialog(
                onDismissRequest = { selectedAppForPin = null },
                shape = RoundedCornerShape(0.dp), // Retro square layout
                containerColor = if (isDark) Color(0xFF1E1E1E) else Color.White,
                title = {
                    Text(
                        text = app.label,
                        color = if (isDark) Color.White else Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        text = "Voulez-vous épingler cette application à votre écran d'accueil Windows Phone ?",
                        color = if (isDark) Color.LightGray else Color.DarkGray
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.pinAppToStart(app)
                            selectedAppForPin = null
                        }
                    ) {
                        Text("ÉPINGLER", color = accentColor, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { selectedAppForPin = null }
                    ) {
                        Text("ANNULER", color = if (isDark) Color.White else Color.Black)
                    }
                }
            )
        }
    }
}

// Accent-folded alphabetical bucket so French accented labels (É, Ç, À...) file under
// their base letter instead of the '#' bucket.
private fun bucketOf(label: String): Char {
    val first = label.trim().firstOrNull() ?: return '#'
    val folded = Normalizer.normalize(first.toString(), Normalizer.Form.NFD)
        .firstOrNull()?.uppercaseChar() ?: '#'
    return if (folded in 'A'..'Z') folded else '#'
}
