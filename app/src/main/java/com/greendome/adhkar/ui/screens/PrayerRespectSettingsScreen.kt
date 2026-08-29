package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.prayer.AsrMadhabPref
import com.greendome.adhkar.prayer.CalculationMethodPref
import com.greendome.adhkar.prayer.DstMode
import com.greendome.adhkar.prayer.PrayerConfig
import com.greendome.adhkar.prayer.PrayerCountryDefaults
import com.greendome.adhkar.prayer.PrayerName
import com.greendome.adhkar.prayer.PrayerTimesCalculator
import com.greendome.adhkar.prayer.TimezoneMode
import com.greendome.adhkar.prayer.formatPrayerClock
import com.greendome.adhkar.service.AdhkarReminderService
import com.greendome.adhkar.service.AfterPrayerAlarmScheduler
import com.greendome.adhkar.service.ReminderScheduler
import com.greendome.adhkar.ui.theme.GreenPrimaryDark
import com.greendome.adhkar.ui.theme.formatLocalizedDigits
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerRespectSettingsScreen(
    settings: SettingsRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(settings.respectPrayerTime) }
    var afterPrayer by remember { mutableStateOf(settings.afterPrayerFromSalahEnabled) }
    var travelAuto by remember { mutableStateOf(settings.prayerTravelAutoUpdate) }
    var location by remember { mutableStateOf(settings.prayerConfig().location) }
    var tzMode by remember { mutableStateOf(settings.prayerTimezoneMode) }
    var tzId by remember { mutableStateOf(settings.prayerTimezoneId.ifBlank { TimeZone.getDefault().id }) }
    var dstMode by remember { mutableStateOf(settings.prayerDstMode) }
    var method by remember { mutableStateOf(settings.prayerCalculationMethod) }
    var madhab by remember { mutableStateOf(settings.prayerAsrMadhab) }
    var jumuahQuiet by remember { mutableIntStateOf(settings.prayerJumuahQuietMinutes) }
    var jumuahAfterDelay by remember { mutableIntStateOf(settings.prayerJumuahAfterDelayMinutes) }
    var timesTick by remember { mutableIntStateOf(0) }

    fun persistAndReschedule() {
        AfterPrayerAlarmScheduler.reschedule(context)
        if (settings.isServiceEnabled) {
            ReminderScheduler.scheduleNext(context)
            AdhkarReminderService.refreshNotification(context)
        }
        timesTick++
    }

    val config = remember(enabled, location, tzMode, tzId, dstMode, method, madhab, timesTick, jumuahQuiet) {
        settings.prayerConfig()
    }
    val today = remember(config, timesTick) { PrayerTimesCalculator.timesFor(config) }
    val zone = remember(config, timesTick) { PrayerTimesCalculator.zoneId(config) }

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
            item {
                SettingSwitch(stringResource(R.string.prayer_after_enable), afterPrayer) {
                    afterPrayer = it
                    settings.afterPrayerFromSalahEnabled = it
                    persistAndReschedule()
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
                item {
                    SectionTitle(
                        title = stringResource(R.string.prayer_location_section),
                        subtitle = stringResource(R.string.prayer_location_hint)
                    )
                }
                item {
                    PrayerCityPicker(
                        current = location,
                        onPicked = { picked, mode ->
                            location = picked
                            settings.setPrayerLocation(picked, mode)
                            persistAndReschedule()
                        }
                    )
                }
                item {
                    SettingSwitch(stringResource(R.string.prayer_travel_auto), travelAuto) {
                        travelAuto = it
                        settings.prayerTravelAutoUpdate = it
                        if (it && location != null) {
                            settings.prayerLocationMode = com.greendome.adhkar.prayer.LocationMode.GPS
                        }
                    }
                    Text(
                        stringResource(R.string.prayer_travel_auto_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                item {
                    SectionTitle(
                        title = stringResource(R.string.prayer_time_section),
                        subtitle = stringResource(R.string.prayer_time_hint)
                    )
                }
                item {
                    EnumDropdown(
                        label = stringResource(R.string.prayer_timezone_mode),
                        value = timezoneModeLabel(tzMode),
                        options = TimezoneMode.entries.map { it to timezoneModeLabel(it) }
                    ) {
                        tzMode = it
                        settings.prayerTimezoneMode = it
                        persistAndReschedule()
                    }
                }
                if (tzMode == TimezoneMode.MANUAL) {
                    item {
                        EnumDropdown(
                            label = stringResource(R.string.prayer_timezone),
                            value = tzId,
                            options = PrayerCountryDefaults.commonTimezones.map { it to it }
                        ) {
                            tzId = it
                            settings.prayerTimezoneId = it
                            persistAndReschedule()
                        }
                    }
                }
                item {
                    EnumDropdown(
                        label = stringResource(R.string.prayer_dst_mode),
                        value = dstModeLabel(dstMode),
                        options = DstMode.entries.map { it to dstModeLabel(it) }
                    ) {
                        dstMode = it
                        settings.prayerDstMode = it
                        persistAndReschedule()
                    }
                    Text(
                        stringResource(R.string.prayer_dst_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                item {
                    EnumDropdown(
                        label = stringResource(R.string.prayer_method),
                        value = methodLabel(method),
                        options = CalculationMethodPref.entries.map { it to methodLabel(it) }
                    ) {
                        method = it
                        settings.prayerCalculationMethod = it
                        persistAndReschedule()
                    }
                }
                item {
                    EnumDropdown(
                        label = stringResource(R.string.prayer_madhab),
                        value = madhabLabel(madhab),
                        options = AsrMadhabPref.entries.map { it to madhabLabel(it) }
                    ) {
                        madhab = it
                        settings.prayerAsrMadhab = it
                        persistAndReschedule()
                    }
                }
                item {
                    val timesSubtitle = if (today == null) {
                        stringResource(R.string.prayer_times_need_city)
                    } else {
                        PrayerName.entries.mapNotNull { prayer ->
                            val millis = today.timeOf(prayer) ?: return@mapNotNull null
                            "${prayerLabel(prayer)} ${formatPrayerClock(millis, zone).formatLocalizedDigits()}"
                        }.joinToString(" · ")
                    }
                    ExpandableSettingsCard(
                        title = stringResource(R.string.prayer_times_today),
                        subtitle = "${stringResource(R.string.prayer_offset_hint)}\n$timesSubtitle"
                    ) {
                        if (today == null) {
                            Text(
                                stringResource(R.string.prayer_times_need_city),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            PrayerName.entries.forEach { prayer ->
                                val millis = today.timeOf(prayer) ?: return@forEach
                                PrayerOffsetRow(
                                    title = prayerLabel(prayer),
                                    clock = formatPrayerClock(millis, zone).formatLocalizedDigits(),
                                    offset = settings.prayerMinuteOffset(prayer),
                                    onChange = { next ->
                                        settings.setPrayerMinuteOffset(prayer, next)
                                        persistAndReschedule()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerOffsetRow(
    title: String,
    clock: String,
    offset: Int,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                clock,
                style = MaterialTheme.typography.titleMedium,
                color = GreenPrimaryDark
            )
        }
        IconButton(
            onClick = { onChange(offset - 1) },
            enabled = offset > PrayerConfig.OFFSET_MIN
        ) {
            Icon(Icons.Filled.Remove, contentDescription = null)
        }
        Text(
            (if (offset > 0) "+$offset" else offset.toString()).formatLocalizedDigits(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        IconButton(
            onClick = { onChange(offset + 1) },
            enabled = offset < PrayerConfig.OFFSET_MAX
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdown(
    label: String,
    value: String,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (item, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun prayerLabel(prayer: PrayerName): String = stringResource(
    when (prayer) {
        PrayerName.FAJR -> R.string.prayer_name_fajr
        PrayerName.DHUHR -> R.string.prayer_name_dhuhr
        PrayerName.ASR -> R.string.prayer_name_asr
        PrayerName.MAGHRIB -> R.string.prayer_name_maghrib
        PrayerName.ISHA -> R.string.prayer_name_isha
    }
)

@Composable
private fun timezoneModeLabel(mode: TimezoneMode): String = stringResource(
    when (mode) {
        TimezoneMode.AUTO -> R.string.prayer_mode_auto
        TimezoneMode.MANUAL -> R.string.prayer_mode_manual
    }
)

@Composable
private fun dstModeLabel(mode: DstMode): String = stringResource(
    when (mode) {
        DstMode.AUTO -> R.string.prayer_dst_auto
        DstMode.ON -> R.string.prayer_dst_on
        DstMode.OFF -> R.string.prayer_dst_off
    }
)

@Composable
private fun methodLabel(method: CalculationMethodPref): String = stringResource(
    when (method) {
        CalculationMethodPref.AUTO -> R.string.prayer_method_auto
        CalculationMethodPref.MUSLIM_WORLD_LEAGUE -> R.string.prayer_method_mwl
        CalculationMethodPref.EGYPTIAN -> R.string.prayer_method_egyptian
        CalculationMethodPref.KARACHI -> R.string.prayer_method_karachi
        CalculationMethodPref.UMM_AL_QURA -> R.string.prayer_method_umm_al_qura
        CalculationMethodPref.DUBAI -> R.string.prayer_method_dubai
        CalculationMethodPref.MOON_SIGHTING_COMMITTEE -> R.string.prayer_method_moonsighting
        CalculationMethodPref.NORTH_AMERICA -> R.string.prayer_method_isna
        CalculationMethodPref.KUWAIT -> R.string.prayer_method_kuwait
        CalculationMethodPref.QATAR -> R.string.prayer_method_qatar
        CalculationMethodPref.SINGAPORE -> R.string.prayer_method_singapore
    }
)

@Composable
private fun madhabLabel(madhab: AsrMadhabPref): String = stringResource(
    when (madhab) {
        AsrMadhabPref.AUTO -> R.string.prayer_madhab_auto
        AsrMadhabPref.SHAFI -> R.string.prayer_madhab_shafi
        AsrMadhabPref.HANAFI -> R.string.prayer_madhab_hanafi
    }
)
