package com.greendome.adhkar.prayer

enum class PrayerName {
    FAJR, DHUHR, ASR, MAGHRIB, ISHA
}

enum class LocationMode {
    MANUAL, GPS
}

enum class TimezoneMode {
    AUTO, MANUAL
}

enum class DstMode {
    AUTO, ON, OFF
}

enum class CalculationMethodPref {
    AUTO,
    MUSLIM_WORLD_LEAGUE,
    EGYPTIAN,
    KARACHI,
    UMM_AL_QURA,
    DUBAI,
    MOON_SIGHTING_COMMITTEE,
    NORTH_AMERICA,
    KUWAIT,
    QATAR,
    SINGAPORE
}

enum class AsrMadhabPref {
    AUTO, SHAFI, HANAFI
}

enum class AdhanSoundMode {
    DEFAULT, SHORT, SILENT, CUSTOM, CATALOG, RECORDED
}

enum class AdhanEventKind {
    PRE, ADHAN, IQAMA
}

data class PrayerAlertSettings(
    val adhanEnabled: Boolean = true,
    val soundMode: AdhanSoundMode = AdhanSoundMode.DEFAULT,
    val customPath: String = "",
    val catalogId: Long = 0L,
    val notifyBeforeMinutes: Int = 0,
    val iqamaMinutes: Int = 0,
    val afterAdhanAzkar: Boolean = true,
    val overrideSilent: Boolean = false,
) {
    fun resolvedSoundMode(): AdhanSoundMode = when {
        soundMode == AdhanSoundMode.CUSTOM && customPath.isBlank() -> AdhanSoundMode.DEFAULT
        soundMode == AdhanSoundMode.RECORDED && customPath.isBlank() -> AdhanSoundMode.DEFAULT
        soundMode == AdhanSoundMode.CATALOG && catalogId <= 0L -> AdhanSoundMode.DEFAULT
        else -> soundMode
    }
}

data class AdhanEvent(
    val prayer: PrayerName,
    val kind: AdhanEventKind,
    val atMillis: Long,
)

data class PrayerLocation(
    val latitude: Double,
    val longitude: Double,
    val cityName: String,
    val countryName: String = "",
    val countryCode: String = "",
    val timezoneId: String = ""
) {
    val isValid: Boolean
        get() = latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
            !(latitude == 0.0 && longitude == 0.0)
}

data class PrayerConfig(
    val enabled: Boolean,
    val afterPrayerReminder: Boolean,
    val location: PrayerLocation?,
    val locationMode: LocationMode,
    val travelAutoUpdate: Boolean,
    val timezoneMode: TimezoneMode,
    val timezoneId: String,
    val dstMode: DstMode,
    val method: CalculationMethodPref,
    val madhab: AsrMadhabPref,
    val minuteOffsets: Map<PrayerName, Int>,
    val quietMinutes: Map<PrayerName, Int>,
    val jumuahQuietMinutes: Int,
    val afterPrayerMinutes: Map<PrayerName, Int> = emptyMap(),
    val jumuahAfterPrayerMinutes: Int = DEFAULT_JUMUAH_QUIET,
    val timesEnabled: Boolean = true,
    val adhanEnabled: Boolean = true,
    val imsakOffsetMinutes: Int = DEFAULT_IMSAK,
    val alerts: Map<PrayerName, PrayerAlertSettings> = emptyMap(),
) {
    val hasLocation: Boolean get() = location?.isValid == true
    val hasTimes: Boolean get() = timesEnabled && hasLocation
    val adhanActive: Boolean get() = adhanEnabled && hasTimes

    fun alert(prayer: PrayerName): PrayerAlertSettings =
        alerts[prayer] ?: PrayerAlertSettings(overrideSilent = prayer == PrayerName.FAJR)

    fun offset(prayer: PrayerName): Int = minuteOffsets[prayer] ?: 0

    fun quietDuration(prayer: PrayerName, friday: Boolean): Int {
        if (friday && prayer == PrayerName.DHUHR) {
            return jumuahQuietMinutes.coerceIn(QUIET_MIN, QUIET_MAX)
        }
        return (quietMinutes[prayer] ?: defaultQuietMinutes(prayer)).coerceIn(QUIET_MIN, QUIET_MAX)
    }

    fun afterPrayerDelay(prayer: PrayerName, friday: Boolean): Int {
        if (friday && prayer == PrayerName.DHUHR) {
            return jumuahAfterPrayerMinutes.coerceIn(QUIET_MIN, QUIET_MAX)
        }
        return (afterPrayerMinutes[prayer] ?: quietDuration(prayer, friday))
            .coerceIn(QUIET_MIN, QUIET_MAX)
    }

    companion object {
        const val OFFSET_MIN = -15
        const val OFFSET_MAX = 15
        const val QUIET_MIN = 5
        const val QUIET_MAX = 120
        const val DEFAULT_JUMUAH_QUIET = 45
        const val DEFAULT_IMSAK = 10
        const val IMSAK_MIN = 5
        const val IMSAK_MAX = 30
        const val PRE_ADHAN_MAX = 60
        const val IQAMA_MAX = 60
        const val TRAVEL_DISTANCE_METERS = 50_000f
        const val TRAVEL_CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L

        fun defaultAlert(prayer: PrayerName) = PrayerAlertSettings(
            overrideSilent = prayer == PrayerName.FAJR
        )

        fun defaultQuietMinutes(prayer: PrayerName): Int = when (prayer) {
            PrayerName.FAJR -> 30
            PrayerName.DHUHR -> 30
            PrayerName.ASR -> 30
            PrayerName.MAGHRIB -> 20
            PrayerName.ISHA -> 30
        }

        fun empty() = PrayerConfig(
            enabled = false,
            afterPrayerReminder = true,
            location = null,
            locationMode = LocationMode.MANUAL,
            travelAutoUpdate = false,
            timezoneMode = TimezoneMode.AUTO,
            timezoneId = "",
            dstMode = DstMode.AUTO,
            method = CalculationMethodPref.AUTO,
            madhab = AsrMadhabPref.AUTO,
            minuteOffsets = emptyMap(),
            quietMinutes = emptyMap(),
            jumuahQuietMinutes = DEFAULT_JUMUAH_QUIET,
            timesEnabled = true,
            adhanEnabled = true,
        )
    }
}

data class PrayerInstant(
    val prayer: PrayerName,
    val epochMillis: Long
)

data class QuietWindow(
    val prayer: PrayerName,
    val startMillis: Long,
    val endMillis: Long
) {
    fun contains(millis: Long): Boolean = millis in startMillis until endMillis
}

data class DailyPrayerTimes(
    val dayStartMillis: Long,
    val prayers: List<PrayerInstant>,
    val sunriseMillis: Long? = null,
    val imsakMillis: Long? = null,
) {
    fun timeOf(prayer: PrayerName): Long? = prayers.firstOrNull { it.prayer == prayer }?.epochMillis

    fun nextPrayer(fromMillis: Long): PrayerInstant? =
        prayers.filter { it.epochMillis > fromMillis }.minByOrNull { it.epochMillis }
}
