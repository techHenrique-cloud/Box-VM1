package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

enum class MainDestination(val label: String) {
    BROWSER("Navegador"),
    BOOKMARKS("Favoritos"),
    HISTORY("Histórico"),
    WEBRTC_LAB("WebRTC"),
    SETTINGS("Ajustes")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val intentUrl = intent?.dataString
        setContent {
            val context = LocalContext.current
            val viewModel: BrowserViewModel = viewModel(factory = BrowserViewModelFactory(context))
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            LaunchedEffect(intentUrl) {
                if (!intentUrl.isNullOrBlank()) {
                    viewModel.updateActiveTabUrl(intentUrl, "Link Externo")
                }
            }

            MyApplicationTheme(darkTheme = settings.darkModeOverride) {
                LabVmBrowserApp(viewModel = viewModel)
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LabVmBrowserApp(viewModel: BrowserViewModel) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
    val consoleLogs by viewModel.consoleLogs.collectAsStateWithLifecycle()
    val webRtcEvents by viewModel.webRtcEvents.collectAsStateWithLifecycle()

    val activeTab = remember(tabs, activeTabId) {
        tabs.firstOrNull { it.id == activeTabId } ?: tabs.first()
    }

    var currentDestination by remember { mutableStateOf(MainDestination.BROWSER) }
    var addressBarInput by remember(activeTab.url) { mutableStateOf(activeTab.url) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showFindInPageBar by remember { mutableStateOf(false) }
    var findInPageQuery by remember { mutableStateOf("") }
    var canGoBackState by remember { mutableStateOf(false) }
    var canGoForwardState by remember { mutableStateOf(false) }
    var pendingWebRtcRequest by remember { mutableStateOf<PermissionRequest?>(null) }

    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasCameraPerm = result[Manifest.permission.CAMERA] ?: hasCameraPerm
        hasAudioPerm = result[Manifest.permission.RECORD_AUDIO] ?: hasAudioPerm

        val req = pendingWebRtcRequest
        if (req != null) {
            try {
                req.grant(req.resources)
                viewModel.logWebRtcEvent("Permissões WebRTC concedidas após aprovação do usuário: ${req.resources.joinToString()}")
            } catch (e: Exception) {
                viewModel.logWebRtcEvent("Aviso ao conceder WebRTC: ${e.message}")
            }
            pendingWebRtcRequest = null
        }
    }

    val webViewMap = remember { mutableStateMapOf<String, WebView>() }
    val activeWebView = webViewMap[activeTab.id]

    // Cleanup WebViews when tabs are closed
    LaunchedEffect(tabs) {
        val validIds = tabs.map { it.id }.toSet()
        val toRemove = webViewMap.keys.filterNot { it in validIds }
        toRemove.forEach { removedId ->
            webViewMap.remove(removedId)?.destroy()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewMap.values.forEach { it.destroy() }
            webViewMap.clear()
        }
    }

    // Apply live settings to all active WebViews
    LaunchedEffect(settings, activeWebView) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(settings.acceptCookies)
        webViewMap.values.forEach { wv ->
            cookieManager.setAcceptThirdPartyCookies(wv, settings.acceptCookies)
            wv.settings.apply {
                javaScriptEnabled = settings.javaScriptEnabled
                domStorageEnabled = settings.domStorageEnabled
                databaseEnabled = settings.domStorageEnabled
                mediaPlaybackRequiresUserGesture = false
                javaScriptCanOpenWindowsAutomatically = true
                allowFileAccess = true
                allowContentAccess = true
                setSupportZoom(settings.supportZoom)
                builtInZoomControls = settings.supportZoom
                displayZoomControls = false
                textZoom = settings.textZoomPercent
                useWideViewPort = true
                loadWithOverviewMode = true
                mixedContentMode = if (settings.allowMixedContent) {
                    WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                } else {
                    WebSettings.MIXED_CONTENT_NEVER_ALLOW
                }
                userAgentString = when {
                    settings.desktopMode -> UserAgentPreset.DESKTOP_LINUX.uaString
                    settings.userAgentPreset.uaString != null -> settings.userAgentPreset.uaString
                    else -> WebSettings.getDefaultUserAgent(context)
                }
            }
        }
    }

    fun navigateToUrl(rawInput: String, openInNewTab: Boolean = false) {
        val targetUrl = viewModel.resolveInputToUrl(rawInput)
        addressBarInput = targetUrl
        currentDestination = MainDestination.BROWSER
        focusManager.clearFocus()
        if (openInNewTab) {
            viewModel.addNewTab(initialUrl = targetUrl, title = targetUrl)
        } else {
            viewModel.updateActiveTabUrl(targetUrl)
            webViewMap[activeTab.id]?.loadUrl(targetUrl)
        }
    }

    fun loadBuiltInWebRtcTest() {
        currentDestination = MainDestination.BROWSER
        addressBarInput = "webrtc://diagnostics-lab"
        viewModel.updateActiveTabUrl("webrtc://diagnostics-lab", "WebRTC Lab Test")
        webViewMap[activeTab.id]?.loadDataWithBaseURL(
            "https://webrtc.github.io/",
            WEBRTC_BUILT_IN_LAB_HTML,
            "text/html",
            "UTF-8",
            null
        )
        viewModel.logWebRtcEvent("Página de diagnóstico WebRTC + ICE local carregada.")
    }

    BackHandler(enabled = currentDestination != MainDestination.BROWSER || canGoBackState) {
        if (currentDestination != MainDestination.BROWSER) {
            currentDestination = MainDestination.BROWSER
        } else if (activeWebView?.canGoBack() == true) {
            activeWebView.goBack()
        }
    }

    val isCurrentBookmarked = remember(bookmarks, activeTab.url) {
        bookmarks.any { it.url.equals(activeTab.url, ignoreCase = true) }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = currentDestination == MainDestination.BROWSER,
                    onClick = { currentDestination = MainDestination.BROWSER },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (tabs.size > 1) {
                                    Badge { Text("${tabs.size}") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Language, contentDescription = "Navegador")
                        }
                    },
                    label = { Text("Navegador") },
                    modifier = Modifier.testTag("nav_tab_browser")
                )
                NavigationBarItem(
                    selected = currentDestination == MainDestination.BOOKMARKS,
                    onClick = { currentDestination = MainDestination.BOOKMARKS },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (bookmarks.isNotEmpty()) {
                                    Badge { Text("${bookmarks.size}") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Bookmark, contentDescription = "Favoritos")
                        }
                    },
                    label = { Text("Favoritos") },
                    modifier = Modifier.testTag("nav_tab_bookmarks")
                )
                NavigationBarItem(
                    selected = currentDestination == MainDestination.HISTORY,
                    onClick = { currentDestination = MainDestination.HISTORY },
                    icon = { Icon(Icons.Default.History, contentDescription = "Histórico") },
                    label = { Text("Histórico") },
                    modifier = Modifier.testTag("nav_tab_history")
                )
                NavigationBarItem(
                    selected = currentDestination == MainDestination.WEBRTC_LAB,
                    onClick = { currentDestination = MainDestination.WEBRTC_LAB },
                    icon = { Icon(Icons.Default.GraphicEq, contentDescription = "WebRTC Lab") },
                    label = { Text("WebRTC") },
                    modifier = Modifier.testTag("nav_tab_webrtc")
                )
                NavigationBarItem(
                    selected = currentDestination == MainDestination.SETTINGS,
                    onClick = { currentDestination = MainDestination.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                    label = { Text("Ajustes") },
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top Omni-Bar & Quick Controls (always visible in Browser mode)
            if (currentDestination == MainDestination.BROWSER) {
                BrowserTopHeader(
                    tabs = tabs,
                    activeTab = activeTab,
                    bookmarks = bookmarks,
                    addressBarInput = addressBarInput,
                    onAddressInputChange = { addressBarInput = it },
                    onSubmitUrl = { navigateToUrl(addressBarInput, false) },
                    canGoBack = canGoBackState,
                    canGoForward = canGoForwardState,
                    isBookmarked = isCurrentBookmarked,
                    showOverflowMenu = showOverflowMenu,
                    onDismissOverflow = { showOverflowMenu = false },
                    onOpenOverflow = { showOverflowMenu = true },
                    onBack = { if (activeWebView?.canGoBack() == true) activeWebView.goBack() },
                    onForward = { if (activeWebView?.canGoForward() == true) activeWebView.goForward() },
                    onReloadOrStop = {
                        if (activeTab.isLoading) activeWebView?.stopLoading() else activeWebView?.reload()
                    },
                    onGoHome = { navigateToUrl(settings.homeUrl, false) },
                    onOpenArmLabVm = { navigateToUrl(ARM_LABVM_URL, false) },
                    onToggleBookmark = {
                        viewModel.toggleBookmarkForCurrentPage(activeTab.title, activeTab.url)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                if (isCurrentBookmarked) "Removido dos favoritos" else "Adicionado aos favoritos!"
                            )
                        }
                    },
                    onSelectTab = { viewModel.selectTab(it) },
                    onCloseTab = { viewModel.closeTab(it) },
                    onNewTab = { viewModel.addNewTab(ARM_LABVM_URL, "ARM LabVM") },
                    onQuickBookmarkClick = { navigateToUrl(it, false) },
                    desktopMode = settings.desktopMode,
                    onToggleDesktopMode = {
                        viewModel.updateSettings { s -> s.copy(desktopMode = !s.desktopMode) }
                        activeWebView?.reload()
                    },
                    onToggleFindInPage = { showFindInPageBar = !showFindInPageBar },
                    onOpenWebRtcTest = { loadBuiltInWebRtcTest() },
                    onCopyUrl = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("URL", activeTab.url))
                        Toast.makeText(context, "URL copiada: ${activeTab.url}", Toast.LENGTH_SHORT).show()
                    },
                    onShareUrl = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, activeTab.url)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Compartilhar link"))
                    }
                )

                AnimatedVisibility(visible = showFindInPageBar) {
                    FindInPageToolbar(
                        query = findInPageQuery,
                        onQueryChange = {
                            findInPageQuery = it
                            activeWebView?.findAllAsync(it)
                        },
                        onNext = { activeWebView?.findNext(true) },
                        onPrevious = { activeWebView?.findNext(false) },
                        onClose = {
                            showFindInPageBar = false
                            findInPageQuery = ""
                            activeWebView?.clearMatches()
                        }
                    )
                }

                if (activeTab.isLoading) {
                    LinearProgressIndicator(
                        progress = { (activeTab.progress / 100f).coerceIn(0.05f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .testTag("webview_progress_bar"),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Main Content Area
            Box(modifier = Modifier.fillMaxSize()) {
                // Keep WebView alive inside composition so switching tabs/screens preserves session
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("browser_webview_container"),
                    factory = { ctx ->
                        createConfiguredWebView(
                            context = ctx,
                            tabId = activeTab.id,
                            initialUrl = activeTab.url,
                            settings = settings,
                            viewModel = viewModel,
                            onNavigationStateChange = { back, forward, currentUrl ->
                                canGoBackState = back
                                canGoForwardState = forward
                                if (!currentUrl.isNullOrBlank()) {
                                    addressBarInput = currentUrl
                                }
                            },
                            onWebRtcPermissionRequest = { request ->
                                if (settings.webRtcAutoGrantMedia && hasCameraPerm && hasAudioPerm) {
                                    try {
                                        request.grant(request.resources)
                                        viewModel.logWebRtcEvent("WebRTC Auto-Grant concedido: ${request.origin}")
                                    } catch (e: Exception) {
                                        viewModel.logWebRtcEvent("Erro Auto-Grant WebRTC: ${e.message}")
                                    }
                                } else {
                                    pendingWebRtcRequest = request
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.CAMERA,
                                            Manifest.permission.RECORD_AUDIO
                                        )
                                    )
                                }
                            }
                        ).also { created ->
                            webViewMap[activeTab.id] = created
                        }
                    },
                    update = { view ->
                        val desiredWebView = webViewMap[activeTab.id]
                        if (desiredWebView != null && view == desiredWebView) {
                            canGoBackState = view.canGoBack()
                            canGoForwardState = view.canGoForward()
                        }
                    }
                )

                // Overlay secondary screens cleanly when selected
                if (currentDestination != MainDestination.BROWSER) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when (currentDestination) {
                            MainDestination.BOOKMARKS -> BookmarksScreen(
                                bookmarks = bookmarks,
                                currentUrl = activeTab.url,
                                currentTitle = activeTab.title,
                                onOpenBookmark = { url, newTab -> navigateToUrl(url, newTab) },
                                onSaveBookmark = { id, title, url, cat, pinned ->
                                    viewModel.saveBookmark(id, title, url, cat, pinned)
                                },
                                onDeleteBookmark = { viewModel.deleteBookmark(it) }
                            )
                            MainDestination.HISTORY -> HistoryScreen(
                                history = history,
                                onOpenUrl = { url, newTab -> navigateToUrl(url, newTab) },
                                onDeleteEntry = { viewModel.deleteHistoryItem(it) },
                                onClearAll = { viewModel.clearAllHistory() }
                            )
                            MainDestination.WEBRTC_LAB -> WebRtcDiagnosticsScreen(
                                hasCameraPermission = hasCameraPerm,
                                hasAudioPermission = hasAudioPerm,
                                webRtcAutoGrant = settings.webRtcAutoGrantMedia,
                                webRtcEvents = webRtcEvents,
                                consoleLogs = consoleLogs,
                                onRequestPermissions = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.CAMERA,
                                            Manifest.permission.RECORD_AUDIO
                                        )
                                    )
                                },
                                onLoadBuiltInWebRtcPage = { loadBuiltInWebRtcTest() },
                                onOpenExternalUrl = { navigateToUrl(it, false) },
                                onClearConsole = { viewModel.clearConsoleLogs() }
                            )
                            MainDestination.SETTINGS -> BrowserSettingsScreen(
                                settings = settings,
                                onUpdateSettings = { viewModel.updateSettings(it) },
                                onResetSettings = {
                                    viewModel.resetSettingsToDefaults()
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Configurações restauradas para o padrão!")
                                    }
                                },
                                onClearWebViewData = {
                                    webViewMap.values.forEach { wv ->
                                        wv.clearCache(true)
                                        wv.clearHistory()
                                        wv.clearFormData()
                                    }
                                    CookieManager.getInstance().removeAllCookies(null)
                                    WebStorage.getInstance().deleteAllData()
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Cache, Cookies e WebStorage limpos com sucesso!")
                                    }
                                }
                            )
                            MainDestination.BROWSER -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowserTopHeader(
    tabs: List<BrowserTab>,
    activeTab: BrowserTab,
    bookmarks: List<BookmarkEntity>,
    addressBarInput: String,
    onAddressInputChange: (String) -> Unit,
    onSubmitUrl: () -> Unit,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isBookmarked: Boolean,
    showOverflowMenu: Boolean,
    onDismissOverflow: () -> Unit,
    onOpenOverflow: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReloadOrStop: () -> Unit,
    onGoHome: () -> Unit,
    onOpenArmLabVm: () -> Unit,
    onToggleBookmark: () -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: () -> Unit,
    onQuickBookmarkClick: (String) -> Unit,
    desktopMode: Boolean,
    onToggleDesktopMode: () -> Unit,
    onToggleFindInPage: () -> Unit,
    onOpenWebRtcTest: () -> Unit,
    onCopyUrl: () -> Unit,
    onShareUrl: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Multi-Tab Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tabs.forEach { tab ->
                    val isSelected = tab.id == activeTab.id
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier
                            .clickable { onSelectTab(tab.id) }
                            .testTag("browser_tab_${tab.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (tab.url.contains("arm.com")) Icons.Default.Computer else Icons.Default.Language,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.title.take(18),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                            IconButton(
                                onClick = { onCloseTab(tab.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fechar aba",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
                IconButton(
                    onClick = onNewTab,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("add_new_tab_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nova aba")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Omni Address Bar + Navigation Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    enabled = canGoBack,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("browser_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                }
                IconButton(
                    onClick = onForward,
                    enabled = canGoForward,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("browser_forward_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Avançar")
                }
                IconButton(
                    onClick = onGoHome,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("browser_home_button")
                ) {
                    Icon(Icons.Default.Home, contentDescription = "Página Inicial")
                }

                val isHttps = activeTab.url.startsWith("https://")
                OutlinedTextField(
                    value = addressBarInput,
                    onValueChange = onAddressInputChange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("url_address_bar"),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    placeholder = {
                        Text(
                            text = ARM_LABVM_URL,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isHttps) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (isHttps) "Conexão HTTPS" else "Conexão HTTP LabVM",
                            tint = if (isHttps) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onToggleBookmark,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("quick_toggle_bookmark_button")
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Favoritar página",
                                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = onReloadOrStop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("browser_reload_button")
                            ) {
                                Icon(
                                    imageVector = if (activeTab.isLoading) Icons.Default.Close else Icons.Default.Refresh,
                                    contentDescription = "Recarregar"
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(onGo = { onSubmitUrl() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Box {
                    IconButton(
                        onClick = onOpenOverflow,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("browser_overflow_menu_button")
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Mais opções")
                    }
                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = onDismissOverflow
                    ) {
                        DropdownMenuItem(
                            text = { Text("Abrir ARM LabVM (http://labvm.arm.com/lab)") },
                            leadingIcon = { Icon(Icons.Default.Computer, contentDescription = null) },
                            onClick = {
                                onDismissOverflow()
                                onOpenArmLabVm()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (desktopMode) "Modo Desktop: ATIVO" else "Ativar Modo Desktop") },
                            leadingIcon = { Icon(Icons.Default.Computer, contentDescription = null) },
                            onClick = {
                                onDismissOverflow()
                                onToggleDesktopMode()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Localizar na Página") },
                            leadingIcon = { Icon(Icons.Default.FindInPage, contentDescription = null) },
                            onClick = {
                                onDismissOverflow()
                                onToggleFindInPage()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Diagnóstico WebRTC Integrado") },
                            leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = null) },
                            onClick = {
                                onDismissOverflow()
                                onOpenWebRtcTest()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Copiar Endereço URL") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                onDismissOverflow()
                                onCopyUrl()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Compartilhar Link") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                onDismissOverflow()
                                onShareUrl()
                            }
                        )
                    }
                }
            }

            // Pinned Bookmarks & Quick ARM LabVM Pill Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = activeTab.url.contains("labvm.arm.com"),
                    onClick = onOpenArmLabVm,
                    label = {
                        Text(
                            text = "ARM LabVM (http://labvm.arm.com/lab)",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("quick_chip_arm_labvm")
                )

                bookmarks.filter { it.isPinned && !it.url.equals(ARM_LABVM_URL, ignoreCase = true) }
                    .forEach { pinned ->
                        FilterChip(
                            selected = activeTab.url.equals(pinned.url, ignoreCase = true),
                            onClick = { onQuickBookmarkClick(pinned.url) },
                            label = { Text(pinned.title.take(22)) }
                        )
                    }
            }
        }
    }
}

@Composable
private fun FindInPageToolbar(
    query: String,
    onQueryChange: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Buscar texto na página...") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onPrevious) { Text("Ant") }
            TextButton(onClick = onNext) { Text("Próx") }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Fechar busca")
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createConfiguredWebView(
    context: Context,
    tabId: String,
    initialUrl: String,
    settings: BrowserSettings,
    viewModel: BrowserViewModel,
    onNavigationStateChange: (Boolean, Boolean, String?) -> Unit,
    onWebRtcPermissionRequest: (PermissionRequest) -> Unit
): WebView {
    return WebView(context).apply {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(settings.acceptCookies)
        cookieManager.setAcceptThirdPartyCookies(this, settings.acceptCookies)

        this.settings.apply {
            javaScriptEnabled = settings.javaScriptEnabled
            domStorageEnabled = settings.domStorageEnabled
            databaseEnabled = settings.domStorageEnabled
            mediaPlaybackRequiresUserGesture = false
            javaScriptCanOpenWindowsAutomatically = true
            allowFileAccess = true
            allowContentAccess = true
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(settings.supportZoom)
            builtInZoomControls = settings.supportZoom
            displayZoomControls = false
            textZoom = settings.textZoomPercent
            mixedContentMode = if (settings.allowMixedContent) {
                WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            } else {
                WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }
            userAgentString = when {
                settings.desktopMode -> UserAgentPreset.DESKTOP_LINUX.uaString
                settings.userAgentPreset.uaString != null -> settings.userAgentPreset.uaString
                else -> WebSettings.getDefaultUserAgent(context)
            }
        }

        webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                viewModel.updateTabState(
                    tabId = tabId,
                    title = view?.title,
                    url = url,
                    isLoading = true,
                    progress = 15
                )
                onNavigationStateChange(
                    view?.canGoBack() ?: false,
                    view?.canGoForward() ?: false,
                    url
                )
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val finalTitle = view?.title?.takeIf { it.isNotBlank() } ?: url ?: "ARM LabVM"
                viewModel.updateTabState(
                    tabId = tabId,
                    title = finalTitle,
                    url = url,
                    isLoading = false,
                    progress = 100
                )
                if (!url.isNullOrBlank()) {
                    viewModel.onPageFinishedVisited(finalTitle, url)
                }
                onNavigationStateChange(
                    view?.canGoBack() ?: false,
                    view?.canGoForward() ?: false,
                    url
                )
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    viewModel.addConsoleLog(
                        level = "WARN",
                        message = "Aviso de rede ao carregar ${request.url}: ${error?.description}",
                        sourceId = request.url.toString()
                    )
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val targetUri: Uri = request?.url ?: return false
                val scheme = targetUri.scheme?.lowercase() ?: ""
                return if (scheme == "http" || scheme == "https" || scheme == "data" || scheme == "about") {
                    false
                } else {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, targetUri))
                    }
                    true
                }
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                viewModel.updateTabState(
                    tabId = tabId,
                    title = view?.title,
                    url = view?.url,
                    isLoading = newProgress < 100,
                    progress = newProgress
                )
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                viewModel.updateTabState(
                    tabId = tabId,
                    title = title,
                    url = view?.url,
                    isLoading = null,
                    progress = null
                )
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                if (request == null) return
                viewModel.logWebRtcEvent(
                    "Solicitação WebRTC de ${request.origin}: ${request.resources.joinToString()}"
                )
                onWebRtcPermissionRequest(request)
            }

            override fun onPermissionRequestCanceled(request: PermissionRequest?) {
                super.onPermissionRequestCanceled(request)
                viewModel.logWebRtcEvent("Solicitação WebRTC cancelada pela origem: ${request?.origin}")
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                callback?.invoke(origin, true, false)
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                if (consoleMessage != null) {
                    viewModel.addConsoleLog(
                        level = consoleMessage.messageLevel().name,
                        message = consoleMessage.message() ?: "",
                        sourceId = "${consoleMessage.sourceId()}:${consoleMessage.lineNumber()}"
                    )
                }
                return true
            }
        }

        loadUrl(initialUrl)
    }
}
