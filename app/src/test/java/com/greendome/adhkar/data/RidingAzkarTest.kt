package com.greendome.adhkar.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RidingAzkarTest {

    @Test
    fun catalogUsesActivityTriggerEnabledByDefault() {
        val spec = AutoAzkarCatalog.spec(RidingAzkar.COLLECTION_ID)
        requireNotNull(spec)
        assertTrue(spec.allowed)
        assertTrue(spec.defaultEnabled)
        assertEquals(AutoAzkarCatalog.Trigger.ACTIVITY, spec.trigger)
        assertTrue(RidingAzkar.COLLECTION_ID in AutoAzkarCatalog.onboardingSpecs().map { it.id })
        assertFalse(RidingAzkar.COLLECTION_ID in AutoAzkarCatalog.defaultEnabledClockIds())
        assertFalse(AutoAzkarCatalog.entityAutoPlayAllowed(RidingAzkar.COLLECTION_ID))
    }

    @Test
    fun seedsAreCompleteAndDistinct() {
        val items = RidingAzkar.items()
        assertEquals(2, items.size)
        items.forEach { seed ->
            assertFalse(seed.text.contains("…"))
            assertFalse(seed.text.contains("..."))
            assertTrue(seed.text.length > 20)
        }
        assertTrue(items[0].text.contains("ظَلَمْتُ نَفْسِي"))
        assertTrue(items[1].text.contains("لَمُنقَلِبُونَ"))
        assertFalse(items[1].text.contains("ظَلَمْتُ نَفْسِي"))
        assertTrue(items[0].matches(items[0].text))
        assertFalse(items[0].matches(items[1].text))
        assertTrue(items[1].matches(items[1].text))
        assertFalse(items[1].matches(items[0].text))
    }

    @Test
    fun pickRandomUsesSelectedPool() {
        val stored = RidingAzkar.toEntities().mapIndexed { index, item ->
            item.copy(id = (index + 1).toLong())
        }
        val all = RidingAzkar.ids()
        repeat(8) {
            val picked = RidingAzkar.pickRandom(stored, all)
            assertNotNull(picked)
            assertTrue(RidingAzkar.items().any { it.matches(picked!!.textAr) })
        }
        assertNull(RidingAzkar.pickRandom(stored, emptySet()))
    }

    @Test
    fun entityHasNoClockAutoPlay() {
        val entity = RidingAzkar.entity()
        assertEquals(RidingAzkar.COLLECTION_ID, entity.id)
        assertFalse(entity.autoPlayAllowed)
        assertFalse(entity.autoPlayEnabled)
    }
}
