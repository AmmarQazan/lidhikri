package com.greendome.adhkar.sync

import android.content.Context
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.prayer.PrayerCountryDefaults
import com.greendome.adhkar.prayer.PrayerDefaultsTable
import com.greendome.adhkar.service.AdhkarReminderService
import com.greendome.adhkar.service.AfterPrayerAlarmScheduler
import com.greendome.adhkar.service.NextAdhanService
import com.greendome.adhkar.service.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class PrayerDefaultsSyncResult {
    data class UpToDate(val version: Int) : PrayerDefaultsSyncResult()
    data class Updated(val version: Int) : PrayerDefaultsSyncResult()
    data class Failed(val reason: String) : PrayerDefaultsSyncResult()
}

object PrayerDefaultsSync {
    suspend fun syncIfNeeded(
        context: Context,
        settings: SettingsRepository,
        force: Boolean = false
    ): PrayerDefaultsSyncResult = withContext(Dispatchers.IO) {
        if (!force && settings.prayerDefaultsDirty) {
            return@withContext PrayerDefaultsSyncResult.UpToDate(settings.prayerDefaultsVersion)
        }
        val body = runCatching {
            FirebaseContentStorage.downloadText(PrayerDefaultsTable.STORAGE_PATH)
        }.getOrElse { error ->
            val message = error.message.orEmpty()
            return@withContext if (isMissingRemote(message)) {
                PrayerDefaultsSyncResult.UpToDate(settings.prayerDefaultsVersion)
            } else {
                PrayerDefaultsSyncResult.Failed(message.ifBlank { "تعذّر جلب جداول الصلاة" })
            }
        }
        val table = runCatching { PrayerDefaultsTable.parse(body) }.getOrElse {
            return@withContext PrayerDefaultsSyncResult.Failed("ملف جداول الصلاة غير صالح")
        }
        if (!force && table.version <= settings.prayerDefaultsVersion) {
            if (settings.prayerDefaultsJson.isBlank() && table.countries.isNotEmpty()) {
                applyTable(context, settings, table, dirty = false)
                return@withContext PrayerDefaultsSyncResult.Updated(table.version)
            }
            PrayerCountryDefaults.applyRemote(table)
            return@withContext PrayerDefaultsSyncResult.UpToDate(table.version)
        }
        applyTable(context, settings, table, dirty = false)
        PrayerDefaultsSyncResult.Updated(table.version)
    }

    private fun applyTable(
        context: Context,
        settings: SettingsRepository,
        table: PrayerDefaultsTable,
        dirty: Boolean
    ) {
        PrayerCountryDefaults.applyRemote(table)
        settings.prayerDefaultsJson = table.toJson()
        settings.prayerDefaultsVersion = table.version
        settings.prayerDefaultsDirty = dirty
        runCatching {
            AfterPrayerAlarmScheduler.reschedule(context)
            if (settings.isServiceEnabled) {
                ReminderScheduler.scheduleNext(context)
                AdhkarReminderService.refreshNotification(context)
            }
            NextAdhanService.sync(context)
        }
    }

    private fun isMissingRemote(message: String): Boolean {
        val lower = message.lowercase()
        return "object does not exist" in lower ||
            "not found" in lower ||
            "404" in message ||
            "not_found" in lower
    }
}
