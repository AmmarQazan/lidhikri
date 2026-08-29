package com.greendome.adhkar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.greendome.adhkar.data.ContentI18n
import com.greendome.adhkar.data.model.AudioSourceType
import com.greendome.adhkar.data.model.DhikrCategory
import com.greendome.adhkar.data.model.DisplayModes
import com.greendome.adhkar.data.model.ReciterVoiceScope
import com.greendome.adhkar.data.model.ScheduleType

/** ترتيب ذكر «جميع الأذكار (متتابعة)» في المسبحة — لا يُستخدم للتسبيح التلقائي */
const val ALL_ADHKAR_CHAIN_SORT_ORDER = 16

fun dhikrEnabledForAutoTasbihImport(
    category: DhikrCategory,
    isLongForm: Boolean,
    isDefault: Boolean,
    sortOrder: Int,
    remoteEnabled: Boolean,
): Boolean {
    if (!remoteEnabled) return false
    return DhikrEntity(
        category = category,
        isLongForm = isLongForm,
        isDefault = isDefault,
        sortOrder = sortOrder,
        textAr = "",
    ).isEligibleForAutoTasbih()
}

@Entity(tableName = "dhikr")
data class DhikrEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val textAr: String,
    val textEn: String = "",
    val textFr: String = "",
    val textEs: String = "",
    val textTr: String = "",
    val textUr: String = "",
    val textId: String = "",
    val textHi: String = "",
    val category: DhikrCategory = DhikrCategory.GENERAL,
    val repeatCount: Int = 1,
    val isEnabled: Boolean = true,
    val isDefault: Boolean = false,
    val isLongForm: Boolean = false,
    val audioSourceType: AudioSourceType = AudioSourceType.NONE,
    val audioPath: String? = null,
    val reciterId: Long? = null,
    val remoteAudioUrl: String? = null,
    val isDownloaded: Boolean = false,
    val displayPopup: Boolean = true,
    val displayNotification: Boolean = false,
    val displayLockScreen: Boolean = true,
    val displayAudioOnly: Boolean = false,
    val displayAudioText: Boolean = true,
    val sortOrder: Int = 0,
    val scheduleType: ScheduleType = ScheduleType.ALWAYS,
    val timeStartHour: Int = -1,
    val timeStartMinute: Int = 0,
    val timeEndHour: Int = -1,
    val timeEndMinute: Int = 0,
    val hijriMonth: Int = -1,
    val hijriDayStart: Int = -1,
    val hijriDayEnd: Int = -1,
    val scheduleLabelAr: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    /** الافتراضي للتسبيح/الذكر التلقائي: نافذة منبثقة + شاشة قفل + صوت مع النص */
    fun withStandardAutoDisplay(): DhikrEntity = copy(
        displayPopup = true,
        displayNotification = false,
        displayLockScreen = true,
        displayAudioOnly = false,
        displayAudioText = true
    )

    fun displayModes() = DisplayModes(
        popup = displayPopup,
        notification = displayNotification,
        lockScreen = displayLockScreen,
        audioOnly = displayAudioOnly,
        audioWithText = displayAudioText
    )

    /** أذكار/تسبيحات طويلة أو جوامع — لا تُشغَّل صوتها تلقائياً ولا كبديل لصوت الأذكار */
    fun isExcludedFromAutoAudio(): Boolean =
        isLongForm ||
            sortOrder == ALL_ADHKAR_CHAIN_SORT_ORDER ||
            category == DhikrCategory.JAWAMI

    /** تسبيحات قصيرة مدمجة — لا جوامع ولا تكبيرات العيد ولا أذكار مجدولة */
    fun isBuiltinShortTasbih(): Boolean =
        isDefault && !isLongForm && category == DhikrCategory.GENERAL

    fun isEidTakbir(): Boolean = isDefault && category == DhikrCategory.EID

    /** جوامع التسبيح في شاشة الإدارة — فئة جوامع أو التسبيحات الطويلة عدا تكبير العيد */
    fun isJawamiSectionItem(): Boolean =
        isDefault && (category == DhikrCategory.JAWAMI || (isLongForm && !isEidTakbir()))

    fun isAdminCatalogDeletable(): Boolean =
        isBuiltinShortTasbih() || isJawamiSectionItem()

    /** تسبيحات المسبحة القصيرة المدمجة فقط — لا أذكار ولا جوامع ولا أذكار مخصصة */
    fun isEligibleForAutoTasbih(): Boolean =
        isDefault &&
            !isExcludedFromAutoAudio() &&
            when (category) {
                DhikrCategory.GENERAL,
                DhikrCategory.EID -> true
                else -> false
            }

    fun localizedText(lang: String): String {
        val direct = when (lang) {
            "ar" -> textAr
            "fr" -> textFr
            "es" -> textEs
            "tr" -> textTr
            "ur" -> textUr
            "id" -> textId
            "hi" -> textHi
            else -> textEn
        }
        if (direct.isNotBlank()) return direct
        ContentI18n.dhikr(textAr, lang)?.let { return it }
        return ContentI18n.withoutEnglishFallback(lang, textAr, textEn)
    }
}

@Entity(tableName = "reciter")
data class ReciterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nameAr: String,
    val nameEn: String = "",
    val nameFr: String = "",
    val nameEs: String = "",
    val nameTr: String = "",
    val nameUr: String = "",
    val nameId: String = "",
    val nameHi: String = "",
    val isBuiltin: Boolean = false,
    val isActive: Boolean = true,
    val voiceScope: ReciterVoiceScope = ReciterVoiceScope.BOTH,
) {
    fun localizedName(lang: String): String {
        val direct = when (lang) {
            "ar" -> nameAr
            "fr" -> nameFr
            "es" -> nameEs
            "tr" -> nameTr
            "ur" -> nameUr
            "id" -> nameId
            "hi" -> nameHi
            else -> nameEn
        }
        if (direct.isNotBlank()) return direct
        ContentI18n.reciter(nameAr, lang)?.let { return it }
        return ContentI18n.withoutEnglishFallback(lang, nameAr, nameEn)
    }
}

@Entity(tableName = "reciter_audio")
data class ReciterAudioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reciterId: Long,
    val dhikrId: Long,
    val localPath: String? = null,
    val remoteUrl: String? = null,
    /** مسار ملف مدمج داخل التطبيق، مثل audio/sou_tasbeeh.mp3 */
    val assetPath: String? = null,
    val isDownloaded: Boolean = false
)

@Entity(tableName = "reciter_azkar_audio")
data class ReciterAzkarAudioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reciterId: Long,
    val azkarItemId: Long,
    val localPath: String? = null,
    val remoteUrl: String? = null,
    val assetPath: String? = null,
    val isDownloaded: Boolean = false
)

@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    @PrimaryKey val dateKey: String,
    val playCount: Int = 0
)

/** تغيير من شاشة المدير لم يُنشر بعد للمستخدمين */
@Entity(tableName = "pending_publish_change")
data class PendingPublishChangeEntity(
    @PrimaryKey val changeKey: String,
    val entityType: String,
    val entityId: Long,
    val entityKey: String,
    val action: String,
    val createdAt: Long
)
