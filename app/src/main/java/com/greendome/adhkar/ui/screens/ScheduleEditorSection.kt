package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.model.SchedulePreset
import com.greendome.adhkar.data.model.ScheduleType
import com.greendome.adhkar.data.model.toValues
import com.greendome.adhkar.util.DhikrScheduleMatcher
import com.greendome.adhkar.ui.theme.stringResourceDigits

data class ScheduleFormState(
    val scheduleType: ScheduleType = ScheduleType.ALWAYS,
    val timeStartHour: Int = -1,
    val timeStartMinute: Int = 0,
    val timeEndHour: Int = -1,
    val timeEndMinute: Int = 0,
    val hijriMonth: Int = -1,
    val hijriDayStart: Int = -1,
    val hijriDayEnd: Int = -1,
    val scheduleLabelAr: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditorSection(
    state: ScheduleFormState,
    onChange: (ScheduleFormState) -> Unit
) {
    var presetExpanded by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf(SchedulePreset.CUSTOM) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.schedule_section))
        Text(
            stringResourceDigits(R.string.today_hijri, DhikrScheduleMatcher.todayHijriFormatted()),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        ExposedDropdownMenuBox(expanded = presetExpanded, onExpandedChange = { presetExpanded = it }) {
            OutlinedTextField(
                value = selectedPreset.labelAr,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.schedule_preset)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(presetExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = presetExpanded, onDismissRequest = { presetExpanded = false }) {
                SchedulePreset.entries.forEach { preset ->
                    DropdownMenuItem(
                        text = { Text(preset.labelAr) },
                        onClick = {
                            selectedPreset = preset
                            presetExpanded = false
                            if (preset != SchedulePreset.CUSTOM) {
                                val v = preset.toValues()
                                onChange(
                                    state.copy(
                                        scheduleType = v.scheduleType,
                                        timeStartHour = v.timeStartHour,
                                        timeStartMinute = v.timeStartMinute,
                                        timeEndHour = v.timeEndHour,
                                        timeEndMinute = v.timeEndMinute,
                                        hijriMonth = v.hijriMonth,
                                        hijriDayStart = v.hijriDayStart,
                                        hijriDayEnd = v.hijriDayEnd,
                                        scheduleLabelAr = v.labelAr
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }

        Text(stringResource(R.string.schedule_type))
        ScheduleType.entries.forEach { type ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = state.scheduleType == type,
                    onClick = { onChange(state.copy(scheduleType = type)) }
                )
                Text(
                    when (type) {
                        ScheduleType.ALWAYS -> stringResource(R.string.schedule_always)
                        ScheduleType.TIME_RANGE -> stringResource(R.string.schedule_time)
                        ScheduleType.HIJRI_RANGE -> stringResource(R.string.schedule_hijri)
                        ScheduleType.TIME_AND_HIJRI -> stringResource(R.string.schedule_both)
                    }
                )
            }
        }

        if (state.scheduleType == ScheduleType.TIME_RANGE || state.scheduleType == ScheduleType.TIME_AND_HIJRI) {
            Text(stringResource(R.string.schedule_time_range))
            TimeRow(stringResource(R.string.time_from), state.timeStartHour, state.timeStartMinute) { h, m ->
                onChange(state.copy(timeStartHour = h, timeStartMinute = m))
            }
            TimeRow(stringResource(R.string.time_to), state.timeEndHour, state.timeEndMinute) { h, m ->
                onChange(state.copy(timeEndHour = h, timeEndMinute = m))
            }
        }

        if (state.scheduleType == ScheduleType.HIJRI_RANGE || state.scheduleType == ScheduleType.TIME_AND_HIJRI) {
            Text(stringResource(R.string.schedule_hijri_range))
            OutlinedTextField(
                value = if (state.hijriMonth > 0) state.hijriMonth.toString() else "",
                onValueChange = { onChange(state.copy(hijriMonth = it.toIntOrNull() ?: -1)) },
                label = { Text(stringResource(R.string.hijri_month)) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = if (state.hijriDayStart > 0) state.hijriDayStart.toString() else "",
                    onValueChange = { onChange(state.copy(hijriDayStart = it.toIntOrNull() ?: -1)) },
                    label = { Text(stringResource(R.string.hijri_day_from)) },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = if (state.hijriDayEnd > 0) state.hijriDayEnd.toString() else "",
                    onValueChange = { onChange(state.copy(hijriDayEnd = it.toIntOrNull() ?: -1)) },
                    label = { Text(stringResource(R.string.hijri_day_to)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        OutlinedTextField(
            value = state.scheduleLabelAr,
            onValueChange = { onChange(state.copy(scheduleLabelAr = it)) },
            label = { Text(stringResource(R.string.schedule_label)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TimeRow(
    label: String,
    hour: Int,
    minute: Int,
    onChange: (Int, Int) -> Unit
) {
    var h by remember(hour) { mutableIntStateOf(if (hour >= 0) hour else 0) }
    var m by remember(minute) { mutableIntStateOf(minute) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(0.4f))
        OutlinedTextField(
            value = h.toString(),
            onValueChange = { it.toIntOrNull()?.coerceIn(0, 23)?.let { v -> h = v; onChange(v, m) } },
            label = { Text(stringResource(R.string.hour)) },
            modifier = Modifier.weight(0.3f)
        )
        OutlinedTextField(
            value = m.toString(),
            onValueChange = { it.toIntOrNull()?.coerceIn(0, 59)?.let { v -> m = v; onChange(h, v) } },
            label = { Text(stringResource(R.string.minute)) },
            modifier = Modifier.weight(0.3f)
        )
    }
}

fun scheduleFormFromEntity(entity: com.greendome.adhkar.data.local.DhikrEntity) = ScheduleFormState(
    scheduleType = entity.scheduleType,
    timeStartHour = entity.timeStartHour,
    timeStartMinute = entity.timeStartMinute,
    timeEndHour = entity.timeEndHour,
    timeEndMinute = entity.timeEndMinute,
    hijriMonth = entity.hijriMonth,
    hijriDayStart = entity.hijriDayStart,
    hijriDayEnd = entity.hijriDayEnd,
    scheduleLabelAr = entity.scheduleLabelAr
)

fun com.greendome.adhkar.data.local.DhikrEntity.applySchedule(form: ScheduleFormState) = copy(
    scheduleType = form.scheduleType,
    timeStartHour = form.timeStartHour,
    timeStartMinute = form.timeStartMinute,
    timeEndHour = form.timeEndHour,
    timeEndMinute = form.timeEndMinute,
    hijriMonth = form.hijriMonth,
    hijriDayStart = form.hijriDayStart,
    hijriDayEnd = form.hijriDayEnd,
    scheduleLabelAr = form.scheduleLabelAr
)
