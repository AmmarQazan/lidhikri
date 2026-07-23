package com.greendome.adhkar.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.ReciterEntity
import com.greendome.adhkar.data.model.ArabicFontStyle
import com.greendome.adhkar.data.model.NumberDigitStyle
import com.greendome.adhkar.service.OfflineDownloadHelper
import com.greendome.adhkar.ui.theme.stringResourceDigits
import com.greendome.adhkar.util.RuntimePermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private enum class SettingsDestination {
    HUB, AUTO_TASBIH, AUTO_AZKAR, MISBAHA, DHIKR_OF_DAY, VERSION
}

@OptIn(ExperimentalMaterial3Api::class)
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
    onLanguageChanged: () -> Unit = {},
    onFontScaleChanged: () -> Unit = {},
    onArabicFontChanged: () -> Unit = {},
    onThemeModeChanged: () -> Unit = {},
    onNumberDigitStyleChanged: (NumberDigitStyle) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var destination by remember { mutableStateOf(SettingsDestination.HUB) }

    if (destination != SettingsDestination.HUB) {
        BackHandler { destination = SettingsDestination.HUB }
    }

    when (destination) {
        SettingsDestination.HUB -> SettingsHubScreen(
            settings = settings,
            lang = lang,
            onOpenAutoTasbih = { destination = SettingsDestination.AUTO_TASBIH },
            onOpenAutoAzkar = { destination = SettingsDestination.AUTO_AZKAR },
            onOpenMisbaha = { destination = SettingsDestination.MISBAHA },
            onOpenDhikrOfDay = { destination = SettingsDestination.DHIKR_OF_DAY },
            onOpenVersionInfo = { destination = SettingsDestination.VERSION },
            onLanguageChanged = onLanguageChanged,
            onFontScaleChanged = onFontScaleChanged,
            onArabicFontChanged = onArabicFontChanged,
            onThemeModeChanged = onThemeModeChanged,
            onNumberDigitStyleChanged = onNumberDigitStyleChanged,
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
        SettingsDestination.AUTO_AZKAR -> AutoAzkarSettingsScreen(
            settings = settings,
            autoAzkarEnabled = autoAzkarEnabled,
            autoAzkarRandom = autoAzkarRandom,
            onToggleAutoAzkar = onToggleAutoAzkar,
            onAutoAzkarRandomChange = onAutoAzkarRandomChange,
            onBack = { destination = SettingsDestination.HUB },
            modifier = modifier
        )
        SettingsDestination.MISBAHA -> MisbahaSettingsScreen(
            settings = settings,
            onBack = { destination = SettingsDestination.HUB },
            modifier = modifier
        )
        SettingsDestination.DHIKR_OF_DAY -> DhikrOfDaySettingsScreen(
            settings = settings,
            onBack = { destination = SettingsDestination.HUB },
            modifier = modifier
        )
        SettingsDestination.VERSION -> VersionInfoScreen(
            lang = lang,
            onBack = { destination = SettingsDestination.HUB },
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsHubScreen(
    settings: SettingsRepository,
    lang: String,
    onOpenAutoTasbih: () -> Unit,
    onOpenAutoAzkar: () -> Unit,
    onOpenMisbaha: () -> Unit,
    onOpenDhikrOfDay: () -> Unit,
    onOpenVersionInfo: () -> Unit,
    onLanguageChanged: () -> Unit,
    onFontScaleChanged: () -> Unit,
    onArabicFontChanged: () -> Unit,
    onThemeModeChanged: () -> Unit,
    onNumberDigitStyleChanged: (NumberDigitStyle) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pauseCalls by remember { mutableStateOf(settings.pauseDuringCalls) }
    var pauseMedia by remember { mutableStateOf(settings.pauseDuringMedia) }
    var langExpanded by remember { mutableStateOf(false) }
    val languages = listOf("ar" to "العربية", "en" to "English", "fr" to "Français", "es" to "Español")
    var selectedLang by remember { mutableStateOf(settings.appLanguage) }
    var downloading by remember { mutableStateOf(false) }
    var fontScale by remember { mutableStateOf(settings.fontScale) }
    var arabicFontStyle by remember { mutableStateOf(settings.arabicFontStyle) }
    var themeMode by remember { mutableStateOf(settings.appThemeMode) }
    var numberDigitStyle by remember { mutableStateOf(settings.numberDigitStyle) }
    var azkarDisplayMode by remember { mutableStateOf(settings.azkarDisplayMode) }
    var ttsGender by remember { mutableStateOf(settings.ttsVoiceGender) }
    var pendingPauseCallsEnable by remember { mutableStateOf(false) }

    val phoneStatePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (pendingPauseCallsEnable) {
            pendingPauseCallsEnable = false
            if (granted) {
                pauseCalls = true
                settings.pauseDuringCalls = true
            }
        }
    }

    fun updatePauseDuringCalls(enabled: Boolean) {
        if (enabled && !RuntimePermissions.hasReadPhoneState(context)) {
            pendingPauseCallsEnable = true
            phoneStatePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        } else {
            pauseCalls = enabled
            settings.pauseDuringCalls = enabled
        }
    }

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
                    title = stringResource(R.string.settings_misbaha_title),
                    subtitle = stringResource(R.string.settings_misbaha_subtitle),
                    onClick = onOpenMisbaha
                )
            }
            item {
                SettingsNavCard(
                    title = stringResource(R.string.settings_dhikr_of_day_title),
                    subtitle = stringResource(R.string.settings_dhikr_of_day_subtitle),
                    onClick = onOpenDhikrOfDay
                )
            }
            item {
                SettingsNavCard(
                    title = stringResource(R.string.version_info_section),
                    subtitle = stringResource(R.string.settings_version_subtitle),
                    onClick = onOpenVersionInfo
                )
            }

            item {
                SectionTitle(
                    title = stringResource(R.string.reading_theme_section),
                    subtitle = stringResource(R.string.reading_theme_hint)
                )
            }
            item {
                AppThemeModeSelector(
                    selected = themeMode,
                    onSelected = {
                        themeMode = it
                        settings.appThemeMode = it
                        onThemeModeChanged()
                    }
                )
            }

            item {
                SectionTitle(
                    title = stringResource(R.string.app_digits_section),
                    subtitle = stringResource(R.string.app_digits_hint),
                )
            }
            item {
                NumberDigitStyleSelector(
                    selected = numberDigitStyle,
                    onSelected = { style ->
                        numberDigitStyle = style
                        settings.numberDigitStyle = style
                        onNumberDigitStyleChanged(style)
                    },
                )
            }

            item {
                SectionTitle(
                    title = stringResource(R.string.azkar_display_section),
                    subtitle = stringResource(R.string.azkar_display_hint)
                )
            }
            item {
                AzkarDisplayModeSelector(
                    selected = azkarDisplayMode,
                    onSelected = {
                        azkarDisplayMode = it
                        settings.azkarDisplayMode = it
                    }
                )
            }

            item {
                SectionTitle(
                    title = stringResource(R.string.font_scale_title),
                    subtitle = stringResource(R.string.font_scale_hint)
                )
            }
            item {
                Slider(
                    value = fontScale,
                    onValueChange = {
                        fontScale = it
                        settings.fontScale = it
                        onFontScaleChanged()
                    },
                    valueRange = 0.8f..1.5f,
                    steps = 6
                )
            }
            item {
                Text(
                    stringResourceDigits(R.string.font_scale_value, (fontScale * 100).toInt()),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            item {
                SectionTitle(
                    title = stringResource(R.string.arabic_font_title),
                    subtitle = stringResource(R.string.arabic_font_hint)
                )
            }
            item {
                ArabicFontStyleSelector(
                    selected = arabicFontStyle,
                    onSelected = {
                        arabicFontStyle = it
                        settings.arabicFontStyle = it
                        onArabicFontChanged()
                    }
                )
            }

            item { SectionTitle(title = stringResource(R.string.general_settings)) }
            item {
                SettingSwitch(stringResource(R.string.pause_during_calls), pauseCalls) {
                    updatePauseDuringCalls(it)
                }
            }
            item {
                SettingSwitch(stringResource(R.string.pause_during_media), pauseMedia) {
                    pauseMedia = it; settings.pauseDuringMedia = it
                }
            }

            item {
                SectionTitle(
                    title = stringResource(R.string.tts_voice_section),
                    subtitle = stringResource(R.string.tts_voice_hint)
                )
            }
            item {
                TtsVoiceGenderSelector(
                    selected = ttsGender,
                    onSelected = {
                        ttsGender = it
                        settings.ttsVoiceGender = it
                    }
                )
            }

            item { FieldLabel(stringResource(R.string.language)) }
            item {
                ExposedDropdownMenuBox(expanded = langExpanded, onExpandedChange = { langExpanded = it }) {
                    OutlinedTextField(
                        value = languages.find { it.first == selectedLang }?.second ?: selectedLang,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(langExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = langExpanded, onDismissRequest = { langExpanded = false }) {
                        languages.forEach { (code, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    if (selectedLang != code) {
                                        selectedLang = code
                                        settings.appLanguage = code
                                        onLanguageChanged()
                                    }
                                    langExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item { SectionTitle(title = stringResource(R.string.permissions_section)) }
            item {
                Button(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        })
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.battery_optimization))
                }
            }
            item {
                Button(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                        })
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.permission_overlay))
                }
            }
            item {
                Button(
                    onClick = {
                        downloading = true
                        scope.launch(Dispatchers.IO) {
                            OfflineDownloadHelper.downloadAllPending(context)
                            downloading = false
                        }
                    },
                    enabled = !downloading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (downloading) stringResource(R.string.downloading) else stringResource(R.string.download_all))
                }
            }
        }
    }
}
