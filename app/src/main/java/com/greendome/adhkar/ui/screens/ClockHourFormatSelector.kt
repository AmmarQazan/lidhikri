package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.model.ClockHourFormat
import com.greendome.adhkar.ui.theme.GreenPrimaryDark

@Composable
fun ClockHourFormatSelector(
    selected: ClockHourFormat,
    onSelected: (ClockHourFormat) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == ClockHourFormat.HOUR_12,
            onClick = { onSelected(ClockHourFormat.HOUR_12) },
            label = {
                Text(
                    stringResource(R.string.azkar_clock_format_12),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = selected == ClockHourFormat.HOUR_24,
            onClick = { onSelected(ClockHourFormat.HOUR_24) },
            label = {
                Text(
                    stringResource(R.string.azkar_clock_format_24),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ClockHourFormatLabel(modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.azkar_clock_format_label),
        style = MaterialTheme.typography.labelMedium,
        color = GreenPrimaryDark,
        modifier = modifier.padding(top = 4.dp)
    )
}
