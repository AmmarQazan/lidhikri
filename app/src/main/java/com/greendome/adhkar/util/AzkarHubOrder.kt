package com.greendome.adhkar.util

import com.greendome.adhkar.data.AzkarFavorites
import com.greendome.adhkar.data.BlessedDaysAzkar
import com.greendome.adhkar.data.FridayAzkar
import com.greendome.adhkar.data.JawamiAzkarSeed
import com.greendome.adhkar.data.RidingAzkar
import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.prayer.PrayerRespectGate

/**
 * ترتيب أقسام شاشة الأذكار: تسبيحاتي + المجموعات.
 * الافتراضي: جوامع التسبيح مباشرة بعد التسبيحات القصيرة.
 */
object AzkarHubOrder {
    const val MY_DHIKR_ID = "my_dhikr"
    const val SHORT_TASBIH_ID = "short_tasbih"

    private val AFTER_JAWAMI = listOf(
        AzkarFavorites.COLLECTION_ID,
        "morning",
        "evening",
        PrayerRespectGate.AFTER_PRAYER_COLLECTION_ID,
        "sleep",
        "wake_up",
        "adhan",
        "home",
        RidingAzkar.COLLECTION_ID,
        FridayAzkar.COLLECTION_ID,
        BlessedDaysAzkar.COLLECTION_ID,
    )

    fun presentIds(collections: List<AdhkarCollectionEntity>): List<String> =
        buildList {
            add(MY_DHIKR_ID)
            collections.forEach { add(it.id) }
        }

    fun shortTasbihId(
        presentIds: Collection<String>,
        titleArById: Map<String, String> = emptyMap(),
    ): String? {
        if (SHORT_TASBIH_ID in presentIds) return SHORT_TASBIH_ID
        titleArById.entries.find { (_, title) -> isShortTasbihTitle(title) }?.key?.let { return it }
        if (MY_DHIKR_ID in presentIds) return MY_DHIKR_ID
        return null
    }

    fun defaultOrder(
        presentIds: List<String>,
        titleArById: Map<String, String> = emptyMap(),
    ): List<String> {
        val remaining = presentIds.toMutableSet()
        val result = mutableListOf<String>()
        val shortId = shortTasbihId(presentIds, titleArById)

        if (MY_DHIKR_ID in remaining && MY_DHIKR_ID != shortId) {
            result += MY_DHIKR_ID
            remaining.remove(MY_DHIKR_ID)
        }
        if (shortId != null && shortId in remaining) {
            result += shortId
            remaining.remove(shortId)
        }
        if (JawamiAzkarSeed.COLLECTION_ID in remaining) {
            result += JawamiAzkarSeed.COLLECTION_ID
            remaining.remove(JawamiAzkarSeed.COLLECTION_ID)
        }
        AFTER_JAWAMI.forEach { id ->
            if (id in remaining) {
                result += id
                remaining.remove(id)
            }
        }
        presentIds.filter { it in remaining }.forEach { result += it }
        return result
    }

    fun merge(
        saved: List<String>,
        presentIds: List<String>,
        titleArById: Map<String, String> = emptyMap(),
    ): List<String> {
        if (saved.isEmpty()) return defaultOrder(presentIds, titleArById)
        val remaining = presentIds.toMutableSet()
        val result = mutableListOf<String>()
        saved.forEach { id ->
            if (id in remaining) {
                result += id
                remaining.remove(id)
            }
        }
        presentIds.filter { it in remaining }.forEach { result += it }
        return result
    }

    fun moved(ids: List<String>, fromIndex: Int, toIndex: Int): List<String> {
        if (fromIndex == toIndex) return ids
        if (fromIndex !in ids.indices || toIndex !in ids.indices) return ids
        return ids.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
    }

    private fun isShortTasbihTitle(title: String): Boolean {
        val t = title.trim()
        return t.contains("التسبيحات القصيرة") || t.contains("تسبيحات قصيرة")
    }
}
