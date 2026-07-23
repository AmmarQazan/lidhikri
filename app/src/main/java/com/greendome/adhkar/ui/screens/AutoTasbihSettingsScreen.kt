package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.audio.DhikrVolumeResolver
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.ReciterEntity
import com.greendome.adhkar.data.model.PopupSettingsTarget
import com.greendome.adhkar.data.model.VolumeMode
import com.greendome.adhkar.ui.theme.formatLocalizedDigits
import com.greendome.adhkar.ui.theme.stringResourceDigits

@OptIn(ExperimentalMaterial3Api::class)
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
    var volume by remember { mutableStateOf(settings.volume) }
    var volumeMode by remember { mutableStateOf(settings.volumeMode) }
    var randomMode by remember { mutableStateOf(settings.randomMode) }
    var reciterExpanded by remember { mutableStateOf(false) }
    var selectedReciterId by remember { mutableStateOf(settings.selectedReciterId) }
    val selectedReciter = reciters.find { it.id == selectedReciterId } ?: reciters.firstOrNull()

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
                SettingSwitch(
                    label = stringResource(R.string.auto_tasbih_enable),
                    checked = isServiceOn,
                    onChange = { onToggleService() }
                )
            }
            item { Text(stringResource(R.string.interval_minutes)) }
            item {
                Slider(
                    value = interval,
                    onValueChange = { interval = it; settings.intervalMinutes = it.toInt().coerceAtLeast(1) },
                    valueRange = 1f..60f,
                    steps = 58
                )
            }
            item { Text("${interval.toInt().formatLocalizedDigits()} ${stringResource(R.string.minute_label)}") }
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
                    title = stringResource(R.string.voice_section),
                    subtitle = stringResource(R.string.voice_section_hint)
                )
            }
            item {
                ExposedDropdownMenuBox(expanded = reciterExpanded, onExpandedChange = { reciterExpanded = it }) {
                    OutlinedTextField(
                        value = selectedReciter?.localizedName(lang) ?: stringResource(R.string.select_reciter),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.select_reciter)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(reciterExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = reciterExpanded, onDismissRequest = { reciterExpanded = false }) {
                        reciters.forEach { reciter ->
                            DropdownMenuItem(
                                text = { Text(reciter.localizedName(lang)) },
                                onClick = {
                                    selectedReciterId = reciter.id
                                    settings.selectedReciterId = reciter.id
                                    reciterExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            item {
                OutlinedButton(onClick = onPreviewVoice, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.preview_voice))
                }
            }
            item { Text(stringResource(R.string.volume_mode_title)) }
            items(VolumeMode.entries.size) { index ->
                val mode = VolumeMode.entries[index]
                VolumeModeOption(
                    label = volumeModeLabel(mode),
                    selected = volumeMode == mode,
                    onSelect = {
                        volumeMode = mode
                        settings.volumeMode = mode
                    }
                )
            }
            item {
                when (volumeMode) {
                    VolumeMode.MANUAL -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                stringResource(R.string.volume_mode_manual_hint),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Slider(
                                value = volume,
                                onValueChange = { volume = it; settings.volume = it }
                            )
                        }
                    }
                    VolumeMode.MEDIA, VolumeMode.RING -> {
                        val percent = DhikrVolumeResolver.systemStreamPercent(context, volumeMode) ?: 0
                        Text(
                            stringResourceDigits(R.string.volume_mode_system_level, percent),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            stringResource(R.string.volume_mode_system_hint),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                SectionTitle(
                    title = stringResource(R.string.tasbih_popup_settings_section)
                )
            }
            item { PopupAppearanceSettings(settings = settings, target = PopupSettingsTarget.TASBIH) }
        }
    }
}

@Composable
private fun VolumeModeOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun volumeModeLabel(mode: VolumeMode): String = when (mode) {
    VolumeMode.MANUAL -> stringResource(R.string.volume_mode_manual)
    VolumeMode.MEDIA -> stringResource(R.string.volume_mode_media)
    VolumeMode.RING -> stringResource(R.string.volume_mode_ring)
}

