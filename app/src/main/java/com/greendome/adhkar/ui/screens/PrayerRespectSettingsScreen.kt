package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import com.greendome.adhkar.prayer.PrayerConfig
import com.greendome.adhkar.prayer.PrayerName
import com.greendome.adhkar.service.PrayerAlarms
import com.greendome.adhkar.ui.theme.formatLocalizedDigits

@Composable
fun PrayerRespectSettingsScreen(
    settings: SettingsRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(settings.respectPrayerTime) }
    var afterPrayer by remember { mutableStateOf(settings.afterPrayerFromSalahEnabled) }
    var jumuahQuiet by remember { mutableIntStateOf(settings.prayerJumuahQuietMinutes) }
    var jumuahAfterDelay by remember { mutableIntStateOf(settings.prayerJumuahAfterDelayMinutes) }
    var timesTick by remember { mutableIntStateOf(0) }

    fun persistAndReschedule() {
        PrayerAlarms.rescheduleAll(context)
        timesTick++
    }

    val config = remember(enabled, jumuahQuiet, timesTick) {
        settings.prayerConfig()
    }
    val hasCity = config.location != null

    SettingsSubScreenScaffold(
        title = stringResource(R.string.prayer_respect_title),
        onBack = onBack,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SectionTitle(
                    title = stringResource(R.string.prayer_respect_section),
                    subtitle = stringResource(R.string.prayer_respect_hint)
                )
            }
            item {
                SettingSwitch(stringResource(R.string.prayer_respect_enable), enabled) {
                    enabled = it
                    settings.respectPrayerTime = it
                    persistAndReschedule()
                }
            }
            if (enabled || afterPrayer) {
                item {
                    Text(
                        stringResource(R.string.prayer_respect_uses_times_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if ((enabled || afterPrayer) && !hasCity) {
                item {
                    Text(
                        stringResource(R.string.prayer_times_need_city),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (enabled) {
                item {
                    val quietSummary = stringResource(
                        R.string.prayer_quiet_current,
                        config.quietDuration(PrayerName.FAJR, false).formatLocalizedDigits(),
                        config.quietDuration(PrayerName.DHUHR, false).formatLocalizedDigits(),
                        config.quietDuration(PrayerName.ASR, false).formatLocalizedDigits(),
                        config.quietDuration(PrayerName.MAGHRIB, false).formatLocalizedDigits(),
                        config.quietDuration(PrayerName.ISHA, false).formatLocalizedDigits(),
                        config.jumuahQuietMinutes.formatLocalizedDigits()
                    )
                    ExpandableSettingsCard(
                        title = stringResource(R.string.prayer_quiet_section),
                        subtitle = "${stringResource(R.string.prayer_quiet_hint)}\n$quietSummary"
                    ) {
                        PrayerName.entries.forEach { prayer ->
                            QuietDurationSlider(
                                label = prayerLabel(prayer),
                                minutes = settings.prayerQuietMinutes(prayer),
                                onChange = {
                                    settings.setPrayerQuietMinutes(prayer, it)
                                    persistAndReschedule()
                                }
                            )
                        }
                        QuietDurationSlider(
                            label = stringResource(R.string.prayer_jumuah_quiet),
                            minutes = jumuahQuiet,
                            onChange = {
                                jumuahQuiet = it
                                settings.prayerJumuahQuietMinutes = it
                                persistAndReschedule()
                            }
                        )
                    }
                }
            }
            if (afterPrayer) {
                item {
                    Text(
                        stringResource(
                            if (enabled) R.string.prayer_after_hint
                            else R.string.prayer_after_needs_pause
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    var presentation by remember { mutableStateOf(settings.afterPrayerPresentation) }
                    ExpandableSettingsCard(
                        title = stringResource(R.string.after_prayer_presentation_section),
                        subtitle = stringResource(R.string.presentation_hint)
                    ) {
                        AutoReminderPresentationSelector(
                            selected = presentation,
                            onSelected = {
                                presentation = it
                                settings.afterPrayerPresentation = it
                            }
                        )
                    }
                }
                item {
                    ExpandableSettingsCard(
                        title = stringResource(R.string.prayer_after_delay_section),
                        subtitle = stringResource(R.string.prayer_after_delay_hint)
                    ) {
                        PrayerName.entries.forEach { prayer ->
                            QuietDurationSlider(
                                label = prayerLabel(prayer),
                                minutes = settings.prayerAfterDelayMinutes(prayer),
                                onChange = {
                                    settings.setPrayerAfterDelayMinutes(prayer, it)
                                    persistAndReschedule()
                                }
                            )
                        }
                        QuietDurationSlider(
                            label = stringResource(R.string.prayer_jumuah_after),
                            minutes = jumuahAfterDelay,
                            onChange = {
                                jumuahAfterDelay = it
                                settings.prayerJumuahAfterDelayMinutes = it
                                persistAndReschedule()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuietDurationSlider(
    label: String,
    minutes: Int,
    onChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            stringResource(
                R.string.prayer_minutes_after,
                label,
                minutes.formatLocalizedDigits(),
                stringResource(R.string.minute_label)
            ),
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = minutes.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = PrayerConfig.QUIET_MIN.toFloat()..PrayerConfig.QUIET_MAX.toFloat(),
            steps = PrayerConfig.QUIET_MAX - PrayerConfig.QUIET_MIN - 1
        )
    }
}
