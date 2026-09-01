package com.greendome.adhkar.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.model.AzkarDisplayMode
import com.greendome.adhkar.ui.theme.AppAccentGreen
import com.greendome.adhkar.ui.theme.GoldDome

@Composable
fun DisplayModeToggle(
    selected: AzkarDisplayMode,
    onSelected: (AzkarDisplayMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedContent = AppAccentGreen()
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = GoldDome.copy(alpha = 0.22f),
        selectedLabelColor = selectedContent,
        selectedLeadingIconColor = selectedContent,
    )
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FilterChip(
            selected = selected == AzkarDisplayMode.LIST,
            onClick = { onSelected(AzkarDisplayMode.LIST) },
            label = {
                Text(
                    stringResource(R.string.azkar_display_list),
                    style = MaterialTheme.typography.labelMedium,
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.ViewList,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
            colors = chipColors,
        )
        FilterChip(
            selected = selected == AzkarDisplayMode.CARD,
            onClick = { onSelected(AzkarDisplayMode.CARD) },
            label = {
                Text(
                    stringResource(R.string.azkar_display_card),
                    style = MaterialTheme.typography.labelMedium,
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.ViewModule,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
            colors = chipColors,
        )
    }
}
