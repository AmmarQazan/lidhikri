package com.greendome.adhkar.sync

import com.greendome.adhkar.data.BundledAdhanSeed
import com.greendome.adhkar.prayer.AdhanSoundMode
import com.greendome.adhkar.prayer.PrayerAlertSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialAsrAdhanBindTest {
    @Test
    fun resetsFormerNaifOfficialDefault() {
        assertTrue(
            RemoteContentSync.shouldResetOfficialAsrAdhan(
                PrayerAlertSettings(
                    soundMode = AdhanSoundMode.CATALOG,
                    catalogId = BundledAdhanSeed.ASR_DEFAULT_ID,
                )
            )
        )
        assertTrue(
            BundledAdhanSeed.isFormerOfficialAsrDefault(
                PrayerAlertSettings(catalogId = BundledAdhanSeed.ASR_DEFAULT_ID)
            )
        )
    }

    @Test
    fun keepsCustomSilentAndOtherCatalogVoices() {
        assertFalse(
            RemoteContentSync.shouldResetOfficialAsrAdhan(
                PrayerAlertSettings(soundMode = AdhanSoundMode.SILENT)
            )
        )
        assertFalse(
            RemoteContentSync.shouldResetOfficialAsrAdhan(
                PrayerAlertSettings(soundMode = AdhanSoundMode.CUSTOM, customPath = "/a.mp3")
            )
        )
        assertFalse(
            RemoteContentSync.shouldResetOfficialAsrAdhan(
                PrayerAlertSettings(soundMode = AdhanSoundMode.CATALOG, catalogId = 20017L)
            )
        )
        assertFalse(
            RemoteContentSync.shouldResetOfficialAsrAdhan(
                PrayerAlertSettings()
            )
        )
        assertFalse(
            RemoteContentSync.shouldResetOfficialAsrAdhan(
                PrayerAlertSettings(
                    soundMode = AdhanSoundMode.CATALOG,
                    catalogId = BundledAdhanSeed.DEFAULT_ID,
                )
            )
        )
    }
}
