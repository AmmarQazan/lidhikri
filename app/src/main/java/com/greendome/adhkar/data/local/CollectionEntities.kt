package com.greendome.adhkar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.greendome.adhkar.data.ContentI18n

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
    val sourceItemId: Long? = null
) {
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
