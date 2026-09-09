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
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.util.RuntimePermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object VehicleActivityScheduler {
    private const val REQUEST = 8102
    const val ACTION_TRANSITION = "com.greendome.adhkar.VEHICLE_TRANSITION"
    const val COOLDOWN_MS = AutoAzkarTriggers.Riding.COOLDOWN_MS

    fun register(context: Context) {
        val app = context.applicationContext
        unregister(app)
        val settings = SettingsRepository(app)
        if (!shouldMonitor(settings)) {
            settings.setRidingMonitor(AutoAzkarMonitorStatus.OFF)
            return
        }
        if (!RuntimePermissions.hasActivityRecognition(app)) {
            settings.setRidingMonitor(AutoAzkarMonitorStatus.NO_ACTIVITY)
            return
        }
        settings.setRidingMonitor(AutoAzkarMonitorStatus.WAITING)
        val request = ActivityTransitionRequest(
            listOf(
                transition(DetectedActivity.IN_VEHICLE, ActivityTransition.ACTIVITY_TRANSITION_ENTER),
                transition(DetectedActivity.IN_VEHICLE, ActivityTransition.ACTIVITY_TRANSITION_EXIT),
            )
        )
        runCatching {
            ActivityRecognition.getClient(app)
                .requestActivityTransitionUpdates(request, pending(app))
                .addOnSuccessListener {
                    settings.setRidingMonitor(AutoAzkarMonitorStatus.OK)
                }
                .addOnFailureListener { error ->
                    settings.setRidingMonitor(
                        AutoAzkarMonitorStatus.FAILED,
                        error.message.orEmpty(),
                    )
                }
        }.onFailure { error ->
            settings.setRidingMonitor(
                AutoAzkarMonitorStatus.FAILED,
                error.message.orEmpty(),
            )
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
                            AutoAzkarEventPlayer.playRiding(context.applicationContext)
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
}
