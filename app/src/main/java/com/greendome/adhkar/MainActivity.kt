package com.greendome.adhkar

import android.content.Intent
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.lifecycle.lifecycleScope
import com.greendome.adhkar.prayer.TravelLocationUpdater
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import com.greendome.adhkar.ui.adaptive.AdaptiveContentContainer
import com.greendome.adhkar.ui.adaptive.rememberAppWindowWidth
import com.greendome.adhkar.ui.adaptive.useNavigationRail
import com.greendome.adhkar.ui.components.AudioUnavailableDialog
import com.greendome.adhkar.ui.components.AzkarNavIcon
import com.greendome.adhkar.ui.components.HomeNavIcon
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
import com.greendome.adhkar.service.NextAdhanService
import com.greendome.adhkar.service.NextAzkarNotifier
import com.greendome.adhkar.service.PrayerPhoneSilent
import com.greendome.adhkar.service.SilentNotificationChannels
import com.greendome.adhkar.service.VehicleActivityScheduler
import com.greendome.adhkar.ui.MainViewModel
import com.greendome.adhkar.ui.screens.AddEditDhikrScreen
import com.greendome.adhkar.ui.screens.AppSplashScreen
import com.greendome.adhkar.ui.screens.AdminAdhanAudioScreen
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
import com.greendome.adhkar.ui.screens.AdminPrayerDefaultsScreen
import com.greendome.adhkar.ui.screens.AzkarCollectionScreen
import com.greendome.adhkar.ui.screens.AzkarHubScreen
import com.greendome.adhkar.ui.screens.MyDhikrScreen
import com.greendome.adhkar.ui.screens.HomeScreen
import com.greendome.adhkar.ui.screens.MisbahaScreen
import com.greendome.adhkar.ui.screens.QiblaSettingsScreen
import com.greendome.adhkar.ui.components.MisbahaNavIcon
import com.greendome.adhkar.ui.screens.SettingsScreen
import com.greendome.adhkar.ui.screens.OnboardingFlow
import com.greendome.adhkar.ui.screens.OnboardingResult
import com.greendome.adhkar.ui.theme.AppArabicFont
import com.greendome.adhkar.ui.theme.AppFontScale
import com.greendome.adhkar.ui.theme.effectiveAppFontScale
import com.greendome.adhkar.data.model.ArabicFontStyle
import com.greendome.adhkar.data.model.NumberDigitStyle
import com.greendome.adhkar.ui.theme.LocalNumberDigitStyle
import com.greendome.adhkar.ui.theme.GoldDome
import com.greendome.adhkar.ui.theme.GreenDomeTheme
import com.greendome.adhkar.ui.theme.GreenPrimary
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.greendome.adhkar.audio.AzkarPlaybackResolver
import com.greendome.adhkar.audio.DhikrAudioPlayer
import com.greendome.adhkar.audio.DhikrPlaybackResolver
import com.greendome.adhkar.audio.playResolved
import com.greendome.adhkar.data.model.VoiceSettingsTarget
import com.greendome.adhkar.sync.ContentUpdateDialogs
import com.greendome.adhkar.sync.RemoteContentPublisher
import com.greendome.adhkar.sync.rememberContentUpdateController
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.review.InAppReviewPrompt
import com.greendome.adhkar.update.ProvidePlayAppUpdate
import com.greendome.adhkar.update.rememberPlayAppUpdateController
import com.greendome.adhkar.util.AppLanguages
import com.greendome.adhkar.util.LocaleHelper
import com.greendome.adhkar.util.RuntimePermissions
import com.greendome.adhkar.widget.DhikrOfDayManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val holdSystemSplash = mutableStateOf(true)
    private var mainUiReady = false

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapWithSavedLanguage(newBase))
    }

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { holdSystemSplash.value }
        super.onCreate(savedInstanceState)
        SilentNotificationChannels.ensureCreated(this)
        DhikrOfDayManager.refreshAsync(this)
        setContent {
            val settings = viewModel.settingsRepo()
            val lang = settings.appLanguage
            var themeMode by remember { mutableStateOf(settings.appThemeMode) }
            var showAppSplash by remember { mutableStateOf(true) }
            var showOnboarding by remember { mutableStateOf(!settings.onboardingCompleted) }
            var arabicFontStyle by remember { mutableStateOf(settings.arabicFontStyle) }
            var pendingOnboardingServiceEnable by remember { mutableStateOf(false) }
            var pendingOnboardingRiding by remember { mutableStateOf(false) }
            val layoutDirection = if (AppLanguages.isRtl(lang)) LayoutDirection.Rtl else LayoutDirection.Ltr
            val splashDurationMs = if (showOnboarding) 900L else 1400L

            val activityPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    VehicleActivityScheduler.register(this@MainActivity)
                }
            }

            fun requestRidingPermissionIfNeeded(ridingEnabled: Boolean) {
                if (!ridingEnabled) return
                val permission = RuntimePermissions.activityRecognitionPermission()
                if (permission != null && !RuntimePermissions.hasActivityRecognition(this@MainActivity)) {
                    activityPermissionLauncher.launch(permission)
                } else {
                    VehicleActivityScheduler.register(this@MainActivity)
                }
            }

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { _ ->
                if (pendingOnboardingServiceEnable) {
                    pendingOnboardingServiceEnable = false
                    startAutoTasbih()
                }
                if (pendingOnboardingRiding) {
                    pendingOnboardingRiding = false
                    requestRidingPermissionIfNeeded(true)
                }
            }

            fun enableDefaultAutoFeatures(result: OnboardingResult) {
                settings.isServiceEnabled = result.autoTasbihEnabled
                val ridingOn = result.ridingAzkarEnabled
                val needsNotifications = result.autoTasbihEnabled || result.autoAzkarEnabled
                if (!needsNotifications) {
                    requestRidingPermissionIfNeeded(ridingOn)
                    return
                }
                val permission = RuntimePermissions.postNotificationsPermission()
                if (permission != null && !RuntimePermissions.hasPostNotifications(this)) {
                    pendingOnboardingServiceEnable = result.autoTasbihEnabled
                    pendingOnboardingRiding = ridingOn
                    notificationPermissionLauncher.launch(permission)
                } else {
                    if (result.autoTasbihEnabled) startAutoTasbih()
                    requestRidingPermissionIfNeeded(ridingOn)
                }
            }

            fun finishOnboarding(result: OnboardingResult) {
                settings.appLanguage = result.language
                settings.arabicFontStyle = result.arabicFontStyle
                settings.azkarCardFontSizeSp = result.azkarCardFontSizeSp
                settings.azkarListFontSizeSp = result.azkarListFontSizeSp
                settings.appThemeMode = result.themeMode
                settings.onboardingStep = 0
                settings.onboardingCompleted = true
                arabicFontStyle = result.arabicFontStyle
                themeMode = result.themeMode
                showOnboarding = false
                viewModel.applyOnboardingReminders(result)
                enableDefaultAutoFeatures(result)
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
                    val updateController = rememberPlayAppUpdateController()
                    val contentUpdate = rememberContentUpdateController()
                    ProvidePlayAppUpdate(updateController) {
                    LaunchedEffect(showAppSplash, showOnboarding) {
                        if (!showAppSplash && !showOnboarding) {
                            mainUiReady = true
                            kotlinx.coroutines.delay(1000)
                            val showedAppUpdate = updateController.autoPromptIfNeeded()
                            val showedContentUpdate = if (!showedAppUpdate) {
                                contentUpdate.autoPromptIfNeeded()
                            } else {
                                false
                            }
                            if (!showedAppUpdate && !showedContentUpdate) {
                                InAppReviewPrompt.onMainScreenReady(this@MainActivity)
                            }
                        }
                    }
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
                                val azkarCollections by viewModel.azkarCollections.collectAsState(
                                    initial = emptyList()
                                )
                                OnboardingFlow(
                                    initialLanguage = lang,
                                    initialFontStyle = arabicFontStyle,
                                    initialThemeMode = themeMode,
                                    initialStep = settings.onboardingStep,
                                    azkarCollections = azkarCollections,
                                    onLanguageChange = { newLang ->
                                        settings.appLanguage = newLang
                                        recreate()
                                    },
                                    onThemeChange = { newTheme ->
                                        settings.appThemeMode = newTheme
                                        themeMode = newTheme
                                    },
                                    onStepChange = { settings.onboardingStep = it },
                                    onComplete = { finishOnboarding(it) }
                                )
                            }
                        }
                    }
                    ContentUpdateDialogs(contentUpdate)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshStats()
        if (mainUiReady) {
            InAppReviewPrompt.onMainScreenReady(this)
        }
        if (viewModel.settingsRepo().isServiceEnabled) {
            ContextCompat.startForegroundService(
                this,
                Intent(this, AdhkarReminderService::class.java).apply {
                    action = AdhkarReminderService.ACTION_REFRESH
                }
            )
        }
        NextAdhanService.sync(this)
        NextAzkarNotifier.sync(this)
        PrayerPhoneSilent.sync(this)
        lifecycleScope.launch {
            TravelLocationUpdater.maybeRefresh(this@MainActivity)
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
        var showNoAudioAlert by remember { mutableStateOf(false) }
        var fontScale by remember { mutableStateOf(settings.fontScale) }
        var numberDigitStyle by remember { mutableStateOf(settings.numberDigitStyle) }
        var autoAzkarEnabled by remember { mutableStateOf(settings.autoAzkarEnabled) }
        var autoAzkarRandom by remember { mutableStateOf(settings.autoAzkarRandomMode) }
        var azkarDisplayMode by remember { mutableStateOf(settings.azkarDisplayMode) }
        var openAzkarCollection by remember { mutableStateOf<AdhkarCollectionEntity?>(null) }
        var openAzkarItemId by remember { mutableStateOf<Long?>(null) }
        var openPrayerRespectSettings by remember { mutableStateOf(false) }
        var openHomeAzkarSettings by remember { mutableStateOf(false) }
        var openAdhanSettings by remember { mutableStateOf(false) }
        var showQiblaScreen by remember { mutableStateOf(false) }
        var settingsReturnToHome by remember { mutableStateOf(false) }
        var azkarSearchQuery by remember { mutableStateOf("") }
        var showMyDhikr by remember { mutableStateOf(false) }
        var adminAzkarBrowse by remember { mutableStateOf(false) }
        var adminAzkarCollection by remember { mutableStateOf<AdhkarCollectionEntity?>(null) }
        var editingAzkarItem by remember { mutableStateOf<AzkarItemEntity?>(null) }
        var adminAddingAzkarItem by remember { mutableStateOf(false) }
        var adminEditingAzkarCollection by remember { mutableStateOf<AdhkarCollectionEntity?>(null) }
        var adminAddingAzkarCollection by remember { mutableStateOf(false) }
        var adminRecitersBrowse by remember { mutableStateOf(false) }
        var adminAdhanAudio by remember { mutableStateOf(false) }
        var adminPrayerDefaults by remember { mutableStateOf(false) }
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
                val playable = DhikrPlaybackResolver.resolvePlayable(context, dhikr)
                if (playable == null) {
                    showNoAudioAlert = true
                    return@launch
                }
                playingDhikrId = dhikr.id
                audioPlayer.playResolved(playable, settings, VoiceSettingsTarget.TASBIH) {
                    playingDhikrId = null
                }
            }
        }

        fun previewAzkarVoice() {
            scope.launch {
                val items = AdhkarDatabase.get(context).azkarItemDao().getAll()
                for (item in items) {
                    val playable = AzkarPlaybackResolver.resolvePlayable(context, item)
                    if (playable != null) {
                        audioPlayer.playResolved(playable, settings, VoiceSettingsTarget.AZKAR)
                        return@launch
                    }
                }
                showNoAudioAlert = true
            }
        }

        val dhikrList by vm.dhikrList.collectAsState()
        val longForm by vm.longFormList.collectAsState()
        val reciters by vm.reciters.collectAsState()
        val allReciters by vm.allReciters.collectAsState()
        val allAdhanAudio by vm.allAdhanAudio.collectAsState()
        val azkarCollections by vm.azkarCollections.collectAsState()
        val allAzkarItems by vm.allAzkarItems.collectAsState()
        val azkarHubOrder by vm.azkarHubOrder.collectAsState()
        val favoriteSourceIds by vm.favoriteSourceIdsFlow()
            .collectAsState(initial = emptySet())
        val azkarItems by vm.collectionItemsFlow(openAzkarCollection?.id ?: adminAzkarCollection?.id ?: "")
            .collectAsState(initial = emptyList())
        val todayTasbihCount by vm.todayTasbihCount.collectAsState()
        val todayAzkarCount by vm.todayAzkarCount.collectAsState()
        val todayMisbahaCount by vm.todayMisbahaCount.collectAsState()
        val tasbihActivityLog by vm.tasbihActivityLog.collectAsState()
        val minutesUntil by vm.minutesUntilNext.collectAsState()
        val isPrayerQuiet by vm.isPrayerQuiet.collectAsState()
        val pendingPublishChanges by vm.pendingPublishChanges.collectAsState()
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

        AppFontScale(effectiveAppFontScale(fontScale)) {
        CompositionLocalProvider(LocalNumberDigitStyle provides numberDigitStyle) {
        fun goHome() {
            tab = 0
            showQiblaScreen = false
            settingsReturnToHome = false
        }

        fun finishAdminDhikrEditor() {
            val returnBucket = adminReturnDhikrBucket
            val openedFromAdmin = adminAddDefault || adminEditing || returnBucket != null
            adminReturnDhikrBucket = null
            adminAddDefault = false
            adminEditing = false
            adminTargetCategory = null
            showAddScreen = false
            editingDhikr = null
            when {
                returnBucket != null -> {
                    adminDhikrBucket = returnBucket
                    showAdminScreen = false
                }
                openedFromAdmin -> showAdminScreen = true
            }
        }

        val canPopNestedScreen = adminAddingAzkarCollection ||
            adminEditingAzkarCollection != null ||
            editingAzkarItem != null ||
            adminAddingAzkarItem ||
            adminAzkarCollection != null ||
            adminAzkarBrowse ||
            adminAddingReciter ||
            adminEditingReciter != null ||
            adminReciterAudio != null ||
            adminRecitersBrowse ||
            adminAdhanAudio ||
            adminPrayerDefaults ||
            showAddScreen ||
            editingDhikr != null ||
            adminDhikrBucket != null ||
            showAdminScreen ||
            showMyDhikr ||
            openAzkarCollection != null
        BackHandler(enabled = canPopNestedScreen || tab != 0) {
            when {
                adminAddingAzkarCollection || adminEditingAzkarCollection != null -> {
                    adminAddingAzkarCollection = false
                    adminEditingAzkarCollection = null
                    if (adminAzkarCollection == null) adminAzkarBrowse = true
                }
                editingAzkarItem != null || adminAddingAzkarItem -> {
                    editingAzkarItem = null
                    adminAddingAzkarItem = false
                }
                adminAzkarCollection != null -> {
                    adminAzkarCollection = null
                    adminAzkarBrowse = true
                }
                adminAzkarBrowse -> {
                    adminAzkarBrowse = false
                    showAdminScreen = true
                }
                adminAddingReciter || adminEditingReciter != null -> {
                    adminAddingReciter = false
                    adminEditingReciter = null
                    adminRecitersBrowse = true
                }
                adminReciterAudio != null -> {
                    adminReciterAudio = null
                    adminRecitersBrowse = true
                }
                adminRecitersBrowse -> {
                    adminRecitersBrowse = false
                    showAdminScreen = true
                }
                adminAdhanAudio -> {
                    adminAdhanAudio = false
                    showAdminScreen = true
                }
                adminPrayerDefaults -> {
                    adminPrayerDefaults = false
                    showAdminScreen = true
                }
                showAddScreen || editingDhikr != null -> finishAdminDhikrEditor()
                adminDhikrBucket != null -> {
                    adminDhikrBucket = null
                    showAdminScreen = true
                }
                showAdminScreen -> showAdminScreen = false
                showMyDhikr -> showMyDhikr = false
                openAzkarCollection != null -> {
                    openAzkarCollection = null
                    openAzkarItemId = null
                }
                else -> goHome()
            }
        }

        if (adminAddingAzkarCollection || adminEditingAzkarCollection != null) {
            AdminEditAzkarCollectionScreen(
                existing = if (adminAddingAzkarCollection) null else adminEditingAzkarCollection,
                nextSortOrder = (azkarCollections.maxOfOrNull { it.sortOrder } ?: 0) + 1,
                onSave = { collection ->
                    vm.saveCollection(collection, markPendingPublish = true) {
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
            val reciterAzkarAudioList by vm.reciterAzkarAudioFlow(reciter.id)
                .collectAsState(initial = emptyList())
            AdminReciterAudioScreen(
                reciter = reciter,
                dhikrList = dhikrList,
                azkarCollections = azkarCollections,
                azkarItems = allAzkarItems,
                dhikrAudioList = reciterAudioList,
                azkarAudioList = reciterAzkarAudioList,
                lang = lang,
                onSaveDhikrAudio = { audio -> vm.saveReciterAudio(audio) },
                onDeleteDhikrAudio = { audio ->
                    vm.deleteReciterAudio(audio.id, audio.localPath)
                },
                onSaveAzkarAudio = { audio -> vm.saveReciterAzkarAudio(audio) },
                onDeleteAzkarAudio = { audio ->
                    vm.deleteReciterAzkarAudio(audio.id, audio.localPath)
                },
                onBack = {
                    adminReciterAudio = null
                    adminRecitersBrowse = true
                }
            )
            return@CompositionLocalProvider
        }

        if (adminAdhanAudio) {
            AdminAdhanAudioScreen(
                items = allAdhanAudio,
                lang = lang,
                onSave = { entity -> vm.saveAdhanAudio(entity) },
                onDelete = { entity -> vm.deleteAdhanAudio(entity) },
                onBack = {
                    adminAdhanAudio = false
                    showAdminScreen = true
                }
            )
            return@CompositionLocalProvider
        }

        if (adminPrayerDefaults) {
            AdminPrayerDefaultsScreen(
                settings = settings,
                onBack = {
                    adminPrayerDefaults = false
                    showAdminScreen = true
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
                    vm.saveDhikr(
                        entity,
                        markPendingPublish = adminAddDefault || adminEditing
                    ) { finishAdminDhikrEditor() }
                },
                onCancel = { finishAdminDhikrEditor() },
                onDelete = { id ->
                    vm.deleteDhikr(id, markPendingPublish = adminAddDefault || adminEditing)
                    finishAdminDhikrEditor()
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
                },
                onDeleteDhikr = if (
                    bucket == AdminDhikrBucket.SHORT_TASBIH ||
                    bucket == AdminDhikrBucket.JAWAMI
                ) {
                    { dhikr -> vm.deleteDhikr(dhikr.id, markPendingPublish = true) }
                } else {
                    null
                }
            )
            return@CompositionLocalProvider
        }

        if (showAdminScreen) {
            val appContext = LocalContext.current
            AdminScreen(
                isLoggedIn = settings.isAdminLoggedIn,
                settings = settings,
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
                onManageAdhanAudio = {
                    adminAdhanAudio = true
                    showAdminScreen = false
                },
                onManagePrayerDefaults = {
                    adminPrayerDefaults = true
                    showAdminScreen = false
                },
                dhikrList = dhikrList,
                azkarCollectionCount = azkarCollections.size,
                pendingPublishChanges = pendingPublishChanges,
                onImport = { list -> list.forEach { vm.saveDhikr(it, markPendingPublish = true) } },
                onPublishUpdates = { onProgress ->
                    RemoteContentPublisher.publish(
                        appContext,
                        settings,
                        AdhkarDatabase.get(appContext),
                        onProgress
                    )
                }
            )
            return@CompositionLocalProvider
        }

        Box(modifier = Modifier.fillMaxSize()) {
        val windowWidth = rememberAppWindowWidth()
        val useRail = windowWidth.useNavigationRail()
        val navBarColors = NavigationBarItemDefaults.colors(
            selectedIconColor = GreenPrimary,
            selectedTextColor = GreenPrimary,
            indicatorColor = GoldDome.copy(alpha = 0.18f),
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val navRailColors = NavigationRailItemDefaults.colors(
            selectedIconColor = GreenPrimary,
            selectedTextColor = GreenPrimary,
            indicatorColor = GoldDome.copy(alpha = 0.18f),
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )

        @Composable
        fun MainTabs(padding: androidx.compose.foundation.layout.PaddingValues) {
            when (tab) {
                0 -> if (showQiblaScreen) {
                    BackHandler { showQiblaScreen = false }
                    QiblaSettingsScreen(
                        settings = settings,
                        onBack = { showQiblaScreen = false },
                        modifier = Modifier.padding(padding)
                    )
                } else {
                    HomeScreen(
                        isServiceOn = isServiceOn,
                        todayTasbihCount = todayTasbihCount,
                        todayAzkarCount = todayAzkarCount,
                        todayMisbahaCount = todayMisbahaCount,
                        minutesUntilNext = minutesUntil,
                        isPrayerQuiet = isPrayerQuiet,
                        autoAzkarEnabled = autoAzkarEnabled,
                        azkarCollections = azkarCollections,
                        azkarItems = allAzkarItems,
                        prayerConfig = settings.prayerConfig(),
                        clockHourFormat = settings.azkarClockHourFormat,
                        appLang = lang,
                        onToggleService = {
                            if (isServiceOn) stopAutoTasbih() else requestEnableAutoTasbih()
                        },
                        onToggleAutoAzkar = {
                            autoAzkarEnabled = it
                            vm.setAutoAzkarEnabled(it)
                        },
                        modifier = Modifier.padding(padding),
                        onOpenPrayerSettings = {
                            settingsReturnToHome = true
                            tab = 3
                            openPrayerRespectSettings = true
                        },
                        onOpenQibla = { showQiblaScreen = true },
                        homeSectionOrder = settings.homeSectionOrder,
                        homeHiddenSections = settings.homeHiddenSections,
                    )
                }
                1 -> MisbahaScreen(
                    items = dhikrList.filter { it.isBuiltinShortTasbih() }
                        .distinctBy { it.textAr.trim() },
                    lang = lang,
                    activityData = tasbihActivityLog,
                    onTasbihCounted = {
                        vm.recordMisbahaCount()
                        InAppReviewPrompt.considerLaunch(this@MainActivity)
                    },
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
                                onBack = {
                                    openAzkarCollection = null
                                    openAzkarItemId = null
                                },
                                onSaveSchedule = { updated ->
                                    vm.saveCollection(updated)
                                    openAzkarCollection = updated
                                },
                                onToggleFavorite = { item ->
                                    vm.toggleAzkarFavorite(item)
                                },
                                onOpenAfterPrayerSettings = {
                                    openAzkarCollection = null
                                    openAzkarItemId = null
                                    settingsReturnToHome = false
                                    openPrayerRespectSettings = true
                                    tab = 3
                                },
                                onOpenHomeAzkarSettings = {
                                    openAzkarCollection = null
                                    openAzkarItemId = null
                                    settingsReturnToHome = false
                                    openHomeAzkarSettings = true
                                    tab = 3
                                },
                                onOpenAdhanSettings = {
                                    openAzkarCollection = null
                                    openAzkarItemId = null
                                    settingsReturnToHome = false
                                    openAdhanSettings = true
                                    tab = 3
                                },
                                initialItemId = openAzkarItemId,
                                modifier = Modifier.padding(padding),
                            )
                        }
                        else -> AzkarHubScreen(
                            collections = azkarCollections,
                            allItems = allAzkarItems,
                            customDhikr = dhikrList,
                            lang = lang,
                            searchQuery = azkarSearchQuery,
                            onSearchQueryChange = { azkarSearchQuery = it },
                            onOpenCollection = {
                                openAzkarItemId = null
                                openAzkarCollection = it
                            },
                            onOpenItem = { collection, item ->
                                openAzkarItemId = item.id
                                openAzkarCollection = collection
                            },
                            onOpenMyDhikr = { showMyDhikr = true },
                            savedOrder = azkarHubOrder,
                            onReorder = { vm.saveAzkarHubOrder(it) },
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
                        vm.setAutoAzkarEnabled(it)
                    },
                    onAutoAzkarRandomChange = {
                        autoAzkarRandom = it
                        settings.autoAzkarRandomMode = it
                    },
                    onPreviewVoice = {
                        dhikrList.firstOrNull { it.audioSourceType != com.greendome.adhkar.data.model.AudioSourceType.NONE }
                            ?.let { playDhikr(it) }
                            ?: run { showNoAudioAlert = true }
                    },
                    onPreviewAzkarVoice = { previewAzkarVoice() },
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
                        NextAdhanService.sync(context)
                        NextAzkarNotifier.sync(context)
                    },
                    openPrayerRespect = openPrayerRespectSettings,
                    onOpenPrayerRespectConsumed = { openPrayerRespectSettings = false },
                    openHomeAzkar = openHomeAzkarSettings,
                    onOpenHomeAzkarConsumed = { openHomeAzkarSettings = false },
                    openAdhan = openAdhanSettings,
                    onOpenAdhanConsumed = { openAdhanSettings = false },
                    returnToHomeOnHubBack = settingsReturnToHome,
                    onReturnHome = { goHome() },
                    modifier = Modifier.padding(padding)
                )
            }
        }

        if (useRail) {
            Row(Modifier.fillMaxSize()) {
                MainNavigationRail(
                    tab = tab,
                    colors = navRailColors,
                    onHome = { goHome() },
                    onHomeLongPress = { showAdminScreen = true },
                    onMisbaha = { tab = 1; settingsReturnToHome = false },
                    onAzkar = { tab = 2; openAzkarCollection = null; showMyDhikr = false; settingsReturnToHome = false },
                    onSettings = { tab = 3; settingsReturnToHome = false },
                )
                AdaptiveContentContainer(Modifier.weight(1f)) {
                    Scaffold { padding -> MainTabs(padding) }
                }
            }
        } else {
        AdaptiveContentContainer {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = GreenPrimary,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = tab == 0,
                        onClick = { goHome() },
                        icon = {
                            HomeTabIcon(
                                selected = tab == 0,
                                onClick = { goHome() },
                                onLongPress = { showAdminScreen = true },
                            )
                        },
                        label = { Text(stringResource(R.string.nav_home)) },
                        colors = navBarColors
                    )
                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = { tab = 1; settingsReturnToHome = false },
                        icon = {
                            val tint = if (tab == 1) GreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            MisbahaNavIcon(tint = tint)
                        },
                        label = { Text(stringResource(R.string.nav_misbaha)) },
                        colors = navBarColors
                    )
                    NavigationBarItem(
                        selected = tab == 2,
                        onClick = { tab = 2; openAzkarCollection = null; showMyDhikr = false; settingsReturnToHome = false },
                        icon = {
                            val tint = if (tab == 2) GreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            AzkarNavIcon(tint = tint)
                        },
                        label = { Text(stringResource(R.string.nav_azkar)) },
                        colors = navBarColors
                    )
                    NavigationBarItem(
                        selected = tab == 3,
                        onClick = { tab = 3; settingsReturnToHome = false },
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text(stringResource(R.string.nav_settings)) },
                        colors = navBarColors
                    )
                }
            }
        ) { padding -> MainTabs(padding) }
        }
        }
        if (showNoAudioAlert) {
            AudioUnavailableDialog(onDismiss = { showNoAudioAlert = false })
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeTabIcon(
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val tint = if (selected) GreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
            onLongClick = onLongPress,
        )
    ) {
        HomeNavIcon(tint = tint)
    }
}

