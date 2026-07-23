package com.greendome.adhkar.audio

import android.content.Context
import com.greendome.adhkar.data.IslambookAzkarSeed
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.AzkarItemEntity

object AzkarPlaybackResolver {
    suspend fun resolvePlayable(context: Context, item: AzkarItemEntity): PlayableAudio? {
        val settings = SettingsRepository(context)
        val db = AdhkarDatabase.get(context)
        val reciterId = settings.selectedReciterId
        val audioItemId = item.sourceItemId ?: item.id

        db.reciterAzkarAudioDao().get(audioItemId, reciterId)
            ?.let { resolveReciterAzkarAudioEntity(it) }
            ?.let { return it }

        if (reciterId != IslambookAzkarSeed.QURANTIME_RECITER_ID) {
            db.reciterAzkarAudioDao().get(audioItemId, IslambookAzkarSeed.QURANTIME_RECITER_ID)
                ?.let { resolveReciterAzkarAudioEntity(it) }
                ?.let { return it }
        }

        val normalizedText = item.textAr.trim()
        val matchingDhikr = db.dhikrDao().getDefaults().find { it.textAr.trim() == normalizedText }
        if (matchingDhikr != null) {
            db.reciterAudioDao().get(matchingDhikr.id, reciterId)
                ?.let { resolveReciterAudioEntity(it) }
                ?.let { return it }
        }
        return null
    }
}
