package com.greendome.adhkar.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.greendome.adhkar.R
import com.greendome.adhkar.ui.theme.GreenPrimary
import com.greendome.adhkar.util.copyTextToClipboard
import com.greendome.adhkar.util.shareText

@Composable
fun AzkarTextMenu(
    text: String,
    modifier: Modifier = Modifier,
    iconTint: Color = GreenPrimary.copy(alpha = 0.6f)
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.azkar_text_menu),
                tint = iconTint
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.azkar_copy_text)) },
                leadingIcon = {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    copyTextToClipboard(context, text)
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.azkar_share_text)) },
                leadingIcon = {
                    Icon(Icons.Default.Share, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    shareText(context, text)
                }
            )
        }
    }
}
