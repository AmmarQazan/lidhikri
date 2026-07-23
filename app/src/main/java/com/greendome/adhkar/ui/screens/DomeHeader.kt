package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.ui.theme.CreamBackground

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DomeHeader(onLongPress: (() -> Unit)? = null) {
    val clickModifier = if (onLongPress != null) {
        Modifier.combinedClickable(onClick = {}, onLongClick = onLongPress)
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CreamBackground)
            .then(clickModifier),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.logo_sabbih),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.68f)
                .height(148.dp)
                .statusBarsPadding()
                .padding(top = 6.dp, bottom = 10.dp)
        )
    }
}
