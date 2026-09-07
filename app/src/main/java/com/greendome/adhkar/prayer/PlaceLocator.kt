package com.greendome.adhkar.prayer

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

object PlaceLocator {
    suspend fun search(
        context: Context,
        query: String,
        near: Location? = null
    ): List<PrayerLocation> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()
        return coroutineScope {
            val photon = async {
                withTimeoutOrNull(5_000) { searchPhoton(trimmed, near) }.orEmpty()
            }
            val geo = async {
                withTimeoutOrNull(2_500) { geocodePlaces(context, trimmed) }.orEmpty()
            }
            (photon.await() + geo.await()).distinctBy {
                "${"%.4f".format(Locale.US, it.latitude)}|" +
                    "${"%.4f".format(Locale.US, it.longitude)}"
            }
        }
    }

    suspend fun reverse(context: Context, latitude: Double, longitude: Double): PrayerLocation {
        val fromGeo = withTimeoutOrNull(3_500) { reverseGeocode(context, latitude, longitude) }
        if (fromGeo != null) {
            return PrayerTimezones.withResolved(
                fromGeo.copy(latitude = latitude, longitude = longitude)
            )
        }
        return PrayerTimezones.withResolved(
            CityLocator.reverse(context, latitude, longitude)
                .copy(latitude = latitude, longitude = longitude)
        )
    }

    private suspend fun searchPhoton(query: String, near: Location?): List<PrayerLocation> =
        withContext(Dispatchers.IO) {
            val url = HttpUrl.Builder()
                .scheme("https")
                .host("photon.komoot.io")
                .addPathSegment("api")
                .addQueryParameter("q", query)
                .addQueryParameter("limit", "10")
                .apply {
                    near?.let {
                        addQueryParameter("lat", it.latitude.toString())
                        addQueryParameter("lon", it.longitude.toString())
                    }
                    PHOTON_POI_TAGS.forEach { tag -> addQueryParameter("osm_tag", tag) }
                }
                .build()
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .get()
                .build()
            runCatching {
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use emptyList()
                    val body = response.body?.string().orEmpty()
                    if (body.isBlank()) emptyList() else parsePhoton(body)
                }
            }.getOrDefault(emptyList())
        }

    @Suppress("DEPRECATION")
    private suspend fun geocodePlaces(context: Context, query: String): List<PrayerLocation> {
        if (!Geocoder.isPresent()) return emptyList()
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = withContext(Dispatchers.IO) {
                geocoder.getFromLocationName(query, 12).orEmpty()
            }
            addresses.mapNotNull { it.toPlaceLocation(requireSpecific = true) }
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
            addresses.firstOrNull()?.toPlaceLocation(requireSpecific = false)
                ?.copy(latitude = latitude, longitude = longitude)
        } catch (_: Exception) {
            null
        }
    }

    private fun parsePhoton(body: String): List<PrayerLocation> {
        val features = JSONObject(body).optJSONArray("features") ?: return emptyList()
        return buildList {
            for (i in 0 until features.length()) {
                val feature = features.optJSONObject(i) ?: continue
                val coords = feature.optJSONObject("geometry")?.optJSONArray("coordinates")
                if (coords == null || coords.length() < 2) continue
                val lon = coords.optDouble(0, Double.NaN)
                val lat = coords.optDouble(1, Double.NaN)
                if (lat.isNaN() || lon.isNaN()) continue
                val props = feature.optJSONObject("properties") ?: continue
                val hit = PhotonHit(
                    name = props.optString("name"),
                    street = listOf(
                        props.optString("housenumber"),
                        props.optString("street")
                    ).filter { it.isNotBlank() }.joinToString(" "),
                    city = props.optString("city").ifBlank {
                        props.optString("district").ifBlank { props.optString("county") }
                    },
                    country = props.optString("country"),
                    countryCode = props.optString("countrycode"),
                    osmKey = props.optString("osm_key"),
                    osmValue = props.optString("osm_value"),
                    type = props.optString("type"),
                    houseNumber = props.optString("housenumber"),
                    latitude = lat,
                    longitude = lon
                )
                if (!hit.isSpecificPlace()) continue
                add(hit.toPrayerLocation() ?: continue)
            }
        }
    }

    private fun Address.toPlaceLocation(requireSpecific: Boolean): PrayerLocation? {
        if (!hasLatitude() || !hasLongitude()) return null
        if (requireSpecific && !isSpecificGeocodedPlace(
                thoroughfare = thoroughfare,
                subThoroughfare = subThoroughfare,
                premises = premises,
                featureName = featureName,
                locality = locality,
                subLocality = subLocality,
                subAdminArea = subAdminArea,
                adminArea = adminArea,
                countryName = countryName
            )
        ) {
            return null
        }
        val street = listOf(subThoroughfare, thoroughfare)
            .mapNotNull { it?.trim()?.takeIf { part -> part.isNotEmpty() } }
            .joinToString(" ")
        val (title, area) = formatPlaceLabel(
            name = featureName.orEmpty(),
            street = street.ifBlank { premises.orEmpty() },
            city = locality ?: subLocality ?: subAdminArea.orEmpty(),
            country = countryName.orEmpty()
        )
        if (title.isBlank()) return null
        return PrayerLocation(
            latitude = latitude,
            longitude = longitude,
            cityName = title,
            countryName = area,
            countryCode = countryCode.orEmpty()
        ).let(PrayerTimezones::withResolved)
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    private const val USER_AGENT = "Sabbih/1.0 (com.greendome.adhkar; home-place-search)"
}

private data class PhotonHit(
    val name: String,
    val street: String,
    val city: String,
    val country: String,
    val countryCode: String,
    val osmKey: String,
    val osmValue: String,
    val type: String,
    val houseNumber: String,
    val latitude: Double,
    val longitude: Double
) {
    fun isSpecificPlace(): Boolean = isSpecificPhotonPlace(
        osmKey = osmKey,
        osmValue = osmValue,
        type = type,
        houseNumber = houseNumber
    )

    fun toPrayerLocation(): PrayerLocation? {
        val (title, area) = formatPlaceLabel(name, street, city, country)
        if (title.isBlank()) return null
        return PrayerLocation(
            latitude = latitude,
            longitude = longitude,
            cityName = title,
            countryName = area,
            countryCode = countryCode
        ).let(PrayerTimezones::withResolved)
    }
}
