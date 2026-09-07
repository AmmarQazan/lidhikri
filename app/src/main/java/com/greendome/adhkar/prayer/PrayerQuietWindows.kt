package com.greendome.adhkar.prayer

import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.TimeZone

object PrayerQuietWindows {
    const val AFTER_PRAYER_TASBIH_GRACE_MS = 30_000L
    const val ADHAN_PRIORITY_GRACE_BEFORE_MS = 5_000L
    const val ADHAN_PRIORITY_GRACE_AFTER_MS = 30_000L

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

    /** نهاية فترة الفرض — موعد أذكار ما بعد الصلاة، حتى إن كان التذكير موقوفاً. */
    fun nextPhoneSilentEndAt(
        config: PrayerConfig,
        fromMillis: Long = System.currentTimeMillis()
    ): Long? = prayerPeriodEndsAround(config, fromMillis)
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
        return afterPrayerTriggersAround(config, triggerAt).any { afterAt ->
            collidesWithClock(
                triggerAt = triggerAt,
                eventAt = afterAt,
                zoneId = zoneId,
                graceBeforeMs = 0L,
                graceAfterMs = graceAfterMs,
            )
        }
    }

    /**
     * نفس دقيقة الأذان، أو مهلة قصيرة قبله وبعده.
     * يمنع انطلاق التسبيح أو الذكر التلقائي مع الأذان — الأولوية للأذان.
     */
    fun collidesWithAdhan(
        config: PrayerConfig,
        triggerAt: Long = System.currentTimeMillis(),
        graceBeforeMs: Long = ADHAN_PRIORITY_GRACE_BEFORE_MS,
        graceAfterMs: Long = ADHAN_PRIORITY_GRACE_AFTER_MS,
    ): Boolean {
        if (!config.adhanActive) return false
        val zoneId = PrayerTimesCalculator.zoneId(config)
        return adhanTimesAround(config, triggerAt).any { adhanAt ->
            collidesWithClock(
                triggerAt = triggerAt,
                eventAt = adhanAt,
                zoneId = zoneId,
                graceBeforeMs = graceBeforeMs,
                graceAfterMs = graceAfterMs,
            )
        }
    }

    /** أول لحظة بعد دقيقة الأذان ومهلة الأولوية. */
    fun delayPastAdhan(
        config: PrayerConfig,
        triggerAt: Long,
        graceBeforeMs: Long = ADHAN_PRIORITY_GRACE_BEFORE_MS,
        graceAfterMs: Long = ADHAN_PRIORITY_GRACE_AFTER_MS,
    ): Long {
        if (!collidesWithAdhan(config, triggerAt, graceBeforeMs, graceAfterMs)) return triggerAt
        val zoneId = PrayerTimesCalculator.zoneId(config)
        val ends = adhanTimesAround(config, triggerAt).mapNotNull { adhanAt ->
            if (!collidesWithClock(triggerAt, adhanAt, zoneId, graceBeforeMs, graceAfterMs)) {
                return@mapNotNull null
            }
            val minuteEnd = Instant.ofEpochMilli(adhanAt).atZone(zoneId)
                .plusMinutes(1)
                .withSecond(0)
                .withNano(0)
                .toInstant()
                .toEpochMilli()
            maxOf(minuteEnd, adhanAt + graceAfterMs + 1L)
        }
        return ends.maxOrNull()?.coerceAtLeast(triggerAt + 1L) ?: triggerAt
    }

    private fun collidesWithClock(
        triggerAt: Long,
        eventAt: Long,
        zoneId: ZoneId,
        graceBeforeMs: Long,
        graceAfterMs: Long,
    ): Boolean {
        if (triggerAt in (eventAt - graceBeforeMs)..(eventAt + graceAfterMs)) return true
        val triggerLocal = Instant.ofEpochMilli(triggerAt).atZone(zoneId)
        val eventLocal = Instant.ofEpochMilli(eventAt).atZone(zoneId)
        return triggerLocal.toLocalDate() == eventLocal.toLocalDate() &&
            triggerLocal.hour == eventLocal.hour &&
            triggerLocal.minute == eventLocal.minute
    }

    private fun adhanTimesAround(config: PrayerConfig, atMillis: Long): List<Long> {
        if (!config.adhanActive) return emptyList()
        return PrayerTimesCalculator.timesAround(config, atMillis).flatMap { day ->
            day.prayers.mapNotNull { instant ->
                if (!config.alert(instant.prayer).adhanEnabled) null else instant.epochMillis
            }
        }
    }

    private fun afterPrayerTriggersAround(config: PrayerConfig, atMillis: Long): List<Long> {
        if (!config.afterPrayerReminder) return emptyList()
        return prayerPeriodEndsAround(config, atMillis)
    }

    private fun prayerPeriodEndsAround(config: PrayerConfig, atMillis: Long): List<Long> {
        if (!config.enabled || !config.hasLocation) return emptyList()
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
