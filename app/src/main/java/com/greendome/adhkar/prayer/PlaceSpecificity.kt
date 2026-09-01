package com.greendome.adhkar.prayer

import java.util.Locale

internal fun isSpecificPhotonPlace(
    osmKey: String,
    osmValue: String,
    type: String,
    houseNumber: String
): Boolean {
    if (type.equals("house", ignoreCase = true)) return true
    if (houseNumber.isNotBlank()) return true
    if (osmKey.equals("place", ignoreCase = true) && osmValue.equals("house", ignoreCase = true)) {
        return true
    }
    if (osmKey.lowercase(Locale.US) in POI_OSM_KEYS) return true
    if (type.lowercase(Locale.US) in COARSE_PHOTON_TYPES) return false
    if (osmValue.lowercase(Locale.US) in COARSE_OSM_VALUES) return false
    if (osmKey.equals("boundary", ignoreCase = true) || osmKey.equals("place", ignoreCase = true)) {
        return false
    }
    return false
}

internal fun isSpecificGeocodedPlace(
    thoroughfare: String?,
    subThoroughfare: String?,
    premises: String?,
    featureName: String?,
    locality: String?,
    subLocality: String?,
    subAdminArea: String?,
    adminArea: String?,
    countryName: String?
): Boolean {
    if (!thoroughfare.isNullOrBlank() ||
        !subThoroughfare.isNullOrBlank() ||
        !premises.isNullOrBlank()
    ) {
        return true
    }
    val feature = featureName?.trim().orEmpty()
    if (feature.isEmpty()) return false
    val areas = listOf(locality, subLocality, subAdminArea, adminArea, countryName)
        .mapNotNull { it?.trim()?.takeIf { part -> part.isNotEmpty() } }
    return areas.none { it.equals(feature, ignoreCase = true) }
}

internal fun formatPlaceLabel(
    name: String,
    street: String,
    city: String,
    country: String
): Pair<String, String> {
    val cleanName = name.trim()
    val cleanStreet = street.trim()
    val title = when {
        cleanName.isNotEmpty() &&
            cleanStreet.isNotEmpty() &&
            !cleanName.equals(cleanStreet, ignoreCase = true) &&
            !cleanStreet.contains(cleanName, ignoreCase = true) ->
            "$cleanName, $cleanStreet"
        cleanName.isNotEmpty() -> cleanName
        cleanStreet.isNotEmpty() -> cleanStreet
        else -> city.trim()
    }
    val area = listOf(city.trim(), country.trim())
        .filter { it.isNotEmpty() && !title.contains(it, ignoreCase = true) }
        .distinct()
        .joinToString("، ")
    return title to area
}

internal val PHOTON_POI_TAGS = listOf(
    "amenity",
    "shop",
    "office",
    "building",
    "tourism",
    "craft",
    "healthcare",
    "leisure",
    "place:house"
)

private val COARSE_PHOTON_TYPES = setOf(
    "country", "state", "county", "city", "district", "locality", "street"
)

private val COARSE_OSM_VALUES = setOf(
    "city", "town", "village", "suburb", "neighbourhood", "neighborhood",
    "hamlet", "state", "country", "county", "municipality", "borough",
    "quarter", "locality", "isolated_dwelling", "administrative",
    "province", "region"
)

private val POI_OSM_KEYS = setOf(
    "amenity", "shop", "office", "building", "tourism", "craft",
    "healthcare", "leisure", "historic"
)
