package com.greendome.adhkar.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BlurCircular
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.audio.DhikrAudioPlayer
import com.greendome.adhkar.audio.DhikrPlaybackResolver
import com.greendome.adhkar.audio.playResolved
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.DhikrEntity
import com.greendome.adhkar.data.model.MisbahaBeadTheme
import com.greendome.adhkar.data.model.MisbahaStyle
import com.greendome.adhkar.util.MisbahaFeedback
import com.greendome.adhkar.ui.components.AudioUnavailableDialog
import com.greendome.adhkar.ui.theme.AppAccentGreen
import com.greendome.adhkar.ui.theme.AppCardColors
import com.greendome.adhkar.ui.theme.formatLocalizedDigits
import com.greendome.adhkar.ui.theme.stringResourceDigits
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val TARGET_OPTIONS = listOf(33, 99, 100)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MisbahaScreen(
    items: List<DhikrEntity>,
    lang: String,
    activityData: Map<String, Int>,
    onTasbihCounted: () -> Unit,
    onOpenActivityLog: () -> Unit,
    onActivityPeriodChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dhikrList = items.ifEmpty { emptyList() }
    val context = LocalContext.current
    val view = LocalView.current
    val settings = remember { SettingsRepository(context) }
    val audioPlayer = remember { DhikrAudioPlayer(context) }
    var selectedIndex by remember(dhikrList) { mutableIntStateOf(0) }
    val selected = dhikrList.getOrNull(selectedIndex)
    var counter by remember { mutableIntStateOf(0) }
    var target by remember { mutableIntStateOf(33) }
    var repeatEnabled by remember { mutableStateOf(settings.misbahaRepeatEnabled) }
    var misbahaStyle by remember { mutableStateOf(settings.misbahaStyle) }
    val beadScale = settings.misbahaBeadScale
    val beadTheme = settings.misbahaBeadTheme
    val electronicTheme = settings.misbahaElectronicTheme
    var isPlaying by remember { mutableStateOf(false) }
    var showNoAudioAlert by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    var showActivityLog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, label = "misbahaPulse")
    val remaining = (target - counter).coerceAtLeast(0)

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.stop()
            isPlaying = false
            MisbahaFeedback.release()
        }
    }

    fun stopPlayback() {
        audioPlayer.stop()
        isPlaying = false
    }

    fun countFromPlayback() {
        counter++
        onTasbihCounted()
        pressed = true
        scope.launch {
            delay(120)
            pressed = false
        }
    }

    fun togglePlayback() {
        val dhikr = selected ?: return
        if (isPlaying) {
            stopPlayback()
            return
        }
        scope.launch {
            val playable = DhikrPlaybackResolver.resolvePlayable(context, dhikr)
            if (playable == null) {
                showNoAudioAlert = true
                return@launch
            }
            isPlaying = true
            if (repeatEnabled) {
                if (counter >= target) counter = 0
                val toPlay = (target - counter).coerceAtLeast(1)
                val playables = List(toPlay) { playable }
                audioPlayer.playSequence(
                    items = playables,
                    settings = settings,
                    onItemStart = { countFromPlayback() },
                ) {
                    isPlaying = false
                }
            } else {
                audioPlayer.playResolved(playable, settings) {
                    isPlaying = false
                }
            }
        }
    }

    fun tapBead() {
        if (!repeatEnabled && counter >= target) return
        pressed = true
        counter++
        onTasbihCounted()
        MisbahaFeedback.perform(context, settings.misbahaFeedbackMode, view)
        if (repeatEnabled && counter >= target) counter = 0
        scope.launch {
            delay(120)
            pressed = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { tapBead() }
            )
    ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ScreenTitle(
                        title = stringResource(R.string.nav_misbaha),
                        subtitle = stringResource(R.string.misbaha_hint),
                    )
                }
                IconButton(
                    onClick = {
                        onOpenActivityLog()
                        showActivityLog = true
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = stringResource(R.string.activity_log_title),
                        tint = AppAccentGreen(),
                    )
                }
            }
        }

        if (dhikrList.isNotEmpty()) {
            item {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(dhikrList.size) { index ->
                        val dhikr = dhikrList[index]
                        FilterChip(
                            selected = index == selectedIndex,
                            onClick = {
                                selectedIndex = index
                                counter = 0
                                stopPlayback()
                            },
                            label = {
                                Text(
                                    dhikr.localizedText(lang).take(28),
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        )
                    }
                }
            }
        }

        item {
        Card(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = AppCardColors(),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(
                        if (selected == null) {
                            R.string.misbaha_pick_dhikr
                        } else {
                            R.string.misbaha_tap_hint
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppAccentGreen(),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                FieldLabel(
                    stringResource(R.string.misbaha_style_section),
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
                ) {
                    FilterChip(
                        selected = misbahaStyle == MisbahaStyle.TRADITIONAL,
                        onClick = {
                            misbahaStyle = MisbahaStyle.TRADITIONAL
                            settings.misbahaStyle = MisbahaStyle.TRADITIONAL
                        },
                        label = {
                            Text(
                                stringResource(R.string.misbaha_style_traditional),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    FilterChip(
                        selected = misbahaStyle == MisbahaStyle.ELECTRONIC,
                        onClick = {
                            misbahaStyle = MisbahaStyle.ELECTRONIC
                            settings.misbahaStyle = MisbahaStyle.ELECTRONIC
                        },
                        label = {
                            Text(
                                stringResource(R.string.misbaha_style_electronic),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.BlurCircular,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }

                FieldLabel(
                    stringResource(R.string.misbaha_target_label),
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
                ) {
                    TARGET_OPTIONS.forEach { option ->
                        FilterChip(
                            selected = target == option,
                            onClick = {
                                target = option
                                counter = 0
                                stopPlayback()
                            },
                            label = {
                                Text(
                                    if (target == option) {
                                        stringResourceDigits(
                                            R.string.misbaha_progress,
                                            counter,
                                            option,
                                        )
                                    } else {
                                        stringResourceDigits(R.string.misbaha_target_value, option)
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MisbahaIconChip(
                            selected = repeatEnabled,
                            icon = Icons.Default.Repeat,
                            label = stringResource(R.string.misbaha_repeat),
                            onClick = {
                                repeatEnabled = !repeatEnabled
                                settings.misbahaRepeatEnabled = repeatEnabled
                                stopPlayback()
                            },
                        )
                        MisbahaIconChip(
                            selected = true,
                            icon = Icons.Default.Refresh,
                            label = stringResource(R.string.misbaha_reset),
                            onClick = {
                                counter = 0
                                stopPlayback()
                            },
                        )
                    }
                }
                Text(
                    stringResource(R.string.misbaha_repeat_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppAccentGreen(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (misbahaStyle == MisbahaStyle.TRADITIONAL) 300.dp else 250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (misbahaStyle) {
                        MisbahaStyle.ELECTRONIC -> ElectronicMisbahaCounter(
                            counter = counter,
                            target = target,
                            remaining = remaining,
                            scale = scale,
                            beadTheme = electronicTheme,
                            isPlaying = isPlaying,
                            onTogglePlayback = { togglePlayback() },
                        )
                        MisbahaStyle.TRADITIONAL -> TraditionalMisbahaCounter(
                            counter = counter,
                            target = target,
                            remaining = remaining,
                            beadScale = beadScale,
                            beadTheme = beadTheme,
                            isPlaying = isPlaying,
                            onTogglePlayback = { togglePlayback() },
                        )
                    }
                }
            }
        }
        }
    }
    }

    if (showActivityLog) {
        TasbihActivityLogSheet(
            activityData = activityData,
            onDismiss = { showActivityLog = false },
            onPeriodChange = onActivityPeriodChange,
        )
    }

    if (showNoAudioAlert) {
        AudioUnavailableDialog(onDismiss = { showNoAudioAlert = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MisbahaIconChip(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
    ) {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(18.dp)
                )
            },
        )
    }
}

@Composable
private fun ElectronicMisbahaCounter(
    counter: Int,
    target: Int,
    remaining: Int,
    scale: Float,
    beadTheme: MisbahaBeadTheme,
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
) {
    val palette = beadTheme.palette()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            BeadRing(count = counter, target = target, beadTheme = beadTheme)

            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color.White.copy(alpha = 0.92f),
                                palette.centerHighlight,
                                palette.centerShadow,
                                palette.activeHighlight.copy(alpha = 0.5f),
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = counter.formatLocalizedDigits(),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.activeShadow,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResourceDigits(
                                R.string.misbaha_counter_of,
                                target,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.activeHighlight,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            MisbahaPlayButton(
                isPlaying = isPlaying,
                onTogglePlayback = onTogglePlayback,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
        Text(
            text = stringResourceDigits(R.string.misbaha_remaining, remaining),
            style = MaterialTheme.typography.labelMedium,
            color = AppAccentGreen(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TraditionalMisbahaCounter(
    counter: Int,
    target: Int,
    remaining: Int,
    beadScale: Float,
    beadTheme: MisbahaBeadTheme,
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            MisbahaPlayButton(
                isPlaying = isPlaying,
                onTogglePlayback = onTogglePlayback,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(AppAccentGreen().copy(alpha = 0.16f))
                .padding(horizontal = 28.dp, vertical = 10.dp)
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = counter.formatLocalizedDigits(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppAccentGreen(),
                    textAlign = TextAlign.Center
                )
            }
        }

        Text(
            text = stringResourceDigits(
                R.string.misbaha_progress,
                counter,
                target,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = AppAccentGreen(),
            modifier = Modifier.padding(top = 6.dp),
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResourceDigits(R.string.misbaha_remaining, remaining),
            style = MaterialTheme.typography.labelSmall,
            color = AppAccentGreen(),
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            textAlign = TextAlign.Center
        )

        TraditionalBeadString(
            count = counter,
            target = target,
            beadScale = beadScale,
            beadTheme = beadTheme,
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
        )
    }
}

@Composable
private fun MisbahaPlayButton(
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onTogglePlayback,
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = stringResource(
                if (isPlaying) R.string.stop else R.string.misbaha_play_dhikr
            ),
            tint = AppAccentGreen(),
        )
    }
}

@Composable
private fun TraditionalBeadString(
    count: Int,
    target: Int,
    beadScale: Float,
    beadTheme: MisbahaBeadTheme,
    modifier: Modifier = Modifier,
) {
    val beadCount = 11
    val progressIndex = if (target == 0 || count == 0) {
        -1
    } else {
        ((count - 1) % beadCount).coerceAtLeast(0)
    }
    val sizeScale = beadScale.coerceIn(0.7f, 1.6f)
    val palette = beadTheme.palette()

    Canvas(modifier = modifier) {
        val centerIndex = beadCount / 2
        val beadPositions = List(beadCount) { index ->
            val t = index / (beadCount - 1).toFloat()
            val x = size.width * 0.08f + size.width * 0.84f * t
            val y = size.height * 0.84f - sin(t * PI).toFloat() * size.height * 0.48f
            Offset(x, y)
        }

        val stringPath = Path().apply {
            moveTo(beadPositions.first().x, beadPositions.first().y)
            beadPositions.drop(1).forEach { point ->
                lineTo(point.x, point.y)
            }
        }
        drawPath(
            path = stringPath,
            color = palette.stringColor,
            style = Stroke(width = size.minDimension * 0.022f * sizeScale, cap = StrokeCap.Round)
        )

        beadPositions.forEachIndexed { index, beadCenter ->
            val isCenter = index == centerIndex
            val isActive = index <= progressIndex
            val beadRadius = size.minDimension * if (isCenter) 0.095f else 0.080f
            val scaledRadius = beadRadius * sizeScale
            val highlight = if (isCenter) palette.centerHighlight else palette.activeHighlight
            val shadow = if (isCenter) palette.centerShadow else palette.activeShadow

            drawCircle(
                brush = Brush.radialGradient(
                    colors = if (isActive) {
                        listOf(highlight, shadow)
                    } else {
                        listOf(palette.inactiveLight, palette.inactiveDark)
                    },
                    center = beadCenter - Offset(scaledRadius * 0.25f, scaledRadius * 0.25f),
                    radius = scaledRadius * 1.6f
                ),
                radius = scaledRadius,
                center = beadCenter
            )
            drawCircle(
                color = Color.White.copy(alpha = if (isActive) 0.35f else 0.18f),
                radius = scaledRadius * 0.28f,
                center = beadCenter - Offset(scaledRadius * 0.28f, scaledRadius * 0.32f)
            )
        }
    }
}

@Composable
private fun BeadRing(
    count: Int,
    target: Int,
    beadTheme: MisbahaBeadTheme,
) {
    val beadCount = 33
    val progress = if (target == 0) 0f else (count % target).toFloat() / target
    val filledBeads = (progress * beadCount).toInt().coerceIn(0, beadCount)
    val palette = beadTheme.palette()

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.42f
        val beadRadius = size.minDimension * 0.028f

        for (index in 0 until beadCount) {
            val angle = (2 * PI * index / beadCount) - (PI / 2)
            val beadCenter = Offset(
                center.x + radius * cos(angle).toFloat(),
                center.y + radius * sin(angle).toFloat()
            )
            val active = index < filledBeads
            drawCircle(
                brush = if (active) {
                    Brush.radialGradient(
                        colors = listOf(palette.activeHighlight, palette.activeShadow),
                        center = beadCenter,
                        radius = beadRadius * 1.5f
                    )
                } else {
                    Brush.radialGradient(
                        colors = listOf(palette.inactiveLight, palette.inactiveDark),
                        center = beadCenter,
                        radius = beadRadius * 1.5f
                    )
                },
                radius = beadRadius,
                center = beadCenter
            )
        }
    }
}
