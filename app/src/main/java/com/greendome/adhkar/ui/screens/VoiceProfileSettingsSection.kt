package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.audio.DhikrVolumeResolver
import com.greendome.adhkar.data.ReciterLibraryDownloadPolicy
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.ReciterEntity
import com.greendome.adhkar.data.model.ReciterVoiceScope
import com.greendome.adhkar.service.OfflineDownloadHelper
import kotlinx.coroutines.launch
import com.greendome.adhkar.data.model.VoiceSettingsTarget
import com.greendome.adhkar.data.model.VolumeMode
import com.greendome.adhkar.ui.theme.stringResourceDigits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceProfileSettingsSection(
    settings: SettingsRepository,
    reciters: List<ReciterEntity>,
    lang: String,
    profile: VoiceSettingsTarget,
    onPreviewVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var volume by remember(profile) { mutableStateOf(settings.volumeFor(profile)) }
    var volumeMode by remember(profile) { mutableStateOf(settings.volumeModeFor(profile)) }
    var reciterExpanded by remember(profile) { mutableStateOf(false) }
    var selectedReciterId by remember(profile) { mutableStateOf(settings.selectedReciterIdFor(profile)) }
    var confirmReciter by remember(profile) { mutableStateOf<ReciterEntity?>(null) }
    var downloadingReciterId by remember { mutableStateOf<Long?>(null) }
    val eligibleReciters = remember(reciters, profile) {
        reciters.filter { it.voiceScope.allows(profile) }
    }
    val selectedReciter = eligibleReciters.find { it.id == selectedReciterId }
        ?: eligibleReciters.firstOrNull()

    fun applyReciter(reciter: ReciterEntity) {
        selectedReciterId = reciter.id
        settings.setSelectedReciterId(profile, reciter.id)
    }

    fun startLibraryDownload(reciterId: Long) {
        downloadingReciterId = reciterId
        scope.launch {
            OfflineDownloadHelper.downloadReciterLibrary(context, reciterId)
            downloadingReciterId = null
        }
    }

    fun onReciterPicked(reciter: ReciterEntity) {
        reciterExpanded = false
        if (reciter.id == selectedReciterId) return
        if (reciter.isBuiltin) {
            applyReciter(reciter)
            return
        }
        scope.launch {
            val pending = reciterLibraryPendingCount(AdhkarDatabase.get(context), reciter)
            val needsConfirm = ReciterLibraryDownloadPolicy.shouldConfirmLibraryDownload(
                isBuiltin = false,
                alreadyOptedIn = settings.isReciterLibraryOptedIn(reciter.id),
                pendingCount = pending,
            )
            if (needsConfirm) {
                confirmReciter = reciter
            } else {
                applyReciter(reciter)
            }
        }
    }

    LaunchedEffect(eligibleReciters, profile) {
        val ids = eligibleReciters.map { it.id }
        if (ids.isNotEmpty() && selectedReciterId !in ids) {
            val next = ids.first()
            selectedReciterId = next
            settings.setSelectedReciterId(profile, next)
        }
    }

    confirmReciter?.let { reciter ->
        AlertDialog(
            onDismissRequest = { confirmReciter = null },
            title = { Text(stringResource(R.string.reciter_library_confirm_title)) },
            text = { Text(stringResource(reciterLibraryConfirmMessage(reciter.voiceScope))) },
            confirmButton = {
                TextButton(
                    onClick = {
                        applyReciter(reciter)
                        startLibraryDownload(reciter.id)
                        confirmReciter = null
                    }
                ) {
                    Text(stringResource(R.string.reciter_library_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReciter = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    val sectionTitle = when (profile) {
        VoiceSettingsTarget.TASBIH -> stringResource(R.string.voice_section)
        VoiceSettingsTarget.AZKAR -> stringResource(R.string.azkar_voice_section)
    }
    val sectionHint = when (profile) {
        VoiceSettingsTarget.TASBIH -> stringResource(R.string.voice_section_hint)
        VoiceSettingsTarget.AZKAR -> stringResource(R.string.azkar_voice_section_hint)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(title = sectionTitle, subtitle = sectionHint)
        ExposedDropdownMenuBox(expanded = reciterExpanded, onExpandedChange = { reciterExpanded = it }) {
            OutlinedTextField(
                value = selectedReciter?.localizedName(lang) ?: stringResource(R.string.select_reciter),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.select_reciter)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(reciterExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = reciterExpanded, onDismissRequest = { reciterExpanded = false }) {
                eligibleReciters.forEach { reciter ->
                    DropdownMenuItem(
                        text = { ReciterDropdownLabel(name = reciter.localizedName(lang), isBuiltin = reciter.isBuiltin) },
                        onClick = { onReciterPicked(reciter) }
                    )
                }
            }
        }
        ReciterLibraryDownloadRow(
            reciter = selectedReciter,
            downloading = downloadingReciterId == selectedReciter?.id,
            onDownload = {
                val reciterId = selectedReciter?.id ?: return@ReciterLibraryDownloadRow
                startLibraryDownload(reciterId)
            },
        )
        OutlinedButton(onClick = onPreviewVoice, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.preview_voice))
        }
        Text(stringResource(R.string.volume_mode_title))
        VolumeMode.entries.forEach { mode ->
            VoiceVolumeModeOption(
                label = voiceVolumeModeLabel(mode),
                selected = volumeMode == mode,
                onSelect = {
                    volumeMode = mode
                    settings.setVolumeMode(profile, mode)
                }
            )
        }
        when (volumeMode) {
            VolumeMode.MANUAL -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.volume_mode_manual_hint),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = volume,
                        onValueChange = {
                            volume = it
                            settings.setVolume(profile, it)
                        }
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
}

@Composable
private fun VoiceVolumeModeOption(label: String, selected: Boolean, onSelect: () -> Unit) {
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
private fun ReciterDropdownLabel(name: String, isBuiltin: Boolean) {
    Column {
        Text(name)
        if (isBuiltin) {
            Text(
                stringResource(R.string.reciter_builtin_badge),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ReciterLibraryDownloadRow(
    reciter: ReciterEntity?,
    downloading: Boolean,
    onDownload: () -> Unit,
) {
    reciter ?: return
    val context = LocalContext.current
    var remoteCount by remember(reciter.id) { mutableIntStateOf(0) }
    var pendingCount by remember(reciter.id) { mutableIntStateOf(0) }

    suspend fun refreshCounts() {
        val db = AdhkarDatabase.get(context)
        remoteCount = reciterLibraryRemoteCount(db, reciter)
        pendingCount = reciterLibraryPendingCount(db, reciter)
    }

    LaunchedEffect(reciter.id, downloading) {
        refreshCounts()
    }

    if (remoteCount <= 0) return

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.reciter_library_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        when {
            reciter.isBuiltin -> {
                Text(
                    if (pendingCount > 0) {
                        stringResource(R.string.reciter_library_builtin_pending)
                    } else {
                        stringResource(R.string.reciter_library_builtin)
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            pendingCount <= 0 -> {
                Text(
                    stringResource(R.string.reciter_library_downloaded),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            else -> {
                OutlinedButton(
                    onClick = onDownload,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !downloading
                ) {
                    if (downloading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(18.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text(
                        if (downloading) stringResource(R.string.reciter_library_downloading)
                        else stringResource(R.string.reciter_library_download)
                    )
                }
            }
        }
    }
}

private fun reciterLibraryConfirmMessage(scope: ReciterVoiceScope): Int = when (scope) {
    ReciterVoiceScope.TASBIH -> R.string.reciter_library_confirm_tasbih
    ReciterVoiceScope.AZKAR -> R.string.reciter_library_confirm_azkar
    ReciterVoiceScope.BOTH -> R.string.reciter_library_confirm_both
}

private suspend fun reciterLibraryRemoteCount(
    db: AdhkarDatabase,
    reciter: ReciterEntity,
): Int {
    val tasbih = if (reciter.voiceScope.allowsTasbih()) {
        db.reciterAudioDao().remoteUrlCount(reciter.id)
    } else {
        0
    }
    val azkar = if (reciter.voiceScope.allowsAzkar()) {
        db.reciterAzkarAudioDao().remoteUrlCount(reciter.id)
    } else {
        0
    }
    return tasbih + azkar
}

private suspend fun reciterLibraryPendingCount(
    db: AdhkarDatabase,
    reciter: ReciterEntity,
): Int {
    val tasbih = if (reciter.voiceScope.allowsTasbih()) {
        db.reciterAudioDao().getPendingDownloadsForReciter(reciter.id).size
    } else {
        0
    }
    val azkar = if (reciter.voiceScope.allowsAzkar()) {
        db.reciterAzkarAudioDao().getPendingDownloadsForReciter(reciter.id).size
    } else {
        0
    }
    return tasbih + azkar
}

@Composable
private fun voiceVolumeModeLabel(mode: VolumeMode): String = when (mode) {
    VolumeMode.MANUAL -> stringResource(R.string.volume_mode_manual)
    VolumeMode.MEDIA -> stringResource(R.string.volume_mode_media)
    VolumeMode.RING -> stringResource(R.string.volume_mode_ring)
}
