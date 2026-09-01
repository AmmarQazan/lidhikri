package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.model.ClockHourFormat
import com.greendome.adhkar.prayer.AsrMadhabPref
import com.greendome.adhkar.prayer.CalculationMethodPref
import com.greendome.adhkar.prayer.DstMode
import com.greendome.adhkar.prayer.PrayerConfig
import com.greendome.adhkar.prayer.PrayerName
import com.greendome.adhkar.prayer.TimezoneMode
import com.greendome.adhkar.ui.theme.formatLocalizedDigits
import com.greendome.adhkar.util.formatClockTime
import java.time.Instant
import java.time.ZoneId

fun prayerNameRes(prayer: PrayerName): Int = when (prayer) {
    PrayerName.FAJR -> R.string.prayer_name_fajr
    PrayerName.DHUHR -> R.string.prayer_name_dhuhr
    PrayerName.ASR -> R.string.prayer_name_asr
    PrayerName.MAGHRIB -> R.string.prayer_name_maghrib
    PrayerName.ISHA -> R.string.prayer_name_isha
}

@Composable
fun prayerLabel(prayer: PrayerName): String = stringResource(prayerNameRes(prayer))

fun formatPrayerInstant(
    millis: Long,
    zone: ZoneId,
    format: ClockHourFormat,
    am: String,
    pm: String,
): String {
    val local = Instant.ofEpochMilli(millis).atZone(zone)
    return formatClockTime(local.hour, local.minute, format, am, pm)
}

@Composable
fun timezoneModeLabel(mode: TimezoneMode): String = stringResource(
    when (mode) {
        TimezoneMode.AUTO -> R.string.prayer_mode_auto
        TimezoneMode.MANUAL -> R.string.prayer_mode_manual
    }
)

@Composable
fun dstModeLabel(mode: DstMode): String = stringResource(
    when (mode) {
        DstMode.AUTO -> R.string.prayer_dst_auto
        DstMode.ON -> R.string.prayer_dst_on
        DstMode.OFF -> R.string.prayer_dst_off
    }
)

@Composable
fun methodLabel(method: CalculationMethodPref): String = stringResource(
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
fun madhabLabel(madhab: AsrMadhabPref): String = stringResource(
    when (madhab) {
        AsrMadhabPref.AUTO -> R.string.prayer_madhab_auto
        AsrMadhabPref.SHAFI -> R.string.prayer_madhab_shafi
        AsrMadhabPref.HANAFI -> R.string.prayer_madhab_hanafi
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> PrayerEnumDropdown(
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
fun PrayerOffsetRow(
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
                color = MaterialTheme.colorScheme.onSurface
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
fun PrayerMinutesSlider(
    label: String,
    minutes: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit,
    zeroMeansOff: Boolean = false,
) {
    val off = zeroMeansOff && minutes == 0
    val valueText = if (off) {
        stringResource(R.string.adhan_off)
    } else {
        stringResource(
            R.string.minutes_value,
            minutes.formatLocalizedDigits(),
            stringResource(R.string.minute_label)
        )
    }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            Text(
                valueText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (off) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            Slider(
                value = minutes.toFloat(),
                onValueChange = { onChange(it.toInt()) },
                valueRange = min.toFloat()..max.toFloat(),
                modifier = Modifier.fillMaxWidth().height(28.dp)
            )
        }
    }
}
