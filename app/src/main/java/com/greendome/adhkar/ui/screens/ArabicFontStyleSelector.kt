package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.model.ArabicFontStyle
import com.greendome.adhkar.ui.theme.AppOnCardColor
import com.greendome.adhkar.ui.theme.arabicFontFamily

@Composable
fun ArabicFontStyleSelector(
    selected: ArabicFontStyle,
    onSelected: (ArabicFontStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        ArabicFontStyle.entries.forEach { style ->
            FontStyleOption(
                style = style,
                selected = selected == style,
                onSelect = { onSelected(style) }
            )
        }
    }
}

@Composable
private fun FontStyleOption(
    style: ArabicFontStyle,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(Modifier.padding(start = 4.dp, top = 12.dp)) {
            Text(
                text = stringResource(fontStyleLabelRes(style)),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.onboarding_font_preview),
                style = TextStyle(
                    fontFamily = arabicFontFamily(style),
                    fontSize = 18.sp,
                    lineHeight = 28.sp,
                    textAlign = TextAlign.Start
                ),
                color = AppOnCardColor(),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun fontStyleLabelRes(style: ArabicFontStyle): Int = when (style) {
    ArabicFontStyle.DEFAULT -> R.string.font_style_default
    ArabicFontStyle.UTHMANI_1 -> R.string.font_style_uthmani_1
    ArabicFontStyle.UTHMANI_2 -> R.string.font_style_uthmani_2
    ArabicFontStyle.INDOPAK_1 -> R.string.font_style_indopak_1
    ArabicFontStyle.INDOPAK_2 -> R.string.font_style_indopak_2
    ArabicFontStyle.BENGALI -> R.string.font_style_bengali
}
