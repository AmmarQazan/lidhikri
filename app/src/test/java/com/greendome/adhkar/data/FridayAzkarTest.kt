package com.greendome.adhkar.data

import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.prayer.PrayerName
import com.greendome.adhkar.util.CollectionScheduleHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FridayAzkarTest {

    @Test
    fun fridayMaskIsOnlyFriday() {
        val mask = FridayAzkar.fridayOnlyMask()
        assertTrue(CollectionScheduleHelper.isDayEnabled(mask, java.util.Calendar.FRIDAY))
        assertFalse(CollectionScheduleHelper.isDayEnabled(mask, java.util.Calendar.THURSDAY))
        assertFalse(CollectionScheduleHelper.isDayEnabled(mask, java.util.Calendar.SATURDAY))
        assertEquals(32, mask)
    }

    @Test
    fun catalogAllowsFridayClockToggle() {
        val spec = AutoAzkarCatalog.spec(FridayAzkar.COLLECTION_ID)
        requireNotNull(spec)
        assertTrue(spec.allowed)
        assertTrue(spec.defaultEnabled)
        assertEquals(AutoAzkarCatalog.Trigger.CLOCK, spec.trigger)
        assertTrue(FridayAzkar.COLLECTION_ID in AutoAzkarCatalog.onboardingSpecs().map { it.id })
        assertTrue(FridayAzkar.COLLECTION_ID in AutoAzkarCatalog.defaultEnabledClockIds())
    }

    @Test
    fun seedsCarryPerItemHours() {
        val items = FridayAzkar.items()
        assertEquals(-1, items[2].scheduleHour)
        assertTrue(items[2].toEntity(3).inheritsCollectionTime())
        assertEquals(11, items[0].scheduleHour)
        assertEquals(PrayerName.DHUHR.name, items[0].prayerAnchor)
        assertEquals(-30, items[0].prayerOffsetMinutes)
        assertEquals(16, items[1].scheduleHour)
        assertEquals(30, items[1].scheduleMinute)
        assertEquals(PrayerName.MAGHRIB.name, items[1].prayerAnchor)
        assertTrue(items[1].skipQuietWindow)
        val entity = AzkarItemEntity(
            collectionId = FridayAzkar.COLLECTION_ID,
            textAr = items[1].text,
            virtueAr = items[1].virtue,
            scheduleHour = items[1].scheduleHour,
            scheduleMinute = items[1].scheduleMinute,
            prayerAnchor = items[1].prayerAnchor,
            prayerOffsetMinutes = items[1].prayerOffsetMinutes,
            skipQuietWindow = items[1].skipQuietWindow,
        )
        assertTrue(entity.textAr.startsWith("قال صلى الله عليه وسلم:"))
        assertTrue(entity.textAr.contains("صدق رسول الله"))
        assertEquals("اللهم صل وسلم وبارك على نبينا محمد", items[0].text)
    }
}
