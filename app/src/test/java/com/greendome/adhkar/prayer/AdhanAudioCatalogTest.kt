package com.greendome.adhkar.prayer

import com.greendome.adhkar.data.local.AdhanAudioEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdhanAudioCatalogTest {
    private fun file(id: Long, fajr: Boolean, url: String = "https://example/$id.mp3") =
        AdhanAudioEntity(id = id, nameAr = "أذان $id", suitableForFajr = fajr, remoteUrl = url)

    @Test
    fun fajrRecordingIsOnlyForFajr() {
        val entity = file(1, fajr = true)
        assertTrue(AdhanAudioResolver.isSuitable(entity, PrayerName.FAJR))
        assertFalse(AdhanAudioResolver.isSuitable(entity, PrayerName.DHUHR))
        assertFalse(AdhanAudioResolver.isSuitable(entity, PrayerName.ISHA))
    }

    @Test
    fun regularRecordingIsNotForFajr() {
        val entity = file(2, fajr = false)
        assertFalse(AdhanAudioResolver.isSuitable(entity, PrayerName.FAJR))
        assertTrue(AdhanAudioResolver.isSuitable(entity, PrayerName.ASR))
        assertTrue(AdhanAudioResolver.isSuitable(entity, PrayerName.MAGHRIB))
    }

    @Test
    fun inactiveRecordingIsNeverUsed() {
        val entity = file(3, fajr = true).copy(isActive = false)
        assertFalse(AdhanAudioResolver.isSuitable(entity, PrayerName.FAJR))
    }

    @Test
    fun defaultPicksFirstMatchingCatalogFile() {
        val catalog = listOf(file(10, fajr = false), file(11, fajr = true))
        val path = AdhanAudioResolver.resolve(
            prayer = PrayerName.FAJR,
            alert = PrayerAlertSettings(),
            catalog = catalog,
            shortTone = { "short" },
            defaultTone = { "tone" },
        )
        assertEquals("https://example/11.mp3", path)
    }

    @Test
    fun catalogChoiceForWrongPrayerFallsBackToTone() {
        val other = file(20, fajr = false)
        val path = AdhanAudioResolver.resolve(
            prayer = PrayerName.FAJR,
            alert = PrayerAlertSettings(soundMode = AdhanSoundMode.CATALOG, catalogId = 20),
            catalog = listOf(other),
            shortTone = { "short" },
            defaultTone = { "tone" },
        )
        assertEquals("tone", path)
    }

    @Test
    fun maqamFallsBackToArabicWhenEnglishBlank() {
        val entity = AdhanAudioEntity(
            id = 20933,
            nameAr = "كبارة",
            maqamAr = "رست",
            maqamEn = "",
        )
        assertEquals("رست", entity.localizedMaqam("ar"))
        assertEquals("رست", entity.localizedMaqam("en"))
        val named = entity.copy(maqamEn = "Rast")
        assertEquals("Rast", named.localizedMaqam("en"))
        assertEquals("رست", named.localizedMaqam("ar"))
    }
}
