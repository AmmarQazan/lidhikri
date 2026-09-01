package com.greendome.adhkar.service

import android.content.Context
import com.greendome.adhkar.audio.AudioDownloadManager
import com.greendome.adhkar.data.ReciterLibraryDownloadPolicy
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.ReciterAudioEntity
import com.greendome.adhkar.data.local.ReciterAzkarAudioEntity
import com.greendome.adhkar.data.model.AudioSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object OfflineDownloadHelper {
    suspend fun downloadAllPending(context: Context): Int = withContext(Dispatchers.IO) {
        val db = AdhkarDatabase.get(context)
        val settings = SettingsRepository(context)
        val reciters = db.reciterDao().getAll()
        val allowedIds = ReciterLibraryDownloadPolicy.allowedReciterIds(
            reciters = reciters.map { it.id to it.isBuiltin },
            optedInIds = settings.optedInReciterLibraryIds(),
        )
        val tasbihIds = reciters.filter { it.id in allowedIds && it.voiceScope.allowsTasbih() }.map { it.id }.toSet()
        val azkarIds = reciters.filter { it.id in allowedIds && it.voiceScope.allowsAzkar() }.map { it.id }.toSet()
        downloadDhikrFiles(context, db) +
            downloadReciterAudio(context, db, db.reciterAudioDao().getPendingDownloads().filter { it.reciterId in tasbihIds }) +
            downloadAzkarReciterAudio(
                context,
                db,
                db.reciterAzkarAudioDao().getPendingDownloads().filter { it.reciterId in azkarIds },
            )
    }

    suspend fun downloadReciterLibrary(context: Context, reciterId: Long): Int = withContext(Dispatchers.IO) {
        val db = AdhkarDatabase.get(context)
        SettingsRepository(context).optInReciterLibrary(reciterId)
        val reciter = db.reciterDao().getById(reciterId)
        val tasbihPending = if (reciter == null || reciter.voiceScope.allowsTasbih()) {
            db.reciterAudioDao().getPendingDownloadsForReciter(reciterId)
        } else {
            emptyList()
        }
        val azkarPending = if (reciter == null || reciter.voiceScope.allowsAzkar()) {
            db.reciterAzkarAudioDao().getPendingDownloadsForReciter(reciterId)
        } else {
            emptyList()
        }
        downloadReciterAudio(context, db, tasbihPending) +
            downloadAzkarReciterAudio(context, db, azkarPending)
    }

    private suspend fun downloadDhikrFiles(context: Context, db: AdhkarDatabase): Int {
        val downloader = AudioDownloadManager(context)
        var count = 0
        db.dhikrDao().getEnabledList()
            .filter { it.audioSourceType == AudioSourceType.DOWNLOAD && !it.isDownloaded }
            .forEach { dhikr ->
                val url = dhikr.remoteAudioUrl ?: return@forEach
                val path = downloader.download(url, "dhikr_${dhikr.id}.mp3")
                if (path != null) {
                    db.dhikrDao().updateDownloadState(dhikr.id, true, path)
                    count++
                }
            }
        return count
    }

    private suspend fun downloadReciterAudio(
        context: Context,
        db: AdhkarDatabase,
        items: List<ReciterAudioEntity>,
    ): Int {
        val downloader = AudioDownloadManager(context)
        var count = 0
        items.forEach { audio ->
            val url = audio.remoteUrl ?: return@forEach
            val path = downloader.download(url, "reciter_${audio.id}.mp3")
            if (path != null) {
                db.reciterAudioDao().markDownloaded(audio.id, path)
                count++
            }
        }
        return count
    }

    private suspend fun downloadAzkarReciterAudio(
        context: Context,
        db: AdhkarDatabase,
        items: List<ReciterAzkarAudioEntity>,
    ): Int {
        val downloader = AudioDownloadManager(context)
        var count = 0
        items.forEach { audio ->
            val url = audio.remoteUrl ?: return@forEach
            val path = downloader.download(url, "azkar_reciter_${audio.id}.mp3")
            if (path != null) {
                db.reciterAzkarAudioDao().markDownloaded(audio.id, path)
                count++
            }
        }
        return count
    }

    suspend fun downloadAdhan(context: Context, audioId: Long, remoteUrl: String): String? =
        withContext(Dispatchers.IO) {
            val path = AudioDownloadManager(context).download(remoteUrl, "adhan_catalog_${audioId}.mp3")
            if (path != null) {
                AdhkarDatabase.get(context).adhanAudioDao().markDownloaded(audioId, path)
            }
            path
        }
}
