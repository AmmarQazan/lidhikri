package com.greendome.adhkar.prayer

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.HighLatitudeRule
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerAdjustments
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.TimeZone

object PrayerTimesCalculator {
    private const val TEHRAN_MAGHRIB_ANGLE = 4.5
    fun zoneId(config: PrayerConfig): ZoneId {
        val resolved = when (config.timezoneMode) {
            TimezoneMode.MANUAL -> config.timezoneId.ifBlank { TimeZone.getDefault().id }
            TimezoneMode.AUTO -> {
                val location = config.location
                if (location?.isValid == true) {
                    PrayerTimezones.resolve(location)
                } else {
                    TimeZone.getDefault().id
                }
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
        val methodPref = resolvedMethod(config, location)
        val params = parameters(config, location, methodPref)
        val coordinates = Coordinates(location.latitude, location.longitude)
        val times = PrayerTimes(coordinates, date, params)
        val maghribAt = maghribInstant(times, coordinates, date, params, methodPref)
        val dayStart = zoned.toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        val dstMode = resolvedDst(config, location)
        val prayers = listOf(
            PrayerName.FAJR to times.fajr.toInstant(),
            PrayerName.DHUHR to times.dhuhr.toInstant(),
            PrayerName.ASR to times.asr.toInstant(),
            PrayerName.MAGHRIB to maghribAt,
            PrayerName.ISHA to times.isha.toInstant()
        ).map { (prayer, raw) ->
            val adjusted = applyDst(raw, zone, dstMode)
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

    private fun resolvedMethod(config: PrayerConfig, location: PrayerLocation): CalculationMethodPref {
        return if (config.method == CalculationMethodPref.AUTO) {
            PrayerCountryDefaults.methodFor(location.countryCode)
        } else {
            config.method
        }
    }

    internal fun parametersFor(config: PrayerConfig, location: PrayerLocation): CalculationParameters {
        return parameters(config, location, resolvedMethod(config, location))
    }

    private fun parameters(
        config: PrayerConfig,
        location: PrayerLocation,
        methodPref: CalculationMethodPref
    ): CalculationParameters {
        val src = toLibraryMethod(methodPref).parameters
        val params = CalculationParameters(src.fajrAngle, src.ishaAngle).apply {
            ishaInterval = src.ishaInterval
            methodAdjustments = copyAdjustments(src.methodAdjustments)
        }
        applyOfficialProfile(params, methodPref, location.countryCode)
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

    private fun applyOfficialProfile(
        params: CalculationParameters,
        methodPref: CalculationMethodPref,
        countryCode: String
    ) {
        when (methodPref) {
            CalculationMethodPref.TURKEY -> {
                params.fajrAngle = 18.0
                params.ishaAngle = 17.0
                params.ishaInterval = 0
                params.methodAdjustments = PrayerAdjustments(0, -7, 5, 4, 7, 0)
            }
            CalculationMethodPref.MOROCCO -> {
                params.fajrAngle = 19.0
                params.ishaAngle = 17.0
                params.ishaInterval = 0
                params.methodAdjustments = PrayerAdjustments(0, -2, 5, 0, 5, 0)
            }
            CalculationMethodPref.OMAN -> {
                params.fajrAngle = 18.0
                params.ishaAngle = 18.0
                params.ishaInterval = 0
                params.methodAdjustments = PrayerAdjustments(0, 0, 5, 5, 5, 1)
            }
            CalculationMethodPref.TEHRAN -> {
                params.fajrAngle = 17.7
                params.ishaAngle = 14.0
                params.ishaInterval = 0
                params.methodAdjustments = PrayerAdjustments()
            }
            else -> Unit
        }
        if (countryCode.equals("JO", ignoreCase = true) &&
            methodPref == CalculationMethodPref.MUSLIM_WORLD_LEAGUE
        ) {
            val base = params.methodAdjustments
            params.methodAdjustments = PrayerAdjustments(
                base.fajr,
                base.sunrise,
                base.dhuhr,
                base.asr,
                base.maghrib + 6,
                base.isha + 5
            )
        }
    }

    private fun maghribInstant(
        times: PrayerTimes,
        coordinates: Coordinates,
        date: DateComponents,
        params: CalculationParameters,
        methodPref: CalculationMethodPref
    ): Instant {
        val sunset = times.maghrib.toInstant()
        if (methodPref != CalculationMethodPref.TEHRAN) return sunset
        val probe = CalculationParameters(params.fajrAngle, TEHRAN_MAGHRIB_ANGLE)
        probe.madhab = params.madhab
        probe.highLatitudeRule = params.highLatitudeRule
        val depressed = PrayerTimes(coordinates, date, probe).isha ?: return sunset
        return if (depressed.after(times.maghrib)) depressed.toInstant() else sunset
    }

    private fun copyAdjustments(src: PrayerAdjustments?): PrayerAdjustments {
        if (src == null) return PrayerAdjustments()
        return PrayerAdjustments(src.fajr, src.sunrise, src.dhuhr, src.asr, src.maghrib, src.isha)
    }

    private fun toLibraryMethod(pref: CalculationMethodPref): CalculationMethod = when (pref) {
        CalculationMethodPref.AUTO,
        CalculationMethodPref.MUSLIM_WORLD_LEAGUE,
        CalculationMethodPref.TURKEY,
        CalculationMethodPref.MOROCCO,
        CalculationMethodPref.TEHRAN -> CalculationMethod.MUSLIM_WORLD_LEAGUE
        CalculationMethodPref.EGYPTIAN -> CalculationMethod.EGYPTIAN
        CalculationMethodPref.KARACHI -> CalculationMethod.KARACHI
        CalculationMethodPref.UMM_AL_QURA,
        CalculationMethodPref.OMAN -> CalculationMethod.UMM_AL_QURA
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