@Composable
private fun MainNavigationRail(
    tab: Int,
    colors: androidx.compose.material3.NavigationRailItemColors,
    onHome: () -> Unit,
    onHomeLongPress: () -> Unit,
    onMisbaha: () -> Unit,
    onAzkar: () -> Unit,
    onSettings: () -> Unit,
) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = GreenPrimary,
    ) {
        NavigationRailItem(
            selected = tab == 0,
            onClick = onHome,
            icon = {
                HomeTabIcon(
                    selected = tab == 0,
                    onClick = onHome,
                    onLongPress = onHomeLongPress,
                )
            },
            label = { Text(stringResource(R.string.nav_home)) },
            colors = colors
        )
        NavigationRailItem(
            selected = tab == 1,
            onClick = onMisbaha,
            icon = {
                val tint = if (tab == 1) GreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                MisbahaNavIcon(tint = tint)
            },
            label = { Text(stringResource(R.string.nav_misbaha)) },
            colors = colors
        )
        NavigationRailItem(
            selected = tab == 2,
            onClick = onAzkar,
            icon = {
                val tint = if (tab == 2) GreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                AzkarNavIcon(tint = tint)
            },
            label = { Text(stringResource(R.string.nav_azkar)) },
            colors = colors
        )
        NavigationRailItem(
            selected = tab == 3,
            onClick = onSettings,
            icon = { Icon(Icons.Default.Settings, null) },
            label = { Text(stringResource(R.string.nav_settings)) },
            colors = colors
        )
    }
}
