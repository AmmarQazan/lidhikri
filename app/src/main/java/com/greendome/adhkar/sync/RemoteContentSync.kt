package com.greendome.adhkar.sync

import android.content.Context
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.service.OfflineDownloadHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object RemoteContentSync {
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun syncIfNeeded(
        context: Context,
        settings: SettingsRepository,
        database: AdhkarDatabase
    ): RemoteSyncResult = withContext(Dispatchers.IO) {
        runCatching {
            val manifestBody = fetchText(RemoteContentConfig.manifestUrl())
                ?: return@withContext RemoteSyncResult.Failed("تعذّر جلب ملف المزامنة")
            val manifest = RemoteContentManifest.parse(JSONObject(manifestBody))
            if (manifest.version <= settings.remoteContentVersion) {
                return@withContext RemoteSyncResult.UpToDate
            }

            val contentUrl = RemoteContentConfig.resolveUrl(manifest.contentPath)
            val contentBody = fetchText(contentUrl)
                ?: return@withContext RemoteSyncResult.Failed("تعذّر جلب حزمة المحتوى")
            if (sha256Hex(contentBody) != manifest.sha256.lowercase()) {
                return@withContext RemoteSyncResult.Failed("فشل التحقق من سلامة الحزمة")
            }

            val bundle = RemoteContentBundle.parse(JSONObject(contentBody))
            if (bundle.version != manifest.version) {
                return@withContext RemoteSyncResult.Failed("إصدار الحزمة غير متطابق")
            }

            RemoteContentApplier(database).apply(bundle)
            settings.remoteContentVersion = manifest.version
            settings.lastRemoteSyncAt = System.currentTimeMillis()
            settings.lastRemoteSyncError = null
            OfflineDownloadHelper.downloadAllPending(context)
            RemoteSyncResult.Updated(manifest.version)
        }.getOrElse { error ->
            val message = error.message ?: "خطأ غير معروف"
            settings.lastRemoteSyncError = message
            RemoteSyncResult.Failed(message)
        }
    }

    private fun fetchText(url: String): String? {
        val request = Request.Builder().url(url).get().build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()
        }
    }

    private fun sha256Hex(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(text.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
