package com.greendome.adhkar.service

import android.content.Context
import com.greendome.adhkar.audio.AudioDownloadManager
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.model.AudioSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object OfflineDownloadHelper {
    suspend fun downloadAllPending(context: Context): Int = withContext(Dispatchers.IO) {
        val db = AdhkarDatabase.get(context)
        val downloader = AudioDownloadManager(context)
        var count = 0

        val dhikrs = db.dhikrDao().getEnabledList()
        dhikrs.filter { it.audioSourceType == AudioSourceType.DOWNLOAD && !it.isDownloaded }.forEach { dhikr ->
            val url = dhikr.remoteAudioUrl ?: return@forEach
            val path = downloader.download(url, "dhikr_${dhikr.id}.mp3")
            if (path != null) {
                db.dhikrDao().updateDownloadState(dhikr.id, true, path)
                count++
            }
        }

        db.reciterAudioDao().getPendingDownloads().forEach { audio ->
            val url = audio.remoteUrl ?: return@forEach
            val path = downloader.download(url, "reciter_${audio.id}.mp3")
            if (path != null) {
                db.reciterAudioDao().markDownloaded(audio.id, path)
                count++
            }
        }

        db.reciterAzkarAudioDao().getPendingDownloads().forEach { audio ->
            val url = audio.remoteUrl ?: return@forEach
            val path = downloader.download(url, "azkar_reciter_${audio.id}.mp3")
            if (path != null) {
                db.reciterAzkarAudioDao().markDownloaded(audio.id, path)
                count++
            }
        }
        count
    }
}
