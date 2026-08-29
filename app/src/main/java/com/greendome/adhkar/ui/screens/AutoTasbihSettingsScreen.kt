package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.greendome.adhkar.service.ReminderScheduler
import com.greendome.adhkar.ui.components.AutoReminderLockScreenSetting
import com.greendome.adhkar.ui.components.rememberLockScreenAccessRequester
import com.greendome.adhkar.ui.theme.formatLocalizedDigits
import com.greendome.adhkar.util.TasbihWindow

@Composable
fun AutoTasbihSettingsScreen(
    settings: SettingsRepository,
    reciters: List<ReciterEntity>,
    lang: String,
    isServiceOn: Boolean,
    onToggleService: () -> Unit,
    onPreviewVoice: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var interval by remember { mutableStateOf(settings.intervalMinutes.toFloat()) }
    var randomMode by remember { mutableStateOf(settings.randomMode) }
    var lockScreenEnabled by remember { mutableStateOf(settings.tasbihAutoLockScreenEnabled) }
    var startHour by remember { mutableIntStateOf(settings.tasbihStartHour) }
    var startMinute by remember { mutableIntStateOf(settings.tasbihStartMinute) }
    var endHour by remember { mutableIntStateOf(settings.tasbihEndHour) }
    var endMinute by remember { mutableIntStateOf(settings.tasbihEndMinute) }
    val requestLockScreenAccess = rememberLockScreenAccessRequester()
    val overnight = TasbihWindow(startHour, startMinute, endHour, endMinute).isOvernight()

    fun persistWindowAndReschedule() {
        settings.setTasbihWindow(startHour, startMinute, endHour, endMinute)
        if (settings.isServiceEnabled) ReminderScheduler.scheduleNext(context)
    }

    SettingsSubScreenScaffold(
        title = stringResource(R.string.settings_auto_tasbih_title),
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
                    title = stringResource(R.string.auto_tasbih_section),
                    subtitle = stringResource(R.string.auto_tasbih_hint)
                )
            }
            item {
                Text(
                    stringResource(R.string.auto_tasbih_audio_when_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            item {
                SettingSwitch(
                    label = stringResource(R.string.auto_tasbih_enable),
                    checked = isServiceOn,
                    onChange = { onToggleService() }
                )
            }
            if (isServiceOn) {
                item { Text(stringResource(R.string.interval_minutes)) }
                item {
                    Slider(
                        value = interval,
                        onValueChange = {
                            interval = it
                            settings.intervalMinutes = it.toInt().coerceAtLeast(1)
                            if (settings.isServiceEnabled) ReminderScheduler.scheduleNext(context)
                        },
                        valueRange = 1f..60f,
                        steps = 58
                    )
                }
                item { Text("${interval.toInt().formatLocalizedDigits()} ${stringResource(R.string.minute_label)}") }

                item {
                    SectionTitle(
                        title = stringResource(R.string.tasbih_window_section),
                        subtitle = stringResource(R.string.tasbih_window_hint)
                    )
                }
                item {
                    TimeOfDaySetting(
                        label = stringResource(R.string.tasbih_first_time),
                        hour = startHour,
                        minute = startMinute,
                        onHourChange = { startHour = it; persistWindowAndReschedule() },
                        onMinuteChange = { startMinute = it; persistWindowAndReschedule() }
                    )
                }
                item {
                    TimeOfDaySetting(
                        label = stringResource(R.string.tasbih_last_time),
                        hour = endHour,
                        minute = endMinute,
                        onHourChange = { endHour = it; persistWindowAndReschedule() },
                        onMinuteChange = { endMinute = it; persistWindowAndReschedule() }
                    )
                }
                if (overnight) {
                    item {
                        Text(
                            stringResource(R.string.tasbih_window_overnight_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    SettingSwitch(stringResource(R.string.random_mode), randomMode) {
                        randomMode = it; settings.randomMode = it
                    }
                }
                item {
                    SettingSwitch(stringResource(R.string.sequential_mode), !randomMode) {
                        randomMode = !it; settings.randomMode = !it
                    }
                }

                item {
                    SectionTitle(
                        title = stringResource(R.string.tasbih_display_section),
                        subtitle = stringResource(R.string.tasbih_display_hint)
                    )
                }
                item {
                    var presentation by remember { mutableStateOf(settings.tasbihPresentation) }
                    AutoReminderPresentationSelector(
                        selected = presentation,
                        onSelected = {
                            presentation = it
                            settings.tasbihPresentation = it
                        }
                    )
                }
                item {
                    AutoReminderLockScreenSetting(
                        enabled = lockScreenEnabled,
                        onEnabledChange = {
                            lockScreenEnabled = it
                            settings.tasbihAutoLockScreenEnabled = it
                        },
                        requestLockScreenAccess = requestLockScreenAccess,
                        titleRes = R.string.tasbih_auto_lock_screen,
                        hintRes = R.string.tasbih_auto_lock_screen_hint
                    )
                }

                item {
                    VoiceProfileSettingsSection(
                        settings = settings,
                        reciters = reciters,
                        lang = lang,
                        profile = VoiceSettingsTarget.TASBIH,
                        onPreviewVoice = onPreviewVoice
                    )
                }

                item {
                    PopupAppearanceSettings(
                        settings = settings,
                        target = PopupSettingsTarget.TASBIH,
                        title = stringResource(R.string.tasbih_popup_settings_section),
                        subtitle = stringResource(R.string.tasbih_popup_settings_hint)
                    )
                }
            }
        }
    }
}
