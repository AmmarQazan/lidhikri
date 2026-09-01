package com.greendome.adhkar.prayer

data class NextAdhanStatus(
    val prayer: PrayerName,
    val atMillis: Long,
    val minutesRemaining: Int,
) {
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
