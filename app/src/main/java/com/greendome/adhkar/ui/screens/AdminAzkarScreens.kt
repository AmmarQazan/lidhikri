package com.greendome.adhkar.ui.screens

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
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.AzkarFavorites
import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.ui.theme.GoldDome
import com.greendome.adhkar.ui.theme.GreenPrimary
import com.greendome.adhkar.ui.theme.formatLocalizedDigits
import com.greendome.adhkar.ui.theme.stringResourceDigits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAzkarCollectionsScreen(
    collections: List<AdhkarCollectionEntity>,
    lang: String,
    onBack: () -> Unit,
    onAddCollection: () -> Unit,
    onOpenCollection: (AdhkarCollectionEntity) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_azkar_sections)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onAddCollection) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.admin_add_azkar_section))
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
                stringResource(R.string.admin_azkar_sections_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = GreenPrimary
            )
            Button(onClick = onAddCollection, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(
                    stringResource(R.string.admin_add_azkar_section),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            collections.filter { it.id != AzkarFavorites.COLLECTION_ID }.forEach { collection ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenCollection(collection) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            if (lang == "ar") collection.titleAr else collection.titleEn.ifBlank { collection.titleAr },
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            if (collection.autoPlayAllowed) {
                                stringResource(R.string.admin_auto_play_allowed_on)
                            } else {
                                stringResource(R.string.admin_auto_play_allowed_off)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (collection.autoPlayAllowed) GreenPrimary else GoldDome,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAzkarItemsScreen(
    collection: AdhkarCollectionEntity,
    items: List<AzkarItemEntity>,
    lang: String,
    onBack: () -> Unit,
    onEditCollection: () -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (AzkarItemEntity) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (lang == "ar") collection.titleAr else collection.titleEn.ifBlank { collection.titleAr })
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onEditCollection) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.admin_edit_azkar_section))
                    }
                    IconButton(onClick = onAddItem) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.admin_add_azkar_item))
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
            Text(stringResource(R.string.admin_tap_to_edit), style = MaterialTheme.typography.labelMedium)
            items.forEach { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditItem(item) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(item.textAr, maxLines = 2)
                        if (item.repeatCount > 1) {
                            Text(
                                stringResourceDigits(R.string.azkar_repeat_label, item.repeatCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldDome
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEditAzkarItemScreen(
    collectionId: String,
    existing: AzkarItemEntity?,
    nextSortOrder: Int,
    onSave: (AzkarItemEntity) -> Unit,
    onDelete: ((Long) -> Unit)?,
    onCancel: () -> Unit
) {
    var textAr by remember { mutableStateOf(existing?.textAr ?: "") }
    var virtueAr by remember { mutableStateOf(existing?.virtueAr ?: "") }
    var repeatCount by remember { mutableFloatStateOf((existing?.repeatCount ?: 1).toFloat()) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (existing == null) stringResource(R.string.admin_add_azkar_item)
                        else stringResource(R.string.admin_edit_azkar_item)
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
                value = textAr,
                onValueChange = { textAr = it },
                label = { Text(stringResource(R.string.dhikr_text_section)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )
            OutlinedTextField(
                value = virtueAr,
                onValueChange = { virtueAr = it },
                label = { Text(stringResource(R.string.admin_azkar_virtue)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Text(stringResourceDigits(R.string.azkar_repeat_label, repeatCount.toInt()))
            Slider(
                value = repeatCount,
                onValueChange = { repeatCount = it },
                valueRange = 1f..300f,
                steps = 20
            )
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (textAr.isBlank()) {
                            error = "النص العربي مطلوب"
                            return@Button
                        }
                        onSave(
                            AzkarItemEntity(
                                id = existing?.id ?: 0L,
                                collectionId = collectionId,
                                textAr = textAr.trim(),
                                virtueAr = virtueAr.trim(),
                                repeatCount = repeatCount.toInt().coerceAtLeast(1),
                                sortOrder = existing?.sortOrder ?: nextSortOrder
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.save)) }
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.cancel))
                }
            }
            if (existing != null && onDelete != null) {
                OutlinedButton(
                    onClick = { onDelete(existing.id) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.admin_delete_azkar_item)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEditAzkarCollectionScreen(
    existing: AdhkarCollectionEntity?,
    nextSortOrder: Int,
    onSave: (AdhkarCollectionEntity) -> Unit,
    onCancel: () -> Unit
) {
    var titleAr by remember { mutableStateOf(existing?.titleAr ?: "") }
    var titleEn by remember { mutableStateOf(existing?.titleEn ?: "") }
    var autoPlayAllowed by remember { mutableStateOf(existing?.autoPlayAllowed ?: false) }
    var autoPlay by remember { mutableStateOf(existing?.autoPlayEnabled ?: false) }
    var useTts by remember { mutableStateOf(existing?.useTtsAutoPlay ?: true) }
    var hour by remember { mutableFloatStateOf((existing?.scheduleHour ?: 7).toFloat()) }
    var minute by remember { mutableFloatStateOf((existing?.scheduleMinute ?: 0).toFloat()) }
    var error by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (existing == null) stringResource(R.string.admin_add_azkar_section)
                        else stringResource(R.string.admin_edit_azkar_section)
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
                value = titleAr,
                onValueChange = { titleAr = it; error = false },
                label = { Text(stringResource(R.string.admin_azkar_section_title_ar)) },
                modifier = Modifier.fillMaxWidth(),
                isError = error
            )
            OutlinedTextField(
                value = titleEn,
                onValueChange = { titleEn = it },
                label = { Text(stringResource(R.string.admin_azkar_section_title_en)) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        stringResource(R.string.admin_auto_play_allowed),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        stringResource(R.string.admin_auto_play_allowed_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = GreenPrimary
                    )
                }
                Switch(
                    checked = autoPlayAllowed,
                    onCheckedChange = {
                        autoPlayAllowed = it
                        if (!it) autoPlay = false
                    }
                )
            }
            if (autoPlayAllowed) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.azkar_auto_enable))
                    Switch(checked = autoPlay, onCheckedChange = { autoPlay = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.azkar_tts_auto))
                    Switch(checked = useTts, onCheckedChange = { useTts = it })
                }
                Text(stringResource(R.string.azkar_time_label))
                Text(
                    "${hour.toInt().toString().padStart(2, '0')}:${minute.toInt().toString().padStart(2, '0')}"
                        .formatLocalizedDigits()
                )
                Slider(value = hour, onValueChange = { hour = it }, valueRange = 0f..23f, steps = 22)
                Slider(value = minute, onValueChange = { minute = it }, valueRange = 0f..59f, steps = 58)
            }
            if (error) {
                Text(
                    stringResource(R.string.error_section_title_required),
                    color = MaterialTheme.colorScheme.error
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (titleAr.isBlank()) {
                            error = true
                            return@Button
                        }
                        val id = existing?.id ?: "custom_${System.currentTimeMillis()}"
                        onSave(
                            AdhkarCollectionEntity(
                                id = id,
                                titleAr = titleAr.trim(),
                                titleEn = titleEn.trim(),
                                sortOrder = existing?.sortOrder ?: nextSortOrder,
                                autoPlayAllowed = autoPlayAllowed,
                                autoPlayEnabled = autoPlayAllowed && autoPlay,
                                scheduleHour = hour.toInt(),
                                scheduleMinute = minute.toInt(),
                                weekDaysMask = existing?.weekDaysMask ?: 127,
                                useTtsAutoPlay = useTts
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.save)) }
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}
