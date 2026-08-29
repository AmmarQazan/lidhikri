package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.model.MisbahaFeedbackMode

@Composable
fun MisbahaFeedbackModeSelector(
    selected: MisbahaFeedbackMode,
    onSelected: (MisbahaFeedbackMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.selectableGroup()) {
        MisbahaFeedbackMode.entries.forEach { mode ->
            MisbahaFeedbackModeOption(
                label = misbahaFeedbackModeLabel(mode),
                selected = selected == mode,
                onSelect = { onSelected(mode) }
            )
        }
    }
}

@Composable
private fun MisbahaFeedbackModeOption(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton
            )
            .padding(vertical = 4.dp),
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
fun misbahaFeedbackModeLabel(mode: MisbahaFeedbackMode): String = when (mode) {
    MisbahaFeedbackMode.SOUND_AND_VIBRATION -> stringResource(R.string.misbaha_feedback_sound_and_vibration)
    MisbahaFeedbackMode.SOUND_ONLY -> stringResource(R.string.misbaha_feedback_sound)
    MisbahaFeedbackMode.VIBRATION_ONLY -> stringResource(R.string.misbaha_feedback_vibration)
    MisbahaFeedbackMode.SILENT -> stringResource(R.string.misbaha_feedback_silent)
}
