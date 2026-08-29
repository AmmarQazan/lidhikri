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

/** كيف يظهر التذكير التلقائي على جهاز المستخدم */
enum class AutoReminderPresentation {
    POPUP_AND_AUDIO,
    POPUP_ONLY,
    NOTIFICATION,
    AUDIO_ONLY;

    fun toDisplayModes(): DisplayModes = when (this) {
        POPUP_AND_AUDIO -> DisplayModes(
            popup = true,
            notification = false,
            audioOnly = false,
            audioWithText = true
        )
        POPUP_ONLY -> DisplayModes(
            popup = true,
            notification = false,
            audioOnly = false,
            audioWithText = false
        )
        NOTIFICATION -> DisplayModes(
            popup = false,
            notification = true,
            audioOnly = false,
            audioWithText = false
        )
        AUDIO_ONLY -> DisplayModes(
            popup = false,
            notification = false,
            audioOnly = true,
            audioWithText = false
        )
    }
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

/** لون نص ويدجت ذكر اليوم */
enum class DhikrOfDayTextColor {
    AUTO,
    BLACK,
    WHITE,
    GOLD,
    GREEN,
    CREAM,
    BROWN,
    NAVY,
    TEAL,
    MAROON,
    GRAY,
    AMBER
}

/** حدود حجم خط ويدجت ذكر اليوم */
object DhikrOfDayWidgetText {
    const val MIN_FONT_SP = 10
    const val MAX_FONT_SP = 28
    const val DEFAULT_FONT_SP = 14
}

/** طريقة عرض أذكار القسم: قائمة أو بطاقة واحدة */
enum class AzkarDisplayMode {
    LIST, CARD
}

/** حجم نص الأذكار في وضع القائمة — الافتراضي يطابق تسبيحة المسبحة المختارة */
object AzkarListText {
    const val MIN_FONT_SP = 13
    const val MAX_FONT_SP = 28
    /** titleMedium في شاشة المسبحة (نص التسبيحة المختارة) */
    const val DEFAULT_FONT_SP = 17
    const val STEP_SP = 1
    const val LINE_HEIGHT_RATIO = 28f / 17f
}

/** حجم نص الذكر في وضع البطاقة */
object AzkarCardText {
    const val MIN_FONT_SP = 14
    const val MAX_FONT_SP = 36
    const val DEFAULT_FONT_SP = 21
    const val STEP_SP = 1
    const val LINE_HEIGHT_RATIO = 34f / 21f
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

/** خلفية ويدجت الشاشة الرئيسية (المسبحة وذكر اليوم) */
enum class MisbahaWidgetBackground {
    WHITE,
    CREAM,
    GREEN,
    DARK,
    TRANSPARENT
}

/** مظهر وألوان خرز المسبحة */
enum class MisbahaBeadTheme {
    CLASSIC,
    ROYAL,
    DESERT,
    EMERALD,
    OCEAN,
    AMBER,
    WOOD,
    SILVER,
    RUBY
}

/** شكل عرض الأرقام في التطبيق */
enum class NumberDigitStyle {
    ARABIC_INDIC,
    LATIN
}

/** عرض ساعة الذكر القادم: 12 أو 24 */
enum class ClockHourFormat {
    HOUR_24,
    HOUR_12
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
    EID_FITR("تكبيرات عيد الفطر (١ شوال)"),
    EID_ADHA("تكبيرات عيد الأضحى (١٠ ذو الحجة)"),
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
        hijriDayEnd = 1,
        labelAr = "تكبيرات عيد الفطر — ١ شوال"
    )
    SchedulePreset.EID_ADHA -> SchedulePresetValues(
        scheduleType = ScheduleType.HIJRI_RANGE,
        hijriMonth = 12,
        hijriDayStart = 10,
        hijriDayEnd = 10,
        labelAr = "تكبيرات عيد الأضحى — ١٠ ذو الحجة"
    )
    SchedulePreset.CUSTOM -> SchedulePresetValues(scheduleType = ScheduleType.ALWAYS)
}
