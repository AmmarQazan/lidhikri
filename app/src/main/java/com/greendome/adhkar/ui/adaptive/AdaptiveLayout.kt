package com.greendome.adhkar.ui.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Material 3 breakpoints — phones, 10" tablets, unfolded foldables. */
enum class AppWindowWidth {
    Compact,
    Medium,
    Expanded,
}

@Composable
fun rememberAppWindowWidth(): AppWindowWidth {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return remember(widthDp) {
        when {
            widthDp < 600 -> AppWindowWidth.Compact
            widthDp < 840 -> AppWindowWidth.Medium
            else -> AppWindowWidth.Expanded
        }
    }
}

fun AppWindowWidth.useNavigationRail(): Boolean = this != AppWindowWidth.Compact

fun AppWindowWidth.maxContentWidth(): Dp? = when (this) {
    AppWindowWidth.Compact -> null
    AppWindowWidth.Medium -> 720.dp
    AppWindowWidth.Expanded -> 840.dp
}

@Composable
fun AdaptiveContentContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val maxWidth = rememberAppWindowWidth().maxContentWidth()
    if (maxWidth == null) {
        Box(modifier.fillMaxSize(), content = content)
    } else {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = maxWidth)
                    .fillMaxWidth(),
                content = content,
            )
        }
    }
}
