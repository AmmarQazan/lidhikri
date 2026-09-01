package com.greendome.adhkar.ui

import com.greendome.adhkar.data.AutoAzkarCatalog
import com.greendome.adhkar.util.AzkarHubOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AzkarHubSectionIconsTest {

    @Test
    fun catalogSectionsHaveDedicatedIcons() {
        AutoAzkarCatalog.specs.forEach { spec ->
            assertNotEquals(
                "${spec.id} should have a dedicated icon",
                "menu_book",
                azkarHubSectionIconKey(spec.id),
            )
        }
    }

    @Test
    fun hubSpecialSectionsHaveDedicatedIcons() {
        assertEquals("edit", azkarHubSectionIconKey(AzkarHubOrder.MY_DHIKR_ID))
        assertEquals("repeat", azkarHubSectionIconKey(AzkarHubOrder.SHORT_TASBIH_ID))
        assertEquals("wb_sunny", azkarHubSectionIconKey("morning"))
        assertEquals("nights_stay", azkarHubSectionIconKey("evening"))
        assertEquals("mosque", azkarHubSectionIconKey("after_prayer"))
    }

    @Test
    fun unknownSectionFallsBackToBook() {
        assertEquals("menu_book", azkarHubSectionIconKey("custom_section"))
        assertTrue(azkarHubSectionIconKey("morning") != "menu_book")
    }
}
