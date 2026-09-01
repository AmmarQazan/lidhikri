package com.greendome.adhkar.prayer

import java.time.Instant
import java.util.Calendar
import java.util.TimeZone

object PrayerQuietWindows {
    const val AFTER_PRAYER_TASBIH_GRACE_MS = 30_000L

    fun windowsAround(
        config: PrayerConfig,
        atMillis: Long = System.currentTimeMillis()
    ): List<QuietWindow> {
        if (!config.enabled || !config.hasLocation) return emptyList()
        val zone = TimeZone.getTimeZone(PrayerTimesCalculator.zoneId(config))
        return PrayerTimesCalculator.timesAround(config, atMillis).flatMap { day ->
            val cal = Calendar.getInstance(zone).apply { timeInMillis = day.dayStartMillis }
            val friday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
            day.prayers.map { instant ->
                val durationMin = config.quietDuration(instant.prayer, friday)
                QuietWindow(
                    prayer = instant.prayer,
                    startMillis = instant.epochMillis,
                    endMillis = instant.epochMillis + durationMin * 60_000L
                )
            }
        }.sortedBy { it.startMillis }
    }

    fun activeWindow(
        config: PrayerConfig,
        atMillis: Long = System.currentTimeMillis()
    ): QuietWindow? = windowsAround(config, atMillis).firstOrNull { it.contains(atMillis) }

    fun isQuiet(
        config: PrayerConfig,
        atMillis: Long = System.currentTimeMillis()
    ): Boolean = activeWindow(config, atMillis) != null

    fun nextQuietEndAfter(
        config: PrayerConfig,
        fromMillis: Long = System.currentTimeMillis()
    ): Long? {
        val windows = windowsAround(config, fromMillis)
        val current = windows.firstOrNull { it.contains(fromMillis) }
        if (current != null && current.endMillis > fromMillis) return current.endMillis
        return windows.firstOrNull { it.endMillis > fromMillis }?.endMillis
    }

    fun delayPastQuiet(
        config: PrayerConfig,
        triggerAt: Long
    ): Long {
        val window = windowsAround(config, triggerAt).firstOrNull { it.contains(triggerAt) }
            ?: return triggerAt
        return window.endMillis + 1_000L
    }

    /** بداية الإيقاف التالي أو نهايته الحالية — لتحديث التنبيه فوراً. */
    fun nextStatusChangeAt(
        config: PrayerConfig,
        fromMillis: Long = System.currentTimeMillis()
    ): Long? {
        val windows = windowsAround(config, fromMillis)
        val current = windows.firstOrNull { it.contains(fromMillis) }
        if (current != null) return current.endMillis
        return windows.firstOrNull { it.startMillis > fromMillis }?.startMillis
    }

    fun nextAfterPrayerTriggerAt(
        config: PrayerConfig,
        fromMillis: Long = System.currentTimeMillis()
    ): Long? = afterPrayerTriggersAround(config, fromMillis)
        .filter { it > fromMillis }
        .minOrNull()

    /**
     * موعد التسبيح في نفس دقيقة أذكار ما بعد الصلاة، أو خلال مهلة قصيرة بعدها.
     * يمنع انطلاق التسبيحة مع ذكر ما بعد الصلاة بعد انتهاء احترام الصلاة.
     */
    fun collidesWithAfterPrayer(
        config: PrayerConfig,
        triggerAt: Long,
        graceAfterMs: Long = AFTER_PRAYER_TASBIH_GRACE_MS
    ): Boolean {
        val zoneId = PrayerTimesCalculator.zoneId(config)
        val triggerLocal = Instant.ofEpochMilli(triggerAt).atZone(zoneId)
        return afterPrayerTriggersAround(config, triggerAt).any { afterAt ->
            if (triggerAt in afterAt..(afterAt + graceAfterMs)) return@any true
            val afterLocal = Instant.ofEpochMilli(afterAt).atZone(zoneId)
            triggerLocal.toLocalDate() == afterLocal.toLocalDate() &&
                triggerLocal.hour == afterLocal.hour &&
                triggerLocal.minute == afterLocal.minute
        }
    }

    private fun afterPrayerTriggersAround(config: PrayerConfig, atMillis: Long): List<Long> {
        if (!config.enabled || !config.afterPrayerReminder || !config.hasLocation) return emptyList()
        val zone = TimeZone.getTimeZone(PrayerTimesCalculator.zoneId(config))
        return PrayerTimesCalculator.timesAround(config, atMillis).flatMap { day ->
            val cal = Calendar.getInstance(zone).apply { timeInMillis = day.dayStartMillis }
            val friday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
            day.prayers.map { instant ->
                instant.epochMillis + config.afterPrayerDelay(instant.prayer, friday) * 60_000L
            }
        }
    }
}

fun formatPrayerClock(epochMillis: Long, zoneId: java.time.ZoneId): String {
    val local = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
    return "%02d:%02d".format(local.hour, local.minute)
}
