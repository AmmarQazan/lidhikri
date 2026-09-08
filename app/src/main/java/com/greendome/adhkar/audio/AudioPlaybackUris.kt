package com.greendome.adhkar.audio

import android.content.Context
import android.net.Uri
import java.io.File

internal fun playbackUri(pathOrUri: String): Uri {
    val value = pathOrUri.trim()
    val lower = value.lowercase()
    return when {
        lower.startsWith("content:") ||
            lower.startsWith("file:") ||
            lower.startsWith("asset:") ||
            lower.startsWith("http://") ||
            lower.startsWith("https://") -> Uri.parse(value)
        else -> Uri.fromFile(File(value))
    }
}

internal fun cachedAssetFile(context: Context, assetPath: String): File? {
    val normalized = normalizeAssetPath(assetPath)
    val dest = File(context.filesDir, "voice_assets/$normalized")
    if (dest.isFile && dest.length() > 16L) return dest
    return runCatching {
        dest.parentFile?.mkdirs()
        context.assets.open(normalized).use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        dest.takeIf { it.isFile && it.length() > 16L }
    }.getOrNull()
}

internal fun isHttpPlaybackUri(pathOrUri: String): Boolean {
    val lower = pathOrUri.trim().lowercase()
    return lower.startsWith("http://") || lower.startsWith("https://")
}
