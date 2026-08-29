package com.greendome.adhkar.audio

import android.content.Context
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.DhikrEntity
import com.greendome.adhkar.data.model.AudioSourceType
import com.greendome.adhkar.data.model.VoiceSettingsTarget

object DhikrPlaybackResolver {
    suspend fun resolvePlayable(context: Context, dhikr: DhikrEntity): PlayableAudio? {
        val settings = SettingsRepository(context)
        val db = AdhkarDatabase.get(context)
        val reciterId = settings.selectedReciterId
        val reciter = db.reciterDao().getById(reciterId)
        if (reciter == null || reciter.voiceScope.allows(VoiceSettingsTarget.TASBIH)) {
            db.reciterAudioDao().get(dhikr.id, reciterId)
                ?.let { resolveReciterAudioEntity(it) }
                ?.let { return it }
        }

        if (!dhikr.isDefault) {
            when (dhikr.audioSourceType) {
                AudioSourceType.RECORDED, AudioSourceType.FILE, AudioSourceType.DOWNLOAD -> {
                    dhikr.audioPath?.let { return PlayableAudio.File(it) }
                }
                else -> Unit
            }
        }
        return null
    }
}

sealed class PlayableAudio {
    data class Asset(val path: String) : PlayableAudio()
    data class File(val path: String) : PlayableAudio()
}

fun DhikrAudioPlayer.playResolved(
    playable: PlayableAudio,
    settings: SettingsRepository,
    voiceProfile: VoiceSettingsTarget = VoiceSettingsTarget.TASBIH,
    onComplete: () -> Unit = {}
) {
    when (playable) {
        is PlayableAudio.Asset -> playAsset(playable.path, settings, voiceProfile, onComplete)
        is PlayableAudio.File -> play(playable.path, settings, voiceProfile, onComplete)
    }
}

