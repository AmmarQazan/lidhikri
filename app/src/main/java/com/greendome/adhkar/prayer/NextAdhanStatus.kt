package com.greendome.adhkar.prayer

enum class NextAdhanBodyKind {
    SOON,
    MINUTES,
    HOURS,
    HOURS_AND_MINUTES,
}

data class NextAdhanStatus(
    val prayer: PrayerName,
    val atMillis: Long,
    val minutesRemaining: Int,
) {
    val hoursRemaining: Int get() = minutesRemaining / 60
    val minutesPastHour: Int get() = minutesRemaining % 60

    fun bodyKind(): NextAdhanBodyKind = when {
        minutesRemaining <= 0 -> NextAdhanBodyKind.SOON
        hoursRemaining <= 0 -> NextAdhanBodyKind.MINUTES
        minutesPastHour == 0 -> NextAdhanBodyKind.HOURS
        else -> NextAdhanBodyKind.HOURS_AND_MINUTES
    }

    companion object {
        fun resolve(
            config: PrayerConfig,
            nowMillis: Long = System.currentTimeMillis(),
        ): NextAdhanStatus? {
            if (!config.hasTimes) return null
            val next = PrayerTimesCalculator.nextPrayer(config, nowMillis) ?: return null
            val minutes = ((next.epochMillis - nowMillis) / 60_000L).toInt().coerceAtLeast(0)
            return NextAdhanStatus(next.prayer, next.epochMillis, minutes)
        }
    }
}
