package com.greendome.adhkar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun MisbahaNavIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val r = size.minDimension * 0.09f
        val centers = listOf(
            Offset(size.width * 0.22f, size.height * 0.55f),
            Offset(size.width * 0.38f, size.height * 0.38f),
            Offset(size.width * 0.55f, size.height * 0.32f),
            Offset(size.width * 0.72f, size.height * 0.38f),
            Offset(size.width * 0.86f, size.height * 0.55f)
        )
        centers.forEach { center ->
            drawCircle(color = tint, radius = r, center = center)
        }
        for (i in 0 until centers.lastIndex) {
            drawLine(
                color = tint.copy(alpha = 0.7f),
                start = centers[i],
                end = centers[i + 1],
                strokeWidth = r * 0.6f
            )
        }
        drawCircle(
            color = tint,
            radius = r * 1.4f,
            center = Offset(size.width * 0.12f, size.height * 0.68f),
            style = Stroke(width = r * 0.5f)
        )
    }
}
