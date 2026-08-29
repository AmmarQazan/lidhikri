package com.greendome.adhkar.util

import com.greendome.adhkar.data.model.ClockHourFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockHourFormatTest {

    @Test
    fun twentyFourHourPadsHourAndMinute() {
        assertEquals(
            "19:49",
            formatClockTime(19, 49, ClockHourFormat.HOUR_24, "AM", "PM")
        )
        assertEquals(
            "00:05",
            formatClockTime(0, 5, ClockHourFormat.HOUR_24, "AM", "PM")
        )
    }

    @Test
    fun twelveHourMapsMidnightNoonAndEvening() {
        assertEquals(
            "12:05 AM",
            formatClockTime(0, 5, ClockHourFormat.HOUR_12, "AM", "PM")
        )
        assertEquals(
            "12:00 PM",
            formatClockTime(12, 0, ClockHourFormat.HOUR_12, "AM", "PM")
        )
        assertEquals(
            "7:49 PM",
            formatClockTime(19, 49, ClockHourFormat.HOUR_12, "AM", "PM")
        )
        assertEquals(
            "7:49 م",
            formatClockTime(19, 49, ClockHourFormat.HOUR_12, "ص", "م")
        )
    }

    @Test
    fun compactTwelveHourOmitsZeroMinutes() {
        assertEquals("10 ص", formatClockHourCompact(10, 0, "ص", "م"))
        assertEquals("10 م", formatClockHourCompact(22, 0, "ص", "م"))
        assertEquals("10:30 ص", formatClockHourCompact(10, 30, "ص", "م"))
    }
}
