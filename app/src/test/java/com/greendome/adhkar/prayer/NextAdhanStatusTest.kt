package com.greendome.adhkar.prayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class NextAdhanStatusTest {
    private val riyadh = PrayerLocation(24.7136, 46.6753, "الرياض", "السعودية", "SA")

    private fun config(
        location: PrayerLocation? = riyadh,
        timesEnabled: Boolean = true,
    ) = PrayerConfig(
        enabled = true,
        afterPrayerReminder = true,
        location = location,
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
        timesEnabled = timesEnabled,
        adhanEnabled = true,
    )

    @Test
    fun minutesMatchNextPrayer() {
        val noon = Instant.parse("2026-06-15T09:00:00Z").toEpochMilli()
        val cfg = config()
        val asr = PrayerTimesCalculator.timesFor(cfg, noon)!!.timeOf(PrayerName.ASR)!!
        val from = asr - 70 * 60_000L
        val status = NextAdhanStatus.resolve(cfg, from)!!
        assertEquals(PrayerName.ASR, status.prayer)
        assertEquals(asr, status.atMillis)
        assertEquals(70, status.minutesRemaining)
    }

    @Test
    fun underOneMinuteUsesZero() {
        val noon = Instant.parse("2026-06-15T09:00:00Z").toEpochMilli()
        val cfg = config()
        val dhuhr = PrayerTimesCalculator.timesFor(cfg, noon)!!.timeOf(PrayerName.DHUHR)!!
        val status = NextAdhanStatus.resolve(cfg, dhuhr - 15_000L)!!
        assertEquals(PrayerName.DHUHR, status.prayer)
        assertEquals(0, status.minutesRemaining)
    }

    @Test
    fun missingCityHidesStatus() {
        val noon = Instant.parse("2026-06-15T09:00:00Z").toEpochMilli()
        assertNull(NextAdhanStatus.resolve(config(location = null), noon))
    }

    @Test
    fun timesDisabledHidesStatus() {
        val noon = Instant.parse("2026-06-15T09:00:00Z").toEpochMilli()
        assertNull(NextAdhanStatus.resolve(config(timesEnabled = false), noon))
    }
}
