package com.greendome.adhkar.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import com.greendome.adhkar.data.HomeAzkar
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.prayer.DeviceLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object HomeGeofenceScheduler {
    private const val REQUEST = 8101
    const val ACTION_PROXIMITY = "com.greendome.adhkar.HOME_PROXIMITY"
    const val COOLDOWN_MS = 3 * 60_000L

    fun register(context: Context) {
        val app = context.applicationContext
        val settings = SettingsRepository(app)
        unregister(app)
        if (!shouldMonitor(settings) || !DeviceLocation.hasFinePermission(app)) return
        val location = settings.homeLocation() ?: return
        registerProximity(app, location.latitude, location.longitude)
    }

    fun unregister(context: Context) {
        val app = context.applicationContext
        runCatching {
            app.getSystemService(LocationManager::class.java)
                ?.removeProximityAlert(pending(app))
        }
    }

    fun shouldMonitor(settings: SettingsRepository): Boolean =
        settings.homeAzkarEnabled && settings.hasHomeLocation

    @SuppressLint("MissingPermission")
    private fun registerProximity(context: Context, lat: Double, lng: Double) {
        val manager = context.getSystemService(LocationManager::class.java) ?: return
        runCatching {
            manager.addProximityAlert(
                lat,
                lng,
                HomeAzkar.DEFAULT_RADIUS_METERS,
                -1L,
                pending(context)
            )
        }
    }

    private fun pending(context: Context): PendingIntent {
        val intent = Intent(context, HomeGeofenceReceiver::class.java).apply {
            action = ACTION_PROXIMITY
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
        return PendingIntent.getBroadcast(context, REQUEST, intent, flags)
    }
}

class HomeGeofenceReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        if (!intent.hasExtra(LocationManager.KEY_PROXIMITY_ENTERING)) return
        val event = if (intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false)) {
            HomeAzkar.Event.ENTER
        } else {
            HomeAzkar.Event.EXIT
        }
        val pending = goAsync()
        scope.launch {
            try {
                play(context.applicationContext, event)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun play(context: Context, event: HomeAzkar.Event) {
        val settings = SettingsRepository(context)
        if (!HomeGeofenceScheduler.shouldMonitor(settings)) return
        val now = System.currentTimeMillis()
        val last = if (event == HomeAzkar.Event.ENTER) settings.homeLastEnterAt else settings.homeLastExitAt
        if (now - last < HomeGeofenceScheduler.COOLDOWN_MS) return
        val items = withContext(Dispatchers.IO) {
            AdhkarDatabase.get(context).azkarItemDao().getByCollection(HomeAzkar.COLLECTION_ID)
        }
        val picked = HomeAzkar.pickRandom(items, event, settings.homeEventKeys(event)) ?: return
        if (event == HomeAzkar.Event.ENTER) settings.homeLastEnterAt = now else settings.homeLastExitAt = now
        val play = Intent(context, AzkarCollectionPlayService::class.java).apply {
            putExtra(AzkarCollectionPlayService.EXTRA_COLLECTION_ID, HomeAzkar.COLLECTION_ID)
            putExtra(AzkarCollectionPlayService.EXTRA_ITEM_ID, picked.id)
            putExtra(AzkarCollectionPlayService.EXTRA_FORCE_PLAY, true)
        }
        runCatching { context.startForegroundService(play) }
    }
}
