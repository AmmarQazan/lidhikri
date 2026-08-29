package com.greendome.adhkar.prayer

import org.json.JSONObject

data class CountryPrayerOverride(
    val method: CalculationMethodPref,
    val madhab: AsrMadhabPref,
    val timezone: String,
    val dst: DstMode = DstMode.AUTO
)

data class PrayerDefaultsTable(
    val version: Int,
    val countries: Map<String, CountryPrayerOverride>
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("version", version)
        val countriesJson = JSONObject()
        countries.toSortedMap().forEach { (code, entry) ->
            countriesJson.put(
                code,
                JSONObject()
                    .put("method", entry.method.name)
                    .put("madhab", entry.madhab.name)
                    .put("timezone", entry.timezone)
                    .put("dst", entry.dst.name)
            )
        }
        root.put("countries", countriesJson)
        return root.toString()
    }

    companion object {
        const val STORAGE_PATH = "prayer_defaults.json"

        fun parse(raw: String): PrayerDefaultsTable {
            if (raw.isBlank()) return PrayerDefaultsTable(0, emptyMap())
            val json = JSONObject(raw)
            val countriesJson = json.optJSONObject("countries") ?: JSONObject()
            val countries = mutableMapOf<String, CountryPrayerOverride>()
            val keys = countriesJson.keys()
            while (keys.hasNext()) {
                val rawCode = keys.next()
                val code = rawCode.uppercase()
                val o = countriesJson.optJSONObject(rawCode) ?: continue
                val method = parseMethod(o.optString("method")) ?: continue
                val madhab = parseMadhab(o.optString("madhab")) ?: AsrMadhabPref.SHAFI
                val timezone = o.optString("timezone")
                if (timezone.isBlank()) continue
                val dst = parseDst(o.optString("dst"))
                countries[code] = CountryPrayerOverride(method, madhab, timezone, dst)
            }
            return PrayerDefaultsTable(
                version = json.optInt("version", 0),
                countries = countries
            )
        }

        private fun parseMethod(raw: String): CalculationMethodPref? {
            val value = runCatching { CalculationMethodPref.valueOf(raw) }.getOrNull()
            return value?.takeIf { it != CalculationMethodPref.AUTO }
        }

        private fun parseMadhab(raw: String): AsrMadhabPref? {
            val value = runCatching { AsrMadhabPref.valueOf(raw) }.getOrNull()
            return value?.takeIf { it != AsrMadhabPref.AUTO }
        }

        private fun parseDst(raw: String): DstMode =
            runCatching { DstMode.valueOf(raw) }.getOrDefault(DstMode.AUTO)
    }
}
