package com.greendome.adhkar.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R

@Composable
fun HomeNavIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(R.drawable.ic_launcher_logo),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(24.dp)
    )
}
