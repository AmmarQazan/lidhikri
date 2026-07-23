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
import com.greendome.adhkar.data.model.NumberDigitStyle

@Composable
fun NumberDigitStyleSelector(
    selected: NumberDigitStyle,
    onSelected: (NumberDigitStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    NumberDigitStyle.entries.forEach { style ->
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selected == style,
                onClick = { onSelected(style) },
            )
            Text(
                numberDigitStyleLabel(style),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
fun numberDigitStyleLabel(style: NumberDigitStyle): String = when (style) {
    NumberDigitStyle.ARABIC_INDIC -> stringResource(R.string.app_digits_arabic)
    NumberDigitStyle.LATIN -> stringResource(R.string.app_digits_latin)
}
