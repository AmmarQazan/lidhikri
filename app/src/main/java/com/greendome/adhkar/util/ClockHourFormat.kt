package com.greendome.adhkar.util

import com.greendome.adhkar.data.model.ClockHourFormat

fun formatClockTime(
    hour24: Int,
    minute: Int,
    format: ClockHourFormat,
    periodAm: String,
    periodPm: String,
    separator: String = ":",
): String {
    val hour = hour24.coerceIn(0, 23)
    val min = minute.coerceIn(0, 59)
    return when (format) {
        ClockHourFormat.HOUR_24 -> "%02d%s%02d".format(hour, separator, min)
        ClockHourFormat.HOUR_12 -> {
            val hour12 = (hour % 12).let { if (it == 0) 12 else it }
            val period = if (hour < 12) periodAm else periodPm
            "%d%s%02d %s".format(hour12, separator, min, period)
        }
    }
}

fun formatClockHourCompact(
    hour24: Int,
    minute: Int,
    periodAm: String,
    periodPm: String
): String {
    val hour = hour24.coerceIn(0, 23)
    val min = minute.coerceIn(0, 59)
    val hour12 = (hour % 12).let { if (it == 0) 12 else it }
    val period = if (hour < 12) periodAm else periodPm
    return if (min == 0) "%d %s".format(hour12, period)
    else "%d:%02d %s".format(hour12, min, period)
}
