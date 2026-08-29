package com.greendome.adhkar.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.prayer.PrayerQuietWindows
import com.greendome.adhkar.prayer.PrayerRespectGate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CollectionAlarmReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        val collectionId = intent?.getStringExtra(EXTRA_COLLECTION_ID) ?: return
        val postponed = intent.getBooleanExtra(EXTRA_POSTPONED, false)
        val pending = goAsync()
        scope.launch {
            try {
                val settings = SettingsRepository(context)
                if (!settings.autoAzkarEnabled) {
                    CollectionAlarmScheduler.cancel(context, collectionId)
                    return@launch
                }
                val db = AdhkarDatabase.get(context)
                val collection = db.collectionDao().getById(collectionId) ?: return@launch
                if (!collection.autoPlayAllowed || !collection.autoPlayEnabled) return@launch

                val prayerBasedAfterPrayer = collectionId == PrayerRespectGate.AFTER_PRAYER_COLLECTION_ID &&
                    settings.prayerConfig().enabled &&
                    settings.afterPrayerFromSalahEnabled
                if (prayerBasedAfterPrayer && !postponed) {
                    CollectionAlarmScheduler.schedule(context, collection)
                    return@launch
                }

                if (!postponed && PrayerRespectGate.isQuiet(context)) {
                    val resumeAt = (PrayerQuietWindows.nextQuietEndAfter(settings.prayerConfig())
                        ?: System.currentTimeMillis()) + 2 * 60_000L
                    CollectionAlarmScheduler.schedulePostponed(context, collectionId, resumeAt)
                    CollectionAlarmScheduler.schedule(context, collection)
                    return@launch
                }

                val playIntent = Intent(context, AzkarCollectionPlayService::class.java).apply {
                    putExtra(AzkarCollectionPlayService.EXTRA_COLLECTION_ID, collectionId)
                }
                context.startForegroundService(playIntent)

                if (!postponed) {
                    CollectionAlarmScheduler.schedule(context, collection)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_COLLECTION_ID = "collection_id"
        const val EXTRA_POSTPONED = "postponed"
    }
}
