package com.greendome.adhkar.audio

import com.greendome.adhkar.data.SubaihatReciterSeed
import com.greendome.adhkar.data.local.DhikrEntity
import com.greendome.adhkar.data.local.ReciterAudioEntity
import com.greendome.adhkar.data.local.ReciterAzkarAudioEntity
import com.greendome.adhkar.data.model.DhikrCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReciterAudioResolveTest {

    @Test
    fun prefersBundledAssetOverRemoteUrl() {
        val audio = ReciterAudioEntity(
            reciterId = 1,
            dhikrId = 145,
            remoteUrl = "https://example.test/subhan.mp3",
            assetPath = "audio/subaihat/tasbih/subhan_allah.mp3",
        )
        assertEquals(
            PlayableAudio.Asset("audio/subaihat/tasbih/subhan_allah.mp3"),
            resolveReciterAudioEntity(audio),
        )
    }

    @Test
    fun usesTrimmedRemoteUrlWhenNoAssetOrFile() {
        val audio = ReciterAzkarAudioEntity(
            reciterId = 10,
            azkarItemId = 297,
            remoteUrl = " https://firebasestorage.googleapis.com/v0/b/bucket/o/a.mp3?alt=media ",
        )
        assertEquals(
            PlayableAudio.File(
                "https://firebasestorage.googleapis.com/v0/b/bucket/o/a.mp3?alt=media",
            ),
            resolveReciterAzkarAudioEntity(audio),
        )
    }

    @Test
    fun ignoresMissingLocalFileAndFallsBackToRemote() {
        val audio = ReciterAudioEntity(
            reciterId = 10,
            dhikrId = 145,
            localPath = File("does-not-exist-sabbih.mp3").absolutePath,
            remoteUrl = "https://example.test/voice.mp3",
        )
        assertEquals(PlayableAudio.File("https://example.test/voice.mp3"), resolveReciterAudioEntity(audio))
    }

    @Test
    fun rejectsNonAudioLocalFile() {
        val junk = File.createTempFile("sabbih-junk", ".mp3")
        junk.writeText("{ \"error\": 404 }")
        try {
            assertEquals(null, usableAudioFile(junk.absolutePath))
        } finally {
            junk.delete()
        }
    }

    @Test
    fun acceptsMp3Magic() {
        val mp3 = File.createTempFile("sabbih-audio", ".mp3")
        mp3.writeBytes(byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0xE0.toByte(), 0x40) + ByteArray(20))
        try {
            assertEquals(mp3.absolutePath, usableAudioFile(mp3.absolutePath))
            assertTrue(hasAudioMagic(mp3))
        } finally {
            mp3.delete()
        }
    }

    @Test
    fun mapsDefaultTasbihAssets() {
        assertEquals(
            "audio/subaihat/tasbih/subhan_allah.mp3",
            SubaihatReciterSeed.bundledDhikrAsset(DhikrCategory.GENERAL, 1),
        )
        assertEquals(
            "audio/subaihat/tasbih/eid_takbir.mp3",
            SubaihatReciterSeed.bundledDhikrAsset(DhikrCategory.EID, 100),
        )
        assertNull(SubaihatReciterSeed.bundledDhikrAsset(DhikrCategory.SLEEP, 1))
    }

    @Test
    fun mapsTasbihAssetsByArabicTextWhenSortOrderMissing() {
        assertEquals(
            "audio/subaihat/tasbih/alhamdulillah.mp3",
            SubaihatReciterSeed.bundledDhikrAsset(DhikrCategory.GENERAL, 0, "الحمدلله"),
        )
        assertEquals(
            "audio/subaihat/tasbih/tahlil.mp3",
            SubaihatReciterSeed.bundledDhikrAsset(DhikrCategory.GENERAL, 0, "لا إله إلا الله"),
        )
        assertEquals(
            "audio/subaihat/tasbih/allahu_akbar.mp3",
            SubaihatReciterSeed.bundledDhikrAsset(DhikrCategory.GENERAL, 0, "الله أكبر"),
        )
        val dhikr = DhikrEntity(
            textAr = "سبحان الله وبحمده",
            category = DhikrCategory.GENERAL,
            isDefault = true,
            sortOrder = 0,
            audioPath = "null",
        )
        assertEquals(
            PlayableAudio.Asset("audio/subaihat/tasbih/subhan_bihamd.mp3"),
            bundledPlayableForDhikr(dhikr),
        )
        assertEquals(
            PlayableAudio.Asset("audio/subaihat/tasbih/subhan_bihamd.mp3"),
            preferLocalOrBundled(
                PlayableAudio.File("https://example.test/missing.mp3"),
                bundledPlayableForDhikr(dhikr),
            ),
        )
    }

    @Test
    fun ignoresLiteralNullAssetPath() {
        assertNull(usableAssetPath("null"))
        assertNull(usableAssetPath("NULL"))
        val audio = ReciterAudioEntity(
            reciterId = 10,
            dhikrId = 146,
            remoteUrl = "https://example.test/hamd.mp3",
            assetPath = "null",
        )
        assertEquals(
            PlayableAudio.File("https://example.test/hamd.mp3"),
            resolveReciterAudioEntity(audio),
        )
    }

    @Test
    fun prefersBundledTasbihWhenReciterOnlyHasRemoteUrl() {
        val dhikr = DhikrEntity(
            textAr = "سبحان الله",
            category = DhikrCategory.GENERAL,
            isDefault = true,
            sortOrder = 1,
            audioPath = "audio/subaihat/tasbih/subhan_allah.mp3",
        )
        val remoteOnly = PlayableAudio.File("https://example.test/missing.mp3")
        assertEquals(
            PlayableAudio.Asset("audio/subaihat/tasbih/subhan_allah.mp3"),
            preferLocalOrBundled(remoteOnly, bundledPlayableForDhikr(dhikr)),
        )
    }
}
