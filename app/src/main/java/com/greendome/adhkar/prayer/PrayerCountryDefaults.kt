package com.greendome.adhkar.prayer

import java.util.Locale
import java.util.TimeZone

object PrayerCountryDefaults {
    @Volatile
    private var overlay: Map<String, CountryPrayerOverride> = emptyMap()

    val builtinCountryCodes: List<String> = listOf(
        "SA", "AE", "KW", "QA", "BH", "OM", "YE",
        "EG", "JO", "LB", "SY", "IQ", "PS", "TR", "IR",
        "MA", "DZ", "TN", "LY", "SD",
        "MR", "SO", "DJ", "KM",
        "PK", "IN", "BD", "AF",
        "ID", "MY", "SG", "BN",
        "GB", "FR", "DE", "US", "CA", "AU"
    )

    fun loadCached(json: String) {
        overlay = PrayerDefaultsTable.parse(json).countries
    }

    fun applyRemote(table: PrayerDefaultsTable) {
        overlay = table.countries
    }

    fun clearRemote() {
        overlay = emptyMap()
    }

    fun upsert(code: String, entry: CountryPrayerOverride) {
        ensureFullOverlay()
        overlay = overlay + (code.uppercase() to entry)
    }

    fun persistJson(version: Int): String {
        ensureFullOverlay()
        return PrayerDefaultsTable(version, overlay).toJson()
    }

    fun snapshot(): List<Pair<String, CountryPrayerOverride>> =
        resolvedEntries().toList().sortedBy { it.first }

    fun methodFor(countryCode: String): CalculationMethodPref {
        val code = countryCode.uppercase()
        return overlay[code]?.method ?: builtinMethod(code)
    }

    fun madhabFor(countryCode: String): AsrMadhabPref {
        val code = countryCode.uppercase()
        return overlay[code]?.madhab ?: builtinMadhab(code)
    }

    fun timezoneIdFor(countryCode: String, fallback: String = TimeZone.getDefault().id): String {
        val code = countryCode.uppercase()
        overlay[code]?.timezone?.takeIf { it.isNotBlank() }?.let { return it }
        return builtinTimezone(code, fallback)
    }

    fun dstFor(countryCode: String): DstMode {
        return overlay[countryCode.uppercase()]?.dst ?: DstMode.AUTO
    }

    fun countryLabel(code: String): String {
        val name = Locale("", code).getDisplayCountry(Locale("ar")).ifBlank {
            Locale("", code).getDisplayCountry(Locale.US)
        }
        return if (name.isBlank() || name.equals(code, ignoreCase = true)) code else "$name ($code)"
    }

    fun resolvedEntries(): Map<String, CountryPrayerOverride> {
        val codes = (builtinCountryCodes + overlay.keys).map { it.uppercase() }.toSortedSet()
        return codes.associateWith { overrideFor(it) }
    }

    internal fun builtinMethod(countryCode: String): CalculationMethodPref {
        return when (countryCode.uppercase()) {
            "SA" -> CalculationMethodPref.UMM_AL_QURA
            "AE" -> CalculationMethodPref.DUBAI
            "KW" -> CalculationMethodPref.KUWAIT
            "QA" -> CalculationMethodPref.QATAR
            "BH", "OM", "YE" -> CalculationMethodPref.UMM_AL_QURA
            "EG", "SD", "LY" -> CalculationMethodPref.EGYPTIAN
            "MR", "SO", "DJ", "KM" -> CalculationMethodPref.MUSLIM_WORLD_LEAGUE
            "PK", "IN", "BD", "AF" -> CalculationMethodPref.KARACHI
            "US", "CA" -> CalculationMethodPref.NORTH_AMERICA
            "SG", "MY", "BN" -> CalculationMethodPref.SINGAPORE
            "ID" -> CalculationMethodPref.SINGAPORE
            else -> CalculationMethodPref.MUSLIM_WORLD_LEAGUE
        }
    }

