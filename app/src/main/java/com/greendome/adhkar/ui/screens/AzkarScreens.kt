package com.greendome.adhkar.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import com.greendome.adhkar.util.AzkarHubOrder
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
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
import androidx.compose.runtime.mutableStateListOf
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
import com.greendome.adhkar.ui.azkarHubSectionIcon
import androidx.compose.runtime.rememberCoroutineScope
import com.greendome.adhkar.data.AdhanAzkar
import com.greendome.adhkar.data.AutoAzkarCatalog
import com.greendome.adhkar.data.AzkarFavorites
import com.greendome.adhkar.data.BlessedDaysAzkar
import com.greendome.adhkar.data.FridayAzkar
import com.greendome.adhkar.data.HomeAzkar
import com.greendome.adhkar.data.RidingAzkar
import com.greendome.adhkar.data.model.CollectionDayMode
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.data.local.DhikrEntity
import com.greendome.adhkar.prayer.PrayerRespectGate
import com.greendome.adhkar.service.AdhkarReminderService
import com.greendome.adhkar.service.AfterPrayerAlarmScheduler
import com.greendome.adhkar.service.AutoAzkarEventPlayer
import com.greendome.adhkar.service.HomeGeofenceScheduler
import com.greendome.adhkar.service.ReminderScheduler
import com.greendome.adhkar.service.VehicleActivityScheduler
import com.greendome.adhkar.util.RuntimePermissions
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
    savedOrder: List<String> = emptyList(),
    onReorder: (List<String>) -> Unit = {},
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
            Text(
                stringResource(R.string.azkar_hub_reorder_hint),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp),
                style = MaterialTheme.typography.labelSmall,
                color = GreenPrimary
            )
            AzkarHubSectionsList(
                collections = collections,
                lang = lang,
                savedOrder = savedOrder,
                onOpenCollection = onOpenCollection,
                onOpenMyDhikr = onOpenMyDhikr,
                onReorder = onReorder,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AzkarHubSectionsList(
    collections: List<AdhkarCollectionEntity>,
    lang: String,
    savedOrder: List<String>,
    onOpenCollection: (AdhkarCollectionEntity) -> Unit,
    onOpenMyDhikr: () -> Unit,
    onReorder: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleArById = remember(collections) { collections.associate { it.id to it.titleAr } }
    val collectionsById = remember(collections) { collections.associateBy { it.id } }
    val presentIds = remember(collections) { AzkarHubOrder.presentIds(collections) }
    val merged = remember(collections, savedOrder) {
        AzkarHubOrder.merge(savedOrder, presentIds, titleArById)
    }
    val orderedIds = remember { mutableStateListOf<String>() }
    LaunchedEffect(merged) {
        if (orderedIds.toList() != merged) {
            orderedIds.clear()
            orderedIds.addAll(merged)
        }
    }
    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        val next = AzkarHubOrder.moved(orderedIds.toList(), from.index, to.index)
        orderedIds.clear()
        orderedIds.addAll(next)
        onReorder(next)
    }
    val context = LocalContext.current
    val settings = remember { SettingsRepository(context) }
    val eventFlags = AutoAzkarCatalog.EventAutoFlags(
        afterPrayer = settings.afterPrayerFromSalahEnabled,
        afterAdhan = settings.afterAdhanAzkarEnabled,
        home = settings.homeAzkarEnabled,
        riding = settings.ridingAzkarEnabled,
    )
    val myDhikrTitle = stringResource(R.string.my_dhikr_section)
    val myDhikrHint = stringResource(R.string.my_dhikr_hint)
    val dragHandleDesc = stringResource(R.string.azkar_hub_drag_handle)

    LazyColumn(
        state = listState,
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
    ) {
        items(orderedIds, key = { it }) { id ->
            ReorderableItem(reorderableState, key = id) { isDragging ->
                val collection = collectionsById[id]
                val title = if (id == AzkarHubOrder.MY_DHIKR_ID) {
                    myDhikrTitle
                } else {
                    collection?.let { azkarCollectionTitle(it, lang) }.orEmpty()
                }
                val autoKind = collection?.let { AutoAzkarCatalog.hubAutoKind(it, eventFlags) }
                val subtitle = when {
                    id == AzkarHubOrder.MY_DHIKR_ID -> myDhikrHint
                    autoKind == AutoAzkarCatalog.HubAutoKind.CLOCK ->
                        collection?.let {
                            stringResourceDigits(
                                R.string.azkar_auto_on,
                                CollectionScheduleHelper.formatSchedule(it, lang),
                            )
                        }
                    autoKind == AutoAzkarCatalog.HubAutoKind.AFTER_PRAYER ->
                        stringResource(R.string.azkar_hub_auto_after_prayer)
                    autoKind == AutoAzkarCatalog.HubAutoKind.AFTER_ADHAN ->
                        stringResource(R.string.azkar_hub_auto_after_adhan)
                    autoKind == AutoAzkarCatalog.HubAutoKind.HOME ->
                        stringResource(R.string.azkar_hub_auto_home)
                    autoKind == AutoAzkarCatalog.HubAutoKind.RIDING ->
                        stringResource(R.string.azkar_hub_auto_riding)
                    else -> null
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (isDragging) 1f else 0f)
                        .shadow(if (isDragging) 8.dp else 0.dp, RoundedCornerShape(16.dp))
                        .longPressDraggableHandle()
                        .clickable {
                            if (id == AzkarHubOrder.MY_DHIKR_ID) onOpenMyDhikr()
                            else collection?.let(onOpenCollection)
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = AppCardColors(),
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AzkarHubSectionIcon(sectionId = id)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp, end = 4.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                if (autoKind != null) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = GreenPrimary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                            if (subtitle != null) {
                                Text(
                                    subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GreenPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                        Icon(
                            Icons.Default.DragHandle,
                            contentDescription = dragHandleDesc,
                            modifier = Modifier.draggableHandle(),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AzkarHubSectionIcon(
    sectionId: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .background(GreenPrimary.copy(alpha = 0.16f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = azkarHubSectionIcon(sectionId),
            contentDescription = null,
            tint = GreenPrimary,
            modifier = Modifier.size(26.dp),
        )
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
    onOpenHomeAzkarSettings: () -> Unit = {},
    onOpenAdhanSettings: () -> Unit = {},
    initialItemId: Long? = null,
    modifier: Modifier = Modifier,
) {
    var autoPlay by remember(collection.id) { mutableStateOf(collection.autoPlayEnabled) }
    var hour by remember(collection.id) { mutableFloatStateOf(collection.scheduleHour.toFloat()) }
    var minute by remember(collection.id) { mutableFloatStateOf(collection.scheduleMinute.toFloat()) }
    var weekMask by remember(collection.id) {
        mutableIntStateOf(
            when (collection.id) {
                FridayAzkar.COLLECTION_ID -> FridayAzkar.fridayOnlyMask()
                else -> collection.weekDaysMask
            }
        )
    }
    var scheduleExpanded by remember(collection.id) { mutableStateOf(false) }
    var selectedItem by remember(collection.id) { mutableStateOf<AzkarItemEntity?>(null) }
    var counter by remember { mutableIntStateOf(0) }
    var selectedIds by remember(collection.id, items) {
        mutableStateOf(items.map { it.id }.toSet())
    }
    val listState = rememberLazyListState()
    var playbackState by remember { mutableStateOf(TtsPlaybackState.IDLE) }
    var playingItemId by remember { mutableStateOf<Long?>(null) }
    var playbackQueueIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    var stopPlaybackRequested by remember { mutableStateOf(false) }
    var skipCurrentRequested by remember { mutableStateOf(false) }
    var playbackJob by remember { mutableStateOf<Job?>(null) }
    var showNoAudioAlert by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { SettingsRepository(context) }
    var afterPrayerOn by remember(collection.id) { mutableStateOf(settings.afterPrayerFromSalahEnabled) }
    var afterAdhanOn by remember(collection.id) { mutableStateOf(settings.afterAdhanAzkarEnabled) }
    var homeAzkarOn by remember(collection.id) { mutableStateOf(settings.homeAzkarEnabled) }
    var ridingAzkarOn by remember(collection.id) { mutableStateOf(settings.ridingAzkarEnabled) }
    var monitorTick by remember(collection.id) { mutableIntStateOf(0) }
    val ridingPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            VehicleActivityScheduler.register(context)
        } else {
            ridingAzkarOn = false
            settings.ridingAzkarEnabled = false
            VehicleActivityScheduler.unregister(context)
        }
    }
    LaunchedEffect(collection.id) {
        val tracksMonitor = collection.id == HomeAzkar.COLLECTION_ID ||
            collection.id == RidingAzkar.COLLECTION_ID
        if (!tracksMonitor) return@LaunchedEffect
        while (isActive) {
            monitorTick++
            delay(2_000)
        }
    }
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

    LaunchedEffect(playingItemId, displayMode, selectedItem) {
        if (displayMode != AzkarDisplayMode.LIST || selectedItem != null) return@LaunchedEffect
        val id = playingItemId ?: return@LaunchedEffect
        val itemIndex = items.indexOfFirst { it.id == id }.takeIf { it >= 0 } ?: return@LaunchedEffect
        val listIndex = AZKAR_LIST_HEADER_COUNT + itemIndex
        val alreadyVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == listIndex }
        if (!alreadyVisible) {
            listState.animateScrollToItem(listIndex)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            playbackJob?.cancel()
            audioPlayer.stop()
        }
    }

    fun stopPlayback() {
        stopPlaybackRequested = true
        skipCurrentRequested = false
        playbackJob?.cancel()
        audioPlayer.stop()
        playbackState = TtsPlaybackState.IDLE
        playingItemId = null
        playbackQueueIds = emptyList()
    }

    fun beginPlayback() {
        stopPlaybackRequested = false
        skipCurrentRequested = false
        audioPlayer.resetForNewPlayback()
        playbackState = TtsPlaybackState.IDLE
        playingItemId = null
    }

    fun skipToNextDhikr() {
        if (playbackState != TtsPlaybackState.PLAYING) return
        val currentId = playingItemId
        if (currentId != null) {
            val idx = playbackQueueIds.indexOf(currentId)
            if (idx < 0 || idx >= playbackQueueIds.lastIndex) return
        } else if (playbackQueueIds.size <= 1) {
            return
        }
        skipCurrentRequested = true
        audioPlayer.finishCurrentItem()
    }

    fun playItem(item: AzkarItemEntity) {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            val playable = AzkarPlaybackResolver.resolvePlayable(context, item)
            if (playable == null) {
                showNoAudioAlert = true
                return@launch
            }
            beginPlayback()
            playbackQueueIds = listOf(item.id)
            playingItemId = item.id
            playbackState = TtsPlaybackState.PLAYING
            try {
                playAzkarItemsOrdered(
                    context = context,
                    items = listOf(item),
                    audioPlayer = audioPlayer,
                    settings = settings,
                    isCancelled = { stopPlaybackRequested },
                    onMissingAudio = { showNoAudioAlert = true },
                )
            } finally {
                if (!stopPlaybackRequested) {
                    playbackState = TtsPlaybackState.IDLE
                    playingItemId = null
                    playbackQueueIds = emptyList()
                }
            }
        }
    }

    fun playSelected() {
        if (selectedIds.isEmpty()) return
        playbackJob?.cancel()
        playbackJob = scope.launch {
            val selectedItems = items.filter { it.id in selectedIds }
            val hasAnyAudio = selectedItems.any {
                AzkarPlaybackResolver.resolvePlayable(context, it) != null
            }
            if (!hasAnyAudio) {
                showNoAudioAlert = true
                return@launch
            }
            beginPlayback()
            playbackQueueIds = selectedItems.map { it.id }
            playbackState = TtsPlaybackState.PLAYING
            try {
                playAzkarItemsOrdered(
                    context = context,
                    items = selectedItems,
                    audioPlayer = audioPlayer,
                    settings = settings,
                    isCancelled = { stopPlaybackRequested },
                    onMissingAudio = {},
                    onItemStart = { playingItemId = it.id },
                    shouldSkipCurrent = { skipCurrentRequested },
                    onSkipConsumed = { skipCurrentRequested = false },
                )
            } finally {
                if (!stopPlaybackRequested) {
                    playbackState = TtsPlaybackState.IDLE
                    playingItemId = null
                    playbackQueueIds = emptyList()
                }
            }
        }
    }

    fun persist() {
        onSaveSchedule(
            collection.copy(
                autoPlayEnabled = collection.autoPlayAllowed && autoPlay,
                scheduleHour = hour.toInt(),
                scheduleMinute = minute.toInt(),
                weekDaysMask = when (collection.id) {
                    FridayAzkar.COLLECTION_ID -> FridayAzkar.fridayOnlyMask()
                    BlessedDaysAzkar.COLLECTION_ID -> 127
                    else -> weekMask
                },
                dayMode = when (collection.id) {
                    BlessedDaysAzkar.COLLECTION_ID -> CollectionDayMode.ITEM_HIJRI
                    else -> collection.dayMode
                },
                hijriMonth = collection.hijriMonth,
                hijriDayStart = collection.hijriDayStart,
                hijriDayEnd = collection.hijriDayEnd,
            )
        )
    }

    fun persistAfterPrayer(enabled: Boolean) {
        afterPrayerOn = enabled
        settings.afterPrayerFromSalahEnabled = enabled
        if (enabled) settings.respectPrayerTime = true
        AfterPrayerAlarmScheduler.reschedule(context)
        if (settings.isServiceEnabled) {
            ReminderScheduler.scheduleNext(context)
            AdhkarReminderService.refreshNotification(context)
        }
    }

    fun persistAfterAdhan(enabled: Boolean) {
        afterAdhanOn = enabled
        settings.afterAdhanAzkarEnabled = enabled
    }

    fun persistHomeAzkar(enabled: Boolean) {
        homeAzkarOn = enabled
        settings.homeAzkarEnabled = enabled
        if (enabled) {
            HomeGeofenceScheduler.register(context)
            val needsSetup = !settings.hasHomeLocation ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    !RuntimePermissions.hasBackgroundLocation(context))
            if (needsSetup) onOpenHomeAzkarSettings()
        } else {
            HomeGeofenceScheduler.unregister(context)
        }
    }

    fun persistRidingAzkar(enabled: Boolean) {
        ridingAzkarOn = enabled
        settings.ridingAzkarEnabled = enabled
        if (!enabled) {
            VehicleActivityScheduler.unregister(context)
            return
        }
        val permission = RuntimePermissions.activityRecognitionPermission()
        if (permission != null && !RuntimePermissions.hasActivityRecognition(context)) {
            ridingPermissionLauncher.launch(permission)
        } else {
            VehicleActivityScheduler.register(context)
        }
    }

    @Composable
    fun AzkarPlaybackSection(modifier: Modifier = Modifier) {
        val canSkipToNext = playbackState == TtsPlaybackState.PLAYING &&
            playbackQueueIds.size > 1 &&
            (playingItemId?.let { id ->
                val idx = playbackQueueIds.indexOf(id)
                idx >= 0 && idx < playbackQueueIds.lastIndex
            } ?: true)
        val playingItem = playingItemId?.let { id -> items.find { it.id == id } }
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
                onSkipNext = { skipToNextDhikr() },
                skipEnabled = canSkipToNext,
            )
            if (playbackState == TtsPlaybackState.PLAYING) {
                Text(
                    if (playingItem != null) {
                        stringResource(
                            R.string.azkar_now_playing,
                            azkarNowPlayingPreview(playingItem.localizedText(lang)),
                        )
                    } else {
                        stringResource(R.string.azkar_playing_reciter)
                    },
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
                if (selectedItem!!.hasOwnHijri()) {
                    Text(
                        CollectionScheduleHelper.hijriSummaryAr(selectedItem!!),
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = GreenPrimary
                    )
                }
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
                    val isAfterAdhan = collection.id == AdhanAzkar.COLLECTION_ID
                    val isHome = collection.id == HomeAzkar.COLLECTION_ID
                    val isRiding = collection.id == RidingAzkar.COLLECTION_ID
                    when {
                        isAfterPrayer -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                SettingRow(stringResource(R.string.azkar_auto_enable), afterPrayerOn) { on ->
                                    persistAfterPrayer(on)
                                    if (on && !settings.hasPrayerLocation) onOpenAfterPrayerSettings()
                                }
                                Text(
                                    stringResource(
                                        if (afterPrayerOn) R.string.azkar_after_prayer_auto_on_hint
                                        else R.string.azkar_after_prayer_auto_hint
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                                )
                                SettingsNavCard(
                                    title = stringResource(R.string.azkar_after_prayer_configure_title),
                                    subtitle = stringResource(R.string.azkar_after_prayer_configure_subtitle),
                                    onClick = onOpenAfterPrayerSettings
                                )
                            }
                        }
                        isAfterAdhan -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                SettingRow(stringResource(R.string.azkar_auto_enable), afterAdhanOn) { on ->
                                    persistAfterAdhan(on)
                                    if (on && !settings.hasPrayerLocation) onOpenAdhanSettings()
                                }
                                Text(
                                    stringResource(
                                        if (afterAdhanOn) R.string.azkar_after_adhan_auto_on_hint
                                        else R.string.azkar_after_adhan_auto_hint
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                                )
                            }
                        }
                        isHome -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                SettingRow(stringResource(R.string.azkar_auto_enable), homeAzkarOn) { on ->
                                    persistHomeAzkar(on)
                                }
                                Text(
                                    stringResource(
                                        if (homeAzkarOn) R.string.azkar_home_auto_on_hint
                                        else R.string.azkar_home_auto_hint
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                                )
                                val homeStatus = remember(monitorTick) { settings.homeMonitorStatus }
                                val homeNeedsBg = remember(monitorTick) { settings.homeNeedsBackgroundPermission }
                                val lastEnter = remember(monitorTick) { settings.homeLastEnterAt }
                                val lastExit = remember(monitorTick) { settings.homeLastExitAt }
                                val homePlayErr = remember(monitorTick) { settings.homeLastPlayError }
                                HomeAzkarMonitorStatus(
                                    status = homeStatus,
                                    needsBackground = homeNeedsBg,
                                    lastEnterAt = lastEnter,
                                    lastExitAt = lastExit,
                                    playError = homePlayErr,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                                SettingsNavCard(
                                    title = stringResource(R.string.home_azkar_settings_title),
                                    subtitle = stringResource(R.string.home_azkar_settings_subtitle),
                                    onClick = onOpenHomeAzkarSettings
                                )
                            }
                        }
                        isRiding -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                SettingRow(stringResource(R.string.azkar_auto_enable), ridingAzkarOn) { on ->
                                    persistRidingAzkar(on)
                                }
                                Text(
                                    stringResource(
                                        if (ridingAzkarOn) R.string.azkar_riding_auto_on_hint
                                        else R.string.azkar_riding_auto_hint
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                                )
                                val ridingStatus = remember(monitorTick) { settings.ridingMonitorStatus }
                                val lastRide = remember(monitorTick) { settings.ridingLastPlayAt }
                                val ridePlayErr = remember(monitorTick) { settings.ridingLastPlayError }
                                RidingAzkarMonitorStatus(
                                    status = ridingStatus,
                                    lastPlayAt = lastRide,
                                    playError = ridePlayErr,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            AutoAzkarEventPlayer.playRiding(
                                                context,
                                                ignoreCooldown = true,
                                                recordEvent = false,
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.riding_azkar_test_play))
                                }
                            }
                        }
                        collection.autoPlayAllowed -> {
                            SettingRow(stringResource(R.string.azkar_auto_enable), autoPlay) {
                                autoPlay = it
                                persist()
                            }
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
                                    subtitle = stringResource(
                                        when (collection.id) {
                                            FridayAzkar.COLLECTION_ID -> R.string.azkar_friday_schedule_hint
                                            BlessedDaysAzkar.COLLECTION_ID -> R.string.azkar_blessed_days_schedule_hint
                                            else -> R.string.azkar_schedule_hint
                                        }
                                    )
                                )
                                Text(
                                    stringResource(
                                        when (collection.id) {
                                            FridayAzkar.COLLECTION_ID -> R.string.azkar_friday_kahf_time_label
                                            BlessedDaysAzkar.COLLECTION_ID -> R.string.azkar_blessed_days_time_label
                                            else -> R.string.azkar_time_label
                                        }
                                    ),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                Text(
                                    "${hour.toInt().toString().padStart(2, '0')}:${minute.toInt().toString().padStart(2, '0')}"
                                        .formatLocalizedDigits()
                                )
                                Slider(value = hour, onValueChange = { hour = it; persist() }, valueRange = 0f..23f, steps = 22)
                                Slider(value = minute, onValueChange = { minute = it; persist() }, valueRange = 0f..59f, steps = 58)
                                if (collection.dayMode == CollectionDayMode.HIJRI ||
                                    collection.dayMode == CollectionDayMode.ITEM_HIJRI
                                ) {
                                    val hijriLabel = if (collection.dayMode == CollectionDayMode.ITEM_HIJRI) {
                                        items.map { CollectionScheduleHelper.hijriSummaryAr(it) }
                                            .filter { it.isNotBlank() }
                                            .distinct()
                                            .joinToString("، ")
                                            .ifBlank { stringResource(R.string.schedule_hijri) }
                                    } else {
                                        CollectionScheduleHelper.hijriSummaryAr(collection)
                                            .ifBlank { stringResource(R.string.schedule_hijri) }
                                    }
                                    Text(
                                        stringResource(R.string.azkar_hijri_days_locked, hijriLabel),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                } else if (collection.id != FridayAzkar.COLLECTION_ID) {
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
                    val isPlayingThis = playingItemId == item.id && playbackState != TtsPlaybackState.IDLE
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = when {
                            isPlayingThis || isSearchTarget -> BorderStroke(2.dp, GreenPrimary)
                            else -> null
                        },
                        colors = when {
                            isPlayingThis -> CardDefaults.cardColors(
                                containerColor = GreenPrimary.copy(alpha = 0.18f)
                            )
                            isSearchTarget -> CardDefaults.cardColors(
                                containerColor = GreenPrimary.copy(alpha = 0.08f)
                            )
                            else -> CardDefaults.cardColors()
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
                                    color = if (isPlayingThis) GreenPrimaryDark else listTextStyle.color,
                                )
                                if (isPlayingThis) {
                                    Text(
                                        stringResource(R.string.azkar_playing),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GreenPrimary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                                if (item.hasOwnHijri()) {
                                    Text(
                                        CollectionScheduleHelper.hijriSummaryAr(item),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GreenPrimary,
                                        modifier = Modifier.padding(top = 2.dp)
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

private fun azkarNowPlayingPreview(text: String, maxLength: Int = 48): String {
    val normalized = text.trim().replace(Regex("\\s+"), " ")
    return if (normalized.length <= maxLength) normalized else normalized.take(maxLength) + "…"
}
