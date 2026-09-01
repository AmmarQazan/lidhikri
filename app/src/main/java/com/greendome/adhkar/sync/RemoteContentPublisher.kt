package com.greendome.adhkar.sync

import android.content.Context
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AdhkarDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object RemoteContentPublisher {
    sealed class PublishResult {
        data class Success(val version: Int, val audioUploaded: Int) : PublishResult()
        data class Failed(val reason: String) : PublishResult()
    }

    suspend fun publish(
        context: Context,
        settings: SettingsRepository,
        database: AdhkarDatabase,
        onProgress: (String) -> Unit
    ): PublishResult = withContext(Dispatchers.IO) {
        runCatching {
            val email = settings.firebaseAdminEmail?.trim().orEmpty()
            val password = settings.firebaseAdminPassword?.trim().orEmpty()
            if (email.isBlank() || password.isBlank()) {
                return@withContext PublishResult.Failed("أدخل بريد وكلمة مرور Firebase للمدير")
            }

            onProgress("جاري تسجيل الدخول…")
            FirebaseContentStorage.ensureAdminSignedIn(email, password)

            onProgress("جاري قراءة الإصدار الحالي…")
            val remoteVersion = runCatching {
                val body = FirebaseContentStorage.downloadText("manifest.json")
                JSONObject(body).optInt("version", 0)
            }.getOrDefault(0)
            val newVersion = maxOf(remoteVersion, settings.remoteContentVersion) + 1

            val pendingRepo = PendingPublishRepository(database)
            val pending = pendingRepo.snapshot()

            onProgress("جاري تجهيز الحزمة…")
            val exported = RemoteContentExporter(context, database).export(newVersion, pending)

            exported.audioFiles.forEachIndexed { index, upload ->
                onProgress("رفع ملفات الصوت ${index + 1}/${exported.audioFiles.size}…")
                FirebaseContentStorage.upload(
                    relativePath = upload.storagePath,
                    bytes = upload.file.readBytes(),
                    contentType = "audio/mpeg"
                )
                writeBackRemoteUrl(database, upload)
            }

            onProgress("رفع حزمة المحتوى v$newVersion…")
            FirebaseContentStorage.upload(
                relativePath = "bundles/v$newVersion/content.json",
                bytes = exported.contentJson.toByteArray(Charsets.UTF_8),
                contentType = "application/json"
            )

            onProgress("تحديث ملف المزامنة…")
            FirebaseContentStorage.upload(
                relativePath = "manifest.json",
                bytes = exported.manifestJson.toByteArray(Charsets.UTF_8),
                contentType = "application/json"
            )

            pendingRepo.clear()
            settings.remoteContentVersion = newVersion
            settings.remoteContentSha256 = JSONObject(exported.manifestJson).optString("sha256").lowercase()
            settings.lastRemoteSyncAt = System.currentTimeMillis()
            settings.lastRemoteSyncError = null
            PublishResult.Success(newVersion, exported.audioFiles.size)
        }.getOrElse { error ->
            val message = error.message ?: "فشل الرفع"
            settings.lastRemoteSyncError = message
            PublishResult.Failed(message)
        }
    }

    private suspend fun writeBackRemoteUrl(
        database: AdhkarDatabase,
        upload: RemoteContentExporter.AudioUpload
    ) {
        val url = RemoteContentConfig.publicDownloadUrl(upload.storagePath)
        when (upload.source) {
            AudioUploadKind.DHIKR -> database.dhikrDao().updateRemoteAudioUrl(upload.sourceId, url)
            AudioUploadKind.RECITER_AUDIO -> database.reciterAudioDao().updateRemoteUrl(upload.sourceId, url)
            AudioUploadKind.RECITER_AZKAR_AUDIO ->
                database.reciterAzkarAudioDao().updateRemoteUrl(upload.sourceId, url)
            AudioUploadKind.ADHAN_AUDIO -> database.adhanAudioDao().updateRemoteUrl(upload.sourceId, url)
        }
    }
}
