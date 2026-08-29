package com.greendome.adhkar.audio

import com.greendome.adhkar.data.local.ReciterAudioEntity
import com.greendome.adhkar.data.local.ReciterAzkarAudioEntity
import java.io.File

fun resolveReciterAudioEntity(audio: ReciterAudioEntity): PlayableAudio? {
    usableLocalFile(audio.localPath)?.let { return PlayableAudio.File(it) }
    audio.remoteUrl?.takeIf { it.isNotBlank() }?.let { return PlayableAudio.File(it) }
    audio.assetPath?.takeIf { it.isNotBlank() }?.let { return PlayableAudio.Asset(normalizeAssetPath(it)) }
    return null
}

fun resolveReciterAzkarAudioEntity(audio: ReciterAzkarAudioEntity): PlayableAudio? {
    usableLocalFile(audio.localPath)?.let { return PlayableAudio.File(it) }
    audio.remoteUrl?.takeIf { it.isNotBlank() }?.let { return PlayableAudio.File(it) }
    audio.assetPath?.takeIf { it.isNotBlank() }?.let { return PlayableAudio.Asset(normalizeAssetPath(it)) }
    return null
}

private fun usableLocalFile(path: String?): String? {
    if (path.isNullOrBlank()) return null
    val file = File(path)
    return path.takeIf { file.isFile && file.length() > 0L }
}

private fun normalizeAssetPath(path: String): String {
    val stripped = path.removePrefix("audio/")
    return "audio/$stripped"
}
