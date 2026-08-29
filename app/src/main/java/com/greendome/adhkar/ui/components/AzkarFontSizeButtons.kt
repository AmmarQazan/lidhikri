package com.greendome.adhkar.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.ui.theme.GreenPrimaryDark

@Composable
fun AzkarFontSizeButtons(
    fontSp: Int,
    minSp: Int,
    maxSp: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = IconButtonDefaults.iconButtonColors(contentColor = GreenPrimaryDark)
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onDecrease,
            enabled = fontSp > minSp,
            modifier = Modifier.size(40.dp),
            colors = colors,
        ) {
            Icon(
                imageVector = Icons.Default.TextDecrease,
                contentDescription = stringResource(R.string.azkar_list_font_decrease),
            )
        }
        IconButton(
            onClick = onIncrease,
            enabled = fontSp < maxSp,
            modifier = Modifier.size(40.dp),
            colors = colors,
        ) {
            Icon(
                imageVector = Icons.Default.TextIncrease,
                contentDescription = stringResource(R.string.azkar_list_font_increase),
            )
        }
    }
}
