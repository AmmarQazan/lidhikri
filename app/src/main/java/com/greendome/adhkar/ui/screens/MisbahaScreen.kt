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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.sp
import com.greendome.adhkar.R
import com.greendome.adhkar.audio.DhikrAudioPlayer
import com.greendome.adhkar.audio.DhikrPlaybackResolver
import com.greendome.adhkar.audio.playResolved
import com.greendome.adhkar.audio.playSequence
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.DhikrEntity
import com.greendome.adhkar.data.model.MisbahaStyle
import com.greendome.adhkar.util.MisbahaFeedback
import com.greendome.adhkar.ui.theme.formatLocalizedDigits
import com.greendome.adhkar.ui.theme.stringResourceDigits
import com.greendome.adhkar.ui.theme.GoldDome
import com.greendome.adhkar.ui.theme.GoldLight
import com.greendome.adhkar.ui.theme.GreenPrimary
import com.greendome.adhkar.ui.theme.GreenPrimaryDark
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
    var isPlaying by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    var showActivityLog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, label = "misbahaPulse")

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

    fun togglePlayback() {
        val dhikr = selected ?: return
        if (isPlaying) {
            stopPlayback()
            return
        }
        scope.launch {
            val playable = DhikrPlaybackResolver.resolvePlayable(context, dhikr) ?: return@launch
            isPlaying = true
            if (repeatEnabled) {
                val playables = List(target.coerceAtLeast(1)) { playable }
                audioPlayer.playSequence(playables, settings) {
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
                        tint = GreenPrimaryDark,
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = selected?.localizedText(lang) ?: stringResource(R.string.misbaha_pick_dhikr),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = GreenPrimaryDark,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 28.sp
                )

                Spacer(Modifier.height(12.dp))

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
                                    stringResourceDigits(R.string.misbaha_target_value, option),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        )
                    }
                    FilterChip(
                        selected = repeatEnabled,
                        onClick = {
                            repeatEnabled = !repeatEnabled
                            settings.misbahaRepeatEnabled = repeatEnabled
                            stopPlayback()
                        },
                        label = {
                            Text(
                                stringResource(R.string.misbaha_repeat),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
                Text(
                    stringResource(R.string.misbaha_repeat_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = GreenPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (misbahaStyle == MisbahaStyle.TRADITIONAL) 280.dp else 220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (misbahaStyle) {
                        MisbahaStyle.ELECTRONIC -> ElectronicMisbahaCounter(
                            counter = counter,
                            target = target,
                            scale = scale,
                            isPlaying = isPlaying,
                            onTogglePlayback = { togglePlayback() },
                        )
                        MisbahaStyle.TRADITIONAL -> TraditionalMisbahaCounter(
                            counter = counter,
                            target = target,
                            isPlaying = isPlaying,
                            onTogglePlayback = { togglePlayback() },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(
                        if (misbahaStyle == MisbahaStyle.TRADITIONAL) {
                            R.string.misbaha_tap_hint_traditional
                        } else {
                            R.string.misbaha_tap_hint_electronic
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = GreenPrimaryDark,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = {
                        counter = 0
                        stopPlayback()
                    }) {
                        Text(
                            stringResource(R.string.misbaha_reset),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    OutlinedButton(onClick = { tapBead() }) {
                        Text(
                            stringResourceDigits(R.string.misbaha_plus_one, 1),
                            style = MaterialTheme.typography.bodyMedium
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
}

@Composable
private fun ElectronicMisbahaCounter(
    counter: Int,
    target: Int,
    scale: Float,
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
) {
    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        BeadRing(count = counter, target = target)

        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color.White.copy(alpha = 0.9f),
                            GoldLight,
                            GoldDome,
                            GreenPrimary.copy(alpha = 0.45f)
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
                        color = GreenPrimaryDark,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResourceDigits(
                            R.string.misbaha_counter_of,
                            target,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = GreenPrimary,
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
}

@Composable
private fun TraditionalMisbahaCounter(
    counter: Int,
    target: Int,
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
                .background(GreenPrimary.copy(alpha = 0.08f))
                .padding(horizontal = 28.dp, vertical = 10.dp)
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = counter.formatLocalizedDigits(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimaryDark,
                    textAlign = TextAlign.Center
                )
            }
        }

        Text(
            text = stringResourceDigits(
                R.string.misbaha_counter_of,
                target,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = GreenPrimary,
            modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
            textAlign = TextAlign.Center
        )

        TraditionalBeadString(
            count = counter,
            target = target,
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
            .background(Color.White.copy(alpha = 0.92f))
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = stringResource(
                if (isPlaying) R.string.stop else R.string.misbaha_play_dhikr
            ),
            tint = GreenPrimaryDark,
        )
    }
}

@Composable
private fun TraditionalBeadString(
    count: Int,
    target: Int,
    modifier: Modifier = Modifier,
) {
    val beadCount = 11
    val progressIndex = if (target == 0 || count == 0) {
        -1
    } else {
        ((count - 1) % beadCount).coerceAtLeast(0)
    }

    Canvas(modifier = modifier) {
        val centerIndex = beadCount / 2
        val stringColor = Color(0xFF4A90C2)
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
            color = stringColor,
            style = Stroke(width = size.minDimension * 0.022f, cap = StrokeCap.Round)
        )

        beadPositions.forEachIndexed { index, beadCenter ->
            val isCenter = index == centerIndex
            val isActive = index <= progressIndex
            val beadRadius = size.minDimension * if (isCenter) 0.095f else 0.080f
            val highlight = if (isCenter) GoldLight else Color(0xFF8FD4B8)
            val shadow = if (isCenter) GoldDome else GreenPrimaryDark
            val inactiveLight = Color(0xFFD8ECE4)
            val inactiveDark = Color(0xFF9BB8AD)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = if (isActive) {
                        listOf(highlight, shadow)
                    } else {
                        listOf(inactiveLight, inactiveDark)
                    },
                    center = beadCenter - Offset(beadRadius * 0.25f, beadRadius * 0.25f),
                    radius = beadRadius * 1.6f
                ),
                radius = beadRadius,
                center = beadCenter
            )
            drawCircle(
                color = Color.White.copy(alpha = if (isActive) 0.35f else 0.18f),
                radius = beadRadius * 0.28f,
                center = beadCenter - Offset(beadRadius * 0.28f, beadRadius * 0.32f)
            )
        }
    }
}

@Composable
private fun BeadRing(count: Int, target: Int) {
    val beadCount = 33
    val progress = if (target == 0) 0f else (count % target).toFloat() / target
    val filledBeads = (progress * beadCount).toInt().coerceIn(0, beadCount)

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
                        colors = listOf(GoldLight, GoldDome),
                        center = beadCenter,
                        radius = beadRadius * 1.5f
                    )
                } else {
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFE8E8E8), Color(0xFFB0B0B0)),
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
