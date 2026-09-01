package com.greendome.adhkar.prayer

import java.time.Instant
import java.time.ZoneId

object AdhanEvents {
    fun upcoming(
        config: PrayerConfig,
        fromMillis: Long = System.currentTimeMillis(),
        skip: AdhanEvent? = null,
    ): List<AdhanEvent> {
        if (!config.adhanActive) return emptyList()
        val zone = PrayerTimesCalculator.zoneId(config)
        return PrayerTimesCalculator.timesAround(config, fromMillis).flatMap { day ->
            day.prayers.flatMap { instant ->
                val alert = config.alert(instant.prayer)
                if (!alert.adhanEnabled) return@flatMap emptyList()
                buildList {
                    if (alert.notifyBeforeMinutes > 0) {
                        add(
                            AdhanEvent(
                                instant.prayer,
                                AdhanEventKind.PRE,
                                instant.epochMillis - alert.notifyBeforeMinutes * 60_000L,
                            )
                        )
                    }
                    add(AdhanEvent(instant.prayer, AdhanEventKind.ADHAN, instant.epochMillis))
                    if (alert.iqamaMinutes > 0) {
                        add(
                            AdhanEvent(
                                instant.prayer,
                                AdhanEventKind.IQAMA,
                                instant.epochMillis + alert.iqamaMinutes * 60_000L,
                            )
                        )
                    }
                }
            }
        }.filter { it.atMillis > fromMillis }
            .filter { skip == null || !sameOccurrence(it, skip, zone) }
            .sortedBy { it.atMillis }
    }

    fun next(
        config: PrayerConfig,
        fromMillis: Long = System.currentTimeMillis(),
        skip: AdhanEvent? = null,
    ): AdhanEvent? = upcoming(config, fromMillis, skip).firstOrNull()

    fun nextAdhan(
        config: PrayerConfig,
        fromMillis: Long = System.currentTimeMillis(),
    ): AdhanEvent? = upcoming(config, fromMillis).firstOrNull { it.kind == AdhanEventKind.ADHAN }

    fun sameOccurrence(a: AdhanEvent, b: AdhanEvent, zone: ZoneId): Boolean {
        if (a.prayer != b.prayer || a.kind != b.kind) return false
        val dayA = Instant.ofEpochMilli(a.atMillis).atZone(zone).toLocalDate()
        val dayB = Instant.ofEpochMilli(b.atMillis).atZone(zone).toLocalDate()
        return dayA == dayB
    }
}
