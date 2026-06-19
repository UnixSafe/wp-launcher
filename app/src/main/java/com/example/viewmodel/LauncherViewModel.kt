package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import com.example.repository.AppItem
import com.example.repository.LauncherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ActiveScreen {
    START, ALL_APPS, SETTINGS, CORTANA
}

data class CortanaMessage(
    val sender: String, // "user" or "cortana"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class LauncherViewModel(
    application: Application,
    private val repository: LauncherRepository
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // Screen navigation
    private val _activeScreen = MutableStateFlow(ActiveScreen.START)
    val activeScreen: StateFlow<ActiveScreen> = _activeScreen.asStateFlow()

    // Notification center overlay visibility
    private val _isNotificationCenterOpen = MutableStateFlow(false)
    val isNotificationCenterOpen: StateFlow<Boolean> = _isNotificationCenterOpen.asStateFlow()

    // List of active tiles in the home screen
    val tiles: StateFlow<List<TileEntity>> = repository.allTiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings
    val settings: StateFlow<SettingsEntity> = repository.settings
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsEntity())

    // All installed apps
    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    val installedApps: StateFlow<List<AppItem>> = _installedApps.asStateFlow()

    // Query for app search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Edit Start screen mode
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    // Cortana (Gemini Support) Chat Flow
    private val _cortanaChat = MutableStateFlow<List<CortanaMessage>>(
        listOf(CortanaMessage("cortana", "Bonjour ! Je suis Cortana, votre assistante Windows Phone. Comment puis-je vous aider aujourd'hui ?"))
    )
    val cortanaChat: StateFlow<List<CortanaMessage>> = _cortanaChat.asStateFlow()

    private val _isCortanaThinking = MutableStateFlow(false)
    val isCortanaThinking: StateFlow<Boolean> = _isCortanaThinking.asStateFlow()

    // Live flip state: rotating index that changes periodically to animate tiles
    private val _liveFlipState = MutableStateFlow(0)
    val liveFlipState: StateFlow<Int> = _liveFlipState.asStateFlow()

    // Recent notification trigger for active banner previews (WP style breadcrumbs)
    private val _incomingNotification = MutableStateFlow<String?>(null)
    val incomingNotification: StateFlow<String?> = _incomingNotification.asStateFlow()

    // Speak events for Cortana French Speech Output (TTS)
    private val _speakEvent = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 1)
    val speakEvent = _speakEvent.asSharedFlow()

    // App Launching state
    private val _isLaunchingApp = MutableStateFlow<String?>(null)
    val isLaunchingApp: StateFlow<String?> = _isLaunchingApp.asStateFlow()

    // Lock Screen state
    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    fun unlock() {
        _isLocked.value = false
    }

    fun lock() {
        _isLocked.value = true
    }

    init {
        // Initialize default tiles if database is empty
        viewModelScope.launch {
            checkAndProvisionDefaultTiles()
            refreshAppsList()
            // Periodic Live Tiles heartbeat!
            launchLiveTilesHeartbeat()
        }
    }

    private suspend fun checkAndProvisionDefaultTiles() {
        // Build defaults
        val defaultTiles = listOf(
            TileEntity(packageName = "wp:phone", label = "Téléphone", size = "MEDIUM", position = 0, unreadCount = 1),
            TileEntity(packageName = "wp:sms", label = "Messages", size = "MEDIUM", position = 1, unreadCount = 4),
            TileEntity(packageName = "wp:people", label = "Contacts", size = "MEDIUM", position = 2),
            TileEntity(packageName = "wp:browser", label = "Internet", size = "MEDIUM", position = 3),
            TileEntity(packageName = "wp:cortana", label = "Cortana", size = "MEDIUM", position = 4, secondaryText = "Demandez-moi n'importe quoi"),
            TileEntity(packageName = "wp:clock", label = "Horloge", size = "SMALL", position = 5),
            TileEntity(packageName = "wp:settings", label = "Paramètres", size = "SMALL", position = 6),
            TileEntity(packageName = "wp:store", label = "Marketplace", size = "MEDIUM", position = 7),
            TileEntity(packageName = "wp:weather", label = "Météo", size = "MEDIUM", position = 8, secondaryText = "Paris • 22° / Ensoleillé"),
            TileEntity(packageName = "wp:photos", label = "Photos", size = "WIDE", position = 9),
            TileEntity(packageName = "wp:calendar", label = "Calendrier", size = "WIDE", position = 10, secondaryText = "14:00 • Design Windows Phone")
        )
        
        // Wait, check if database is empty by reading current values block
        withContext(Dispatchers.IO) {
            val current = repository.allTiles.firstOrNull() ?: emptyList()
            if (current.isEmpty()) {
                repository.insertTiles(defaultTiles)
            }
            val existingSettings = repository.getSettingsDirect()
            // Pre-provison settings if null
        }
    }

    private fun launchLiveTilesHeartbeat() {
        viewModelScope.launch {
            while (true) {
                delay(4000)
                _liveFlipState.value = (_liveFlipState.value + 1) % 4
                
                // Randomly trigger mock live update sometimes to prove dyn tiles are working
                if (_liveFlipState.value == 2) {
                    updateLiveTileContent()
                }
            }
        }
    }

    private suspend fun updateLiveTileContent() {
        val currentTiles = tiles.value
        val weatherTile = currentTiles.find { it.packageName == "wp:weather" }
        if (weatherTile != null) {
            val forecasts = listOf(
                "Paris • 19°C Nuageux",
                "Paris • 22°C Soleil",
                "Paris • 17°C Pluie légère",
                "Paris • 24°C Éclaircies"
            )
            val randomForecast = forecasts.random()
            repository.updateTile(weatherTile.copy(secondaryText = randomForecast))
        }

        val calendarTile = currentTiles.find { it.packageName == "wp:calendar" }
        if (calendarTile != null) {
            val events = listOf(
                "14:00 • Design Windows Phone",
                "16:30 • Boire un café",
                "Demain • Déploiement de l'App",
                "Aucun événement pour aujourd'hui"
            )
            val randomEvent = events.random()
            repository.updateTile(calendarTile.copy(secondaryText = randomEvent))
        }
    }

    // Load actual apps in launcher
    fun refreshAppsList() {
        viewModelScope.launch {
            val apps = repository.getInstalledApps()
            _installedApps.value = apps
        }
    }

    fun setScreen(screen: ActiveScreen) {
        _activeScreen.value = screen
        _isEditMode.value = false // Leave edit mode when leaving screen
    }

    fun setNotificationCenterOpen(isOpen: Boolean) {
        _isNotificationCenterOpen.value = isOpen
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setEditMode(isEdit: Boolean) {
        _isEditMode.value = isEdit
    }

    // Pin app to start
    fun pinAppToStart(app: AppItem) {
        viewModelScope.launch {
            val existing = tiles.value
            val maxPos = existing.maxOfOrNull { it.position } ?: -1
            
            // Avoid double pining
            if (existing.any { it.packageName == app.packageName }) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "${app.label} est déjà épinglé !", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val newTile = TileEntity(
                packageName = app.packageName,
                className = app.className,
                label = app.label,
                size = "MEDIUM",
                position = maxPos + 1,
                isCustomTile = false
            )
            repository.insertTile(newTile)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "${app.label} épinglé au début !", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Unpin tile
    fun unpinTile(tile: TileEntity) {
        viewModelScope.launch {
            repository.deleteTile(tile)
        }
    }

    // Resize tile (SMALL -> MEDIUM -> WIDE -> SMALL)
    fun cycleTileSize(tile: TileEntity) {
        viewModelScope.launch {
            val newSize = when (tile.size) {
                "SMALL" -> "MEDIUM"
                "MEDIUM" -> "WIDE"
                "WIDE" -> "SMALL"
                else -> "MEDIUM"
            }
            repository.updateTile(tile.copy(size = newSize))
        }
    }

    // Support dragging / tile reordering in position array
    fun moveTileUp(tile: TileEntity) {
        viewModelScope.launch {
            val current = tiles.value.toMutableList()
            val index = current.indexOfFirst { it.id == tile.id }
            if (index > 0) {
                // Swap position numbers
                val tmp = current[index]
                val prev = current[index - 1]
                
                repository.updateTile(tmp.copy(position = prev.position))
                repository.updateTile(prev.copy(position = tmp.position))
            }
        }
    }

    fun moveTileDown(tile: TileEntity) {
        viewModelScope.launch {
            val current = tiles.value.toMutableList()
            val index = current.indexOfFirst { it.id == tile.id }
            if (index != -1 && index < current.size - 1) {
                val tmp = current[index]
                val next = current[index + 1]

                repository.updateTile(tmp.copy(position = next.position))
                repository.updateTile(next.copy(position = tmp.position))
            }
        }
    }

    // Launch appropriate action
    fun launchTile(tile: TileEntity) {
        viewModelScope.launch {
            _isLaunchingApp.value = tile.label
            kotlinx.coroutines.delay(650) // Let the turnstile animation fly-in completely
            
            if (tile.packageName.startsWith("wp:")) {
                // Built-in actions
                when (tile.packageName) {
                    "wp:phone" -> launchSystemIntent(Intent(Intent.ACTION_DIAL))
                    "wp:sms" -> {
                        val defaultSmsPackage = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                            android.provider.Telephony.Sms.getDefaultSmsPackage(context)
                        } else {
                            null
                        }
                        val intent = if (defaultSmsPackage != null) {
                            context.packageManager.getLaunchIntentForPackage(defaultSmsPackage) ?: Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))
                        } else {
                            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))
                        }
                        launchSystemIntent(intent)
                    }
                    "wp:browser" -> launchSystemIntent(Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com")))
                    "wp:settings" -> setScreen(ActiveScreen.SETTINGS)
                    "wp:cortana" -> setScreen(ActiveScreen.CORTANA)
                    "wp:weather", "wp:photos", "wp:calendar", "wp:people" -> {
                        // Just show detail simulation or notice since it's a built-in widget,
                        // or let's notify the user about mock action!
                        Toast.makeText(context, "Tile dynamique ${tile.label} activée !", Toast.LENGTH_SHORT).show()
                    }
                    "wp:store" -> launchSystemIntent(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.android.settings")))
                }
            } else {
                // Actual Android App launch
                val pm = context.packageManager
                if (tile.packageName.isNotEmpty()) {
                    val intent = pm.getLaunchIntentForPackage(tile.packageName)
                    if (intent != null) {
                        launchSystemIntent(intent)
                    } else {
                        Toast.makeText(context, "Impossible de lancer ${tile.label}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            
            kotlinx.coroutines.delay(350) // Graceful duration before clearing
            _isLaunchingApp.value = null
        }
    }

    private fun launchSystemIntent(intent: Intent) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Erreur lors du lancement de l'application", Toast.LENGTH_SHORT).show()
        }
    }

    // Launch arbitrary app items
    fun launchAppItem(app: AppItem) {
        viewModelScope.launch {
            _isLaunchingApp.value = app.label
            kotlinx.coroutines.delay(650) // Let the turnstile animation fly-in completely
            
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                launchSystemIntent(intent)
            } else {
                Toast.makeText(context, "Impossible d'ouvrir l'application !", Toast.LENGTH_SHORT).show()
            }
            
            kotlinx.coroutines.delay(350)
            _isLaunchingApp.value = null
        }
    }

    // Settings adjustments
    fun updateAccent(name: String, hex: String) {
        viewModelScope.launch {
            val curr = settings.value
            repository.saveSettings(curr.copy(accentName = name, accentColorHex = hex))
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            val curr = settings.value
            repository.saveSettings(curr.copy(isDarkTheme = !curr.isDarkTheme))
        }
    }

    fun toggleColumns() {
        viewModelScope.launch {
            val curr = settings.value
            repository.saveSettings(curr.copy(useThreeColumns = !curr.useThreeColumns))
        }
    }

    fun toggleStatusBar() {
        viewModelScope.launch {
            val curr = settings.value
            repository.saveSettings(curr.copy(showStatusBar = !curr.showStatusBar))
        }
    }

    fun updateCortanaNickname(name: String) {
        viewModelScope.launch {
            val curr = settings.value
            repository.saveSettings(curr.copy(cortanaName = name))
        }
    }

    // Cortana / Gemini conversational capability
    fun sendCortanaPrompt(prompt: String) {
        if (prompt.trim().isEmpty()) return

        val userMsg = CortanaMessage("user", prompt)
        _cortanaChat.value = _cortanaChat.value + userMsg
        _isCortanaThinking.value = true

        viewModelScope.launch {
            val replyText = try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    // Fallback to beautiful local simulated responses if Key is not initialized yet
                    delay(1500)
                    "Je fonctionne en mode local. Configurez votre clé API Gemini dans le panneau 'Secrets' du studio pour débloquer ma pleine puissance Windows Phone !"
                } else {
                    // Prepare system instructions to act as Cortana from Windows Phone
                    val promptWithContext = "Tu es Cortana, l'assistante virtuelle de Windows Phone. Réponds de manière amicale, concise et avec une touche de l'esprit rétro de Windows Phone 8.1 / Windows 10 Mobile. L'utilisateur t'écrit: \"$prompt\""
                    
                    val request = GeminiRequest(
                        contents = listOf(
                            GeminiRequest.Content(
                                parts = listOf(GeminiRequest.Part(text = promptWithContext))
                            )
                        ),
                        generationConfig = GeminiRequest.GenerationConfig(temperature = 0.7f, maxOutputTokens = 250)
                    )

                    val response = GeminiClient.apiService.generateContent(apiKey, request)
                    response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                        ?: "Désolée, je n'ai pas pu générer de réponse actuellement."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                "Désolée, une erreur réseau s'est produite lors de ma tentative de connexion : ${e.localizedMessage}"
            }

            _isCortanaThinking.value = false
            _cortanaChat.value = _cortanaChat.value + CortanaMessage("cortana", replyText)
            if (settings.value.useCortanaVoice) {
                _speakEvent.emit(replyText)
            }
        }
    }

    fun toggleWallpaper() {
        viewModelScope.launch {
            val curr = settings.value
            repository.saveSettings(curr.copy(useWallpaperBackground = !curr.useWallpaperBackground))
        }
    }

    fun updateWallpaperName(name: String) {
        viewModelScope.launch {
            val curr = settings.value
            repository.saveSettings(curr.copy(wallpaperName = name))
        }
    }

    fun toggleCortanaVoice() {
        viewModelScope.launch {
            val curr = settings.value
            repository.saveSettings(curr.copy(useCortanaVoice = !curr.useCortanaVoice))
        }
    }

    fun toggleLockScreen() {
        viewModelScope.launch {
            val curr = settings.value
            repository.saveSettings(curr.copy(isLockScreenEnabled = !curr.isLockScreenEnabled))
        }
    }

    fun clearCortanaChat() {
        _cortanaChat.value = listOf(
            CortanaMessage("cortana", "Mémoire réinitialisée ! De quoi souhaitez-vous discuter ?")
        )
    }

    // Simulated alerts / trigger notification banners (WP style top floating overlay alert)
    fun simulateNotificationIncoming(appTitle: String, messageText: String) {
        viewModelScope.launch {
            _incomingNotification.value = "$appTitle • $messageText"
            
            // Increment corresponding tile count for realistic visual feedback
            val targetPkgPrfx = when (appTitle.lowercase()) {
                "téléphone", "phone" -> "wp:phone"
                "messages", "sms", "whatsapp" -> "wp:sms"
                "weather", "météo" -> "wp:weather"
                else -> ""
            }
            if (targetPkgPrfx.isNotEmpty()) {
                val tile = tiles.value.find { it.packageName == targetPkgPrfx }
                if (tile != null) {
                    repository.updateTile(tile.copy(unreadCount = tile.unreadCount + 1))
                }
            }

            delay(6000) // Keep banner visible for 6s just like Windows Phone toast alert!
            if (_incomingNotification.value == "$appTitle • $messageText") {
                _incomingNotification.value = null
            }
        }
    }

    fun dismissNotificationBanner() {
        _incomingNotification.value = null
    }

    fun resetMockNotifications() {
        viewModelScope.launch {
            val phone = tiles.value.find { it.packageName == "wp:phone" }
            if (phone != null) repository.updateTile(phone.copy(unreadCount = 0))
            
            val sms = tiles.value.find { it.packageName == "wp:sms" }
            if (sms != null) repository.updateTile(sms.copy(unreadCount = 0))

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Notifications réinitialisées", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun forceDefaultLayout() {
        viewModelScope.launch {
            repository.clearAllTiles()
            checkAndProvisionDefaultTiles()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Disposition Windows Phone d'origine restaurée", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

class ViewModelFactory(
    private val application: Application,
    private val repository: LauncherRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LauncherViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LauncherViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
