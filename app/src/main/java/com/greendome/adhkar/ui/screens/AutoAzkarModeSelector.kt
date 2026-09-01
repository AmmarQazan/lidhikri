package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R

@Composable
fun AutoAzkarModeSelector(
    randomMode: Boolean,
    onRandomModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    useFilterChips: Boolean = false
) {
    if (useFilterChips) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = randomMode,
                onClick = { onRandomModeChange(true) },
                label = {
                    Text(
                        stringResource(R.string.auto_azkar_random),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = !randomMode,
                onClick = { onRandomModeChange(false) },
                label = {
                    Text(
                        stringResource(R.string.auto_azkar_sequential),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        ModeOption(
            label = stringResource(R.string.auto_azkar_random),
            selected = randomMode,
            onSelect = { onRandomModeChange(true) }
        )
        ModeOption(
            label = stringResource(R.string.auto_azkar_sequential),
            selected = !randomMode,
            onSelect = { onRandomModeChange(false) }
        )
    }
}

@Composable
private fun ModeOption(label: String, selected: Boolean, onSelect: () -> Unit) {
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

@Composable
fun AutoAzkarModeLabel(modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.auto_azkar_mode_label),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}

