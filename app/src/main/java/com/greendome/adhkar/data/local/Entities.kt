package com.greendome.adhkar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.greendome.adhkar.data.model.AudioSourceType
import com.greendome.adhkar.data.model.DhikrCategory
import com.greendome.adhkar.data.model.DisplayModes
import com.greendome.adhkar.data.model.ScheduleType

@Entity(tableName = "dhikr")
data class DhikrEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val textAr: String,
    val textEn: String = "",
    val textFr: String = "",
    val textEs: String = "",
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

    fun localizedText(lang: String): String = when (lang) {
        "ar" -> textAr
        "fr" -> textFr.ifBlank { textAr }
        "es" -> textEs.ifBlank { textAr }
        else -> textEn.ifBlank { textAr }
    }
}

@Entity(tableName = "reciter")
data class ReciterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nameAr: String,
    val nameEn: String = "",
    val nameFr: String = "",
    val nameEs: String = "",
    val isBuiltin: Boolean = true,
    val isActive: Boolean = true
) {
    fun localizedName(lang: String): String = when (lang) {
        "ar" -> nameAr
        "fr" -> nameFr.ifBlank { nameAr }
        "es" -> nameEs.ifBlank { nameAr }
        else -> nameEn.ifBlank { nameAr }
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
