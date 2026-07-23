package com.greendome.adhkar.data

import android.content.Context
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.DhikrEntity
import com.greendome.adhkar.data.local.ReciterAudioEntity
import com.greendome.adhkar.data.local.ReciterEntity
import com.greendome.adhkar.data.model.AppThemeMode
import com.greendome.adhkar.data.model.ArabicFontStyle
import com.greendome.adhkar.data.model.AzkarDisplayMode
import com.greendome.adhkar.data.model.DhikrOfDayDisplayMode
import com.greendome.adhkar.data.model.MisbahaFeedbackMode
import com.greendome.adhkar.data.model.MisbahaStyle
import com.greendome.adhkar.data.model.NumberDigitStyle
import com.greendome.adhkar.data.model.AudioSourceType
import com.greendome.adhkar.data.model.DhikrCategory
import com.greendome.adhkar.data.model.ScheduleType
import com.greendome.adhkar.util.DhikrScheduleMatcher
import com.greendome.adhkar.data.model.PopupAppearance
import com.greendome.adhkar.data.model.PopupSettingsTarget
import com.greendome.adhkar.data.model.ReminderDisplayStyle
import com.greendome.adhkar.data.model.TtsVoiceGender
import com.greendome.adhkar.data.model.VolumeMode
import kotlinx.coroutines.flow.Flow

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("adhkar_settings", Context.MODE_PRIVATE)

    companion object {
        const val DEFAULT_ADMIN_PIN = "23704660"
    }

    var intervalMinutes: Int
        get() = prefs.getInt("interval_minutes", 15)
        set(v) = prefs.edit().putInt("interval_minutes", v).apply()

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
        get() = prefs.getLong("selected_reciter", 1L)
        set(v) = prefs.edit().putLong("selected_reciter", v).apply()

    var appLanguage: String
        get() = prefs.getString("app_language", "ar") ?: "ar"
        set(v) = prefs.edit().putString("app_language", v).apply()

    var sequentialIndex: Int
        get() = prefs.getInt("sequential_index", 0)
        set(v) = prefs.edit().putInt("sequential_index", v).apply()

    var isAdminLoggedIn: Boolean
        get() = prefs.getBoolean("admin_logged_in", false)
        set(v) = prefs.edit().putBoolean("admin_logged_in", v).apply()

    var seedVersion: Int
        get() = prefs.getInt("seed_version", 0)
        set(v) = prefs.edit().putInt("seed_version", v).apply()

    var reminderDisplayStyle: ReminderDisplayStyle
        get() {
            val raw = prefs.getString("reminder_display_style", ReminderDisplayStyle.POPUP_ONLY.name)
                ?: ReminderDisplayStyle.POPUP_ONLY.name
            return runCatching { ReminderDisplayStyle.valueOf(raw) }
                .getOrDefault(ReminderDisplayStyle.POPUP_ONLY)
        }
        set(v) = prefs.edit().putString("reminder_display_style", v.name).apply()

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
        get() = prefs.getBoolean("auto_azkar_enabled", true)
        set(v) = prefs.edit().putBoolean("auto_azkar_enabled", v).apply()

    var autoAzkarRandomMode: Boolean
        get() = prefs.getBoolean("auto_azkar_random", true)
        set(v) = prefs.edit().putBoolean("auto_azkar_random", v).apply()

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
        get() = prefs.getInt("onboarding_step", 0).coerceIn(0, 5)
        set(v) = prefs.edit().putInt("onboarding_step", v.coerceIn(0, 5)).apply()

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
    suspend fun deleteCustom(id: Long) = dao.deleteCustom(id)
    suspend fun getEnabledList() = dao.getEnabledList()

    suspend fun getActiveNowList(): List<DhikrEntity> =
        dao.getEnabledList().filter { DhikrScheduleMatcher.isActiveNow(it) }
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
        return db.reciterAudioDao().insert(toSave)
    }

    suspend fun deleteReciterAudio(id: Long, localPath: String?) {
        localPath?.let { path ->
            try {
                java.io.File(path).takeIf { it.exists() }?.delete()
            } catch (_: Exception) { }
        }
        db.reciterAudioDao().delete(id)
    }
}

