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
        val triggerIndex = intent.getIntExtra(EXTRA_TRIGGER_INDEX, 0)
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, 0L)
        val inheritGroup = intent.getBooleanExtra(EXTRA_INHERIT, false)
        val skipQuiet = intent.getBooleanExtra(EXTRA_SKIP_QUIET, false)
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
                val items = db.azkarItemDao().getByCollection(collectionId)

                val prayerBasedAfterPrayer = collectionId == PrayerRespectGate.AFTER_PRAYER_COLLECTION_ID &&
                    settings.prayerConfig().enabled &&
                    settings.afterPrayerFromSalahEnabled
                if (prayerBasedAfterPrayer && !postponed) {
                    CollectionAlarmScheduler.schedule(context, collection, items)
                    return@launch
                }

                if (!postponed && !skipQuiet && PrayerRespectGate.isQuiet(context)) {
                    val resumeAt = (PrayerQuietWindows.nextQuietEndAfter(settings.prayerConfig())
                        ?: System.currentTimeMillis()) + 2 * 60_000L
                    CollectionAlarmScheduler.schedulePostponed(
                        context,
                        collectionId,
                        resumeAt,
                        triggerIndex,
                        itemId,
                        inheritGroup,
                        skipQuiet,
                    )
                    CollectionAlarmScheduler.schedule(context, collection, items)
                    return@launch
                }

                val playIntent = Intent(context, AzkarCollectionPlayService::class.java).apply {
                    putExtra(AzkarCollectionPlayService.EXTRA_COLLECTION_ID, collectionId)
                    putExtra(AzkarCollectionPlayService.EXTRA_ITEM_ID, itemId)
                    putExtra(AzkarCollectionPlayService.EXTRA_INHERIT, inheritGroup)
                }
                context.startForegroundService(playIntent)

                if (!postponed) {
                    CollectionAlarmScheduler.schedule(context, collection, items)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_COLLECTION_ID = "collection_id"
        const val EXTRA_POSTPONED = "postponed"
        const val EXTRA_TRIGGER_INDEX = "trigger_index"
        const val EXTRA_ITEM_ID = "item_id"
        const val EXTRA_INHERIT = "inherit_group"
        const val EXTRA_SKIP_QUIET = "skip_quiet"
    }
}
