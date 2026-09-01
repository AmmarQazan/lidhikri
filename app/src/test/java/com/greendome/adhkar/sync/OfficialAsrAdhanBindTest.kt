package com.greendome.adhkar.sync

import com.greendome.adhkar.data.BundledAdhanSeed
import com.greendome.adhkar.prayer.AdhanSoundMode
import com.greendome.adhkar.prayer.PrayerAlertSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialAsrAdhanBindTest {
    @Test
    fun bindsDefaultAndIndonesianAsr() {
        assertTrue(RemoteContentSync.shouldBindOfficialAsrAdhan(PrayerAlertSettings()))
        assertTrue(
            RemoteContentSync.shouldBindOfficialAsrAdhan(
                PrayerAlertSettings(
                    soundMode = AdhanSoundMode.CATALOG,
                    catalogId = BundledAdhanSeed.DEFAULT_ID,
                )
            )
        )
        assertTrue(
            RemoteContentSync.shouldBindOfficialAsrAdhan(
                PrayerAlertSettings(
                    soundMode = AdhanSoundMode.CATALOG,
                    catalogId = BundledAdhanSeed.ASR_DEFAULT_ID,
                )
            )
        )
    }

    @Test
    fun keepsCustomSilentAndOtherCatalogVoices() {
        assertFalse(
            RemoteContentSync.shouldBindOfficialAsrAdhan(
                PrayerAlertSettings(soundMode = AdhanSoundMode.SILENT)
            )
        )
        assertFalse(
            RemoteContentSync.shouldBindOfficialAsrAdhan(
                PrayerAlertSettings(soundMode = AdhanSoundMode.CUSTOM, customPath = "/a.mp3")
            )
        )
        assertFalse(
            RemoteContentSync.shouldBindOfficialAsrAdhan(
                PrayerAlertSettings(soundMode = AdhanSoundMode.CATALOG, catalogId = 20017L)
            )
        )
    }
}
