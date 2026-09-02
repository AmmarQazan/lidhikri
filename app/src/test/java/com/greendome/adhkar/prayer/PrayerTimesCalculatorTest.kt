package com.greendome.adhkar.prayer

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class PrayerTimesCalculatorTest {
    @After
    fun tearDown() {
        PrayerCountryDefaults.clearRemote()
    }

    private val riyadh = PrayerLocation(24.7136, 46.6753, "الرياض", "السعودية", "SA")

    private fun config(
        dst: DstMode = DstMode.AUTO,
        offsets: Map<PrayerName, Int> = emptyMap(),
        quiet: Map<PrayerName, Int> = emptyMap(),
        afterPrayer: Map<PrayerName, Int> = emptyMap(),
        jumuah: Int = 75,
        enabled: Boolean = true,
        afterPrayerReminder: Boolean = true,
        adhanEnabled: Boolean = true,
        alerts: Map<PrayerName, PrayerAlertSettings> = emptyMap(),
    ) = PrayerConfig(
        enabled = enabled,
        afterPrayerReminder = afterPrayerReminder,
        location = riyadh,
        locationMode = LocationMode.MANUAL,
        travelAutoUpdate = false,
        timezoneMode = TimezoneMode.MANUAL,
        timezoneId = "Asia/Riyadh",
        dstMode = dst,
        method = CalculationMethodPref.UMM_AL_QURA,
        madhab = AsrMadhabPref.SHAFI,
        minuteOffsets = offsets,
        quietMinutes = quiet,
        jumuahQuietMinutes = jumuah,
        afterPrayerMinutes = afterPrayer,
        adhanEnabled = adhanEnabled,
        alerts = alerts,
    )

    @Test
    fun riyadhTimesAreInDaylightHours() {
        val noon = Instant.parse("2026-06-15T09:00:00Z").toEpochMilli()
        val times = PrayerTimesCalculator.timesFor(config(), noon)!!
        val zone = ZoneId.of("Asia/Riyadh")
        val fajr = Instant.ofEpochMilli(times.timeOf(PrayerName.FAJR)!!).atZone(zone)
        val maghrib = Instant.ofEpochMilli(times.timeOf(PrayerName.MAGHRIB)!!).atZone(zone)
        val isha = Instant.ofEpochMilli(times.timeOf(PrayerName.ISHA)!!).atZone(zone)
        assertTrue(fajr.hour in 3..5)
        assertTrue(maghrib.hour in 18..20)
        assertTrue(isha.isAfter(maghrib))
    }

    @Test
    fun minuteOffsetShiftsPrayer() {
        val noon = Instant.parse("2026-06-15T09:00:00Z").toEpochMilli()
        val base = PrayerTimesCalculator.timesFor(config(), noon)!!.timeOf(PrayerName.DHUHR)!!
        val shifted = PrayerTimesCalculator.timesFor(
            config(offsets = mapOf(PrayerName.DHUHR to 5)),
            noon
        )!!.timeOf(PrayerName.DHUHR)!!
        assertEquals(5 * 60_000L, shifted - base)
    }

    @Test
    fun dstOnAddsHourWhenZoneHasNoDst() {
        val zone = ZoneId.of("Asia/Riyadh")
        val winter = Instant.parse("2026-01-15T12:00:00Z")
        val auto = PrayerTimesCalculator.applyDst(winter, zone, DstMode.AUTO)
        val on = PrayerTimesCalculator.applyDst(winter, zone, DstMode.ON)
        val off = PrayerTimesCalculator.applyDst(winter, zone, DstMode.OFF)
        assertEquals(auto, off)
        assertEquals(auto.plus(1, ChronoUnit.HOURS), on)
    }

    @Test
    fun quietWindowCoversAdhanAndEnds() {
        val noon = Instant.parse("2026-06-15T09:00:00Z").toEpochMilli()
        val cfg = config(quiet = mapOf(PrayerName.DHUHR to 30))
        val dhuhr = PrayerTimesCalculator.timesFor(cfg, noon)!!.timeOf(PrayerName.DHUHR)!!
        assertTrue(PrayerQuietWindows.isQuiet(cfg, dhuhr + 10 * 60_000L))
        assertFalse(PrayerQuietWindows.isQuiet(cfg, dhuhr + 31 * 60_000L))
        assertEquals(dhuhr + 30 * 60_000L, PrayerQuietWindows.nextQuietEndAfter(cfg, dhuhr + 1_000L))
    }

    @Test
    fun delayPastQuietMovesTriggerToWindowEnd() {
        val noon = Instant.parse("2026-06-15T09:00:00Z").toEpochMilli()
        val cfg = config(quiet = mapOf(PrayerName.ASR to 25))
        val asr = PrayerTimesCalculator.timesFor(cfg, noon)!!.timeOf(PrayerName.ASR)!!
        val delayed = PrayerQuietWindows.delayPastQuiet(cfg, asr + 60_000L)
        assertEquals(asr + 25 * 60_000L + 1_000L, delayed)
    }

    @Test
    fun tasbihAtQuietEndCollidesWhenAfterPrayerDelayMatchesQuiet() {
        val noon = Instant.parse("2026-06-15T09:00:00Z").toEpochMilli()
        val cfg = config(quiet = mapOf(PrayerName.DHUHR to 30))
        val dhuhr = PrayerTimesCalculator.timesFor(cfg, noon)!!.timeOf(PrayerName.DHUHR)!!
        val delayed = PrayerQuietWindows.delayPastQuiet(cfg, dhuhr + 60_000L)
        assertTrue(PrayerQuietWindows.collidesWithAfterPrayer(cfg, delayed))
        assertFalse(PrayerQuietWindows.collidesWithAfterPrayer(cfg, delayed + 5 * 60_000L))
    }

    @Test
    fun tasbihCollidesInSameMinuteAndGraceAfterAfterPrayer() {
        val noon = Instant.parse("2026-06-15T09:00:00Z").toEpochMilli()
        val cfg = config(
            quiet = mapOf(PrayerName.ASR to 25),
            afterPrayer = mapOf(PrayerName.ASR to 25)
        )
        val asr = PrayerTimesCalculator.timesFor(cfg, noon)!!.timeOf(PrayerName.ASR)!!
        val afterAt = asr + 25 * 60_000L
        val zone = ZoneId.of("Asia/Riyadh")
        val minuteStart = Instant.ofEpochMilli(afterAt).atZone(zone)
            .withSecond(0)
            .withNano(0)
            .toInstant()
            .toEpochMilli()
        assertTrue(PrayerQuietWindows.collidesWithAfterPrayer(cfg, afterAt))
        assertTrue(PrayerQuietWindows.collidesWithAfterPrayer(cfg, minuteStart))
        assertTrue(PrayerQuietWindows.collidesWithAfterPrayer(cfg, afterAt + 20_000L))
        assertFalse(PrayerQuietWindows.collidesWithAfterPrayer(cfg, afterAt + 90_000L))
    }

    @Test
    fun tasbihAtQuietEndDoesNotCollideWhenAfterPrayerIsLater() {
        val noon = Instant.parse("2026-06-15T09:00:00Z").toEpochMilli()
        val cfg = config(
            quiet = mapOf(PrayerName.DHUHR to 20),
            afterPrayer = mapOf(PrayerName.DHUHR to 45)
        )
        val dhuhr = PrayerTimesCalculator.timesFor(cfg, noon)!!.timeOf(PrayerName.DHUHR)!!
        val delayed = PrayerQuietWindows.delayPastQuiet(cfg, dhuhr + 60_000L)
        assertFalse(PrayerQuietWindows.collidesWithAfterPrayer(cfg, delayed))
        val afterAt = dhuhr + 45 * 60_000L
        assertTrue(PrayerQuietWindows.collidesWithAfterPrayer(cfg, afterAt + 1_000L))
    }

    @Test
    fun afterPrayerCollisionDisabledWhenReminderOff() {
        val noon = Instant.parse("2026-06-15T09:00:00Z").toEpochMilli()
        val cfg = config(
            quiet = mapOf(PrayerName.DHUHR to 30),
            afterPrayerReminder = false
        )
        val dhuhr = PrayerTimesCalculator.timesFor(cfg, noon)!!.timeOf(PrayerName.DHUHR)!!
        val delayed = PrayerQuietWindows.delayPastQuiet(cfg, dhuhr + 60_000L)
        assertFalse(PrayerQuietWindows.collidesWithAfterPrayer(cfg, delayed))
    }

    @Test
    fun tasbihAndAzkarYieldToExactAdhanMinute() {
        val noon = Instant.parse("2026-06-15T09:00:00Z").toEpochMilli()
        val cfg = config()
        val dhuhr = PrayerTimesCalculator.timesFor(cfg, noon)!!.timeOf(PrayerName.DHUHR)!!
        val zone = ZoneId.of("Asia/Riyadh")
        val minuteStart = Instant.ofEpochMilli(dhuhr).atZone(zone)
            .withSecond(0)
            .withNano(0)
            .toInstant()
            .toEpochMilli()
        assertTrue(PrayerQuietWindows.collidesWithAdhan(cfg, dhuhr))
        assertTrue(PrayerQuietWindows.collidesWithAdhan(cfg, minuteStart))
        assertTrue(PrayerQuietWindows.collidesWithAdhan(cfg, dhuhr - 2_000L))
        assertTrue(PrayerQuietWindows.collidesWithAdhan(cfg, dhuhr + 20_000L))
        assertFalse(PrayerQuietWindows.collidesWithAdhan(cfg, dhuhr + 90_000L))
        val delayed = PrayerQuietWindows.delayPastAdhan(cfg, minuteStart)
        assertTrue(delayed > dhuhr)
        assertFalse(PrayerQuietWindows.collidesWithAdhan(cfg, delayed))
    }

    @Test
    fun adhanCollisionOffWhenAdhanDisabled() {
        val noon = Instant.parse("2026-06-15T09:00:00Z").toEpochMilli()
        val cfg = config(adhanEnabled = false)
        val dhuhr = PrayerTimesCalculator.timesFor(cfg, noon)!!.timeOf(PrayerName.DHUHR)!!
        assertFalse(PrayerQuietWindows.collidesWithAdhan(cfg, dhuhr))
    }

    @Test
    fun adhanCollisionOffWhenThatPrayerAdhanIsOff() {
        val noon = Instant.parse("2026-06-15T09:00:00Z").toEpochMilli()
        val cfg = config(
            alerts = mapOf(PrayerName.DHUHR to PrayerAlertSettings(adhanEnabled = false)),
        )
        val dhuhr = PrayerTimesCalculator.timesFor(cfg, noon)!!.timeOf(PrayerName.DHUHR)!!
        val asr = PrayerTimesCalculator.timesFor(cfg, noon)!!.timeOf(PrayerName.ASR)!!
        assertFalse(PrayerQuietWindows.collidesWithAdhan(cfg, dhuhr))
        assertTrue(PrayerQuietWindows.collidesWithAdhan(cfg, asr))
    }

    @Test
    fun countryDefaultsPickUmmAlQuraForSaudi() {
        assertEquals(CalculationMethodPref.UMM_AL_QURA, PrayerCountryDefaults.methodFor("SA"))
        assertEquals(AsrMadhabPref.HANAFI, PrayerCountryDefaults.madhabFor("PK"))
        assertEquals(AsrMadhabPref.SHAFI, PrayerCountryDefaults.madhabFor("JO"))
        assertEquals(CalculationMethodPref.MUSLIM_WORLD_LEAGUE, PrayerCountryDefaults.methodFor("MR"))
        assertEquals(AsrMadhabPref.SHAFI, PrayerCountryDefaults.madhabFor("SO"))
        assertEquals("Africa/Nouakchott", PrayerCountryDefaults.timezoneIdFor("MR"))
        assertEquals("Africa/Mogadishu", PrayerCountryDefaults.timezoneIdFor("SO"))
        assertEquals("Africa/Djibouti", PrayerCountryDefaults.timezoneIdFor("DJ"))
        assertEquals("Indian/Comoro", PrayerCountryDefaults.timezoneIdFor("KM"))
        assertEquals("Asia/Riyadh", PrayerCountryDefaults.timezoneIdFor("SA"))
    }

    @Test
    fun remoteOverlayOverridesBuiltinForThatCountryOnly() {
        try {
            PrayerCountryDefaults.applyRemote(
                PrayerDefaultsTable(
                    version = 1,
                    countries = mapOf(
                        "SA" to CountryPrayerOverride(
                            method = CalculationMethodPref.EGYPTIAN,
                            madhab = AsrMadhabPref.HANAFI,
                            timezone = "Asia/Riyadh",
                            dst = DstMode.OFF
                        )
                    )
                )
            )
            assertEquals(CalculationMethodPref.EGYPTIAN, PrayerCountryDefaults.methodFor("SA"))
            assertEquals(AsrMadhabPref.HANAFI, PrayerCountryDefaults.madhabFor("SA"))
            assertEquals(DstMode.OFF, PrayerCountryDefaults.dstFor("SA"))
            assertEquals(AsrMadhabPref.HANAFI, PrayerCountryDefaults.madhabFor("PK"))
            assertEquals(AsrMadhabPref.SHAFI, PrayerCountryDefaults.madhabFor("EG"))
        } finally {
            PrayerCountryDefaults.clearRemote()
        }
    }
}
