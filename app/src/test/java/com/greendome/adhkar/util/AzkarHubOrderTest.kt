package com.greendome.adhkar.util

import com.greendome.adhkar.data.AzkarFavorites
import com.greendome.adhkar.data.JawamiAzkarSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AzkarHubOrderTest {

    private val present = listOf(
        AzkarHubOrder.MY_DHIKR_ID,
        AzkarFavorites.COLLECTION_ID,
        "morning",
        "evening",
        "after_prayer",
        "sleep",
        "wake_up",
        "adhan",
        "home",
        "riding",
        JawamiAzkarSeed.COLLECTION_ID,
        "friday",
        "blessed_days",
    )

    @Test
    fun defaultPutsJawamiRightAfterShortTasbih() {
        val order = AzkarHubOrder.defaultOrder(present)
        val shortIdx = order.indexOf(AzkarHubOrder.MY_DHIKR_ID)
        val jawamiIdx = order.indexOf(JawamiAzkarSeed.COLLECTION_ID)
        assertTrue(shortIdx >= 0)
        assertEquals(shortIdx + 1, jawamiIdx)
    }

    @Test
    fun defaultUsesNamedShortTasbihCollectionWhenPresent() {
        val ids = listOf(
            AzkarHubOrder.MY_DHIKR_ID,
            AzkarFavorites.COLLECTION_ID,
            AzkarHubOrder.SHORT_TASBIH_ID,
            JawamiAzkarSeed.COLLECTION_ID,
            "morning",
        )
        val order = AzkarHubOrder.defaultOrder(ids)
        assertEquals(AzkarHubOrder.MY_DHIKR_ID, order[0])
        assertEquals(AzkarHubOrder.SHORT_TASBIH_ID, order[1])
        assertEquals(JawamiAzkarSeed.COLLECTION_ID, order[2])
    }

    @Test
    fun defaultUsesArabicTitleAsShortTasbihAnchor() {
        val ids = listOf("morning", "custom", JawamiAzkarSeed.COLLECTION_ID)
        val order = AzkarHubOrder.defaultOrder(
            ids,
            titleArById = mapOf("custom" to "التسبيحات القصيرة"),
        )
        assertEquals("custom", order[0])
        assertEquals(JawamiAzkarSeed.COLLECTION_ID, order[1])
        assertEquals("morning", order[2])
    }

    @Test
    fun mergeKeepsSavedOrderAndAppendsNewSections() {
        val saved = listOf("evening", AzkarHubOrder.MY_DHIKR_ID, "morning")
        val order = AzkarHubOrder.merge(saved, present)
        assertEquals("evening", order[0])
        assertEquals(AzkarHubOrder.MY_DHIKR_ID, order[1])
        assertEquals("morning", order[2])
        assertTrue(order.contains(JawamiAzkarSeed.COLLECTION_ID))
        assertEquals(present.size, order.size)
        assertEquals(present.toSet(), order.toSet())
    }

    @Test
    fun mergeEmptySavedUsesDefault() {
        val order = AzkarHubOrder.merge(emptyList(), present)
        assertEquals(AzkarHubOrder.defaultOrder(present), order)
    }

    @Test
    fun movedSwapsItems() {
        val ids = listOf("a", "b", "c")
        assertEquals(listOf("b", "a", "c"), AzkarHubOrder.moved(ids, 0, 1))
        assertEquals(listOf("a", "c", "b"), AzkarHubOrder.moved(ids, 2, 1))
        assertEquals(ids, AzkarHubOrder.moved(ids, 1, 1))
        assertEquals(ids, AzkarHubOrder.moved(ids, -1, 0))
    }
}
