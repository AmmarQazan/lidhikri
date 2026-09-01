package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.model.AppThemeMode
import com.greendome.adhkar.ui.theme.AppAccentGreen

@Composable
fun AppThemeModeSelector(
    selected: AppThemeMode,
    onSelected: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    AppThemeModeOption(
        label = stringResource(R.string.reading_theme_light),
        selected = selected == AppThemeMode.LIGHT,
        onSelect = { onSelected(AppThemeMode.LIGHT) },
        modifier = modifier
    )
    AppThemeModeOption(
        label = stringResource(R.string.reading_theme_dark),
        selected = selected == AppThemeMode.DARK,
        onSelect = { onSelected(AppThemeMode.DARK) },
        modifier = modifier
    )
    AppThemeModeOption(
        label = stringResource(R.string.reading_theme_system),
        selected = selected == AppThemeMode.SYSTEM,
        onSelect = { onSelected(AppThemeMode.SYSTEM) },
        modifier = modifier
    )
}

@Composable
private fun AppThemeModeOption(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) AppAccentGreen() else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
