package com.greendome.adhkar.util

import com.greendome.adhkar.data.SettingsRepository
import java.util.Calendar

/** نافذة اليوم لأول وآخر تسبيحة تلقائية. */
data class TasbihWindow(
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
) {
    private val startMin = startHour.coerceIn(0, 23) * 60 + startMinute.coerceIn(0, 59)
    private val endMin = endHour.coerceIn(0, 23) * 60 + endMinute.coerceIn(0, 59)

    fun contains(hour: Int, minute: Int): Boolean {
        val now = hour.coerceIn(0, 23) * 60 + minute.coerceIn(0, 59)
        return if (startMin <= endMin) now in startMin..endMin
        else now >= startMin || now <= endMin
    }

    fun contains(calendar: Calendar): Boolean =
        contains(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))

    fun isOvernight(): Boolean = startMin > endMin

    /** الموعد التالي بعد [fromMillis] شامل طرفَي النافذة. */
    fun nextTriggerAfter(fromMillis: Long, intervalMs: Long): Long {
        val interval = intervalMs.coerceAtLeast(60_000L)
        val startToday = calendarAt(fromMillis, startHour, startMinute)
        val endToday = calendarAt(fromMillis, endHour, endMinute)

        if (startMin <= endMin) {
            nextInSession(fromMillis, startToday.timeInMillis, endToday.timeInMillis, interval)
                ?.let { return it }
            startToday.add(Calendar.DAY_OF_YEAR, 1)
            return startToday.timeInMillis
        }

        val yesterdayStart = calendarAt(fromMillis, startHour, startMinute).apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        nextInSession(fromMillis, yesterdayStart.timeInMillis, endToday.timeInMillis, interval)
            ?.let { return it }
        val tomorrowEnd = calendarAt(fromMillis, endHour, endMinute).apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        nextInSession(fromMillis, startToday.timeInMillis, tomorrowEnd.timeInMillis, interval)
            ?.let { return it }
        startToday.add(Calendar.DAY_OF_YEAR, 1)
        return startToday.timeInMillis
    }

    companion object {
        const val FALLBACK_START_HOUR = 10
        const val FALLBACK_START_MINUTE = 0
        const val FALLBACK_END_HOUR = 22
        const val FALLBACK_END_MINUTE = 0
        const val DEFAULT_MORNING_HOUR = 10
        const val DEFAULT_MORNING_MINUTE = 0
        const val DEFAULT_SLEEP_HOUR = 23
        const val DEFAULT_SLEEP_MINUTE = 0
        const val DEFAULT_WAKE_HOUR = 8
        const val DEFAULT_WAKE_MINUTE = 0
        const val DEFAULT_INTERVAL_MINUTES = 25

        fun from(settings: SettingsRepository) = TasbihWindow(
            startHour = settings.tasbihStartHour,
            startMinute = settings.tasbihStartMinute,
            endHour = settings.tasbihEndHour,
            endMinute = settings.tasbihEndMinute
        )

        fun fallback() = TasbihWindow(
            FALLBACK_START_HOUR,
            FALLBACK_START_MINUTE,
            FALLBACK_END_HOUR,
            FALLBACK_END_MINUTE
        )

        private fun calendarAt(fromMillis: Long, hour: Int, minute: Int): Calendar =
            Calendar.getInstance().apply {
                timeInMillis = fromMillis
                set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
                set(Calendar.MINUTE, minute.coerceIn(0, 59))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
    }
}

private fun nextInSession(
    fromMillis: Long,
    origin: Long,
    endAt: Long,
    intervalMs: Long
): Long? {
    if (fromMillis < origin) return origin
    if (fromMillis >= endAt) return null
    val nextAligned = origin + ((fromMillis - origin) / intervalMs + 1) * intervalMs
    return if (nextAligned < endAt) nextAligned else endAt
}
