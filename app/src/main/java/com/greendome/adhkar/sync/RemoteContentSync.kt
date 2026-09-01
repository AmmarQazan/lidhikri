package com.greendome.adhkar.sync

import android.content.Context
import com.greendome.adhkar.data.BlessedDaysAzkar
import com.greendome.adhkar.data.BundledAdhanSeed
import com.greendome.adhkar.data.CatalogRecovery
import com.greendome.adhkar.data.FridayAzkar
import com.greendome.adhkar.data.ReciterLibrariesMigration
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.SubaihatReciterSeed
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.prayer.AdhanSoundMode
import com.greendome.adhkar.prayer.PrayerAlertSettings
import com.greendome.adhkar.prayer.PrayerName
import com.greendome.adhkar.service.OfflineDownloadHelper
import com.greendome.adhkar.service.PrayerAlarms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.MessageDigest

object RemoteContentSync {
    suspend fun peekRemoteVersion(): Int? = withContext(Dispatchers.IO) {
        runCatching {
            val manifestBody = FirebaseContentStorage.downloadText("manifest.json")
            RemoteContentManifest.parse(JSONObject(manifestBody)).version
        }.getOrNull()
    }

    suspend fun syncIfNeeded(
        context: Context,
        settings: SettingsRepository,
        database: AdhkarDatabase,
        force: Boolean = false
    ): RemoteSyncResult = withContext(Dispatchers.IO) {
        runCatching {
            runCatching { PrayerDefaultsSync.syncIfNeeded(context, settings) }
            val manifestBody = runCatching {
                FirebaseContentStorage.downloadText("manifest.json")
            }.getOrElse { error ->
                return@withContext RemoteSyncResult.Failed(syncFailureMessage(error, "تعذّر جلب ملف المزامنة"))
            }
            val manifest = RemoteContentManifest.parse(JSONObject(manifestBody))
            if (CatalogRecovery.shouldSkipRemoteApply(
                    force = force,
                    remoteSha256 = manifest.sha256,
                    appliedSha256 = settings.remoteContentSha256
                )
            ) {
                settings.remoteContentVersion = maxOf(settings.remoteContentVersion, manifest.version)
                return@withContext RemoteSyncResult.UpToDate(manifest.version)
            }

            val contentBody = runCatching {
                FirebaseContentStorage.downloadText(manifest.contentPath)
            }.getOrElse { error ->
                return@withContext RemoteSyncResult.Failed(syncFailureMessage(error, "تعذّر جلب حزمة المحتوى"))
            }
            if (sha256Hex(contentBody) != manifest.sha256.lowercase()) {
                return@withContext RemoteSyncResult.Failed("فشل التحقق من سلامة الحزمة")
            }

            val bundle = RemoteContentBundle.parse(JSONObject(contentBody))
            if (bundle.version != manifest.version) {
                return@withContext RemoteSyncResult.Failed("إصدار الحزمة غير متطابق")
            }

            RemoteContentApplier(database).apply(bundle)
            applyOfficialAdhanDefaults(settings, bundle)
            PrayerAlarms.rescheduleAll(context)
            reconcileAutoTasbihDhikrState(database)
            FridayAzkar.ensure(database)
            BlessedDaysAzkar.ensure(database)
            SubaihatReciterSeed.ensure(database)
            ReciterLibrariesMigration.enforceDefaultBuiltinReciter(database)
            settings.remoteContentVersion = manifest.version
            settings.remoteContentSha256 = manifest.sha256.lowercase()
            settings.lastRemoteSyncAt = System.currentTimeMillis()
            settings.lastRemoteSyncError = null
            OfflineDownloadHelper.downloadAllPending(context)
            RemoteSyncResult.Updated(manifest.version)
        }.getOrElse { error ->
            val message = syncFailureMessage(error, "خطأ غير معروف")
            settings.lastRemoteSyncError = message
            RemoteSyncResult.Failed(message)
        }
    }

    internal fun sha256Hex(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(text.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    internal fun applyOfficialAdhanDefaults(
        settings: SettingsRepository,
        bundle: RemoteContentBundle,
    ) {
        val ids = buildSet {
            for (i in 0 until bundle.adhanAudio.length()) {
                add(bundle.adhanAudio.getJSONObject(i).optLong("id"))
            }
        }
        val asrId = BundledAdhanSeed.ASR_DEFAULT_ID
        if (asrId !in ids) return
        val current = settings.adhanAlert(PrayerName.ASR)
        if (!shouldBindOfficialAsrAdhan(current)) return
        settings.setAdhanAlert(
            PrayerName.ASR,
            current.copy(
                soundMode = AdhanSoundMode.CATALOG,
                catalogId = asrId,
            )
        )
    }

    internal fun shouldBindOfficialAsrAdhan(current: PrayerAlertSettings): Boolean {
        if (current.soundMode == AdhanSoundMode.SILENT ||
            current.soundMode == AdhanSoundMode.SHORT ||
            current.soundMode == AdhanSoundMode.CUSTOM ||
            current.soundMode == AdhanSoundMode.RECORDED
        ) {
            return false
        }
        val id = current.catalogId
        return id <= 0L ||
            id == BundledAdhanSeed.DEFAULT_ID ||
            id == BundledAdhanSeed.ASR_DEFAULT_ID
    }

    internal suspend fun reconcileAutoTasbihDhikrState(database: AdhkarDatabase) {
        database.dhikrDao().markKnownLongFormDhikr()
        database.dhikrDao().disableJawamiAutoTasbih()
        database.dhikrDao().disableLongFormAutoTasbih()
    }

    private fun syncFailureMessage(error: Throwable, fallback: String): String {
        val raw = error.message.orEmpty()
        return when {
            raw.contains("Permission", ignoreCase = true) ||
                raw.contains("403") -> "تعذّر قراءة المحتوى من السحابة. تحقّق من صلاحيات التخزين."
            raw.contains("404") || raw.contains("Object does not exist", ignoreCase = true) ->
                "ملف المحتوى غير موجود على السحابة."
            raw.isBlank() -> fallback
            else -> raw
        }
    }
}
