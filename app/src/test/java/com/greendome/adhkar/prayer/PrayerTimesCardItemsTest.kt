package com.greendome.adhkar.prayer

import org.junit.Assert.assertEquals
import org.junit.Test

class PrayerTimesCardItemsTest {

    @Test
    fun keysKeepChronologicalOrder() {
        assertEquals(
            listOf(
                PRAYER_TIMES_CARD_IMSAK,
                PrayerName.FAJR.name,
                PRAYER_TIMES_CARD_SUNRISE,
                PrayerName.DHUHR.name,
                PrayerName.ASR.name,
                PrayerName.MAGHRIB.name,
                PrayerName.ISHA.name,
            ),
            prayerTimesCardKeys(),
        )
    }

    @Test
    fun highlightIndexCentersNextPrayer() {
        assertEquals(-1, prayerTimesCardHighlightIndex(null))
        assertEquals(1, prayerTimesCardHighlightIndex(PrayerName.FAJR))
        assertEquals(3, prayerTimesCardHighlightIndex(PrayerName.DHUHR))
        assertEquals(4, prayerTimesCardHighlightIndex(PrayerName.ASR))
        assertEquals(5, prayerTimesCardHighlightIndex(PrayerName.MAGHRIB))
        assertEquals(6, prayerTimesCardHighlightIndex(PrayerName.ISHA))
    }
}
