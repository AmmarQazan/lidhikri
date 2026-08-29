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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.model.DhikrOfDayDisplayMode
import com.greendome.adhkar.data.model.DhikrOfDayTextColor
import com.greendome.adhkar.data.model.DhikrOfDayWidgetText
import com.greendome.adhkar.data.model.MisbahaWidgetBackground
import com.greendome.adhkar.ui.components.rememberLockScreenAccessRequester
import com.greendome.adhkar.ui.theme.GoldDome
import com.greendome.adhkar.ui.theme.GreenPrimary
import com.greendome.adhkar.ui.theme.GreenPrimaryDark
import com.greendome.adhkar.ui.theme.TextPrimary
import com.greendome.adhkar.ui.theme.stringResourceDigits
import com.greendome.adhkar.widget.DhikrOfDayManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun DhikrOfDaySettingsScreen(
    settings: SettingsRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var enabled by remember { mutableStateOf(settings.dhikrOfDayEnabled) }
    var displayMode by remember { mutableStateOf(settings.dhikrOfDayDisplayMode) }
    var background by remember { mutableStateOf(settings.dhikrOfDayWidgetBackground) }
    var textColor by remember { mutableStateOf(settings.dhikrOfDayWidgetTextColor) }
    var fontSizeSp by remember { mutableFloatStateOf(settings.dhikrOfDayWidgetFontSizeSp.toFloat()) }
    var todayText by remember { mutableStateOf("") }
    val requestLockScreenAccess = rememberLockScreenAccessRequester(requireFullScreenIntent = false)

    fun refreshPreview() {
        scope.launch {
            todayText = withContext(Dispatchers.IO) {
                DhikrOfDayManager.getTodayText(context)
            }
        }
    }

    LaunchedEffect(enabled, displayMode, background, textColor, fontSizeSp) {
        DhikrOfDayManager.refreshAsync(context)
        refreshPreview()
    }

    fun applyEnabled(value: Boolean) {
        if (value && displayMode == DhikrOfDayDisplayMode.LOCK_SCREEN) {
            requestLockScreenAccess {
                enabled = true
                settings.dhikrOfDayEnabled = true
                DhikrOfDayManager.refreshAsync(context)
                refreshPreview()
            }
            return
        }
        enabled = value
        settings.dhikrOfDayEnabled = value
        DhikrOfDayManager.refreshAsync(context)
        if (value) {
            refreshPreview()
        }
    }

    fun applyDisplayMode(mode: DhikrOfDayDisplayMode) {
        if (mode == DhikrOfDayDisplayMode.LOCK_SCREEN && enabled) {
            requestLockScreenAccess {
                displayMode = mode
                settings.dhikrOfDayDisplayMode = mode
                DhikrOfDayManager.refreshAsync(context)
            }
            return
        }
        displayMode = mode
        settings.dhikrOfDayDisplayMode = mode
        if (mode == DhikrOfDayDisplayMode.HOME_WIDGET) {
            enabled = true
            settings.dhikrOfDayEnabled = true
        }
        DhikrOfDayManager.refreshAsync(context)
    }

    SettingsSubScreenScaffold(
        title = stringResource(R.string.settings_dhikr_of_day_title),
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
                    stringResource(R.string.settings_dhikr_of_day_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                SectionTitle(
                    title = stringResource(R.string.dhikr_of_day_display_section),
                    subtitle = stringResource(R.string.dhikr_of_day_display_hint)
                )
            }
            item {
                DhikrOfDayDisplayModeSelector(
                    selected = displayMode,
                    onSelected = ::applyDisplayMode
                )
            }

            if (displayMode == DhikrOfDayDisplayMode.LOCK_SCREEN) {
                item {
                    SettingSwitch(
                        label = stringResource(R.string.dhikr_of_day_enable),
                        checked = enabled,
                        onChange = ::applyEnabled
                    )
                }
                item {
                    Text(
                        stringResource(R.string.dhikr_of_day_lock_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                SectionTitle(
                    title = stringResource(R.string.misbaha_widget_background_section),
                    subtitle = stringResource(R.string.dhikr_of_day_widget_background_hint)
                )
            }
            item {
                WidgetBackgroundSelector(
                    selected = background,
                    onSelected = { selected ->
                        background = selected
                        settings.dhikrOfDayWidgetBackground = selected
                    }
                )
            }
            item {
                SectionTitle(
                    title = stringResource(R.string.dhikr_of_day_text_color_section),
                    subtitle = stringResource(R.string.dhikr_of_day_text_color_hint)
                )
            }
            item {
                DhikrOfDayTextColorSelector(
                    selected = textColor,
                    onSelected = { selected ->
                        textColor = selected
                        settings.dhikrOfDayWidgetTextColor = selected
                    }
                )
            }
            item {
                SectionTitle(
                    title = stringResource(R.string.dhikr_of_day_font_size_section),
                    subtitle = stringResource(R.string.dhikr_of_day_font_size_hint)
                )
            }
            item {
                DhikrOfDayFontSizeSlider(
                    fontSizeSp = fontSizeSp,
                    onChange = { size ->
                        fontSizeSp = size
                        settings.dhikrOfDayWidgetFontSizeSp = size.roundToInt()
                    }
                )
            }

            item {
                SectionTitle(title = stringResource(R.string.dhikr_of_day_preview_section))
            }
            item {
                DhikrOfDayWidgetPreview(
                    text = todayText.ifBlank { stringResource(R.string.dhikr_of_day_loading) },
                    background = background,
                    textColor = textColor,
                    fontSizeSp = fontSizeSp,
                )
            }
            if (displayMode == DhikrOfDayDisplayMode.HOME_WIDGET) {
                item {
                    Button(
                        onClick = {
                            enabled = true
                            settings.dhikrOfDayEnabled = true
                            DhikrOfDayManager.refreshAsync(context)
                            val pinned = DhikrOfDayManager.requestPinWidget(context)
                            if (!pinned) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.dhikr_of_day_widget_manual_hint),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.dhikr_of_day_add_widget))
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = {
                        DhikrOfDayManager.forceNewDhikr(context)
                        refreshPreview()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.dhikr_of_day_refresh))
                }
            }
            if (displayMode == DhikrOfDayDisplayMode.HOME_WIDGET) {
                item {
                    Text(
                        stringResource(R.string.dhikr_of_day_widget_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DhikrOfDayDisplayModeSelector(
    selected: DhikrOfDayDisplayMode,
    onSelected: (DhikrOfDayDisplayMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        DhikrOfDayDisplayModeOption(
            label = stringResource(R.string.dhikr_of_day_mode_home_widget),
            selected = selected == DhikrOfDayDisplayMode.HOME_WIDGET,
            onSelect = { onSelected(DhikrOfDayDisplayMode.HOME_WIDGET) },
        )
        DhikrOfDayDisplayModeOption(
            label = stringResource(R.string.dhikr_of_day_mode_lock_screen),
            selected = selected == DhikrOfDayDisplayMode.LOCK_SCREEN,
            onSelect = { onSelected(DhikrOfDayDisplayMode.LOCK_SCREEN) },
        )
    }
}

@Composable
private fun DhikrOfDayDisplayModeOption(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
private fun DhikrOfDayWidgetPreview(
    text: String,
    background: MisbahaWidgetBackground,
    textColor: DhikrOfDayTextColor,
    fontSizeSp: Float,
) {
    val titleColor = textColor.previewTitleColor(background)
    val bodyColor = textColor.previewBodyColor(background)
    val titleSp = (fontSizeSp - 1f).coerceAtLeast(11f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .then(widgetBackgroundPreview(background))
            .border(1.dp, Color.Black.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.dhikr_of_day_title),
            fontSize = titleSp.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor,
            textAlign = TextAlign.Center,
        )
        Text(
            text = text,
            modifier = Modifier.padding(top = 6.dp),
            fontSize = fontSizeSp.sp,
            fontWeight = FontWeight.Medium,
            color = bodyColor,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DhikrOfDayTextColorSelector(
    selected: DhikrOfDayTextColor,
    onSelected: (DhikrOfDayTextColor) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DhikrOfDayTextColor.entries.forEach { option ->
            val borderColor = if (selected == option) GreenPrimary else Color.Transparent
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = if (selected == option) 2.dp else 0.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(14.dp),
                    )
                    .clickable { onSelected(option) }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(option.swatchColor())
                        .border(1.dp, Color.Black.copy(alpha = 0.12f), CircleShape),
                )
                Text(
                    text = dhikrOfDayTextColorLabel(option),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun DhikrOfDayFontSizeSlider(
    fontSizeSp: Float,
    onChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = fontSizeSp,
            onValueChange = onChange,
            valueRange = DhikrOfDayWidgetText.MIN_FONT_SP.toFloat()..DhikrOfDayWidgetText.MAX_FONT_SP.toFloat(),
        )
        Text(
            stringResourceDigits(R.string.dhikr_of_day_font_size_value, fontSizeSp.roundToInt()),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun dhikrOfDayTextColorLabel(option: DhikrOfDayTextColor): String = stringResource(
    when (option) {
        DhikrOfDayTextColor.AUTO -> R.string.dhikr_of_day_text_color_auto
        DhikrOfDayTextColor.BLACK -> R.string.dhikr_of_day_text_color_black
        DhikrOfDayTextColor.WHITE -> R.string.misbaha_widget_background_white
        DhikrOfDayTextColor.GOLD -> R.string.dhikr_of_day_text_color_gold
        DhikrOfDayTextColor.GREEN -> R.string.misbaha_widget_background_green
        DhikrOfDayTextColor.CREAM -> R.string.misbaha_widget_background_cream
        DhikrOfDayTextColor.BROWN -> R.string.dhikr_of_day_text_color_brown
        DhikrOfDayTextColor.NAVY -> R.string.dhikr_of_day_text_color_navy
        DhikrOfDayTextColor.TEAL -> R.string.dhikr_of_day_text_color_teal
        DhikrOfDayTextColor.MAROON -> R.string.dhikr_of_day_text_color_maroon
        DhikrOfDayTextColor.GRAY -> R.string.dhikr_of_day_text_color_gray
        DhikrOfDayTextColor.AMBER -> R.string.dhikr_of_day_text_color_amber
    }
)

private fun DhikrOfDayTextColor.swatchColor(): Color = when (this) {
    DhikrOfDayTextColor.AUTO -> Color(0xFF9E9E9E)
    DhikrOfDayTextColor.BLACK -> TextPrimary
    DhikrOfDayTextColor.WHITE -> Color.White
    DhikrOfDayTextColor.GOLD -> GoldDome
    DhikrOfDayTextColor.GREEN -> GreenPrimaryDark
    DhikrOfDayTextColor.CREAM -> Color(0xFFF3E6C8)
    DhikrOfDayTextColor.BROWN -> Color(0xFF6D4C41)
    DhikrOfDayTextColor.NAVY -> Color(0xFF1A365D)
    DhikrOfDayTextColor.TEAL -> Color(0xFF0F766E)
    DhikrOfDayTextColor.MAROON -> Color(0xFF8B3A2A)
    DhikrOfDayTextColor.GRAY -> Color(0xFF5A5A5A)
    DhikrOfDayTextColor.AMBER -> Color(0xFFC67A2A)
}

private fun DhikrOfDayTextColor.previewTitleColor(background: MisbahaWidgetBackground): Color =
    if (this == DhikrOfDayTextColor.AUTO) {
        if (background == MisbahaWidgetBackground.TRANSPARENT) GreenPrimaryDark else GoldDome
    } else {
        swatchColor()
    }

private fun DhikrOfDayTextColor.previewBodyColor(background: MisbahaWidgetBackground): Color =
    if (this == DhikrOfDayTextColor.AUTO) {
        when (background) {
            MisbahaWidgetBackground.DARK -> Color(0xFFF3E6C8)
            MisbahaWidgetBackground.TRANSPARENT -> GreenPrimaryDark
            else -> TextPrimary
        }
    } else {
        swatchColor()
    }
