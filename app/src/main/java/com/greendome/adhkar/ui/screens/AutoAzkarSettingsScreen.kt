package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.greendome.adhkar.data.local.ReciterEntity
import com.greendome.adhkar.data.model.PopupSettingsTarget
import com.greendome.adhkar.data.model.VoiceSettingsTarget
import com.greendome.adhkar.service.AdhkarReminderService
import com.greendome.adhkar.service.AfterPrayerAlarmScheduler
import com.greendome.adhkar.service.ReminderScheduler
import com.greendome.adhkar.ui.components.AutoReminderLockScreenSetting
import com.greendome.adhkar.ui.components.rememberLockScreenAccessRequester
import com.greendome.adhkar.ui.theme.GreenPrimaryDark

@Composable
fun AutoAzkarSettingsScreen(
    settings: SettingsRepository,
    reciters: List<ReciterEntity>,
    lang: String,
    autoAzkarEnabled: Boolean,
    autoAzkarRandom: Boolean,
    onToggleAutoAzkar: (Boolean) -> Unit,
    onAutoAzkarRandomChange: (Boolean) -> Unit,
    onPreviewVoice: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var autoAzkarOn by remember { mutableStateOf(autoAzkarEnabled) }
    var autoAzkarRandomMode by remember { mutableStateOf(autoAzkarRandom) }
    var afterPrayerOn by remember { mutableStateOf(settings.afterPrayerFromSalahEnabled) }
    var lockScreenEnabled by remember { mutableStateOf(settings.azkarAutoLockScreenEnabled) }
    val requestLockScreenAccess = rememberLockScreenAccessRequester()

    SettingsSubScreenScaffold(
        title = stringResource(R.string.settings_auto_azkar_title),
        onBack = onBack,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SectionTitle(
                    title = stringResource(R.string.auto_azkar_section),
                    subtitle = stringResource(R.string.auto_azkar_hint)
                )
            }
            item {
                SettingSwitch(
                    label = if (autoAzkarOn) stringResource(R.string.auto_azkar_on)
                    else stringResource(R.string.auto_azkar_off),
                    checked = autoAzkarOn,
                    onChange = {
                        autoAzkarOn = it
                        onToggleAutoAzkar(it)
                    }
                )
            }
            if (autoAzkarOn) {
                item {
                    SettingSwitch(
                        label = stringResource(R.string.auto_azkar_after_prayer_enable),
                        checked = afterPrayerOn,
                        onChange = {
                            afterPrayerOn = it
                            settings.afterPrayerFromSalahEnabled = it
                            AfterPrayerAlarmScheduler.reschedule(context)
                            if (settings.isServiceEnabled) {
                                ReminderScheduler.scheduleNext(context)
                                AdhkarReminderService.refreshNotification(context)
                            }
                        }
                    )
                }
                item { AutoAzkarModeLabel() }
                item {
                    AutoAzkarModeSelector(
                        randomMode = autoAzkarRandomMode,
                        onRandomModeChange = {
                            autoAzkarRandomMode = it
                            onAutoAzkarRandomChange(it)
                        }
                    )
                }
                item { ClockHourFormatLabel() }
                item {
                    Text(
                        stringResource(R.string.azkar_clock_format_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = GreenPrimaryDark.copy(alpha = 0.7f)
                    )
                }
                item {
                    var clockFormat by remember { mutableStateOf(settings.azkarClockHourFormat) }
                    ClockHourFormatSelector(
                        selected = clockFormat,
                        onSelected = {
                            clockFormat = it
                            settings.azkarClockHourFormat = it
                        }
                    )
                }
                item { AutoAzkarScheduleExplainer() }

                item {
                    SectionTitle(
                        title = stringResource(R.string.azkar_display_section),
                        subtitle = stringResource(R.string.azkar_auto_display_hint)
                    )
                }
                item {
                    var presentation by remember { mutableStateOf(settings.azkarPresentation) }
                    AutoReminderPresentationSelector(
                        selected = presentation,
                        onSelected = {
                            presentation = it
                            settings.azkarPresentation = it
                        }
                    )
                }
                item {
                    AutoReminderLockScreenSetting(
                        enabled = lockScreenEnabled,
                        onEnabledChange = {
                            lockScreenEnabled = it
                            settings.azkarAutoLockScreenEnabled = it
                        },
                        requestLockScreenAccess = requestLockScreenAccess,
                        titleRes = R.string.azkar_auto_lock_screen,
                        hintRes = R.string.azkar_auto_lock_screen_hint
                    )
                }

                item {
                    VoiceProfileSettingsSection(
                        settings = settings,
                        reciters = reciters,
                        lang = lang,
                        profile = VoiceSettingsTarget.AZKAR,
                        onPreviewVoice = onPreviewVoice
                    )
                }

                item {
                    PopupAppearanceSettings(
                        settings = settings,
                        target = PopupSettingsTarget.AZKAR,
                        title = stringResource(R.string.azkar_popup_settings_section),
                        subtitle = stringResource(R.string.azkar_popup_settings_hint)
                    )
                }
            }
        }
    }
}
