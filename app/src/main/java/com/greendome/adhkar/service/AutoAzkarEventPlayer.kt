package com.greendome.adhkar.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.greendome.adhkar.data.AutoAzkarCatalog
import com.greendome.adhkar.data.HomeAzkar
import com.greendome.adhkar.data.RidingAzkar
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.AzkarItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AutoAzkarEventPlayer {
    suspend fun playHome(
        context: Context,
        event: HomeAzkar.Event,
        ignoreCooldown: Boolean = false,
        recordEvent: Boolean = true,
    ): Boolean {
        val app = context.applicationContext
        val settings = SettingsRepository(app)
        if (!ignoreCooldown && !HomeGeofenceScheduler.shouldMonitor(settings)) return false
        val now = System.currentTimeMillis()
        if (!ignoreCooldown &&
            event == HomeAzkar.Event.ENTER &&
            AutoAzkarTriggers.Home.isFreshRegister(now, settings.homeMonitorArmedAt)
        ) {
            settings.homeLastSkip = AutoAzkarSkip.FRESH_REGISTER
            return false
        }
        val last = if (event == HomeAzkar.Event.ENTER) settings.homeLastEnterAt else settings.homeLastExitAt
        if (!ignoreCooldown && !AutoAzkarTriggers.Home.canPlay(now, last)) {
            settings.homeLastSkip = AutoAzkarSkip.COOLDOWN
            return false
        }
        val items = withContext(Dispatchers.IO) {
            AdhkarDatabase.get(app).azkarItemDao().getByCollection(HomeAzkar.COLLECTION_ID)
        }
        val picked = HomeAzkar.pickRandom(items, event, settings.homeEventKeys(event))
        if (picked == null) {
            settings.homeLastSkip = AutoAzkarSkip.NO_ITEMS
            return false
        }
        settings.homeLastSkip = ""
        if (recordEvent) {
            if (event == HomeAzkar.Event.ENTER) settings.homeLastEnterAt = now else settings.homeLastExitAt = now
        }
        return startOrNotify(app, settings, HomeAzkar.COLLECTION_ID, picked, home = true)
    }

    suspend fun playRiding(
        context: Context,
        ignoreCooldown: Boolean = false,
        recordEvent: Boolean = true,
    ): Boolean {
        val app = context.applicationContext
        val settings = SettingsRepository(app)
        if (!ignoreCooldown && !VehicleActivityScheduler.shouldMonitor(settings)) return false
        val now = System.currentTimeMillis()
        if (!ignoreCooldown && !AutoAzkarTriggers.Riding.canPlay(now, settings.ridingLastPlayAt)) {
            settings.ridingLastSkip = AutoAzkarSkip.COOLDOWN
            return false
        }
        val items = withContext(Dispatchers.IO) {
            AdhkarDatabase.get(app).azkarItemDao().getByCollection(RidingAzkar.COLLECTION_ID)
        }
        val picked = RidingAzkar.pickRandom(items, settings.ridingItemKeys())
        if (picked == null) {
            settings.ridingLastSkip = AutoAzkarSkip.NO_ITEMS
            return false
        }
        settings.ridingLastSkip = ""
        if (recordEvent) {
            settings.ridingInTrip = true
            settings.ridingLastPlayAt = now
        }
        return startOrNotify(app, settings, RidingAzkar.COLLECTION_ID, picked, home = false)
    }

    private fun startOrNotify(
        context: Context,
        settings: SettingsRepository,
        collectionId: String,
        item: AzkarItemEntity,
        home: Boolean,
    ): Boolean {
        val started = AzkarCollectionPlayService.startForced(context, collectionId, item.id)
        if (started.isSuccess) {
            clearPlayError(settings, home)
            return true
        }
        val err = started.exceptionOrNull()?.javaClass?.simpleName.orEmpty()
        if (home) settings.homeLastPlayError = err else settings.ridingLastPlayError = err
        notifyFallback(context, settings, collectionId, item)
        scheduleRetry(context, collectionId, item.id)
        return false
    }

    private fun clearPlayError(settings: SettingsRepository, home: Boolean) {
        if (home) {
            settings.homeLastPlayError = ""
            settings.homeLastSkip = ""
        } else {
            settings.ridingLastPlayError = ""
            settings.ridingLastSkip = ""
        }
    }

    private fun scheduleRetry(context: Context, collectionId: String, itemId: Long) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = PendingIntent.getBroadcast(
            context,
            retryRequestCode(itemId),
            retryIntent(context, collectionId, itemId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        runCatching {
            alarm.scheduleWakeup(
                System.currentTimeMillis() + RETRY_DELAY_MS,
                pending,
            )
        }
    }

    private fun notifyFallback(
        context: Context,
        settings: SettingsRepository,
        collectionId: String,
        item: AzkarItemEntity,
    ) {
        SilentNotificationChannels.ensureCreated(context)
        if (!com.greendome.adhkar.util.RuntimePermissions.hasPostNotifications(context)) return
        val title = AutoAzkarCatalog.displayTitle(collectionId, settings.appLanguage)
        val text = item.localizedText(settings.appLanguage)
        val play = Intent(context, AzkarCollectionPlayService::class.java).apply {
            putExtra(AzkarCollectionPlayService.EXTRA_COLLECTION_ID, collectionId)
            putExtra(AzkarCollectionPlayService.EXTRA_ITEM_ID, item.id)
            putExtra(AzkarCollectionPlayService.EXTRA_FORCE_PLAY, true)
        }
        val open = PendingIntent.getForegroundService(
            context,
            item.id.toInt(),
            play,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = SilentNotificationChannels.applyAppIcon(
            NotificationCompat.Builder(context, SilentNotificationChannels.DHIKR_OF_DAY),
            context,
        )
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        context.getSystemService(android.app.NotificationManager::class.java)
            .notify(item.id.toInt(), notification)
    }

    private fun retryIntent(context: Context, collectionId: String, itemId: Long) =
        Intent(context, AutoAzkarPlayRetryReceiver::class.java).apply {
            putExtra(AutoAzkarPlayRetryReceiver.EXTRA_COLLECTION_ID, collectionId)
            putExtra(AutoAzkarPlayRetryReceiver.EXTRA_ITEM_ID, itemId)
        }

    private fun retryRequestCode(itemId: Long) = 8300 + (itemId % 400).toInt()

    private const val RETRY_DELAY_MS = 4_000L
}

class AutoAzkarPlayRetryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val collectionId = intent?.getStringExtra(EXTRA_COLLECTION_ID) ?: return
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, 0L)
        if (itemId <= 0L) return
        val app = context.applicationContext
        val started = AzkarCollectionPlayService.startForced(app, collectionId, itemId)
        if (started.isFailure) return
        val settings = SettingsRepository(app)
        if (collectionId == HomeAzkar.COLLECTION_ID) {
            settings.homeLastPlayError = ""
            settings.homeLastSkip = ""
        } else {
            settings.ridingLastPlayError = ""
            settings.ridingLastSkip = ""
        }
        app.getSystemService(android.app.NotificationManager::class.java).cancel(itemId.toInt())
    }

    companion object {
        const val EXTRA_COLLECTION_ID = "retry_collection_id"
        const val EXTRA_ITEM_ID = "retry_item_id"
    }
}
