package com.greendome.adhkar.ui.screens

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.prayer.QiblaCalculator
import com.greendome.adhkar.ui.theme.formatLocalizedDigits
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun QiblaCompass(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
) {
    val context = LocalContext.current
    var azimuth by remember { mutableFloatStateOf(0f) }
    val qibla = remember(latitude, longitude) { QiblaCalculator.bearing(latitude, longitude) }
    val distance = remember(latitude, longitude) { QiblaCalculator.distanceKm(latitude, longitude) }
    DisposableEffect(Unit) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotation = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        @Suppress("DEPRECATION")
        val magnetic = manager.getDefaultSensor(Sensor.TYPE_ORIENTATION)
        val listener = object : SensorEventListener {
            private val matrix = FloatArray(9)
            private val orientation = FloatArray(3)
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(matrix, event.values)
                    SensorManager.getOrientation(matrix, orientation)
                    azimuth = ((Math.toDegrees(orientation[0].toDouble()) + 360) % 360).toFloat()
                } else {
                    @Suppress("DEPRECATION")
                    if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
                        azimuth = (event.values[0] + 360) % 360
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (rotation != null) {
            manager.registerListener(listener, rotation, SensorManager.SENSOR_DELAY_GAME)
        } else if (magnetic != null) {
            manager.registerListener(listener, magnetic, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose { manager.unregisterListener(listener) }
    }
    val needle = qibla - azimuth
    val ring = MaterialTheme.colorScheme.outline
    val accent = MaterialTheme.colorScheme.primary
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(size)) {
                val r = size.toPx() / 2f
                val c = Offset(r, r)
                drawCircle(color = ring, radius = r - 4f, style = Stroke(width = 4f))
                val rad = Math.toRadians(needle.toDouble() - 90)
                val tip = Offset(
                    c.x + cos(rad).toFloat() * (r - 16f),
                    c.y + sin(rad).toFloat() * (r - 16f)
                )
                drawLine(accent, c, tip, strokeWidth = 8f, cap = StrokeCap.Round)
                drawCircle(accent, radius = 10f, center = c)
            }
        }
        Text(
            stringResource(R.string.qibla_degrees, qibla.toInt().formatLocalizedDigits()),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            stringResource(R.string.qibla_distance, distance.toInt().formatLocalizedDigits()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
