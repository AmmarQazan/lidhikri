package com.greendome.adhkar.audio

import android.content.Context
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.SubaihatReciterSeed
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.DhikrEntity
import com.greendome.adhkar.data.model.AudioSourceType
import com.greendome.adhkar.data.model.MediaRespectPart
import com.greendome.adhkar.data.model.VoiceSettingsTarget

object DhikrPlaybackResolver {
    suspend fun resolvePlayable(context: Context, dhikr: DhikrEntity): PlayableAudio? {
        val settings = SettingsRepository(context)
        val db = AdhkarDatabase.get(context)
        val reciterId = settings.selectedReciterIdFor(VoiceSettingsTarget.TASBIH)
        val reciter = db.reciterDao().getById(reciterId)
        val fromSelected = if (reciter == null || reciter.voiceScope.allows(VoiceSettingsTarget.TASBIH)) {
            db.reciterAudioDao().get(dhikr.id, reciterId)?.let { resolveReciterAudioEntity(it) }
        } else {
            null
        }
        preferLocalOrBundled(fromSelected, bundledPlayableForDhikr(dhikr))?.let { return it }

        if (!dhikr.isDefault) {
            when (dhikr.audioSourceType) {
                AudioSourceType.RECORDED, AudioSourceType.FILE, AudioSourceType.DOWNLOAD -> {
                    dhikr.audioPath?.let { return PlayableAudio.File(it) }
                }
                else -> Unit
            }
        }

        if (reciterId != SubaihatReciterSeed.RECITER_ID) {
            val fromDefault = db.reciterAudioDao().get(dhikr.id, SubaihatReciterSeed.RECITER_ID)
                ?.let { resolveReciterAudioEntity(it) }
            preferLocalOrBundled(fromDefault, bundledPlayableForDhikr(dhikr))?.let { return it }
        }
        return bundledPlayableForDhikr(dhikr)
    }
}

internal fun bundledPlayableForDhikr(dhikr: DhikrEntity): PlayableAudio? {
    SubaihatReciterSeed.bundledDhikrAsset(dhikr.category, dhikr.sortOrder, dhikr.textAr)
        ?.let { return PlayableAudio.Asset(normalizeAssetPath(it)) }
    val path = usableAssetPath(dhikr.audioPath)
    if (path != null) {
        return PlayableAudio.Asset(normalizeAssetPath(path))
    }
    return null
}

internal fun bundledPlayableForAzkar(collectionId: String, textAr: String): PlayableAudio? {
    val file = SubaihatReciterSeed.matchedAzkarFile(collectionId, textAr) ?: return null
    return PlayableAudio.Asset(normalizeAssetPath(SubaihatReciterSeed.bundledAssetPath(file)))
}

internal fun preferLocalOrBundled(
    resolved: PlayableAudio?,
    bundled: PlayableAudio?,
): PlayableAudio? {
    when (resolved) {
        is PlayableAudio.Asset -> return resolved
        is PlayableAudio.File -> if (!isHttpPlaybackUri(resolved.path)) return resolved
        null -> Unit
    }
    return bundled ?: resolved
}

sealed class PlayableAudio {
    data class Asset(val path: String) : PlayableAudio()
    data class File(val path: String) : PlayableAudio()
}

fun DhikrAudioPlayer.playResolved(
    playable: PlayableAudio,
    settings: SettingsRepository,
    voiceProfile: VoiceSettingsTarget = VoiceSettingsTarget.TASBIH,
    mediaPart: MediaRespectPart? = null,
    onComplete: () -> Unit = {}
) {
    when (playable) {
        is PlayableAudio.Asset -> playAsset(playable.path, settings, voiceProfile, mediaPart, onComplete)
        is PlayableAudio.File -> play(playable.path, settings, voiceProfile, mediaPart, onComplete)
    }
}

