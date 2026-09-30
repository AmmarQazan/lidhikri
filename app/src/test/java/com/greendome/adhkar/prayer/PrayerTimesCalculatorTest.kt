package com.greendome.adhkar.prayer

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun phoneSilentEndMatchesAfterPrayerEvenIfReminderOff() {
        val noon = Instant.parse("2026-06-15T09:00:00Z").toEpochMilli()
        val cfg = config(
            quiet = mapOf(PrayerName.DHUHR to 30),
            afterPrayer = mapOf(PrayerName.DHUHR to 40),
            afterPrayerReminder = false
        )
        val dhuhr = PrayerTimesCalculator.timesFor(cfg, noon)!!.timeOf(PrayerName.DHUHR)!!
        val expected = dhuhr + 40 * 60_000L
        assertEquals(expected, PrayerQuietWindows.nextPhoneSilentEndAt(cfg, dhuhr + 1_000L))
        assertEquals(expected, PrayerQuietWindows.activePhoneSilentEndAt(cfg, dhuhr + 1_000L))
        assertTrue(PrayerQuietWindows.nextAfterPrayerTriggerAt(cfg, dhuhr + 1_000L) == null)
    }

    @Test
    fun earlyAfterPrayerAlarmSkipsTheTriggerJustDue() {
        val noon = Instant.parse("2026-06-15T09:00:00Z").toEpochMilli()
        val cfg = config(
            afterPrayer = mapOf(PrayerName.DHUHR to 30, PrayerName.ASR to 30),
        )
        val day = PrayerTimesCalculator.timesFor(cfg, noon)!!
        val dhuhrEnd = day.timeOf(PrayerName.DHUHR)!! + 30 * 60_000L
        val asrEnd = day.timeOf(PrayerName.ASR)!! + 30 * 60_000L
        val early = dhuhrEnd - 1_000L
        assertEquals(dhuhrEnd, PrayerQuietWindows.afterPrayerTriggerJustDue(cfg, early))
        assertEquals(dhuhrEnd, PrayerQuietWindows.nextAfterPrayerTriggerAt(cfg, early))
        assertEquals(
            asrEnd,
            PrayerQuietWindows.nextAfterPrayerTriggerAt(cfg, early, notBefore = dhuhrEnd),
        )
    }

    @Test
    fun phoneSilentDoesNotExtendUntilNextPrayer() {
        val noon = Instant.parse("2026-06-15T09:00:00Z").toEpochMilli()
        val cfg = config(
            quiet = mapOf(PrayerName.DHUHR to 30),
            afterPrayer = mapOf(PrayerName.DHUHR to 30, PrayerName.ASR to 30),
        )
        val day = PrayerTimesCalculator.timesFor(cfg, noon)!!
        val dhuhr = day.timeOf(PrayerName.DHUHR)!!
        val asr = day.timeOf(PrayerName.ASR)!!
        val windowEnd = dhuhr + 30 * 60_000L
        val between = windowEnd + 60_000L
        assertTrue(between < asr)
        assertNull(PrayerQuietWindows.activePhoneSilentEndAt(cfg, between))
        val nextEnd = PrayerQuietWindows.nextPhoneSilentEndAt(cfg, between)
        assertTrue(nextEnd != null && nextEnd >= asr + 30 * 60_000L)
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
        assertEquals(AsrMadhabPref.SHAFI, PrayerCountryDefaults.madhabFor("IQ"))
        assertEquals(AsrMadhabPref.SHAFI, PrayerCountryDefaults.madhabFor("SY"))
        assertEquals(CalculationMethodPref.MUSLIM_WORLD_LEAGUE, PrayerCountryDefaults.methodFor("MR"))
        assertEquals(AsrMadhabPref.SHAFI, PrayerCountryDefaults.madhabFor("SO"))
        assertEquals("Africa/Nouakchott", PrayerCountryDefaults.timezoneIdFor("MR"))
        assertEquals("Africa/Mogadishu", PrayerCountryDefaults.timezoneIdFor("SO"))
        assertEquals("Africa/Djibouti", PrayerCountryDefaults.timezoneIdFor("DJ"))
        assertEquals("Indian/Comoro", PrayerCountryDefaults.timezoneIdFor("KM"))
        assertEquals("Asia/Riyadh", PrayerCountryDefaults.timezoneIdFor("SA"))
        assertEquals("Asia/Tokyo", PrayerCountryDefaults.timezoneIdFor("JP"))
        assertEquals("Africa/Lagos", PrayerCountryDefaults.timezoneIdFor("NG"))
        assertEquals("America/Sao_Paulo", PrayerCountryDefaults.timezoneIdFor("BR"))
        assertEquals("Africa/Johannesburg", PrayerCountryDefaults.timezoneIdFor("ZA"))
        assertEquals("Asia/Tashkent", PrayerCountryDefaults.timezoneIdFor("UZ"))
        assertEquals(AsrMadhabPref.HANAFI, PrayerCountryDefaults.madhabFor("UZ"))
        assertEquals(AsrMadhabPref.HANAFI, PrayerCountryDefaults.madhabFor("KZ"))
        assertEquals(AsrMadhabPref.HANAFI, PrayerCountryDefaults.madhabFor("RU"))
        assertEquals(AsrMadhabPref.HANAFI, PrayerCountryDefaults.madhabFor("BA"))
        assertEquals(AsrMadhabPref.HANAFI, PrayerCountryDefaults.madhabFor("AL"))
        assertEquals(AsrMadhabPref.HANAFI, PrayerCountryDefaults.madhabFor("XK"))
        assertEquals(AsrMadhabPref.HANAFI, PrayerCountryDefaults.madhabFor("CN"))
        assertEquals(AsrMadhabPref.SHAFI, PrayerCountryDefaults.madhabFor("EG"))
        assertEquals(AsrMadhabPref.SHAFI, PrayerCountryDefaults.madhabFor("IR"))
        assertEquals(AsrMadhabPref.SHAFI, PrayerCountryDefaults.madhabFor("GB"))
        assertEquals(AsrMadhabPref.SHAFI, PrayerCountryDefaults.madhabFor("ID"))
        assertEquals(CalculationMethodPref.MUSLIM_WORLD_LEAGUE, PrayerCountryDefaults.methodFor("JP"))
        assertEquals(CalculationMethodPref.TURKEY, PrayerCountryDefaults.methodFor("TR"))
        assertEquals(CalculationMethodPref.MOROCCO, PrayerCountryDefaults.methodFor("MA"))
        assertEquals(CalculationMethodPref.OMAN, PrayerCountryDefaults.methodFor("OM"))
        assertEquals(CalculationMethodPref.TEHRAN, PrayerCountryDefaults.methodFor("IR"))
        assertEquals(CalculationMethodPref.DUBAI, PrayerCountryDefaults.methodFor("AE"))
    }

    @Test
    fun officialProfilesMatchCountryCalendars() {
        val amman = PrayerLocation(31.9539, 35.9106, "عمّان", "الأردن", "JO")
        val ammanNoon = Instant.parse("2026-09-30T09:00:00Z").toEpochMilli()
        val jordan = PrayerTimesCalculator.timesFor(autoConfig(amman), ammanNoon)!!
        val ammanZone = ZoneId.of("Asia/Amman")
        assertNear(jordan.timeOf(PrayerName.FAJR)!!, ammanZone, 5, 8)
        assertNear(jordan.timeOf(PrayerName.DHUHR)!!, ammanZone, 12, 27)
        assertNear(jordan.timeOf(PrayerName.MAGHRIB)!!, ammanZone, 18, 29)
        assertNear(jordan.timeOf(PrayerName.ISHA)!!, ammanZone, 19, 44)

        val dubai = PrayerLocation(25.2048, 55.2708, "دبي", "الإمارات", "AE")
        val dubaiParams = PrayerTimesCalculator.parametersFor(autoConfig(dubai), dubai)
        assertEquals(-3, dubaiParams.methodAdjustments.sunrise)
        assertEquals(3, dubaiParams.methodAdjustments.dhuhr)
        assertEquals(3, dubaiParams.methodAdjustments.asr)
        assertEquals(3, dubaiParams.methodAdjustments.maghrib)

        val istanbul = PrayerLocation(41.0082, 28.9784, "إسطنبول", "تركيا", "TR")
        val turkey = PrayerTimesCalculator.parametersFor(autoConfig(istanbul), istanbul)
        assertEquals(18.0, turkey.fajrAngle, 0.01)
        assertEquals(17.0, turkey.ishaAngle, 0.01)
        assertEquals(-7, turkey.methodAdjustments.sunrise)
        assertEquals(5, turkey.methodAdjustments.dhuhr)
        assertEquals(4, turkey.methodAdjustments.asr)
        assertEquals(7, turkey.methodAdjustments.maghrib)
        assertEquals(AsrMadhabPref.HANAFI, PrayerCountryDefaults.madhabFor("TR"))

        val rabat = PrayerLocation(34.0209, -6.8416, "الرباط", "المغرب", "MA")
        val morocco = PrayerTimesCalculator.parametersFor(autoConfig(rabat), rabat)
        assertEquals(19.0, morocco.fajrAngle, 0.01)
        assertEquals(17.0, morocco.ishaAngle, 0.01)
        assertEquals(-2, morocco.methodAdjustments.sunrise)
        assertEquals(5, morocco.methodAdjustments.dhuhr)
        assertEquals(5, morocco.methodAdjustments.maghrib)

        val muscat = PrayerLocation(23.5880, 58.3829, "مسقط", "عُمان", "OM")
        val oman = PrayerTimesCalculator.parametersFor(autoConfig(muscat), muscat)
        assertEquals(18.0, oman.fajrAngle, 0.01)
        assertEquals(18.0, oman.ishaAngle, 0.01)
        assertEquals(0, oman.ishaInterval)
        assertEquals(5, oman.methodAdjustments.dhuhr)
        assertEquals(5, oman.methodAdjustments.asr)
        assertEquals(5, oman.methodAdjustments.maghrib)
        assertEquals(1, oman.methodAdjustments.isha)

        val tehran = PrayerLocation(35.6892, 51.3890, "طهران", "إيران", "IR")
        val tehranNoon = Instant.parse("2026-09-30T08:30:00Z").toEpochMilli()
        val iranAuto = PrayerTimesCalculator.timesFor(autoConfig(tehran), tehranNoon)!!
        val iranPlain = PrayerTimesCalculator.timesFor(
            autoConfig(tehran).copy(method = CalculationMethodPref.MUSLIM_WORLD_LEAGUE),
            tehranNoon
        )!!
        val maghribShift = (iranAuto.timeOf(PrayerName.MAGHRIB)!! - iranPlain.timeOf(PrayerName.MAGHRIB)!!) / 60_000L
        assertTrue("maghrib shift $maghribShift", maghribShift in 15..35)
        val ishaShift = (iranPlain.timeOf(PrayerName.ISHA)!! - iranAuto.timeOf(PrayerName.ISHA)!!) / 60_000L
        assertTrue("isha shift $ishaShift", ishaShift in 8..30)
    }

    private fun assertNear(millis: Long, zone: ZoneId, hour: Int, minute: Int) {
        val local = Instant.ofEpochMilli(millis).atZone(zone)
        val delta = kotlin.math.abs((local.hour * 60 + local.minute) - (hour * 60 + minute))
        assertTrue("${local.toLocalTime()} expected $hour:$minute", delta <= 1)
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

    private fun autoConfig(location: PrayerLocation) = PrayerConfig(
        enabled = true,
        afterPrayerReminder = true,
        location = location,
        locationMode = LocationMode.MANUAL,
        travelAutoUpdate = false,
        timezoneMode = TimezoneMode.AUTO,
        timezoneId = "",
        dstMode = DstMode.AUTO,
        method = CalculationMethodPref.AUTO,
        madhab = AsrMadhabPref.AUTO,
        minuteOffsets = emptyMap(),
        quietMinutes = emptyMap(),
        jumuahQuietMinutes = 55,
    )

    @Test
    fun autoTimezoneUsesCityNotCountry() {
        val tokyo = PrayerLocation(35.6762, 139.6503, "طوكيو", "اليابان", "JP")
        val losAngeles = PrayerLocation(34.0522, -118.2437, "لوس أنجلوس", "أمريكا", "US")
        val manaus = PrayerLocation(-3.1190, -60.0217, "ماناوس", "البرازيل", "BR")
        assertEquals("Asia/Tokyo", PrayerTimesCalculator.zoneId(autoConfig(tokyo)).id)
        assertEquals("America/Los_Angeles", PrayerTimesCalculator.zoneId(autoConfig(losAngeles)).id)
        assertEquals("America/Manaus", PrayerTimesCalculator.zoneId(autoConfig(manaus)).id)
        assertEquals("Africa/Lagos", PrayerTimezones.resolve(PrayerLocation(6.5244, 3.3792, "لاغوس", "نيجيريا", "NG")))
        assertEquals("Africa/Johannesburg", PrayerTimezones.resolve(PrayerLocation(-33.9249, 18.4241, "كيب تاون", "جنوب أفريقيا", "ZA")))
        assertEquals("Asia/Tashkent", PrayerTimezones.resolve(PrayerLocation(41.2995, 69.2401, "طشقند", "أوزبكستان", "UZ")))
    }

    @Test
    fun damascusAsrFollowsPublicCalendarsNotHanafi() {
        val damascus = PrayerLocation(33.5138, 36.2765, "دمشق", "سوريا", "SY")
        val noon = Instant.parse("2026-09-23T09:00:00Z").toEpochMilli()
        val auto = PrayerTimesCalculator.timesFor(autoConfig(damascus), noon)!!
        val zone = ZoneId.of("Asia/Damascus")
        val asr = Instant.ofEpochMilli(auto.timeOf(PrayerName.ASR)!!).atZone(zone)
        assertEquals(15, asr.hour)
        assertTrue(asr.minute in 40..59)
        val hanafi = PrayerTimesCalculator.timesFor(
            autoConfig(damascus).copy(madhab = AsrMadhabPref.HANAFI),
            noon
        )!!
        val hanafiAsr = Instant.ofEpochMilli(hanafi.timeOf(PrayerName.ASR)!!).atZone(zone)
        assertTrue(hanafiAsr.toEpochSecond() - asr.toEpochSecond() >= 40 * 60)
    }

    @Test
    fun tokyoLocalTimesAreNotShiftedToDeviceZone() {
        val tokyo = PrayerLocation(35.6762, 139.6503, "طوكيو", "اليابان", "JP")
        val noon = Instant.parse("2026-09-06T03:00:00Z").toEpochMilli()
        val times = PrayerTimesCalculator.timesFor(autoConfig(tokyo), noon)!!
        val zone = ZoneId.of("Asia/Tokyo")
        val sunrise = Instant.ofEpochMilli(times.sunriseMillis!!).atZone(zone)
        val maghrib = Instant.ofEpochMilli(times.timeOf(PrayerName.MAGHRIB)!!).atZone(zone)
        assertEquals(5, sunrise.hour)
        assertTrue(maghrib.hour in 17..19)
        assertTrue(searchKnownCities("Tokyo").any { it.timezoneId == "Asia/Tokyo" })
    }
}
