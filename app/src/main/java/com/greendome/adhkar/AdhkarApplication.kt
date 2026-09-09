package com.greendome.adhkar

import android.app.Application
import android.content.Context
import com.greendome.adhkar.data.CatalogRecovery
import com.greendome.adhkar.data.CollectionRepository
import com.greendome.adhkar.data.DhikrRepository
import com.greendome.adhkar.data.ReciterRepository
import com.greendome.adhkar.data.SeedData
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.isOfficialCatalogEmpty
import com.greendome.adhkar.service.AdhanAlarmScheduler
import com.greendome.adhkar.service.AdhanAlertNotifier
import com.greendome.adhkar.service.AfterPrayerAlarmScheduler
import com.greendome.adhkar.service.NextAdhanService
import com.greendome.adhkar.service.NextAzkarNotifier
import com.greendome.adhkar.service.OfflineDownloadHelper
import com.greendome.adhkar.service.PrayerPhoneSilent
import com.greendome.adhkar.service.SilentNotificationChannels
import com.greendome.adhkar.prayer.PrayerCountryDefaults
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
        LocaleHelper.applySavedAppLocales(this)
        PrayerCountryDefaults.loadCached(settings.prayerDefaultsJson)
        dhikrRepo = DhikrRepository(database)
        reciterRepo = ReciterRepository(database)
        SilentNotificationChannels.ensureCreated(this)
        AdhanAlertNotifier.ensureChannel(this)
        SilentNotificationChannels.cancelLegacyAlertIds(this)
        NextAdhanService.sync(this)
        NextAzkarNotifier.sync(this)
        appScope.launch {
            val catalogEmpty = database.isOfficialCatalogEmpty()
            SeedData(database, settings).seedIfEmpty()
            val forceRemote = CatalogRecovery.shouldForceRemoteSync(catalogEmpty)
            val syncResult = RemoteContentSync.syncIfNeeded(
                this@AdhkarApplication,
                settings,
                database,
                force = forceRemote
            )
            if (syncResult is RemoteSyncResult.Updated) {
                settings.seedVersion = maxOf(settings.seedVersion, 19)
                SeedData(database, settings).seedIfEmpty()
                OfflineDownloadHelper.downloadAllPending(this@AdhkarApplication)
            }
            val collections = CollectionRepository(database, this@AdhkarApplication)
            collections.initializeTasbihWindowIfNeeded()
            collections.rescheduleAllAlarms()
            AfterPrayerAlarmScheduler.reschedule(this@AdhkarApplication)
            AdhanAlarmScheduler.reschedule(this@AdhkarApplication)
            PrayerPhoneSilent.reschedule(this@AdhkarApplication)
            DhikrOfDayManager.refresh(this@AdhkarApplication)
        }
    }
}
