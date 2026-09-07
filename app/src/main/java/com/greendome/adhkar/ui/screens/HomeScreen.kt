package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.data.model.ClockHourFormat
import com.greendome.adhkar.prayer.DailyPrayerTimes
import com.greendome.adhkar.prayer.PRAYER_TIMES_CARD_IMSAK
import com.greendome.adhkar.prayer.PRAYER_TIMES_CARD_SUNRISE
import com.greendome.adhkar.prayer.PrayerConfig
import com.greendome.adhkar.prayer.PrayerInstant
import com.greendome.adhkar.prayer.PrayerName
import com.greendome.adhkar.prayer.PrayerTimesCalculator
import com.greendome.adhkar.prayer.prayerTimesCardHighlightIndex
import com.greendome.adhkar.ui.theme.AppCardColors
import com.greendome.adhkar.ui.theme.AppMutedTextColor
import com.greendome.adhkar.ui.theme.GreenPrimary
import com.greendome.adhkar.ui.theme.brandLogoRes
import com.greendome.adhkar.ui.theme.LocalNumberDigitStyle
import com.greendome.adhkar.ui.theme.formatLocalizedDigits
import com.greendome.adhkar.ui.theme.stringResourceDigits
import com.greendome.adhkar.util.HomeLayout
import com.greendome.adhkar.util.HomeSection
import com.greendome.adhkar.util.NextAzkarSchedule
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.ZoneId

