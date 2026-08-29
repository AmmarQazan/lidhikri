package com.greendome.adhkar.data

import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.prayer.PrayerRespectGate

/**
 * طبقتان لكل قسم:
 * - [Spec.allowed]: هل يُسمح أصلاً بالتشغيل التلقائي (المدير / البذرة)
 * - [Spec.defaultEnabled]: هل يُفعَّل للمستخدم من أول فتح
 */
object AutoAzkarCatalog {
    enum class Trigger { CLOCK, PRAYER }

    data class Spec(
        val id: String,
        val allowed: Boolean,
        val defaultEnabled: Boolean,
        val trigger: Trigger = Trigger.CLOCK,
    )

    val specs: List<Spec> = listOf(
        Spec("wake_up", allowed = true, defaultEnabled = true),
        Spec("morning", allowed = true, defaultEnabled = true),
        Spec(PrayerRespectGate.AFTER_PRAYER_COLLECTION_ID, allowed = true, defaultEnabled = false, trigger = Trigger.PRAYER),
        Spec("evening", allowed = true, defaultEnabled = true),
        Spec("sleep", allowed = true, defaultEnabled = true),
        Spec("home", allowed = true, defaultEnabled = false),
        Spec(JawamiAzkarSeed.COLLECTION_ID, allowed = true, defaultEnabled = false),
        Spec(AzkarFavorites.COLLECTION_ID, allowed = true, defaultEnabled = false),
        Spec("adhan", allowed = false, defaultEnabled = false),
    )

    fun spec(id: String): Spec? = specs.find { it.id == id }

    fun onboardingSpecs(): List<Spec> = specs.filter { it.allowed }

    fun defaultEnabledClockIds(): Set<String> =
        specs.filter { it.allowed && it.defaultEnabled && it.trigger == Trigger.CLOCK }
            .map { it.id }
            .toSet()

    /** حقل الكيان: الأقسام الزمنية المسموح لها فقط. بعد الصلاة مسار مواقيت منفصل. */
    fun entityAutoPlayAllowed(id: String): Boolean {
        val spec = spec(id) ?: return false
        return spec.allowed && spec.trigger == Trigger.CLOCK
    }

    fun entityDefaultEnabled(id: String): Boolean {
        val spec = spec(id) ?: return false
        return spec.allowed && spec.defaultEnabled && spec.trigger == Trigger.CLOCK
    }

    fun displayTitle(
        id: String,
        lang: String,
        collections: List<AdhkarCollectionEntity> = emptyList()
    ): String {
        collections.find { it.id == id }?.localizedTitle(lang)?.takeIf { it.isNotBlank() }?.let { return it }
        if (lang == "ar") return titleAr(id)
        ContentI18n.collectionTitle(id, lang)?.let { return it }
        if (lang == "en") return titleEn(id)
        return titleAr(id)
    }

    private fun titleAr(id: String): String = when (id) {
        "wake_up" -> "أذكار الاستيقاظ من النوم"
        "morning" -> "أذكار الصباح"
        PrayerRespectGate.AFTER_PRAYER_COLLECTION_ID -> "أذكار بعد الصلاة المفروضة"
        "evening" -> "أذكار المساء"
        "sleep" -> "أذكار النوم والأحلام"
        "home" -> "أذكار دخول وخروج المنزل"
        JawamiAzkarSeed.COLLECTION_ID -> "جوامع التسبيح"
        AzkarFavorites.COLLECTION_ID -> "الأذكار المفضلة"
        "adhan" -> "أذكار عند سماع الأذان"
        else -> id
    }

    private fun titleEn(id: String): String = when (id) {
        "wake_up" -> "Waking up"
        "morning" -> "Morning adhkar"
        PrayerRespectGate.AFTER_PRAYER_COLLECTION_ID -> "After obligatory prayer"
        "evening" -> "Evening adhkar"
        "sleep" -> "Sleep adhkar"
        "home" -> "Entering and leaving home"
        JawamiAzkarSeed.COLLECTION_ID -> "Jawami Tasbih"
        AzkarFavorites.COLLECTION_ID -> "Favorite adhkar"
        "adhan" -> "When hearing the adhan"
        else -> id
    }
}
