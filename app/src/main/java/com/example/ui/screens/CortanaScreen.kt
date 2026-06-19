package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SettingsEntity
import com.example.viewmodel.ActiveScreen
import com.example.viewmodel.CortanaMessage
import com.example.viewmodel.LauncherViewModel
import kotlinx.coroutines.launch

@Composable
fun CortanaScreen(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val chatMessages by viewModel.cortanaChat.collectAsState()
    val isThinking by viewModel.isCortanaThinking.collectAsState()

    val accentColor = Color(android.graphics.Color.parseColor(settings.accentColorHex))
    val isDark = settings.isDarkTheme

    var promptText by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Breathing Animation for Cortana Circle
    val infiniteTransition = rememberInfiniteTransition(label = "CortanaBreath")
    val circleScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CircleBreathScale"
    )

    // Scroll to bottom when message list updates
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            coroutineScope.launch {
                lazyListState.animateScrollToItem(chatMessages.size - 1)
            }
        }
    }

    // Cortana preset query ideas
    val suggestions = listOf(
        "Qui es-tu ?",
        "C'est quoi Windows Phone ?",
        "Raconte une blague",
        "Météo aujourd'hui ?"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) Color.Black else Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Row: Back, Title, Clear chats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                        text = settings.cortanaName.lowercase(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraLight,
                        color = if (isDark) Color.White else Color.Black
                    )
                }

                IconButton(
                    onClick = { viewModel.clearCortanaChat() }
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Vider la discussion",
                        tint = if (isDark) Color.LightGray else Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Pulsing Cortana Ring Centerpiece! (Breathing blue circle)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentAlignment = Alignment.Center
            ) {
                val outerColor = if (isThinking) Color(0xFF00D2FF) else accentColor
                
                Box(
                    modifier = Modifier
                        .size(100.dp * circleScale)
                        .clip(CircleShape)
                        .border(
                            width = if (isThinking) 6.dp else 4.dp,
                            brush = Brush.radialGradient(
                                colors = listOf(outerColor, outerColor.copy(alpha = 0.3f)),
                            ),
                            shape = CircleShape
                        )
                ) {
                    // Glowing background core
                    Box(
                        modifier = Modifier
                            .fillMaxSize(if (isThinking) 0.85f else 0.9f)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        outerColor.copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
                
                if (isThinking) {
                    Text(
                        text = "Réflexion...",
                        color = if (isDark) Color.White else Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Suggestions horizontal list (Badges)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestions.forEach { sug ->
                    Box(
                        modifier = Modifier
                            .background(
                                if (isDark) Color(0xFF222222) else Color(0xFFEEEEEE),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                promptText = ""
                                viewModel.sendCortanaPrompt(sug)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = sug,
                            color = if (isDark) Color.White else Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Conversation Chat Screen
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chatMessages) { message ->
                    CortanaChatBubble(message, settings, accentColor)
                }
                
                // Extra breathing space
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Bottom text field and mic icon
            Surface(
                color = if (isDark) Color(0xFF121212) else Color(0xFFF3F3F3),
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val presetPrompts = listOf(
                                "Conseille-moi un jeu Xbox culte sur Windows Phone",
                                "Pourquoi les tuiles Windows Phone étaient géniales ?",
                                "Quelle est ta couleur préférée ?"
                            )
                            promptText = presetPrompts.random()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Simuler la voix",
                            tint = accentColor
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        placeholder = { Text("Demander quelque chose à ${settings.cortanaName}...", color = Color.Gray, fontSize = 13.sp) },
                        maxLines = 3,
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

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (promptText.isNotBlank()) {
                                viewModel.sendCortanaPrompt(promptText)
                                promptText = ""
                            }
                        },
                        enabled = promptText.isNotBlank() && !isThinking,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (promptText.isNotBlank()) accentColor else Color.Gray.copy(alpha = 0.3f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Envoyer",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CortanaChatBubble(
    message: CortanaMessage,
    settings: SettingsEntity,
    accentColor: Color
) {
    val isUser = message.sender == "user"
    val isDark = settings.isDarkTheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Chat containers
        val bubbleColor = if (isUser) {
            if (isDark) Color(0xFF262626) else Color(0xFFE5E5E5)
        } else {
            accentColor.copy(alpha = 0.15f)
        }

        val textThemeColor = if (isUser) {
            if (isDark) Color.White else Color.Black
        } else {
            if (isDark) Color(0xFF4EE3FF) else accentColor
        }

        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isUser) 12.dp else 0.dp,
                bottomEnd = if (isUser) 0.dp else 12.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Name label
                Text(
                    text = if (isUser) "Moi" else settings.cortanaName,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = textThemeColor.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                // Text body
                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    color = if (isDark) Color.White else Color.Black,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
