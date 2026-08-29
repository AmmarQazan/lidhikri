package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greendome.adhkar.R
import androidx.compose.runtime.rememberCoroutineScope
import com.greendome.adhkar.data.AzkarFavorites
import com.greendome.adhkar.audio.AzkarPlaybackResolver
import com.greendome.adhkar.audio.DhikrAudioPlayer
import com.greendome.adhkar.audio.playAzkarItemsOrdered
import com.greendome.adhkar.audio.TtsPlaybackState
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.model.AzkarDisplayMode
import com.greendome.adhkar.data.model.AzkarListText
import com.greendome.adhkar.ui.components.AudioUnavailableDialog
import com.greendome.adhkar.ui.components.AzkarFontSizeButtons
import com.greendome.adhkar.ui.components.DisplayModeToggle
import com.greendome.adhkar.ui.components.PlaybackControlBar
import kotlinx.coroutines.launch
import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.data.local.DhikrEntity
import com.greendome.adhkar.prayer.PrayerRespectGate
import com.greendome.adhkar.ui.components.AzkarTextMenu
import com.greendome.adhkar.ui.theme.AppCardColors
import com.greendome.adhkar.ui.theme.ArabicText
import com.greendome.adhkar.ui.theme.GoldDome
import com.greendome.adhkar.ui.theme.GreenPrimary
import com.greendome.adhkar.ui.theme.GreenPrimaryDark
import com.greendome.adhkar.ui.theme.formatLocalizedDigits
import com.greendome.adhkar.ui.theme.stringResourceDigits
import com.greendome.adhkar.util.AzkarSearch
import com.greendome.adhkar.util.AzkarSearchHit
import com.greendome.adhkar.util.CollectionScheduleHelper
import java.util.Calendar

private const val AZKAR_LIST_HEADER_COUNT = 3

