package com.greendome.adhkar.prayer

import android.content.Context
import android.location.Address
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

object CityLocator {
    suspend fun search(context: Context, query: String): List<PrayerLocation> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()
        val known = searchKnownCities(trimmed)
        val geo = withTimeoutOrNull(2_500) { geocode(context, trimmed) }.orEmpty()
        return (known + geo).distinctBy {
            "${it.cityName}|${it.countryCode}|${"%.2f".format(Locale.US, it.latitude)}"
        }
    }

    suspend fun reverse(context: Context, latitude: Double, longitude: Double): PrayerLocation {
        val fromGeo = withTimeoutOrNull(3_500) { reverseGeocode(context, latitude, longitude) }
        if (fromGeo != null) {
            return fromGeo.copy(latitude = latitude, longitude = longitude)
        }
        val nearest = KNOWN_CITIES.minByOrNull { city ->
            distanceMeters(latitude, longitude, city.latitude, city.longitude)
        }
        if (nearest != null &&
            distanceMeters(latitude, longitude, nearest.latitude, nearest.longitude) < 80_000
        ) {
            return nearest.copy(latitude = latitude, longitude = longitude)
        }
        return PrayerLocation(
            latitude = latitude,
            longitude = longitude,
            cityName = "%.2f, %.2f".format(latitude, longitude)
        )
    }

    @Suppress("DEPRECATION")
    private suspend fun geocode(context: Context, query: String): List<PrayerLocation> {
        if (!Geocoder.isPresent()) return emptyList()
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = withContext(Dispatchers.IO) {
                geocoder.getFromLocationName(query, 8).orEmpty()
            }
            addresses.mapNotNull { it.toPrayerLocation() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun reverseGeocode(
        context: Context,
        latitude: Double,
        longitude: Double
    ): PrayerLocation? {
        if (!Geocoder.isPresent()) return null
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = withContext(Dispatchers.IO) {
                geocoder.getFromLocation(latitude, longitude, 1).orEmpty()
            }
            addresses.firstOrNull()?.toPrayerLocation()
        } catch (_: Exception) {
            null
        }
    }

    private fun Address.toPrayerLocation(): PrayerLocation? {
        if (!hasLatitude() || !hasLongitude()) return null
        val city = locality
            ?: subAdminArea
            ?: featureName
            ?: adminArea
            ?: return null
        return PrayerLocation(
            latitude = latitude,
            longitude = longitude,
            cityName = city,
            countryName = countryName.orEmpty(),
            countryCode = countryCode.orEmpty()
        )
    }
}

fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
    val result = FloatArray(1)
    android.location.Location.distanceBetween(lat1, lng1, lat2, lng2, result)
    return result[0]
}
