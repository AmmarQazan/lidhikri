package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greendome.adhkar.R
import com.greendome.adhkar.audio.TtsPlaybackState
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.data.model.AzkarCardText
import com.greendome.adhkar.ui.components.AzkarFontSizeButtons
import com.greendome.adhkar.ui.components.AzkarTextMenu
import com.greendome.adhkar.ui.theme.AppCardColors
import com.greendome.adhkar.ui.theme.ArabicText
import com.greendome.adhkar.ui.theme.GoldDome
import com.greendome.adhkar.ui.theme.GreenPrimary
import com.greendome.adhkar.ui.theme.GreenPrimaryDark
import com.greendome.adhkar.ui.theme.formatLocalizedDigits
import com.greendome.adhkar.ui.theme.stringResourceDigits
import kotlin.math.min

@Composable
fun AzkarCardReader(
    items: List<AzkarItemEntity>,
    isFavorite: (AzkarItemEntity) -> Boolean,
    onToggleFavorite: (AzkarItemEntity) -> Unit,
    playingItemId: Long?,
    playbackState: TtsPlaybackState,
    onPlayItem: (AzkarItemEntity) -> Unit,
    onStopPlayback: () -> Unit,
    initialItemId: Long? = null,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.azkar_card_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        return
    }

    var currentIndex by remember(items, initialItemId) {
        val idx = initialItemId?.let { id -> items.indexOfFirst { it.id == id } }
            ?.takeIf { it >= 0 } ?: 0
        mutableIntStateOf(idx)
    }
    var counter by remember(currentIndex) { mutableIntStateOf(0) }
    var virtueExpanded by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val settings = remember { SettingsRepository(context) }
    val lang = settings.appLanguage
    var cardFontSp by remember { mutableIntStateOf(settings.azkarCardFontSizeSp) }
    fun changeCardFont(delta: Int) {
        val next = (cardFontSp + delta).coerceIn(
            AzkarCardText.MIN_FONT_SP,
            AzkarCardText.MAX_FONT_SP,
        )
        cardFontSp = next
        settings.azkarCardFontSizeSp = next
    }

    val item = items[currentIndex.coerceIn(0, items.lastIndex)]
    val nextItem = items.getOrNull(currentIndex + 1)
    val target = item.repeatCount.coerceAtLeast(1)
    val progress = if (target == 0) 0f else counter.toFloat() / target
    val isPlayingThis = playingItemId == item.id && playbackState != TtsPlaybackState.IDLE

    fun goNext() {
        if (currentIndex < items.lastIndex) {
            if (isPlayingThis) onStopPlayback()
            currentIndex++
            counter = 0
            virtueExpanded = false
        }
    }

    fun goPrevious() {
        if (currentIndex > 0) {
            if (isPlayingThis) onStopPlayback()
            currentIndex--
            counter = 0
            virtueExpanded = false
        }
    }

    fun incrementCounter() {
        if (counter < target) {
            counter++
            if (counter >= target && currentIndex < items.lastIndex) {
                goNext()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { incrementCounter() }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.azkar_nav_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResourceDigits(R.string.azkar_card_progress, currentIndex + 1, items.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = GreenPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = AppCardColors(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { goPrevious() },
                        enabled = currentIndex > 0,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = if (currentIndex > 0) GreenPrimary else GreenPrimary.copy(alpha = 0.3f),
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (nextItem != null) {
                                    Modifier.clickable { goNext() }
                                } else {
                                    Modifier
                                }
                            )
                            .padding(horizontal = 4.dp),
                    ) {
                        if (nextItem != null) {
                            Text(
                                text = stringResource(
                                    R.string.azkar_card_next_preview,
                                    azkarPreview(nextItem.localizedText(lang)),
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 22.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Text(
                                stringResource(R.string.azkar_card_last_item),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    IconButton(
                        onClick = { goNext() },
                        enabled = nextItem != null,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = if (nextItem != null) GreenPrimary else GreenPrimary.copy(alpha = 0.3f),
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = AppCardColors(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            val textScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showInfoDialog = true },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = stringResource(R.string.azkar_card_info),
                                tint = GreenPrimary.copy(alpha = 0.6f)
                            )
                        }
                        AzkarFontSizeButtons(
                            fontSp = cardFontSp,
                            minSp = AzkarCardText.MIN_FONT_SP,
                            maxSp = AzkarCardText.MAX_FONT_SP,
                            onDecrease = { changeCardFont(-AzkarCardText.STEP_SP) },
                            onIncrease = { changeCardFont(AzkarCardText.STEP_SP) },
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (isPlayingThis) onStopPlayback() else onPlayItem(item) },
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = GreenPrimary,
                            ),
                        ) {
                            Icon(
                                if (isPlayingThis) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = stringResource(
                                    if (isPlayingThis) R.string.azkar_stop else R.string.azkar_play_item
                                ),
                            )
                        }
                        AzkarTextMenu(text = item.localizedText(lang))
                        IconButton(onClick = { onToggleFavorite(item) }) {
                            Icon(
                                imageVector = if (isFavorite(item)) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = stringResource(
                                    if (isFavorite(item)) R.string.azkar_remove_favorite else R.string.azkar_add_favorite
                                ),
                                tint = if (isFavorite(item)) GoldDome else GreenPrimary.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    val displayText = item.localizedText(lang)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(textScroll),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ArabicText(
                            text = displayText,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = cardFontSp.sp,
                                lineHeight = (cardFontSp * AzkarCardText.LINE_HEIGHT_RATIO).sp,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                val virtueText = item.localizedVirtue(lang)
                if (virtueText.isNotBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = GreenPrimary.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(top = 2.dp)
                            )
                            Column(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
                                Text(
                                    if (virtueExpanded) virtueText else virtueText.take(120) +
                                        if (virtueText.length > 120) "…" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                                if (virtueText.length > 120) {
                                    TextButton(
                                        onClick = { virtueExpanded = !virtueExpanded },
                                        modifier = Modifier.padding(top = 0.dp)
                                    ) {
                                        Text(
                                            if (virtueExpanded) {
                                                stringResource(R.string.azkar_card_less)
                                            } else {
                                                stringResource(R.string.azkar_card_more)
                                            },
                                            color = GreenPrimary,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            azkarRepeatLabel(item.repeatCount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            stringResource(R.string.azkar_card_tap_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    CircularCounter(
                        count = counter,
                        target = target,
                        progress = progress,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }

        Button(
            onClick = { goNext() },
            enabled = currentIndex < items.lastIndex,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GreenPrimary,
                disabledContainerColor = GreenPrimary.copy(alpha = 0.4f)
            )
        ) {
            Text(
                stringResource(R.string.azkar_card_next),
                modifier = Modifier.padding(vertical = 4.dp),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }

    if (showInfoDialog && item.virtueAr.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text(stringResource(R.string.azkar_card_virtue_title)) },
            text = { Text(item.virtueAr) },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun CircularCounter(
    count: Int,
    target: Int,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.08f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = GreenPrimary,
                startAngle = -90f,
                sweepAngle = 360f * min(progress, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Text(
                count.formatLocalizedDigits(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun azkarRepeatLabel(count: Int): String = when {
    count <= 1 -> stringResource(R.string.azkar_repeat_once)
    count == 2 -> stringResource(R.string.azkar_repeat_twice)
    else -> stringResourceDigits(R.string.azkar_repeat_n_times, count)
}

private fun azkarPreview(text: String, maxLength: Int = 110): String {
    val normalized = text.trim().replace(Regex("\\s+"), " ")
    return if (normalized.length <= maxLength) normalized else normalized.take(maxLength) + "…"
}