@Composable
fun azkarCollectionTitle(collection: AdhkarCollectionEntity, lang: String): String =
    when {
        collection.id == AzkarFavorites.COLLECTION_ID -> stringResource(R.string.azkar_favorites_title)
        else -> collection.localizedTitle(lang)
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
    allItems: List<AzkarItemEntity>,
    customDhikr: List<DhikrEntity>,
    lang: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenCollection: (AdhkarCollectionEntity) -> Unit,
    onOpenItem: (AdhkarCollectionEntity, AzkarItemEntity) -> Unit,
    onOpenMyDhikr: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val myDhikrSectionTitle = stringResource(R.string.my_dhikr_section)
    val results = remember(searchQuery, allItems, collections, customDhikr, lang, myDhikrSectionTitle) {
        val sources = buildList {
            val collectionsById = collections.associateBy { it.id }
            allItems.forEach { item ->
                if (item.collectionId == AzkarFavorites.COLLECTION_ID) return@forEach
                val collection = collectionsById[item.collectionId]
                val sectionTitle = when {
                    collection == null -> item.collectionId
                    else -> collection.localizedTitle(lang)
                }
                add(
                    AzkarSearchHit(
                        key = "azkar:${item.id}",
                        textAr = item.localizedText(lang),
                        virtueAr = item.localizedVirtue(lang),
                        sectionTitle = sectionTitle,
                        collectionId = item.collectionId,
                        itemId = item.id,
                        extraSearchText = item.textAr,
                    )
                )
            }
            customDhikr.filter { !it.isDefault }.forEach { dhikr ->
                add(
                    AzkarSearchHit(
                        key = "dhikr:${dhikr.id}",
                        textAr = dhikr.localizedText(lang).ifBlank { dhikr.textAr },
                        sectionTitle = myDhikrSectionTitle,
                        extraSearchText = listOf(
                            dhikr.textAr, dhikr.textEn, dhikr.textFr, dhikr.textEs,
                            dhikr.textTr, dhikr.textUr, dhikr.textId, dhikr.textHi
                        ).joinToString(" "),
                        isMyDhikr = true,
                    )
                )
            }
        }
        AzkarSearch.search(searchQuery, sources)
    }
    val searching = AzkarSearch.normalize(searchQuery).isNotEmpty()
    val collectionsById = remember(collections) { collections.associateBy { it.id } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text(stringResource(R.string.azkar_search_hint)) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.azkar_search_clear)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search,
                showKeyboardOnFocus = true,
            ),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        )
        if (searching) {
            Text(
                if (results.isEmpty()) {
                    stringResource(R.string.azkar_search_empty)
                } else {
                    stringResourceDigits(R.string.azkar_search_results, results.size)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(results, key = { it.key }) { hit ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                focusManager.clearFocus()
                                when {
                                    hit.isMyDhikr -> onOpenMyDhikr()
                                    hit.collectionId != null && hit.itemId != null -> {
                                        val collection = collectionsById[hit.collectionId]
                                        val item = allItems.find { it.id == hit.itemId }
                                        if (collection != null && item != null) {
                                            onOpenItem(collection, item)
                                        }
                                    }
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = AppCardColors()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.azkar_search_section, hit.sectionTitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = GreenPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            ArabicText(
                                text = hit.textAr,
                                modifier = Modifier.padding(top = 8.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (hit.virtueAr.isNotBlank()) {
                                Text(
                                    hit.virtueAr,
                                    modifier = Modifier.padding(top = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldDome,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.padding(bottom = 8.dp)) }
            }
        } else {
            Text(
                stringResource(R.string.azkar_hub_hint),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
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
                        colors = AppCardColors()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.my_dhikr_section),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
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
                        colors = AppCardColors()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                azkarCollectionTitle(collection, lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (collection.autoPlayAllowed && collection.autoPlayEnabled) {
                                Text(
                                    stringResourceDigits(R.string.azkar_auto_on, CollectionScheduleHelper.formatSchedule(collection, lang)),
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
    onToggleFavorite: (AzkarItemEntity) -> Unit,
    onOpenAfterPrayerSettings: () -> Unit = {},
    initialItemId: Long? = null,
    modifier: Modifier = Modifier,
) {
    var autoPlay by remember(collection.id) { mutableStateOf(collection.autoPlayEnabled) }
    var hour by remember(collection.id) { mutableFloatStateOf(collection.scheduleHour.toFloat()) }
    var minute by remember(collection.id) { mutableFloatStateOf(collection.scheduleMinute.toFloat()) }
    var weekMask by remember(collection.id) { mutableIntStateOf(collection.weekDaysMask) }
    var scheduleExpanded by remember(collection.id) { mutableStateOf(false) }
    var selectedItem by remember(collection.id) { mutableStateOf<AzkarItemEntity?>(null) }
    var counter by remember { mutableIntStateOf(0) }
    var selectedIds by remember(collection.id, items) {
        mutableStateOf(items.map { it.id }.toSet())
    }
    val listState = rememberLazyListState()
    var playbackState by remember { mutableStateOf(TtsPlaybackState.IDLE) }
    var playingItemId by remember { mutableStateOf<Long?>(null) }
    var stopPlaybackRequested by remember { mutableStateOf(false) }
    var showNoAudioAlert by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { SettingsRepository(context) }
    val audioPlayer = remember { DhikrAudioPlayer(context) }
    var listFontSp by remember { mutableIntStateOf(settings.azkarListFontSizeSp) }
    val listTextStyle = MaterialTheme.typography.titleMedium.copy(
        fontSize = listFontSp.sp,
        lineHeight = (listFontSp * AzkarListText.LINE_HEIGHT_RATIO).sp,
    )
    fun changeListFont(delta: Int) {
        val next = (listFontSp + delta).coerceIn(
            AzkarListText.MIN_FONT_SP,
            AzkarListText.MAX_FONT_SP,
        )
        listFontSp = next
        settings.azkarListFontSizeSp = next
    }

    LaunchedEffect(collection.id, initialItemId, items, displayMode) {
        if (displayMode != AzkarDisplayMode.LIST) return@LaunchedEffect
        val itemIndex = initialItemId?.let { id -> items.indexOfFirst { it.id == id } }
            ?.takeIf { it >= 0 } ?: return@LaunchedEffect
        listState.animateScrollToItem(AZKAR_LIST_HEADER_COUNT + itemIndex)
    }

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.stop()
        }
    }

    fun stopPlayback() {
        stopPlaybackRequested = true
        audioPlayer.stop()
        playbackState = TtsPlaybackState.IDLE
        playingItemId = null
    }

    fun beginPlayback() {
        stopPlaybackRequested = false
        audioPlayer.stop()
        playbackState = TtsPlaybackState.IDLE
        playingItemId = null
    }

    fun playItem(item: AzkarItemEntity) {
        scope.launch {
            val playable = AzkarPlaybackResolver.resolvePlayable(context, item)
            if (playable == null) {
                showNoAudioAlert = true
                return@launch
            }
            beginPlayback()
            playingItemId = item.id
            playbackState = TtsPlaybackState.PLAYING
            playAzkarItemsOrdered(
                context = context,
                items = listOf(item),
                audioPlayer = audioPlayer,
                settings = settings,
                isCancelled = { stopPlaybackRequested },
                onMissingAudio = { showNoAudioAlert = true },
            )
            if (!stopPlaybackRequested) {
                playbackState = TtsPlaybackState.IDLE
                playingItemId = null
            }
        }
    }

    fun playSelected() {
        if (selectedIds.isEmpty()) return
        scope.launch {
            val selectedItems = items.filter { it.id in selectedIds }
            val hasAnyAudio = selectedItems.any {
                AzkarPlaybackResolver.resolvePlayable(context, it) != null
            }
            if (!hasAnyAudio) {
                showNoAudioAlert = true
                return@launch
            }
            beginPlayback()
            playbackState = TtsPlaybackState.PLAYING
            playAzkarItemsOrdered(
                context = context,
                items = selectedItems,
                audioPlayer = audioPlayer,
                settings = settings,
                isCancelled = { stopPlaybackRequested },
                onMissingAudio = { showNoAudioAlert = true },
            )
            if (!stopPlaybackRequested) {
                playbackState = TtsPlaybackState.IDLE
                playingItemId = null
            }
        }
    }

    fun persist() {
        onSaveSchedule(
            collection.copy(
                autoPlayEnabled = collection.autoPlayAllowed && autoPlay,
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
                canPause = false,
                canResume = false,
                onPlay = { playSelected() },
                onPause = {},
                onResume = {},
                onStop = { stopPlayback() },
                playEnabled = selectedIds.isNotEmpty() && playbackState != TtsPlaybackState.PLAYING,
            )
            if (playbackState == TtsPlaybackState.PLAYING) {
                Text(
                    stringResource(R.string.azkar_playing_reciter),
                    style = MaterialTheme.typography.labelMedium,
                    color = GreenPrimary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }

    Scaffold(
        modifier = modifier,
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
            AzkarCardReader(
                items = items,
                isFavorite = { item -> isAzkarFavorite(item, favoriteSourceIds) },
                onToggleFavorite = onToggleFavorite,
                playingItemId = playingItemId,
                playbackState = playbackState,
                onPlayItem = ::playItem,
                onStopPlayback = ::stopPlayback,
                initialItemId = initialItemId,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        } else if (selectedItem != null) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AzkarFontSizeButtons(
                        fontSp = listFontSp,
                        minSp = AzkarListText.MIN_FONT_SP,
                        maxSp = AzkarListText.MAX_FONT_SP,
                        onDecrease = { changeListFont(-AzkarListText.STEP_SP) },
                        onIncrease = { changeListFont(AzkarListText.STEP_SP) },
                    )
                    AzkarTextMenu(text = selectedItem!!.localizedText(lang))
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
                    val isPlayingSelected = playingItemId == selectedItem!!.id &&
                        playbackState != TtsPlaybackState.IDLE
                    IconButton(
                        onClick = {
                            if (isPlayingSelected) stopPlayback() else playItem(selectedItem!!)
                        }
                    ) {
                        Icon(
                            imageVector = if (isPlayingSelected) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.azkar_play_item),
                            tint = if (isPlayingSelected) GoldDome else GreenPrimary
                        )
                    }
                }
                ArabicText(
                    text = selectedItem!!.localizedText(lang),
                    style = listTextStyle,
                    color = GreenPrimaryDark,
                    modifier = Modifier.fillMaxWidth()
                )
                if (selectedItem!!.virtueAr.isNotBlank()) {
                    Text(
                        selectedItem!!.localizedVirtue(lang),
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
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    val isAfterPrayer = collection.id == PrayerRespectGate.AFTER_PRAYER_COLLECTION_ID
                    when {
                        isAfterPrayer -> {
                            val afterPrayerOn = settings.respectPrayerTime && settings.afterPrayerFromSalahEnabled
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onOpenAfterPrayerSettings)
                            ) {
                                SettingRow(stringResource(R.string.azkar_auto_enable), afterPrayerOn) {
                                    onOpenAfterPrayerSettings()
                                }
                                Text(
                                    stringResource(
                                        if (afterPrayerOn) R.string.azkar_after_prayer_auto_on_hint
                                        else R.string.azkar_after_prayer_auto_hint
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                        collection.autoPlayAllowed -> {
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
                                val days = CollectionScheduleHelper.dayLabels(lang)
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
                        }
                        else -> {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = GreenPrimary.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    stringResource(R.string.azkar_auto_not_allowed),
                                    modifier = Modifier.padding(14.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                item {
                    AzkarPlaybackSection()
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            SectionTitle(
                                title = stringResource(R.string.azkar_read_section),
                                subtitle = stringResourceDigits(R.string.azkar_read_hint, items.size)
                            )
                        }
                        AzkarFontSizeButtons(
                            fontSp = listFontSp,
                            minSp = AzkarListText.MIN_FONT_SP,
                            maxSp = AzkarListText.MAX_FONT_SP,
                            onDecrease = { changeListFont(-AzkarListText.STEP_SP) },
                            onIncrease = { changeListFont(AzkarListText.STEP_SP) },
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
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
                    val isSearchTarget = initialItemId != null && item.id == initialItemId
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = if (isSearchTarget) BorderStroke(2.dp, GreenPrimary) else null,
                        colors = if (isSearchTarget) {
                            CardDefaults.cardColors(
                                containerColor = GreenPrimary.copy(alpha = 0.08f)
                            )
                        } else {
                            CardDefaults.cardColors()
                        },
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
                                    text = item.localizedText(lang),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    style = listTextStyle,
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

    if (showNoAudioAlert) {
        AudioUnavailableDialog(onDismiss = { showNoAudioAlert = false })
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
