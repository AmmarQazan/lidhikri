package com.greendome.adhkar.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutTest {

    @Test
    fun defaultOrderListsEverySection() {
        assertEquals(HomeSection.entries.map { it.id }, HomeLayout.merge(emptyList()))
    }

    @Test
    fun defaultHidesQiblaUntilUserSaves() {
        val hidden = HomeLayout.hidden(emptySet(), keyPresent = false)
        assertEquals(setOf(HomeSection.QIBLA.id), hidden)
    }

    @Test
    fun emptySavedHiddenMeansNothingHidden() {
        val hidden = HomeLayout.hidden(emptySet(), keyPresent = true)
        assertTrue(hidden.isEmpty())
        assertFalse(HomeSection.QIBLA.id in hidden)
    }

    @Test
    fun mergeKeepsSavedOrderAndAppendsNewSections() {
        val saved = listOf(HomeSection.TODAY_STATS.id, HomeSection.NEXT_PRAYER.id)
        val order = HomeLayout.merge(saved)
        assertEquals(HomeSection.TODAY_STATS.id, order[0])
        assertEquals(HomeSection.NEXT_PRAYER.id, order[1])
        assertEquals(HomeSection.entries.size, order.size)
        assertEquals(HomeSection.entries.map { it.id }.toSet(), order.toSet())
    }

    @Test
    fun mergeDropsUnknownIds() {
        val order = HomeLayout.merge(listOf("unknown", HomeSection.QIBLA.id))
        assertFalse("unknown" in order)
        assertEquals(HomeSection.QIBLA.id, order[0])
    }

    @Test
    fun customizableHidesEmbeddedReminderCards() {
        val ids = HomeLayout.customizable(emptyList())
        assertFalse(HomeSection.AUTO_TASBIH.id in ids)
        assertFalse(HomeSection.AUTO_AZKAR.id in ids)
        assertFalse(HomeSection.QIBLA.id in ids)
        assertTrue(HomeSection.NEXT_PRAYER.id in ids)
        assertTrue(HomeSection.PRAYER_TIMES.id in ids)
        assertTrue(HomeSection.TODAY_STATS.id in ids)
    }

    @Test
    fun movedReordersWithinBounds() {
        val ids = listOf("a", "b", "c")
        assertEquals(listOf("b", "a", "c"), HomeLayout.moved(ids, 0, 1))
        assertEquals(listOf("a", "c", "b"), HomeLayout.moved(ids, 2, 1))
        assertEquals(ids, HomeLayout.moved(ids, 1, 1))
        assertEquals(ids, HomeLayout.moved(ids, -1, 0))
    }
}
