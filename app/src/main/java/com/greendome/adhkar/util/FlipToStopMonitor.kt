package com.greendome.adhkar.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * يوقف التشغيل عند قلب الهاتف (الوجه للأسفل) أثناء الاستماع.
 */
class FlipToStopMonitor(
    private val context: Context,
    private val onFlip: () -> Unit,
) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var faceDownSinceMs = 0L
    private var active = false

    fun start() {
        if (active || accelerometer == null) return
        faceDownSinceMs = 0L
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        active = true
    }

    fun stop() {
        if (!active) return
        sensorManager.unregisterListener(this)
        faceDownSinceMs = 0L
        active = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!active || event == null) return
        val z = event.values[2]
        if (z < -8f) {
            if (faceDownSinceMs == 0L) faceDownSinceMs = System.currentTimeMillis()
            if (System.currentTimeMillis() - faceDownSinceMs >= FACE_DOWN_HOLD_MS) {
                stop()
                onFlip()
            }
        } else {
            faceDownSinceMs = 0L
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val FACE_DOWN_HOLD_MS = 350L
    }
}
