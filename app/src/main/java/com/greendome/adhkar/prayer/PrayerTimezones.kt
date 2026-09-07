package com.greendome.adhkar.prayer

import java.time.ZoneId
import java.util.TimeZone
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object PrayerTimezones {
    private const val NEAREST_CITY_MAX_KM = 2_000.0

    fun resolve(location: PrayerLocation): String {
        location.timezoneId.takeIf { isValidZone(it) }?.let { return it }
        nearestKnown(location.latitude, location.longitude)
            ?.timezoneId
            ?.takeIf { isValidZone(it) }
            ?.let { return it }
        location.countryCode
            .takeIf { it.isNotBlank() }
            ?.let { PrayerCountryDefaults.timezoneIdFor(it, fallback = "") }
            ?.takeIf { isValidZone(it) }
            ?.let { return it }
        return TimeZone.getDefault().id
    }

    fun withResolved(location: PrayerLocation): PrayerLocation {
        val resolved = resolve(location)
        return if (location.timezoneId == resolved) location else location.copy(timezoneId = resolved)
    }

    fun isValidZone(id: String): Boolean =
        id.isNotBlank() && runCatching { ZoneId.of(id) }.isSuccess

    internal fun nearestKnown(latitude: Double, longitude: Double): PrayerLocation? {
        val nearest = KNOWN_CITIES
            .filter { isValidZone(it.timezoneId) }
            .minByOrNull { haversineKm(latitude, longitude, it.latitude, it.longitude) }
            ?: return null
        val km = haversineKm(latitude, longitude, nearest.latitude, nearest.longitude)
        return nearest.takeIf { km <= NEAREST_CITY_MAX_KM }
    }

    internal fun haversineKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        return 2 * earthKm * asin(min(1.0, sqrt(a)))
    }
}
