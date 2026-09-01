package com.greendome.adhkar.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.greendome.adhkar.data.RidingAzkar
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.util.RuntimePermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object VehicleActivityScheduler {
    private const val REQUEST = 8102
    const val ACTION_TRANSITION = "com.greendome.adhkar.VEHICLE_TRANSITION"
    const val COOLDOWN_MS = 15 * 60_000L

    fun register(context: Context) {
        val app = context.applicationContext
        unregister(app)
        val settings = SettingsRepository(app)
        if (!shouldMonitor(settings) || !RuntimePermissions.hasActivityRecognition(app)) return
        val request = ActivityTransitionRequest(
            listOf(
                transition(DetectedActivity.IN_VEHICLE, ActivityTransition.ACTIVITY_TRANSITION_ENTER),
                transition(DetectedActivity.IN_VEHICLE, ActivityTransition.ACTIVITY_TRANSITION_EXIT),
            )
        )
        runCatching {
            ActivityRecognition.getClient(app)
                .requestActivityTransitionUpdates(request, pending(app))
        }
    }

    fun unregister(context: Context) {
        val app = context.applicationContext
        runCatching {
            ActivityRecognition.getClient(app).removeActivityTransitionUpdates(pending(app))
        }
    }

    fun shouldMonitor(settings: SettingsRepository): Boolean =
        settings.ridingAzkarEnabled

    private fun transition(activity: Int, type: Int) =
        ActivityTransition.Builder()
            .setActivityType(activity)
            .setActivityTransition(type)
            .build()

    private fun pending(context: Context): PendingIntent {
        val intent = Intent(context, VehicleActivityReceiver::class.java).apply {
            action = ACTION_TRANSITION
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

class VehicleActivityReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || !ActivityTransitionResult.hasResult(intent)) return
        val result = ActivityTransitionResult.extractResult(intent) ?: return
        val pending = goAsync()
        scope.launch {
            try {
                val settings = SettingsRepository(context.applicationContext)
                result.transitionEvents.forEach { event ->
                    if (event.activityType != DetectedActivity.IN_VEHICLE) return@forEach
                    when (event.transitionType) {
                        ActivityTransition.ACTIVITY_TRANSITION_ENTER -> {
                            play(context.applicationContext, settings)
                        }
                        ActivityTransition.ACTIVITY_TRANSITION_EXIT -> {
                            settings.ridingInTrip = false
                        }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun play(context: Context, settings: SettingsRepository) {
        if (!VehicleActivityScheduler.shouldMonitor(settings)) return
        if (settings.ridingInTrip) return
        val now = System.currentTimeMillis()
        if (now - settings.ridingLastPlayAt < VehicleActivityScheduler.COOLDOWN_MS) return
        val items = withContext(Dispatchers.IO) {
            AdhkarDatabase.get(context).azkarItemDao().getByCollection(RidingAzkar.COLLECTION_ID)
        }
        val picked = RidingAzkar.pickRandom(items, settings.ridingItemKeys()) ?: return
        settings.ridingInTrip = true
        settings.ridingLastPlayAt = now
        val play = Intent(context, AzkarCollectionPlayService::class.java).apply {
            putExtra(AzkarCollectionPlayService.EXTRA_COLLECTION_ID, RidingAzkar.COLLECTION_ID)
            putExtra(AzkarCollectionPlayService.EXTRA_ITEM_ID, picked.id)
            putExtra(AzkarCollectionPlayService.EXTRA_FORCE_PLAY, true)
        }
        runCatching { context.startForegroundService(play) }
    }
}
