package com.greendome.adhkar.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.greendome.adhkar.MainActivity
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
        val last = if (event == HomeAzkar.Event.ENTER) settings.homeLastEnterAt else settings.homeLastExitAt
        if (!ignoreCooldown && !AutoAzkarTriggers.Home.canPlay(now, last)) return false
        val items = withContext(Dispatchers.IO) {
            AdhkarDatabase.get(app).azkarItemDao().getByCollection(HomeAzkar.COLLECTION_ID)
        }
        val picked = HomeAzkar.pickRandom(items, event, settings.homeEventKeys(event)) ?: return false
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
        if (AutoAzkarTriggers.Riding.tripIsStale(now, settings.ridingLastPlayAt, settings.ridingInTrip)) {
            settings.ridingInTrip = false
        }
        if (!ignoreCooldown &&
            !AutoAzkarTriggers.Riding.canPlay(now, settings.ridingLastPlayAt, settings.ridingInTrip)
        ) {
            return false
        }
        val items = withContext(Dispatchers.IO) {
            AdhkarDatabase.get(app).azkarItemDao().getByCollection(RidingAzkar.COLLECTION_ID)
        }
        val picked = RidingAzkar.pickRandom(items, settings.ridingItemKeys()) ?: return false
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
            if (home) settings.homeLastPlayError = "" else settings.ridingLastPlayError = ""
            return true
        }
        val err = started.exceptionOrNull()?.javaClass?.simpleName.orEmpty()
        if (home) settings.homeLastPlayError = err else settings.ridingLastPlayError = err
        notifyFallback(context, settings, collectionId, item)
        return false
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
        val open = PendingIntent.getActivity(
            context,
            item.id.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
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
}
