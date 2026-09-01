package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.data.model.CollectionDayMode
import com.greendome.adhkar.prayer.PrayerName
import com.greendome.adhkar.ui.theme.GoldDome
import com.greendome.adhkar.ui.theme.GreenPrimary
import com.greendome.adhkar.ui.theme.formatLocalizedDigits
import com.greendome.adhkar.ui.theme.stringResourceDigits
import com.greendome.adhkar.util.CollectionScheduleHelper
import java.util.Calendar

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
            Text(
                stringResource(R.string.admin_azkar_publish_hint),
                style = MaterialTheme.typography.bodySmall,
                color = GoldDome,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Button(onClick = onAddCollection, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(
                    stringResource(R.string.admin_add_azkar_section),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            collections.forEach { collection ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenCollection(collection) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            azkarCollectionTitle(collection, lang),
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
                    Text(azkarCollectionTitle(collection, lang))
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
                        Text(item.localizedText(lang), maxLines = 3)
                        if (item.localizedVirtue(lang).isNotBlank()) {
                            Text(
                                item.localizedVirtue(lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldDome,
                                modifier = Modifier.padding(top = 4.dp),
                                maxLines = 2
                            )
                        }
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var customTime by remember { mutableStateOf((existing?.scheduleHour ?: -1) >= 0) }
    var hour by remember { mutableFloatStateOf((existing?.scheduleHour?.takeIf { it >= 0 } ?: 8).toFloat()) }
    var minute by remember { mutableFloatStateOf((existing?.scheduleMinute ?: 0).toFloat()) }
    var prayerAnchor by remember { mutableStateOf(existing?.prayerAnchor.orEmpty()) }
    var prayerOffset by remember { mutableFloatStateOf((existing?.prayerOffsetMinutes ?: 0).toFloat()) }
    var skipQuiet by remember { mutableStateOf(existing?.skipQuietWindow ?: false) }
    var hijriMonth by remember {
        mutableStateOf(if ((existing?.hijriMonth ?: -1) > 0) existing!!.hijriMonth.toString() else "")
    }
    var hijriDayStart by remember {
        mutableStateOf(if ((existing?.hijriDayStart ?: -1) > 0) existing!!.hijriDayStart.toString() else "")
    }
    var hijriDayEnd by remember {
        mutableStateOf(if ((existing?.hijriDayEnd ?: -1) > 0) existing!!.hijriDayEnd.toString() else "")
    }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.azkar_item_custom_time))
                Switch(checked = customTime, onCheckedChange = { customTime = it })
            }
            if (customTime) {
                Text(stringResource(R.string.azkar_time_label))
                Text(
                    "${hour.toInt().toString().padStart(2, '0')}:${minute.toInt().toString().padStart(2, '0')}"
                        .formatLocalizedDigits()
                )
                Slider(value = hour, onValueChange = { hour = it }, valueRange = 0f..23f, steps = 22)
                Slider(value = minute, onValueChange = { minute = it }, valueRange = 0f..59f, steps = 58)
            } else {
                Text(
                    stringResource(R.string.azkar_item_inherit_time),
                    style = MaterialTheme.typography.bodySmall,
                    color = GreenPrimary
                )
            }
            Text(stringResource(R.string.azkar_item_prayer_anchor), modifier = Modifier.padding(top = 4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = prayerAnchor.isBlank(),
                    onClick = { prayerAnchor = "" },
                    label = { Text(stringResource(R.string.azkar_item_prayer_none)) }
                )
                PrayerName.entries.forEach { prayer ->
                    FilterChip(
                        selected = prayerAnchor == prayer.name,
                        onClick = { prayerAnchor = if (prayerAnchor == prayer.name) "" else prayer.name },
                        label = {
                            Text(
                                stringResource(
                                    when (prayer) {
                                        PrayerName.FAJR -> R.string.prayer_name_fajr
                                        PrayerName.DHUHR -> R.string.prayer_name_dhuhr
                                        PrayerName.ASR -> R.string.prayer_name_asr
                                        PrayerName.MAGHRIB -> R.string.prayer_name_maghrib
                                        PrayerName.ISHA -> R.string.prayer_name_isha
                                    }
                                )
                            )
                        }
                    )
                }
            }
            if (prayerAnchor.isNotBlank()) {
                Text(stringResourceDigits(R.string.azkar_item_prayer_offset, prayerOffset.toInt()))
                Slider(
                    value = prayerOffset,
                    onValueChange = { prayerOffset = it },
                    valueRange = -120f..120f,
                    steps = 47
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.azkar_item_skip_quiet))
                Switch(checked = skipQuiet, onCheckedChange = { skipQuiet = it })
            }
            OutlinedTextField(
                value = hijriMonth,
                onValueChange = { hijriMonth = it },
                label = { Text(stringResource(R.string.hijri_month)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = hijriDayStart,
                onValueChange = { hijriDayStart = it },
                label = { Text(stringResource(R.string.hijri_day_from)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = hijriDayEnd,
                onValueChange = { hijriDayEnd = it },
                label = { Text(stringResource(R.string.hijri_day_to)) },
                modifier = Modifier.fillMaxWidth()
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
                                sortOrder = existing?.sortOrder ?: nextSortOrder,
                                sourceItemId = existing?.sourceItemId,
                                scheduleHour = if (customTime) hour.toInt() else -1,
                                scheduleMinute = if (customTime) minute.toInt() else 0,
                                prayerAnchor = prayerAnchor,
                                prayerOffsetMinutes = if (prayerAnchor.isBlank()) 0 else prayerOffset.toInt(),
                                skipQuietWindow = skipQuiet,
                                hijriMonth = hijriMonth.toIntOrNull() ?: -1,
                                hijriDayStart = hijriDayStart.toIntOrNull() ?: -1,
                                hijriDayEnd = hijriDayEnd.toIntOrNull() ?: -1,
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var hour by remember { mutableFloatStateOf((existing?.scheduleHour ?: 7).toFloat()) }
    var minute by remember { mutableFloatStateOf((existing?.scheduleMinute ?: 0).toFloat()) }
    var dayMode by remember { mutableStateOf(existing?.dayMode ?: CollectionDayMode.WEEKDAYS) }
    var weekMask by remember { mutableIntStateOf(existing?.weekDaysMask ?: 127) }
    var hijriMonth by remember { mutableStateOf(if ((existing?.hijriMonth ?: -1) > 0) existing!!.hijriMonth.toString() else "") }
    var hijriDayStart by remember { mutableStateOf(if ((existing?.hijriDayStart ?: -1) > 0) existing!!.hijriDayStart.toString() else "") }
    var hijriDayEnd by remember { mutableStateOf(if ((existing?.hijriDayEnd ?: -1) > 0) existing!!.hijriDayEnd.toString() else "") }
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
                Text(stringResource(R.string.azkar_time_label))
                Text(
                    "${hour.toInt().toString().padStart(2, '0')}:${minute.toInt().toString().padStart(2, '0')}"
                        .formatLocalizedDigits()
                )
                Slider(value = hour, onValueChange = { hour = it }, valueRange = 0f..23f, steps = 22)
                Slider(value = minute, onValueChange = { minute = it }, valueRange = 0f..59f, steps = 58)
                Text(stringResource(R.string.azkar_day_mode_label), modifier = Modifier.padding(top = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = dayMode == CollectionDayMode.WEEKDAYS,
                        onClick = { dayMode = CollectionDayMode.WEEKDAYS },
                        label = { Text(stringResource(R.string.azkar_day_mode_weekdays)) }
                    )
                    FilterChip(
                        selected = dayMode == CollectionDayMode.HIJRI,
                        onClick = { dayMode = CollectionDayMode.HIJRI },
                        label = { Text(stringResource(R.string.azkar_day_mode_hijri)) }
                    )
                    FilterChip(
                        selected = dayMode == CollectionDayMode.ITEM_HIJRI,
                        onClick = { dayMode = CollectionDayMode.ITEM_HIJRI },
                        label = { Text(stringResource(R.string.azkar_day_mode_item_hijri)) }
                    )
                }
                when (dayMode) {
                    CollectionDayMode.WEEKDAYS -> {
                        Text(stringResource(R.string.azkar_days_label), modifier = Modifier.padding(top = 8.dp))
                        val days = CollectionScheduleHelper.dayLabels("ar")
                        val dayConstants = listOf(
                            Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            days.forEachIndexed { index, label ->
                                val day = dayConstants[index]
                                val selected = CollectionScheduleHelper.isDayEnabled(weekMask, day)
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        weekMask = CollectionScheduleHelper.toggleDay(weekMask, day, !selected)
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                    CollectionDayMode.HIJRI -> {
                        OutlinedTextField(
                            value = hijriMonth,
                            onValueChange = { hijriMonth = it },
                            label = { Text(stringResource(R.string.hijri_month)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = hijriDayStart,
                            onValueChange = { hijriDayStart = it },
                            label = { Text(stringResource(R.string.hijri_day_from)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = hijriDayEnd,
                            onValueChange = { hijriDayEnd = it },
                            label = { Text(stringResource(R.string.hijri_day_to)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    CollectionDayMode.ITEM_HIJRI -> {
                        Text(
                            stringResource(R.string.azkar_blessed_days_schedule_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = GreenPrimary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
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
                        val base = existing ?: AdhkarCollectionEntity(
                            id = id,
                            titleAr = titleAr.trim(),
                            sortOrder = nextSortOrder
                        )
                        onSave(
                            base.copy(
                                id = id,
                                titleAr = titleAr.trim(),
                                titleEn = titleEn.trim(),
                                autoPlayAllowed = autoPlayAllowed,
                                autoPlayEnabled = autoPlayAllowed && autoPlay,
                                scheduleHour = hour.toInt(),
                                scheduleMinute = minute.toInt(),
                                weekDaysMask = weekMask,
                                dayMode = dayMode,
                                hijriMonth = hijriMonth.toIntOrNull() ?: -1,
                                hijriDayStart = hijriDayStart.toIntOrNull() ?: -1,
                                hijriDayEnd = hijriDayEnd.toIntOrNull() ?: -1,
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
