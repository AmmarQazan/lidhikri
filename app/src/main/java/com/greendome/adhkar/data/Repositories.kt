package com.greendome.adhkar.data

import android.content.Context
import com.greendome.adhkar.data.local.AdhanAudioEntity
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.DhikrEntity
import com.greendome.adhkar.data.local.ReciterAudioEntity
import com.greendome.adhkar.data.local.ReciterAzkarAudioEntity
import com.greendome.adhkar.data.local.ReciterEntity
import com.greendome.adhkar.data.model.AppThemeMode
import com.greendome.adhkar.data.model.ArabicFontStyle
import com.greendome.adhkar.data.model.AzkarCardText
import com.greendome.adhkar.data.model.AzkarDisplayMode
import com.greendome.adhkar.data.model.AzkarListText
import com.greendome.adhkar.data.model.DhikrOfDayDisplayMode
import com.greendome.adhkar.data.model.DhikrOfDayTextColor
import com.greendome.adhkar.data.model.DhikrOfDayWidgetText
import com.greendome.adhkar.data.model.MisbahaFeedbackMode
import com.greendome.adhkar.data.model.MisbahaBeadTheme
import com.greendome.adhkar.data.model.MisbahaStyle
import com.greendome.adhkar.data.model.MisbahaWidgetBackground
import com.greendome.adhkar.data.model.ClockHourFormat
import com.greendome.adhkar.data.model.NumberDigitStyle
import com.greendome.adhkar.data.model.AudioSourceType
import com.greendome.adhkar.data.model.DhikrCategory
import com.greendome.adhkar.data.model.ScheduleType
import com.greendome.adhkar.prayer.AdhanAudioResolver
import com.greendome.adhkar.prayer.AdhanSoundMode
import com.greendome.adhkar.prayer.AsrMadhabPref
import com.greendome.adhkar.prayer.CalculationMethodPref
import com.greendome.adhkar.prayer.DstMode
import com.greendome.adhkar.prayer.LocationMode
import com.greendome.adhkar.prayer.PrayerAlertSettings
import com.greendome.adhkar.prayer.PrayerConfig
import com.greendome.adhkar.prayer.PrayerLocation
import com.greendome.adhkar.prayer.PrayerName
import com.greendome.adhkar.prayer.TimezoneMode
import com.greendome.adhkar.util.AppLanguages
import com.greendome.adhkar.util.DhikrScheduleMatcher
import com.greendome.adhkar.util.HomeLayout
import com.greendome.adhkar.util.TasbihWindow
import com.greendome.adhkar.data.model.PopupAppearance
import com.greendome.adhkar.data.model.PopupSettingsTarget
import com.greendome.adhkar.data.model.VoiceSettingsTarget
import com.greendome.adhkar.data.model.ReminderDisplayStyle
import com.greendome.adhkar.data.model.AutoReminderPresentation
import com.greendome.adhkar.data.model.TtsVoiceGender
import com.greendome.adhkar.data.model.VolumeMode
import kotlinx.coroutines.flow.Flow

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("adhkar_settings", Context.MODE_PRIVATE)

    companion object {
        const val DEFAULT_ADMIN_PIN = "23704660"
        val WIDGET_TARGET_OPTIONS = listOf(33, 99, 100)
        private const val KEY_RECITER_LIBRARY_OPT_IN = "reciter_library_opt_in"
    }

    var intervalMinutes: Int
        get() = prefs.getInt("interval_minutes", TasbihWindow.DEFAULT_INTERVAL_MINUTES)
        set(v) = prefs.edit().putInt("interval_minutes", v).apply()

    var tasbihStartHour: Int
        get() = prefs.getInt("tasbih_start_h", TasbihWindow.FALLBACK_START_HOUR).coerceIn(0, 23)
        set(v) = prefs.edit().putInt("tasbih_start_h", v.coerceIn(0, 23)).apply()

    var tasbihStartMinute: Int
        get() = prefs.getInt("tasbih_start_m", TasbihWindow.FALLBACK_START_MINUTE).coerceIn(0, 59)
        set(v) = prefs.edit().putInt("tasbih_start_m", v.coerceIn(0, 59)).apply()

    var tasbihEndHour: Int
        get() = prefs.getInt("tasbih_end_h", TasbihWindow.FALLBACK_END_HOUR).coerceIn(0, 23)
        set(v) = prefs.edit().putInt("tasbih_end_h", v.coerceIn(0, 23)).apply()

    var tasbihEndMinute: Int
        get() = prefs.getInt("tasbih_end_m", TasbihWindow.FALLBACK_END_MINUTE).coerceIn(0, 59)
        set(v) = prefs.edit().putInt("tasbih_end_m", v.coerceIn(0, 59)).apply()

    val hasTasbihWindowSet: Boolean
        get() = prefs.contains("tasbih_start_h")

    fun setTasbihWindow(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        prefs.edit()
            .putInt("tasbih_start_h", startHour.coerceIn(0, 23))
            .putInt("tasbih_start_m", startMinute.coerceIn(0, 59))
            .putInt("tasbih_end_h", endHour.coerceIn(0, 23))
            .putInt("tasbih_end_m", endMinute.coerceIn(0, 59))
            .apply()
    }

    var isServiceEnabled: Boolean
        get() = prefs.getBoolean("service_enabled", true)
        set(v) = prefs.edit().putBoolean("service_enabled", v).apply()

    var randomMode: Boolean
        get() = prefs.getBoolean("random_mode", true)
        set(v) = prefs.edit().putBoolean("random_mode", v).apply()

    var misbahaRepeatEnabled: Boolean
        get() = prefs.getBoolean("misbaha_repeat", true)
        set(v) = prefs.edit().putBoolean("misbaha_repeat", v).apply()

    var misbahaFeedbackMode: MisbahaFeedbackMode
        get() {
            val raw = prefs.getString("misbaha_feedback", MisbahaFeedbackMode.SOUND_AND_VIBRATION.name)
                ?: MisbahaFeedbackMode.SOUND_AND_VIBRATION.name
            return runCatching { MisbahaFeedbackMode.valueOf(raw) }
                .getOrDefault(MisbahaFeedbackMode.SOUND_AND_VIBRATION)
        }
        set(v) = prefs.edit().putString("misbaha_feedback", v.name).apply()

    var misbahaStyle: MisbahaStyle
        get() {
            val raw = prefs.getString("misbaha_style", MisbahaStyle.ELECTRONIC.name)
                ?: MisbahaStyle.ELECTRONIC.name
            return runCatching { MisbahaStyle.valueOf(raw) }
                .getOrDefault(MisbahaStyle.ELECTRONIC)
        }
        set(v) = prefs.edit().putString("misbaha_style", v.name).apply()

    var misbahaBeadScale: Float
        get() = prefs.getFloat("misbaha_bead_scale", 1f).coerceIn(0.7f, 1.6f)
        set(v) = prefs.edit().putFloat("misbaha_bead_scale", v.coerceIn(0.7f, 1.6f)).apply()

    var misbahaBeadTheme: MisbahaBeadTheme
        get() {
            val raw = prefs.getString("misbaha_bead_theme", MisbahaBeadTheme.CLASSIC.name)
                ?: MisbahaBeadTheme.CLASSIC.name
            return runCatching { MisbahaBeadTheme.valueOf(raw) }
                .getOrDefault(MisbahaBeadTheme.CLASSIC)
        }
        set(v) = prefs.edit().putString("misbaha_bead_theme", v.name).apply()

    var misbahaElectronicTheme: MisbahaBeadTheme
        get() {
            val raw = prefs.getString("misbaha_electronic_theme", null)
                ?: prefs.getString("misbaha_bead_theme", MisbahaBeadTheme.CLASSIC.name)
                ?: MisbahaBeadTheme.CLASSIC.name
            return runCatching { MisbahaBeadTheme.valueOf(raw) }
                .getOrDefault(MisbahaBeadTheme.CLASSIC)
        }
        set(v) = prefs.edit().putString("misbaha_electronic_theme", v.name).apply()

    var numberDigitStyle: NumberDigitStyle
        get() {
            val raw = prefs.getString("number_digit_style", NumberDigitStyle.ARABIC_INDIC.name)
                ?: NumberDigitStyle.ARABIC_INDIC.name
            return runCatching { NumberDigitStyle.valueOf(raw) }
                .getOrDefault(NumberDigitStyle.ARABIC_INDIC)
        }
        set(v) = prefs.edit().putString("number_digit_style", v.name).apply()

    var volume: Float
        get() = prefs.getFloat("volume", 0.8f)
        set(v) = prefs.edit().putFloat("volume", v).apply()

    var volumeMode: VolumeMode
        get() {
            val raw = prefs.getString("volume_mode", VolumeMode.MANUAL.name) ?: VolumeMode.MANUAL.name
            return runCatching { VolumeMode.valueOf(raw) }.getOrDefault(VolumeMode.MANUAL)
        }
        set(v) = prefs.edit().putString("volume_mode", v.name).apply()

    var pauseDuringCalls: Boolean
        get() = prefs.getBoolean("pause_calls", true)
        set(v) = prefs.edit().putBoolean("pause_calls", v).apply()

    var pauseDuringMedia: Boolean
        get() = prefs.getBoolean("pause_media", true)
        set(v) = prefs.edit().putBoolean("pause_media", v).apply()

    /** لا تشغّل الصوت في الصامت أو عدم الإزعاج */
    var respectQuietMode: Boolean
        get() = prefs.getBoolean("respect_quiet_mode", true)
        set(v) = prefs.edit().putBoolean("respect_quiet_mode", v).apply()

    var respectPrayerTime: Boolean
        get() = prefs.getBoolean("respect_prayer_time", false)
        set(v) = prefs.edit().putBoolean("respect_prayer_time", v).apply()

    var afterPrayerFromSalahEnabled: Boolean
        get() = prefs.getBoolean("after_prayer_from_salah", true)
        set(v) = prefs.edit().putBoolean("after_prayer_from_salah", v).apply()

    var afterAdhanAzkarEnabled: Boolean
        get() = prefs.getBoolean("after_adhan_azkar_enabled", true)
        set(v) = prefs.edit().putBoolean("after_adhan_azkar_enabled", v).apply()

    var prayerImsakOffsetMinutes: Int
        get() = prefs.getInt("prayer_imsak_offset", PrayerConfig.DEFAULT_IMSAK)
            .coerceIn(PrayerConfig.IMSAK_MIN, PrayerConfig.IMSAK_MAX)
        set(v) = prefs.edit().putInt(
            "prayer_imsak_offset",
            v.coerceIn(PrayerConfig.IMSAK_MIN, PrayerConfig.IMSAK_MAX)
        ).apply()

    var homeAzkarEnabled: Boolean
        get() = prefs.getBoolean("home_azkar_enabled", false)
        set(v) = prefs.edit().putBoolean("home_azkar_enabled", v).apply()

    var ridingAzkarEnabled: Boolean
        get() = prefs.getBoolean("riding_azkar_enabled", false)
        set(v) = prefs.edit().putBoolean("riding_azkar_enabled", v).apply()

    var ridingLastPlayAt: Long
        get() = prefs.getLong("riding_last_play_at", 0L)
        set(v) = prefs.edit().putLong("riding_last_play_at", v).apply()

    var ridingInTrip: Boolean
        get() = prefs.getBoolean("riding_in_trip", false)
        set(v) = prefs.edit().putBoolean("riding_in_trip", v).apply()

    fun ridingItemKeys(): Set<String> {
        val raw = prefs.getString("riding_item_keys", null) ?: return RidingAzkar.ids()
        if (raw.isBlank()) return emptySet()
        return raw.split(',').map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    fun setRidingItemKeys(ids: Set<String>) {
        prefs.edit().putString("riding_item_keys", ids.joinToString(",")).apply()
    }

    var homeLatitude: Double
        get() = java.lang.Double.longBitsToDouble(prefs.getLong("home_lat", 0L))
        set(v) = prefs.edit().putLong("home_lat", java.lang.Double.doubleToRawLongBits(v)).apply()

    var homeLongitude: Double
        get() = java.lang.Double.longBitsToDouble(prefs.getLong("home_lng", 0L))
        set(v) = prefs.edit().putLong("home_lng", java.lang.Double.doubleToRawLongBits(v)).apply()

    var homeLabel: String
        get() = prefs.getString("home_label", "").orEmpty()
        set(v) = prefs.edit().putString("home_label", v).apply()

    var homeCountry: String
        get() = prefs.getString("home_country", "").orEmpty()
        set(v) = prefs.edit().putString("home_country", v).apply()

    var homeLastEnterAt: Long
        get() = prefs.getLong("home_last_enter_at", 0L)
        set(v) = prefs.edit().putLong("home_last_enter_at", v).apply()

    var homeLastExitAt: Long
        get() = prefs.getLong("home_last_exit_at", 0L)
        set(v) = prefs.edit().putLong("home_last_exit_at", v).apply()

    val hasHomeLocation: Boolean
        get() = homeLabel.isNotBlank() &&
            !(homeLatitude == 0.0 && homeLongitude == 0.0) &&
            homeLatitude in -90.0..90.0 &&
            homeLongitude in -180.0..180.0

    fun homeLocation(): PrayerLocation? {
        if (!hasHomeLocation) return null
        return PrayerLocation(
            latitude = homeLatitude,
            longitude = homeLongitude,
            cityName = homeLabel,
            countryName = homeCountry,
        )
    }

    fun setHomeLocation(location: PrayerLocation) {
        prefs.edit()
            .putLong("home_lat", java.lang.Double.doubleToRawLongBits(location.latitude))
            .putLong("home_lng", java.lang.Double.doubleToRawLongBits(location.longitude))
            .putString("home_label", location.cityName)
            .putString("home_country", location.countryName)
            .commit()
    }

    fun clearHomeLocation() {
        prefs.edit()
            .remove("home_lat")
            .remove("home_lng")
            .remove("home_label")
            .remove("home_country")
            .apply()
    }

    fun homeEventKeys(event: HomeAzkar.Event): Set<String> {
        val key = if (event == HomeAzkar.Event.ENTER) "home_enter_keys" else "home_exit_keys"
        val raw = prefs.getString(key, null) ?: return HomeAzkar.idsFor(event)
        if (raw.isBlank()) return emptySet()
        return raw.split(',').map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    fun setHomeEventKeys(event: HomeAzkar.Event, ids: Set<String>) {
        val key = if (event == HomeAzkar.Event.ENTER) "home_enter_keys" else "home_exit_keys"
        prefs.edit().putString(key, ids.joinToString(",")).apply()
    }

    var prayerLocationMode: LocationMode
        get() = enumPref("prayer_location_mode", LocationMode.MANUAL)
        set(v) = prefs.edit().putString("prayer_location_mode", v.name).apply()

    var prayerTravelAutoUpdate: Boolean
        get() = prefs.getBoolean("prayer_travel_auto", false)
        set(v) = prefs.edit().putBoolean("prayer_travel_auto", v).apply()

    var prayerLastTravelCheckAt: Long
        get() = prefs.getLong("prayer_travel_check_at", 0L)
        set(v) = prefs.edit().putLong("prayer_travel_check_at", v).apply()

    var prayerLatitude: Double
        get() = java.lang.Double.longBitsToDouble(prefs.getLong("prayer_lat", 0L))
        set(v) = prefs.edit().putLong("prayer_lat", java.lang.Double.doubleToRawLongBits(v)).apply()

    var prayerLongitude: Double
        get() = java.lang.Double.longBitsToDouble(prefs.getLong("prayer_lng", 0L))
        set(v) = prefs.edit().putLong("prayer_lng", java.lang.Double.doubleToRawLongBits(v)).apply()

    var prayerCityName: String
        get() = prefs.getString("prayer_city", "").orEmpty()
        set(v) = prefs.edit().putString("prayer_city", v).apply()

    var prayerCountryName: String
        get() = prefs.getString("prayer_country", "").orEmpty()
        set(v) = prefs.edit().putString("prayer_country", v).apply()

    var prayerCountryCode: String
        get() = prefs.getString("prayer_country_code", "").orEmpty()
        set(v) = prefs.edit().putString("prayer_country_code", v).apply()

    var prayerTimezoneMode: TimezoneMode
        get() = enumPref("prayer_tz_mode", TimezoneMode.AUTO)
        set(v) = prefs.edit().putString("prayer_tz_mode", v.name).apply()

    var prayerTimezoneId: String
        get() = prefs.getString("prayer_tz_id", "").orEmpty()
        set(v) = prefs.edit().putString("prayer_tz_id", v).apply()

    var prayerDstMode: DstMode
        get() = enumPref("prayer_dst_mode", DstMode.AUTO)
        set(v) = prefs.edit().putString("prayer_dst_mode", v.name).apply()

    var prayerCalculationMethod: CalculationMethodPref
        get() = enumPref("prayer_method", CalculationMethodPref.AUTO)
        set(v) = prefs.edit().putString("prayer_method", v.name).apply()

    var prayerAsrMadhab: AsrMadhabPref
        get() = enumPref("prayer_madhab", AsrMadhabPref.AUTO)
        set(v) = prefs.edit().putString("prayer_madhab", v.name).apply()

    var prayerJumuahQuietMinutes: Int
        get() = prefs.getInt("prayer_jumuah_quiet", PrayerConfig.DEFAULT_JUMUAH_QUIET)
            .coerceIn(PrayerConfig.QUIET_MIN, PrayerConfig.QUIET_MAX)
        set(v) = prefs.edit().putInt(
            "prayer_jumuah_quiet",
            v.coerceIn(PrayerConfig.QUIET_MIN, PrayerConfig.QUIET_MAX)
        ).apply()

    fun prayerMinuteOffset(prayer: PrayerName): Int =
        prefs.getInt("prayer_off_${prayer.name}", 0)
            .coerceIn(PrayerConfig.OFFSET_MIN, PrayerConfig.OFFSET_MAX)

    fun setPrayerMinuteOffset(prayer: PrayerName, minutes: Int) {
        prefs.edit().putInt(
            "prayer_off_${prayer.name}",
            minutes.coerceIn(PrayerConfig.OFFSET_MIN, PrayerConfig.OFFSET_MAX)
        ).apply()
    }

    fun prayerQuietMinutes(prayer: PrayerName): Int =
        prefs.getInt("prayer_quiet_${prayer.name}", PrayerConfig.defaultQuietMinutes(prayer))
            .coerceIn(PrayerConfig.QUIET_MIN, PrayerConfig.QUIET_MAX)

    fun setPrayerQuietMinutes(prayer: PrayerName, minutes: Int) {
        prefs.edit().putInt(
            "prayer_quiet_${prayer.name}",
            minutes.coerceIn(PrayerConfig.QUIET_MIN, PrayerConfig.QUIET_MAX)
        ).apply()
    }

    fun prayerAfterDelayMinutes(prayer: PrayerName): Int {
        val key = "prayer_after_delay_${prayer.name}"
        val fallback = prayerQuietMinutes(prayer)
        val raw = if (prefs.contains(key)) prefs.getInt(key, fallback) else fallback
        return raw.coerceIn(PrayerConfig.QUIET_MIN, PrayerConfig.QUIET_MAX)
    }

    fun setPrayerAfterDelayMinutes(prayer: PrayerName, minutes: Int) {
        prefs.edit().putInt(
            "prayer_after_delay_${prayer.name}",
            minutes.coerceIn(PrayerConfig.QUIET_MIN, PrayerConfig.QUIET_MAX)
        ).apply()
    }

    fun adhanAlert(prayer: PrayerName): PrayerAlertSettings {
        val def = PrayerConfig.defaultAlert(prayer)
        return PrayerAlertSettings(
            adhanEnabled = prefs.getBoolean("adhan_on_${prayer.name}", true),
            soundMode = enumPref("adhan_sound_${prayer.name}", def.soundMode),
            customPath = prefs.getString("adhan_custom_${prayer.name}", "").orEmpty(),
            catalogId = prefs.getLong("adhan_catalog_${prayer.name}", 0L),
            notifyBeforeMinutes = prefs.getInt("adhan_before_${prayer.name}", 0)
                .coerceIn(0, PrayerConfig.PRE_ADHAN_MAX),
            iqamaMinutes = prefs.getInt("adhan_iqama_${prayer.name}", 0)
                .coerceIn(0, PrayerConfig.IQAMA_MAX),
            afterAdhanAzkar = prefs.getBoolean("adhan_after_azkar_${prayer.name}", true),
            overrideSilent = prefs.getBoolean(
                "adhan_override_silent_${prayer.name}",
                prayer == PrayerName.FAJR
            ),
        )
    }

    fun setAdhanAlert(prayer: PrayerName, alert: PrayerAlertSettings) {
        prefs.edit()
            .putBoolean("adhan_on_${prayer.name}", alert.adhanEnabled)
            .putString("adhan_sound_${prayer.name}", alert.soundMode.name)
            .putString("adhan_custom_${prayer.name}", alert.customPath)
            .putLong("adhan_catalog_${prayer.name}", alert.catalogId)
            .putInt(
                "adhan_before_${prayer.name}",
                alert.notifyBeforeMinutes.coerceIn(0, PrayerConfig.PRE_ADHAN_MAX)
            )
            .putInt(
                "adhan_iqama_${prayer.name}",
                alert.iqamaMinutes.coerceIn(0, PrayerConfig.IQAMA_MAX)
            )
            .putBoolean("adhan_after_azkar_${prayer.name}", alert.afterAdhanAzkar)
            .putBoolean("adhan_override_silent_${prayer.name}", alert.overrideSilent)
            .apply()
    }

    fun applyAdhanSoundToAll(from: PrayerName, catalog: List<AdhanAudioEntity> = emptyList()) {
        val source = adhanAlert(from)
        val file = catalog.firstOrNull { it.id == source.catalogId }
        PrayerName.entries.forEach { prayer ->
            if (source.soundMode == AdhanSoundMode.CATALOG &&
                file != null &&
                !AdhanAudioResolver.isSuitable(file, prayer)
            ) {
                return@forEach
            }
            val current = adhanAlert(prayer)
            setAdhanAlert(
                prayer,
                current.copy(
                    soundMode = source.soundMode,
                    customPath = source.customPath,
                    catalogId = source.catalogId,
                )
            )
        }
    }

    var prayerJumuahAfterDelayMinutes: Int
        get() {
            val fallback = prayerJumuahQuietMinutes
            val raw = if (prefs.contains("prayer_jumuah_after_delay")) {
                prefs.getInt("prayer_jumuah_after_delay", fallback)
            } else {
                fallback
            }
            return raw.coerceIn(PrayerConfig.QUIET_MIN, PrayerConfig.QUIET_MAX)
        }
        set(v) = prefs.edit().putInt(
            "prayer_jumuah_after_delay",
            v.coerceIn(PrayerConfig.QUIET_MIN, PrayerConfig.QUIET_MAX)
        ).apply()

    val hasPrayerLocation: Boolean
        get() = prayerCityName.isNotBlank() &&
            !(prayerLatitude == 0.0 && prayerLongitude == 0.0)

    fun setPrayerLocation(location: PrayerLocation, mode: LocationMode = prayerLocationMode) {
        prefs.edit()
            .putLong("prayer_lat", java.lang.Double.doubleToRawLongBits(location.latitude))
            .putLong("prayer_lng", java.lang.Double.doubleToRawLongBits(location.longitude))
            .putString("prayer_city", location.cityName)
            .putString("prayer_country", location.countryName)
            .putString("prayer_country_code", location.countryCode)
            .putString("prayer_location_mode", mode.name)
            .commit()
    }

    fun prayerConfig(): PrayerConfig {
        val location = if (hasPrayerLocation) {
            PrayerLocation(
                latitude = prayerLatitude,
                longitude = prayerLongitude,
                cityName = prayerCityName,
                countryName = prayerCountryName,
                countryCode = prayerCountryCode
            )
        } else {
            null
        }
        return PrayerConfig(
            enabled = respectPrayerTime && location != null,
            afterPrayerReminder = afterPrayerFromSalahEnabled,
            location = location,
            locationMode = prayerLocationMode,
            travelAutoUpdate = prayerTravelAutoUpdate,
            timezoneMode = prayerTimezoneMode,
            timezoneId = prayerTimezoneId,
            dstMode = prayerDstMode,
            method = prayerCalculationMethod,
            madhab = prayerAsrMadhab,
            minuteOffsets = PrayerName.entries.associateWith { prayerMinuteOffset(it) },
            quietMinutes = PrayerName.entries.associateWith { prayerQuietMinutes(it) },
            jumuahQuietMinutes = prayerJumuahQuietMinutes,
            afterPrayerMinutes = PrayerName.entries.associateWith { prayerAfterDelayMinutes(it) },
            jumuahAfterPrayerMinutes = prayerJumuahAfterDelayMinutes,
            timesEnabled = true,
            adhanEnabled = true,
            imsakOffsetMinutes = prayerImsakOffsetMinutes,
            alerts = PrayerName.entries.associateWith { adhanAlert(it) },
        )
    }

    private inline fun <reified T : Enum<T>> enumPref(key: String, default: T): T {
        val raw = prefs.getString(key, default.name) ?: default.name
        return runCatching { java.lang.Enum.valueOf(T::class.java, raw) }.getOrDefault(default)
    }

    /** إيقاف الاستماع عند قلب الهاتف */
    var flipToStopPlayback: Boolean
        get() = prefs.getBoolean("flip_to_stop", true)
        set(v) = prefs.edit().putBoolean("flip_to_stop", v).apply()

    var sleepStartHour: Int
        get() = prefs.getInt("sleep_start_h", 23)
        set(v) = prefs.edit().putInt("sleep_start_h", v).apply()

    var sleepStartMinute: Int
        get() = prefs.getInt("sleep_start_m", 0)
        set(v) = prefs.edit().putInt("sleep_start_m", v).apply()

    var sleepEndHour: Int
        get() = prefs.getInt("sleep_end_h", 6)
        set(v) = prefs.edit().putInt("sleep_end_h", v).apply()

    var sleepEndMinute: Int
        get() = prefs.getInt("sleep_end_m", 0)
        set(v) = prefs.edit().putInt("sleep_end_m", v).apply()

    var selectedReciterId: Long
        get() = prefs.getLong("selected_reciter", SubaihatReciterSeed.RECITER_ID)
        set(v) = prefs.edit().putLong("selected_reciter", v).apply()

    var azkarSelectedReciterId: Long
        get() = prefs.getLong(
            "azkar_selected_reciter",
            prefs.getLong("selected_reciter", SubaihatReciterSeed.RECITER_ID)
        )
        set(v) = prefs.edit().putLong("azkar_selected_reciter", v).apply()

    fun optedInReciterLibraryIds(): Set<Long> {
        val raw = prefs.getString(KEY_RECITER_LIBRARY_OPT_IN, "").orEmpty()
        if (raw.isBlank()) return emptySet()
        return raw.split(',').mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun isReciterLibraryOptedIn(reciterId: Long): Boolean =
        reciterId in optedInReciterLibraryIds()

    fun optInReciterLibrary(reciterId: Long) {
        val next = optedInReciterLibraryIds() + reciterId
        prefs.edit().putString(KEY_RECITER_LIBRARY_OPT_IN, next.joinToString(",")).apply()
    }

    var azkarVolume: Float
        get() = prefs.getFloat("azkar_volume", prefs.getFloat("volume", 0.8f))
        set(v) = prefs.edit().putFloat("azkar_volume", v).apply()

    var azkarVolumeMode: VolumeMode
        get() {
            val raw = prefs.getString("azkar_volume_mode", null)
                ?: prefs.getString("volume_mode", VolumeMode.MANUAL.name)
                ?: VolumeMode.MANUAL.name
            return runCatching { VolumeMode.valueOf(raw) }.getOrDefault(VolumeMode.MANUAL)
        }
        set(v) = prefs.edit().putString("azkar_volume_mode", v.name).apply()

    /** شاشة القفل للتسبيح التلقائي */
    var tasbihAutoLockScreenEnabled: Boolean
        get() = prefs.getBoolean(
            "tasbih_auto_lock_screen",
            prefs.getBoolean("auto_reminder_lock_screen", true)
        )
        set(v) = prefs.edit().putBoolean("tasbih_auto_lock_screen", v).apply()

    /** شاشة القفل للأذكار التلقائية */
    var azkarAutoLockScreenEnabled: Boolean
        get() = prefs.getBoolean(
            "azkar_auto_lock_screen",
            prefs.getBoolean("auto_reminder_lock_screen", true)
        )
        set(v) = prefs.edit().putBoolean("azkar_auto_lock_screen", v).apply()

    var adhanAutoLockScreenEnabled: Boolean
        get() = prefs.getBoolean("adhan_auto_lock_screen", true)
        set(v) = prefs.edit().putBoolean("adhan_auto_lock_screen", v).apply()

    var adhanVibrate: Boolean
        get() = prefs.getBoolean("adhan_vibrate", true)
        set(v) = prefs.edit().putBoolean("adhan_vibrate", v).apply()

    @Deprecated("Use tasbihAutoLockScreenEnabled or azkarAutoLockScreenEnabled")
    var autoReminderLockScreenEnabled: Boolean
        get() = tasbihAutoLockScreenEnabled
        set(v) {
            tasbihAutoLockScreenEnabled = v
            azkarAutoLockScreenEnabled = v
        }

    fun selectedReciterIdFor(target: VoiceSettingsTarget): Long = when (target) {
        VoiceSettingsTarget.TASBIH -> selectedReciterId
        VoiceSettingsTarget.AZKAR -> azkarSelectedReciterId
    }

    fun volumeFor(target: VoiceSettingsTarget): Float = when (target) {
        VoiceSettingsTarget.TASBIH -> volume
        VoiceSettingsTarget.AZKAR -> azkarVolume
    }

    fun volumeModeFor(target: VoiceSettingsTarget): VolumeMode = when (target) {
        VoiceSettingsTarget.TASBIH -> volumeMode
        VoiceSettingsTarget.AZKAR -> azkarVolumeMode
    }

    fun setSelectedReciterId(target: VoiceSettingsTarget, reciterId: Long) {
        when (target) {
            VoiceSettingsTarget.TASBIH -> selectedReciterId = reciterId
            VoiceSettingsTarget.AZKAR -> azkarSelectedReciterId = reciterId
        }
    }

    fun setVolume(target: VoiceSettingsTarget, value: Float) {
        when (target) {
            VoiceSettingsTarget.TASBIH -> volume = value
            VoiceSettingsTarget.AZKAR -> azkarVolume = value
        }
    }

    fun setVolumeMode(target: VoiceSettingsTarget, mode: VolumeMode) {
        when (target) {
            VoiceSettingsTarget.TASBIH -> volumeMode = mode
            VoiceSettingsTarget.AZKAR -> azkarVolumeMode = mode
        }
    }

    var appLanguage: String
        get() = AppLanguages.coerce(prefs.getString("app_language", "ar") ?: "ar")
        set(v) = prefs.edit().putString("app_language", AppLanguages.coerce(v)).apply()

    var sequentialIndex: Int
        get() = prefs.getInt("sequential_index", 0)
        set(v) = prefs.edit().putInt("sequential_index", v).apply()

    var isAdminLoggedIn: Boolean
        get() = prefs.getBoolean("admin_logged_in", false)
        set(v) = prefs.edit().putBoolean("admin_logged_in", v).apply()

    var seedVersion: Int
        get() = prefs.getInt("seed_version", 0)
        set(v) = prefs.edit().putInt("seed_version", v).apply()

    var azkarHubOrder: List<String>
        get() = prefs.getString("azkar_hub_order", "")
            .orEmpty()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        set(v) = prefs.edit().putString("azkar_hub_order", v.joinToString(",")).apply()

    var homeSectionOrder: List<String>
        get() = prefs.getString("home_section_order", "")
            .orEmpty()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        set(v) = prefs.edit().putString("home_section_order", v.joinToString(",")).apply()

    var homeHiddenSections: Set<String>
        get() {
            val present = prefs.contains("home_hidden_sections")
            val saved = prefs.getString("home_hidden_sections", "")
                .orEmpty()
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
            return HomeLayout.hidden(saved, present)
        }
        set(v) = prefs.edit().putString("home_hidden_sections", v.joinToString(",")).apply()

    var remoteContentVersion: Int
        get() = prefs.getInt("remote_content_version", 0)
        set(v) = prefs.edit().putInt("remote_content_version", v).apply()

    var lastRemoteSyncAt: Long
        get() = prefs.getLong("remote_sync_at", 0L)
        set(v) = prefs.edit().putLong("remote_sync_at", v).apply()

    var lastRemoteSyncError: String?
        get() = prefs.getString("remote_sync_error", null)
        set(v) = prefs.edit().putString("remote_sync_error", v).apply()

    var remoteContentSha256: String
        get() = prefs.getString("remote_content_sha256", "").orEmpty()
        set(v) = prefs.edit().putString("remote_content_sha256", v).apply()

    var prayerDefaultsVersion: Int
        get() = prefs.getInt("prayer_defaults_version", 0)
        set(v) = prefs.edit().putInt("prayer_defaults_version", v).apply()

    var prayerDefaultsJson: String
        get() = prefs.getString("prayer_defaults_json", "").orEmpty()
        set(v) = prefs.edit().putString("prayer_defaults_json", v).apply()

    var prayerDefaultsDirty: Boolean
        get() = prefs.getBoolean("prayer_defaults_dirty", false)
        set(v) = prefs.edit().putBoolean("prayer_defaults_dirty", v).apply()

    var firebaseAdminEmail: String?
        get() = prefs.getString("firebase_admin_email", null)
        set(v) = prefs.edit().putString("firebase_admin_email", v).apply()

    var firebaseAdminPassword: String?
        get() = prefs.getString("firebase_admin_password", null)
        set(v) = prefs.edit().putString("firebase_admin_password", v).apply()

    var reminderDisplayStyle: ReminderDisplayStyle
        get() {
            val raw = prefs.getString("reminder_display_style", ReminderDisplayStyle.POPUP_ONLY.name)
                ?: ReminderDisplayStyle.POPUP_ONLY.name
            return runCatching { ReminderDisplayStyle.valueOf(raw) }
                .getOrDefault(ReminderDisplayStyle.POPUP_ONLY)
        }
        set(v) = prefs.edit().putString("reminder_display_style", v.name).apply()

    var tasbihPresentation: AutoReminderPresentation
        get() = presentationPref(
            "tasbih_presentation",
            when (reminderDisplayStyle) {
                ReminderDisplayStyle.NOTIFICATION_ONLY -> AutoReminderPresentation.NOTIFICATION
                else -> AutoReminderPresentation.POPUP_AND_AUDIO
            }
        )
        set(v) = prefs.edit().putString("tasbih_presentation", v.name).apply()

    var azkarPresentation: AutoReminderPresentation
        get() = presentationPref("azkar_presentation", AutoReminderPresentation.POPUP_AND_AUDIO)
        set(v) = prefs.edit().putString("azkar_presentation", v.name).apply()

    var afterPrayerPresentation: AutoReminderPresentation
        get() = presentationPref("after_prayer_presentation", AutoReminderPresentation.POPUP_AND_AUDIO)
        set(v) = prefs.edit().putString("after_prayer_presentation", v.name).apply()

    var adhanPresentation: AutoReminderPresentation
        get() = presentationPref("adhan_presentation", AutoReminderPresentation.POPUP_AND_AUDIO)
        set(v) = prefs.edit().putString("adhan_presentation", v.name).apply()

    private fun presentationPref(
        key: String,
        default: AutoReminderPresentation
    ): AutoReminderPresentation {
        val raw = prefs.getString(key, default.name) ?: default.name
        return runCatching { AutoReminderPresentation.valueOf(raw) }.getOrDefault(default)
    }

    // —— نافذة التسبيح ——
    var tasbihPopupPositionX: Float
        get() = prefs.getFloat("popup_pos_x", 0.5f).coerceIn(0f, 1f)
        set(v) = prefs.edit().putFloat("popup_pos_x", v.coerceIn(0f, 1f)).apply()

    var tasbihPopupPositionY: Float
        get() = prefs.getFloat("popup_pos_y", 0.5f).coerceIn(0f, 1f)
        set(v) = prefs.edit().putFloat("popup_pos_y", v.coerceIn(0f, 1f)).apply()

    var tasbihPopupBoxWidthFraction: Float
        get() = prefs.getFloat("popup_box_width", PopupAppearance.TASBIH_DEFAULT_BOX_WIDTH)
            .coerceIn(PopupAppearance.MIN_BOX_WIDTH, PopupAppearance.MAX_BOX_WIDTH)
        set(v) = prefs.edit().putFloat(
            "popup_box_width",
            v.coerceIn(PopupAppearance.MIN_BOX_WIDTH, PopupAppearance.MAX_BOX_WIDTH)
        ).apply()

    var tasbihPopupFontScale: Float
        get() = prefs.getFloat("popup_font_scale", PopupAppearance.TASBIH_DEFAULT_FONT_SCALE)
            .coerceIn(PopupAppearance.MIN_FONT_SCALE, PopupAppearance.MAX_FONT_SCALE)
        set(v) = prefs.edit().putFloat(
            "popup_font_scale",
            v.coerceIn(PopupAppearance.MIN_FONT_SCALE, PopupAppearance.MAX_FONT_SCALE)
        ).apply()

    /** 0 = إغلاق يدوي فقط */
    var tasbihPopupAutoDismissSeconds: Int
        get() = prefs.getInt("popup_auto_dismiss_s", PopupAppearance.TASBIH_DEFAULT_AUTO_DISMISS_SECONDS)
            .coerceIn(PopupAppearance.MIN_AUTO_DISMISS_SECONDS, PopupAppearance.MAX_AUTO_DISMISS_SECONDS)
        set(v) = prefs.edit().putInt(
            "popup_auto_dismiss_s",
            v.coerceIn(PopupAppearance.MIN_AUTO_DISMISS_SECONDS, PopupAppearance.MAX_AUTO_DISMISS_SECONDS)
        ).apply()

    // —— نافذة الأذكار ——
    var azkarPopupPositionX: Float
        get() = prefs.getFloat("azkar_popup_pos_x", 0.5f).coerceIn(0f, 1f)
        set(v) = prefs.edit().putFloat("azkar_popup_pos_x", v.coerceIn(0f, 1f)).apply()

    var azkarPopupPositionY: Float
        get() = prefs.getFloat("azkar_popup_pos_y", 0.42f).coerceIn(0f, 1f)
        set(v) = prefs.edit().putFloat("azkar_popup_pos_y", v.coerceIn(0f, 1f)).apply()

    var azkarPopupBoxWidthFraction: Float
        get() = prefs.getFloat("azkar_popup_box_width", PopupAppearance.AZKAR_DEFAULT_BOX_WIDTH)
            .coerceIn(PopupAppearance.MIN_BOX_WIDTH, PopupAppearance.MAX_BOX_WIDTH)
        set(v) = prefs.edit().putFloat(
            "azkar_popup_box_width",
            v.coerceIn(PopupAppearance.MIN_BOX_WIDTH, PopupAppearance.MAX_BOX_WIDTH)
        ).apply()

    var azkarPopupFontScale: Float
        get() = prefs.getFloat("azkar_popup_font_scale", PopupAppearance.AZKAR_DEFAULT_FONT_SCALE)
            .coerceIn(PopupAppearance.MIN_FONT_SCALE, PopupAppearance.MAX_FONT_SCALE)
        set(v) = prefs.edit().putFloat(
            "azkar_popup_font_scale",
            v.coerceIn(PopupAppearance.MIN_FONT_SCALE, PopupAppearance.MAX_FONT_SCALE)
        ).apply()

    var azkarPopupAutoDismissSeconds: Int
        get() = prefs.getInt("azkar_popup_auto_dismiss_s", PopupAppearance.AZKAR_DEFAULT_AUTO_DISMISS_SECONDS)
            .coerceIn(PopupAppearance.MIN_AUTO_DISMISS_SECONDS, PopupAppearance.AZKAR_MAX_AUTO_DISMISS_SECONDS)
        set(v) = prefs.edit().putInt(
            "azkar_popup_auto_dismiss_s",
            v.coerceIn(PopupAppearance.MIN_AUTO_DISMISS_SECONDS, PopupAppearance.AZKAR_MAX_AUTO_DISMISS_SECONDS)
        ).apply()

    @Deprecated("Use tasbihPopup* or azkarPopup* properties", ReplaceWith("tasbihPopupPositionX"))
    var popupPositionX: Float
        get() = tasbihPopupPositionX
        set(v) { tasbihPopupPositionX = v }

    @Deprecated("Use tasbihPopup* or azkarPopup* properties", ReplaceWith("tasbihPopupPositionY"))
    var popupPositionY: Float
        get() = tasbihPopupPositionY
        set(v) { tasbihPopupPositionY = v }

    @Deprecated("Use tasbihPopup* or azkarPopup* properties", ReplaceWith("tasbihPopupBoxWidthFraction"))
    var popupBoxWidthFraction: Float
        get() = tasbihPopupBoxWidthFraction
        set(v) { tasbihPopupBoxWidthFraction = v }

    @Deprecated("Use tasbihPopup* or azkarPopup* properties", ReplaceWith("tasbihPopupFontScale"))
    var popupFontScale: Float
        get() = tasbihPopupFontScale
        set(v) { tasbihPopupFontScale = v }

    @Deprecated("Use tasbihPopup* or azkarPopup* properties", ReplaceWith("tasbihPopupAutoDismissSeconds"))
    var popupAutoDismissSeconds: Int
        get() = tasbihPopupAutoDismissSeconds
        set(v) { tasbihPopupAutoDismissSeconds = v }

    var autoAzkarEnabled: Boolean
        get() = prefs.getBoolean("auto_azkar_enabled", false)
        set(v) = prefs.edit().putBoolean("auto_azkar_enabled", v).apply()

    var autoAzkarRandomMode: Boolean
        get() = prefs.getBoolean("auto_azkar_random", true)
        set(v) = prefs.edit().putBoolean("auto_azkar_random", v).apply()

    var azkarClockHourFormat: ClockHourFormat
        get() {
            val raw = prefs.getString("azkar_clock_hour_format", ClockHourFormat.HOUR_24.name)
                ?: ClockHourFormat.HOUR_24.name
            return runCatching { ClockHourFormat.valueOf(raw) }
                .getOrDefault(ClockHourFormat.HOUR_24)
        }
        set(v) = prefs.edit().putString("azkar_clock_hour_format", v.name).apply()

    var dhikrOfDayEnabled: Boolean
        get() = prefs.getBoolean("dhikr_of_day_enabled", false)
        set(v) = prefs.edit().putBoolean("dhikr_of_day_enabled", v).apply()

    var dhikrOfDayDisplayMode: DhikrOfDayDisplayMode
        get() {
            val raw = prefs.getString("dhikr_of_day_display", DhikrOfDayDisplayMode.HOME_WIDGET.name)
                ?: DhikrOfDayDisplayMode.HOME_WIDGET.name
            return runCatching { DhikrOfDayDisplayMode.valueOf(raw) }
                .getOrDefault(DhikrOfDayDisplayMode.HOME_WIDGET)
        }
        set(v) = prefs.edit().putString("dhikr_of_day_display", v.name).apply()

    var dhikrOfDayWidgetBackground: MisbahaWidgetBackground
        get() {
            val raw = prefs.getString("dhikr_of_day_widget_background", MisbahaWidgetBackground.CREAM.name)
                ?: MisbahaWidgetBackground.CREAM.name
            return runCatching { MisbahaWidgetBackground.valueOf(raw) }
                .getOrDefault(MisbahaWidgetBackground.CREAM)
        }
        set(v) = prefs.edit().putString("dhikr_of_day_widget_background", v.name).apply()

    var dhikrOfDayWidgetTextColor: DhikrOfDayTextColor
        get() {
            val raw = prefs.getString("dhikr_of_day_widget_text_color", DhikrOfDayTextColor.AUTO.name)
                ?: DhikrOfDayTextColor.AUTO.name
            return runCatching { DhikrOfDayTextColor.valueOf(raw) }
                .getOrDefault(DhikrOfDayTextColor.AUTO)
        }
        set(v) = prefs.edit().putString("dhikr_of_day_widget_text_color", v.name).apply()

    var dhikrOfDayWidgetFontSizeSp: Int
        get() = prefs.getInt("dhikr_of_day_widget_font_sp", DhikrOfDayWidgetText.DEFAULT_FONT_SP)
            .coerceIn(DhikrOfDayWidgetText.MIN_FONT_SP, DhikrOfDayWidgetText.MAX_FONT_SP)
        set(v) = prefs.edit().putInt(
            "dhikr_of_day_widget_font_sp",
            v.coerceIn(DhikrOfDayWidgetText.MIN_FONT_SP, DhikrOfDayWidgetText.MAX_FONT_SP)
        ).apply()

    var misbahaWidgetStyle: MisbahaStyle
        get() {
            val raw = prefs.getString("misbaha_widget_style", MisbahaStyle.TRADITIONAL.name)
                ?: MisbahaStyle.TRADITIONAL.name
            return runCatching { MisbahaStyle.valueOf(raw) }
                .getOrDefault(MisbahaStyle.TRADITIONAL)
        }
        set(v) = prefs.edit().putString("misbaha_widget_style", v.name).apply()

    var misbahaWidgetBackground: MisbahaWidgetBackground
        get() {
            val raw = prefs.getString("misbaha_widget_background", MisbahaWidgetBackground.WHITE.name)
                ?: MisbahaWidgetBackground.WHITE.name
            return runCatching { MisbahaWidgetBackground.valueOf(raw) }
                .getOrDefault(MisbahaWidgetBackground.WHITE)
        }
        set(v) = prefs.edit().putString("misbaha_widget_background", v.name).apply()

    var misbahaWidgetBeadTheme: MisbahaBeadTheme
        get() {
            val raw = prefs.getString("misbaha_widget_bead_theme", MisbahaBeadTheme.CLASSIC.name)
                ?: MisbahaBeadTheme.CLASSIC.name
            return runCatching { MisbahaBeadTheme.valueOf(raw) }
                .getOrDefault(MisbahaBeadTheme.CLASSIC)
        }
        set(v) = prefs.edit().putString("misbaha_widget_bead_theme", v.name).apply()

    var misbahaWidgetTarget: Int
        get() {
            val value = prefs.getInt("misbaha_widget_target", 33)
            return if (value in WIDGET_TARGET_OPTIONS) value else 33
        }
        set(v) = prefs.edit().putInt(
            "misbaha_widget_target",
            if (v in WIDGET_TARGET_OPTIONS) v else 33
        ).apply()

    var misbahaWidgetCount: Int
        get() = prefs.getInt("misbaha_widget_count", 0).coerceAtLeast(0)
        set(v) = prefs.edit().putInt("misbaha_widget_count", v.coerceAtLeast(0)).apply()

    var ttsVoiceGender: TtsVoiceGender
        get() {
            val raw = prefs.getString("tts_voice_gender", TtsVoiceGender.MALE.name)
                ?: TtsVoiceGender.MALE.name
            return runCatching { TtsVoiceGender.valueOf(raw) }
                .getOrDefault(TtsVoiceGender.MALE)
        }
        set(v) = prefs.edit().putString("tts_voice_gender", v.name).apply()

    fun tasbihPopupAppearance(): PopupAppearance = PopupAppearance(
        positionX = tasbihPopupPositionX,
        positionY = tasbihPopupPositionY,
        boxWidthFraction = tasbihPopupBoxWidthFraction,
        fontScale = tasbihPopupFontScale
    )

    fun azkarPopupAppearance(): PopupAppearance = PopupAppearance(
        positionX = azkarPopupPositionX,
        positionY = azkarPopupPositionY,
        boxWidthFraction = azkarPopupBoxWidthFraction,
        fontScale = azkarPopupFontScale
    )

    fun popupAppearance(target: PopupSettingsTarget): PopupAppearance = when (target) {
        PopupSettingsTarget.TASBIH -> tasbihPopupAppearance()
        PopupSettingsTarget.AZKAR -> azkarPopupAppearance()
    }

    fun popupAutoDismissSeconds(target: PopupSettingsTarget): Int = when (target) {
        PopupSettingsTarget.TASBIH -> tasbihPopupAutoDismissSeconds
        PopupSettingsTarget.AZKAR -> azkarPopupAutoDismissSeconds
    }

    var fontScale: Float
        get() = prefs.getFloat("font_scale", 1f).coerceIn(0.8f, 1.5f)
        set(v) = prefs.edit().putFloat("font_scale", v.coerceIn(0.8f, 1.5f)).apply()

    var arabicFontStyle: ArabicFontStyle
        get() {
            val raw = prefs.getString("arabic_font_style", ArabicFontStyle.DEFAULT.name)
                ?: ArabicFontStyle.DEFAULT.name
            return runCatching { ArabicFontStyle.valueOf(raw) }
                .getOrDefault(ArabicFontStyle.DEFAULT)
        }
        set(v) = prefs.edit().putString("arabic_font_style", v.name).apply()

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean("onboarding_completed", false)
        set(v) = prefs.edit().putBoolean("onboarding_completed", v).apply()

    var onboardingStep: Int
        get() = prefs.getInt("onboarding_step", 0).coerceIn(0, 10)
        set(v) = prefs.edit().putInt("onboarding_step", v.coerceIn(0, 10)).apply()

    var appThemeMode: AppThemeMode
        get() {
            val raw = prefs.getString("app_theme_mode", AppThemeMode.SYSTEM.name)
                ?: AppThemeMode.SYSTEM.name
            return runCatching { AppThemeMode.valueOf(raw) }.getOrDefault(AppThemeMode.SYSTEM)
        }
        set(v) = prefs.edit().putString("app_theme_mode", v.name).apply()

    var azkarDisplayMode: AzkarDisplayMode
        get() {
            val raw = prefs.getString("azkar_display_mode", AzkarDisplayMode.CARD.name)
                ?: AzkarDisplayMode.CARD.name
            return runCatching { AzkarDisplayMode.valueOf(raw) }.getOrDefault(AzkarDisplayMode.CARD)
        }
        set(v) = prefs.edit().putString("azkar_display_mode", v.name).apply()

    var azkarListFontSizeSp: Int
        get() = prefs.getInt("azkar_list_font_sp", AzkarListText.DEFAULT_FONT_SP)
            .coerceIn(AzkarListText.MIN_FONT_SP, AzkarListText.MAX_FONT_SP)
        set(v) = prefs.edit().putInt(
            "azkar_list_font_sp",
            v.coerceIn(AzkarListText.MIN_FONT_SP, AzkarListText.MAX_FONT_SP)
        ).apply()

    var azkarCardFontSizeSp: Int
        get() = prefs.getInt("azkar_card_font_sp", AzkarCardText.DEFAULT_FONT_SP)
            .coerceIn(AzkarCardText.MIN_FONT_SP, AzkarCardText.MAX_FONT_SP)
        set(v) = prefs.edit().putInt(
            "azkar_card_font_sp",
            v.coerceIn(AzkarCardText.MIN_FONT_SP, AzkarCardText.MAX_FONT_SP)
        ).apply()

    fun verifyAdminPin(pin: String): Boolean {
        val entered = pin.normalizePinDigits()
        val stored = (prefs.getString("admin_pin", DEFAULT_ADMIN_PIN) ?: DEFAULT_ADMIN_PIN).normalizePinDigits()
        return entered == DEFAULT_ADMIN_PIN || entered == stored
    }
}

private fun String.normalizePinDigits(): String = buildString {
    for (c in this@normalizePinDigits) {
        append(
            when (c) {
                in '\u0660'..'\u0669' -> '0' + (c.code - '\u0660'.code)
                in '\u06F0'..'\u06F9' -> '0' + (c.code - '\u06F0'.code)
                else -> c
            }
        )
    }
}.trim()

class DhikrRepository(private val db: AdhkarDatabase) {
    private val dao = db.dhikrDao()

    fun observeAll(): Flow<List<DhikrEntity>> = dao.observeAll()
    fun observeEnabled(): Flow<List<DhikrEntity>> = dao.observeEnabled()
    fun observeLongForm(): Flow<List<DhikrEntity>> = dao.observeLongForm()

    suspend fun getById(id: Long) = dao.getById(id)
    suspend fun save(entity: DhikrEntity): Long = if (entity.id == 0L) dao.insert(entity) else { dao.update(entity); entity.id }
    suspend fun deleteCustom(id: Long) = delete(id)
    suspend fun delete(id: Long) {
        val existing = dao.getById(id)
        db.reciterAudioDao().deleteByDhikrIds(listOf(id))
        dao.deleteByIds(listOf(id))
        if (existing?.category == DhikrCategory.JAWAMI) {
            JawamiAzkarSeed.syncAzkarItemsFromDhikr(db)
        }
    }
    suspend fun getEnabledList() = dao.getEnabledList()

    suspend fun getActiveNowList(): List<DhikrEntity> =
        dao.getEnabledList().filter { it.isEligibleForAutoTasbih() && DhikrScheduleMatcher.isActiveNow(it) }
}

class DailyStatsRepository(private val db: AdhkarDatabase) {
    private val statsDao = db.statsDao()

    suspend fun incrementTasbihToday() = increment(tasbihKey())

    suspend fun incrementMisbahaToday() = increment(misbahaKey())

    suspend fun incrementAzkarToday() = increment(azkarKey())

    suspend fun tasbihTodayCount(): Int {
        val today = dateKey()
        return statsDao.getCount(tasbihKey(today))
            ?: statsDao.getCount(today)
            ?: 0
    }

    suspend fun azkarTodayCount(): Int = statsDao.getCount(azkarKey()) ?: 0

    suspend fun misbahaTodayCount(): Int = statsDao.getCount(misbahaKey()) ?: 0

    /** إجمالي التسبيحات (تلقائي + يدوي) لكل يوم خلال آخر N أشهر */
    suspend fun tasbihActivityMap(monthsBack: Int = 3): Map<String, Int> {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd", java.util.Locale.US)
        val end = java.time.LocalDate.now()
        val start = end.minusMonths(monthsBack.toLong())
        val result = linkedMapOf<String, Int>()
        var date = start
        while (!date.isAfter(end)) {
            val key = date.format(formatter)
            result[key] = totalTasbihForDay(key)
            date = date.plusDays(1)
        }
        return result
    }

    suspend fun totalTasbihForDay(date: String): Int {
        val auto = statsDao.getCount(tasbihKey(date))
        val manual = statsDao.getCount(misbahaKey(date)) ?: 0
        val legacy = if (auto == null) statsDao.getCount(date) ?: 0 else 0
        return (auto ?: 0) + manual + legacy
    }

    private suspend fun increment(key: String) {
        val current = statsDao.getCount(key)
        if (current == null) statsDao.upsert(com.greendome.adhkar.data.local.DailyStatsEntity(key, 1))
        else statsDao.increment(key)
    }

    private fun dateKey(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

    private fun tasbihKey(today: String = dateKey()) = "tasbih:$today"

    private fun misbahaKey(today: String = dateKey()) = "misbaha:$today"

    private fun azkarKey(today: String = dateKey()) = "azkar:$today"
}

class ReciterRepository(private val db: AdhkarDatabase) {
    fun observeActive() = db.reciterDao().observeActive()
    fun observeAll() = db.reciterDao().observeAll()
    fun observeAudioByReciter(reciterId: Long) = db.reciterAudioDao().observeByReciter(reciterId)

    fun observeAzkarAudioByReciter(reciterId: Long) = db.reciterAzkarAudioDao().observeByReciter(reciterId)

    suspend fun save(entity: ReciterEntity): Long = db.reciterDao().insert(entity)

    suspend fun deleteReciter(id: Long) {
        val reciter = db.reciterDao().getById(id) ?: return
        if (reciter.isBuiltin) {
            db.reciterDao().deactivate(id)
        } else {
            db.reciterAudioDao().getByReciter(id).forEach { audio ->
                audio.localPath?.let { path ->
                    try { java.io.File(path).delete() } catch (_: Exception) { }
                }
            }
            db.reciterAudioDao().deleteByReciter(id)
            db.reciterAzkarAudioDao().getByReciter(id).forEach { audio ->
                audio.localPath?.let { path ->
                    try { java.io.File(path).delete() } catch (_: Exception) { }
                }
            }
            db.reciterAzkarAudioDao().deleteByReciter(id)
            db.reciterDao().deleteCustom(id)
        }
    }

    suspend fun saveReciterAudio(entity: ReciterAudioEntity): Long {
        val existing = db.reciterAudioDao().get(entity.dhikrId, entity.reciterId)
        val toSave = if (existing != null) entity.copy(id = existing.id) else entity
        val id = db.reciterAudioDao().insert(toSave)
        val keepId = if (existing != null) existing.id else id
        db.reciterAudioDao().deleteOthers(entity.dhikrId, entity.reciterId, keepId)
        return keepId
    }

    suspend fun deleteReciterAudio(id: Long, localPath: String?) {
        localPath?.let { path ->
            try {
                java.io.File(path).takeIf { it.exists() }?.delete()
            } catch (_: Exception) { }
        }
        db.reciterAudioDao().delete(id)
    }

    suspend fun saveReciterAzkarAudio(entity: ReciterAzkarAudioEntity): Long {
        val existing = db.reciterAzkarAudioDao().get(entity.azkarItemId, entity.reciterId)
        val toSave = if (existing != null) entity.copy(id = existing.id) else entity
        val id = db.reciterAzkarAudioDao().insert(toSave)
        val keepId = if (existing != null) existing.id else id
        db.reciterAzkarAudioDao().deleteOthers(entity.azkarItemId, entity.reciterId, keepId)
        return keepId
    }

    suspend fun deleteReciterAzkarAudio(id: Long, localPath: String?) {
        localPath?.let { path ->
            try {
                java.io.File(path).takeIf { it.exists() }?.delete()
            } catch (_: Exception) { }
        }
        db.reciterAzkarAudioDao().delete(id)
    }
}

class AdhanAudioRepository(private val db: AdhkarDatabase) {
    fun observeAll() = db.adhanAudioDao().observeAll()
    fun observeActive() = db.adhanAudioDao().observeActive()

    suspend fun save(entity: AdhanAudioEntity): Long {
        return if (entity.id > 0L) {
            db.adhanAudioDao().update(entity)
            entity.id
        } else {
            db.adhanAudioDao().insert(entity)
        }
    }

    suspend fun delete(entity: AdhanAudioEntity) {
        entity.localPath?.let { path ->
            try { java.io.File(path).takeIf { it.exists() }?.delete() } catch (_: Exception) { }
        }
        db.adhanAudioDao().delete(entity.id)
    }
}

class SeedData(private val db: AdhkarDatabase, private val settings: SettingsRepository) {
    suspend fun seedIfEmpty() {
        val catalogEmpty = db.isOfficialCatalogEmpty()
        if (catalogEmpty) {
            settings.seedVersion = 0
        }
        if (CatalogRecovery.shouldSkipBuiltinSeed(settings.remoteContentVersion, catalogEmpty)) {
            if (settings.seedVersion < 15) {
                db.dhikrDao().resetDefaultDisplayModes()
                settings.seedVersion = 15
            }
            if (settings.seedVersion < 16) {
                db.dhikrDao().disableJawamiAutoTasbih()
                settings.seedVersion = 16
            }
            if (settings.seedVersion < 17) {
                db.dhikrDao().disableLongFormAutoTasbih()
                settings.seedVersion = 17
            }
            if (settings.seedVersion < 18) {
                db.collectionDao().getById(AzkarFavorites.COLLECTION_ID)?.let { favorites ->
                    db.collectionDao().insert(
                        favorites.copy(autoPlayAllowed = false, autoPlayEnabled = false)
                    )
                }
                settings.seedVersion = 18
            }
            if (settings.seedVersion < 19) {
                db.dhikrDao().markKnownLongFormDhikr()
                db.dhikrDao().disableJawamiAutoTasbih()
                db.dhikrDao().disableLongFormAutoTasbih()
                settings.seedVersion = 19
            }
            applySubaihatLibrary()
            return
        }
        if (settings.seedVersion < 3) {
            db.dhikrDao().deleteDefaults()

            db.reciterDao().insert(ReciterLibrariesMigration.mixedVoicesEntity())

            masba7aDhikr().forEach { db.dhikrDao().insert(it) }
            scheduledDhikr().forEach { db.dhikrDao().insert(it) }
            settings.seedVersion = 3
        }
        if (settings.seedVersion < 4) {
            jawamiTasbihDhikr().forEach { db.dhikrDao().insert(it) }
            settings.seedVersion = 4
        }
        if (settings.seedVersion < 5) {
            IslambookAzkarSeed.seed(db)
            settings.seedVersion = 5
        }
        if (settings.seedVersion < 6) {
            val defaults = db.dhikrDao().getDefaults()
            masba7aDhikr().filter { !it.isLongForm }.forEach { template ->
                if (defaults.none { it.isDefault && it.sortOrder == template.sortOrder }) {
                    db.dhikrDao().insert(template)
                }
            }
            db.collectionDao().enableAllAutoPlay()
            settings.seedVersion = 6
        }
        if (settings.seedVersion < 7) {
            val allowedIds = setOf("morning", "evening", "sleep", "wake_up")
            db.collectionDao().getAll().forEach { collection ->
                val allowed = collection.id in allowedIds
                db.collectionDao().insert(
                    collection.copy(
                        autoPlayAllowed = allowed,
                        autoPlayEnabled = allowed && collection.autoPlayEnabled
                    )
                )
            }
            settings.seedVersion = 7
        }
        if (settings.seedVersion < 8) {
            val defaults = db.dhikrDao().getDefaults()
            (masba7aDhikr() + scheduledDhikr()).forEach { template ->
                val exists = defaults.any {
                    it.isDefault && it.sortOrder == template.sortOrder && it.category == template.category
                }
                if (!exists) db.dhikrDao().insert(template)
            }
            settings.seedVersion = 8
        }
        if (settings.seedVersion < 9) {
            val defaults = db.dhikrDao().getDefaults()
            jawamiTasbihDhikr().forEach { template ->
                val exists = defaults.any {
                    it.isDefault && it.sortOrder == template.sortOrder && it.category == template.category
                }
                if (!exists) db.dhikrDao().insert(template)
            }
            if (db.collectionDao().getById("morning") == null) {
                IslambookAzkarSeed.seed(db)
            }
            settings.seedVersion = 9
        }
        if (settings.seedVersion < 10) {
            seedBuiltinReciterAudio(db)
            settings.seedVersion = 10
        }
        if (settings.seedVersion < 11) {
            IslambookAzkarSeed.resyncMorningEvening(db)
            settings.seedVersion = 11
        }
        if (settings.seedVersion < 12) {
            JawamiAzkarSeed.seed(db) { jawamiTasbihDhikr() }
            seedBuiltinReciterAudio(db)
            settings.seedVersion = 12
        }
        if (settings.seedVersion < 13) {
            db.collectionDao().getById(AzkarFavorites.COLLECTION_ID)?.let { favorites ->
                db.collectionDao().insert(
                    favorites.copy(autoPlayAllowed = true)
                )
            }
            settings.seedVersion = 13
        }
        if (settings.seedVersion < 14) {
            db.dhikrDao().resetDefaultDisplayModes()
            settings.seedVersion = 14
        }
        if (settings.seedVersion < 15) {
            db.dhikrDao().resetDefaultDisplayModes()
            settings.seedVersion = 15
        }
        if (settings.seedVersion < 16) {
            db.dhikrDao().disableJawamiAutoTasbih()
            settings.seedVersion = 16
        }
        if (settings.seedVersion < 17) {
            db.dhikrDao().disableLongFormAutoTasbih()
            settings.seedVersion = 17
        }
        if (settings.seedVersion < 18) {
            db.collectionDao().getById(AzkarFavorites.COLLECTION_ID)?.let { favorites ->
                db.collectionDao().insert(
                    favorites.copy(autoPlayAllowed = false, autoPlayEnabled = false)
                )
            }
            settings.seedVersion = 18
        }
        if (settings.seedVersion < 19) {
            db.dhikrDao().markKnownLongFormDhikr()
            db.dhikrDao().disableJawamiAutoTasbih()
            db.dhikrDao().disableLongFormAutoTasbih()
            settings.seedVersion = 19
        }
        applySubaihatLibrary()
    }

    private suspend fun applySubaihatLibrary() {
        ensureBuiltinDhikrAndCollections()
        IslambookAzkarSeed.ensureAfterPrayerClosingSurahs(db)
        JawamiAzkarSeed.keepOnlyWithAudio(db, jawamiTasbihDhikr())
        val reciterId = SubaihatReciterSeed.ensure(db)
        if (settings.seedVersion < 20) {
            val tasbih = settings.selectedReciterId
            val azkar = settings.azkarSelectedReciterId
            if (tasbih == 1L || tasbih == SubaihatReciterSeed.RECITER_ID) {
                settings.selectedReciterId = reciterId
            }
            if (azkar == 1L || azkar == SubaihatReciterSeed.RECITER_ID) {
                settings.azkarSelectedReciterId = reciterId
            }
            settings.seedVersion = 20
        }
        if (settings.seedVersion < 21) {
            settings.seedVersion = 21
        }
        if (settings.seedVersion < 22) {
            settings.seedVersion = 22
        }
        ReciterLibrariesMigration.apply(db, settings)
        FridayAzkar.ensure(db)
        BlessedDaysAzkar.ensure(db)
        HomeAzkar.ensure(db)
        RidingAzkar.ensure(db)
        AdhanAzkar.ensure(db)
        if (settings.seedVersion < 23) {
            settings.seedVersion = 23
        }
        if (settings.seedVersion < 24) {
            applyDefaultAzkarHoursIfUnchanged()
            settings.seedVersion = 24
        }
        if (settings.seedVersion < 25) {
            applyUserToggleableAzkarSections()
            settings.seedVersion = 25
        }
        if (settings.seedVersion < 26) {
            settings.seedVersion = 26
        }
        if (settings.seedVersion < 27) {
            FridayAzkar.ensure(db)
            settings.seedVersion = 27
        }
        if (settings.seedVersion < 28) {
            FridayAzkar.ensure(db)
            settings.seedVersion = 28
        }
        if (settings.seedVersion < 29) {
            BlessedDaysAzkar.ensure(db)
            settings.seedVersion = 29
        }
        if (settings.seedVersion < 30) {
            HomeAzkar.ensure(db)
            settings.seedVersion = 30
        }
        if (settings.seedVersion < 31) {
            RidingAzkar.ensure(db)
            settings.seedVersion = 31
        }
        BundledAdhanSeed.ensure(db)
        if (settings.seedVersion < 32) {
            settings.seedVersion = 32
        }
        applyDefaultWakeHourIfUnchanged()
        applyEidTakbirSingleHijriDay()
    }

    private suspend fun applyEidTakbirSingleHijriDay() {
        val defaults = db.dhikrDao().getDefaults()
        val extraScheduled = defaults.filter {
            it.scheduleType != ScheduleType.ALWAYS && it.category != DhikrCategory.EID
        }
        if (extraScheduled.isNotEmpty()) {
            val ids = extraScheduled.map { it.id }
            db.reciterAudioDao().deleteByDhikrIds(ids)
            db.dhikrDao().deleteByIds(ids)
        }
        val templates = scheduledDhikr()
        defaults.filter { it.category == DhikrCategory.EID }.forEach { existing ->
            val template = templates.find { it.sortOrder == existing.sortOrder } ?: return@forEach
            db.dhikrDao().update(
                existing.copy(
                    scheduleType = template.scheduleType,
                    hijriMonth = template.hijriMonth,
                    hijriDayStart = template.hijriDayStart,
                    hijriDayEnd = template.hijriDayEnd,
                    scheduleLabelAr = template.scheduleLabelAr
                )
            )
        }
    }

    private suspend fun applyUserToggleableAzkarSections() {
        listOf(HomeAzkar.COLLECTION_ID, JawamiAzkarSeed.COLLECTION_ID, AzkarFavorites.COLLECTION_ID).forEach { id ->
            val existing = db.collectionDao().getById(id)
            val allowed = AutoAzkarCatalog.entityAutoPlayAllowed(id)
            if (existing != null) {
                db.collectionDao().update(
                    existing.copy(
                        autoPlayAllowed = allowed,
                        autoPlayEnabled = allowed && existing.autoPlayEnabled
                    )
                )
            } else if (id == AzkarFavorites.COLLECTION_ID) {
                AzkarFavorites.ensureCollection(db)
            }
        }
    }

    private suspend fun applyDefaultAzkarHoursIfUnchanged() {
        updateCollectionHourIfUnchanged(
            "morning", 6, 0, TasbihWindow.DEFAULT_MORNING_HOUR, TasbihWindow.DEFAULT_MORNING_MINUTE
        )
        updateCollectionHourIfUnchanged(
            "sleep", 22, 0, TasbihWindow.DEFAULT_SLEEP_HOUR, TasbihWindow.DEFAULT_SLEEP_MINUTE
        )
        applyDefaultWakeHourIfUnchanged()
    }

    private suspend fun applyDefaultWakeHourIfUnchanged() {
        updateCollectionHourIfUnchanged(
            "wake_up", 6, 0, TasbihWindow.DEFAULT_WAKE_HOUR, TasbihWindow.DEFAULT_WAKE_MINUTE
        )
    }

    private suspend fun updateCollectionHourIfUnchanged(
        id: String,
        oldHour: Int,
        oldMinute: Int,
        newHour: Int,
        newMinute: Int
    ) {
        val collection = db.collectionDao().getById(id) ?: return
        if (collection.scheduleHour == oldHour && collection.scheduleMinute == oldMinute) {
            db.collectionDao().update(
                collection.copy(scheduleHour = newHour, scheduleMinute = newMinute)
            )
        }
    }

    private suspend fun ensureBuiltinDhikrAndCollections() {
        val defaults = db.dhikrDao().getDefaults().toMutableList()
        if (defaults.isEmpty()) {
            (masba7aDhikr() + scheduledDhikr() + jawamiTasbihDhikr()).forEach { template ->
                val id = db.dhikrDao().insert(template)
                defaults += template.copy(id = id)
            }
        } else if (defaults.none { it.category == DhikrCategory.EID }) {
            scheduledDhikr().forEach { template ->
                val exists = defaults.any {
                    it.isDefault && it.sortOrder == template.sortOrder && it.category == template.category
                }
                if (!exists) {
                    val id = db.dhikrDao().insert(template)
                    defaults += template.copy(id = id)
                }
            }
        }
        if (db.collectionDao().getById("morning") == null) {
            IslambookAzkarSeed.seed(db)
        }
        JawamiAzkarSeed.seed(db) { jawamiTasbihDhikr() }
        FridayAzkar.ensure(db)
        BlessedDaysAzkar.ensure(db)
        seedBuiltinReciterAudio(db)
    }

    private suspend fun seedBuiltinReciterAudio(db: AdhkarDatabase) {
        val builtinReciterId = ReciterLibrariesMigration.ensureMixedVoicesReciter(db)
        db.dhikrDao().getDefaults().forEach { dhikr ->
            val asset = dhikr.audioPath?.takeIf {
                dhikr.audioSourceType == AudioSourceType.BUILTIN && it.isNotBlank()
            } ?: return@forEach
            val existing = db.reciterAudioDao().get(dhikr.id, builtinReciterId)
            if (existing == null) {
                db.reciterAudioDao().insert(
                    ReciterAudioEntity(
                        reciterId = builtinReciterId,
                        dhikrId = dhikr.id,
                        assetPath = asset,
                        isDownloaded = true
                    )
                )
            } else if (
                existing.assetPath.isNullOrBlank() &&
                existing.localPath.isNullOrBlank() &&
                existing.remoteUrl.isNullOrBlank()
            ) {
                db.reciterAudioDao().insert(
                    existing.copy(assetPath = asset, isDownloaded = true)
                )
            }
        }
    }

    private fun masba7aDhikr(): List<DhikrEntity> = listOf(
        dhikr(1, "سبحان الله", "Subhan Allah", "Gloire à Allah", "Gloria a Allah", "audio/sou_tasbeeh.mp3"),
        dhikr(2, "الحمدلله", "Alhamdulillah", "Louange à Allah", "Alabado sea Allah", "audio/sou_tahmeed.mp3"),
        dhikr(3, "لا إله إلا الله", "La ilaha illallah", "Il n'y a de dieu qu'Allah", "No hay dios sino Allah", "audio/sou_tahleel.mp3"),
        dhikr(4, "الله أكبر", "Allahu Akbar", "Allah est le plus grand", "Allah es el más grande", "audio/sou_takbeer.mp3"),
        dhikr(5, "لا حول ولا قوة الا بالله", "La hawla wa la quwwata illa billah", "Pas de force ni de puissance sauf par Allah", "No hay poder ni fuerza sino en Allah", "audio/sou_hawqalah.mp3"),
        dhikr(6, "سبحان الله ، والحمد لله ، ولا إله إلا الله ، والله أكبر", "Subhan Allah, Alhamdulillah, La ilaha illallah, Allahu Akbar", "Gloire, louange, unicité et grandeur d'Allah", "Gloria, alabanza, unicidad y grandeza de Allah", "audio/sou_baqyat.mp3"),
        dhikr(7, "أستغفر الله وأتوب اليه", "Astaghfirullah wa atubu ilayh", "Je demande pardon à Allah et je me repens", "Pido perdón a Allah y me arrepiento", "audio/sou_esteghfar.mp3"),
        dhikr(8, "اللهم صل وسلم وبارك على نبينا محمد", "O Allah, bless and grant peace to our Prophet Muhammad", "Ô Allah, bénis notre Prophète Muhammad", "Oh Allah, bendice a nuestro Profeta Muhammad", "audio/sou_salah.mp3"),
        dhikr(9, "سبحان الله وبحمده", "Subhan Allah wa bihamdih", "Gloire à Allah et louange à Lui", "Gloria a Allah y alabanza a Él", "audio/sou_tasbhamd.mp3"),
        dhikr(10, "سبحان الله العظيم", "Subhan Allah Al-Azim", "Gloire à Allah le Très Grand", "Gloria a Allah el Grandioso", "audio/sou_tasbta3zeem.mp3"),
        dhikr(11, "لَا إلَه إلّا اللهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلُّ شَيْءٍ قَدِيرٍ", "La ilaha illallah wahdahu la sharika lah...", "Il n'y a de dieu qu'Allah, Seul, sans associé...", "No hay dios sino Allah, Único, sin asociado...", "audio/sou_tawheed.mp3", longForm = true, enabled = false),
        dhikr(12, "سبحان الله وبحمده عدد خلقه ورضا نفسه وزنة عرشه ومداد كلماته", "Subhan Allah wa bihamdih adada khalqih...", "Gloire et louange à Allah autant que Sa création...", "Gloria y alabanza a Allah según Su creación...", "audio/sou_adadd.mp3", longForm = true, enabled = false),
        dhikr(13, "لا إله إلا أنت سبحانك إني كنت من ظالمين", "La ilaha illa anta subhanaka inni kuntu minaz-zalimin", "Il n'y a de dieu que Toi, gloire à Toi, j'étais du nombre des injustes", "No hay dios sino Tú, gloria a Ti, yo era de los injustos", "audio/sou_ghamm.mp3", longForm = true, enabled = false),
        dhikr(14, "يا ذا الجلال والإكرام", "Ya Dhal-Jalali wal-Ikram", "Ô Détenteur de la majesté et de la générosité", "Oh Poseedor de la majestad y la generosidad", "audio/sou_galal.mp3"),
        dhikr(15, "حسبي الله ونعم الوكيل", "Hasbiyallahu wa ni'mal wakeel", "Allah me suffit, Il est le meilleur garant", "Allah me basta, Él es el mejor dispositor", "audio/sou_hasbalah.mp3"),
        dhikr(16, "جميع الأذكار (متتابعة)", "All adhkar combined", "Tous les adhkar combinés", "Todos los adhkar combinados", "audio/sou_all.mp3", enabled = false, longForm = true)
    )

    private fun jawamiTasbihDhikr(): List<DhikrEntity> = listOf(
        jawami(
            200,
            "سُبْحَانَ اللهِ وَبِحَمْدِهِ عَدَدَ خَلْقِهِ، وَرِضَا نَفْسِهِ وَزِنَةَ عَرْشِهِ، وَمِدَادَ كَلِمَاتِهِ.",
            "Subhan Allah wa bihamdih, adada khalqih, wa rida nafsih, wa zinata arshih, wa midada kalimatih.",
            "Gloire et louange à Allah, autant que Sa création, Son agrément, le poids de Son Trône et l'encre de Ses paroles.",
            "Gloria y alabanza a Allah, según Su creación, Su complacencia, el peso de Su Trono y la tinta de Sus palabras.",
            repeat = 3,
            audio = "audio/sou_adadd.mp3"
        ),
        jawami(
            201,
            "سُبْحَانَ اللهِ عَدَدَ خَلْقِهِ، وَسُبْحَانَ اللهِ رِضَا نَفْسِهِ وَسُبْحَانَ اللهِ زِنَةَ عَرْشِهِ، وَسُبْحَانَ اللهِ مِدَادَ كَلِمَاتِهِ.",
            "Subhan Allah adada khalqih, wa Subhan Allah rida nafsih, wa Subhan Allah zinata arshih, wa Subhan Allah midada kalimatih.",
            repeat = 3
        )
    )

    private fun jawami(
        order: Int,
        ar: String,
        en: String,
        fr: String = "",
        es: String = "",
        repeat: Int = 1,
        audio: String? = null
    ) = DhikrEntity(
        textAr = ar,
        textEn = en,
        textFr = fr.ifBlank { en },
        textEs = es.ifBlank { en },
        category = DhikrCategory.JAWAMI,
        repeatCount = repeat,
        isDefault = true,
        isEnabled = false,
        isLongForm = true,
        audioSourceType = if (audio != null) AudioSourceType.BUILTIN else AudioSourceType.NONE,
        audioPath = audio,
        isDownloaded = audio != null,
        sortOrder = order
    ).withStandardAutoDisplay()

    private fun scheduledDhikr(): List<DhikrEntity> = listOf(
        DhikrEntity(
            textAr = "الله أكبر الله أكبر لا إله إلا الله والله أكبر الله أكبر ولله الحمد",
            textEn = "Allahu Akbar, Allahu Akbar, La ilaha illallah, Allahu Akbar, Allahu Akbar, wa lillahil hamd",
            textFr = "Allahu Akbar... Louange à Allah",
            textEs = "Allahu Akbar... Alabanza a Allah",
            category = DhikrCategory.EID,
            isDefault = true,
            audioSourceType = AudioSourceType.BUILTIN,
            audioPath = "audio/sou_takbeer.mp3",
            isDownloaded = true,
            sortOrder = 100,
            scheduleType = ScheduleType.HIJRI_RANGE,
            hijriMonth = 10,
            hijriDayStart = 1,
            hijriDayEnd = 1,
            scheduleLabelAr = "تكبيرات عيد الفطر — ١ شوال"
        ).withStandardAutoDisplay(),
        DhikrEntity(
            textAr = "الله أكبر الله أكبر لا إله إلا الله والله أكبر الله أكبر ولله الحمد",
            textEn = "Allahu Akbar (Eid Al-Adha takbirat)",
            category = DhikrCategory.EID,
            isDefault = true,
            audioSourceType = AudioSourceType.BUILTIN,
            audioPath = "audio/sou_takbeer.mp3",
            isDownloaded = true,
            sortOrder = 101,
            scheduleType = ScheduleType.HIJRI_RANGE,
            hijriMonth = 12,
            hijriDayStart = 10,
            hijriDayEnd = 10,
            scheduleLabelAr = "تكبيرات عيد الأضحى — ١٠ ذو الحجة"
        ).withStandardAutoDisplay()
    )

    private fun dhikr(
        order: Int,
        ar: String,
        en: String,
        fr: String,
        es: String,
        asset: String,
        longForm: Boolean = false,
        enabled: Boolean = true
    ) = DhikrEntity(
        textAr = ar,
        textEn = en,
        textFr = fr,
        textEs = es,
        category = DhikrCategory.GENERAL,
        isDefault = true,
        isEnabled = enabled,
        isLongForm = longForm,
        audioSourceType = AudioSourceType.BUILTIN,
        audioPath = asset,
        isDownloaded = true,
        sortOrder = order
    ).withStandardAutoDisplay()
}
