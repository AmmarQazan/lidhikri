package com.greendome.adhkar.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal object MisbahaWidgetRenderer {
    private const val BEAD_CREAM = 0xFFF6E7CC.toInt()
    private const val BEAD_CREAM_DARK = 0xFFE2C79A.toInt()
    private const val BEAD_ACTIVE = 0xFFE8D5A8.toInt()
    private const val STRING_ORANGE = 0xFFE86A2A.toInt()

    fun drawTraditionalBeads(width: Int, height: Int, count: Int, target: Int): Bitmap {
        val w = width.coerceAtLeast(120)
        val h = height.coerceAtLeast(80)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val beadCount = 11
        val progressIndex = if (target == 0 || count == 0) -1 else (count - 1).mod(beadCount)
        val paddingX = w * 0.04f
        val paddingY = h * 0.12f
        val positions = List(beadCount) { index ->
            val t = index / (beadCount - 1).toFloat()
            val x = paddingX + (w - paddingX * 2f) * t
            val y = paddingY + (1f - sin(t * PI).toFloat()) * (h * 0.72f)
            x to y
        }

        val stringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = STRING_ORANGE
            style = Paint.Style.STROKE
            strokeWidth = min(w, h) * 0.035f
            strokeCap = Paint.Cap.ROUND
        }
        val stringPath = Path().apply {
            moveTo(positions.first().first, positions.first().second)
            positions.drop(1).forEach { (x, y) -> lineTo(x, y) }
        }
        canvas.drawPath(stringPath, stringPaint)

        val centerIndex = beadCount / 2
        positions.forEachIndexed { index, (x, y) ->
            val isCenter = index == centerIndex
            val radius = min((w / beadCount) * if (isCenter) 0.40f else 0.32f, h * 0.22f)
            drawBead(canvas, x, y, radius, index <= progressIndex)
        }
        return bitmap
    }

    fun drawElectronicGraphic(
        width: Int,
        height: Int,
        count: Int,
        target: Int,
        colors: ElectronicMisbahaColors,
    ): Bitmap {
        val size = min(width, height).coerceAtLeast(160)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = size / 2f
        val cy = size / 2f
        val outerRadius = size * 0.46f
        val innerRadius = size * 0.33f
        val beadCount = 33
        val filled = if (target <= 0) 0 else ((count.coerceAtMost(target) * beadCount) / target)

        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx,
                cy,
                innerRadius,
                intArrayOf(colors.centerInner, colors.centerOuter),
                floatArrayOf(0.12f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, cy, innerRadius, innerPaint)

        val beadRadius = size * 0.028f
        for (index in 0 until beadCount) {
            val angle = (2 * PI * index / beadCount) - (PI / 2)
            val bx = cx + cos(angle).toFloat() * outerRadius
            val by = cy + sin(angle).toFloat() * outerRadius
            drawBead(canvas, bx, by, beadRadius, index < filled, colors)
        }
        return bitmap
    }

    private fun drawBead(
        canvas: Canvas,
        x: Float,
        y: Float,
        radius: Float,
        active: Boolean,
        colors: ElectronicMisbahaColors? = null,
    ) {
        val highlight: Int
        val mid: Int
        val shadow: Int
        if (colors != null) {
            highlight = if (active) colors.beadActiveLight else 0xFFFFFBF3.toInt()
            mid = if (active) colors.beadActiveMid else colors.beadInactiveLight
            shadow = if (active) colors.beadActiveDark else colors.beadInactiveDark
        } else {
            highlight = if (active) 0xFFFFF6E8.toInt() else 0xFFFFFBF3.toInt()
            mid = if (active) BEAD_ACTIVE else BEAD_CREAM
            shadow = if (active) 0xFFC9A56B.toInt() else BEAD_CREAM_DARK
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                x - radius * 0.28f,
                y - radius * 0.32f,
                radius * 1.55f,
                intArrayOf(highlight, mid, shadow),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(x, y, radius, paint)
        val gloss = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x66FFFFFF
        }
        canvas.drawCircle(x - radius * 0.28f, y - radius * 0.32f, radius * 0.28f, gloss)
    }
}
