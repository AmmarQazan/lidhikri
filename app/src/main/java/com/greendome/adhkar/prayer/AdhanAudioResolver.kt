package com.greendome.adhkar.prayer

import android.content.Context
import com.greendome.adhkar.data.BundledAdhanSeed
import com.greendome.adhkar.data.local.AdhanAudioEntity
import com.greendome.adhkar.data.local.AdhkarDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

object AdhanAudioResolver {
    fun isSuitable(entity: AdhanAudioEntity, prayer: PrayerName): Boolean {
        if (!entity.isActive) return false
        if (entity.id == BundledAdhanSeed.DEFAULT_ID) return true
        if (entity.id == BundledAdhanSeed.ASR_DEFAULT_ID && prayer == PrayerName.ASR) return true
        return if (prayer == PrayerName.FAJR) entity.suitableForFajr else !entity.suitableForFajr
    }

    fun isRemoteUrl(value: String): Boolean =
        value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true)

    fun hasLocalPlayback(entity: AdhanAudioEntity, context: Context? = null): Boolean {
        val uri = playbackUri(entity, context) ?: return false
        return !isRemoteUrl(uri)
    }

    fun playbackUri(entity: AdhanAudioEntity, context: Context? = null): String? {
        entity.localPath
            ?.takeIf { path -> File(path).let { it.isFile && it.length() > 0L } }
            ?.let { return it }
        entity.assetPath?.takeIf { it.isNotBlank() }?.let { asset ->
            if (context != null) {
                cachedAssetFile(context, entity.id, asset)?.let { return it }
            } else {
                return "asset:///$asset"
            }
        }
        return entity.remoteUrl?.takeIf { it.isNotBlank() }
    }

    fun suitableFor(catalog: List<AdhanAudioEntity>, prayer: PrayerName): List<AdhanAudioEntity> =
        catalog.filter { isSuitable(it, prayer) }

    fun resolve(
        prayer: PrayerName,
        alert: PrayerAlertSettings,
        catalog: List<AdhanAudioEntity>,
        shortTone: () -> String,
        defaultTone: () -> String,
        context: Context? = null,
    ): String? {
        val suitable = suitableFor(catalog, prayer)
        fun exact(preferredId: Long): String? =
            suitable.find { it.id == preferredId }?.let { playbackUri(it, context) }
        fun anyReady(): String? =
            suitable.firstNotNullOfOrNull { playbackUri(it, context) }
        val bundledDefault = BundledAdhanSeed.defaultId(prayer)
        val indonesia = BundledAdhanSeed.DEFAULT_ID
        return when (alert.resolvedSoundMode()) {
            AdhanSoundMode.SILENT -> null
            AdhanSoundMode.SHORT -> shortTone()
            AdhanSoundMode.CUSTOM, AdhanSoundMode.RECORDED -> {
                File(alert.customPath).takeIf { it.isFile && it.length() > 0L }?.absolutePath
                    ?: exact(alert.catalogId)
                    ?: exact(bundledDefault)
                    ?: exact(indonesia)
                    ?: defaultTone()
            }
            AdhanSoundMode.CATALOG ->
                exact(alert.catalogId)
                    ?: exact(bundledDefault)
                    ?: exact(indonesia)
                    ?: anyReady()
                    ?: defaultTone()
            AdhanSoundMode.DEFAULT ->
                exact(bundledDefault)
                    ?: exact(indonesia)
                    ?: anyReady()
                    ?: defaultTone()
        }
    }

    fun resolveBlocking(
        context: Context,
        prayer: PrayerName,
        alert: PrayerAlertSettings,
    ): String? = runBlocking {
        val catalog = withContext(Dispatchers.IO) {
            AdhkarDatabase.get(context).adhanAudioDao().getAll()
        }
        resolve(
            prayer = prayer,
            alert = alert,
            catalog = catalog,
            shortTone = { AdhanToneGenerator.shortFile(context).absolutePath },
            defaultTone = { AdhanToneGenerator.defaultFile(context).absolutePath },
            context = context,
        )
    }

    private fun cachedAssetFile(context: Context, id: Long, assetPath: String): String? {
        val dest = File(context.filesDir, "adhan_voices").apply { mkdirs() }.resolve("$id.mp3")
        if (dest.length() > 1000L) return dest.absolutePath
        return runCatching {
            context.assets.open(assetPath).use { input ->
                dest.outputStream().use { input.copyTo(it) }
            }
            dest.takeIf { it.length() > 0L }?.absolutePath
        }.getOrNull()
    }
}