@Composable
fun HomeScreen(
    isServiceOn: Boolean,
    todayTasbihCount: Int,
    todayAzkarCount: Int,
    todayMisbahaCount: Int,
    minutesUntilNext: Int,
    isPrayerQuiet: Boolean,
    autoAzkarEnabled: Boolean,
    azkarCollections: List<AdhkarCollectionEntity>,
    azkarItems: List<AzkarItemEntity> = emptyList(),
    prayerConfig: PrayerConfig,
    clockHourFormat: ClockHourFormat,
    appLang: String,
    onToggleService: () -> Unit,
    onToggleAutoAzkar: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onOpenPrayerSettings: () -> Unit = {},
    onOpenQibla: () -> Unit = {},
    homeSectionOrder: List<String> = emptyList(),
    homeHiddenSections: Set<String> = HomeSection.DEFAULT_HIDDEN,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    val times = remember(prayerConfig, now / 30_000L) {
        if (prayerConfig.hasTimes) PrayerTimesCalculator.timesFor(prayerConfig, now) else null
    }
    val zone = remember(prayerConfig) { PrayerTimesCalculator.zoneId(prayerConfig) }
    val am = stringResource(R.string.clock_period_am)
    val pm = stringResource(R.string.clock_period_pm)
    val next = remember(prayerConfig, now / 30_000L) {
        if (prayerConfig.hasTimes) PrayerTimesCalculator.nextPrayer(prayerConfig, now) else null
    }
    val remaining = next?.let { (it.epochMillis - now).coerceAtLeast(0L) }
    val visible = remember(homeSectionOrder, homeHiddenSections) {
        HomeLayout.merge(homeSectionOrder).filter {
            it !in homeHiddenSections &&
                it !in HomeSection.NOT_CUSTOMIZABLE
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(brandLogoRes()),
                contentDescription = stringResource(R.string.app_name),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(168.dp)
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            visible.forEach { id ->
                if (id == HomeSection.PRAYER_TIMES.id && times == null) return@forEach
                item(key = id) {
                    val section = HomeSection.fromId(id) ?: return@item
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        when (section) {
                            HomeSection.NEXT_PRAYER -> HomeNextPrayerCard(
                                prayerConfig = prayerConfig,
                                next = next,
                                remaining = remaining,
                                zone = zone,
                                clockHourFormat = clockHourFormat,
                                am = am,
                                pm = pm,
                                isServiceOn = isServiceOn,
                                isPrayerQuiet = isPrayerQuiet,
                                minutesUntilNext = minutesUntilNext,
                                autoAzkarEnabled = autoAzkarEnabled,
                                azkarCollections = azkarCollections,
                                azkarItems = azkarItems,
                                appLang = appLang,
                                onOpenPrayerSettings = onOpenPrayerSettings,
                                onOpenQibla = onOpenQibla,
                                onToggleService = onToggleService,
                                onToggleAutoAzkar = onToggleAutoAzkar,
                            )
                            HomeSection.PRAYER_TIMES -> if (times != null) {
                                HomePrayerTimesCard(
                                    times = times,
                                    next = next,
                                    zone = zone,
                                    clockHourFormat = clockHourFormat,
                                    am = am,
                                    pm = pm,
                                )
                            }
                            HomeSection.QIBLA,
                            HomeSection.AUTO_TASBIH,
                            HomeSection.AUTO_AZKAR -> Unit
                            HomeSection.TODAY_STATS -> HomeTodayStatsCard(
                                todayTasbihCount = todayTasbihCount,
                                todayAzkarCount = todayAzkarCount,
                                todayMisbahaCount = todayMisbahaCount,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeNextPrayerCard(
    prayerConfig: PrayerConfig,
    next: PrayerInstant?,
    remaining: Long?,
    zone: ZoneId,
    clockHourFormat: ClockHourFormat,
    am: String,
    pm: String,
    isServiceOn: Boolean,
    isPrayerQuiet: Boolean,
    minutesUntilNext: Int,
    autoAzkarEnabled: Boolean,
    azkarCollections: List<AdhkarCollectionEntity>,
    azkarItems: List<AzkarItemEntity>,
    appLang: String,
    onOpenPrayerSettings: () -> Unit,
    onOpenQibla: () -> Unit,
    onToggleService: () -> Unit,
    onToggleAutoAzkar: (Boolean) -> Unit,
) {
    val qiblaAlign = if (LocalLayoutDirection.current == LayoutDirection.Rtl) {
        Alignment.TopEnd
    } else {
        Alignment.TopStart
    }
    Card(
        colors = AppCardColors(),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Box(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 44.dp)
                        .clickable(onClick = onOpenPrayerSettings),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val city = prayerConfig.location?.cityName
                    Text(
                        city?.ifBlank { stringResource(R.string.prayer_times_need_city) }
                            ?: stringResource(R.string.prayer_times_need_city),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                    if (next == null || remaining == null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.prayer_home_choose_city),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy((-10).dp)
                        ) {
                            Text(
                                stringResource(R.string.prayer_next_is, prayerLabel(next.prayer)),
                                style = MaterialTheme.typography.titleSmall,
                                color = GreenPrimary,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 14.sp
                            )
                            Text(
                                formatPrayerInstant(next.epochMillis, zone, clockHourFormat, am, pm)
                                    .formatLocalizedDigits(),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 32.sp
                            )
                            Text(
                                formatCountdown(remaining),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onOpenQibla,
                    modifier = Modifier
                        .align(qiblaAlign)
                        .size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Explore,
                        contentDescription = stringResource(R.string.qibla_title),
                        tint = GreenPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            HomeReminderStatusRow(
                status = when {
                    !isServiceOn -> stringResource(R.string.auto_tasbih_off)
                    isPrayerQuiet -> stringResource(R.string.auto_tasbih_paused_prayer)
                    else -> stringResource(R.string.auto_tasbih_on)
                },
                statusOn = isServiceOn && !isPrayerQuiet,
                checked = isServiceOn,
                onCheckedChange = { onToggleService() },
            ) {
                if (isServiceOn) {
                    Text(
                        stringResourceDigits(R.string.next_reminder, minutesUntilNext),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            HomeReminderStatusRow(
                status = if (autoAzkarEnabled) stringResource(R.string.auto_azkar_on)
                else stringResource(R.string.auto_azkar_off),
                statusOn = autoAzkarEnabled,
                checked = autoAzkarEnabled,
                onCheckedChange = onToggleAutoAzkar,
            ) {
                if (autoAzkarEnabled) {
                    Column(Modifier.padding(top = 4.dp)) {
                        NextAzkarScheduleLine(
                            collections = azkarCollections,
                            items = azkarItems,
                            prayerConfig = prayerConfig,
                            clockHourFormat = clockHourFormat,
                            appLang = appLang,
                            refreshKey = minutesUntilNext
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeReminderStatusRow(
    status: String,
    statusOn: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    extra: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                status,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (statusOn) GreenPrimary else AppMutedTextColor()
            )
            extra()
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun HomePrayerTimesCard(
    times: DailyPrayerTimes,
    next: PrayerInstant?,
    zone: ZoneId,
    clockHourFormat: ClockHourFormat,
    am: String,
    pm: String,
) {
    val chips = prayerTimesCardChips(times, next, zone, clockHourFormat, am, pm)
    val highlightIndex = prayerTimesCardHighlightIndex(next?.prayer)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = highlightIndex.coerceAtLeast(0)
    )
    var centered by remember { mutableStateOf(false) }
    Card(
        colors = AppCardColors(),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (centered) 1f else 0f)
        ) {
            val viewportPx = constraints.maxWidth
            LaunchedEffect(highlightIndex, viewportPx) {
                if (viewportPx <= 0) {
                    centered = true
                    return@LaunchedEffect
                }
                if (highlightIndex < 0) {
                    centered = true
                    return@LaunchedEffect
                }
                listState.centerItem(highlightIndex, animate = centered)
                centered = true
            }
            LazyRow(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = maxWidth / 2),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(chips, key = { it.key }) { chip ->
                    TimeChip(
                        label = chip.label,
                        time = chip.time,
                        highlight = chip.highlight
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTodayStatsCard(
    todayTasbihCount: Int,
    todayAzkarCount: Int,
    todayMisbahaCount: Int,
) {
    Card(
        colors = AppCardColors(),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            TodayStatLine(
                label = stringResource(R.string.today_stats_tasbih_title),
                count = todayTasbihCount,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.padding(horizontal = 6.dp))
            TodayStatLine(
                label = stringResource(R.string.today_stats_azkar_title),
                count = todayAzkarCount,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.padding(horizontal = 6.dp))
            TodayStatLine(
                label = stringResource(R.string.today_stats_misbaha_title),
                count = todayMisbahaCount,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TimeChip(label: String, time: String?, highlight: Boolean) {
    Column(
        modifier = Modifier.padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (highlight) GreenPrimary else AppMutedTextColor(),
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium
        )
        Text(
            (time ?: "--:--").formatLocalizedDigits(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (highlight) GreenPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun formatCountdown(remainingMs: Long): String {
    val totalMin = (remainingMs / 60_000L).toInt()
    val hours = totalMin / 60
    val minutes = totalMin % 60
    val seconds = ((remainingMs / 1000L) % 60).toInt()
    return if (hours > 0) {
        stringResourceDigits(R.string.prayer_countdown_hms, hours, minutes, seconds)
    } else {
        stringResourceDigits(R.string.prayer_countdown_ms, minutes, seconds)
    }
}

@Composable
private fun TodayStatLine(
    label: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            count.formatLocalizedDigits(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = GreenPrimary,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        )
    }
}

@Composable
private fun NextAzkarScheduleLine(
    collections: List<AdhkarCollectionEntity>,
    items: List<AzkarItemEntity>,
    prayerConfig: PrayerConfig,
    clockHourFormat: ClockHourFormat,
    appLang: String,
    refreshKey: Int
) {
    val context = LocalContext.current
    val digits = LocalNumberDigitStyle.current
    val text = remember(collections, items, prayerConfig, clockHourFormat, appLang, digits, refreshKey) {
        val next = NextAzkarSchedule.resolve(collections, items, prayerConfig)
        if (next == null) {
            context.getString(R.string.next_azkar_none)
        } else {
            NextAzkarSchedule.formatLine(
                context = context,
                collection = next.first,
                triggerAt = next.second,
                lang = appLang,
                clockHourFormat = clockHourFormat,
                numberDigitStyle = digits,
            )
        }
    }
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium
    )
}

private data class PrayerTimeChipUi(
    val key: String,
    val label: String,
    val time: String?,
    val highlight: Boolean,
)

@Composable
private fun prayerTimesCardChips(
    times: DailyPrayerTimes,
    next: PrayerInstant?,
    zone: ZoneId,
    clockHourFormat: ClockHourFormat,
    am: String,
    pm: String,
): List<PrayerTimeChipUi> {
    fun clock(millis: Long?) = millis?.let {
        formatPrayerInstant(it, zone, clockHourFormat, am, pm)
    }
    return buildList {
        add(
            PrayerTimeChipUi(
                key = PRAYER_TIMES_CARD_IMSAK,
                label = stringResource(R.string.prayer_name_imsak),
                time = clock(times.imsakMillis),
                highlight = false,
            )
        )
        add(
            PrayerTimeChipUi(
                key = PrayerName.FAJR.name,
                label = stringResource(R.string.prayer_name_fajr),
                time = clock(times.timeOf(PrayerName.FAJR)),
                highlight = next?.prayer == PrayerName.FAJR,
            )
        )
        add(
            PrayerTimeChipUi(
                key = PRAYER_TIMES_CARD_SUNRISE,
                label = stringResource(R.string.prayer_name_sunrise),
                time = clock(times.sunriseMillis),
                highlight = false,
            )
        )
        PrayerName.entries.filter { it != PrayerName.FAJR }.forEach { prayer ->
            add(
                PrayerTimeChipUi(
                    key = prayer.name,
                    label = prayerLabel(prayer),
                    time = clock(times.timeOf(prayer)),
                    highlight = next?.prayer == prayer,
                )
            )
        }
    }
}

private suspend fun LazyListState.centerItem(index: Int, animate: Boolean) {
    fun offsetToCenter(): Int? {
        val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return null
        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
        val itemCenter = item.offset + item.size / 2
        return itemCenter - viewportCenter
    }
    var offset = offsetToCenter()
    if (offset == null) {
        scrollToItem(index)
        offset = withTimeoutOrNull(500) {
            snapshotFlow { offsetToCenter() }.filterNotNull().first()
        }
    }
    val delta = offset ?: return
    if (delta == 0) return
    if (animate) animateScrollBy(delta.toFloat()) else scrollBy(delta.toFloat())
}
