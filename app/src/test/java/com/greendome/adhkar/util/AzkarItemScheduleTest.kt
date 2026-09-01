package com.greendome.adhkar.util

import com.greendome.adhkar.data.FridayAzkar
import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.data.model.CollectionDayMode
import com.greendome.adhkar.prayer.PrayerConfig
import com.greendome.adhkar.prayer.PrayerName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.util.Calendar

class AzkarItemScheduleTest {

    @Test
    fun kahfInheritsCollectionFridayMorning() {
        val collection = fridayCollection()
        val kahf = item(id = 3, hour = -1)
        val from = atDay(Calendar.THURSDAY, 22, 0)
        val next = AzkarItemSchedule.nextTriggerAt(collection, kahf, PrayerConfig.empty(), from)
        val cal = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(Calendar.FRIDAY, cal.get(Calendar.DAY_OF_WEEK))
        assertEquals(8, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
        assertTrue(next > from)
    }

    @Test
    fun customClockFallsBackWithoutPrayerTimes() {
        val collection = fridayCollection()
        val salawat = item(
            id = 1,
            hour = 11,
            minute = 0,
            prayer = PrayerName.DHUHR,
            offset = -30,
        )
        val from = atDay(Calendar.THURSDAY, 22, 0)
        val next = AzkarItemSchedule.nextTriggerAt(collection, salawat, PrayerConfig.empty(), from)
        val cal = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(Calendar.FRIDAY, cal.get(Calendar.DAY_OF_WEEK))
        assertEquals(11, cal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun hourItemFallsBackToAfternoonClock() {
        val collection = fridayCollection()
        val hourItem = item(
            id = 2,
            hour = 16,
            minute = 30,
            prayer = PrayerName.MAGHRIB,
            offset = -60,
            skipQuiet = true,
        )
        val from = atDay(Calendar.THURSDAY, 22, 0)
        val next = AzkarItemSchedule.nextTriggerAt(collection, hourItem, PrayerConfig.empty(), from)
        val cal = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(Calendar.FRIDAY, cal.get(Calendar.DAY_OF_WEEK))
        assertEquals(16, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, cal.get(Calendar.MINUTE))
    }

    @Test
    fun extraTriggersEmptyWhenDisabled() {
        val extras = AzkarItemSchedule.extraTriggers(
            fridayCollection().copy(autoPlayEnabled = false),
            listOf(item(id = 1, hour = 11)),
            PrayerConfig.empty(),
        )
        assertTrue(extras.isEmpty())
    }

    @Test
    fun blessedDaysSkipNonMatchingHijri() {
        val collection = AdhkarCollectionEntity(
            id = "blessed_days",
            titleAr = "أذكار الأيام المباركة",
            autoPlayAllowed = true,
            autoPlayEnabled = true,
            scheduleHour = 10,
            scheduleMinute = 0,
            dayMode = CollectionDayMode.ITEM_HIJRI,
        )
        val arafah = AzkarItemEntity(
            id = 2,
            collectionId = collection.id,
            textAr = "لا إله إلا الله",
            hijriMonth = 12,
            hijriDayStart = 9,
            hijriDayEnd = 9,
        )
        val hijri = HijrahDate.now()
            .with(ChronoField.MONTH_OF_YEAR, 12)
            .with(ChronoField.DAY_OF_MONTH, 9)
        val iso = java.time.LocalDate.from(hijri)
        val before = Calendar.getInstance().apply {
            set(iso.year, iso.monthValue - 1, iso.dayOfMonth, 8, 0, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis
        val next = AzkarItemSchedule.nextTriggerAt(
            collection,
            arafah,
            PrayerConfig.empty(),
            before,
            listOf(arafah),
        )
        assertTrue(arafah.matchesHijri(next))
        val cal = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(10, cal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun hijriSectionMatchesTodayHijri() {
        val hijri = HijrahDate.now()
        val collection = AdhkarCollectionEntity(
            id = "eid",
            titleAr = "عيد",
            autoPlayAllowed = true,
            autoPlayEnabled = true,
            scheduleHour = 8,
            scheduleMinute = 0,
            dayMode = CollectionDayMode.HIJRI,
            hijriMonth = hijri.get(ChronoField.MONTH_OF_YEAR),
            hijriDayStart = hijri.get(ChronoField.DAY_OF_MONTH),
            hijriDayEnd = hijri.get(ChronoField.DAY_OF_MONTH),
        )
        val from = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 22)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val next = CollectionScheduleHelper.nextTriggerAt(collection, from)
        assertTrue(CollectionScheduleHelper.isHijriMatch(collection, next))
        val cal = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(8, cal.get(Calendar.HOUR_OF_DAY))
    }

    private fun fridayCollection() = AdhkarCollectionEntity(
        id = FridayAzkar.COLLECTION_ID,
        titleAr = "أذكار يوم الجمعة",
        autoPlayAllowed = true,
        autoPlayEnabled = true,
        scheduleHour = 8,
        scheduleMinute = 0,
        weekDaysMask = FridayAzkar.fridayOnlyMask(),
        dayMode = CollectionDayMode.WEEKDAYS,
    )

    private fun item(
        id: Long,
        hour: Int,
        minute: Int = 0,
        prayer: PrayerName? = null,
        offset: Int = 0,
        skipQuiet: Boolean = false,
    ) = AzkarItemEntity(
        id = id,
        collectionId = FridayAzkar.COLLECTION_ID,
        textAr = "ذكر",
        scheduleHour = hour,
        scheduleMinute = minute,
        prayerAnchor = prayer?.name.orEmpty(),
        prayerOffsetMinutes = offset,
        skipQuietWindow = skipQuiet,
    )

    private fun atDay(dayOfWeek: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
                add(Calendar.DAY_OF_YEAR, 7)
                set(Calendar.DAY_OF_WEEK, dayOfWeek)
            }
        }.timeInMillis
}
