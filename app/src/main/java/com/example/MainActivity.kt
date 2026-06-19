package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.SettingsEntity
import com.example.repository.LauncherRepository
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.*
import java.util.*

class MainActivity : ComponentActivity(), android.speech.tts.TextToSpeech.OnInitListener {
    private var textToSpeech: android.speech.tts.TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = LauncherRepository(applicationContext, database.launcherDao)
        val factory = ViewModelFactory(application, repository)
        
        textToSpeech = android.speech.tts.TextToSpeech(this, this)
        
        enableEdgeToEdge()
        setContent {
            val viewModel: LauncherViewModel = viewModel(factory = factory)
            
            val activeScreen by viewModel.activeScreen.collectAsState()
            val settings by viewModel.settings.collectAsState()
            val isNotificationCenterOpen by viewModel.isNotificationCenterOpen.collectAsState()
            val incomingNotification by viewModel.incomingNotification.collectAsState()
            val isLocked by viewModel.isLocked.collectAsState()
            val isLaunchingApp by viewModel.isLaunchingApp.collectAsState()
            
            val accentColor = Color(android.graphics.Color.parseColor(settings.accentColorHex))

            var showDefaultLauncherDialog by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                if (!isMyLauncherDefault()) {
                    showDefaultLauncherDialog = true
                }
            }

            MyApplicationTheme(darkTheme = settings.isDarkTheme) {
                LaunchedEffect(Unit) {
                    viewModel.speakEvent.collect { text ->
                        textToSpeech?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (settings.isDarkTheme) Color.Black else Color.White)
                ) {
                    // MAIN CONTENT SCREEN SWITCHER
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (activeScreen) {
                            ActiveScreen.START -> StartScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                            ActiveScreen.ALL_APPS -> AllAppsScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                            ActiveScreen.SETTINGS -> SettingsScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                            ActiveScreen.CORTANA -> CortanaScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                        }
                    }

                    // METRO STYLE STATUS BAR (DRAG HANDLE AT THE VERY TOP)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .pointerInput(Unit) {
                                var accumulatedDrag = 0f
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        if (accumulatedDrag > 15f) {
                                            viewModel.setNotificationCenterOpen(true)
                                        }
                                        accumulatedDrag = 0f
                                    },
                                    onDragCancel = {
                                        accumulatedDrag = 0f
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        accumulatedDrag += dragAmount
                                        if (accumulatedDrag > 60f) {
                                            viewModel.setNotificationCenterOpen(true)
                                        }
                                    }
                                )
                            }
                    ) {
                        StatusIndicatorBar(
                            settings = settings,
                            accentColor = accentColor,
                            onClick = { viewModel.setNotificationCenterOpen(true) }
                        )
                        
                        // Drag open trigger handle
                        if (settings.showStatusBar && !isNotificationCenterOpen) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .width(50.dp)
                                    .height(4.dp)
                                    .background(
                                        color = if (settings.isDarkTheme) Color.DarkGray.copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)
                                    )
                                    .clickable { viewModel.setNotificationCenterOpen(true) }
                             )
                        }
                    }

                    // ACTION / NOTIFICATION CENTER SLIDING DRAWER OVERLAY
                    ActionCenter(
                        viewModel = viewModel,
                        isOpen = isNotificationCenterOpen,
                        onClose = { viewModel.setNotificationCenterOpen(false) }
                    )

                    // METRO FLOATING ACTIVE OUTGOING TOAST NOTIFICATION BANNER
                    if (incomingNotification != null && !isNotificationCenterOpen) {
                        Surface(
                            color = accentColor,
                            shape = RoundedCornerShape(0.dp), // strict tiles look
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .align(Alignment.TopCenter)
                                .clickable {
                                    viewModel.setNotificationCenterOpen(true)
                                    viewModel.dismissNotificationBanner()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = incomingNotification!!.substringBefore(" • ").uppercase(Locale.ROOT),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = incomingNotification!!.substringAfter(" • "),
                                            fontSize = 13.sp,
                                            color = Color.White,
                                            maxLines = 1,
                                            fontWeight = FontWeight.Light
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.dismissNotificationBanner() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "OK",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    // LOCK SCREEN OVERLAY
                    if (settings.isLockScreenEnabled && isLocked) {
                        LockScreen(
                            viewModel = viewModel,
                            onUnlock = { viewModel.unlock() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // APP OPENING OVERLAY
                    isLaunchingApp?.let { appName ->
                        LauncherAppOpenOverlay(
                            appName = appName,
                            accentColor = accentColor,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // DEFAULT LAUNCHER DIALOG
                    if (showDefaultLauncherDialog) {
                        MetroDefaultLauncherDialog(
                            isDark = settings.isDarkTheme,
                            accentColor = accentColor,
                            onDismiss = { showDefaultLauncherDialog = false },
                            onConfirm = {
                                showDefaultLauncherDialog = false
                                launchDefaultHomeSetup()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun isMyLauncherDefault(): Boolean {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_HOME)
        }
        val resolveInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            packageManager.resolveActivity(
                intent,
                android.content.pm.PackageManager.ResolveInfoFlags.of(android.content.pm.PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        }
        return resolveInfo?.activityInfo?.packageName == packageName
    }

    private fun launchDefaultHomeSetup() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(android.content.Context.ROLE_SERVICE) as android.app.role.RoleManager?
            if (roleManager != null && roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME)) {
                if (!roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_HOME)) {
                    val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME)
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        fallbackDefaultHomeSetup()
                    }
                } else {
                    android.widget.Toast.makeText(this, "Ce launcher est déjà défini par défaut !", android.widget.Toast.LENGTH_SHORT).show()
                }
            } else {
                fallbackDefaultHomeSetup()
            }
        } else {
            fallbackDefaultHomeSetup()
        }
    }

    private fun fallbackDefaultHomeSetup() {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                addCategory(android.content.Intent.CATEGORY_HOME)
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = android.content.Intent(android.provider.Settings.ACTION_HOME_SETTINGS).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            } catch (ex: Exception) {
                android.widget.Toast.makeText(this, "Impossible d'ouvrir les paramètres du lanceur par défaut.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == android.speech.tts.TextToSpeech.SUCCESS) {
            textToSpeech?.language = java.util.Locale.FRENCH
        }
    }

    override fun onDestroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onDestroy()
    }
}

@Composable
fun StatusIndicatorBar(
    settings: SettingsEntity,
    accentColor: Color,
    onClick: () -> Unit
) {
    if (!settings.showStatusBar) return

    val currentClockText = remember {
        val calendar = Calendar.getInstance()
        var hours = calendar.get(Calendar.HOUR_OF_DAY).toString()
        var minutes = calendar.get(Calendar.MINUTE).toString()
        if (hours.length == 1) hours = "0$hours"
        if (minutes.length == 1) minutes = "0$minutes"
        "$hours:$minutes"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left section: Signal indicators
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.SignalCellular4Bar,
                contentDescription = null,
                tint = if (settings.isDarkTheme) Color.White else Color.Black,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "H+",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (settings.isDarkTheme) Color.White else Color.Black
            )
        }

        // Right section: Wifi, battery, details
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = null,
                tint = if (settings.isDarkTheme) Color.White else Color.Black,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.BatteryChargingFull,
                contentDescription = null,
                tint = if (settings.isDarkTheme) Color.White else Color.Black,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = currentClockText,
                fontSize = 11.sp,
                color = if (settings.isDarkTheme) Color.White else Color.Black,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
