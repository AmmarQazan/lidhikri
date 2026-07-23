package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.runtime.rememberCoroutineScope
import com.greendome.adhkar.data.AzkarFavorites
import com.greendome.adhkar.audio.AzkarPlaybackResolver
import com.greendome.adhkar.audio.AzkarTtsPlayer
import com.greendome.adhkar.audio.DhikrAudioPlayer
import com.greendome.adhkar.audio.playAzkarItemsOrdered
import com.greendome.adhkar.audio.TtsPlaybackState
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.model.AzkarDisplayMode
import com.greendome.adhkar.ui.components.DisplayModeToggle
import com.greendome.adhkar.ui.components.PlaybackControlBar
import kotlinx.coroutines.launch
import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.ui.components.AzkarTextMenu
import com.greendome.adhkar.ui.theme.ArabicText
import com.greendome.adhkar.ui.theme.GoldDome
import com.greendome.adhkar.ui.theme.GreenPrimary
import com.greendome.adhkar.ui.theme.GreenPrimaryDark
import com.greendome.adhkar.ui.theme.formatLocalizedDigits
import com.greendome.adhkar.ui.theme.stringResourceDigits
import com.greendome.adhkar.util.CollectionScheduleHelper
import java.util.Calendar

@Composable
private fun azkarCollectionTitle(collection: AdhkarCollectionEntity, lang: String): String =
    when {
        collection.id == AzkarFavorites.COLLECTION_ID -> stringResource(R.string.azkar_favorites_title)
        lang == "ar" -> collection.titleAr
        else -> collection.titleEn.ifBlank { collection.titleAr }
    }

private fun isAzkarFavorite(item: AzkarItemEntity, favoriteSourceIds: Set<Long>): Boolean =
    when {
        item.collectionId == AzkarFavorites.COLLECTION_ID -> true
        item.id in favoriteSourceIds -> true
        else -> false
    }

