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
import com.greendome.adhkar.prayer.DeviceLocation
import com.greendome.adhkar.util.RuntimePermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object HomeGeofenceScheduler {
    private const val REQUEST = 8101
    private const val GEOFENCE_ID = "home_azkar"
    const val ACTION_PROXIMITY = "com.greendome.adhkar.HOME_PROXIMITY"
    const val COOLDOWN_MS = AutoAzkarTriggers.Home.COOLDOWN_MS

    fun register(context: Context) {
        val app = context.applicationContext
        val settings = SettingsRepository(app)
        unregister(app)
        if (!shouldMonitor(settings)) {
            settings.homeNeedsBackgroundPermission = false
            settings.setHomeMonitor(AutoAzkarMonitorStatus.OFF)
            return
        }
        if (!DeviceLocation.hasFinePermission(app)) {
            settings.setHomeMonitor(AutoAzkarMonitorStatus.NO_FINE)
            return
        }
        val location = settings.homeLocation() ?: run {
            settings.setHomeMonitor(AutoAzkarMonitorStatus.NO_PLACE)
            return
        }
        settings.homeNeedsBackgroundPermission =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                !RuntimePermissions.hasBackgroundLocation(app)
        settings.setHomeMonitor(AutoAzkarMonitorStatus.WAITING)
        if (!DeviceLocation.isEnabled(app)) {
            registerProximity(app, location.latitude, location.longitude)
            settings.setHomeMonitor(AutoAzkarMonitorStatus.PROXIMITY)
            return
        }
        registerGeofence(app, settings, location.latitude, location.longitude)
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
    private fun registerGeofence(
        context: Context,
        settings: SettingsRepository,
        lat: Double,
        lng: Double,
    ) {
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
                .addOnSuccessListener {
                    settings.setHomeMonitor(AutoAzkarMonitorStatus.GEOFENCE)
                }
                .addOnFailureListener { error ->
                    registerProximity(context, lat, lng)
                    settings.setHomeMonitor(
                        AutoAzkarMonitorStatus.PROXIMITY,
                        error.message.orEmpty(),
                    )
                }
        }.onFailure { error ->
            registerProximity(context, lat, lng)
            settings.setHomeMonitor(
                AutoAzkarMonitorStatus.PROXIMITY,
                error.message.orEmpty(),
            )
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
                AutoAzkarEventPlayer.playHome(context.applicationContext, event)
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
