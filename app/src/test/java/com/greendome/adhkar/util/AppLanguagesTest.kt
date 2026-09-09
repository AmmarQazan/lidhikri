package com.greendome.adhkar.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

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
    fun arabicIsDefaultAndOthersAreEnabled() {
        assertTrue(AppLanguages.isEnabled("ar"))
        assertTrue(AppLanguages.isEnabled("en"))
        assertTrue(AppLanguages.isEnabled("tr"))
        assertEquals("en", AppLanguages.coerce("en"))
        assertEquals("ar", AppLanguages.coerce("xx"))
        assertEquals("ar", AppLanguages.coerce("ar"))
        assertEquals("id", AppLanguages.coerce("in"))
        assertEquals("id", AppLanguages.coerce("id-ID"))
    }

    @Test
    fun fromDevicePicksFirstSupportedLocale() {
        assertEquals("id", AppLanguages.fromDevice(listOf(Locale("in", "ID"))))
        assertEquals("fr", AppLanguages.fromDevice(listOf(Locale.FRENCH)))
        assertEquals("ar", AppLanguages.fromDevice(listOf(Locale("de"), Locale("ar"))))
        assertEquals("en", AppLanguages.fromDevice(listOf(Locale.JAPANESE)))
        assertEquals("en", AppLanguages.fromDevice(emptyList()))
    }

    @Test
    fun systemLocaleTagMatchesResourceFolders() {
        assertEquals("ar", AppLanguages.systemLocaleTag("ar"))
        assertEquals("en", AppLanguages.systemLocaleTag("en"))
        assertEquals("in", AppLanguages.systemLocaleTag("id"))
        assertEquals("ar", AppLanguages.systemLocaleTag("xx"))
        assertEquals("hi", AppLanguages.systemLocaleTag("hi"))
        assertEquals("ur", AppLanguages.systemLocaleTag("ur"))
    }

    @Test
    fun launcherAliasClassUsesResourceLocale() {
        assertEquals("com.greendome.adhkar.launcher.Ar", AppLanguages.launcherAliasClass("ar"))
        assertEquals("com.greendome.adhkar.launcher.En", AppLanguages.launcherAliasClass("en"))
        assertEquals("com.greendome.adhkar.launcher.In", AppLanguages.launcherAliasClass("id"))
        assertEquals("com.greendome.adhkar.launcher.Hi", AppLanguages.launcherAliasClass("hi"))
        assertEquals("com.greendome.adhkar.launcher.Ar", AppLanguages.launcherAliasClass("xx"))
    }

    @Test
    fun pickerPairsIncludeArabicAndEnglish() {
        val pairs = AppLanguages.pickerPairs()
        assertTrue(pairs.contains("ar" to "العربية"))
        assertTrue(pairs.contains("en" to "English"))
        assertEquals(8, pairs.size)
    }
}
