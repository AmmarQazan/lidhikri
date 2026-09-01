package com.greendome.adhkar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.greendome.adhkar.data.ContentI18n
import com.greendome.adhkar.data.model.CollectionDayMode
import com.greendome.adhkar.prayer.PrayerName
import java.time.Instant
import java.time.ZoneId
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

@Entity(tableName = "adhkar_collection")
data class AdhkarCollectionEntity(
    @PrimaryKey val id: String,
    val titleAr: String,
    val titleEn: String = "",
    val titleFr: String = "",
    val titleEs: String = "",
    val titleTr: String = "",
    val titleUr: String = "",
    val titleId: String = "",
    val titleHi: String = "",
    val sortOrder: Int = 0,
    /** يحدده المدير / البذرة: هل يُسمح لهذا القسم بالتشغيل التلقائي أصلاً */
    val autoPlayAllowed: Boolean = false,
    /** تفعيل المستخدم؛ القيمة الابتدائية من البذرة (الوضع الافتراضي عند أول فتح) */
    val autoPlayEnabled: Boolean = false,
    val scheduleHour: Int = 7,
    val scheduleMinute: Int = 0,
    /** بت لكل يوم: الأحد=1، الاثنين=2 … السبت=64 — الكل=127 */
    val weekDaysMask: Int = 127,
    val dayMode: CollectionDayMode = CollectionDayMode.WEEKDAYS,
    val hijriMonth: Int = -1,
    val hijriDayStart: Int = -1,
    val hijriDayEnd: Int = -1,
    val useTtsAutoPlay: Boolean = true
) {
    fun localizedTitle(lang: String): String {
        val direct = when (lang) {
            "ar" -> titleAr
            "fr" -> titleFr
            "es" -> titleEs
            "tr" -> titleTr
            "ur" -> titleUr
            "id" -> titleId
            "hi" -> titleHi
            else -> titleEn
        }
        if (direct.isNotBlank()) return direct
        ContentI18n.collectionTitle(id, lang)?.let { return it }
        return ContentI18n.withoutEnglishFallback(lang, titleAr, titleEn)
    }
}

@Entity(tableName = "azkar_item")
data class AzkarItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val collectionId: String,
    val textAr: String,
    val virtueAr: String = "",
    val repeatCount: Int = 1,
    val sortOrder: Int = 0,
    /** معرّف الذكر الأصلي عند نسخه إلى قائمة المفضلة */
    val sourceItemId: Long? = null,
    /** -1 = وقت القسم. 0–23 = ساعة هذا الذكر */
    val scheduleHour: Int = -1,
    val scheduleMinute: Int = 0,
    /** اسم صلاة من PrayerName أو فارغ للساعة فقط */
    val prayerAnchor: String = "",
    val prayerOffsetMinutes: Int = 0,
    val skipQuietWindow: Boolean = false,
    val hijriMonth: Int = -1,
    val hijriDayStart: Int = -1,
    val hijriDayEnd: Int = -1,
) {
    fun inheritsCollectionTime(): Boolean = scheduleHour < 0 && prayerAnchor.isBlank()

    fun hasOwnHijri(): Boolean = hijriMonth >= 1

    fun matchesHijri(millis: Long): Boolean {
        if (hijriMonth < 1) return false
        val hijri = HijrahDate.from(
            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        )
        if (hijri.get(ChronoField.MONTH_OF_YEAR) != hijriMonth) return false
        val day = hijri.get(ChronoField.DAY_OF_MONTH)
        val start = if (hijriDayStart > 0) hijriDayStart else 1
        val end = if (hijriDayEnd > 0) hijriDayEnd else start
        return day in start..end
    }

    fun prayerNameOrNull(): PrayerName? =
        PrayerName.entries.find { it.name == prayerAnchor }

    fun localizedVirtue(lang: String): String {
        if (virtueAr.isBlank() || lang == "ar") return virtueAr
        return ContentI18n.virtue(virtueAr, lang) ?: virtueAr
    }

    /** النص الظاهر للمستخدم بلغة التطبيق — بلا سقوط إلى الإنجليزية. */
    fun localizedText(lang: String): String {
        if (lang == "ar") return textAr
        ContentI18n.azkarMeaning(textAr, lang)?.takeIf { it.isNotBlank() }?.let { return it }
        ContentI18n.dhikr(textAr, lang)?.let { return it }
        return textAr
    }

    fun localizedMeaning(lang: String): String {
        if (lang == "ar") return ""
        return ContentI18n.azkarMeaning(textAr, lang).orEmpty()
    }
}
