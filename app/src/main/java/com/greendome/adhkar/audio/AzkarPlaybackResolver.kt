package com.greendome.adhkar.audio

import android.content.Context
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.model.VoiceSettingsTarget
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.AzkarItemEntity

object AzkarPlaybackResolver {
    suspend fun resolvePlayable(context: Context, item: AzkarItemEntity): PlayableAudio? {
        val settings = SettingsRepository(context)
        val db = AdhkarDatabase.get(context)
        val reciterId = settings.selectedReciterIdFor(VoiceSettingsTarget.AZKAR)
        val reciter = db.reciterDao().getById(reciterId)
        if (reciter != null && !reciter.voiceScope.allows(VoiceSettingsTarget.AZKAR)) {
            return null
        }

        lookupAzkarAudio(db, item, reciterId)?.let { return it }

        val normalizedText = item.textAr.trim()
        val matchingDhikr = db.dhikrDao().getDefaults().find {
            it.textAr.trim() == normalizedText && !it.isExcludedFromAutoAudio()
        }
        if (matchingDhikr != null) {
            db.reciterAudioDao().get(matchingDhikr.id, reciterId)
                ?.let { resolveReciterAudioEntity(it) }
                ?.let { return it }
        }
        return null
    }

    private suspend fun lookupAzkarAudio(
        db: AdhkarDatabase,
        item: AzkarItemEntity,
        reciterId: Long
    ): PlayableAudio? {
        val ids = listOfNotNull(item.id, item.sourceItemId).distinct()
        for (id in ids) {
            db.reciterAzkarAudioDao().get(id, reciterId)
                ?.let { resolveReciterAzkarAudioEntity(it) }
                ?.let { return it }
        }
        return null
    }
}
