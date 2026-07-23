package com.greendome.adhkar.audio

import com.greendome.adhkar.data.local.ReciterAudioEntity
import com.greendome.adhkar.data.local.ReciterAzkarAudioEntity

fun resolveReciterAudioEntity(audio: ReciterAudioEntity): PlayableAudio? {
    audio.localPath?.let { return PlayableAudio.File(it) }
    audio.remoteUrl?.let { return PlayableAudio.File(it) }
    audio.assetPath?.let { return PlayableAudio.Asset(normalizeAssetPath(it)) }
    return null
}

fun resolveReciterAzkarAudioEntity(audio: ReciterAzkarAudioEntity): PlayableAudio? {
    audio.localPath?.let { return PlayableAudio.File(it) }
    audio.remoteUrl?.let { return PlayableAudio.File(it) }
    audio.assetPath?.let { return PlayableAudio.Asset(normalizeAssetPath(it)) }
    return null
}

private fun normalizeAssetPath(path: String): String {
    val stripped = path.removePrefix("audio/")
    return "audio/$stripped"
}
