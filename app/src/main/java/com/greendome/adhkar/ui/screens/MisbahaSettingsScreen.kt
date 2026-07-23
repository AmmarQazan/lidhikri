package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.greendome.adhkar.data.model.MisbahaFeedbackMode
import com.greendome.adhkar.data.model.MisbahaStyle
import com.greendome.adhkar.util.MisbahaFeedback

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
