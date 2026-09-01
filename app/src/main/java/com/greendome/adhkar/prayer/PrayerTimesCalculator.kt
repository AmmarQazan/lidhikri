package com.greendome.adhkar.prayer

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.HighLatitudeRule
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.TimeZone

object PrayerTimesCalculator {
    fun zoneId(config: PrayerConfig): ZoneId {
        val location = config.location
        val resolved = when (config.timezoneMode) {
            TimezoneMode.MANUAL -> config.timezoneId.ifBlank { TimeZone.getDefault().id }
            TimezoneMode.AUTO -> {
                val fromCountry = location?.countryCode
                    ?.takeIf { it.isNotBlank() }
                    ?.let { PrayerCountryDefaults.timezoneIdFor(it) }
                fromCountry ?: TimeZone.getDefault().id
            }
        }
        return runCatching { ZoneId.of(resolved) }.getOrDefault(ZoneId.systemDefault())
    }

    fun timesFor(
        config: PrayerConfig,
        atMillis: Long = System.currentTimeMillis()
    ): DailyPrayerTimes? {
        val location = config.location?.takeIf { it.isValid } ?: return null
        val zone = zoneId(config)
        val zoned = Instant.ofEpochMilli(atMillis).atZone(zone)
        val date = DateComponents(zoned.year, zoned.monthValue, zoned.dayOfMonth)
        val params = parameters(config, location)
        val times = PrayerTimes(
            Coordinates(location.latitude, location.longitude),
            date,
            params
        )
        val dayStart = zoned.toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        val dstMode = resolvedDst(config, location)
        val prayers = listOf(
            PrayerName.FAJR to times.fajr,
            PrayerName.DHUHR to times.dhuhr,
            PrayerName.ASR to times.asr,
            PrayerName.MAGHRIB to times.maghrib,
            PrayerName.ISHA to times.isha
        ).map { (prayer, raw) ->
            val adjusted = applyDst(raw.toInstant(), zone, dstMode)
                .plus(config.offset(prayer).toLong(), ChronoUnit.MINUTES)
            PrayerInstant(prayer, adjusted.toEpochMilli())
        }
        val sunrise = applyDst(times.sunrise.toInstant(), zone, dstMode).toEpochMilli()
        val fajrAt = prayers.first { it.prayer == PrayerName.FAJR }.epochMillis
        val imsak = fajrAt - config.imsakOffsetMinutes.coerceIn(
            PrayerConfig.IMSAK_MIN,
            PrayerConfig.IMSAK_MAX
        ) * 60_000L
        return DailyPrayerTimes(dayStart, prayers, sunriseMillis = sunrise, imsakMillis = imsak)
    }

    fun timesAround(
        config: PrayerConfig,
        atMillis: Long = System.currentTimeMillis()
    ): List<DailyPrayerTimes> {
        val dayMs = 24 * 60 * 60 * 1000L
        return listOfNotNull(
            timesFor(config, atMillis - dayMs),
            timesFor(config, atMillis),
            timesFor(config, atMillis + dayMs)
        )
    }

    fun nextPrayer(
        config: PrayerConfig,
        fromMillis: Long = System.currentTimeMillis()
    ): PrayerInstant? = timesAround(config, fromMillis)
        .flatMap { it.prayers }
        .filter { it.epochMillis > fromMillis }
        .minByOrNull { it.epochMillis }

    private fun parameters(config: PrayerConfig, location: PrayerLocation): CalculationParameters {
        val methodPref = if (config.method == CalculationMethodPref.AUTO) {
            PrayerCountryDefaults.methodFor(location.countryCode)
        } else {
            config.method
        }
        val src = toLibraryMethod(methodPref).parameters
        val params = CalculationParameters(src.fajrAngle, src.ishaAngle).apply {
            ishaInterval = src.ishaInterval
            adjustments = src.adjustments
        }
        val madhabPref = if (config.madhab == AsrMadhabPref.AUTO) {
            PrayerCountryDefaults.madhabFor(location.countryCode)
        } else {
            config.madhab
        }
        params.madhab = if (madhabPref == AsrMadhabPref.HANAFI) Madhab.HANAFI else Madhab.SHAFI
        if (kotlin.math.abs(location.latitude) >= 48.0) {
            params.highLatitudeRule = HighLatitudeRule.TWILIGHT_ANGLE
        }
        return params
    }

    private fun toLibraryMethod(pref: CalculationMethodPref): CalculationMethod = when (pref) {
        CalculationMethodPref.AUTO,
        CalculationMethodPref.MUSLIM_WORLD_LEAGUE -> CalculationMethod.MUSLIM_WORLD_LEAGUE
        CalculationMethodPref.EGYPTIAN -> CalculationMethod.EGYPTIAN
        CalculationMethodPref.KARACHI -> CalculationMethod.KARACHI
        CalculationMethodPref.UMM_AL_QURA -> CalculationMethod.UMM_AL_QURA
        CalculationMethodPref.DUBAI -> CalculationMethod.DUBAI
        CalculationMethodPref.MOON_SIGHTING_COMMITTEE -> CalculationMethod.MOON_SIGHTING_COMMITTEE
        CalculationMethodPref.NORTH_AMERICA -> CalculationMethod.NORTH_AMERICA
        CalculationMethodPref.KUWAIT -> CalculationMethod.KUWAIT
        CalculationMethodPref.QATAR -> CalculationMethod.QATAR
        CalculationMethodPref.SINGAPORE -> CalculationMethod.SINGAPORE
    }

    private fun resolvedDst(config: PrayerConfig, location: PrayerLocation): DstMode {
        return if (config.dstMode == DstMode.AUTO) {
            PrayerCountryDefaults.dstFor(location.countryCode)
        } else {
            config.dstMode
        }
    }

    internal fun applyDst(instant: Instant, zone: ZoneId, mode: DstMode): Instant {
        val inDst = zone.rules.isDaylightSavings(instant)
        return when (mode) {
            DstMode.AUTO -> instant
            DstMode.ON -> if (inDst) instant else instant.plus(1, ChronoUnit.HOURS)
            DstMode.OFF -> if (inDst) instant.minus(1, ChronoUnit.HOURS) else instant
        }
    }
}
