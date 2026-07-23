package com.greendome.adhkar.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.greendome.adhkar.audio.AudioDownloadManager
import com.greendome.adhkar.audio.DhikrAudioPlayer
import com.greendome.adhkar.audio.playResolved
import com.greendome.adhkar.audio.resolveReciterAudioEntity
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.DhikrEntity
import com.greendome.adhkar.data.local.ReciterAudioEntity
import com.greendome.adhkar.data.local.ReciterEntity
import com.greendome.adhkar.ui.theme.GoldDome
import com.greendome.adhkar.ui.theme.GreenPrimary
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRecitersScreen(
    reciters: List<ReciterEntity>,
    lang: String,
    onBack: () -> Unit,
    onAddReciter: () -> Unit,
    onEditReciter: (ReciterEntity) -> Unit,
    onOpenReciterAudio: (ReciterEntity) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_manage_reciters)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onAddReciter) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.admin_add_reciter))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.admin_reciters_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = GreenPrimary
            )
            Button(onClick = onAddReciter, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(
                    stringResource(R.string.admin_add_reciter),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            if (reciters.isEmpty()) {
                Text(
                    stringResource(R.string.admin_no_reciters),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            reciters.forEach { reciter ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenReciterAudio(reciter) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(reciter.localizedName(lang), fontWeight = FontWeight.Medium)
                            if (!reciter.isActive) {
                                Text(
                                    stringResource(R.string.disabled),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldDome
                                )
                            } else if (reciter.isBuiltin) {
                                Text(
                                    stringResource(R.string.admin_reciter_builtin),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GreenPrimary
                                )
                            }
                        }
                        IconButton(onClick = { onEditReciter(reciter) }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEditReciterScreen(
    existing: ReciterEntity?,
    onSave: (ReciterEntity) -> Unit,
    onDelete: ((Long) -> Unit)?,
    onCancel: () -> Unit
) {
    var nameAr by remember { mutableStateOf(existing?.nameAr ?: "") }
    var nameEn by remember { mutableStateOf(existing?.nameEn ?: "") }
    var nameFr by remember { mutableStateOf(existing?.nameFr ?: "") }
    var nameEs by remember { mutableStateOf(existing?.nameEs ?: "") }
    var isActive by remember { mutableStateOf(existing?.isActive ?: true) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val errorNameRequired = stringResource(R.string.error_reciter_name_required)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (existing == null) {
                            stringResource(R.string.admin_add_reciter)
                        } else {
                            stringResource(R.string.admin_edit_reciter)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = nameAr,
                onValueChange = { nameAr = it; saveError = null },
                label = { Text(stringResource(R.string.admin_reciter_name_ar)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = nameEn,
                onValueChange = { nameEn = it },
                label = { Text(stringResource(R.string.admin_reciter_name_en)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = nameFr,
                onValueChange = { nameFr = it },
                label = { Text(stringResource(R.string.admin_reciter_name_fr)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = nameEs,
                onValueChange = { nameEs = it },
                label = { Text(stringResource(R.string.admin_reciter_name_es)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (existing != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.enabled))
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }
            if (saveError != null) {
                Text(saveError!!, color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = {
                    if (nameAr.isBlank()) {
                        saveError = errorNameRequired
                        return@Button
                    }
                    onSave(
                        ReciterEntity(
                            id = existing?.id ?: 0,
                            nameAr = nameAr.trim(),
                            nameEn = nameEn.trim(),
                            nameFr = nameFr.trim(),
                            nameEs = nameEs.trim(),
                            isBuiltin = existing?.isBuiltin ?: false,
                            isActive = isActive
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
            if (existing != null && onDelete != null) {
                OutlinedButton(
                    onClick = { onDelete(existing.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Text(
                        stringResource(R.string.admin_delete_reciter),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReciterAudioScreen(
    reciter: ReciterEntity,
    dhikrList: List<DhikrEntity>,
    audioList: List<ReciterAudioEntity>,
    lang: String,
    onSaveAudio: (ReciterAudioEntity) -> Unit,
    onDeleteAudio: (ReciterAudioEntity) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings = remember { SettingsRepository(context) }
    val downloadManager = remember { AudioDownloadManager(context) }
    val previewPlayer = remember { DhikrAudioPlayer(context) }
    var previewingDhikrId by remember { mutableLongStateOf(-1L) }
    var pendingFileDhikrId by remember { mutableLongStateOf(-1L) }

    DisposableEffect(Unit) {
        onDispose { previewPlayer.stop() }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val dhikrId = pendingFileDhikrId
        if (uri == null || dhikrId < 0) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) { }
        val fileName = "r${reciter.id}_d${dhikrId}_${System.currentTimeMillis()}.mp3"
        val path = downloadManager.copyFromUri(uri, fileName)
        if (path != null) {
            val existing = audioList.find { it.dhikrId == dhikrId }
            existing?.localPath?.let { old ->
                if (old != path) try { File(old).delete() } catch (_: Exception) { }
            }
            onSaveAudio(
                ReciterAudioEntity(
                    id = existing?.id ?: 0,
                    reciterId = reciter.id,
                    dhikrId = dhikrId,
                    localPath = path,
                    remoteUrl = null,
                    isDownloaded = true
                )
            )
        }
        pendingFileDhikrId = -1L
    }

    fun saveRemoteUrl(dhikrId: Long, url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return
        val existing = audioList.find { it.dhikrId == dhikrId }
        existing?.localPath?.let { old ->
            try { File(old).delete() } catch (_: Exception) { }
        }
        onSaveAudio(
            ReciterAudioEntity(
                id = existing?.id ?: 0,
                reciterId = reciter.id,
                dhikrId = dhikrId,
                localPath = null,
                remoteUrl = trimmed,
                isDownloaded = false
            )
        )
    }

    fun togglePreview(dhikrId: Long, audio: ReciterAudioEntity?) {
        if (audio == null) return
        val playable = resolveReciterAudioEntity(audio) ?: return
        if (previewingDhikrId == dhikrId) {
            previewPlayer.stop()
            previewingDhikrId = -1L
        } else {
            previewPlayer.stop()
            previewingDhikrId = dhikrId
            previewPlayer.playResolved(playable, settings) { previewingDhikrId = -1L }
        }
    }

    val defaultDhikrs = dhikrList.filter { it.isDefault }.sortedBy { it.sortOrder }
    val audioByDhikr = audioList.associateBy { it.dhikrId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.admin_reciter_audio_title))
                        Text(
                            reciter.localizedName(lang),
                            style = MaterialTheme.typography.labelMedium,
                            color = GreenPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.admin_reciter_audio_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = GreenPrimary
            )
            if (defaultDhikrs.isEmpty()) {
                Text(stringResource(R.string.admin_no_default_dhikr))
            }
            defaultDhikrs.forEach { dhikr ->
                val audio = audioByDhikr[dhikr.id]
                val hasLocal = !audio?.localPath.isNullOrBlank()
                val hasRemote = !audio?.remoteUrl.isNullOrBlank()
                val hasAsset = !audio?.assetPath.isNullOrBlank()
                val hasAudio = hasLocal || hasRemote || hasAsset
                val isThisPreview = previewingDhikrId == dhikr.id
                var urlDraft by remember(dhikr.id, audio?.remoteUrl) {
                    mutableStateOf(audio?.remoteUrl.orEmpty())
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(dhikr.textAr, fontWeight = FontWeight.Medium, maxLines = 2)
                        Text(
                            when {
                                hasLocal -> stringResource(R.string.admin_reciter_audio_attached_file)
                                hasRemote -> stringResource(R.string.admin_reciter_audio_attached_link)
                                hasAsset -> stringResource(R.string.admin_reciter_audio_attached_builtin)
                                else -> stringResource(R.string.admin_reciter_audio_missing)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasAudio) GreenPrimary else GoldDome
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (hasAudio) {
                                IconButton(onClick = { togglePreview(dhikr.id, audio) }) {
                                    Icon(
                                        if (isThisPreview) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = stringResource(R.string.preview),
                                        tint = GreenPrimary
                                    )
                                }
                                IconButton(onClick = { audio?.let { onDeleteAudio(it) } }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.admin_delete_audio),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    pendingFileDhikrId = dhikr.id
                                    filePicker.launch(arrayOf("audio/*"))
                                }
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                                Text(stringResource(R.string.admin_upload_audio))
                            }
                        }
                        OutlinedTextField(
                            value = urlDraft,
                            onValueChange = { urlDraft = it },
                            label = { Text(stringResource(R.string.admin_reciter_audio_url_label)) },
                            placeholder = { Text(stringResource(R.string.admin_reciter_audio_url_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedButton(
                            onClick = { saveRemoteUrl(dhikr.id, urlDraft) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = urlDraft.isNotBlank()
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                            Text(stringResource(R.string.admin_link_audio))
                        }
                    }
                }
            }
        }
    }
}
