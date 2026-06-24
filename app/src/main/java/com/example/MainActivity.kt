package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.SettingsEntity
import com.example.repository.LauncherRepository
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.WpStatusBarHeight
import com.example.ui.theme.parseAccent
import com.example.viewmodel.*
import java.util.*

class MainActivity : ComponentActivity(), android.speech.tts.TextToSpeech.OnInitListener {
    private var textToSpeech: android.speech.tts.TextToSpeech? = null

    // Live reference to the composed ViewModel so the Activity (onNewIntent) can drive it.
    private var launcherViewModel: LauncherViewModel? = null

    // ROLE_HOME (API 29+) request. On many OEMs this is a silent no-op, so we inspect the
    // result and, if the role was not granted, deep-link to the system "Default home app" screen.
    private val roleHomeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val granted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                (getSystemService(android.content.Context.ROLE_SERVICE) as android.app.role.RoleManager?)
                    ?.isRoleHeld(android.app.role.RoleManager.ROLE_HOME) == true
            } else false
            if (result.resultCode != RESULT_OK && !granted) {
                openHomeSettingsFallback()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = LauncherRepository(applicationContext, database.launcherDao)
        val factory = ViewModelFactory(application, repository)

        // TextToSpeech is initialised lazily on first speak (see ensureTts) — its service bind is
        // pure overhead on the Start cold-start path and is only needed when Cortana speaks.

        enableEdgeToEdge()
        hideSystemStatusBar()

        setContent {
            val viewModel: LauncherViewModel = viewModel(factory = factory)
            SideEffect { launcherViewModel = viewModel }

            val activeScreen by viewModel.activeScreen.collectAsState()
            val settings by viewModel.settings.collectAsState()
            val isNotificationCenterOpen by viewModel.isNotificationCenterOpen.collectAsState()
            val incomingNotification by viewModel.incomingNotification.collectAsState()
            val isLocked by viewModel.isLocked.collectAsState()
            val isLaunchingApp by viewModel.isLaunchingApp.collectAsState()

            val accentColor = remember(settings.accentColorHex) { parseAccent(settings.accentColorHex) }

            // Default-launcher prompt: re-evaluate on every resume (so it disappears the moment
            // the launcher becomes default) and only show it after a 24h cooldown once dismissed.
            var isDefault by remember { mutableStateOf(true) }
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) isDefault = isMyLauncherDefault()
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }
            var showDefaultLauncherDialog by remember { mutableStateOf(false) }
            LaunchedEffect(isDefault, settings.defaultPromptDismissedAt, isLocked, settings.isLockScreenEnabled) {
                val cooldownPassed =
                    System.currentTimeMillis() - settings.defaultPromptDismissedAt > 24L * 60 * 60 * 1000
                // Don't pop the dialog over the lock screen (its window would steal the unlock swipe).
                val unlocked = !(settings.isLockScreenEnabled && isLocked)
                showDefaultLauncherDialog = !isDefault && cooldownPassed && unlocked
            }

            // Performance-mode prompt: offered once (7-day cooldown) on modest hardware when the
            // mode is off and the launcher is unlocked.
            val lowEndDevice = remember { isLowEndDevice() }
            var showPerfDialog by remember { mutableStateOf(false) }
            LaunchedEffect(settings.performanceMode, settings.perfModePromptDismissedAt, isLocked, settings.isLockScreenEnabled) {
                val cooldownPassed =
                    System.currentTimeMillis() - settings.perfModePromptDismissedAt > 7L * 24 * 60 * 60 * 1000
                val unlocked = !(settings.isLockScreenEnabled && isLocked)
                showPerfDialog = lowEndDevice && !settings.performanceMode && cooldownPassed && unlocked
            }

            MyApplicationTheme(
                darkTheme = settings.isDarkTheme,
                accentColorHex = settings.accentColorHex
            ) {
                LaunchedEffect(Unit) {
                    viewModel.speakEvent.collect { text ->
                        ensureTts()
                        textToSpeech?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                }

                // System Back: close overlays first, then return to Start; never exit on Start.
                BackHandler(enabled = true) {
                    when {
                        isNotificationCenterOpen -> viewModel.setNotificationCenterOpen(false)
                        activeScreen != ActiveScreen.START -> viewModel.setScreen(ActiveScreen.START)
                        else -> { /* a launcher must not finish on Back from Start */ }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (settings.isDarkTheme) Color.Black else Color.White)
                ) {
                    // In-flow chrome: the WP status strip owns the top, content fills below it.
                    Column(modifier = Modifier.fillMaxSize()) {
                        TopStatusStrip(
                            settings = settings,
                            accentColor = accentColor,
                            onOpenActionCenter = { viewModel.setNotificationCenterOpen(true) }
                        )

                        Box(modifier = Modifier.fillMaxSize().weight(1f).navigationBarsPadding()) {
                            AnimatedContent(
                                targetState = activeScreen,
                                transitionSpec = {
                                    val from = initialState
                                    val to = targetState
                                    if (from == ActiveScreen.START && to == ActiveScreen.ALL_APPS) {
                                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it / 4 }
                                    } else if (from == ActiveScreen.ALL_APPS && to == ActiveScreen.START) {
                                        slideInHorizontally { -it / 4 } togetherWith slideOutHorizontally { it }
                                    } else {
                                        fadeIn() togetherWith fadeOut()
                                    }
                                },
                                label = "screen"
                            ) { screen ->
                                when (screen) {
                                    ActiveScreen.START -> StartScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                                    ActiveScreen.ALL_APPS -> AllAppsScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                                    ActiveScreen.SETTINGS -> SettingsScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                                    ActiveScreen.CORTANA -> CortanaScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }

                    // ACTION / NOTIFICATION CENTER SLIDING DRAWER OVERLAY
                    ActionCenter(
                        viewModel = viewModel,
                        isOpen = isNotificationCenterOpen,
                        onClose = { viewModel.setNotificationCenterOpen(false) }
                    )

                    // METRO FLOATING TOAST NOTIFICATION BANNER (just below the WP status strip)
                    if (incomingNotification != null && !isNotificationCenterOpen) {
                        Surface(
                            color = accentColor,
                            shape = RoundedCornerShape(0.dp), // strict tiles look
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = WpStatusBarHeight)
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

                    // DEFAULT LAUNCHER DIALOG (takes precedence over the perf prompt)
                    if (showDefaultLauncherDialog) {
                        MetroDefaultLauncherDialog(
                            isDark = settings.isDarkTheme,
                            accentColor = accentColor,
                            onDismiss = {
                                viewModel.markDefaultPromptDismissed()
                                showDefaultLauncherDialog = false
                            },
                            onConfirm = {
                                showDefaultLauncherDialog = false
                                launchDefaultHomeSetup()
                            }
                        )
                    } else if (showPerfDialog) {
                        PerformanceModeDialog(
                            isDark = settings.isDarkTheme,
                            accentColor = accentColor,
                            onDismiss = {
                                viewModel.markPerfModePromptDismissed()
                                showPerfDialog = false
                            },
                            onEnable = {
                                viewModel.setPerformanceMode(true)
                                showPerfDialog = false
                            }
                        )
                    }
                }
            }
        }
    }

    /** Hide the real Android status bar so only the Windows Phone strip is the top chrome. */
    private fun hideSystemStatusBar() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        // Sticky immersive: a top-edge swipe shows the bar transiently then it auto-hides, so it
        // does not compete with the WP status strip's pull-down-to-open-Action-Center gesture.
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.statusBars())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // The system re-shows the status bar after dialogs / the role prompt / returning from
        // another app, so re-assert the hide whenever we regain focus.
        if (hasFocus) hideSystemStatusBar()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // HOME pressed while we are already the running home app -> go back to a clean Start.
        launcherViewModel?.setScreen(ActiveScreen.START)
        launcherViewModel?.setNotificationCenterOpen(false)
    }

    private fun isMyLauncherDefault(): Boolean {
        // On API 29+ the role is the authoritative source of truth (matches launchDefaultHomeSetup).
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val rm = getSystemService(android.content.Context.ROLE_SERVICE) as android.app.role.RoleManager?
            if (rm != null && rm.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME)) {
                return rm.isRoleHeld(android.app.role.RoleManager.ROLE_HOME)
            }
        }
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
            val rm = getSystemService(android.content.Context.ROLE_SERVICE) as android.app.role.RoleManager?
            if (rm != null && rm.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME)) {
                if (rm.isRoleHeld(android.app.role.RoleManager.ROLE_HOME)) {
                    android.widget.Toast.makeText(this, "Ce launcher est déjà défini par défaut !", android.widget.Toast.LENGTH_SHORT).show()
                    return
                }
                try {
                    roleHomeLauncher.launch(rm.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME))
                } catch (e: Exception) {
                    openHomeSettingsFallback()
                }
            } else {
                openHomeSettingsFallback()
            }
        } else {
            openHomeSettingsFallback()
        }
    }

    /**
     * Deep-link to the system "Default home app" screen — the most reliable cross-OEM path on
     * API 23–28 and the fallback when the ROLE_HOME request is a no-op. The previous code fired
     * ACTION_MAIN+CATEGORY_HOME first, which merely re-launched the current home and never opened
     * any picker (and never threw, so the Settings deep-link was dead code).
     */
    private fun openHomeSettingsFallback() {
        val homeSettings = android.content.Intent(android.provider.Settings.ACTION_HOME_SETTINGS).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (homeSettings.resolveActivity(packageManager) != null) {
            try {
                startActivity(homeSettings)
                return
            } catch (e: Exception) { /* fall through to chooser */ }
        }
        // Last resort: a chooser over the HOME intent.
        val pickHome = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_HOME)
        }
        val chooser = android.content.Intent.createChooser(pickHome, "Choisir l'écran d'accueil").apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            startActivity(chooser)
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                this,
                "Impossible d'ouvrir les paramètres du lanceur par défaut. Allez dans Paramètres > Applications > Applications par défaut.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    /** Heuristic for "modest hardware" that benefits from the simplified animations. */
    private fun isLowEndDevice(): Boolean {
        val am = getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        if (am.isLowRamDevice) return true
        val mi = android.app.ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val lowRam = mi.totalMem in 1..(3L * 1024 * 1024 * 1024) // < 3 GB
        val fewCores = Runtime.getRuntime().availableProcessors() <= 4
        return lowRam && fewCores
    }

    private fun ensureTts() {
        if (textToSpeech == null) textToSpeech = android.speech.tts.TextToSpeech(this, this)
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

/** The Windows Phone status strip (+ pull-down-to-open Action Center). Hidden when the user
 *  disables it; when hidden, the launcher is fully immersive with no top chrome. */
@Composable
fun TopStatusStrip(
    settings: SettingsEntity,
    accentColor: Color,
    onOpenActionCenter: () -> Unit
) {
    if (!settings.showStatusBar) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                var accumulatedDrag = 0f
                detectVerticalDragGestures(
                    onDragStart = { accumulatedDrag = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        accumulatedDrag += dragAmount
                        change.consume()
                    },
                    onDragEnd = {
                        if (accumulatedDrag > 60f) onOpenActionCenter()
                        accumulatedDrag = 0f
                    },
                    onDragCancel = { accumulatedDrag = 0f }
                )
            }
    ) {
        StatusIndicatorBar(
            settings = settings,
            accentColor = accentColor,
            onClick = onOpenActionCenter
        )
        // Drag-open trigger handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(50.dp)
                .height(4.dp)
                .background(
                    color = if (settings.isDarkTheme) Color.DarkGray.copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)
                )
                .clickable { onOpenActionCenter() }
        )
    }
}

