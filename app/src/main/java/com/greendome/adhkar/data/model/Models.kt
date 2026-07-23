package com.greendome.adhkar.data.model

enum class DhikrCategory {
    MORNING, EVENING, GENERAL, SLEEP, CUSTOM, EID, SEASONAL, JAWAMI
}

enum class AudioSourceType {
    BUILTIN, RECORDED, FILE, DOWNLOAD, NONE
}

/** مصدر ضبط مستوى صوت التسبيح */
enum class VolumeMode {
    MANUAL,
    MEDIA,
    RING
}

/** طريقة عرض تذكير التسبيح */
enum class ReminderDisplayStyle {
    POPUP_ONLY,
    NOTIFICATION_ONLY,
    BOTH
}

enum class DisplayMode {
    POPUP, NOTIFICATION, LOCK_SCREEN, AUDIO_ONLY, AUDIO_TEXT
}

/** نوع جدولة الذكر: دائماً، وقت يومي، تاريخ هجري، أو الاثنان معاً */
enum class ScheduleType {
    ALWAYS,
    TIME_RANGE,
    HIJRI_RANGE,
    TIME_AND_HIJRI
}

/** جنس صوت القراءة الآلية (TTS) للأذكار */
enum class TtsVoiceGender {
    MALE, FEMALE
}

/** وضع مظهر التطبيق: نهاري، ليلي، أو حسب إعدادات الهاتف */
enum class AppThemeMode {
    LIGHT, DARK, SYSTEM
}

/** مكان عرض ذكر اليوم: ويدجت الشاشة الرئيسية أو شاشة القفل */
enum class DhikrOfDayDisplayMode {
    HOME_WIDGET, LOCK_SCREEN
}

/** طريقة عرض أذكار القسم: قائمة أو بطاقة واحدة */
enum class AzkarDisplayMode {
    LIST, CARD
}

/** ردّة فعل ضغط خرزة المسبحة */
enum class MisbahaFeedbackMode {
    SOUND_AND_VIBRATION,
    SOUND_ONLY,
    VIBRATION_ONLY,
    SILENT
}

/** شكل واجهة المسبحة */
enum class MisbahaStyle {
    TRADITIONAL,
    ELECTRONIC
}

/** شكل عرض الأرقام في التطبيق */
enum class NumberDigitStyle {
    ARABIC_INDIC,
    LATIN
}

/** نمط خط نصوص الأذكار والقرآن */
enum class ArabicFontStyle {
    DEFAULT,
    UTHMANI_1,
    UTHMANI_2,
    INDOPAK_1,
    INDOPAK_2,
    BENGALI
}

data class DisplayModes(
    val popup: Boolean = true,
    val notification: Boolean = false,
    val lockScreen: Boolean = false,
    val audioOnly: Boolean = false,
    val audioWithText: Boolean = true
) {
    fun primaryMode(): DisplayMode = when {
        audioOnly -> DisplayMode.AUDIO_ONLY
        audioWithText && popup -> DisplayMode.AUDIO_TEXT
        popup -> DisplayMode.POPUP
        notification -> DisplayMode.NOTIFICATION
        lockScreen -> DisplayMode.LOCK_SCREEN
        else -> DisplayMode.NOTIFICATION
    }

    fun showsText(): Boolean = !audioOnly && (popup || notification || lockScreen || audioWithText)

    fun playsAudio(): Boolean = !notification && (audioOnly || audioWithText)

    fun showsTextViaNotification(): Boolean = notification

    fun showsTextViaLockScreen(): Boolean = lockScreen && !notification && !audioOnly

    fun showsTextViaPopup(): Boolean = !notification && !audioOnly && popup
}

/** قوالب جدولة جاهزة للمدير */
enum class SchedulePreset(val labelAr: String) {
    EID_FITR("تكبيرات عيد الفطر (١–٤ شوال)"),
    EID_ADHA("تكبيرات عيد الأضحى (١٠–١٣ ذو الحجة)"),
    MORNING("أذكار الصباح (٥:٠٠ – ١٠:٠٠)"),
    EVENING("أذكار المساء (١٦:٠٠ – ٢٠:٠٠)"),
    RAMADAN("شهر رمضان كاملاً"),
    CUSTOM("مخصص")
}

data class SchedulePresetValues(
    val scheduleType: ScheduleType,
    val timeStartHour: Int = -1,
    val timeStartMinute: Int = 0,
    val timeEndHour: Int = -1,
    val timeEndMinute: Int = 0,
    val hijriMonth: Int = -1,
    val hijriDayStart: Int = -1,
    val hijriDayEnd: Int = -1,
    val labelAr: String = ""
)

fun SchedulePreset.toValues(): SchedulePresetValues = when (this) {
    SchedulePreset.EID_FITR -> SchedulePresetValues(
        scheduleType = ScheduleType.HIJRI_RANGE,
        hijriMonth = 10,
        hijriDayStart = 1,
        hijriDayEnd = 4,
        labelAr = "تكبيرات عيد الفطر"
    )
    SchedulePreset.EID_ADHA -> SchedulePresetValues(
        scheduleType = ScheduleType.HIJRI_RANGE,
        hijriMonth = 12,
        hijriDayStart = 10,
        hijriDayEnd = 13,
        labelAr = "تكبيرات عيد الأضحى"
    )
    SchedulePreset.MORNING -> SchedulePresetValues(
        scheduleType = ScheduleType.TIME_RANGE,
        timeStartHour = 5,
        timeStartMinute = 0,
        timeEndHour = 10,
        timeEndMinute = 0,
        labelAr = "أذكار الصباح"
    )
    SchedulePreset.EVENING -> SchedulePresetValues(
        scheduleType = ScheduleType.TIME_RANGE,
        timeStartHour = 16,
        timeStartMinute = 0,
        timeEndHour = 20,
        timeEndMinute = 0,
        labelAr = "أذكار المساء"
    )
    SchedulePreset.RAMADAN -> SchedulePresetValues(
        scheduleType = ScheduleType.HIJRI_RANGE,
        hijriMonth = 9,
        hijriDayStart = 1,
        hijriDayEnd = 30,
        labelAr = "شهر رمضان"
    )
    SchedulePreset.CUSTOM -> SchedulePresetValues(scheduleType = ScheduleType.ALWAYS)
}