    internal fun builtinMadhab(countryCode: String): AsrMadhabPref {
        return when (countryCode.uppercase()) {
            "PK", "IN", "BD", "AF", "TR", "IQ", "SY" -> AsrMadhabPref.HANAFI
            else -> AsrMadhabPref.SHAFI
        }
    }

    internal fun builtinTimezone(countryCode: String, fallback: String = TimeZone.getDefault().id): String {
        return when (countryCode.uppercase()) {
            "SA" -> "Asia/Riyadh"
            "AE" -> "Asia/Dubai"
            "KW" -> "Asia/Kuwait"
            "QA" -> "Asia/Qatar"
            "BH" -> "Asia/Bahrain"
            "OM" -> "Asia/Muscat"
            "YE" -> "Asia/Aden"
            "EG" -> "Africa/Cairo"
            "JO" -> "Asia/Amman"
            "LB" -> "Asia/Beirut"
            "SY" -> "Asia/Damascus"
            "IQ" -> "Asia/Baghdad"
            "PS" -> "Asia/Gaza"
            "TR" -> "Europe/Istanbul"
            "IR" -> "Asia/Tehran"
            "MA" -> "Africa/Casablanca"
            "DZ" -> "Africa/Algiers"
            "TN" -> "Africa/Tunis"
            "LY" -> "Africa/Tripoli"
            "SD" -> "Africa/Khartoum"
            "MR" -> "Africa/Nouakchott"
            "SO" -> "Africa/Mogadishu"
            "DJ" -> "Africa/Djibouti"
            "KM" -> "Indian/Comoro"
            "PK" -> "Asia/Karachi"
            "IN" -> "Asia/Kolkata"
            "BD" -> "Asia/Dhaka"
            "AF" -> "Asia/Kabul"
            "ID" -> "Asia/Jakarta"
            "MY" -> "Asia/Kuala_Lumpur"
            "SG" -> "Asia/Singapore"
            "BN" -> "Asia/Brunei"
            "GB" -> "Europe/London"
            "FR" -> "Europe/Paris"
            "DE" -> "Europe/Berlin"
            "US" -> "America/New_York"
            "CA" -> "America/Toronto"
            "AU" -> "Australia/Sydney"
            else -> fallback
        }
    }

    private fun overrideFor(code: String): CountryPrayerOverride {
        overlay[code]?.let { return it }
        return CountryPrayerOverride(
            method = builtinMethod(code),
            madhab = builtinMadhab(code),
            timezone = builtinTimezone(code),
            dst = DstMode.AUTO
        )
    }

    private fun ensureFullOverlay() {
        if (overlay.size < builtinCountryCodes.size) {
            overlay = resolvedEntries()
        }
    }

    val commonTimezones: List<String> = listOf(
        "Asia/Riyadh",
        "Asia/Dubai",
        "Asia/Kuwait",
        "Asia/Qatar",
        "Asia/Bahrain",
        "Asia/Muscat",
        "Asia/Aden",
        "Africa/Cairo",
        "Asia/Amman",
        "Asia/Beirut",
        "Asia/Damascus",
        "Asia/Baghdad",
        "Asia/Gaza",
        "Asia/Brunei",
        "Europe/Istanbul",
        "Asia/Tehran",
        "Africa/Casablanca",
        "Africa/Algiers",
        "Africa/Tunis",
        "Africa/Tripoli",
        "Africa/Khartoum",
        "Africa/Nouakchott",
        "Africa/Mogadishu",
        "Africa/Djibouti",
        "Indian/Comoro",
        "Asia/Karachi",
        "Asia/Kolkata",
        "Asia/Dhaka",
        "Asia/Jakarta",
        "Asia/Kuala_Lumpur",
        "Asia/Singapore",
        "Europe/London",
        "Europe/Paris",
        "Europe/Berlin",
        "America/New_York",
        "America/Chicago",
        "America/Toronto",
        "Australia/Sydney"
    )
}
