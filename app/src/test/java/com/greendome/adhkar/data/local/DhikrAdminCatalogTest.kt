package com.greendome.adhkar.data.local

import com.greendome.adhkar.data.model.DhikrCategory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DhikrAdminCatalogTest {

    @Test
    fun shortTasbihIsCatalogDeletable() {
        val dhikr = DhikrEntity(textAr = "سبحان الله", isDefault = true)
        assertTrue(dhikr.isBuiltinShortTasbih())
        assertTrue(dhikr.isAdminCatalogDeletable())
        assertFalse(dhikr.isJawamiSectionItem())
    }

    @Test
    fun jawamiIsCatalogDeletable() {
        val dhikr = DhikrEntity(
            textAr = "سُبْحَانَ اللهِ وَبِحَمْدِهِ",
            isDefault = true,
            category = DhikrCategory.JAWAMI,
            isLongForm = true,
        )
        assertTrue(dhikr.isJawamiSectionItem())
        assertTrue(dhikr.isAdminCatalogDeletable())
        assertFalse(dhikr.isBuiltinShortTasbih())
    }

    @Test
    fun eidTakbirIsNotCatalogDeletable() {
        val dhikr = DhikrEntity(
            textAr = "الله أكبر",
            isDefault = true,
            category = DhikrCategory.EID,
        )
        assertFalse(dhikr.isAdminCatalogDeletable())
        assertFalse(dhikr.isJawamiSectionItem())
    }
}
