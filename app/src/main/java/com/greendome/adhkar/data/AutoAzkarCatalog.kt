package com.greendome.adhkar.data

import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.prayer.PrayerRespectGate

/**
 * طبقتان لكل قسم:
 * - [Spec.allowed]: هل يُسمح أصلاً بالتشغيل التلقائي (المدير / البذرة)
 * - [Spec.defaultEnabled]: هل يُفعَّل للمستخدم من أول فتح
 */
object AutoAzkarCatalog {
    enum class Trigger { CLOCK, PRAYER, ADHAN, LOCATION, ACTIVITY }

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
        Spec(AdhanAzkar.COLLECTION_ID, allowed = true, defaultEnabled = true, trigger = Trigger.ADHAN),
        Spec("evening", allowed = true, defaultEnabled = true),
        Spec("sleep", allowed = true, defaultEnabled = true),
        Spec(HomeAzkar.COLLECTION_ID, allowed = true, defaultEnabled = true, trigger = Trigger.LOCATION),
        Spec(RidingAzkar.COLLECTION_ID, allowed = true, defaultEnabled = true, trigger = Trigger.ACTIVITY),
        Spec(FridayAzkar.COLLECTION_ID, allowed = true, defaultEnabled = true),
        Spec(BlessedDaysAzkar.COLLECTION_ID, allowed = true, defaultEnabled = true),
        Spec(JawamiAzkarSeed.COLLECTION_ID, allowed = true, defaultEnabled = false),
        Spec(AzkarFavorites.COLLECTION_ID, allowed = true, defaultEnabled = false),
    )

    fun spec(id: String): Spec? = specs.find { it.id == id }

    fun onboardingSpecs(): List<Spec> = specs.filter { it.allowed }

    fun defaultEnabledClockIds(): Set<String> =
        specs.filter { it.allowed && it.defaultEnabled && it.trigger == Trigger.CLOCK }
            .map { it.id }
            .toSet()

    /** حقل الكيان: الأقسام الزمنية المسموح لها فقط. بعد الصلاة والأذان والموقع مسارات منفصلة. */
    fun entityAutoPlayAllowed(id: String): Boolean {
        val spec = spec(id) ?: return false
        return spec.allowed && spec.trigger == Trigger.CLOCK
    }

    fun entityDefaultEnabled(id: String): Boolean {
        val spec = spec(id) ?: return false
        return spec.allowed && spec.defaultEnabled && spec.trigger == Trigger.CLOCK
    }

    data class EventAutoFlags(
        val afterPrayer: Boolean = false,
        val afterAdhan: Boolean = false,
        val home: Boolean = false,
        val riding: Boolean = false,
    )

    enum class HubAutoKind { CLOCK, AFTER_PRAYER, AFTER_ADHAN, HOME, RIDING }

    /** حالة التلقائي الظاهرة في قائمة أقسام الأذكار — الساعة أو المسارات المنفصلة. */
    fun hubAutoKind(
        collection: AdhkarCollectionEntity,
        flags: EventAutoFlags,
    ): HubAutoKind? {
        if (collection.autoPlayAllowed && collection.autoPlayEnabled) return HubAutoKind.CLOCK
        return when (spec(collection.id)?.trigger) {
            Trigger.PRAYER -> if (flags.afterPrayer) HubAutoKind.AFTER_PRAYER else null
            Trigger.ADHAN -> if (flags.afterAdhan) HubAutoKind.AFTER_ADHAN else null
            Trigger.LOCATION -> if (flags.home) HubAutoKind.HOME else null
            Trigger.ACTIVITY -> if (flags.riding) HubAutoKind.RIDING else null
            else -> null
        }
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
        RidingAzkar.COLLECTION_ID -> "أذكار الركوب"
        FridayAzkar.COLLECTION_ID -> "أذكار يوم الجمعة"
        BlessedDaysAzkar.COLLECTION_ID -> "أذكار الأيام المباركة"
        JawamiAzkarSeed.COLLECTION_ID -> "جوامع التسبيح"
        AzkarFavorites.COLLECTION_ID -> "الأذكار المفضلة"
        AdhanAzkar.COLLECTION_ID -> AdhanAzkar.TITLE_AR
        else -> id
    }

    private fun titleEn(id: String): String = when (id) {
        "wake_up" -> "Waking up"
        "morning" -> "Morning adhkar"
        PrayerRespectGate.AFTER_PRAYER_COLLECTION_ID -> "After obligatory prayer"
        "evening" -> "Evening adhkar"
        "sleep" -> "Sleep adhkar"
        "home" -> "Entering and leaving home"
        RidingAzkar.COLLECTION_ID -> "Riding adhkar"
        FridayAzkar.COLLECTION_ID -> "Friday adhkar"
        BlessedDaysAzkar.COLLECTION_ID -> "Blessed days adhkar"
        JawamiAzkarSeed.COLLECTION_ID -> "Jawami Tasbih"
        AzkarFavorites.COLLECTION_ID -> "Favorite adhkar"
        AdhanAzkar.COLLECTION_ID -> AdhanAzkar.TITLE_EN
        else -> id
    }
}
