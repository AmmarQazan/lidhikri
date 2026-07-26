package com.greendome.adhkar

import android.app.Application
import android.content.Context
import com.greendome.adhkar.data.CollectionRepository
import com.greendome.adhkar.data.DhikrRepository
import com.greendome.adhkar.data.ReciterRepository
import com.greendome.adhkar.data.SeedData
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.service.OfflineDownloadHelper
import com.greendome.adhkar.service.SilentNotificationChannels
import com.greendome.adhkar.sync.RemoteContentSync
import com.greendome.adhkar.sync.RemoteSyncResult
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.util.LocaleHelper
import com.greendome.adhkar.widget.DhikrOfDayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AdhkarApplication : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    lateinit var database: AdhkarDatabase
        private set
    lateinit var settings: SettingsRepository
        private set
    lateinit var dhikrRepo: DhikrRepository
        private set
    lateinit var reciterRepo: ReciterRepository
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrapWithSavedLanguage(base))
    }

    override fun onCreate() {
        super.onCreate()
        database = AdhkarDatabase.get(this)
        settings = SettingsRepository(this)
        dhikrRepo = DhikrRepository(database)
        reciterRepo = ReciterRepository(database)
        SilentNotificationChannels.ensureCreated(this)
        SilentNotificationChannels.cancelDhikrAlerts(this)
        appScope.launch {
            val syncResult = RemoteContentSync.syncIfNeeded(this@AdhkarApplication, settings, database)
            if (syncResult is RemoteSyncResult.Updated) {
                settings.seedVersion = maxOf(settings.seedVersion, 15)
            }
            SeedData(database, settings).seedIfEmpty()
            if (syncResult is RemoteSyncResult.Updated) {
                OfflineDownloadHelper.downloadAllPending(this@AdhkarApplication)
            }
            CollectionRepository(database, this@AdhkarApplication).rescheduleAllAlarms()
            DhikrOfDayManager.refresh(this@AdhkarApplication)
        }
    }
}
