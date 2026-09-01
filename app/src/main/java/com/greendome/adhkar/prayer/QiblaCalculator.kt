package com.greendome.adhkar.prayer

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object QiblaCalculator {
    const val KAABA_LAT = 21.422487
    const val KAABA_LNG = 39.826206

    /** اتجاه القبلة بالدرجات من الشمال، 0–360 */
    fun bearing(latitude: Double, longitude: Double): Float {
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(KAABA_LAT)
        val dLng = Math.toRadians(KAABA_LNG - longitude)
        val y = sin(dLng) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
        val deg = Math.toDegrees(atan2(y, x))
        return ((deg + 360.0) % 360.0).toFloat()
    }

    fun distanceKm(latitude: Double, longitude: Double): Double {
        val earthKm = 6371.0
        val dLat = Math.toRadians(KAABA_LAT - latitude)
        val dLng = Math.toRadians(KAABA_LNG - longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(latitude)) * cos(Math.toRadians(KAABA_LAT)) *
            sin(dLng / 2) * sin(dLng / 2)
        return 2 * earthKm * atan2(sqrt(a), sqrt(1 - a))
    }
}
