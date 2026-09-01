package com.greendome.adhkar.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdhanAzkarTest {

    @Test
    fun catalogUsesAdhanTriggerEnabledByDefault() {
        val spec = AutoAzkarCatalog.spec(AdhanAzkar.COLLECTION_ID)
        requireNotNull(spec)
        assertTrue(spec.allowed)
        assertTrue(spec.defaultEnabled)
        assertEquals(AutoAzkarCatalog.Trigger.ADHAN, spec.trigger)
        assertTrue(AdhanAzkar.COLLECTION_ID in AutoAzkarCatalog.onboardingSpecs().map { it.id })
        assertFalse(AdhanAzkar.COLLECTION_ID in AutoAzkarCatalog.defaultEnabledClockIds())
        assertFalse(AutoAzkarCatalog.entityAutoPlayAllowed(AdhanAzkar.COLLECTION_ID))
    }

    @Test
    fun onlyWasilahDuaIsSeeded() {
        val items = AdhanAzkar.items()
        assertEquals(1, items.size)
        assertTrue(items[0].text.contains("الْوَسِيلَةَ"))
        assertTrue(items[0].matches(items[0].text))
        assertFalse(items[0].matches("تقول مثل ما يقول المؤذن إلا في حيعلتين فيقول: لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللهِ."))
    }

    @Test
    fun waitsTenSecondsAfterAdhanEnds() {
        assertEquals(10_000L, AdhanAzkar.AFTER_END_DELAY_MS)
    }

    @Test
    fun entityHasNoClockAutoPlay() {
        val entity = AdhanAzkar.entity()
        assertEquals(AdhanAzkar.COLLECTION_ID, entity.id)
        assertEquals(AdhanAzkar.TITLE_AR, entity.titleAr)
        assertFalse(entity.autoPlayAllowed)
        assertFalse(entity.autoPlayEnabled)
    }
}
