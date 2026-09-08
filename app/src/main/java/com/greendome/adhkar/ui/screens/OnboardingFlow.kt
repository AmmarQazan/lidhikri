package com.greendome.adhkar.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.greendome.adhkar.R
import com.greendome.adhkar.data.AutoAzkarCatalog
import com.greendome.adhkar.data.BlessedDaysAzkar
import com.greendome.adhkar.data.FridayAzkar
import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.model.AppThemeMode
import com.greendome.adhkar.data.model.ArabicFontStyle
import com.greendome.adhkar.data.model.AzkarCardText
import com.greendome.adhkar.data.model.AzkarListText
import com.greendome.adhkar.data.model.ClockHourFormat
import com.greendome.adhkar.data.model.azkarListFontSpMatchingCard
import com.greendome.adhkar.ui.components.AzkarFontSizeButtons
import com.greendome.adhkar.ui.theme.AppAccentGreen
import com.greendome.adhkar.ui.theme.brandLogoRes
import com.greendome.adhkar.ui.theme.GoldDome
import com.greendome.adhkar.ui.theme.arabicFontFamily
import com.greendome.adhkar.ui.theme.stringResourceDigits
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.prayer.LocationMode
import com.greendome.adhkar.prayer.PrayerLocation
import com.greendome.adhkar.service.PrayerPhoneSilent
import com.greendome.adhkar.ui.overlay.OverlayWindow
import com.greendome.adhkar.util.AppLanguages
import com.greendome.adhkar.util.AppSetupNeeds
import com.greendome.adhkar.util.LockScreenPermissions
import com.greendome.adhkar.util.RuntimePermissions
import com.greendome.adhkar.util.TasbihWindow

private enum class OnboardingPage {
    WELCOME,
    LANGUAGE,
    APPEARANCE,
    LOCATION,
    REMINDERS,
    PERMISSIONS,
}

private fun onboardingPages(): List<OnboardingPage> = listOf(
    OnboardingPage.WELCOME,
    OnboardingPage.LANGUAGE,
    OnboardingPage.APPEARANCE,
    OnboardingPage.LOCATION,
    OnboardingPage.REMINDERS,
    OnboardingPage.PERMISSIONS,
)

data class OnboardingResult(
    val language: String,
    val arabicFontStyle: ArabicFontStyle,
    val themeMode: AppThemeMode,
    val azkarCardFontSizeSp: Int = AzkarCardText.DEFAULT_FONT_SP,
    val azkarListFontSizeSp: Int = AzkarListText.DEFAULT_FONT_SP,
    val autoTasbihEnabled: Boolean = true,
    val autoAzkarEnabled: Boolean = true,
    val morningHour: Int = TasbihWindow.DEFAULT_MORNING_HOUR,
    val morningMinute: Int = TasbihWindow.DEFAULT_MORNING_MINUTE,
    val sleepHour: Int = TasbihWindow.DEFAULT_SLEEP_HOUR,
    val sleepMinute: Int = TasbihWindow.DEFAULT_SLEEP_MINUTE,
    val tasbihStartHour: Int = TasbihWindow.DEFAULT_MORNING_HOUR,
    val tasbihStartMinute: Int = TasbihWindow.DEFAULT_MORNING_MINUTE,
    val tasbihEndHour: Int = TasbihWindow.DEFAULT_SLEEP_HOUR,
    val tasbihEndMinute: Int = TasbihWindow.DEFAULT_SLEEP_MINUTE,
    val respectPrayerTime: Boolean = true,
    val prayerLocation: PrayerLocation? = null,
    val prayerLocationMode: LocationMode = LocationMode.MANUAL,
    val enabledAzkarCollectionIds: Set<String> = AutoAzkarCatalog.defaultEnabledClockIds(),
    val afterPrayerAzkarEnabled: Boolean = true,
    val afterAdhanAzkarEnabled: Boolean = true,
    val homeAzkarEnabled: Boolean = true,
    val homeLocation: PrayerLocation? = null,
    val ridingAzkarEnabled: Boolean = true,
    val silentDuringFardPrayer: Boolean = true,
    val clockHourFormat: ClockHourFormat = ClockHourFormat.HOUR_24
)