class SeedData(private val db: AdhkarDatabase, private val settings: SettingsRepository) {
    suspend fun seedIfEmpty() {
        if (settings.seedVersion < 3) {
            db.dhikrDao().deleteDefaults()

            db.reciterDao().insert(
                ReciterEntity(
                    id = 1,
                    nameAr = "المسبحة الصوتية - masba7a",
                    nameEn = "Masba7a Audio Tasbih",
                    nameFr = "Tasbih audio Masba7a",
                    nameEs = "Tasbih audio Masba7a"
                )
            )

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
    }

    private suspend fun seedBuiltinReciterAudio(db: AdhkarDatabase) {
        val builtinReciterId = 1L
        if (db.reciterDao().getById(builtinReciterId) == null) {
            db.reciterDao().insert(
                ReciterEntity(
                    id = builtinReciterId,
                    nameAr = "المسبحة الصوتية - masba7a",
                    nameEn = "Masba7a Audio Tasbih",
                    nameFr = "Tasbih audio Masba7a",
                    nameEs = "Tasbih audio Masba7a",
                    isBuiltin = true
                )
            )
        }
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
        dhikr(11, "لَا إلَه إلّا اللهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلُّ شَيْءٍ قَدِيرٍ", "La ilaha illallah wahdahu la sharika lah...", "Il n'y a de dieu qu'Allah, Seul, sans associé...", "No hay dios sino Allah, Único, sin asociado...", "audio/sou_tawheed.mp3", longForm = true),
        dhikr(12, "سبحان الله وبحمده عدد خلقه ورضا نفسه وزنة عرشه ومداد كلماته", "Subhan Allah wa bihamdih adada khalqih...", "Gloire et louange à Allah autant que Sa création...", "Gloria y alabanza a Allah según Su creación...", "audio/sou_adadd.mp3", longForm = true),
        dhikr(13, "لا إله إلا أنت سبحانك إني كنت من ظالمين", "La ilaha illa anta subhanaka inni kuntu minaz-zalimin", "Il n'y a de dieu que Toi, gloire à Toi, j'étais du nombre des injustes", "No hay dios sino Tú, gloria a Ti, yo era de los injustos", "audio/sou_ghamm.mp3", longForm = true),
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
        ),
        jawami(
            202,
            "الحَمدُلِلَّه عَدَدَ خَلْقِهِ، وَالحَمدُلِلَّه رِضَا نَفْسِهِ وَالحَمدُلِلَّه زِنَةَ عَرْشِهِ، وَالحَمدُلِلَّه مِدَادَ كَلِمَاتِهِ.",
            "Alhamdulillah adada khalqih, wal-hamdu lillah rida nafsih, wal-hamdu lillah zinata arshih, wal-hamdu lillah midada kalimatih.",
            repeat = 3
        ),
        jawami(
            203,
            "سبحانَ اللهِ عددَ خَلْقِهِ ، سبحانَ اللهِ عدَدَ خلْقِهِ ، سبحانَ اللهِ عدَدَ خَلْقِهِ.",
            "Subhan Allah adada khalqih (three times)."
        ),
        jawami(
            204,
            "سَبْحانَ اللهِ رِضَى نَفْسِهِ ، سبحانَ اللهِ رِضَى نَفْسِهِ ، سبحانَ اللهِ رِضَى نَفْسِهِ.",
            "Subhan Allah rida nafsih (three times)."
        ),
        jawami(
            205,
            "سَبحانَ اللهِ زِنَةَ عَرْشِهِ ، سبحانَ اللهِ زِنَةَ عَرْشِهِ ، سبحانَ اللهِ زِنَةَ عَرْشِهِ.",
            "Subhan Allah zinata arshih (three times)."
        ),
        jawami(
            206,
            "سبحانَ اللهِ مِدادَ كَلِماتِهِ ، سبحانَ اللهِ مِدادَ كَلِماتِهِ ، سبحانَ اللهِ مِدادَ كَلِماتِهِ.",
            "Subhan Allah midada kalimatih (three times)."
        ),
        jawami(
            207,
            "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، وَلَا إِلَهَ إِلَّا اللَّهُ، عَدَدَ خَلْقِهِ، وَرِضَا نَفْسِهِ، وَزِنَةَ عَرْشِهِ، وَمِدَادَ كَلِمَاتِهِ.",
            "Subhan Allah wa bihamdih, wa la ilaha illallah, adada khalqih, wa rida nafsih, wa zinata arshih, wa midada kalimatih.",
            repeat = 3
        ),
        jawami(
            208,
            "سُبْحَانَ اللَّهِ عَدَدَ مَا خَلَقَ فِي السَّمَاءِ، وَسُبْحَانَ اللَّهِ عَدَدَ مَا خَلَقَ فِي الْأَرْضِ، وَسُبْحَانَ اللهِ عَدَدَ مَا خَلَقَ بَيْنَ ذَلِكَ، وَسُبْحَانَ اللَّهِ عَدَدَ مَا هُوَ خَالِقٌ.",
            "Subhan Allah according to all He created in the heavens, earth, between them, and all He creates."
        ),
        jawami(
            209,
            "اللهُ أَكْبَرُ عَدَدَ مَا خَلَقَ فِي السَّمَاءِ، وَاللَّهُ أَكْبَرُ عَدَدَ مَا خَلَقَ فِي الْأَرْضِ، وَاللهُ أَكْبَرُ عَدَدَ مَا خَلَقَ بَيْنَ ذَلِكَ، وَاللَّهُ أَكْبَرُ عَدَدَ مَا هُوَ خَالِقٌ.",
            "Allahu Akbar according to all He created in the heavens, earth, between them, and all He creates."
        ),
        jawami(
            210,
            "الْحَمْدُ لِلَّهِ عَدَدَ مَا خَلَقَ فِي السَّمَاءِ، وَالْحَمْدُ لِلَّهِ عَدَدَ مَا خَلَقَ فِي الْأَرْضِ، وَالْحَمْدُ لِلَّهِ عَدَدَ مَا خَلَقَ بَيْنَ ذَلِكَ، وَالْحَمْدُ لِلَّهِ عَدَدَ مَا هُوَ خَالِقٌ.",
            "Alhamdulillah according to all He created in the heavens, earth, between them, and all He creates."
        ),
        jawami(
            211,
            "لَا إِلَهَ إِلَّا اللَّهُ عَدَدَ مَا خَلَقَ فِي السَّمَاءِ، وَلَا إِلَهَ إِلَّا اللهُ عَدَدَ مَا خَلَقَ فِي الْأَرْضِ وَلَا إِلَهَ إِلَّا اللَّهُ عَدَدَ مَا خَلَقَ بَيْنَ ذَلِكَ، وَلَا إِلَهَ إِلَّا اللَّهُ عَدَدَ مَا هُوَ خَالِقٌ.",
            "La ilaha illallah according to all He created in the heavens, earth, between them, and all He creates."
        ),
        jawami(
            212,
            "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ عَدَدَ مَا خَلَقَ في السَّمَاءِ، وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ عَدَدَ مَا خَلَقَ فِي الْأَرْضِ، وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ عَدَدَ مَا خَلَقَ بَيْنَ ذَلِكَ، وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللهِ عَدَدَ مَا هُوَ خَالِقٌ.",
            "La hawla wa la quwwata illa billah according to all He created in the heavens, earth, between them, and all He creates."
        ),
        jawami(
            213,
            "الْحَمْدُ لِلَّهِ عَدَدَ مَا خَلَقَ، وَالْحَمْدُ لِلَّهِ مِلْءَ مَا خَلَقَ ، وَالْحَمْدُ لِلَّهِ عَدَدَ مَا فِي السَّمَاوَاتِ وَالْأَرْضِ، وَالْحَمْدُ لِلَّهِ مِلْءَ مَا فِي السَّمَاوَاتِ وَالْأَرْضِ ، وَالْحَمْدُ لِلَّهِ عَدَدَ مَا أَحْصَى كِتَابُهُ، وَالْحَمْدُ لِلَّهِ مِلْءَ مَا أَحْصَى كِتَابُهُ ، وَالْحَمْدُ لِلَّهِ عَدَدَ كُلَّ شَيْءٍ، وَالْحَمْدُ لِلَّهِ مِلْءَ كُلِّ شَيْءٍ.",
            "Alhamdulillah by number and fullness of creation, heavens and earth, His Book, and all things."
        ),
        jawami(
            214,
            "سُبْحَانَ اللهِ عَدَدَ مَا خَلَقَ، وَسُبْحَانَ اللهِ مِلْءَ مَا خَلَقَ ، وَسُبْحَانَ اللهِ عَدَدَ مَا فِي السَّمَاوَاتِ وَالأَرْضِ، وَسُبْحَانَ اللَّهِ مِلْءَ مَا فِي السَّمَاوَاتِ وَالْأَرْضِ ، وَسُبْحَانَ اللَّهِ عَدَدَ مَا أَحْصَى كِتَابُهُ، وَسُبْحَانَ اللَّهِ مِلْءَ مَا أَحْصَى كِتَابُهُ ، وَسُبْحَانَ اللَّهِ عَدَدَ كُلِّ شَيْءٍ، وَسُبْحَانَ اللهِ مِلْءَ كُلِّ شَيْءٍ.",
            "Subhan Allah by number and fullness of creation, heavens and earth, His Book, and all things."
        ),
        jawami(
            215,
            "الْحَمْدُ لِلَّهِ عَدَدَ مَا أَحْصَى كِتَابُهُ، وَالْحَمْدُ لِلَّهِ عَدَدَ مَا فِي كِتَابِهِ، وَالْحَمْدُ لِلَّهِ عَدَدَ مَا أَحْصَى خَلْقُهُ، وَالْحَمْدُ لِلَّهِ عَلَى مَا فِي خَلْقِهِ، وَالْحَمْدُ لِلَّهِ مِلْءَ سَمَاوَاتِهِ وَأَرْضِهِ، وَالْحَمْدُ لِلَّهِ عَدَدَ كُلِّ شَيْءٍ، وَالْحَمْدُ لِلَّهِ مِلْءَ كُلِّ شَيْءٍ.",
            "Alhamdulillah by what His Book counted, what is in His Book, what His creation counted, and the fullness of all things."
        ),
        jawami(
            216,
            "سُبْحَانَ اللَّهِ عَدَدَ مَا أَحْصَى كِتَابُهُ، وَسُبْحَانَ اللَّهِ عَدَدَ مَا فِي كِتَابِهِ، وَسُبْحَانَ اللهِ عَدَدَ مَا أَحْصَى خَلْقُهُ، وَسُبْحَانَ اللَّهِ عَلَى مَا فِي خَلْقِهِ، وَسُبْحَانَ اللَّهِ مِلْءَ سَمَاوَاتِهِ وَأَرْضِهِ، وَسُبْحَانَ اللَّهِ عَدَدَ كُلِّ شَيْءٍ، وَسُبْحَانَ اللهِ مِلْءَ كُلِّ شَيْءٍ.",
            "Subhan Allah by what His Book counted, what is in His Book, what His creation counted, and the fullness of all things."
        ),
        jawami(
            217,
            "اللهُ أَكْبَرُ عَدَدَ مَا أَحْصَى كِتَابُهُ، وَاللهُ أَكْبَرُ عَدَدَ مَا فِي كِتَابِهِ، وَاللَّهُ أَكْبَرُ عَدَدَ مَا أَحْصَى خَلْقُهُ، وَاللَّهُ أَكْبَرُ عَلَى مَا فِي خَلْقِهِ، وَاللهُ أَكْبَرُ مِلْءَ سَمَاوَاتِهِ وَأَرْضِهِ، وَاللهُ أَكْبَرُ عَدَدَ كُلِّ شَيْءٍ، وَاللَّهُ أَكْبَرُ مِلْءَ كُلِّ شَيْءٍ.",
            "Allahu Akbar by what His Book counted, what is in His Book, what His creation counted, and the fullness of all things."
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
            hijriDayEnd = 4,
            scheduleLabelAr = "تكبيرات عيد الفطر"
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
            hijriDayEnd = 13,
            scheduleLabelAr = "تكبيرات عيد الأضحى"
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
