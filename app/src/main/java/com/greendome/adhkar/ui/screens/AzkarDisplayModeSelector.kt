package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.model.AzkarDisplayMode

@Composable
fun AzkarDisplayModeSelector(
    selected: AzkarDisplayMode,
    onSelected: (AzkarDisplayMode) -> Unit,
    modifier: Modifier = Modifier
) {
    AzkarDisplayModeOption(
        label = stringResource(R.string.azkar_display_list),
        selected = selected == AzkarDisplayMode.LIST,
        onSelect = { onSelected(AzkarDisplayMode.LIST) },
        modifier = modifier
    )
    AzkarDisplayModeOption(
        label = stringResource(R.string.azkar_display_card),
        selected = selected == AzkarDisplayMode.CARD,
        onSelect = { onSelected(AzkarDisplayMode.CARD) },
        modifier = modifier
    )
}

@Composable
private fun AzkarDisplayModeOption(
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
