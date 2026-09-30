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
import kotlinx.coroutines.withContext

class CollectionAlarmReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onReceive(context: Context, intent: Intent?) {
        val collectionId = intent?.getStringExtra(EXTRA_COLLECTION_ID) ?: return
        val postponed = intent.getBooleanExtra(EXTRA_POSTPONED, false)
        val triggerIndex = intent.getIntExtra(EXTRA_TRIGGER_INDEX, 0)
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, 0L)
        val inheritGroup = intent.getBooleanExtra(EXTRA_INHERIT, false)
        val skipQuiet = intent.getBooleanExtra(EXTRA_SKIP_QUIET, false)
        val pending = goAsync()
        val app = context.applicationContext
        scope.launch {
            try {
                val playIntent = withContext(Dispatchers.IO) {
                    try {
                        preparePlay(
                            context = app,
                            collectionId = collectionId,
                            postponed = postponed,
                            triggerIndex = triggerIndex,
                            itemId = itemId,
                            inheritGroup = inheritGroup,
                            skipQuiet = skipQuiet,
                        )
                    } finally {
                        NextAzkarNotifier.sync(app)
                    }
                }
                if (playIntent != null) {
                    ForegroundServiceStarts.start(app, playIntent)
                }
            } catch (_: Exception) {
                // استثناء القاعدة لا يُسقط العملية أثناء معالجة المنبّه
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun preparePlay(
        context: Context,
        collectionId: String,
        postponed: Boolean,
        triggerIndex: Int,
        itemId: Long,
        inheritGroup: Boolean,
        skipQuiet: Boolean,
    ): Intent? {
        val settings = SettingsRepository(context)
        if (!settings.autoAzkarEnabled) {
            CollectionAlarmScheduler.cancel(context, collectionId)
            return null
        }
        val db = AdhkarDatabase.get(context)
        val collection = db.collectionDao().getById(collectionId) ?: return null
        if (!collection.autoPlayAllowed || !collection.autoPlayEnabled) return null
        val items = db.azkarItemDao().getByCollection(collectionId)

        val prayerBasedAfterPrayer = collectionId == PrayerRespectGate.AFTER_PRAYER_COLLECTION_ID &&
            settings.prayerConfig().enabled &&
            settings.afterPrayerFromSalahEnabled
        if (prayerBasedAfterPrayer && !postponed) {
            CollectionAlarmScheduler.schedule(context, collection, items)
            return null
        }

        val now = System.currentTimeMillis()
        val prayerConfig = settings.prayerConfig()
        val adhanPlaying = AdhanPlaybackService.isPlaying()
        val atAdhan = adhanPlaying || PrayerQuietWindows.collidesWithAdhan(prayerConfig, now)
        val inQuiet = !skipQuiet && PrayerRespectGate.isQuiet(context, now)
        if (!postponed && (atAdhan || inQuiet)) {
            val resumeAt = when {
                inQuiet -> (PrayerQuietWindows.nextQuietEndAfter(prayerConfig, now) ?: now) + 2 * 60_000L
                adhanPlaying -> now + 60_000L
                else -> PrayerQuietWindows.delayPastAdhan(prayerConfig, now)
            }
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
            return null
        }

        if (!postponed) {
            CollectionAlarmScheduler.schedule(context, collection, items)
        }
        return Intent(context, AzkarCollectionPlayService::class.java).apply {
            putExtra(AzkarCollectionPlayService.EXTRA_COLLECTION_ID, collectionId)
            putExtra(AzkarCollectionPlayService.EXTRA_ITEM_ID, itemId)
            putExtra(AzkarCollectionPlayService.EXTRA_INHERIT, inheritGroup)
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
