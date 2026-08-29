package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.model.MisbahaBeadTheme
import com.greendome.adhkar.data.model.MisbahaFeedbackMode
import com.greendome.adhkar.data.model.MisbahaStyle
import com.greendome.adhkar.ui.theme.stringResourceDigits
import com.greendome.adhkar.util.MisbahaFeedback
import com.greendome.adhkar.widget.MisbahaWidgetManager

@Composable
fun MisbahaSettingsScreen(
    settings: SettingsRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    var feedbackMode by remember { mutableStateOf(settings.misbahaFeedbackMode) }
    var misbahaStyle by remember { mutableStateOf(settings.misbahaStyle) }
    var beadScale by remember { mutableFloatStateOf(settings.misbahaBeadScale) }
    var beadTheme by remember { mutableStateOf(settings.misbahaBeadTheme) }
    var electronicTheme by remember { mutableStateOf(settings.misbahaElectronicTheme) }

    SettingsSubScreenScaffold(
        title = stringResource(R.string.settings_misbaha_title),
        onBack = onBack,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
        ) {
            item {
                SectionTitle(
                    title = stringResource(R.string.misbaha_style_section),
                    subtitle = stringResource(R.string.misbaha_style_hint)
                )
            }
            item {
                MisbahaStyleSelector(
                    selected = misbahaStyle,
                    onSelected = { style ->
                        misbahaStyle = style
                        settings.misbahaStyle = style
                    }
                )
            }
            if (misbahaStyle == MisbahaStyle.TRADITIONAL) {
                item {
                    SectionTitle(
                        title = stringResource(R.string.misbaha_bead_theme_section),
                        subtitle = stringResource(R.string.misbaha_bead_theme_hint)
                    )
                }
                item {
                    MisbahaBeadThemeSelector(
                        selected = beadTheme,
                        onSelected = { theme ->
                            beadTheme = theme
                            settings.misbahaBeadTheme = theme
                        }
                    )
                }
                item {
                    SectionTitle(
                        title = stringResource(R.string.misbaha_bead_scale_section),
                        subtitle = stringResource(R.string.misbaha_bead_scale_hint)
                    )
                }
                item {
                    Slider(
                        value = beadScale,
                        onValueChange = {
                            beadScale = it
                            settings.misbahaBeadScale = it
                        },
                        valueRange = 0.7f..1.6f,
                        steps = 8
                    )
                }
                item {
                    Text(
                        stringResourceDigits(R.string.misbaha_bead_scale_value, (beadScale * 100).toInt()),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                item {
                    SectionTitle(
                        title = stringResource(R.string.misbaha_electronic_theme_section),
                        subtitle = stringResource(R.string.misbaha_electronic_theme_hint)
                    )
                }
                item {
                    MisbahaBeadThemeSelector(
                        selected = electronicTheme,
                        onSelected = { theme ->
                            electronicTheme = theme
                            settings.misbahaElectronicTheme = theme
                            MisbahaWidgetManager.updateAll(context)
                        }
                    )
                }
            }
            item {
                SectionTitle(
                    title = stringResource(R.string.misbaha_feedback_section),
                    subtitle = stringResource(R.string.misbaha_feedback_hint)
                )
            }
            item {
                MisbahaFeedbackModeSelector(
                    selected = feedbackMode,
                    onSelected = { mode ->
                        feedbackMode = mode
                        settings.misbahaFeedbackMode = mode
                        MisbahaFeedback.perform(context, mode, view)
                    }
                )
            }
        }
    }
}
