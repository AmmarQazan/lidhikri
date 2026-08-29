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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import com.greendome.adhkar.audio.resolveReciterAzkarAudioEntity
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.data.local.DhikrEntity
import com.greendome.adhkar.data.local.ReciterAudioEntity
import com.greendome.adhkar.data.local.ReciterAzkarAudioEntity
import com.greendome.adhkar.data.local.ReciterEntity
import com.greendome.adhkar.data.model.ReciterVoiceScope
import com.greendome.adhkar.ui.theme.GoldDome
import com.greendome.adhkar.ui.theme.AppCardColors
import com.greendome.adhkar.ui.theme.GreenPrimary
import com.greendome.adhkar.ui.theme.stringResourceDigits
import java.io.File

private sealed interface ReciterAudioSectionNav {
    data class Dhikr(val bucket: AdminDhikrBucket) : ReciterAudioSectionNav
    data class Azkar(val collection: AdhkarCollectionEntity) : ReciterAudioSectionNav
}

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
                    colors = AppCardColors()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(reciter.localizedName(lang), fontWeight = FontWeight.Medium)
                            Text(
                                reciterVoiceScopeLabel(reciter.voiceScope),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
    var nameTr by remember { mutableStateOf(existing?.nameTr ?: "") }
    var nameUr by remember { mutableStateOf(existing?.nameUr ?: "") }
    var nameId by remember { mutableStateOf(existing?.nameId ?: "") }
    var nameHi by remember { mutableStateOf(existing?.nameHi ?: "") }
    var isActive by remember { mutableStateOf(existing?.isActive ?: true) }
    var isBuiltin by remember { mutableStateOf(existing?.isBuiltin ?: false) }
    var voiceScope by remember { mutableStateOf(existing?.voiceScope ?: ReciterVoiceScope.BOTH) }
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
            OutlinedTextField(
                value = nameTr,
                onValueChange = { nameTr = it },
                label = { Text(stringResource(R.string.admin_reciter_name_tr)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = nameUr,
                onValueChange = { nameUr = it },
                label = { Text(stringResource(R.string.admin_reciter_name_ur)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = nameId,
                onValueChange = { nameId = it },
                label = { Text(stringResource(R.string.admin_reciter_name_id)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = nameHi,
                onValueChange = { nameHi = it },
                label = { Text(stringResource(R.string.admin_reciter_name_hi)) },
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(stringResource(R.string.admin_reciter_builtin))
                    Text(
                        stringResource(R.string.admin_reciter_builtin_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = isBuiltin, onCheckedChange = { isBuiltin = it })
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.admin_reciter_scope_title))
                Text(
                    stringResource(R.string.admin_reciter_scope_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ReciterVoiceScope.entries.forEach { scope ->
                    ReciterVoiceScopeOption(
                        scope = scope,
                        selected = voiceScope == scope,
                        onSelect = { voiceScope = scope }
                    )
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
                            nameTr = nameTr.trim(),
                            nameUr = nameUr.trim(),
                            nameId = nameId.trim(),
                            nameHi = nameHi.trim(),
                            isBuiltin = isBuiltin,
                            isActive = isActive,
                            voiceScope = voiceScope,
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

@Composable
private fun reciterVoiceScopeLabel(scope: ReciterVoiceScope): String = when (scope) {
    ReciterVoiceScope.BOTH -> stringResource(R.string.admin_reciter_scope_both)
    ReciterVoiceScope.TASBIH -> stringResource(R.string.admin_reciter_scope_tasbih)
    ReciterVoiceScope.AZKAR -> stringResource(R.string.admin_reciter_scope_azkar)
}

@Composable
private fun reciterVoiceScopeHint(scope: ReciterVoiceScope): String = when (scope) {
    ReciterVoiceScope.BOTH -> stringResource(R.string.admin_reciter_scope_both_hint)
    ReciterVoiceScope.TASBIH -> stringResource(R.string.admin_reciter_scope_tasbih_hint)
    ReciterVoiceScope.AZKAR -> stringResource(R.string.admin_reciter_scope_azkar_hint)
}

@Composable
private fun ReciterVoiceScopeOption(
    scope: ReciterVoiceScope,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(Modifier.padding(start = 4.dp)) {
            Text(reciterVoiceScopeLabel(scope))
            Text(
                reciterVoiceScopeHint(scope),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReciterAudioScreen(
    reciter: ReciterEntity,
    dhikrList: List<DhikrEntity>,
    azkarCollections: List<AdhkarCollectionEntity>,
    azkarItems: List<AzkarItemEntity>,
    dhikrAudioList: List<ReciterAudioEntity>,
    azkarAudioList: List<ReciterAzkarAudioEntity>,
    lang: String,
    onSaveDhikrAudio: (ReciterAudioEntity) -> Unit,
    onDeleteDhikrAudio: (ReciterAudioEntity) -> Unit,
    onSaveAzkarAudio: (ReciterAzkarAudioEntity) -> Unit,
    onDeleteAzkarAudio: (ReciterAzkarAudioEntity) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings = remember { SettingsRepository(context) }
    val downloadManager = remember { AudioDownloadManager(context) }
    val previewPlayer = remember { DhikrAudioPlayer(context) }
    var previewingKey by remember { mutableStateOf<String?>(null) }
    var pendingFileTarget by remember { mutableStateOf<Pair<String, Long>?>(null) }

    DisposableEffect(Unit) {
        onDispose { previewPlayer.stop() }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val target = pendingFileTarget ?: return@rememberLauncherForActivityResult
        if (uri == null) {
            pendingFileTarget = null
            return@rememberLauncherForActivityResult
        }
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) { }
        val (kind, itemId) = target
        val fileName = "r${reciter.id}_${kind}${itemId}_${System.currentTimeMillis()}.mp3"
        val path = downloadManager.copyFromUri(uri, fileName)
        if (path != null) {
            when (kind) {
                "d" -> {
                    val existing = dhikrAudioList.find { it.dhikrId == itemId }
                    existing?.localPath?.let { old ->
                        if (old != path) try { File(old).delete() } catch (_: Exception) { }
                    }
                    onSaveDhikrAudio(
                        ReciterAudioEntity(
                            id = existing?.id ?: 0,
                            reciterId = reciter.id,
                            dhikrId = itemId,
                            localPath = path,
                            remoteUrl = null,
                            isDownloaded = true
                        )
                    )
                }
                "a" -> {
                    val existing = azkarAudioList.find { it.azkarItemId == itemId }
                    existing?.localPath?.let { old ->
                        if (old != path) try { File(old).delete() } catch (_: Exception) { }
                    }
                    onSaveAzkarAudio(
                        ReciterAzkarAudioEntity(
                            id = existing?.id ?: 0,
                            reciterId = reciter.id,
                            azkarItemId = itemId,
                            localPath = path,
                            remoteUrl = null,
                            isDownloaded = true
                        )
                    )
                }
            }
        }
        pendingFileTarget = null
    }

    fun saveDhikrRemoteUrl(dhikrId: Long, url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return
        val existing = dhikrAudioList.find { it.dhikrId == dhikrId }
        existing?.localPath?.let { old ->
            try { File(old).delete() } catch (_: Exception) { }
        }
        onSaveDhikrAudio(
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

    fun saveAzkarRemoteUrl(azkarItemId: Long, url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return
        val existing = azkarAudioList.find { it.azkarItemId == azkarItemId }
        existing?.localPath?.let { old ->
            try { File(old).delete() } catch (_: Exception) { }
        }
        onSaveAzkarAudio(
            ReciterAzkarAudioEntity(
                id = existing?.id ?: 0,
                reciterId = reciter.id,
                azkarItemId = azkarItemId,
                localPath = null,
                remoteUrl = trimmed,
                isDownloaded = false
            )
        )
    }

    fun toggleDhikrPreview(dhikrId: Long, audio: ReciterAudioEntity?) {
        if (audio == null) return
        val playable = resolveReciterAudioEntity(audio) ?: return
        val key = "d$dhikrId"
        if (previewingKey == key) {
            previewPlayer.stop()
            previewingKey = null
        } else {
            previewPlayer.stop()
            previewingKey = key
            previewPlayer.playResolved(playable, settings) { previewingKey = null }
        }
    }

    fun toggleAzkarPreview(azkarItemId: Long, audio: ReciterAzkarAudioEntity?) {
        if (audio == null) return
        val playable = resolveReciterAzkarAudioEntity(audio) ?: return
        val key = "a$azkarItemId"
        if (previewingKey == key) {
            previewPlayer.stop()
            previewingKey = null
        } else {
            previewPlayer.stop()
            previewingKey = key
            previewPlayer.playResolved(playable, settings) { previewingKey = null }
        }
    }

    val audioByDhikr = dhikrAudioList.associateBy { it.dhikrId }
    val audioByAzkar = azkarAudioList.associateBy { it.azkarItemId }
    val azkarByCollection = azkarItems.groupBy { it.collectionId }
    val dhikrBuckets = AdminDhikrBucket.entries.mapNotNull { bucket ->
        val items = adminDhikrItems(dhikrList, bucket)
        if (items.isEmpty()) null else bucket to items
    }
    val azkarSections = azkarCollections
        .sortedBy { it.sortOrder }
        .mapNotNull { collection ->
            val items = azkarByCollection[collection.id].orEmpty().sortedBy { it.sortOrder }
            if (items.isEmpty()) null else collection to items
        }
    var openSection by remember { mutableStateOf<ReciterAudioSectionNav?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            openSection?.let { reciterAudioSectionTitle(it, lang) }
                                ?: stringResource(R.string.admin_reciter_audio_title)
                        )
                        Text(
                            reciter.localizedName(lang),
                            style = MaterialTheme.typography.labelMedium,
                            color = GreenPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (openSection != null) {
                            previewPlayer.stop()
                            previewingKey = null
                            openSection = null
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        when (val section = openSection) {
            null -> AdminReciterAudioHub(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
                dhikrBuckets = if (reciter.voiceScope.allowsTasbih()) dhikrBuckets else emptyList(),
                azkarSections = if (reciter.voiceScope.allowsAzkar()) azkarSections else emptyList(),
                lang = lang,
                onOpenSection = { openSection = it }
            )
            is ReciterAudioSectionNav.Dhikr -> {
                val items = adminDhikrItems(dhikrList, section.bucket)
                AdminReciterAudioItemsList(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.background),
                    hint = stringResource(adminDhikrBucketHint(section.bucket)),
                    items = items,
                    audioByDhikr = audioByDhikr,
                    previewingKey = previewingKey,
                    onToggleDhikrPreview = ::toggleDhikrPreview,
                    onDeleteDhikrAudio = onDeleteDhikrAudio,
                    onUploadDhikr = { dhikrId ->
                        pendingFileTarget = "d" to dhikrId
                        filePicker.launch(arrayOf("audio/*"))
                    },
                    onLinkDhikrUrl = ::saveDhikrRemoteUrl
                )
            }
            is ReciterAudioSectionNav.Azkar -> {
                val items = azkarByCollection[section.collection.id].orEmpty().sortedBy { it.sortOrder }
                AdminReciterAzkarAudioItemsList(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.background),
                    items = items,
                    lang = lang,
                    audioByAzkar = audioByAzkar,
                    previewingKey = previewingKey,
                    onToggleAzkarPreview = ::toggleAzkarPreview,
                    onDeleteAzkarAudio = onDeleteAzkarAudio,
                    onUploadAzkar = { itemId ->
                        pendingFileTarget = "a" to itemId
                        filePicker.launch(arrayOf("audio/*"))
                    },
                    onLinkAzkarUrl = ::saveAzkarRemoteUrl
                )
            }
        }
    }
}

@Composable
private fun reciterAudioSectionTitle(section: ReciterAudioSectionNav, lang: String): String = when (section) {
    is ReciterAudioSectionNav.Dhikr -> stringResource(adminDhikrBucketTitle(section.bucket))
    is ReciterAudioSectionNav.Azkar -> azkarCollectionTitle(section.collection, lang)
}

@Composable
private fun AdminReciterAudioHub(
    modifier: Modifier,
    dhikrBuckets: List<Pair<AdminDhikrBucket, List<DhikrEntity>>>,
    azkarSections: List<Pair<AdhkarCollectionEntity, List<AzkarItemEntity>>>,
    lang: String,
    onOpenSection: (ReciterAudioSectionNav) -> Unit
) {
    val dhikrSectionNavs = dhikrBuckets.map { (bucket, items) ->
        ReciterAudioSectionNav.Dhikr(bucket) to items.size
    }
    val azkarSectionNavs = azkarSections.map { (collection, items) ->
        ReciterAudioSectionNav.Azkar(collection) to items.size
    }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                stringResource(R.string.admin_reciter_audio_pick_section),
                style = MaterialTheme.typography.bodyMedium,
                color = GreenPrimary,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
        }
        if (dhikrSectionNavs.isEmpty() && azkarSectionNavs.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.admin_no_default_dhikr),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
        if (dhikrSectionNavs.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.admin_tasbih_section),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            items(dhikrSectionNavs, key = { (nav, _) -> "dhikr_${(nav as ReciterAudioSectionNav.Dhikr).bucket}" }) { (nav, count) ->
                AdminReciterAudioSectionCard(
                    title = reciterAudioSectionTitle(nav, lang),
                    count = count,
                    onClick = { onOpenSection(nav) }
                )
            }
        }
        if (azkarSectionNavs.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.admin_azkar_sections),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            }
            items(azkarSectionNavs, key = { (nav, _) -> "azkar_${(nav as ReciterAudioSectionNav.Azkar).collection.id}" }) { (nav, count) ->
                AdminReciterAudioSectionCard(
                    title = reciterAudioSectionTitle(nav, lang),
                    count = count,
                    onClick = { onOpenSection(nav) }
                )
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun AdminReciterAudioSectionCard(
    title: String,
    count: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = AppCardColors()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResourceDigits(R.string.admin_section_count, count),
                style = MaterialTheme.typography.bodySmall,
                color = GreenPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun AdminReciterAudioItemsList(
    modifier: Modifier,
    hint: String,
    items: List<DhikrEntity>,
    audioByDhikr: Map<Long, ReciterAudioEntity>,
    previewingKey: String?,
    onToggleDhikrPreview: (Long, ReciterAudioEntity?) -> Unit,
    onDeleteDhikrAudio: (ReciterAudioEntity) -> Unit,
    onUploadDhikr: (Long) -> Unit,
    onLinkDhikrUrl: (Long, String) -> Unit
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                hint,
                style = MaterialTheme.typography.bodyMedium,
                color = GreenPrimary,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
            Text(
                stringResource(R.string.admin_reciter_audio_hint),
                style = MaterialTheme.typography.bodySmall,
                color = GoldDome,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(items, key = { it.id }) { dhikr ->
            val audio = audioByDhikr[dhikr.id]
            AdminReciterAudioItemCard(
                text = dhikr.textAr,
                hasLocal = !audio?.localPath.isNullOrBlank(),
                hasRemote = !audio?.remoteUrl.isNullOrBlank(),
                hasAsset = !audio?.assetPath.isNullOrBlank(),
                remoteUrl = audio?.remoteUrl.orEmpty(),
                isPreviewing = previewingKey == "d${dhikr.id}",
                onTogglePreview = { onToggleDhikrPreview(dhikr.id, audio) },
                onDelete = { audio?.let(onDeleteDhikrAudio) },
                onUpload = { onUploadDhikr(dhikr.id) },
                onLinkUrl = { onLinkDhikrUrl(dhikr.id, it) }
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun AdminReciterAzkarAudioItemsList(
    modifier: Modifier,
    items: List<AzkarItemEntity>,
    lang: String,
    audioByAzkar: Map<Long, ReciterAzkarAudioEntity>,
    previewingKey: String?,
    onToggleAzkarPreview: (Long, ReciterAzkarAudioEntity?) -> Unit,
    onDeleteAzkarAudio: (ReciterAzkarAudioEntity) -> Unit,
    onUploadAzkar: (Long) -> Unit,
    onLinkAzkarUrl: (Long, String) -> Unit
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                stringResource(R.string.admin_reciter_audio_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = GreenPrimary,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
        }
        items(items, key = { it.id }) { item ->
            val audio = audioByAzkar[item.id]
            AdminReciterAudioItemCard(
                text = item.localizedText(lang),
                hasLocal = !audio?.localPath.isNullOrBlank(),
                hasRemote = !audio?.remoteUrl.isNullOrBlank(),
                hasAsset = !audio?.assetPath.isNullOrBlank(),
                remoteUrl = audio?.remoteUrl.orEmpty(),
                isPreviewing = previewingKey == "a${item.id}",
                onTogglePreview = { onToggleAzkarPreview(item.id, audio) },
                onDelete = { audio?.let(onDeleteAzkarAudio) },
                onUpload = { onUploadAzkar(item.id) },
                onLinkUrl = { onLinkAzkarUrl(item.id, it) }
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun AdminReciterAudioItemCard(
    text: String,
    hasLocal: Boolean,
    hasRemote: Boolean,
    hasAsset: Boolean,
    remoteUrl: String,
    isPreviewing: Boolean,
    onTogglePreview: () -> Unit,
    onDelete: () -> Unit,
    onUpload: () -> Unit,
    onLinkUrl: (String) -> Unit
) {
    val hasAudio = hasLocal || hasRemote || hasAsset
    var urlDraft by remember(text, remoteUrl) { mutableStateOf(remoteUrl) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = AppCardColors()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text, fontWeight = FontWeight.Medium, maxLines = 3)
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
                    IconButton(onClick = onTogglePreview) {
                        Icon(
                            if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.preview),
                            tint = GreenPrimary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.admin_delete_audio),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                OutlinedButton(onClick = onUpload) {
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
                onClick = { onLinkUrl(urlDraft) },
                modifier = Modifier.fillMaxWidth(),
                enabled = urlDraft.isNotBlank()
            ) {
                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text(stringResource(R.string.admin_link_audio))
            }
        }
    }
}
