package com.example

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val ARM_LABVM_URL = "http://labvm.arm.com/lab"

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val category: String = "Geral",
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val visitedAt: Long = System.currentTimeMillis()
)

@Dao
interface BrowserDao {
    @Query("SELECT * FROM bookmarks ORDER BY isPinned DESC, createdAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Update
    suspend fun updateBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteBookmarkByUrl(url: String)

    @Query("SELECT COUNT(*) FROM bookmarks")
    suspend fun countBookmarks(): Int

    @Query("SELECT * FROM history ORDER BY visitedAt DESC LIMIT 300")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: HistoryEntity)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM history")
    suspend fun clearAllHistory()
}

@Database(
    entities = [BookmarkEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun browserDao(): BrowserDao

    companion object {
        @Volatile
        private var INSTANCE: BrowserDatabase? = null

        fun getInstance(context: Context): BrowserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BrowserDatabase::class.java,
                    "labvm_browser_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

enum class SearchEngine(val label: String, val queryPrefix: String) {
    GOOGLE("Google", "https://www.google.com/search?q="),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q="),
    BING("Bing", "https://www.bing.com/search?q="),
    BRAVE("Brave Search", "https://search.brave.com/search?q=")
}

enum class UserAgentPreset(val label: String, val uaString: String?) {
    MOBILE("Android Padrão (Mobile)", null),
    DESKTOP_LINUX(
        "Desktop Linux (Ideal p/ LabVM)",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    ),
    DESKTOP_WINDOWS(
        "Desktop Windows 11",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    )
}

data class BrowserSettings(
    val homeUrl: String = ARM_LABVM_URL,
    val searchEngine: SearchEngine = SearchEngine.GOOGLE,
    val javaScriptEnabled: Boolean = true,
    val domStorageEnabled: Boolean = true,
    val desktopMode: Boolean = false,
    val userAgentPreset: UserAgentPreset = UserAgentPreset.MOBILE,
    val webRtcAutoGrantMedia: Boolean = true,
    val allowMixedContent: Boolean = true,
    val supportZoom: Boolean = true,
    val textZoomPercent: Int = 100,
    val saveHistory: Boolean = true,
    val acceptCookies: Boolean = true,
    val darkModeOverride: Boolean = true
)

data class BrowserTab(
    val id: String,
    val title: String = "ARM LabVM",
    val url: String = ARM_LABVM_URL,
    val isLoading: Boolean = false,
    val progress: Int = 0
)

data class ConsoleLogItem(
    val timestamp: Long = System.currentTimeMillis(),
    val level: String,
    val message: String,
    val sourceId: String
)

class BrowserRepository(
    private val dao: BrowserDao,
    private val prefs: SharedPreferences
) {
    val bookmarks: Flow<List<BookmarkEntity>> = dao.getAllBookmarks()
    val history: Flow<List<HistoryEntity>> = dao.getAllHistory()

    suspend fun ensureDefaultBookmarks() {
        if (dao.countBookmarks() == 0) {
            val defaults = listOf(
                BookmarkEntity(
                    title = "ARM LabVM Oficial",
                    url = ARM_LABVM_URL,
                    category = "LabVM & Cloud",
                    isPinned = true
                ),
                BookmarkEntity(
                    title = "ARM Developer Portal",
                    url = "https://developer.arm.com",
                    category = "LabVM & Cloud",
                    isPinned = true
                ),
                BookmarkEntity(
                    title = "WebRTC Trickle ICE Test",
                    url = "https://webrtc.github.io/samples/src/content/peerconnection/trickle-ice/",
                    category = "WebRTC",
                    isPinned = true
                ),
                BookmarkEntity(
                    title = "WebRTC Samples Hub",
                    url = "https://webrtc.github.io/samples/",
                    category = "WebRTC",
                    isPinned = false
                ),
                BookmarkEntity(
                    title = "HTML5 Test Score",
                    url = "https://html5test.co",
                    category = "Ferramentas",
                    isPinned = false
                ),
                BookmarkEntity(
                    title = "GitHub",
                    url = "https://github.com",
                    category = "Desenvolvimento",
                    isPinned = false
                )
            )
            defaults.forEach { dao.insertBookmark(it) }
        }
    }

    suspend fun saveBookmark(bookmark: BookmarkEntity) {
        if (bookmark.id == 0L) {
            dao.insertBookmark(bookmark)
        } else {
            dao.updateBookmark(bookmark)
        }
    }

    suspend fun deleteBookmark(id: Long) = dao.deleteBookmarkById(id)
    suspend fun deleteBookmarkByUrl(url: String) = dao.deleteBookmarkByUrl(url)

    suspend fun addHistory(title: String, url: String) {
        if (url.isBlank() || url.startsWith("data:") || url.startsWith("about:")) return
        dao.insertHistory(
            HistoryEntity(
                title = title.ifBlank { url },
                url = url,
                visitedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteHistoryItem(id: Long) = dao.deleteHistoryById(id)
    suspend fun clearHistory() = dao.clearAllHistory()

    fun loadSettings(): BrowserSettings {
        val engineName = prefs.getString("search_engine", SearchEngine.GOOGLE.name) ?: SearchEngine.GOOGLE.name
        val uaName = prefs.getString("ua_preset", UserAgentPreset.MOBILE.name) ?: UserAgentPreset.MOBILE.name
        return BrowserSettings(
            homeUrl = prefs.getString("home_url", ARM_LABVM_URL) ?: ARM_LABVM_URL,
            searchEngine = runCatching { SearchEngine.valueOf(engineName) }.getOrDefault(SearchEngine.GOOGLE),
            javaScriptEnabled = prefs.getBoolean("js_enabled", true),
            domStorageEnabled = prefs.getBoolean("dom_storage", true),
            desktopMode = prefs.getBoolean("desktop_mode", false),
            userAgentPreset = runCatching { UserAgentPreset.valueOf(uaName) }.getOrDefault(UserAgentPreset.MOBILE),
            webRtcAutoGrantMedia = prefs.getBoolean("webrtc_auto_grant", true),
            allowMixedContent = prefs.getBoolean("mixed_content", true),
            supportZoom = prefs.getBoolean("support_zoom", true),
            textZoomPercent = prefs.getInt("text_zoom", 100),
            saveHistory = prefs.getBoolean("save_history", true),
            acceptCookies = prefs.getBoolean("accept_cookies", true),
            darkModeOverride = prefs.getBoolean("dark_mode_override", true)
        )
    }

    fun saveSettings(settings: BrowserSettings) {
        prefs.edit()
            .putString("home_url", settings.homeUrl)
            .putString("search_engine", settings.searchEngine.name)
            .putBoolean("js_enabled", settings.javaScriptEnabled)
            .putBoolean("dom_storage", settings.domStorageEnabled)
            .putBoolean("desktop_mode", settings.desktopMode)
            .putString("ua_preset", settings.userAgentPreset.name)
            .putBoolean("webrtc_auto_grant", settings.webRtcAutoGrantMedia)
            .putBoolean("mixed_content", settings.allowMixedContent)
            .putBoolean("support_zoom", settings.supportZoom)
            .putInt("text_zoom", settings.textZoomPercent)
            .putBoolean("save_history", settings.saveHistory)
            .putBoolean("accept_cookies", settings.acceptCookies)
            .putBoolean("dark_mode_override", settings.darkModeOverride)
            .apply()
    }
}

class BrowserViewModel(private val repository: BrowserRepository) : ViewModel() {

    val bookmarks: StateFlow<List<BookmarkEntity>> = repository.bookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryEntity>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _settings = MutableStateFlow(repository.loadSettings())
    val settings: StateFlow<BrowserSettings> = _settings.asStateFlow()

    private val _tabs = MutableStateFlow(
        listOf(
            BrowserTab(
                id = "tab_1",
                title = "ARM LabVM",
                url = repository.loadSettings().homeUrl
            )
        )
    )
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow("tab_1")
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    private val _consoleLogs = MutableStateFlow<List<ConsoleLogItem>>(emptyList())
    val consoleLogs: StateFlow<List<ConsoleLogItem>> = _consoleLogs.asStateFlow()

    private val _webRtcEvents = MutableStateFlow<List<String>>(
        listOf("Motor WebRTC Inicializado • Suporte ICE/STUN/TURN e MediaStream prontos.")
    )
    val webRtcEvents: StateFlow<List<String>> = _webRtcEvents.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultBookmarks()
        }
    }

    fun updateSettings(transform: (BrowserSettings) -> BrowserSettings) {
        _settings.update { current ->
            val updated = transform(current)
            repository.saveSettings(updated)
            updated
        }
    }

    fun resetSettingsToDefaults() {
        val defaults = BrowserSettings()
        _settings.value = defaults
        repository.saveSettings(defaults)
    }

    fun resolveInputToUrl(rawInput: String): String {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return _settings.value.homeUrl
        if (trimmed.equals("labvm", ignoreCase = true) || trimmed.equals("arm lab", ignoreCase = true)) {
            return ARM_LABVM_URL
        }
        if (trimmed.startsWith("http://") ||
            trimmed.startsWith("https://") ||
            trimmed.startsWith("file://") ||
            trimmed.startsWith("data:") ||
            trimmed.startsWith("about:")
        ) {
            return trimmed
        }
        val looksLikeDomain = !trimmed.contains(" ") && (
            trimmed.contains(".") ||
                trimmed.startsWith("localhost") ||
                trimmed.matches(Regex("^\\d{1,3}(\\.\\d{1,3}){3}(:\\d+)?(/.*)?$"))
            )
        return if (looksLikeDomain) {
            if (trimmed.startsWith("labvm.arm.com")) "http://$trimmed" else "https://$trimmed"
        } else {
            _settings.value.searchEngine.queryPrefix + android.net.Uri.encode(trimmed)
        }
    }

    fun addNewTab(initialUrl: String = _settings.value.homeUrl, title: String = "Nova Aba") {
        val newId = "tab_${System.currentTimeMillis()}"
        val newTab = BrowserTab(id = newId, title = title, url = initialUrl)
        _tabs.update { it + newTab }
        _activeTabId.value = newId
    }

    fun selectTab(tabId: String) {
        if (_tabs.value.any { it.id == tabId }) {
            _activeTabId.value = tabId
        }
    }

    fun closeTab(tabId: String) {
        val currentList = _tabs.value
        if (currentList.size <= 1) {
            updateActiveTabUrl(_settings.value.homeUrl, "ARM LabVM")
            return
        }
        val updated = currentList.filterNot { it.id == tabId }
        _tabs.value = updated
        if (_activeTabId.value == tabId) {
            _activeTabId.value = updated.last().id
        }
    }

    fun updateActiveTabUrl(url: String, title: String? = null) {
        val activeId = _activeTabId.value
        _tabs.update { list ->
            list.map { tab ->
                if (tab.id == activeId) {
                    tab.copy(url = url, title = title ?: tab.title)
                } else {
                    tab
                }
            }
        }
    }

    fun updateTabState(tabId: String, title: String?, url: String?, isLoading: Boolean?, progress: Int?) {
        _tabs.update { list ->
            list.map { tab ->
                if (tab.id == tabId) {
                    tab.copy(
                        title = if (!title.isNullOrBlank()) title else tab.title,
                        url = if (!url.isNullOrBlank()) url else tab.url,
                        isLoading = isLoading ?: tab.isLoading,
                        progress = progress ?: tab.progress
                    )
                } else {
                    tab
                }
            }
        }
    }

    fun onPageFinishedVisited(title: String, url: String) {
        if (_settings.value.saveHistory) {
            viewModelScope.launch {
                repository.addHistory(title, url)
            }
        }
    }

    fun saveBookmark(id: Long = 0L, title: String, url: String, category: String, isPinned: Boolean) {
        viewModelScope.launch {
            repository.saveBookmark(
                BookmarkEntity(
                    id = id,
                    title = title.trim().ifBlank { url },
                    url = resolveInputToUrl(url),
                    category = category.trim().ifBlank { "Geral" },
                    isPinned = isPinned
                )
            )
        }
    }

    fun toggleBookmarkForCurrentPage(title: String, url: String) {
        viewModelScope.launch {
            val existing = bookmarks.value.firstOrNull { it.url.equals(url, ignoreCase = true) }
            if (existing != null) {
                repository.deleteBookmark(existing.id)
            } else {
                repository.saveBookmark(
                    BookmarkEntity(
                        title = title.ifBlank { url },
                        url = url,
                        category = if (url.contains("arm.com")) "LabVM & Cloud" else "Favoritos",
                        isPinned = url.contains("labvm.arm.com")
                    )
                )
            }
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch {
            repository.deleteBookmark(id)
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteHistoryItem(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun addConsoleLog(level: String, message: String, sourceId: String) {
        _consoleLogs.update { current ->
            (listOf(ConsoleLogItem(level = level, message = message, sourceId = sourceId)) + current).take(80)
        }
    }

    fun clearConsoleLogs() {
        _consoleLogs.value = emptyList()
    }

    fun logWebRtcEvent(event: String) {
        _webRtcEvents.update { current ->
            (listOf(event) + current).take(40)
        }
    }
}

class BrowserViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = BrowserDatabase.getInstance(context)
        val prefs = context.getSharedPreferences("labvm_browser_prefs", Context.MODE_PRIVATE)
        val repo = BrowserRepository(db.browserDao(), prefs)
        return BrowserViewModel(repo) as T
    }
}
