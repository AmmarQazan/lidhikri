package com.greendome.adhkar.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.model.MisbahaStyle
import com.greendome.adhkar.data.model.MisbahaWidgetBackground
import com.greendome.adhkar.ui.theme.GreenPrimary
import com.greendome.adhkar.ui.theme.stringResourceDigits
import com.greendome.adhkar.util.formatDigits
import com.greendome.adhkar.widget.MisbahaWidgetManager

@Composable
fun WidgetsSettingsScreen(
    settings: SettingsRepository,
    onOpenDhikrOfDay: () -> Unit,
    onOpenMisbahaWidget: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsSubScreenScaffold(
        title = stringResource(R.string.settings_widgets_title),
        onBack = onBack,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.settings_widgets_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                SettingsNavCard(
                    title = stringResource(R.string.settings_dhikr_of_day_title),
                    subtitle = stringResource(R.string.settings_dhikr_of_day_subtitle),
                    onClick = onOpenDhikrOfDay
                )
            }
            item {
                SettingsNavCard(
                    title = stringResource(R.string.settings_misbaha_widget_title),
                    subtitle = stringResource(R.string.settings_misbaha_widget_subtitle),
                    onClick = onOpenMisbahaWidget
                )
            }
        }
    }
}

@Composable
fun MisbahaWidgetSettingsScreen(
    settings: SettingsRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var style by remember { mutableStateOf(settings.misbahaWidgetStyle) }
    var background by remember { mutableStateOf(settings.misbahaWidgetBackground) }
    var target by remember { mutableIntStateOf(settings.misbahaWidgetTarget) }
    var count by remember { mutableIntStateOf(settings.misbahaWidgetCount) }
    val remaining = (target - count).coerceAtLeast(0)

    LaunchedEffect(style, background, target, count) {
        MisbahaWidgetManager.updateAll(context)
    }

    SettingsSubScreenScaffold(
        title = stringResource(R.string.settings_misbaha_widget_title),
        onBack = onBack,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.settings_misbaha_widget_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                SectionTitle(
                    title = stringResource(R.string.misbaha_widget_style_section),
                    subtitle = stringResource(
                        if (style == MisbahaStyle.TRADITIONAL) {
                            R.string.misbaha_widget_size_traditional
                        } else {
                            R.string.misbaha_widget_size_electronic
                        }
                    )
                )
            }
            item {
                MisbahaStyleSelector(
                    selected = style,
                    onSelected = { selected ->
                        style = selected
                        settings.misbahaWidgetStyle = selected
                    }
                )
            }
            item {
                SectionTitle(
                    title = stringResource(R.string.misbaha_widget_background_section),
                    subtitle = stringResource(R.string.misbaha_widget_background_hint)
                )
            }
            item {
                WidgetBackgroundSelector(
                    selected = background,
                    onSelected = { selected ->
                        background = selected
                        settings.misbahaWidgetBackground = selected
                    }
                )
            }
            item {
                SectionTitle(title = stringResource(R.string.misbaha_widget_target_section))
            }
            item {
                SettingsRepository.WIDGET_TARGET_OPTIONS.forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = target == option,
                            onClick = {
                                target = option
                                settings.misbahaWidgetTarget = option
                                if (count > option) {
                                    count = 0
                                    settings.misbahaWidgetCount = 0
                                }
                            }
                        )
                        Text(
                            stringResourceDigits(R.string.misbaha_target_value, option),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
            item {
                Text(
                    stringResource(
                        R.string.misbaha_progress,
                        count.formatDigits(settings.numberDigitStyle),
                        target.formatDigits(settings.numberDigitStyle)
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Text(
                    stringResource(
                        R.string.misbaha_remaining,
                        remaining.formatDigits(settings.numberDigitStyle)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Text(
                    stringResource(R.string.misbaha_widget_tap_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Button(
                    onClick = {
                        val pinned = MisbahaWidgetManager.requestPin(context, style)
                        if (!pinned) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.misbaha_widget_manual_hint),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.misbaha_widget_add))
                }
            }
            item {
                OutlinedButton(
                    onClick = {
                        MisbahaWidgetManager.resetCount(context)
                        count = 0
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.misbaha_widget_reset))
                }
            }
            item {
                Text(
                    stringResource(R.string.misbaha_widget_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WidgetBackgroundSelector(
    selected: MisbahaWidgetBackground,
    onSelected: (MisbahaWidgetBackground) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MisbahaWidgetBackground.entries.forEach { option ->
            WidgetBackgroundOption(
                option = option,
                selected = selected == option,
                onSelect = { onSelected(option) },
            )
        }
    }
}

@Composable
private fun WidgetBackgroundOption(
    option: MisbahaWidgetBackground,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val borderColor = if (selected) GreenPrimary else Color.Transparent
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .then(widgetBackgroundPreview(option))
                .border(1.dp, Color.Black.copy(alpha = 0.12f), CircleShape),
        )
        Text(
            text = widgetBackgroundLabel(option),
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

internal fun widgetBackgroundPreview(option: MisbahaWidgetBackground): Modifier = when (option) {
    MisbahaWidgetBackground.WHITE -> Modifier.background(Color.White)
    MisbahaWidgetBackground.CREAM -> Modifier.background(Color(0xFFF5F1E9))
    MisbahaWidgetBackground.GREEN -> Modifier.background(Color(0xFFE7F3EC))
    MisbahaWidgetBackground.DARK -> Modifier.background(Color(0xFF1B2E26))
    MisbahaWidgetBackground.TRANSPARENT -> Modifier.drawBehind {
        val cell = size.minDimension / 4f
        val light = Color(0xFFE8E8E8)
        val dark = Color(0xFFBDBDBD)
        var y = 0f
        var row = 0
        while (y < size.height) {
            var x = 0f
            var col = 0
            while (x < size.width) {
                drawRect(
                    color = if ((row + col) % 2 == 0) light else dark,
                    topLeft = Offset(x, y),
                    size = Size(cell, cell),
                )
                x += cell
                col++
            }
            y += cell
            row++
        }
    }
}

@Composable
private fun widgetBackgroundLabel(option: MisbahaWidgetBackground): String = stringResource(
    when (option) {
        MisbahaWidgetBackground.WHITE -> R.string.misbaha_widget_background_white
        MisbahaWidgetBackground.CREAM -> R.string.misbaha_widget_background_cream
        MisbahaWidgetBackground.GREEN -> R.string.misbaha_widget_background_green
        MisbahaWidgetBackground.DARK -> R.string.misbaha_widget_background_dark
        MisbahaWidgetBackground.TRANSPARENT -> R.string.misbaha_widget_background_transparent
    }
)

