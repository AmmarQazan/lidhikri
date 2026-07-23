package com.greendome.adhkar

import android.content.Intent
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import com.greendome.adhkar.ui.components.AzkarNavIcon
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.greendome.adhkar.data.AzkarFavorites
import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.data.local.DhikrEntity
import com.greendome.adhkar.data.local.ReciterEntity
import com.greendome.adhkar.data.model.DhikrCategory
import com.greendome.adhkar.service.AdhkarReminderService
import com.greendome.adhkar.service.SilentNotificationChannels
import com.greendome.adhkar.ui.MainViewModel
import com.greendome.adhkar.ui.screens.AddEditDhikrScreen
import com.greendome.adhkar.ui.screens.AppSplashScreen
import com.greendome.adhkar.ui.screens.AdminAzkarCollectionsScreen
import com.greendome.adhkar.ui.screens.AdminAzkarItemsScreen
import com.greendome.adhkar.ui.screens.AdminDhikrBrowseScreen
import com.greendome.adhkar.ui.screens.AdminDhikrBucket
import com.greendome.adhkar.ui.screens.AdminEditAzkarCollectionScreen
import com.greendome.adhkar.ui.screens.AdminEditAzkarItemScreen
import com.greendome.adhkar.ui.screens.AdminEditReciterScreen
import com.greendome.adhkar.ui.screens.AdminReciterAudioScreen
import com.greendome.adhkar.ui.screens.AdminRecitersScreen
import com.greendome.adhkar.ui.screens.AdminScreen
import com.greendome.adhkar.ui.screens.AzkarCollectionScreen
import com.greendome.adhkar.ui.screens.AzkarHubScreen
import com.greendome.adhkar.ui.screens.MyDhikrScreen
import com.greendome.adhkar.ui.screens.HomeScreen
import com.greendome.adhkar.ui.screens.MisbahaScreen
import com.greendome.adhkar.ui.components.MisbahaNavIcon
import com.greendome.adhkar.ui.screens.SettingsScreen
import com.greendome.adhkar.ui.screens.OnboardingFlow
import com.greendome.adhkar.ui.screens.OnboardingResult
import com.greendome.adhkar.ui.theme.AppArabicFont
import com.greendome.adhkar.ui.theme.AppFontScale
import com.greendome.adhkar.data.model.ArabicFontStyle
import com.greendome.adhkar.data.model.NumberDigitStyle
import com.greendome.adhkar.ui.theme.LocalNumberDigitStyle
import com.greendome.adhkar.ui.theme.GoldDome
import com.greendome.adhkar.ui.theme.GreenDomeTheme
import com.greendome.adhkar.ui.theme.GreenPrimary
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.greendome.adhkar.audio.DhikrAudioPlayer
import com.greendome.adhkar.audio.DhikrPlaybackResolver
import com.greendome.adhkar.audio.playResolved
import com.greendome.adhkar.util.LocaleHelper
import com.greendome.adhkar.util.RuntimePermissions
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val holdSystemSplash = mutableStateOf(true)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapWithSavedLanguage(newBase))
    }

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { holdSystemSplash.value }
        super.onCreate(savedInstanceState)
        SilentNotificationChannels.ensureCreated(this)
        SilentNotificationChannels.cancelDhikrAlerts(this)
        setContent {
            val settings = viewModel.settingsRepo()
            val lang = settings.appLanguage
            var themeMode by remember { mutableStateOf(settings.appThemeMode) }
            var showAppSplash by remember { mutableStateOf(true) }
            var showOnboarding by remember { mutableStateOf(!settings.onboardingCompleted) }
            var arabicFontStyle by remember { mutableStateOf(settings.arabicFontStyle) }
            var pendingOnboardingServiceEnable by remember { mutableStateOf(false) }
            val layoutDirection = if (lang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
            val splashDurationMs = if (showOnboarding) 900L else 1400L

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { _ ->
                if (pendingOnboardingServiceEnable) {
                    pendingOnboardingServiceEnable = false
                    startAutoTasbih()
                }
            }

            fun enableDefaultAutoFeatures() {
                settings.autoAzkarEnabled = true
                settings.isServiceEnabled = true
                val permission = RuntimePermissions.postNotificationsPermission()
                if (permission != null && !RuntimePermissions.hasPostNotifications(this)) {
                    pendingOnboardingServiceEnable = true
                    notificationPermissionLauncher.launch(permission)
                } else {
                    startAutoTasbih()
                }
            }

            fun finishOnboarding(result: OnboardingResult) {
                settings.appLanguage = result.language
                settings.arabicFontStyle = result.arabicFontStyle
                settings.appThemeMode = result.themeMode
                settings.onboardingStep = 0
                settings.onboardingCompleted = true
                arabicFontStyle = result.arabicFontStyle
                themeMode = result.themeMode
                showOnboarding = false
                enableDefaultAutoFeatures()
                if (result.language != lang) recreate()
            }

            LaunchedEffect(splashDurationMs) {
                withFrameNanos { }
                holdSystemSplash.value = false
                kotlinx.coroutines.delay(splashDurationMs)
                showAppSplash = false
            }

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                GreenDomeTheme(themeMode = themeMode) {
                    AppArabicFont(arabicFontStyle) {
                        Box(Modifier.fillMaxSize()) {
                            if (!showOnboarding) {
                                AdhkarApp(
                                    viewModel,
                                    onLanguageChanged = { recreate() },
                                    onThemeModeChanged = { themeMode = settings.appThemeMode },
                                    onArabicFontChanged = { arabicFontStyle = settings.arabicFontStyle }
                                )
                            }
                            if (showAppSplash) {
                                AppSplashScreen()
                            }
                            if (!showAppSplash && showOnboarding) {
                                OnboardingFlow(
                                    initialLanguage = lang,
                                    initialFontStyle = arabicFontStyle,
                                    initialThemeMode = themeMode,
                                    initialStep = settings.onboardingStep,
                                    onLanguageChange = { newLang ->
                                        settings.appLanguage = newLang
                                        recreate()
                                    },
                                    onStepChange = { settings.onboardingStep = it },
                                    onComplete = { finishOnboarding(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshStats()
        if (viewModel.settingsRepo().isServiceEnabled) {
            ContextCompat.startForegroundService(
                this,
                Intent(this, AdhkarReminderService::class.java).apply {
                    action = AdhkarReminderService.ACTION_REFRESH
                }
            )
        }
    }

    @Composable
    private fun AdhkarApp(
        vm: MainViewModel,
        onLanguageChanged: () -> Unit,
        onThemeModeChanged: () -> Unit,
        onArabicFontChanged: () -> Unit = {}
    ) {
        val settings = vm.settingsRepo()
        var tab by remember { mutableIntStateOf(0) }
        var editingDhikr by remember { mutableStateOf<DhikrEntity?>(null) }
        var showAddScreen by remember { mutableStateOf(false) }
        var adminAddDefault by remember { mutableStateOf(false) }
        var adminEditing by remember { mutableStateOf(false) }
        var adminTargetCategory by remember { mutableStateOf<DhikrCategory?>(null) }
        var showAdminScreen by remember { mutableStateOf(false) }
        var isServiceOn by remember { mutableStateOf(settings.isServiceEnabled) }
        var playingDhikrId by remember { mutableStateOf<Long?>(null) }
        var fontScale by remember { mutableStateOf(settings.fontScale) }
        var numberDigitStyle by remember { mutableStateOf(settings.numberDigitStyle) }
        var autoAzkarEnabled by remember { mutableStateOf(settings.autoAzkarEnabled) }
        var autoAzkarRandom by remember { mutableStateOf(settings.autoAzkarRandomMode) }
        var azkarDisplayMode by remember { mutableStateOf(settings.azkarDisplayMode) }
        var openAzkarCollection by remember { mutableStateOf<AdhkarCollectionEntity?>(null) }
        var showMyDhikr by remember { mutableStateOf(false) }
        var adminAzkarBrowse by remember { mutableStateOf(false) }
        var adminAzkarCollection by remember { mutableStateOf<AdhkarCollectionEntity?>(null) }
        var editingAzkarItem by remember { mutableStateOf<AzkarItemEntity?>(null) }
        var adminAddingAzkarItem by remember { mutableStateOf(false) }
        var adminEditingAzkarCollection by remember { mutableStateOf<AdhkarCollectionEntity?>(null) }
        var adminAddingAzkarCollection by remember { mutableStateOf(false) }
        var adminRecitersBrowse by remember { mutableStateOf(false) }
        var adminAddingReciter by remember { mutableStateOf(false) }
        var adminEditingReciter by remember { mutableStateOf<ReciterEntity?>(null) }
        var adminReciterAudio by remember { mutableStateOf<ReciterEntity?>(null) }
        var adminDhikrBucket by remember { mutableStateOf<AdminDhikrBucket?>(null) }
        var adminReturnDhikrBucket by remember { mutableStateOf<AdminDhikrBucket?>(null) }
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val audioPlayer = remember { DhikrAudioPlayer(context) }
        var pendingServiceEnable by remember { mutableStateOf(false) }

        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { _ ->
            if (pendingServiceEnable) {
                pendingServiceEnable = false
                startAutoTasbih()
                isServiceOn = true
            }
        }

        fun requestEnableAutoTasbih() {
            val permission = RuntimePermissions.postNotificationsPermission()
            if (permission != null && !RuntimePermissions.hasPostNotifications(context)) {
                pendingServiceEnable = true
                notificationPermissionLauncher.launch(permission)
            } else {
                startAutoTasbih()
                isServiceOn = true
            }
        }

        fun stopAutoTasbih() {
            toggleService(currentlyOn = true)
            isServiceOn = false
        }

        DisposableEffect(Unit) {
            onDispose { audioPlayer.stop() }
        }

        LaunchedEffect(tab) {
            isServiceOn = settings.isServiceEnabled
        }

        LaunchedEffect(Unit) {
            isServiceOn = settings.isServiceEnabled
            if (settings.isServiceEnabled) {
                startAutoTasbih()
            }
        }

        fun playDhikr(dhikr: DhikrEntity) {
            scope.launch {
                val playable = DhikrPlaybackResolver.resolvePlayable(context, dhikr) ?: return@launch
                playingDhikrId = dhikr.id
                audioPlayer.playResolved(playable, settings) { playingDhikrId = null }
            }
        }

        val dhikrList by vm.dhikrList.collectAsState()
        val longForm by vm.longFormList.collectAsState()
        val reciters by vm.reciters.collectAsState()
        val allReciters by vm.allReciters.collectAsState()
        val azkarCollections by vm.azkarCollections.collectAsState()
        val favoriteSourceIds by vm.favoriteSourceIdsFlow()
            .collectAsState(initial = emptySet())
        val azkarItems by vm.collectionItemsFlow(openAzkarCollection?.id ?: adminAzkarCollection?.id ?: "")
            .collectAsState(initial = emptyList())
        val todayTasbihCount by vm.todayTasbihCount.collectAsState()
        val todayAzkarCount by vm.todayAzkarCount.collectAsState()
        val todayMisbahaCount by vm.todayMisbahaCount.collectAsState()
        val tasbihActivityLog by vm.tasbihActivityLog.collectAsState()
        val minutesUntil by vm.minutesUntilNext.collectAsState()
        val lang = settings.appLanguage

        LaunchedEffect(azkarCollections, openAzkarCollection?.id) {
            val current = openAzkarCollection ?: return@LaunchedEffect
            if (current.id == AzkarFavorites.COLLECTION_ID &&
                azkarCollections.none { it.id == AzkarFavorites.COLLECTION_ID }
            ) {
                openAzkarCollection = null
            }
        }

        val selectedReciterName = reciters.find { it.id == settings.selectedReciterId }
            ?.localizedName(lang) ?: ""

        AppFontScale(fontScale) {
        CompositionLocalProvider(LocalNumberDigitStyle provides numberDigitStyle) {
        if (adminAddingAzkarCollection || adminEditingAzkarCollection != null) {
            AdminEditAzkarCollectionScreen(
                existing = if (adminAddingAzkarCollection) null else adminEditingAzkarCollection,
                nextSortOrder = (azkarCollections.maxOfOrNull { it.sortOrder } ?: 0) + 1,
                onSave = { collection ->
                    vm.saveCollection(collection) {
                        adminAddingAzkarCollection = false
                        adminEditingAzkarCollection = null
                        adminAzkarCollection = collection
                        adminAzkarBrowse = false
                    }
                },
                onCancel = {
                    adminAddingAzkarCollection = false
                    adminEditingAzkarCollection = null
                    if (adminAzkarCollection == null) adminAzkarBrowse = true
                }
            )
            return@CompositionLocalProvider
        }

        if (editingAzkarItem != null || adminAddingAzkarItem) {
            val collectionId = adminAzkarCollection?.id ?: return@CompositionLocalProvider
            AdminEditAzkarItemScreen(
                collectionId = collectionId,
                existing = if (adminAddingAzkarItem) null else editingAzkarItem,
                nextSortOrder = (azkarItems.maxOfOrNull { it.sortOrder } ?: 0) + 1,
                onSave = { item ->
                    vm.saveAzkarItem(item) {
                        editingAzkarItem = null
                        adminAddingAzkarItem = false
                    }
                },
                onDelete = if (editingAzkarItem != null) {
                    { id ->
                        vm.deleteAzkarItem(id) {
                            editingAzkarItem = null
                            adminAddingAzkarItem = false
                        }
                    }
                } else null,
                onCancel = {
                    editingAzkarItem = null
                    adminAddingAzkarItem = false
                }
            )
            return@CompositionLocalProvider
        }

        if (adminAzkarCollection != null) {
            AdminAzkarItemsScreen(
                collection = adminAzkarCollection!!,
                items = azkarItems,
                lang = lang,
                onBack = {
                    adminAzkarCollection = null
                    adminAzkarBrowse = true
                },
                onEditCollection = {
                    adminEditingAzkarCollection = adminAzkarCollection
                },
                onAddItem = {
                    editingAzkarItem = null
                    adminAddingAzkarItem = true
                },
                onEditItem = {
                    editingAzkarItem = it
                    adminAddingAzkarItem = false
                }
            )
            return@CompositionLocalProvider
        }

        if (adminAzkarBrowse) {
            AdminAzkarCollectionsScreen(
                collections = azkarCollections,
                lang = lang,
                onBack = {
                    adminAzkarBrowse = false
                    showAdminScreen = true
                },
                onAddCollection = {
                    adminAddingAzkarCollection = true
                },
                onOpenCollection = {
                    adminAzkarCollection = it
                    adminAzkarBrowse = false
                }
            )
            return@CompositionLocalProvider
        }

        if (adminAddingReciter || adminEditingReciter != null) {
            AdminEditReciterScreen(
                existing = if (adminAddingReciter) null else adminEditingReciter,
                onSave = { reciter ->
                    vm.saveReciter(reciter) {
                        adminAddingReciter = false
                        adminEditingReciter = null
                        adminRecitersBrowse = true
                    }
                },
                onDelete = if (adminEditingReciter != null) {
                    { id ->
                        vm.deleteReciter(id) {
                            adminEditingReciter = null
                            adminRecitersBrowse = true
                        }
                    }
                } else null,
                onCancel = {
                    adminAddingReciter = false
                    adminEditingReciter = null
                    adminRecitersBrowse = true
                }
            )
            return@CompositionLocalProvider
        }

        if (adminReciterAudio != null) {
            val reciter = adminReciterAudio!!
            val reciterAudioList by vm.reciterAudioFlow(reciter.id)
                .collectAsState(initial = emptyList())
            AdminReciterAudioScreen(
                reciter = reciter,
                dhikrList = dhikrList,
                audioList = reciterAudioList,
                lang = lang,
                onSaveAudio = { audio -> vm.saveReciterAudio(audio) },
                onDeleteAudio = { audio ->
                    vm.deleteReciterAudio(audio.id, audio.localPath)
                },
                onBack = {
                    adminReciterAudio = null
                    adminRecitersBrowse = true
                }
            )
            return@CompositionLocalProvider
        }

        if (adminRecitersBrowse) {
            AdminRecitersScreen(
                reciters = allReciters,
                lang = lang,
                onBack = {
                    adminRecitersBrowse = false
                    showAdminScreen = true
                },
                onAddReciter = {
                    adminAddingReciter = true
                    adminRecitersBrowse = false
                },
                onEditReciter = { reciter ->
                    adminEditingReciter = reciter
                    adminRecitersBrowse = false
                },
                onOpenReciterAudio = { reciter ->
                    adminReciterAudio = reciter
                    adminRecitersBrowse = false
                }
            )
            return@CompositionLocalProvider
        }

        fun finishAdminDhikrEditor() {
            val returnBucket = adminReturnDhikrBucket
            adminReturnDhikrBucket = null
            adminAddDefault = false
            adminEditing = false
            adminTargetCategory = null
            showAddScreen = false
            editingDhikr = null
            if (returnBucket != null) {
                adminDhikrBucket = returnBucket
                showAdminScreen = false
            } else if (settings.isAdminLoggedIn) {
                showAdminScreen = true
            }
        }

        if (showAddScreen || editingDhikr != null) {
            val nextSortOrder = when (adminTargetCategory) {
                DhikrCategory.JAWAMI -> (dhikrList.filter { it.category == DhikrCategory.JAWAMI }
                    .maxOfOrNull { it.sortOrder } ?: 199) + 1
                else -> (dhikrList.filter { it.isDefault }.maxOfOrNull { it.sortOrder } ?: 0) + 1
            }
            AddEditDhikrScreen(
                existing = editingDhikr,
                reciters = reciters,
                isAdminDefault = adminAddDefault,
                isAdminEdit = adminEditing,
                adminTargetCategory = adminTargetCategory,
                adminNextSortOrder = nextSortOrder,
                showSchedule = (adminAddDefault || adminEditing) && adminTargetCategory != DhikrCategory.JAWAMI &&
                    editingDhikr?.category != DhikrCategory.JAWAMI,
                onSave = { entity ->
                    vm.saveDhikr(entity) { finishAdminDhikrEditor() }
                },
                onCancel = { finishAdminDhikrEditor() },
                onDelete = { id ->
                    vm.deleteDhikr(id)
                    showAddScreen = false
                    editingDhikr = null
                }
            )
            return@CompositionLocalProvider
        }

        if (adminDhikrBucket != null) {
            val bucket = adminDhikrBucket!!
            AdminDhikrBrowseScreen(
                bucket = bucket,
                items = dhikrList,
                onBack = {
                    adminDhikrBucket = null
                    showAdminScreen = true
                },
                onAdd = {
                    adminReturnDhikrBucket = bucket
                    when (bucket) {
                        AdminDhikrBucket.JAWAMI -> {
                            adminAddDefault = true
                            adminTargetCategory = DhikrCategory.JAWAMI
                        }
                        AdminDhikrBucket.SHORT_TASBIH -> {
                            adminAddDefault = true
                            adminTargetCategory = DhikrCategory.GENERAL
                        }
                        AdminDhikrBucket.OTHER_BUILTIN -> {
                            adminAddDefault = true
                            adminTargetCategory = null
                        }
                    }
                    showAddScreen = true
                    adminDhikrBucket = null
                    showAdminScreen = false
                },
                onEditDhikr = {
                    adminReturnDhikrBucket = bucket
                    editingDhikr = it
                    adminEditing = true
                    adminTargetCategory = when (it.category) {
                        DhikrCategory.JAWAMI -> DhikrCategory.JAWAMI
                        else -> null
                    }
                    adminDhikrBucket = null
                    showAdminScreen = false
                }
            )
            return@CompositionLocalProvider
        }

        if (showAdminScreen) {
            AdminScreen(
                isLoggedIn = settings.isAdminLoggedIn,
                onLogin = vm::loginAdmin,
                onLogout = {
                    vm.logoutAdmin()
                    showAdminScreen = false
                },
                onClose = { showAdminScreen = false },
                onOpenDhikrSection = { bucket ->
                    adminDhikrBucket = bucket
                    showAdminScreen = false
                },
                onManageAzkar = {
                    adminAzkarBrowse = true
                    showAdminScreen = false
                },
                onManageReciters = {
                    adminRecitersBrowse = true
                    showAdminScreen = false
                },
                dhikrList = dhikrList,
                azkarCollectionCount = azkarCollections.size,
                onImport = { list -> list.forEach { vm.saveDhikr(it) } }
            )
            return@CompositionLocalProvider
        }

        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = GreenPrimary,
                    tonalElevation = 0.dp
                ) {
                    val navColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GreenPrimary,
                        selectedTextColor = GreenPrimary,
                        indicatorColor = GoldDome.copy(alpha = 0.18f),
                        unselectedIconColor = Color(0xFF6B6B6B),
                        unselectedTextColor = Color(0xFF6B6B6B)
                    )
                    NavigationBarItem(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text(stringResource(R.string.nav_home)) },
                        colors = navColors
                    )
                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        icon = {
                            val tint = if (tab == 1) GreenPrimary else Color(0xFF6B6B6B)
                            MisbahaNavIcon(tint = tint)
                        },
                        label = { Text(stringResource(R.string.nav_misbaha)) },
                        colors = navColors
                    )
                    NavigationBarItem(
                        selected = tab == 2,
                        onClick = { tab = 2; openAzkarCollection = null; showMyDhikr = false },
                        icon = {
                            val tint = if (tab == 2) GreenPrimary else Color(0xFF6B6B6B)
                            AzkarNavIcon(tint = tint)
                        },
                        label = { Text(stringResource(R.string.nav_azkar)) },
                        colors = navColors
                    )
                    NavigationBarItem(
                        selected = tab == 3,
                        onClick = { tab = 3 },
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text(stringResource(R.string.nav_settings)) },
                        colors = navColors
                    )
                }
            }
        ) { padding ->
            when (tab) {
                0 -> HomeScreen(
                    isServiceOn = isServiceOn,
                    todayTasbihCount = todayTasbihCount,
                    todayAzkarCount = todayAzkarCount,
                    todayMisbahaCount = todayMisbahaCount,
                    minutesUntilNext = minutesUntil,
                    autoAzkarEnabled = autoAzkarEnabled,
                    azkarCollections = azkarCollections,
                    appLang = lang,
                    onToggleService = {
                        if (isServiceOn) stopAutoTasbih() else requestEnableAutoTasbih()
                    },
                    onToggleAutoAzkar = {
                        autoAzkarEnabled = it
                        settings.autoAzkarEnabled = it
                    },
                    modifier = Modifier.padding(padding),
                    onAdminSecretTap = { showAdminScreen = true }
                )
                1 -> MisbahaScreen(
                    items = dhikrList.filter {
                        it.isDefault && !it.isLongForm && it.category != DhikrCategory.JAWAMI
                    },
                    lang = lang,
                    activityData = tasbihActivityLog,
                    onTasbihCounted = vm::recordMisbahaCount,
                    onOpenActivityLog = { vm.loadTasbihActivityLog() },
                    onActivityPeriodChange = vm::loadTasbihActivityLog,
                    modifier = Modifier.padding(padding)
                )
                2 -> {
                    when {
                        showMyDhikr -> MyDhikrScreen(
                            items = dhikrList,
                            lang = lang,
                            playingDhikrId = playingDhikrId,
                            onBack = { showMyDhikr = false },
                            onAdd = { showAddScreen = true },
                            onEdit = { editingDhikr = it },
                            onToggle = { vm.saveDhikr(it) },
                            onPlay = { playDhikr(it) },
                            onStopPlay = { audioPlayer.stop(); playingDhikrId = null },
                            modifier = Modifier.padding(padding),
                        )
                        openAzkarCollection != null -> {
                            val current = openAzkarCollection!!
                            AzkarCollectionScreen(
                                collection = current,
                                items = azkarItems,
                                lang = lang,
                                favoriteSourceIds = favoriteSourceIds,
                                displayMode = azkarDisplayMode,
                                onDisplayModeChange = { mode ->
                                    azkarDisplayMode = mode
                                    settings.azkarDisplayMode = mode
                                },
                                onBack = { openAzkarCollection = null },
                                onSaveSchedule = { updated ->
                                    vm.saveCollection(updated)
                                    openAzkarCollection = updated
                                },
                                onToggleFavorite = { item ->
                                    vm.toggleAzkarFavorite(item)
                                },
                            )
                        }
                        else -> AzkarHubScreen(
                            collections = azkarCollections,
                            lang = lang,
                            onOpenCollection = { openAzkarCollection = it },
                            onOpenMyDhikr = { showMyDhikr = true },
                            modifier = Modifier.padding(padding),
                        )
                    }
                }
                3 -> SettingsScreen(
                    settings = settings,
                    reciters = reciters,
                    lang = lang,
                    isServiceOn = isServiceOn,
                    autoAzkarEnabled = autoAzkarEnabled,
                    autoAzkarRandom = autoAzkarRandom,
                    onToggleService = {
                        if (isServiceOn) stopAutoTasbih() else requestEnableAutoTasbih()
                    },
                    onToggleAutoAzkar = {
                        autoAzkarEnabled = it
                        settings.autoAzkarEnabled = it
                    },
                    onAutoAzkarRandomChange = {
                        autoAzkarRandom = it
                        settings.autoAzkarRandomMode = it
                    },
                    onPreviewVoice = {
                        dhikrList.firstOrNull { it.audioSourceType != com.greendome.adhkar.data.model.AudioSourceType.NONE }
                            ?.let { playDhikr(it) }
                    },
                    onLanguageChanged = onLanguageChanged,
                    onFontScaleChanged = { fontScale = settings.fontScale },
                    onArabicFontChanged = onArabicFontChanged,
                    onThemeModeChanged = onThemeModeChanged,
                    onNumberDigitStyleChanged = { style ->
                        numberDigitStyle = style
                        if (settings.isServiceEnabled) {
                            ContextCompat.startForegroundService(
                                context,
                                Intent(context, AdhkarReminderService::class.java).apply {
                                    action = AdhkarReminderService.ACTION_REFRESH
                                }
                            )
                        }
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }
        }
        }
    }

    private fun startAutoTasbih() {
        toggleService(currentlyOn = false)
        viewModel.refreshStats()
    }

    private fun toggleService(currentlyOn: Boolean) {
        val action = if (currentlyOn) AdhkarReminderService.ACTION_STOP else AdhkarReminderService.ACTION_START
        ContextCompat.startForegroundService(this, Intent(this, AdhkarReminderService::class.java).apply { this.action = action })
        viewModel.refreshStats()
    }
}
