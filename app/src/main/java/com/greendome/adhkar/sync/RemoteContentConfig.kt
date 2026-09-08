package com.greendome.adhkar.sync

import com.greendome.adhkar.BuildConfig
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object RemoteContentConfig {
    const val STORAGE_ROOT = "content"
    val storageBucket: String = BuildConfig.FIREBASE_STORAGE_BUCKET

    /** للتحقق من روابط الصوت المستضافة سابقاً على Firebase */
    val firebaseUrlPrefix: String
        get() = "https://firebasestorage.googleapis.com/v0/b/$storageBucket/"

    fun storagePath(relativePath: String): String {
        val path = relativePath.trimStart('/')
        return "$STORAGE_ROOT/$path"
    }

    fun publicDownloadUrl(relativePath: String): String {
        val fullPath = storagePath(relativePath)
        val encoded = URLEncoder.encode(fullPath, StandardCharsets.UTF_8.toString())
            .replace("+", "%20")
        return "https://firebasestorage.googleapis.com/v0/b/$storageBucket/o/$encoded?alt=media"
    }

    fun manifestPublicUrl(): String = publicDownloadUrl("manifest.json")

    fun resolvePublicUrl(relativePath: String): String = publicDownloadUrl(relativePath)
}
