package com.greendome.adhkar.prayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class AdhanEventsTest {
    private val riyadh = PrayerLocation(24.7136, 46.6753, "الرياض", "السعودية", "SA")

    private fun config(
        adhan: Boolean = true,
        fajrBefore: Int = 10,
        dhuhrIqama: Int = 15,
        fajrOn: Boolean = true,
    ) = PrayerConfig(
        enabled = true,
        afterPrayerReminder = true,
        location = riyadh,
        locationMode = LocationMode.MANUAL,
        travelAutoUpdate = false,
        timezoneMode = TimezoneMode.MANUAL,
        timezoneId = "Asia/Riyadh",
        dstMode = DstMode.AUTO,
        method = CalculationMethodPref.UMM_AL_QURA,
        madhab = AsrMadhabPref.SHAFI,
        minuteOffsets = emptyMap(),
        quietMinutes = emptyMap(),
        jumuahQuietMinutes = 55,
        timesEnabled = true,
        adhanEnabled = adhan,
        alerts = mapOf(
            PrayerName.FAJR to PrayerAlertSettings(
                adhanEnabled = fajrOn,
                notifyBeforeMinutes = fajrBefore,
                overrideSilent = true,
            ),
            PrayerName.DHUHR to PrayerAlertSettings(iqamaMinutes = dhuhrIqama),
        )
    )

    @Test
    fun nextEventIsBeforeAdhanWhenConfigured() {
        val noon = Instant.parse("2026-06-15T00:05:00Z").toEpochMilli()
        val cfg = config()
        val fajr = PrayerTimesCalculator.timesFor(cfg, noon)!!.timeOf(PrayerName.FAJR)!!
        val next = AdhanEvents.next(cfg, fajr - 11 * 60_000L)!!
        assertEquals(PrayerName.FAJR, next.prayer)
        assertEquals(AdhanEventKind.PRE, next.kind)
        assertEquals(fajr - 10 * 60_000L, next.atMillis)
    }

    @Test
    fun iqamaFollowsAdhan() {
        val noon = Instant.parse("2026-06-15T09:00:00Z").toEpochMilli()
        val cfg = config()
        val dhuhr = PrayerTimesCalculator.timesFor(cfg, noon)!!.timeOf(PrayerName.DHUHR)!!
        val next = AdhanEvents.next(cfg, dhuhr + 1_000L)!!
        assertEquals(PrayerName.DHUHR, next.prayer)
        assertEquals(AdhanEventKind.IQAMA, next.kind)
        assertEquals(dhuhr + 15 * 60_000L, next.atMillis)
    }

    @Test
    fun nextFromEventTimeSkipsThatAdhanEvenIfClockIsEarly() {
        val noon = Instant.parse("2026-06-15T00:05:00Z").toEpochMilli()
        val cfg = config()
        val fajr = PrayerTimesCalculator.timesFor(cfg, noon)!!.timeOf(PrayerName.FAJR)!!
        val twoSecondsEarly = fajr - 2_000L
        val nextIfUsingNow = AdhanEvents.next(cfg, twoSecondsEarly)!!
        assertEquals(PrayerName.FAJR, nextIfUsingNow.prayer)
        assertEquals(AdhanEventKind.ADHAN, nextIfUsingNow.kind)
        assertEquals(fajr, nextIfUsingNow.atMillis)

        val nextAfterFired = AdhanEvents.next(cfg, fajr)!!
        assertTrue(nextAfterFired.atMillis > fajr)
    }

    @Test
    fun skipAlreadyFiredPreEvenIfClockIsStillBeforeIt() {
        val noon = Instant.parse("2026-06-15T00:05:00Z").toEpochMilli()
        val cfg = config()
        val fajr = PrayerTimesCalculator.timesFor(cfg, noon)!!.timeOf(PrayerName.FAJR)!!
        val preAt = fajr - 10 * 60_000L
        val twoSecondsEarly = preAt - 2_000L
        val next = AdhanEvents.next(
            cfg,
            twoSecondsEarly,
            skip = AdhanEvent(PrayerName.FAJR, AdhanEventKind.PRE, preAt),
        )!!
        assertEquals(PrayerName.FAJR, next.prayer)
        assertEquals(AdhanEventKind.ADHAN, next.kind)
        assertEquals(fajr, next.atMillis)
    }

    @Test
    fun sameOccurrenceIsOncePerPrayerKindAndDay() {
        val zone = ZoneId.of("Asia/Riyadh")
        val first = AdhanEvent(PrayerName.ASR, AdhanEventKind.PRE, 1_000_000L)
        val laterSameDay = AdhanEvent(PrayerName.ASR, AdhanEventKind.PRE, 1_000_000L + 30_000L)
        assertTrue(AdhanEvents.sameOccurrence(first, laterSameDay, zone))
        assertFalse(
            AdhanEvents.sameOccurrence(
                first,
                AdhanEvent(PrayerName.ASR, AdhanEventKind.PRE, 1_000_000L + 86_400_000L),
                zone,
            )
        )
        assertFalse(
            AdhanEvents.sameOccurrence(
                first,
                AdhanEvent(PrayerName.ASR, AdhanEventKind.ADHAN, 1_000_000L),
                zone,
            )
        )
    }

    @Test
    fun sunriseAndImsakArePresent() {
        val noon = Instant.parse("2026-06-15T09:00:00Z").toEpochMilli()
        val times = PrayerTimesCalculator.timesFor(config(), noon)!!
        val fajr = times.timeOf(PrayerName.FAJR)!!
        assertNotNull(times.sunriseMillis)
        assertNotNull(times.imsakMillis)
        assertTrue(times.imsakMillis!! < fajr)
        assertTrue(times.sunriseMillis!! > fajr)
    }

    @Test
    fun qiblaFromRiyadhPointsWestOfSouth() {
        val bearing = QiblaCalculator.bearing(24.7136, 46.6753)
        assertTrue(bearing > 240f && bearing < 290f)
        assertTrue(QiblaCalculator.distanceKm(24.7136, 46.6753) in 700.0..1000.0)
    }
}
