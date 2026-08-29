package com.greendome.adhkar.sync

import android.content.Context
import com.greendome.adhkar.data.AzkarFavorites
import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.data.local.DhikrEntity
import com.greendome.adhkar.data.local.ReciterAudioEntity
import com.greendome.adhkar.data.local.ReciterAzkarAudioEntity
import com.greendome.adhkar.data.local.ReciterEntity
import com.greendome.adhkar.data.model.AudioSourceType
import com.greendome.adhkar.data.local.dhikrEnabledForAutoTasbihImport
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant

class RemoteContentExporter(
    private val context: Context,
    private val db: AdhkarDatabase
) {
    data class AudioUpload(
        val storagePath: String,
        val file: File,
        val source: AudioUploadKind,
        val sourceId: Long
    )

    data class ExportResult(
        val version: Int,
        val contentJson: String,
        val manifestJson: String,
        val audioFiles: List<AudioUpload>
    )

    suspend fun export(
        version: Int,
        pending: PendingPublishSet = PendingPublishSet()
    ): ExportResult {
        val audioUploads = linkedMapOf<String, AudioUpload>()

        val dhikr = db.dhikrDao().getDefaults()
        val dhikrIds = dhikr.map { it.id }.toSet()
        val reciters = db.reciterDao().getAll()
        val collections = db.collectionDao().getAll()
            .filter { it.id != AzkarFavorites.COLLECTION_ID }
        val azkarItems = db.azkarItemDao().getAll()
            .filter { it.collectionId != AzkarFavorites.COLLECTION_ID }
        val azkarIds = azkarItems.map { it.id }.toSet()
        val reciterAudio = db.reciterAudioDao().getAll()
            .filter { it.dhikrId in dhikrIds }
        val reciterAzkarAudio = db.reciterAzkarAudioDao().getAll()
            .filter { it.azkarItemId in azkarIds }

        val generatedAt = Instant.now().toString()
        val bundle = JSONObject().apply {
            put("version", version)
            put("generatedAt", generatedAt)
            put("dhikr", JSONArray(dhikr.map { it.toExportJson(audioUploads, pending) }))
            put("reciters", JSONArray(reciters.map { it.toExportJson() }))
            put("reciterAudio", JSONArray(reciterAudio.map { it.toExportJson(audioUploads, pending) }))
            put("reciterAzkarAudio", JSONArray(reciterAzkarAudio.map { it.toExportJson(audioUploads, pending) }))
            put("collections", JSONArray(collections.map { it.toExportJson() }))
            put("azkarItems", JSONArray(azkarItems.map { it.toExportJson() }))
        }

        val contentJson = bundle.toString()
        val manifest = JSONObject().apply {
            put("version", version)
            put("contentPath", "bundles/v$version/content.json")
            put("sha256", RemoteContentSync.sha256Hex(contentJson))
            put("generatedAt", generatedAt)
            put("minAppVersion", "1.0.5")
        }

        return ExportResult(
            version = version,
            contentJson = contentJson,
            manifestJson = manifest.toString(2) + "\n",
            audioFiles = audioUploads.values.toList()
        )
    }

    private fun DhikrEntity.toExportJson(
        audioUploads: LinkedHashMap<String, AudioUpload>,
        pending: PendingPublishSet
    ): JSONObject = JSONObject().apply {
        put("id", id)
        put("textAr", textAr)
        put("textEn", textEn)
        put("textFr", textFr)
        put("textEs", textEs)
        put("textTr", textTr)
        put("textUr", textUr)
        put("textId", textId)
        put("textHi", textHi)
        put("category", category.name)
        put("repeatCount", repeatCount)
        put("isEnabled", dhikrEnabledForAutoTasbihImport(
            category = category,
            isLongForm = isLongForm,
            isDefault = isDefault,
            sortOrder = sortOrder,
            remoteEnabled = isEnabled,
        ))
        put("isDefault", isDefault)
        put("isLongForm", isLongForm)
        val remoteUrl = resolveRemoteAudioUrl(audioUploads, pending)
        put("audioSourceType", when {
            remoteUrl != null -> AudioSourceType.DOWNLOAD.name
            else -> audioSourceType.name
        })
        put("audioPath", audioPath)
        put("reciterId", reciterId ?: JSONObject.NULL)
        put("remoteAudioUrl", remoteUrl ?: JSONObject.NULL)
        put("isDownloaded", remoteUrl == null && isDownloaded)
        put("displayPopup", displayPopup)
        put("displayNotification", displayNotification)
        put("displayLockScreen", displayLockScreen)
        put("displayAudioOnly", displayAudioOnly)
        put("displayAudioText", displayAudioText)
        put("sortOrder", sortOrder)
        put("scheduleType", scheduleType.name)
        put("timeStartHour", timeStartHour)
        put("timeStartMinute", timeStartMinute)
        put("timeEndHour", timeEndHour)
        put("timeEndMinute", timeEndMinute)
        put("hijriMonth", hijriMonth)
        put("hijriDayStart", hijriDayStart)
        put("hijriDayEnd", hijriDayEnd)
        put("scheduleLabelAr", scheduleLabelAr)
        put("createdAt", createdAt)
    }

    private fun DhikrEntity.resolveRemoteAudioUrl(
        audioUploads: LinkedHashMap<String, AudioUpload>,
        pending: PendingPublishSet
    ): String? {
        remoteAudioUrl?.takeIf { isHostedRemoteUrl(it) }?.let { hosted ->
            if (!pending.shouldUploadDhikrAudio(id)) return hosted
        }
        val asset = audioPath?.takeIf {
            audioSourceType == AudioSourceType.BUILTIN || audioSourceType == AudioSourceType.DOWNLOAD
        }
        if (asset != null && asset.startsWith("audio/")) {
            return assetToRemoteUrl(asset)
        }
        val local = resolveLocalAudioFile(audioPath, null)
            ?: return remoteAudioUrl?.takeIf { isHostedRemoteUrl(it) }
        return queueOrUrl(
            local = local,
            audioUploads = audioUploads,
            uploadIfPending = pending.shouldUploadDhikrAudio(id),
            kind = AudioUploadKind.DHIKR,
            sourceId = id
        )
    }

    private fun ReciterEntity.toExportJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("nameAr", nameAr)
        put("nameEn", nameEn)
        put("nameFr", nameFr)
        put("nameEs", nameEs)
        put("nameTr", nameTr)
        put("nameUr", nameUr)
        put("nameId", nameId)
        put("nameHi", nameHi)
        put("isBuiltin", isBuiltin)
        put("isActive", isActive)
        put("voiceScope", voiceScope.name)
    }

    private fun ReciterAudioEntity.toExportJson(
        audioUploads: LinkedHashMap<String, AudioUpload>,
        pending: PendingPublishSet
    ): JSONObject = JSONObject().apply {
        put("id", id)
        put("reciterId", reciterId)
        put("dhikrId", dhikrId)
        val remote = resolveAudioRemoteUrl(
            assetPath, localPath, remoteUrl, audioUploads,
            pending.shouldUploadReciterAudio(id),
            AudioUploadKind.RECITER_AUDIO,
            id
        )
        put("remoteUrl", remote ?: JSONObject.NULL)
        put("assetPath", JSONObject.NULL)
        put("localPath", JSONObject.NULL)
        put("isDownloaded", false)
    }

    private fun ReciterAzkarAudioEntity.toExportJson(
        audioUploads: LinkedHashMap<String, AudioUpload>,
        pending: PendingPublishSet
    ): JSONObject = JSONObject().apply {
        put("id", id)
        put("reciterId", reciterId)
        put("azkarItemId", azkarItemId)
        val remote = resolveAudioRemoteUrl(
            assetPath, localPath, remoteUrl, audioUploads,
            pending.shouldUploadReciterAzkarAudio(id),
            AudioUploadKind.RECITER_AZKAR_AUDIO,
            id
        )
        put("remoteUrl", remote ?: JSONObject.NULL)
        put("assetPath", JSONObject.NULL)
        put("localPath", JSONObject.NULL)
        put("isDownloaded", false)
    }

    private fun resolveAudioRemoteUrl(
        assetPath: String?,
        localPath: String?,
        remoteUrl: String?,
        audioUploads: LinkedHashMap<String, AudioUpload>,
        uploadIfPending: Boolean,
        kind: AudioUploadKind,
        sourceId: Long
    ): String? {
        val hosted = remoteUrl?.takeIf { isHostedRemoteUrl(it) }
        if (hosted != null && !uploadIfPending) return hosted
        assetPath?.takeIf { it.startsWith("audio/") }?.let { return assetToRemoteUrl(it) }
        val local = resolveLocalAudioFile(localPath, assetPath) ?: return hosted ?: remoteUrl
        return queueOrUrl(local, audioUploads, uploadIfPending, kind, sourceId)
    }

    private fun queueOrUrl(
        local: File,
        audioUploads: LinkedHashMap<String, AudioUpload>,
        uploadIfPending: Boolean,
        kind: AudioUploadKind,
        sourceId: Long
    ): String {
        val rel = audioRepoRelativePath(local)
        val path = "audio/$rel"
        if (uploadIfPending) {
            audioUploads.putIfAbsent(path, AudioUpload(path, local, kind, sourceId))
        }
        return RemoteContentConfig.publicDownloadUrl(path)
    }

    private fun resolveLocalAudioFile(localPath: String?, assetPath: String?): File? {
        localPath?.let {
            val file = File(it)
            if (file.isFile) return file
        }
        assetPath?.let { path ->
            if (path.startsWith("audio/")) {
                runCatching {
                    val temp = File(context.cacheDir, "export-audio/${path.substringAfter("audio/")}")
                    if (temp.isFile) return temp
                    context.assets.open(path).use { input ->
                        temp.parentFile?.mkdirs()
                        temp.outputStream().use { output -> input.copyTo(output) }
                    }
                    return temp
                }
            }
        }
        return null
    }

    private fun audioRepoRelativePath(file: File): String {
        val absolute = file.absolutePath.replace('\\', '/')
        val marker = "/files/"
        val idx = absolute.indexOf(marker)
        return if (idx >= 0) {
            "data/user/0/com.greendome.adhkar/files/${absolute.substring(idx + marker.length)}"
        } else {
            "uploads/${file.name}"
        }
    }

    private fun assetToRemoteUrl(assetPath: String): String {
        val path = assetPath.replace('\\', '/').trimStart('/')
        val rel = if (path.startsWith("audio/")) path else "audio/$path"
        return RemoteContentConfig.publicDownloadUrl(rel)
    }

    private fun isHostedRemoteUrl(url: String): Boolean =
        url.startsWith(RemoteContentConfig.firebaseUrlPrefix)

    private fun AdhkarCollectionEntity.toExportJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("titleAr", titleAr)
        put("titleEn", titleEn)
        put("titleFr", titleFr)
        put("titleEs", titleEs)
        put("titleTr", titleTr)
        put("titleUr", titleUr)
        put("titleId", titleId)
        put("titleHi", titleHi)
        put("sortOrder", sortOrder)
        put("autoPlayAllowed", autoPlayAllowed)
        put("autoPlayEnabled", autoPlayEnabled)
        put("scheduleHour", scheduleHour)
        put("scheduleMinute", scheduleMinute)
        put("weekDaysMask", weekDaysMask)
        put("useTtsAutoPlay", useTtsAutoPlay)
    }

    private fun AzkarItemEntity.toExportJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("collectionId", collectionId)
        put("textAr", textAr)
        put("virtueAr", virtueAr)
        put("repeatCount", repeatCount)
        put("sortOrder", sortOrder)
        put("sourceItemId", sourceItemId ?: JSONObject.NULL)
    }
}
