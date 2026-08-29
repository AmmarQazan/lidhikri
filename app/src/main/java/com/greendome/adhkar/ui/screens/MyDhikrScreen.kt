package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.local.DhikrEntity
import com.greendome.adhkar.data.model.AudioSourceType
import com.greendome.adhkar.data.model.ScheduleType
import com.greendome.adhkar.ui.theme.ArabicText
import com.greendome.adhkar.ui.theme.GoldDome
import com.greendome.adhkar.ui.theme.GreenPrimary
import com.greendome.adhkar.ui.theme.AppCardColors
import com.greendome.adhkar.ui.theme.formatLocalizedDigits
import com.greendome.adhkar.util.DhikrScheduleMatcher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDhikrScreen(
    items: List<DhikrEntity>,
    lang: String,
    playingDhikrId: Long?,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (DhikrEntity) -> Unit,
    onToggle: (DhikrEntity) -> Unit,
    onPlay: (DhikrEntity) -> Unit,
    onStopPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val myDhikr = items.filter { !it.isDefault }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_dhikr_section)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.my_dhikr_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.add_my_dhikr))
                }
            }
            if (myDhikr.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GreenPrimary.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(R.string.my_dhikr_empty),
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            } else {
                items(myDhikr, key = { "mine-${it.id}" }) { dhikr ->
                    val hasAudio = dhikr.audioSourceType != AudioSourceType.NONE
                    val isPlaying = playingDhikrId == dhikr.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEdit(dhikr) },
                        shape = RoundedCornerShape(12.dp),
                        colors = AppCardColors(),
                    ) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = {
                                if (isPlaying) onStopPlay() else onPlay(dhikr)
                            }) {
                                Icon(
                                    if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = stringResource(R.string.play),
                                    tint = GreenPrimary,
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.phone_only_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GreenPrimary,
                                )
                                ArabicText(
                                    text = dhikr.localizedText(lang),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                if (dhikr.scheduleType != ScheduleType.ALWAYS) {
                                    Text(
                                        DhikrScheduleMatcher.scheduleSummaryAr(dhikr).formatLocalizedDigits(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (DhikrScheduleMatcher.isActiveNow(dhikr)) {
                                            GreenPrimary
                                        } else {
                                            GoldDome
                                        },
                                    )
                                }
                                if (!hasAudio) {
                                    Text(
                                        stringResource(R.string.audio_none),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                            Switch(
                                checked = dhikr.isEnabled,
                                onCheckedChange = { onToggle(dhikr.copy(isEnabled = it)) },
                            )
                        }
                    }
                }
            }
        }
    }
}
