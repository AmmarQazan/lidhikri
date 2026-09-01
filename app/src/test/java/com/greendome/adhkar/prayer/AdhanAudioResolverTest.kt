package com.greendome.adhkar.prayer

import com.greendome.adhkar.data.BundledAdhanSeed
import com.greendome.adhkar.data.local.AdhanAudioEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdhanAudioResolverTest {
    private val catalog = BundledAdhanSeed.entities()

    @Test
    fun defaultPlaysQassasForFajr() {
        val uri = AdhanAudioResolver.resolve(
            prayer = PrayerName.FAJR,
            alert = PrayerAlertSettings(),
            catalog = catalog,
            shortTone = { "short" },
            defaultTone = { "tone" },
        )
        assertTrue(uri!!.contains("muhammad_qassas_madinah_fajr"))
    }

    @Test
    fun defaultPlaysIndonesianForOtherPrayers() {
        PrayerName.entries.filter { it != PrayerName.FAJR }.forEach { prayer ->
            val uri = AdhanAudioResolver.resolve(
                prayer = prayer,
                alert = PrayerAlertSettings(),
                catalog = catalog,
                shortTone = { "short" },
                defaultTone = { "tone" },
            )
            assertTrue(prayer.name, uri!!.contains("dhiyauddin_nizaruddin_indonesia"))
        }
    }

    @Test
    fun catalogSelectionPlaysChosenVoice() {
        val qassas = catalog.first { it.id == BundledAdhanSeed.FAJR_DEFAULT_ID }
        val uri = AdhanAudioResolver.resolve(
            prayer = PrayerName.FAJR,
            alert = PrayerAlertSettings(
                soundMode = AdhanSoundMode.CATALOG,
                catalogId = qassas.id,
            ),
            catalog = catalog,
            shortTone = { "short" },
            defaultTone = { "tone" },
        )
        assertTrue(uri!!.contains("muhammad_qassas_madinah_fajr"))
    }

    @Test
    fun indonesianIsSuitableForAllPrayers() {
        val indonesia = catalog.first { it.id == BundledAdhanSeed.DEFAULT_ID }
        PrayerName.entries.forEach { prayer ->
            assertTrue(prayer.name, AdhanAudioResolver.isSuitable(indonesia, prayer))
        }
    }

    @Test
    fun qassasIsFajrOnly() {
        val qassas = catalog.first { it.id == BundledAdhanSeed.FAJR_DEFAULT_ID }
        assertTrue(AdhanAudioResolver.isSuitable(qassas, PrayerName.FAJR))
        assertFalse(AdhanAudioResolver.isSuitable(qassas, PrayerName.DHUHR))
        assertFalse(AdhanAudioResolver.isSuitable(qassas, PrayerName.ISHA))
    }

    @Test
    fun remoteFajrOnlyStaysExclusive() {
        val fajrOnly = AdhanAudioEntity(
            id = 5,
            nameAr = "فجر",
            suitableForFajr = true,
        )
        assertTrue(AdhanAudioResolver.isSuitable(fajrOnly, PrayerName.FAJR))
        assertFalse(AdhanAudioResolver.isSuitable(fajrOnly, PrayerName.DHUHR))
    }

    @Test
    fun defaultIds() {
        assertEquals(10001L, BundledAdhanSeed.DEFAULT_ID)
        assertEquals(10005L, BundledAdhanSeed.FAJR_DEFAULT_ID)
        assertEquals(22002L, BundledAdhanSeed.ASR_DEFAULT_ID)
        assertEquals(BundledAdhanSeed.FAJR_DEFAULT_ID, BundledAdhanSeed.defaultId(PrayerName.FAJR))
        assertEquals(BundledAdhanSeed.ASR_DEFAULT_ID, BundledAdhanSeed.defaultId(PrayerName.ASR))
        assertEquals(BundledAdhanSeed.DEFAULT_ID, BundledAdhanSeed.defaultId(PrayerName.DHUHR))
        assertEquals(
            "adhan/dhiyauddin_nizaruddin_indonesia.mp3",
            catalog.first { it.id == BundledAdhanSeed.DEFAULT_ID }.assetPath,
        )
        assertEquals(
            "adhan/muhammad_qassas_madinah_fajr.mp3",
            catalog.first { it.id == BundledAdhanSeed.FAJR_DEFAULT_ID }.assetPath,
        )
    }

    @Test
    fun defaultAsrUsesNaifWhenCatalogHasHim() {
        val naif = AdhanAudioEntity(
            id = BundledAdhanSeed.ASR_DEFAULT_ID,
            nameAr = "نايف",
            remoteUrl = "https://example/naif.mp3",
            suitableForFajr = false,
        )
        val uri = AdhanAudioResolver.resolve(
            prayer = PrayerName.ASR,
            alert = PrayerAlertSettings(),
            catalog = catalog + naif,
            shortTone = { "short" },
            defaultTone = { "tone" },
        )
        assertEquals("https://example/naif.mp3", uri)
    }

    @Test
    fun naifIsSuitableForAsr() {
        val naif = AdhanAudioEntity(
            id = BundledAdhanSeed.ASR_DEFAULT_ID,
            nameAr = "نايف",
            suitableForFajr = false,
        )
        assertTrue(AdhanAudioResolver.isSuitable(naif, PrayerName.ASR))
        assertTrue(AdhanAudioResolver.isSuitable(naif, PrayerName.DHUHR))
        assertFalse(AdhanAudioResolver.isSuitable(naif, PrayerName.FAJR))
    }

    @Test
    fun playbackUriUsesAssetSchemeWithoutContext() {
        val indonesia = catalog.first { it.id == BundledAdhanSeed.DEFAULT_ID }
        assertEquals(
            "asset:///adhan/dhiyauddin_nizaruddin_indonesia.mp3",
            AdhanAudioResolver.playbackUri(indonesia),
        )
        assertTrue(AdhanAudioResolver.hasLocalPlayback(indonesia))
        assertFalse(AdhanAudioResolver.isRemoteUrl(AdhanAudioResolver.playbackUri(indonesia)!!))
    }

    @Test
    fun playbackUriUsesRemoteWhenNoLocalFile() {
        val remote = AdhanAudioEntity(
            id = 20001,
            nameAr = "مؤذن",
            remoteUrl = "https://example/adhan.mp3",
        )
        assertEquals("https://example/adhan.mp3", AdhanAudioResolver.playbackUri(remote))
        assertFalse(AdhanAudioResolver.hasLocalPlayback(remote))
        assertTrue(AdhanAudioResolver.isRemoteUrl(AdhanAudioResolver.playbackUri(remote)!!))
    }

    @Test
    fun playbackUriPrefersExistingLocalFile() {
        val tmp = kotlin.io.path.createTempFile(suffix = ".mp3").toFile()
        try {
            tmp.writeBytes(byteArrayOf(1, 2, 3, 4))
            val entity = AdhanAudioEntity(
                id = 20002,
                nameAr = "مؤذن",
                localPath = tmp.absolutePath,
                remoteUrl = "https://example/adhan.mp3",
                assetPath = "adhan/missing.mp3",
            )
            assertEquals(tmp.absolutePath, AdhanAudioResolver.playbackUri(entity))
            assertTrue(AdhanAudioResolver.hasLocalPlayback(entity))
        } finally {
            tmp.delete()
        }
    }
}
