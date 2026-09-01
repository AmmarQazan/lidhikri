package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.greendome.adhkar.R
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.ui.theme.AppCardColors
import com.greendome.adhkar.ui.theme.GreenPrimary
import com.greendome.adhkar.util.HomeLayout
import com.greendome.adhkar.util.HomeSection
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun HomeLayoutSettingsScreen(
    settings: SettingsRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val merged = remember(settings.homeSectionOrder) {
        HomeLayout.customizable(settings.homeSectionOrder)
    }
    val orderedIds = remember { mutableStateListOf<String>() }
    LaunchedEffect(merged) {
        if (orderedIds.toList() != merged) {
            orderedIds.clear()
            orderedIds.addAll(merged)
        }
    }
    var hiddenIds by remember { mutableStateOf(settings.homeHiddenSections) }
    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        val next = HomeLayout.moved(orderedIds.toList(), from.index, to.index)
        orderedIds.clear()
        orderedIds.addAll(next)
        settings.homeSectionOrder = next
    }
    val dragHandleDesc = stringResource(R.string.azkar_hub_drag_handle)

    SettingsSubScreenScaffold(
        title = stringResource(R.string.settings_home_layout_title),
        onBack = onBack,
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.settings_home_layout_hint),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                stringResource(R.string.azkar_hub_reorder_hint),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp),
                style = MaterialTheme.typography.labelSmall,
                color = GreenPrimary
            )
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            ) {
                items(orderedIds, key = { it }) { id ->
                    val section = HomeSection.fromId(id) ?: return@items
                    ReorderableItem(reorderableState, key = id) { isDragging ->
                        val visible = id !in hiddenIds
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (isDragging) 1f else 0f)
                                .shadow(if (isDragging) 8.dp else 0.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = AppCardColors(),
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                ) {
                                    Text(
                                        homeSectionTitle(section),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    homeSectionSubtitle(section)?.let { subtitle ->
                                        Text(
                                            subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = visible,
                                    onCheckedChange = { checked ->
                                        hiddenIds = if (checked) hiddenIds - id else hiddenIds + id
                                        settings.homeHiddenSections = hiddenIds
                                    }
                                )
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
    }
}

@Composable
fun homeSectionTitle(section: HomeSection): String = stringResource(
    when (section) {
        HomeSection.NEXT_PRAYER -> R.string.home_section_next_prayer
        HomeSection.PRAYER_TIMES -> R.string.home_section_prayer_times
        HomeSection.QIBLA -> R.string.home_section_qibla
        HomeSection.AUTO_TASBIH -> R.string.home_section_auto_tasbih
        HomeSection.AUTO_AZKAR -> R.string.home_section_auto_azkar
        HomeSection.TODAY_STATS -> R.string.home_section_today_stats
    }
)

@Composable
fun homeSectionSubtitle(section: HomeSection): String? = when (section) {
    HomeSection.NEXT_PRAYER -> stringResource(R.string.home_section_next_prayer_hint)
    else -> null
}
