package com.greendome.adhkar.prayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceSpecificityTest {

    @Test
    fun photonKeepsShopsOfficesAndHouses() {
        assertTrue(isSpecificPhotonPlace("shop", "supermarket", "house", ""))
        assertTrue(isSpecificPhotonPlace("office", "company", "house", ""))
        assertTrue(isSpecificPhotonPlace("amenity", "cafe", "house", ""))
        assertTrue(isSpecificPhotonPlace("building", "yes", "house", "12"))
        assertTrue(isSpecificPhotonPlace("place", "house", "house", "7"))
    }

    @Test
    fun photonRejectsCityNeighborhoodAndStreet() {
        assertFalse(isSpecificPhotonPlace("place", "city", "city", ""))
        assertFalse(isSpecificPhotonPlace("place", "suburb", "district", ""))
        assertFalse(isSpecificPhotonPlace("place", "neighbourhood", "locality", ""))
        assertFalse(isSpecificPhotonPlace("boundary", "administrative", "state", ""))
        assertFalse(isSpecificPhotonPlace("highway", "primary", "street", ""))
    }

    @Test
    fun geocoderKeepsStreetAndNamedPlace() {
        assertTrue(
            isSpecificGeocodedPlace(
                thoroughfare = "King Street",
                subThoroughfare = "15",
                premises = null,
                featureName = "15",
                locality = "Amman",
                subLocality = null,
                subAdminArea = null,
                adminArea = null,
                countryName = "Jordan"
            )
        )
        assertTrue(
            isSpecificGeocodedPlace(
                thoroughfare = null,
                subThoroughfare = null,
                premises = null,
                featureName = "Mafraq store",
                locality = "Abu Dhabi",
                subLocality = null,
                subAdminArea = null,
                adminArea = null,
                countryName = "UAE"
            )
        )
    }

    @Test
    fun geocoderRejectsCityAndNeighborhoodOnly() {
        assertFalse(
            isSpecificGeocodedPlace(
                thoroughfare = null,
                subThoroughfare = null,
                premises = null,
                featureName = "المفرق",
                locality = "المفرق",
                subLocality = null,
                subAdminArea = null,
                adminArea = "المفرق",
                countryName = "الأردن"
            )
        )
        assertFalse(
            isSpecificGeocodedPlace(
                thoroughfare = null,
                subThoroughfare = null,
                premises = null,
                featureName = "Saida",
                locality = "Saida",
                subLocality = null,
                subAdminArea = "Saida",
                adminArea = "Algeria",
                countryName = "Algeria"
            )
        )
    }

    @Test
    fun labelUsesPlaceNameThenStreetThenCity() {
        assertEquals(
            "Mafraq store, شارع الزنبق" to "أبوظبي، الإمارات",
            formatPlaceLabel("Mafraq store", "شارع الزنبق", "أبوظبي", "الإمارات")
        )
        assertEquals(
            "15 King Street" to "Amman، Jordan",
            formatPlaceLabel("15 King Street", "15 King Street", "Amman", "Jordan")
        )
        assertEquals(
            "المفرق" to "الأردن",
            formatPlaceLabel("", "", "المفرق", "الأردن")
        )
    }
}
