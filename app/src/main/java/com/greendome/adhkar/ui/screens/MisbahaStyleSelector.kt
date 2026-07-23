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
import com.greendome.adhkar.data.model.MisbahaStyle

@Composable
fun MisbahaStyleSelector(
    selected: MisbahaStyle,
    onSelected: (MisbahaStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    MisbahaStyle.entries.forEach { style ->
        MisbahaStyleOption(
            label = misbahaStyleLabel(style),
            selected = selected == style,
            onSelect = { onSelected(style) },
            modifier = modifier
        )
    }
}

@Composable
private fun MisbahaStyleOption(
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

@Composable
fun misbahaStyleLabel(style: MisbahaStyle): String = when (style) {
    MisbahaStyle.TRADITIONAL -> stringResource(R.string.misbaha_style_traditional)
    MisbahaStyle.ELECTRONIC -> stringResource(R.string.misbaha_style_electronic)
}
