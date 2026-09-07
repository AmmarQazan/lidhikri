package com.greendome.adhkar.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
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
    private const val GEOFENCE_ID = "home_azkar"
    const val ACTION_PROXIMITY = "com.greendome.adhkar.HOME_PROXIMITY"
    const val COOLDOWN_MS = 3 * 60_000L

    fun register(context: Context) {
        val app = context.applicationContext
        val settings = SettingsRepository(app)
        unregister(app)
        if (!shouldMonitor(settings) || !DeviceLocation.hasFinePermission(app)) return
        val location = settings.homeLocation() ?: return
        if (!DeviceLocation.isEnabled(app)) {
            registerProximity(app, location.latitude, location.longitude)
            return
        }
        registerGeofence(app, location.latitude, location.longitude)
    }

    fun unregister(context: Context) {
        val app = context.applicationContext
        runCatching {
            app.getSystemService(LocationManager::class.java)
                ?.removeProximityAlert(pending(app))
        }
        runCatching {
            LocationServices.getGeofencingClient(app).removeGeofences(pending(app))
        }
        runCatching {
            LocationServices.getGeofencingClient(app).removeGeofences(listOf(GEOFENCE_ID))
        }
    }

    fun shouldMonitor(settings: SettingsRepository): Boolean =
        settings.homeAzkarEnabled && settings.hasHomeLocation

    @SuppressLint("MissingPermission")
    private fun registerGeofence(context: Context, lat: Double, lng: Double) {
        val geofence = Geofence.Builder()
            .setRequestId(GEOFENCE_ID)
            .setCircularRegion(lat, lng, HomeAzkar.DEFAULT_RADIUS_METERS)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
            )
            .build()
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()
        val pending = pending(context)
        runCatching {
            LocationServices.getGeofencingClient(context)
                .addGeofences(request, pending)
                .addOnFailureListener {
                    registerProximity(context, lat, lng)
                }
        }.onFailure {
            registerProximity(context, lat, lng)
        }
    }

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
        val event = parseEvent(intent) ?: return
        val pending = goAsync()
        scope.launch {
            try {
                play(context.applicationContext, event)
            } finally {
                pending.finish()
            }
        }
    }

    private fun parseEvent(intent: Intent): HomeAzkar.Event? {
        val geo = runCatching { GeofencingEvent.fromIntent(intent) }.getOrNull()
        if (geo != null && !geo.hasError()) {
            return when (geo.geofenceTransition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> HomeAzkar.Event.ENTER
                Geofence.GEOFENCE_TRANSITION_EXIT -> HomeAzkar.Event.EXIT
                else -> null
            }
        }
        if (!intent.hasExtra(LocationManager.KEY_PROXIMITY_ENTERING)) return null
        return if (intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false)) {
            HomeAzkar.Event.ENTER
        } else {
            HomeAzkar.Event.EXIT
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

class LocationProvidersReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != LocationManager.MODE_CHANGED_ACTION &&
            action != LocationManager.PROVIDERS_CHANGED_ACTION
        ) {
            return
        }
        HomeGeofenceScheduler.register(context)
        VehicleActivityScheduler.register(context)
    }
}