@Composable
fun AzkarHubScreen(
    collections: List<AdhkarCollectionEntity>,
    lang: String,
    onOpenCollection: (AdhkarCollectionEntity) -> Unit,
    onOpenMyDhikr: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Text(
            stringResource(R.string.azkar_hub_hint),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = GreenPrimaryDark
        )
        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenMyDhikr() },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.my_dhikr_section),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimaryDark
                        )
                        Text(
                            stringResource(R.string.my_dhikr_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = GreenPrimary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            items(collections, key = { it.id }) { collection ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenCollection(collection) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            azkarCollectionTitle(collection, lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimaryDark
                        )
                        if (collection.autoPlayAllowed && collection.autoPlayEnabled) {
                            Text(
                                stringResourceDigits(R.string.azkar_auto_on, CollectionScheduleHelper.formatScheduleAr(collection)),
                                style = MaterialTheme.typography.labelSmall,
                                color = GreenPrimary,
                                modifier = Modifier.padding(top = 4.dp)
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
fun AzkarCollectionScreen(
    collection: AdhkarCollectionEntity,
    items: List<AzkarItemEntity>,
    lang: String,
    favoriteSourceIds: Set<Long>,
    displayMode: AzkarDisplayMode,
    onDisplayModeChange: (AzkarDisplayMode) -> Unit,
    onBack: () -> Unit,
    onSaveSchedule: (AdhkarCollectionEntity) -> Unit,
    onToggleFavorite: (AzkarItemEntity) -> Unit
) {
    val isFavoritesCollection = collection.id == AzkarFavorites.COLLECTION_ID
    var autoPlay by remember(collection.id) { mutableStateOf(collection.autoPlayEnabled) }
    var useTts by remember(collection.id) { mutableStateOf(collection.useTtsAutoPlay) }
    var hour by remember(collection.id) { mutableFloatStateOf(collection.scheduleHour.toFloat()) }
    var minute by remember(collection.id) { mutableFloatStateOf(collection.scheduleMinute.toFloat()) }
    var weekMask by remember(collection.id) { mutableIntStateOf(collection.weekDaysMask) }
    var scheduleExpanded by remember(collection.id) { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<AzkarItemEntity?>(null) }
    var counter by remember { mutableIntStateOf(0) }
    var selectedIds by remember(collection.id, items) {
        mutableStateOf(items.map { it.id }.toSet())
    }
    var speechRate by remember(collection.id) { mutableFloatStateOf(1f) }
    var playbackState by remember { mutableStateOf(TtsPlaybackState.IDLE) }
    var usingReciterAudio by remember { mutableStateOf(false) }
    var playingItemId by remember { mutableStateOf<Long?>(null) }
    var stopPlaybackRequested by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { SettingsRepository(context) }
    val tts = remember(settings) { AzkarTtsPlayer(context) { settings } }
    val audioPlayer = remember { DhikrAudioPlayer(context) }

    DisposableEffect(Unit) {
        tts.onStateChanged = { state ->
            if (!usingReciterAudio) {
                playbackState = state
                if (state == TtsPlaybackState.IDLE) playingItemId = null
            }
        }
        onDispose {
            tts.onStateChanged = null
            tts.shutdown()
            audioPlayer.stop()
        }
    }

    LaunchedEffect(speechRate) {
        tts.speechRate = speechRate
    }

    fun textsForSelected(): List<String> =
        items
            .filter { it.id in selectedIds }
            .flatMap { item -> List(item.repeatCount.coerceAtLeast(1)) { item.textAr } }

    fun stopPlayback() {
        stopPlaybackRequested = true
        audioPlayer.stop()
        tts.stop()
        usingReciterAudio = false
        playbackState = TtsPlaybackState.IDLE
        playingItemId = null
    }

    fun beginPlayback() {
        stopPlaybackRequested = false
        audioPlayer.stop()
        tts.stop()
        usingReciterAudio = false
        playbackState = TtsPlaybackState.IDLE
        playingItemId = null
    }

    fun playItem(item: AzkarItemEntity) {
        scope.launch {
            beginPlayback()
            playingItemId = item.id
            playbackState = TtsPlaybackState.PLAYING
            playAzkarItemsOrdered(
                context = context,
                items = listOf(item),
                useTts = useTts,
                audioPlayer = audioPlayer,
                tts = tts,
                settings = settings,
                isCancelled = { stopPlaybackRequested },
            )
            if (!stopPlaybackRequested) {
                usingReciterAudio = false
                playbackState = TtsPlaybackState.IDLE
                playingItemId = null
            }
        }
    }

    fun playSelected() {
        if (selectedIds.isEmpty()) return
        scope.launch {
            beginPlayback()
            val selectedItems = items.filter { it.id in selectedIds }
            usingReciterAudio = selectedItems.any { item ->
                AzkarPlaybackResolver.resolvePlayable(context, item) != null
            }
            playbackState = TtsPlaybackState.PLAYING
            playAzkarItemsOrdered(
                context = context,
                items = selectedItems,
                useTts = useTts,
                audioPlayer = audioPlayer,
                tts = tts,
                settings = settings,
                isCancelled = { stopPlaybackRequested },
            )
            if (!stopPlaybackRequested) {
                usingReciterAudio = false
                playbackState = TtsPlaybackState.IDLE
                playingItemId = null
            }
        }
    }

    fun persist() {
        onSaveSchedule(
            collection.copy(
                autoPlayEnabled = collection.autoPlayAllowed && autoPlay,
                useTtsAutoPlay = useTts,
                scheduleHour = hour.toInt(),
                scheduleMinute = minute.toInt(),
                weekDaysMask = weekMask
            )
        )
    }

    @Composable
    fun AzkarPlaybackSection(modifier: Modifier = Modifier) {
        Column(modifier = modifier) {
            PlaybackControlBar(
                playbackState = playbackState,
                canPause = !usingReciterAudio && playbackState == TtsPlaybackState.PLAYING,
                canResume = !usingReciterAudio && playbackState == TtsPlaybackState.PAUSED,
                onPlay = { playSelected() },
                onPause = { tts.pause() },
                onResume = { tts.resume() },
                onStop = { stopPlayback() },
                playEnabled = selectedIds.isNotEmpty() && playbackState != TtsPlaybackState.PLAYING,
            )
            when {
                usingReciterAudio && playbackState == TtsPlaybackState.PLAYING -> Text(
                    stringResource(R.string.azkar_playing_reciter),
                    style = MaterialTheme.typography.labelMedium,
                    color = GreenPrimary,
                    modifier = Modifier.padding(top = 4.dp),
                )
                playbackState == TtsPlaybackState.PLAYING -> Text(
                    stringResource(R.string.azkar_playing),
                    style = MaterialTheme.typography.labelMedium,
                    color = GreenPrimary,
                    modifier = Modifier.padding(top = 4.dp),
                )
                playbackState == TtsPlaybackState.PAUSED -> Text(
                    stringResource(R.string.azkar_paused),
                    style = MaterialTheme.typography.labelMedium,
                    color = GoldDome,
                    modifier = Modifier.padding(top = 4.dp),
                )
                else -> Unit
            }
            if (collection.autoPlayAllowed && !isFavoritesCollection) {
                SettingRow(stringResource(R.string.azkar_tts_auto), useTts) {
                    useTts = it
                    persist()
                }
            } else {
                SettingRow(stringResource(R.string.azkar_tts_auto), useTts) {
                    useTts = it
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.azkar_speed_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = GreenPrimaryDark,
                )
                Text(
                    stringResourceDigits(R.string.azkar_speed_value, speechRate),
                    style = MaterialTheme.typography.labelMedium,
                    color = GreenPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Slider(
                value = speechRate,
                onValueChange = { speechRate = it },
                valueRange = 0.5f..2f,
                steps = 5,
                enabled = useTts && !usingReciterAudio,
            )
        }
    }

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
                    DisplayModeToggle(
                        selected = displayMode,
                        onSelected = onDisplayModeChange,
                    )
                }
            )
        }
    ) { padding ->
        if (displayMode == AzkarDisplayMode.CARD) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            ) {
                AzkarPlaybackSection(modifier = Modifier.padding(vertical = 8.dp))
                AzkarCardReader(
                    items = items,
                    isFavorite = { item -> isAzkarFavorite(item, favoriteSourceIds) },
                    onToggleFavorite = onToggleFavorite,
                    playingItemId = playingItemId,
                    playbackState = playbackState,
                    usingReciterAudio = usingReciterAudio,
                    onPlayItem = ::playItem,
                    onPause = { tts.pause() },
                    onResume = { tts.resume() },
                    onStopPlayback = ::stopPlayback,
                    modifier = Modifier.weight(1f),
                )
            }
        } else if (selectedItem != null) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    ArabicText(
                        text = selectedItem!!.textAr,
                        style = MaterialTheme.typography.titleLarge,
                        color = GreenPrimaryDark,
                        modifier = Modifier.weight(1f)
                    )
                    AzkarTextMenu(text = selectedItem!!.textAr)
                    IconButton(onClick = { onToggleFavorite(selectedItem!!) }) {
                        Icon(
                            imageVector = if (isAzkarFavorite(selectedItem!!, favoriteSourceIds)) {
                                Icons.Default.Favorite
                            } else {
                                Icons.Default.FavoriteBorder
                            },
                            contentDescription = stringResource(
                                if (isAzkarFavorite(selectedItem!!, favoriteSourceIds)) {
                                    R.string.azkar_remove_favorite
                                } else {
                                    R.string.azkar_add_favorite
                                }
                            ),
                            tint = GoldDome
                        )
                    }
                }
                if (selectedItem!!.virtueAr.isNotBlank()) {
                    Text(
                        selectedItem!!.virtueAr,
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = GoldDome
                    )
                }
                Text(
                    stringResourceDigits(R.string.counter, counter),
                    modifier = Modifier.padding(vertical = 16.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    color = GreenPrimary
                )
                OutlinedButton(
                    onClick = { counter++ },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("+1 / ${selectedItem!!.repeatCount}".formatLocalizedDigits()) }
                OutlinedButton(
                    onClick = { selectedItem = null; counter = 0 },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.mark_done)) }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    if (collection.autoPlayAllowed && !isFavoritesCollection) {
                        OutlinedButton(
                            onClick = { scheduleExpanded = !scheduleExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null)
                            Text(
                                if (scheduleExpanded) {
                                    stringResource(R.string.azkar_schedule_hide)
                                } else {
                                    stringResource(R.string.azkar_schedule_show)
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        if (scheduleExpanded) {
                            SectionTitle(
                                title = stringResource(R.string.azkar_schedule_section),
                                subtitle = stringResource(R.string.azkar_schedule_hint)
                            )
                            SettingRow(stringResource(R.string.azkar_auto_enable), autoPlay) {
                                autoPlay = it
                                persist()
                            }
                            Text(stringResource(R.string.azkar_time_label), modifier = Modifier.padding(top = 8.dp))
                            Text(
                                "${hour.toInt().toString().padStart(2, '0')}:${minute.toInt().toString().padStart(2, '0')}"
                                    .formatLocalizedDigits()
                            )
                            Slider(value = hour, onValueChange = { hour = it; persist() }, valueRange = 0f..23f, steps = 22)
                            Slider(value = minute, onValueChange = { minute = it; persist() }, valueRange = 0f..59f, steps = 58)
                            Text(stringResource(R.string.azkar_days_label), modifier = Modifier.padding(top = 8.dp))
                            val days = CollectionScheduleHelper.dayLabelsAr()
                            val dayConstants = listOf(
                                Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                                Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                days.forEachIndexed { index, label ->
                                    val day = dayConstants[index]
                                    val selected = CollectionScheduleHelper.isDayEnabled(weekMask, day)
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            weekMask = CollectionScheduleHelper.toggleDay(weekMask, day, !selected)
                                            persist()
                                        },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = GreenPrimary.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                stringResource(R.string.azkar_auto_not_allowed),
                                modifier = Modifier.padding(14.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = GreenPrimaryDark
                            )
                        }
                    }
                }
                item {
                    AzkarPlaybackSection()
                }
                item {
                    SectionTitle(
                        title = stringResource(R.string.azkar_read_section),
                        subtitle = stringResourceDigits(R.string.azkar_read_hint, items.size)
                    )
                    Text(
                        stringResource(R.string.azkar_select_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = GreenPrimaryDark,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        FilterChip(
                            selected = selectedIds.size == items.size,
                            onClick = { selectedIds = items.map { it.id }.toSet() },
                            label = { Text(stringResource(R.string.azkar_select_all)) }
                        )
                        FilterChip(
                            selected = selectedIds.isEmpty(),
                            onClick = { selectedIds = emptySet() },
                            label = { Text(stringResource(R.string.azkar_select_none)) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { playSelected() },
                            enabled = selectedIds.isNotEmpty() && playbackState != TtsPlaybackState.PLAYING,
                            label = { Text(stringResourceDigits(R.string.azkar_play_selected, selectedIds.size)) }
                        )
                    }
                }
                items(items, key = { it.id }) { item ->
                    val checked = item.id in selectedIds
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { on ->
                                    selectedIds = if (on) selectedIds + item.id else selectedIds - item.id
                                }
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedItem = item; counter = 0 }
                            ) {
                                ArabicText(
                                    text = item.textAr,
                                    maxLines = 3,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (item.repeatCount > 1) {
                                    Text(
                                        stringResourceDigits(R.string.azkar_repeat_label, item.repeatCount),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GoldDome
                                    )
                                }
                            }
                            val isFavorite = isAzkarFavorite(item, favoriteSourceIds)
                            IconButton(onClick = { onToggleFavorite(item) }) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = stringResource(
                                        if (isFavorite) R.string.azkar_remove_favorite else R.string.azkar_add_favorite
                                    ),
                                    tint = if (isFavorite) GoldDome else GreenPrimary.copy(alpha = 0.5f)
                                )
                            }
                            val isPlayingThis = playingItemId == item.id && playbackState != TtsPlaybackState.IDLE
                            IconButton(
                                onClick = {
                                    if (isPlayingThis) stopPlayback() else playItem(item)
                                }
                            ) {
                                Icon(
                                    imageVector = if (isPlayingThis) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = stringResource(R.string.azkar_play_item),
                                    tint = if (isPlayingThis) GoldDome else GreenPrimary
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
private fun SettingRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
