package com.greendome.adhkar.audio

import android.content.Context
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.DhikrEntity
import com.greendome.adhkar.data.model.AudioSourceType

object DhikrPlaybackResolver {
    suspend fun resolvePlayable(context: Context, dhikr: DhikrEntity): PlayableAudio? {
        val settings = SettingsRepository(context)
        val db = AdhkarDatabase.get(context)
        val reciterId = settings.selectedReciterId

        db.reciterAudioDao().get(dhikr.id, reciterId)
            ?.let { resolveReciterAudioEntity(it) }
            ?.let { return it }

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
    onComplete: () -> Unit = {}
) {
    when (playable) {
        is PlayableAudio.Asset -> playAsset(playable.path, settings, onComplete)
        is PlayableAudio.File -> play(playable.path, settings, onComplete)
    }
}

fun DhikrAudioPlayer.playSequence(
    items: List<PlayableAudio>,
    settings: SettingsRepository,
    onComplete: () -> Unit = {}
) {
    if (items.isEmpty()) {
        onComplete()
        return
    }
    val queue = items.toMutableList()
    fun playNext() {
        val next = queue.removeFirstOrNull() ?: run {
            onComplete()
            return
        }
        playResolved(next, settings) { playNext() }
    }
    playNext()
}
