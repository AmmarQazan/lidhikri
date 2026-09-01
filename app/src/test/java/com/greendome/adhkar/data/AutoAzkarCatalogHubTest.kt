package com.greendome.adhkar.data

import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.prayer.PrayerRespectGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutoAzkarCatalogHubTest {

    private val allOn = AutoAzkarCatalog.EventAutoFlags(
        afterPrayer = true,
        afterAdhan = true,
        home = true,
        riding = true,
    )

    @Test
    fun clockCollectionShowsWhenEnabled() {
        val morning = AdhkarCollectionEntity(
            id = "morning",
            titleAr = "صباح",
            autoPlayAllowed = true,
            autoPlayEnabled = true,
        )
        assertEquals(
            AutoAzkarCatalog.HubAutoKind.CLOCK,
            AutoAzkarCatalog.hubAutoKind(morning, AutoAzkarCatalog.EventAutoFlags()),
        )
    }

    @Test
    fun clockCollectionHiddenWhenOff() {
        val morning = AdhkarCollectionEntity(
            id = "morning",
            titleAr = "صباح",
            autoPlayAllowed = true,
            autoPlayEnabled = false,
        )
        assertNull(AutoAzkarCatalog.hubAutoKind(morning, allOn))
    }

    @Test
    fun eventCollectionsShowFromSettingsFlags() {
        assertEquals(
            AutoAzkarCatalog.HubAutoKind.AFTER_PRAYER,
            AutoAzkarCatalog.hubAutoKind(afterPrayerEntity(), allOn),
        )
        assertEquals(
            AutoAzkarCatalog.HubAutoKind.AFTER_ADHAN,
            AutoAzkarCatalog.hubAutoKind(AdhanAzkar.entity(), allOn),
        )
        assertEquals(
            AutoAzkarCatalog.HubAutoKind.HOME,
            AutoAzkarCatalog.hubAutoKind(HomeAzkar.entity(), allOn),
        )
        assertEquals(
            AutoAzkarCatalog.HubAutoKind.RIDING,
            AutoAzkarCatalog.hubAutoKind(RidingAzkar.entity(), allOn),
        )
    }

    @Test
    fun eventCollectionsHiddenWhenFlagsOff() {
        val off = AutoAzkarCatalog.EventAutoFlags()
        assertNull(AutoAzkarCatalog.hubAutoKind(afterPrayerEntity(), off))
        assertNull(AutoAzkarCatalog.hubAutoKind(AdhanAzkar.entity(), off))
        assertNull(AutoAzkarCatalog.hubAutoKind(HomeAzkar.entity(), off))
        assertNull(AutoAzkarCatalog.hubAutoKind(RidingAzkar.entity(), off))
    }

    @Test
    fun clockFieldsTakePriorityOverEventFlags() {
        val afterAdhanWithClock = AdhanAzkar.entity().copy(
            autoPlayAllowed = true,
            autoPlayEnabled = true,
        )
        assertEquals(
            AutoAzkarCatalog.HubAutoKind.CLOCK,
            AutoAzkarCatalog.hubAutoKind(afterAdhanWithClock, AutoAzkarCatalog.EventAutoFlags()),
        )
    }

    private fun afterPrayerEntity() = AdhkarCollectionEntity(
        id = PrayerRespectGate.AFTER_PRAYER_COLLECTION_ID,
        titleAr = "بعد الصلاة",
        autoPlayAllowed = false,
        autoPlayEnabled = false,
    )
}
