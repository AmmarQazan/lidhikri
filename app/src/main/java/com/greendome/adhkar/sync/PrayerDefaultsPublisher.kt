package com.greendome.adhkar.sync

import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.prayer.PrayerCountryDefaults
import com.greendome.adhkar.prayer.PrayerDefaultsTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object PrayerDefaultsPublisher {
    sealed class PublishResult {
        data class Success(val version: Int) : PublishResult()
        data class Failed(val reason: String) : PublishResult()
    }

    suspend fun publish(
        settings: SettingsRepository,
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
                val body = FirebaseContentStorage.downloadText(PrayerDefaultsTable.STORAGE_PATH)
                JSONObject(body).optInt("version", 0)
            }.getOrDefault(0)
            val newVersion = maxOf(remoteVersion, settings.prayerDefaultsVersion) + 1

            onProgress("جاري تجهيز الجداول…")
            val snapshot = PrayerCountryDefaults.resolvedEntries()
            val table = PrayerDefaultsTable(newVersion, snapshot)

            onProgress("رفع جداول الصلاة v$newVersion…")
            FirebaseContentStorage.upload(
                relativePath = PrayerDefaultsTable.STORAGE_PATH,
                bytes = table.toJson().toByteArray(Charsets.UTF_8),
                contentType = "application/json"
            )

            PrayerCountryDefaults.applyRemote(table)
            settings.prayerDefaultsJson = table.toJson()
            settings.prayerDefaultsVersion = newVersion
            settings.prayerDefaultsDirty = false
            PublishResult.Success(newVersion)
        }.getOrElse { error ->
            PublishResult.Failed(error.message ?: "فشل رفع جداول الصلاة")
        }
    }
}
