package com.greendome.adhkar.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguagesTest {
    @Test
    fun includesRequestedLanguages() {
        val codes = AppLanguages.codes
        assertTrue(codes.containsAll(listOf("ar", "en", "fr", "es", "tr", "ur", "id", "hi")))
        assertEquals(8, codes.size)
    }

    @Test
    fun rtlOnlyArabicAndUrdu() {
        assertTrue(AppLanguages.isRtl("ar"))
        assertTrue(AppLanguages.isRtl("ur"))
        assertFalse(AppLanguages.isRtl("tr"))
        assertFalse(AppLanguages.isRtl("id"))
        assertFalse(AppLanguages.isRtl("hi"))
        assertFalse(AppLanguages.isRtl("en"))
    }

    @Test
    fun nativeNamesAreLocal() {
        assertEquals("Türkçe", AppLanguages.nativeName("tr"))
        assertEquals("اردو", AppLanguages.nativeName("ur"))
        assertEquals("Bahasa Indonesia", AppLanguages.nativeName("id"))
        assertEquals("हिन्दी", AppLanguages.nativeName("hi"))
    }

    @Test
    fun onlyArabicIsEnabled() {
        assertTrue(AppLanguages.isEnabled("ar"))
        assertFalse(AppLanguages.isEnabled("en"))
        assertFalse(AppLanguages.isEnabled("tr"))
        assertFalse(AppLanguages.isEnabled("ur"))
        assertFalse(AppLanguages.isEnabled("id"))
        assertFalse(AppLanguages.isEnabled("hi"))
        assertEquals("ar", AppLanguages.coerce("en"))
        assertEquals("ar", AppLanguages.coerce("tr"))
        assertEquals("ar", AppLanguages.coerce("ar"))
    }

    @Test
    fun pickerPairsOnlyEnabledLanguages() {
        val pairs = AppLanguages.pickerPairs()
        assertEquals(listOf("ar" to "العربية"), pairs)
        assertFalse(pairs.any { it.first == "en" })
    }
}
