package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.model.TtsVoiceGender

@Composable
fun TtsVoiceGenderSelector(
    selected: TtsVoiceGender,
    onSelected: (TtsVoiceGender) -> Unit,
    modifier: Modifier = Modifier
) {
    TtsVoiceGenderOption(
        label = stringResource(R.string.tts_voice_male),
        selected = selected == TtsVoiceGender.MALE,
        onSelect = { onSelected(TtsVoiceGender.MALE) },
        modifier = modifier
    )
    TtsVoiceGenderOption(
        label = stringResource(R.string.tts_voice_female),
        selected = selected == TtsVoiceGender.FEMALE,
        onSelect = { onSelected(TtsVoiceGender.FEMALE) },
        modifier = modifier
    )
}

@Composable
private fun TtsVoiceGenderOption(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
