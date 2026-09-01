package com.greendome.adhkar.data

import com.greendome.adhkar.data.model.CollectionDayMode
import com.greendome.adhkar.prayer.PrayerName
import com.greendome.adhkar.util.CollectionScheduleHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.util.Calendar

class BlessedDaysAzkarTest {

    @Test
    fun catalogEnablesBlessedDaysByDefault() {
        val spec = AutoAzkarCatalog.spec(BlessedDaysAzkar.COLLECTION_ID)
        requireNotNull(spec)
        assertTrue(spec.allowed)
        assertTrue(spec.defaultEnabled)
        assertEquals(AutoAzkarCatalog.Trigger.CLOCK, spec.trigger)
        assertTrue(BlessedDaysAzkar.COLLECTION_ID in AutoAzkarCatalog.onboardingSpecs().map { it.id })
        assertTrue(BlessedDaysAzkar.COLLECTION_ID in AutoAzkarCatalog.defaultEnabledClockIds())
    }

    @Test
    fun collectionUsesItemHijriAndAutoPlay() {
        val entity = BlessedDaysAzkar.entity()
        assertEquals(CollectionDayMode.ITEM_HIJRI, entity.dayMode)
        assertTrue(entity.autoPlayAllowed)
        assertTrue(entity.autoPlayEnabled)
        assertEquals("أذكار الأيام المباركة", entity.titleAr)
        val (hour, minute) = BlessedDaysAzkar.daytimeFromMorning(
            com.greendome.adhkar.util.TasbihWindow.DEFAULT_MORNING_HOUR,
            com.greendome.adhkar.util.TasbihWindow.DEFAULT_MORNING_MINUTE,
        )
        assertEquals(hour, entity.scheduleHour)
        assertEquals(minute, entity.scheduleMinute)
    }

    @Test
    fun daytimeReminderIsOneHourAfterMorning() {
        assertEquals(11 to 0, BlessedDaysAzkar.daytimeFromMorning(10, 0))
        assertEquals(8 to 15, BlessedDaysAzkar.daytimeFromMorning(7, 15))
        assertEquals(0 to 30, BlessedDaysAzkar.daytimeFromMorning(23, 30))
        assertTrue(BlessedDaysAzkar.shouldFollowMorning(10, 0, 10, 0))
        assertTrue(BlessedDaysAzkar.shouldFollowMorning(11, 0, 10, 0))
        assertFalse(BlessedDaysAzkar.shouldFollowMorning(15, 0, 10, 0))
    }

    @Test
    fun itemsAreTiedToHijriDays() {
        val items = BlessedDaysAzkar.items()
        assertEquals(3, items.size)
        assertEquals(BlessedDaysAzkar.RAMADAN, items[0].hijriMonth)
        assertEquals(21, items[0].hijriDayStart)
        assertEquals(30, items[0].hijriDayEnd)
        assertEquals(PrayerName.MAGHRIB.name, items[0].prayerAnchor)
        assertTrue(items[0].skipQuietWindow)
        assertEquals(BlessedDaysAzkar.DHUL_HIJJAH, items[1].hijriMonth)
        assertEquals(9, items[1].hijriDayStart)
        assertTrue(items[1].toEntity(2).inheritsCollectionTime())
        assertEquals(1, items[2].hijriDayStart)
        assertEquals(13, items[2].hijriDayEnd)
        assertFalse(BlessedDaysAzkar.QADR_TEXT.contains("كريم"))
        assertFalse(
            "تشكيل عفوٌّ يمنع contains الخام — المطابقة تكون بعد إزالة التشكيل",
            BlessedDaysAzkar.QADR_TEXT.contains("عفو تحب العفو"),
        )
        items.forEach { seed ->
            assertTrue(
                "detect must match text or virtue: ${seed.detect}",
                seed.matches(seed.text) || seed.matchesVirtue(seed.virtue),
            )
        }
        assertTrue(items[0].matches(BlessedDaysAzkar.QADR_TEXT))
    }

    @Test
    fun arafahItemMatchesDhulHijjahNinth() {
        val item = BlessedDaysAzkar.items()[1].toEntity(2)
        val hijri = HijrahDate.now()
            .with(ChronoField.MONTH_OF_YEAR, BlessedDaysAzkar.DHUL_HIJJAH.toLong())
            .with(ChronoField.DAY_OF_MONTH, 9)
        val iso = java.time.LocalDate.from(hijri)
        val millis = Calendar.getInstance().apply {
            set(iso.year, iso.monthValue - 1, iso.dayOfMonth, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertTrue(item.matchesHijri(millis))
        assertEquals("9 ذو الحجة", CollectionScheduleHelper.hijriSummaryAr(item))
        val ramadanNight = BlessedDaysAzkar.items()[0].toEntity(1)
        assertFalse(ramadanNight.matchesHijri(millis))
    }
}
