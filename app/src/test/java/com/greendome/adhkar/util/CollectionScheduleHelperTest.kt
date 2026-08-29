package com.greendome.adhkar.util

import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class CollectionScheduleHelperTest {

    private val from = at(18, 0)

    @Test
    fun clockSectionAppearsAsNext() {
        val evening = collection("evening", hour = 19, minute = 0)
        val morning = collection("morning", hour = 7, minute = 0)
        val next = CollectionScheduleHelper.findNextEnabled(
            listOf(evening, morning),
            fromMillis = from
        )
        assertEquals("evening", next?.first?.id)
    }

    @Test
    fun afterPrayerWinsWhenSoonerThanClockSection() {
        val evening = collection("evening", hour = 19, minute = 0)
        val afterPrayer = collection(
            "after_prayer",
            hour = 12,
            minute = 30,
            allowed = false,
            enabled = false
        )
        val next = CollectionScheduleHelper.findNextEnabled(
            listOf(evening, afterPrayer),
            fromMillis = from,
            extraTriggers = listOf(afterPrayer to at(18, 30))
        )
        assertEquals("after_prayer", next?.first?.id)
    }

    @Test
    fun clockSectionWinsWhenSoonerThanAfterPrayer() {
        val evening = collection("evening", hour = 19, minute = 0)
        val afterPrayer = collection(
            "after_prayer",
            hour = 12,
            minute = 30,
            allowed = false,
            enabled = false
        )
        val next = CollectionScheduleHelper.findNextEnabled(
            listOf(evening, afterPrayer),
            fromMillis = from,
            extraTriggers = listOf(afterPrayer to at(20, 0))
        )
        assertEquals("evening", next?.first?.id)
    }

    @Test
    fun afterPrayerAloneAppearsWhenNoClockSectionsEnabled() {
        val afterPrayer = collection(
            "after_prayer",
            hour = 12,
            minute = 30,
            allowed = false,
            enabled = false
        )
        val next = CollectionScheduleHelper.findNextEnabled(
            listOf(afterPrayer),
            fromMillis = from,
            extraTriggers = listOf(afterPrayer to at(18, 15))
        )
        assertEquals("after_prayer", next?.first?.id)
    }

    @Test
    fun afterPrayerWithoutExtraIsIgnored() {
        val afterPrayer = collection(
            "after_prayer",
            hour = 12,
            minute = 30,
            allowed = false,
            enabled = false
        )
        val next = CollectionScheduleHelper.findNextEnabled(
            listOf(afterPrayer),
            fromMillis = from
        )
        assertNull(next)
    }

    @Test
    fun pastExtraTriggerIsIgnored() {
        val evening = collection("evening", hour = 19, minute = 0)
        val afterPrayer = collection(
            "after_prayer",
            hour = 12,
            minute = 30,
            allowed = false,
            enabled = false
        )
        val next = CollectionScheduleHelper.findNextEnabled(
            listOf(evening, afterPrayer),
            fromMillis = from,
            extraTriggers = listOf(afterPrayer to at(17, 0))
        )
        assertEquals("evening", next?.first?.id)
    }

    private fun collection(
        id: String,
        hour: Int,
        minute: Int,
        allowed: Boolean = true,
        enabled: Boolean = true
    ) = AdhkarCollectionEntity(
        id = id,
        titleAr = id,
        autoPlayAllowed = allowed,
        autoPlayEnabled = enabled,
        scheduleHour = hour,
        scheduleMinute = minute,
        weekDaysMask = 127
    )

    private fun at(hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = fromBase()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun fromBase(): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
