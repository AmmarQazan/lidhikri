package com.greendome.adhkar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun AzkarNavIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val stroke = Stroke(width = w * 0.09f)

        val leftPage = Path().apply {
            moveTo(cx, h * 0.16f)
            cubicTo(w * 0.1f, h * 0.2f, w * 0.06f, h * 0.48f, w * 0.1f, h * 0.72f)
            cubicTo(w * 0.12f, h * 0.84f, cx * 0.94f, h * 0.86f, cx, h * 0.82f)
            close()
        }
        val rightPage = Path().apply {
            moveTo(cx, h * 0.16f)
            cubicTo(w * 0.9f, h * 0.2f, w * 0.94f, h * 0.48f, w * 0.9f, h * 0.72f)
            cubicTo(w * 0.88f, h * 0.84f, w * 1.06f - cx * 0.94f, h * 0.86f, cx, h * 0.82f)
            close()
        }

        drawPath(leftPage, tint)
        drawPath(rightPage, tint)
        drawLine(
            color = tint.copy(alpha = 0.55f),
            start = Offset(cx, h * 0.14f),
            end = Offset(cx, h * 0.84f),
            strokeWidth = w * 0.045f
        )

        val lineStroke = w * 0.05f
        val lineAlpha = 0.35f
        listOf(0.34f, 0.46f, 0.58f, 0.7f).forEach { y ->
            drawLine(
                color = tint.copy(alpha = lineAlpha),
                start = Offset(w * 0.2f, h * y),
                end = Offset(w * 0.42f, h * y),
                strokeWidth = lineStroke
            )
            drawLine(
                color = tint.copy(alpha = lineAlpha),
                start = Offset(w * 0.58f, h * y),
                end = Offset(w * 0.8f, h * y),
                strokeWidth = lineStroke
            )
        }
    }
}
