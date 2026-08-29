package com.greendome.adhkar.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.ReciterEntity
import com.greendome.adhkar.sync.RemoteContentSync
import com.greendome.adhkar.data.model.NumberDigitStyle
import com.greendome.adhkar.ui.theme.stringResourceDigits

private enum class SettingsDestination {
    HUB, AUTO_TASBIH, PRAYER_RESPECT, AUTO_AZKAR, DISPLAY, APP_GENERAL, MISBAHA, WIDGETS, DHIKR_OF_DAY, MISBAHA_WIDGET, VERSION
}

@Composable
fun SettingsScreen(
    settings: SettingsRepository,
    reciters: List<ReciterEntity>,
    lang: String,
    isServiceOn: Boolean,
    autoAzkarEnabled: Boolean,
    autoAzkarRandom: Boolean,
    onToggleService: () -> Unit,
    onToggleAutoAzkar: (Boolean) -> Unit,
    onAutoAzkarRandomChange: (Boolean) -> Unit,
    onPreviewVoice: () -> Unit,
    onPreviewAzkarVoice: () -> Unit,
    onLanguageChanged: () -> Unit = {},
    onFontScaleChanged: () -> Unit = {},
    onArabicFontChanged: () -> Unit = {},
    onThemeModeChanged: () -> Unit = {},
    onNumberDigitStyleChanged: (NumberDigitStyle) -> Unit = {},
    openPrayerRespect: Boolean = false,
    onOpenPrayerRespectConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var destination by remember { mutableStateOf(SettingsDestination.HUB) }
    var prayerRespectBack by remember { mutableStateOf(SettingsDestination.APP_GENERAL) }
    LaunchedEffect(openPrayerRespect) {
        if (openPrayerRespect) {
            prayerRespectBack = SettingsDestination.APP_GENERAL
            destination = SettingsDestination.PRAYER_RESPECT
            onOpenPrayerRespectConsumed()
        }
    }

    val backTarget = when (destination) {
        SettingsDestination.HUB -> null
        SettingsDestination.PRAYER_RESPECT -> prayerRespectBack
        SettingsDestination.DHIKR_OF_DAY,
        SettingsDestination.MISBAHA_WIDGET -> SettingsDestination.WIDGETS
        else -> SettingsDestination.HUB
    }
    if (backTarget != null) {
        BackHandler { destination = backTarget }
    }

    when (destination) {
        SettingsDestination.HUB -> SettingsHubScreen(
            settings = settings,
            onOpenAutoTasbih = { destination = SettingsDestination.AUTO_TASBIH },
            onOpenAutoAzkar = { destination = SettingsDestination.AUTO_AZKAR },
            onOpenDisplay = { destination = SettingsDestination.DISPLAY },
            onOpenAppGeneral = { destination = SettingsDestination.APP_GENERAL },
            onOpenMisbaha = { destination = SettingsDestination.MISBAHA },
            onOpenWidgets = { destination = SettingsDestination.WIDGETS },
            onOpenVersionInfo = { destination = SettingsDestination.VERSION },
            modifier = modifier
        )
        SettingsDestination.AUTO_TASBIH -> AutoTasbihSettingsScreen(
            settings = settings,
            reciters = reciters,
            lang = lang,
            isServiceOn = isServiceOn,
            onToggleService = onToggleService,
            onPreviewVoice = onPreviewVoice,
            onBack = { destination = SettingsDestination.HUB },
            modifier = modifier
        )
        SettingsDestination.PRAYER_RESPECT -> PrayerRespectSettingsScreen(
            settings = settings,
            onBack = { destination = prayerRespectBack },
            modifier = modifier
        )
        SettingsDestination.AUTO_AZKAR -> AutoAzkarSettingsScreen(
            settings = settings,
            reciters = reciters,
            lang = lang,
            autoAzkarEnabled = autoAzkarEnabled,
            autoAzkarRandom = autoAzkarRandom,
            onToggleAutoAzkar = onToggleAutoAzkar,
            onAutoAzkarRandomChange = onAutoAzkarRandomChange,
            onPreviewVoice = onPreviewAzkarVoice,
            onBack = { destination = SettingsDestination.HUB },
            modifier = modifier
        )
        SettingsDestination.DISPLAY -> DisplayAppearanceSettingsScreen(
            settings = settings,
            onFontScaleChanged = onFontScaleChanged,
            onArabicFontChanged = onArabicFontChanged,
            onThemeModeChanged = onThemeModeChanged,
            onNumberDigitStyleChanged = onNumberDigitStyleChanged,
            onBack = { destination = SettingsDestination.HUB },
            modifier = modifier
        )
        SettingsDestination.APP_GENERAL -> AppGeneralSettingsScreen(
            settings = settings,
            onLanguageChanged = onLanguageChanged,
            onOpenPrayerRespect = {
                prayerRespectBack = SettingsDestination.APP_GENERAL
                destination = SettingsDestination.PRAYER_RESPECT
            },
            onBack = { destination = SettingsDestination.HUB },
            modifier = modifier
        )
        SettingsDestination.MISBAHA -> MisbahaSettingsScreen(
            settings = settings,
            onBack = { destination = SettingsDestination.HUB },
            modifier = modifier
        )
        SettingsDestination.WIDGETS -> WidgetsSettingsScreen(
            settings = settings,
            onOpenDhikrOfDay = { destination = SettingsDestination.DHIKR_OF_DAY },
            onOpenMisbahaWidget = { destination = SettingsDestination.MISBAHA_WIDGET },
            onBack = { destination = SettingsDestination.HUB },
            modifier = modifier
        )
        SettingsDestination.DHIKR_OF_DAY -> DhikrOfDaySettingsScreen(
            settings = settings,
            onBack = { destination = SettingsDestination.WIDGETS },
            modifier = modifier
        )
        SettingsDestination.MISBAHA_WIDGET -> MisbahaWidgetSettingsScreen(
            settings = settings,
            onBack = { destination = SettingsDestination.WIDGETS },
            modifier = modifier
        )
        SettingsDestination.VERSION -> VersionInfoScreen(
            settings = settings,
            lang = lang,
            onBack = { destination = SettingsDestination.HUB },
            onSyncContent = {
                RemoteContentSync.syncIfNeeded(
                    context,
                    settings,
                    AdhkarDatabase.get(context),
                    force = true
                )
            },
            modifier = modifier
        )
    }
}