@Composable
fun OnboardingFlow(
    initialLanguage: String,
    initialFontStyle: ArabicFontStyle,
    initialThemeMode: AppThemeMode,
    initialStep: Int = 0,
    onLanguageChange: (String) -> Unit,
    onThemeChange: (AppThemeMode) -> Unit,
    onStepChange: (Int) -> Unit,
    onComplete: (OnboardingResult) -> Unit,
    azkarCollections: List<AdhkarCollectionEntity> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pages = remember { onboardingPages() }
    val totalSteps = pages.size
    var step by remember { mutableIntStateOf(initialStep.coerceIn(0, totalSteps - 1)) }
    var selectedLanguage by remember { mutableStateOf(AppLanguages.coerce(initialLanguage)) }
    var selectedFont by remember { mutableStateOf(initialFontStyle) }
    var selectedFontSp by remember { mutableIntStateOf(AzkarCardText.DEFAULT_FONT_SP) }
    var selectedTheme by remember { mutableStateOf(initialThemeMode) }
    var wantAutoTasbih by remember { mutableStateOf(true) }
    var wantAutoAzkar by remember { mutableStateOf(true) }
    var morningHour by remember { mutableIntStateOf(TasbihWindow.DEFAULT_MORNING_HOUR) }
    var morningMinute by remember { mutableIntStateOf(TasbihWindow.DEFAULT_MORNING_MINUTE) }
    var sleepHour by remember { mutableIntStateOf(TasbihWindow.DEFAULT_SLEEP_HOUR) }
    var sleepMinute by remember { mutableIntStateOf(TasbihWindow.DEFAULT_SLEEP_MINUTE) }
    var tasbihStartHour by remember { mutableIntStateOf(TasbihWindow.DEFAULT_MORNING_HOUR) }
    var tasbihStartMinute by remember { mutableIntStateOf(TasbihWindow.DEFAULT_MORNING_MINUTE) }
    var tasbihEndHour by remember { mutableIntStateOf(TasbihWindow.DEFAULT_SLEEP_HOUR) }
    var tasbihEndMinute by remember { mutableIntStateOf(TasbihWindow.DEFAULT_SLEEP_MINUTE) }
    var wantRespectPrayer by remember { mutableStateOf(true) }
    var afterPrayerAzkar by remember { mutableStateOf(true) }
    var afterAdhanAzkar by remember { mutableStateOf(true) }
    var homeAzkar by remember { mutableStateOf(true) }
    var ridingAzkar by remember { mutableStateOf(true) }
    var homeLocation by remember { mutableStateOf<PrayerLocation?>(null) }
    var enabledAzkarIds by remember { mutableStateOf(AutoAzkarCatalog.defaultEnabledClockIds()) }
    var prayerLocation by remember { mutableStateOf<PrayerLocation?>(null) }
    var prayerLocationMode by remember { mutableStateOf(LocationMode.MANUAL) }
    var showHomeAddressRequired by remember { mutableStateOf(false) }
    var showLocationRequired by remember { mutableStateOf(false) }

    fun homeAzkarNeedsAddress(): Boolean =
        wantAutoAzkar && homeAzkar && homeLocation == null

    fun prayerNeedsLocation(): Boolean = prayerLocation == null

    fun syncTasbihDefaults(enabled: Boolean) {
        if (enabled) {
            tasbihStartHour = morningHour
            tasbihStartMinute = morningMinute
            tasbihEndHour = sleepHour
            tasbihEndMinute = sleepMinute
        } else {
            tasbihStartHour = TasbihWindow.FALLBACK_START_HOUR
            tasbihStartMinute = TasbihWindow.FALLBACK_START_MINUTE
            tasbihEndHour = TasbihWindow.FALLBACK_END_HOUR
            tasbihEndMinute = TasbihWindow.FALLBACK_END_MINUTE
        }
    }

    fun finish() {
        onComplete(
            OnboardingResult(
                language = selectedLanguage,
                arabicFontStyle = selectedFont,
                azkarCardFontSizeSp = selectedFontSp,
                azkarListFontSizeSp = azkarListFontSpMatchingCard(selectedFontSp),
                themeMode = selectedTheme,
                autoTasbihEnabled = true,
                autoAzkarEnabled = wantAutoAzkar,
                morningHour = morningHour,
                morningMinute = morningMinute,
                sleepHour = sleepHour,
                sleepMinute = sleepMinute,
                tasbihStartHour = tasbihStartHour,
                tasbihStartMinute = tasbihStartMinute,
                tasbihEndHour = tasbihEndHour,
                tasbihEndMinute = tasbihEndMinute,
                respectPrayerTime = true,
                prayerLocation = prayerLocation,
                prayerLocationMode = prayerLocationMode,
                enabledAzkarCollectionIds = enabledAzkarIds,
                afterPrayerAzkarEnabled = afterPrayerAzkar,
                afterAdhanAzkarEnabled = afterAdhanAzkar,
                homeAzkarEnabled = homeAzkar,
                homeLocation = homeLocation,
                ridingAzkarEnabled = ridingAzkar,
                silentDuringFardPrayer = true,
                clockHourFormat = ClockHourFormat.HOUR_24
            )
        )
    }

    fun goNext() {
        val page = pages[step]
        if (page == OnboardingPage.LOCATION && prayerNeedsLocation()) {
            showLocationRequired = true
            return
        }
        if (page == OnboardingPage.REMINDERS && homeAzkarNeedsAddress()) {
            showHomeAddressRequired = true
            return
        }
        if (page == OnboardingPage.APPEARANCE) {
            onThemeChange(selectedTheme)
        }
        if (page == OnboardingPage.LANGUAGE && selectedLanguage != initialLanguage) {
            onStepChange(step + 1)
            onLanguageChange(selectedLanguage)
            return
        }
        val nextStep = step + 1
        if (step < totalSteps - 1) {
            step = nextStep
            onStepChange(nextStep)
        } else {
            finish()
        }
    }

    fun goBack() {
        if (step > 0) {
            val prevStep = step - 1
            step = prevStep
            onStepChange(prevStep)
        }
    }

    BackHandler(enabled = step > 0) { goBack() }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (pages[step]) {
                OnboardingPage.WELCOME -> OnboardingWelcomeStep()
                OnboardingPage.LANGUAGE -> OnboardingLanguageStep(
                    selected = selectedLanguage,
                    onSelect = { selectedLanguage = it }
                )
                OnboardingPage.APPEARANCE -> OnboardingAppearanceStep(
                    selectedFont = selectedFont,
                    onSelectFont = { selectedFont = it },
                    fontSp = selectedFontSp,
                    onFontSpChange = { selectedFontSp = it },
                    selectedTheme = selectedTheme,
                    onSelectTheme = {
                        selectedTheme = it
                        onThemeChange(it)
                    }
                )
                OnboardingPage.LOCATION -> OnboardingLocationStep(
                    prayerLocation = prayerLocation,
                    onPrayerLocationPicked = { loc, mode ->
                        prayerLocation = loc
                        prayerLocationMode = mode
                    }
                )
                OnboardingPage.PERMISSIONS -> OnboardingPermissionsStep()
                OnboardingPage.REMINDERS -> OnboardingRemindersStep(
                    language = selectedLanguage,
                    azkarCollections = azkarCollections,
                    wantAutoAzkar = wantAutoAzkar,
                    autoTasbihEnabled = wantAutoTasbih,
                    onWantAutoAzkarChange = {
                        wantAutoAzkar = it
                        if (wantAutoTasbih) syncTasbihDefaults(it)
                    },
                    enabledAzkarIds = enabledAzkarIds,
                    onEnabledAzkarIdsChange = { enabledAzkarIds = it },
                    afterPrayerAzkar = afterPrayerAzkar,
                    onAfterPrayerAzkarChange = { afterPrayerAzkar = it },
                    afterAdhanAzkar = afterAdhanAzkar,
                    onAfterAdhanAzkarChange = { afterAdhanAzkar = it },
                    homeAzkar = homeAzkar,
                    onHomeAzkarChange = { homeAzkar = it },
                    ridingAzkar = ridingAzkar,
                    onRidingAzkarChange = { ridingAzkar = it },
                    homeLocation = homeLocation,
                    onHomeLocationPicked = { homeLocation = it },
                    prayerTimesOn = prayerLocation != null
                )
            }
        }

        OnboardingFooter(
            step = step,
            totalSteps = totalSteps,
            isLastStep = step == totalSteps - 1,
            canGoBack = step > 0,
            onNext = { goNext() },
            onBack = { goBack() },
            onSkip = {
                when {
                    pages[step] == OnboardingPage.LOCATION && prayerNeedsLocation() -> {
                        showLocationRequired = true
                    }
                    pages[step] == OnboardingPage.REMINDERS && homeAzkarNeedsAddress() -> {
                        showHomeAddressRequired = true
                    }
                    pages[step] != OnboardingPage.PERMISSIONS &&
                        !AppSetupNeeds.allReady(context) -> {
                        val permissionsIndex = pages.indexOf(OnboardingPage.PERMISSIONS)
                        step = permissionsIndex
                        onStepChange(permissionsIndex)
                    }
                    else -> finish()
                }
            }
        )
    }
    }

    if (showLocationRequired) {
        AlertDialog(
            onDismissRequest = { showLocationRequired = false },
            title = { Text(stringResource(R.string.onboarding_location_required_title)) },
            text = { Text(stringResource(R.string.onboarding_location_required_message)) },
            confirmButton = {
                TextButton(onClick = { showLocationRequired = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    if (showHomeAddressRequired) {
        AlertDialog(
            onDismissRequest = { showHomeAddressRequired = false },
            title = { Text(stringResource(R.string.onboarding_home_address_required_title)) },
            text = { Text(stringResource(R.string.onboarding_home_address_required_message)) },
            confirmButton = {
                TextButton(onClick = { showHomeAddressRequired = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}

@Composable
private fun OnboardingWelcomeStep() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        Image(
            painter = painterResource(brandLogoRes()),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.56f)
                .height(128.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineSmall,
            color = AppAccentGreen(),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
            lineHeight = 26.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_settings_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(24.dp))
        OnboardingFeatureRow(
            icon = Icons.Default.Notifications,
            title = stringResource(R.string.onboarding_feature_tasbih_title),
            subtitle = stringResource(R.string.onboarding_feature_tasbih_subtitle)
        )
        OnboardingFeatureRow(
            icon = Icons.Default.Schedule,
            title = stringResource(R.string.onboarding_feature_azkar_title),
            subtitle = stringResource(R.string.onboarding_feature_azkar_subtitle)
        )
        OnboardingFeatureRow(
            icon = Icons.Default.VolumeUp,
            title = stringResource(R.string.onboarding_feature_adhan_title),
            subtitle = stringResource(R.string.onboarding_feature_adhan_subtitle)
        )
        OnboardingFeatureRow(
            icon = Icons.Default.CheckCircle,
            title = stringResource(R.string.onboarding_feature_after_prayer_title),
            subtitle = stringResource(R.string.onboarding_feature_after_prayer_subtitle)
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun OnboardingLanguageStep(
    selected: String,
    onSelect: (String) -> Unit
) {
    val languages = AppLanguages.pickerPairs()

    OnboardingStepScaffold(
        title = stringResource(R.string.onboarding_language_title),
        subtitle = stringResource(R.string.onboarding_language_subtitle)
    ) {
        languages.forEach { (code, label) ->
            OnboardingSelectionRow(
                label = label,
                selected = selected == code,
                onSelect = { onSelect(code) }
            )
        }
    }
}

@Composable
private fun OnboardingAppearanceStep(
    selectedFont: ArabicFontStyle,
    onSelectFont: (ArabicFontStyle) -> Unit,
    fontSp: Int,
    onFontSpChange: (Int) -> Unit,
    selectedTheme: AppThemeMode,
    onSelectTheme: (AppThemeMode) -> Unit
) {
    OnboardingStepScaffold(
        title = stringResource(R.string.onboarding_appearance_title),
        subtitle = stringResource(R.string.onboarding_appearance_subtitle)
    ) {
        OnboardingFontSizeRow(
            fontSp = fontSp,
            onDecrease = {
                onFontSpChange((fontSp - AzkarCardText.STEP_SP)
                    .coerceAtLeast(AzkarCardText.MIN_FONT_SP))
            },
            onIncrease = {
                onFontSpChange((fontSp + AzkarCardText.STEP_SP)
                    .coerceAtMost(AzkarCardText.MAX_FONT_SP))
            }
        )
        Spacer(Modifier.height(12.dp))
        ArabicFontStyle.entries.forEach { style ->
            OnboardingFontOptionRow(
                style = style,
                selected = selectedFont == style,
                fontSp = fontSp,
                onSelect = { onSelectFont(style) }
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.onboarding_theme_title),
            style = MaterialTheme.typography.titleMedium,
            color = AppAccentGreen(),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        AppThemeModeSelector(
            selected = selectedTheme,
            onSelected = onSelectTheme
        )
    }
}

@Composable
private fun OnboardingLocationStep(
    prayerLocation: PrayerLocation?,
    onPrayerLocationPicked: (PrayerLocation, LocationMode) -> Unit
) {
    OnboardingStepScaffold(
        title = stringResource(R.string.onboarding_location_title),
        subtitle = stringResource(R.string.onboarding_location_subtitle)
    ) {
        PrayerCityPicker(
            current = prayerLocation,
            onPicked = onPrayerLocationPicked
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun OnboardingPermissionsStep() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var notificationsOn by remember { mutableStateOf(AppSetupNeeds.notificationsReady(context)) }
    var overlayOn by remember { mutableStateOf(AppSetupNeeds.overlayReady(context)) }
    var lockOn by remember { mutableStateOf(AppSetupNeeds.lockScreenReady(context)) }
    var silentOn by remember { mutableStateOf(AppSetupNeeds.prayerSilentPolicyReady(context)) }
    var awaitingSettings by remember { mutableStateOf(false) }
    var notificationsAsked by remember { mutableStateOf(false) }
    val advanceRef = remember { arrayOf({}) }

    fun refreshStatus() {
        notificationsOn = AppSetupNeeds.notificationsReady(context)
        overlayOn = AppSetupNeeds.overlayReady(context)
        lockOn = AppSetupNeeds.lockScreenReady(context)
        silentOn = AppSetupNeeds.prayerSilentPolicyReady(context)
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        refreshStatus()
        advanceRef[0]()
    }

    fun requestNextMissing() {
        refreshStatus()
        when {
            !AppSetupNeeds.notificationsReady(context) && !notificationsAsked -> {
                val permission = RuntimePermissions.postNotificationsPermission()
                if (permission != null) {
                    notificationsAsked = true
                    notificationLauncher.launch(permission)
                } else {
                    notificationsAsked = true
                    requestNextMissing()
                }
            }
            !AppSetupNeeds.overlayReady(context) -> {
                awaitingSettings = true
                OverlayWindow.openPermissionSettings(context)
            }
            !AppSetupNeeds.lockScreenReady(context) -> {
                awaitingSettings = true
                LockScreenPermissions.openFullScreenIntentSettings(context)
            }
            !AppSetupNeeds.prayerSilentPolicyReady(context) -> {
                awaitingSettings = true
                PrayerPhoneSilent.openPolicySettings(context)
            }
        }
    }
    advanceRef[0] = { requestNextMissing() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshStatus()
                if (awaitingSettings) {
                    awaitingSettings = false
                    if (!AppSetupNeeds.allReady(context)) {
                        advanceRef[0]()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val allReady = notificationsOn && overlayOn && lockOn && silentOn

    OnboardingStepScaffold(
        title = stringResource(R.string.onboarding_permissions_title),
        subtitle = stringResource(R.string.onboarding_permissions_subtitle)
    ) {
        OnboardingNeedRow(
            granted = notificationsOn,
            pendingIcon = Icons.Filled.Notifications,
            label = stringResource(R.string.onboarding_need_notifications)
        )
        OnboardingNeedRow(
            granted = overlayOn,
            pendingIcon = Icons.Filled.Layers,
            label = stringResource(R.string.onboarding_need_overlay)
        )
        OnboardingNeedRow(
            granted = lockOn,
            pendingIcon = Icons.Filled.Lock,
            label = stringResource(R.string.onboarding_need_lock_screen)
        )
        OnboardingNeedRow(
            granted = silentOn,
            pendingIcon = Icons.Filled.Vibration,
            label = stringResource(R.string.onboarding_need_silent_prayer)
        )
        Spacer(Modifier.height(16.dp))
        if (!allReady) {
            Button(
                onClick = {
                    SettingsRepository(context).apply {
                        silentDuringFardPrayer = true
                        tasbihAutoLockScreenEnabled = true
                        azkarAutoLockScreenEnabled = true
                        adhanAutoLockScreenEnabled = true
                    }
                    requestNextMissing()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.onboarding_permissions_action))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.onboarding_permissions_later),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun OnboardingNeedRow(
    granted: Boolean,
    pendingIcon: ImageVector,
    label: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (granted) Icons.Filled.CheckCircle else pendingIcon,
            contentDescription = null,
            tint = if (granted) AppAccentGreen() else GoldDome,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = if (granted) AppAccentGreen() else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (granted) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun OnboardingRemindersStep(
    language: String,
    azkarCollections: List<AdhkarCollectionEntity>,
    wantAutoAzkar: Boolean,
    autoTasbihEnabled: Boolean,
    onWantAutoAzkarChange: (Boolean) -> Unit,
    enabledAzkarIds: Set<String>,
    onEnabledAzkarIdsChange: (Set<String>) -> Unit,
    afterPrayerAzkar: Boolean,
    onAfterPrayerAzkarChange: (Boolean) -> Unit,
    afterAdhanAzkar: Boolean,
    onAfterAdhanAzkarChange: (Boolean) -> Unit,
    homeAzkar: Boolean,
    onHomeAzkarChange: (Boolean) -> Unit,
    ridingAzkar: Boolean,
    onRidingAzkarChange: (Boolean) -> Unit,
    homeLocation: PrayerLocation?,
    onHomeLocationPicked: (PrayerLocation) -> Unit,
    prayerTimesOn: Boolean
) {
    OnboardingStepScaffold(
        title = stringResource(R.string.onboarding_auto_azkar_title),
        subtitle = stringResource(
            if (autoTasbihEnabled) R.string.onboarding_auto_azkar_subtitle
            else R.string.onboarding_auto_azkar_subtitle_no_tasbih
        )
    ) {
        OnboardingSelectionRow(
            label = stringResource(R.string.onboarding_auto_azkar_yes),
            selected = wantAutoAzkar,
            onSelect = { onWantAutoAzkarChange(true) }
        )
        OnboardingSelectionRow(
            label = stringResource(R.string.onboarding_auto_azkar_no),
            selected = !wantAutoAzkar,
            onSelect = { onWantAutoAzkarChange(false) }
        )
        if (wantAutoAzkar) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.onboarding_azkar_sections_title),
                style = MaterialTheme.typography.titleSmall,
                color = AppAccentGreen(),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.onboarding_azkar_sections_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp)
            )
            AutoAzkarCatalog.onboardingSpecs().forEach { spec ->
                val checked = when (spec.trigger) {
                    AutoAzkarCatalog.Trigger.PRAYER -> afterPrayerAzkar
                    AutoAzkarCatalog.Trigger.ADHAN -> afterAdhanAzkar
                    AutoAzkarCatalog.Trigger.LOCATION -> homeAzkar
                    AutoAzkarCatalog.Trigger.ACTIVITY -> ridingAzkar
                    AutoAzkarCatalog.Trigger.CLOCK -> spec.id in enabledAzkarIds
                }
                val collection = azkarCollections.find { it.id == spec.id }
                val label = if (collection != null) {
                    azkarCollectionTitle(collection, language)
                } else {
                    AutoAzkarCatalog.displayTitle(spec.id, language)
                }
                OnboardingSectionToggle(
                    label = label,
                    checked = checked,
                    hint = when {
                        spec.trigger == AutoAzkarCatalog.Trigger.PRAYER -> stringResource(
                            if (prayerTimesOn) R.string.onboarding_azkar_after_prayer_hint
                            else R.string.onboarding_azkar_after_prayer_needs_prayer
                        )
                        spec.trigger == AutoAzkarCatalog.Trigger.ADHAN ->
                            stringResource(R.string.onboarding_azkar_after_adhan_hint)
                        spec.trigger == AutoAzkarCatalog.Trigger.LOCATION -> stringResource(
                            if (homeLocation != null) R.string.onboarding_azkar_home_hint
                            else R.string.onboarding_azkar_home_needs_location
                        )
                        spec.trigger == AutoAzkarCatalog.Trigger.ACTIVITY ->
                            stringResource(R.string.onboarding_azkar_riding_hint)
                        spec.id == FridayAzkar.COLLECTION_ID -> stringResource(R.string.onboarding_azkar_friday_hint)
                        spec.id == BlessedDaysAzkar.COLLECTION_ID -> stringResource(R.string.onboarding_azkar_blessed_days_hint)
                        else -> null
                    },
                    onCheckedChange = { on ->
                        when (spec.trigger) {
                            AutoAzkarCatalog.Trigger.PRAYER -> onAfterPrayerAzkarChange(on)
                            AutoAzkarCatalog.Trigger.ADHAN -> onAfterAdhanAzkarChange(on)
                            AutoAzkarCatalog.Trigger.LOCATION -> onHomeAzkarChange(on)
                            AutoAzkarCatalog.Trigger.ACTIVITY -> onRidingAzkarChange(on)
                            AutoAzkarCatalog.Trigger.CLOCK -> {
                                if (on) onEnabledAzkarIdsChange(enabledAzkarIds + spec.id)
                                else onEnabledAzkarIdsChange(enabledAzkarIds - spec.id)
                            }
                        }
                    }
                )
            }
            if (homeAzkar) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.home_azkar_location_section),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppAccentGreen(),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.home_azkar_location_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp)
                )
                HomeAddressPicker(
                    current = homeLocation,
                    onPicked = onHomeLocationPicked
                )
            }
        }
    }
}

@Composable
private fun OnboardingStepScaffold(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = AppAccentGreen(),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        content()
    }
}

@Composable
private fun OnboardingSelectionRow(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onSelect: () -> Unit
) {
    val background = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(enabled = enabled, onClick = onSelect)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = when {
                selected -> AppAccentGreen()
                !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun OnboardingSectionToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    hint: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (checked) MaterialTheme.colorScheme.surface else Color.Transparent)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = if (checked) AppAccentGreen() else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal
            )
            if (!hint.isNullOrBlank()) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun OnboardingFontSizeRow(
    fontSp: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    val percent = ((fontSp * 100f) / AzkarCardText.DEFAULT_FONT_SP).toInt()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AzkarFontSizeButtons(
            fontSp = fontSp,
            minSp = AzkarCardText.MIN_FONT_SP,
            maxSp = AzkarCardText.MAX_FONT_SP,
            onDecrease = onDecrease,
            onIncrease = onIncrease
        )
        Text(
            text = stringResourceDigits(R.string.onboarding_font_size_value, percent),
            style = MaterialTheme.typography.titleSmall,
            color = AppAccentGreen(),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(end = 10.dp)
        )
    }
}

@Composable
private fun OnboardingFontOptionRow(
    style: ArabicFontStyle,
    selected: Boolean,
    fontSp: Int,
    onSelect: () -> Unit
) {
    val background = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = stringResource(onboardingFontStyleLabelRes(style)),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.onboarding_font_preview),
            style = TextStyle(
                fontFamily = arabicFontFamily(style),
                fontSize = fontSp.sp,
                lineHeight = (fontSp * AzkarCardText.LINE_HEIGHT_RATIO).sp,
                textAlign = TextAlign.Start
            ),
            color = AppAccentGreen(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun OnboardingFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GoldDome,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = AppAccentGreen(),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun OnboardingFooter(
    step: Int,
    totalSteps: Int,
    isLastStep: Boolean,
    canGoBack: Boolean,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OnboardingPageIndicator(
            currentStep = step,
            totalSteps = totalSteps,
            modifier = Modifier.padding(bottom = 20.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLastStep) {
                if (canGoBack) {
                    TextButton(onClick = onBack) {
                        Text(
                            text = stringResource(R.string.onboarding_back),
                            color = AppAccentGreen(),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Button(
                    onClick = onNext,
                    modifier = if (canGoBack) Modifier else Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_start),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onSkip) {
                        Text(
                            text = stringResource(R.string.onboarding_skip),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    if (canGoBack) {
                        TextButton(onClick = onBack) {
                            Text(
                                text = stringResource(R.string.onboarding_back),
                                color = AppAccentGreen(),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_next),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            val active = index == currentStep
            Box(
                modifier = Modifier
                    .then(
                        if (active) Modifier
                            .width(22.dp)
                            .height(8.dp)
                        else Modifier.size(8.dp)
                    )
                    .clip(CircleShape)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    )
            )
        }
    }
}

private fun onboardingFontStyleLabelRes(style: ArabicFontStyle): Int = when (style) {
    ArabicFontStyle.DEFAULT -> R.string.font_style_default
    ArabicFontStyle.UTHMANI_1 -> R.string.font_style_uthmani_1
    ArabicFontStyle.UTHMANI_2 -> R.string.font_style_uthmani_2
    ArabicFontStyle.INDOPAK_1 -> R.string.font_style_indopak_1
    ArabicFontStyle.INDOPAK_2 -> R.string.font_style_indopak_2
    ArabicFontStyle.BENGALI -> R.string.font_style_bengali
}
