package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.model.NumberDigitStyle
import com.greendome.adhkar.ui.theme.stringResourceDigits

@Composable
fun DisplayAppearanceSettingsScreen(
    settings: SettingsRepository,
    onFontScaleChanged: () -> Unit,
    onArabicFontChanged: () -> Unit,
    onThemeModeChanged: () -> Unit,
    onNumberDigitStyleChanged: (NumberDigitStyle) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var themeMode by remember { mutableStateOf(settings.appThemeMode) }
    var numberDigitStyle by remember { mutableStateOf(settings.numberDigitStyle) }
    var azkarDisplayMode by remember { mutableStateOf(settings.azkarDisplayMode) }
    var fontScale by remember { mutableStateOf(settings.fontScale) }
    var arabicFontStyle by remember { mutableStateOf(settings.arabicFontStyle) }

    SettingsSubScreenScaffold(
        title = stringResource(R.string.settings_display_title),
        onBack = onBack,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SectionTitle(
                    title = stringResource(R.string.reading_theme_section),
                    subtitle = stringResource(R.string.reading_theme_hint)
                )
            }
            item {
                AppThemeModeSelector(
                    selected = themeMode,
                    onSelected = {
                        themeMode = it
                        settings.appThemeMode = it
                        onThemeModeChanged()
                    }
                )
            }

            item {
                SectionTitle(
                    title = stringResource(R.string.app_digits_section),
                    subtitle = stringResource(R.string.app_digits_hint),
                )
            }
            item {
                NumberDigitStyleSelector(
                    selected = numberDigitStyle,
                    onSelected = { style ->
                        numberDigitStyle = style
                        settings.numberDigitStyle = style
                        onNumberDigitStyleChanged(style)
                    },
                )
            }

            item {
                SectionTitle(
                    title = stringResource(R.string.azkar_display_section),
                    subtitle = stringResource(R.string.azkar_display_hint)
                )
            }
            item {
                AzkarDisplayModeSelector(
                    selected = azkarDisplayMode,
                    onSelected = {
                        azkarDisplayMode = it
                        settings.azkarDisplayMode = it
                    }
                )
            }

            item {
                SectionTitle(
                    title = stringResource(R.string.font_scale_title),
                    subtitle = stringResource(R.string.font_scale_hint)
                )
            }
            item {
                Slider(
                    value = fontScale,
                    onValueChange = {
                        fontScale = it
                        settings.fontScale = it
                        onFontScaleChanged()
                    },
                    valueRange = 0.8f..1.5f,
                    steps = 6
                )
            }
            item {
                Text(
                    stringResourceDigits(R.string.font_scale_value, (fontScale * 100).toInt()),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            item {
                ExpandableSettingsCard(
                    title = stringResource(R.string.arabic_font_title),
                    subtitle = stringResource(R.string.arabic_font_hint)
                ) {
                    ArabicFontStyleSelector(
                        selected = arabicFontStyle,
                        onSelected = {
                            arabicFontStyle = it
                            settings.arabicFontStyle = it
                            onArabicFontChanged()
                        }
                    )
                }
            }
        }
    }
}
