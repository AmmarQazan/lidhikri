package com.greendome.adhkar.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeAzkarTest {

    @Test
    fun catalogUsesLocationTriggerNotClock() {
        val spec = AutoAzkarCatalog.spec(HomeAzkar.COLLECTION_ID)
        requireNotNull(spec)
        assertTrue(spec.allowed)
        assertTrue(spec.defaultEnabled)
        assertEquals(AutoAzkarCatalog.Trigger.LOCATION, spec.trigger)
        assertTrue(HomeAzkar.COLLECTION_ID in AutoAzkarCatalog.onboardingSpecs().map { it.id })
        assertFalse(HomeAzkar.COLLECTION_ID in AutoAzkarCatalog.defaultEnabledClockIds())
        assertFalse(AutoAzkarCatalog.entityAutoPlayAllowed(HomeAzkar.COLLECTION_ID))
    }

    @Test
    fun seedsAreCompleteAndSplitByEvent() {
        val items = HomeAzkar.items()
        assertEquals(4, items.size)
        items.forEach { seed ->
            assertFalse(seed.text.contains("…"))
            assertFalse(seed.text.contains("..."))
            assertTrue(seed.text.length > 20)
        }
        assertEquals(2, items.count { it.event == HomeAzkar.Event.ENTER })
        assertEquals(2, items.count { it.event == HomeAzkar.Event.EXIT })
        assertTrue(items[1].text.contains("خَيْرَ الْمَوْلِجِ"))
        assertTrue(items[1].text.contains("وَلَجْنَا"))
        assertTrue(items[3].text.contains("أَظْلِمَ"))
        assertTrue(items[3].text.contains("أَجْهَلَ"))
    }

    @Test
    fun truncatedLegacyTextsStillMatch() {
        val shortEnter = "بِسْمِ اللهِ وَلَجْنَا، وَبِسْمِ اللهِ خَرَجْنَا، وَعَلَى رَبِّنَا تَوَكَّلْنَا."
        val truncatedMawlj = "اللَّهُمَّ إِنِّي أَسْأَلُكَ خَيْرَ الْمَوْلِجِ وَخَيْرَ الْمَخْرَجِ …"
        val exit = "بِسْمِ اللهِ، تَوَكَّلْتُ عَلَى اللهِ، وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللهِ."
        val truncatedAdilla = "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ أَنْ أَضِلَّ أَوْ أُضَلَّ، أَوْ أَزِلَّ أَوْ أُزَلَّ …"
        val byId = HomeAzkar.items().associateBy { it.id }
        assertTrue(byId.getValue("enter_walajna").matches(shortEnter))
        assertFalse(byId.getValue("enter_walajna").matches(truncatedMawlj))
        assertTrue(byId.getValue("enter_mawlaj").matches(truncatedMawlj))
        assertTrue(byId.getValue("exit_tawakkalt").matches(exit))
        assertTrue(byId.getValue("exit_adilla").matches(truncatedAdilla))
    }

    @Test
    fun pickRandomUsesSelectedEventPool() {
        val stored = HomeAzkar.toEntities().mapIndexed { index, item ->
            item.copy(id = (index + 1).toLong())
        }
        val enterOnly = HomeAzkar.idsFor(HomeAzkar.Event.ENTER)
        repeat(8) {
            val picked = HomeAzkar.pickRandom(stored, HomeAzkar.Event.ENTER, enterOnly)
            assertNotNull(picked)
            assertTrue(HomeAzkar.items().filter { it.event == HomeAzkar.Event.ENTER }.any { it.matches(picked!!.textAr) })
        }
        assertNull(HomeAzkar.pickRandom(stored, HomeAzkar.Event.ENTER, emptySet()))
    }

    @Test
    fun entityHasNoClockAutoPlay() {
        val entity = HomeAzkar.entity()
        assertEquals(HomeAzkar.COLLECTION_ID, entity.id)
        assertFalse(entity.autoPlayAllowed)
        assertFalse(entity.autoPlayEnabled)
    }

    @Test
    fun hasCoordinatesDoesNotNeedACityLabel() {
        assertTrue(HomeAzkar.hasCoordinates(21.3891, 39.8579))
        assertFalse(HomeAzkar.hasCoordinates(0.0, 0.0))
    }
}
