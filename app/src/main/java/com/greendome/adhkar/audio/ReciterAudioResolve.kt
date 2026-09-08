package com.greendome.adhkar.audio

import com.greendome.adhkar.data.local.ReciterAudioEntity
import com.greendome.adhkar.data.local.ReciterAzkarAudioEntity
import java.io.File

fun resolveReciterAudioEntity(audio: ReciterAudioEntity): PlayableAudio? {
    usableAudioFile(audio.localPath)?.let { return PlayableAudio.File(it) }
    audio.assetPath?.takeIf { it.isNotBlank() }?.let { return PlayableAudio.Asset(normalizeAssetPath(it)) }
    audio.remoteUrl?.trim()?.takeIf { it.isNotBlank() }?.let { return PlayableAudio.File(it) }
    return null
}

fun resolveReciterAzkarAudioEntity(audio: ReciterAzkarAudioEntity): PlayableAudio? {
    usableAudioFile(audio.localPath)?.let { return PlayableAudio.File(it) }
    audio.assetPath?.takeIf { it.isNotBlank() }?.let { return PlayableAudio.Asset(normalizeAssetPath(it)) }
    audio.remoteUrl?.trim()?.takeIf { it.isNotBlank() }?.let { return PlayableAudio.File(it) }
    return null
}

internal fun usableAudioFile(path: String?): String? {
    if (path.isNullOrBlank()) return null
    if (isHttpPlaybackUri(path)) return null
    val file = File(path)
    if (!file.isFile || file.length() < 16L) return null
    if (!hasAudioMagic(file)) return null
    return path
}

internal fun normalizeAssetPath(path: String): String {
    val stripped = path.replace('\\', '/').trim().removePrefix("asset:///").removePrefix("audio/")
    return "audio/$stripped"
}

internal fun hasAudioMagic(file: File): Boolean {
    val header = ByteArray(12)
    val read = runCatching { file.inputStream().use { it.read(header) } }.getOrDefault(-1)
    if (read < 3) return false
    // ID3
    if (header[0] == 'I'.code.toByte() && header[1] == 'D'.code.toByte() && header[2] == '3'.code.toByte()) {
        return true
    }
    // MPEG frame sync
    if (header[0] == 0xFF.toByte() && (header[1].toInt() and 0xE0) == 0xE0) return true
    // ftyp (m4a / mp4)
    if (read >= 8 &&
        header[4] == 'f'.code.toByte() &&
        header[5] == 't'.code.toByte() &&
        header[6] == 'y'.code.toByte() &&
        header[7] == 'p'.code.toByte()
    ) {
        return true
    }
    // RIFF / WAVE
    if (header[0] == 'R'.code.toByte() &&
        header[1] == 'I'.code.toByte() &&
        header[2] == 'F'.code.toByte() &&
        header[3] == 'F'.code.toByte()
    ) {
        return true
    }
    return false
}
