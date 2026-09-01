package com.greendome.adhkar.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class NextAzkarScheduleTest {
    @Test
    fun relativeDayTodayTomorrowAndLater() {
        val now = at(2026, Calendar.SEPTEMBER, 1, 16, 20)
        val todayEvening = at(2026, Calendar.SEPTEMBER, 1, 19, 0)
        val tomorrowMorning = at(2026, Calendar.SEPTEMBER, 2, 7, 0)
        val nextWeek = at(2026, Calendar.SEPTEMBER, 8, 7, 0)
        assertEquals(
            NextAzkarSchedule.RelativeDay.TODAY,
            NextAzkarSchedule.relativeDay(todayEvening, now),
        )
        assertEquals(
            NextAzkarSchedule.RelativeDay.TOMORROW,
            NextAzkarSchedule.relativeDay(tomorrowMorning, now),
        )
        assertEquals(
            NextAzkarSchedule.RelativeDay.LATER,
            NextAzkarSchedule.relativeDay(nextWeek, now),
        )
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
