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

data class PrayerLocation(
    val latitude: Double,
    val longitude: Double,
    val cityName: String,
    val countryName: String = "",
    val countryCode: String = ""
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
    val jumuahAfterPrayerMinutes: Int = DEFAULT_JUMUAH_QUIET
) {
    val hasLocation: Boolean get() = location?.isValid == true

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
        const val DEFAULT_JUMUAH_QUIET = 55
        const val TRAVEL_DISTANCE_METERS = 50_000f
        const val TRAVEL_CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L

        fun defaultQuietMinutes(prayer: PrayerName): Int = when (prayer) {
            PrayerName.FAJR -> 55
            PrayerName.DHUHR -> 55
            PrayerName.ASR -> 55
            PrayerName.MAGHRIB -> 45
            PrayerName.ISHA -> 45
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
            jumuahQuietMinutes = DEFAULT_JUMUAH_QUIET
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
    val prayers: List<PrayerInstant>
) {
    fun timeOf(prayer: PrayerName): Long? = prayers.firstOrNull { it.prayer == prayer }?.epochMillis
}
