package com.greendome.adhkar.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AdhanAudioEntity
import com.greendome.adhkar.prayer.AdhanAudioResolver
import com.greendome.adhkar.ui.theme.AppCardColors
import com.greendome.adhkar.ui.theme.GoldDome
import com.greendome.adhkar.ui.theme.GreenPrimary
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAdhanAudioScreen(
    items: List<AdhanAudioEntity>,
    lang: String,
    onSave: (AdhanAudioEntity) -> Unit,
    onDelete: (AdhanAudioEntity) -> Unit,
    onBack: () -> Unit,
) {
    var editing by remember { mutableStateOf<AdhanAudioEntity?>(null) }
    var adding by remember { mutableStateOf(false) }

    if (adding || editing != null) {
        BackHandler {
            adding = false
            editing = null
        }
        AdminEditAdhanAudioScreen(
            existing = if (adding) null else editing,
            nextSortOrder = (items.maxOfOrNull { it.sortOrder } ?: 0) + 1,
            onSave = { entity ->
                onSave(entity)
                adding = false
                editing = null
            },
            onDelete = editing?.let { current ->
                {
                    onDelete(current)
                    editing = null
                }
            },
            onCancel = {
                adding = false
                editing = null
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_manage_adhan)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { adding = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.admin_adhan_add))
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
                stringResource(R.string.admin_adhan_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = GreenPrimary
            )
            Button(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(
                    stringResource(R.string.admin_adhan_add),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            if (items.isEmpty()) {
                Text(
                    stringResource(R.string.admin_adhan_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            items.forEach { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editing = item },
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
                            Text(item.catalogLabel(lang), fontWeight = FontWeight.Medium)
                            Text(
                                stringResource(
                                    if (item.suitableForFajr) R.string.adhan_for_fajr
                                    else R.string.adhan_for_other
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                when {
                                    !item.remoteUrl.isNullOrBlank() ->
                                        stringResource(R.string.admin_adhan_on_firebase)
                                    !item.assetPath.isNullOrBlank() ->
                                        stringResource(R.string.admin_adhan_bundled)
                                    !item.localPath.isNullOrBlank() ->
                                        stringResource(R.string.admin_adhan_local_pending)
                                    else -> stringResource(R.string.admin_adhan_no_file)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (item.remoteUrl.isNullOrBlank()) GoldDome else GreenPrimary
                            )
                            if (!item.isActive) {
                                Text(
                                    stringResource(R.string.disabled),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldDome
                                )
                            }
                        }
                        IconButton(onClick = { editing = item }) {
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
private fun AdminEditAdhanAudioScreen(
    existing: AdhanAudioEntity?,
    nextSortOrder: Int,
    onSave: (AdhanAudioEntity) -> Unit,
    onDelete: (() -> Unit)?,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val downloadManager = remember { AudioDownloadManager(context) }
    val preview = remember { DhikrAudioPlayer(context) }
    DisposableEffect(Unit) {
        onDispose { preview.stop() }
    }

    var nameAr by remember { mutableStateOf(existing?.nameAr.orEmpty()) }
    var nameEn by remember { mutableStateOf(existing?.nameEn.orEmpty()) }
    var muezzinAr by remember { mutableStateOf(existing?.muezzinAr.orEmpty()) }
    var countryAr by remember { mutableStateOf(existing?.countryAr.orEmpty()) }
    var cityAr by remember { mutableStateOf(existing?.cityAr.orEmpty()) }
    var maqamAr by remember { mutableStateOf(existing?.maqamAr.orEmpty()) }
    var suitableForFajr by remember { mutableStateOf(existing?.suitableForFajr ?: false) }
    var isActive by remember { mutableStateOf(existing?.isActive ?: true) }
    var localPath by remember { mutableStateOf(existing?.localPath) }
    var remoteUrl by remember { mutableStateOf(existing?.remoteUrl) }
    var assetPath by remember { mutableStateOf(existing?.assetPath) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val errorName = stringResource(R.string.error_adhan_name_required)
    val errorFile = stringResource(R.string.error_adhan_file_required)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) { }
        val fileName = "adhan_${System.currentTimeMillis()}.mp3"
        val path = downloadManager.copyFromUri(uri, fileName)
        if (path != null) {
            existing?.localPath?.let { old ->
                if (old != path) try { File(old).delete() } catch (_: Exception) { }
            }
            localPath = path
            remoteUrl = null
            assetPath = null
            saveError = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (existing == null) R.string.admin_adhan_add else R.string.admin_adhan_edit
                        )
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = nameAr,
                onValueChange = { nameAr = it },
                label = { Text(stringResource(R.string.admin_adhan_name_ar)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = nameEn,
                onValueChange = { nameEn = it },
                label = { Text(stringResource(R.string.admin_adhan_name_en)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = muezzinAr,
                onValueChange = { muezzinAr = it },
                label = { Text(stringResource(R.string.admin_adhan_muezzin)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = countryAr,
                onValueChange = { countryAr = it },
                label = { Text(stringResource(R.string.admin_adhan_country)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = cityAr,
                onValueChange = { cityAr = it },
                label = { Text(stringResource(R.string.admin_adhan_city)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = maqamAr,
                onValueChange = { maqamAr = it },
                label = { Text(stringResource(R.string.admin_adhan_maqam)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.admin_adhan_fajr), fontWeight = FontWeight.Medium)
                    Text(
                        stringResource(R.string.admin_adhan_fajr_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = suitableForFajr, onCheckedChange = { suitableForFajr = it })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.enabled), modifier = Modifier.weight(1f))
                Switch(checked = isActive, onCheckedChange = { isActive = it })
            }
            Text(
                when {
                    !localPath.isNullOrBlank() -> File(localPath!!).name
                    !assetPath.isNullOrBlank() -> stringResource(R.string.admin_adhan_bundled)
                    !remoteUrl.isNullOrBlank() -> stringResource(R.string.admin_adhan_on_firebase)
                    else -> stringResource(R.string.admin_adhan_no_file)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = { picker.launch(arrayOf("audio/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.admin_adhan_pick_file))
            }
            OutlinedButton(
                onClick = {
                    val uri = AdhanAudioResolver.playbackUri(
                        AdhanAudioEntity(
                            nameAr = nameAr,
                            localPath = localPath,
                            remoteUrl = remoteUrl,
                            assetPath = assetPath,
                        ),
                        context,
                    )
                    if (uri != null) preview.playAdhan(uri, SettingsRepository(context), true)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !localPath.isNullOrBlank() || !remoteUrl.isNullOrBlank() || !assetPath.isNullOrBlank()
            ) {
                Text(stringResource(R.string.adhan_preview))
            }
            saveError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = {
                    val name = nameAr.trim()
                    if (name.isBlank()) {
                        saveError = errorName
                        return@Button
                    }
                    if (localPath.isNullOrBlank() && remoteUrl.isNullOrBlank() && assetPath.isNullOrBlank()) {
                        saveError = errorFile
                        return@Button
                    }
                    onSave(
                        AdhanAudioEntity(
                            id = existing?.id ?: 0L,
                            nameAr = name,
                            nameEn = nameEn.trim(),
                            muezzinAr = muezzinAr.trim(),
                            countryAr = countryAr.trim(),
                            cityAr = cityAr.trim(),
                            maqamAr = maqamAr.trim(),
                            maqamEn = existing?.maqamEn.orEmpty(),
                            localPath = localPath,
                            remoteUrl = remoteUrl,
                            assetPath = assetPath,
                            suitableForFajr = suitableForFajr,
                            isActive = isActive,
                            sortOrder = existing?.sortOrder ?: nextSortOrder,
                            isDownloaded = !localPath.isNullOrBlank() || !assetPath.isNullOrBlank(),
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
            if (onDelete != null) {
                OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Text(stringResource(R.string.delete), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