@Composable
private fun SettingsHubScreen(
    settings: SettingsRepository,
    onOpenAutoTasbih: () -> Unit,
    onOpenAutoAzkar: () -> Unit,
    onOpenDisplay: () -> Unit,
    onOpenAppGeneral: () -> Unit,
    onOpenMisbaha: () -> Unit,
    onOpenWidgets: () -> Unit,
    onOpenVersionInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                ScreenTitle(
                    title = stringResource(R.string.settings_hub_section),
                    subtitle = stringResource(R.string.settings_hub_hint)
                )
            }
            item {
                SettingsNavCard(
                    title = stringResource(R.string.settings_auto_tasbih_title),
                    subtitle = stringResource(R.string.settings_auto_tasbih_subtitle),
                    onClick = onOpenAutoTasbih
                )
            }
            item {
                SettingsNavCard(
                    title = stringResource(R.string.settings_auto_azkar_title),
                    subtitle = stringResource(R.string.settings_auto_azkar_subtitle),
                    onClick = onOpenAutoAzkar
                )
            }
            item {
                SettingsNavCard(
                    title = stringResource(R.string.settings_display_title),
                    subtitle = stringResource(R.string.settings_display_subtitle),
                    onClick = onOpenDisplay
                )
            }
            item {
                SettingsNavCard(
                    title = stringResource(R.string.settings_app_general_title),
                    subtitle = stringResource(R.string.settings_app_general_subtitle),
                    onClick = onOpenAppGeneral
                )
            }
            item {
                SettingsNavCard(
                    title = stringResource(R.string.settings_misbaha_title),
                    subtitle = stringResource(R.string.settings_misbaha_subtitle),
                    onClick = onOpenMisbaha
                )
            }
            item {
                SettingsNavCard(
                    title = stringResource(R.string.settings_widgets_title),
                    subtitle = stringResource(R.string.settings_widgets_subtitle),
                    onClick = onOpenWidgets
                )
            }
            item {
                val contentPackVersion = settings.remoteContentVersion
                SettingsNavCard(
                    title = stringResource(R.string.version_info_section),
                    subtitle = if (contentPackVersion > 0) {
                        stringResourceDigits(R.string.settings_version_subtitle_with_pack, contentPackVersion)
                    } else {
                        stringResource(R.string.settings_version_subtitle)
                    },
                    onClick = onOpenVersionInfo
                )
            }
        }
    }
}