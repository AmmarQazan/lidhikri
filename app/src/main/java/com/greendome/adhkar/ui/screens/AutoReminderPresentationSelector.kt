package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.model.AutoReminderPresentation

@Composable
fun AutoReminderPresentationSelector(
    selected: AutoReminderPresentation,
    onSelected: (AutoReminderPresentation) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        PresentationOption(
            label = stringResource(R.string.presentation_popup_audio),
            selected = selected == AutoReminderPresentation.POPUP_AND_AUDIO,
            onSelect = { onSelected(AutoReminderPresentation.POPUP_AND_AUDIO) }
        )
        PresentationOption(
            label = stringResource(R.string.presentation_popup_only),
            selected = selected == AutoReminderPresentation.POPUP_ONLY,
            onSelect = { onSelected(AutoReminderPresentation.POPUP_ONLY) }
        )
        PresentationOption(
            label = stringResource(R.string.presentation_notification),
            selected = selected == AutoReminderPresentation.NOTIFICATION,
            onSelect = { onSelected(AutoReminderPresentation.NOTIFICATION) }
        )
        PresentationOption(
            label = stringResource(R.string.presentation_audio_only),
            selected = selected == AutoReminderPresentation.AUDIO_ONLY,
            onSelect = { onSelected(AutoReminderPresentation.AUDIO_ONLY) }
        )
    }
}

@Composable
private fun PresentationOption(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, modifier = Modifier.padding(start = 4.dp))
    }
}
