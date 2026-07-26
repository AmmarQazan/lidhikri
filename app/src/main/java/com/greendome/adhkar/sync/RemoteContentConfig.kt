package com.greendome.adhkar.sync

import com.greendome.adhkar.BuildConfig

object RemoteContentConfig {
    val baseUrl: String = BuildConfig.REMOTE_CONTENT_BASE_URL.trimEnd('/')

    fun manifestUrl(): String = "$baseUrl/manifest.json"

    fun resolveUrl(relativePath: String): String {
        val path = relativePath.trimStart('/')
        return "$baseUrl/$path"
    }
}