private fun currentHm(): String {
    val c = Calendar.getInstance()
    val h = c.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
    val m = c.get(Calendar.MINUTE).toString().padStart(2, '0')
    return "$h:$m"
}

@Composable
fun StatusIndicatorBar(
    settings: SettingsEntity,
    accentColor: Color,
    onClick: () -> Unit
) {
    if (!settings.showStatusBar) return

    // Live clock that flips on the minute boundary (the old one was captured once and froze).
    var clock by remember { mutableStateOf(currentHm()) }
    LaunchedEffect(Unit) {
        while (true) {
            clock = currentHm()
            val c = Calendar.getInstance()
            val msToNextMinute = (60 - c.get(Calendar.SECOND)) * 1000L - c.get(Calendar.MILLISECOND)
            kotlinx.coroutines.delay(msToNextMinute.coerceAtLeast(1000L))
        }
    }

    // Real battery charging state via a sticky broadcast — no permission, API 23 safe. Now that
    // this is the ONLY status bar, the indicators must reflect real device state.
    val context = LocalContext.current
    var charging by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val filter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: android.content.Context?, i: android.content.Intent?) {
                i ?: return
                val status = i.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
                charging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == android.os.BatteryManager.BATTERY_STATUS_FULL
            }
        }
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

    val iconTint = if (settings.isDarkTheme) Color.White else Color.Black

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(top = 4.dp)
            .padding(horizontal = 16.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left section: Signal indicators
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.SignalCellular4Bar,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "H+",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = iconTint
            )
        }

        // Right section: Wifi, battery, clock
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = if (charging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = clock,
                fontSize = 11.sp,
                color = iconTint,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
