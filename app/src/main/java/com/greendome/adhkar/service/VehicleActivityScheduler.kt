package com.greendome.adhkar.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionResult
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
    private const val SNAPSHOT_REQUEST = 8103
    private const val SNAPSHOT_CANCEL_REQUEST = 8104
    private const val SNAPSHOT_WINDOW_MS = 60_000L
    private const val VEHICLE_CONFIDENCE = 75
    const val ACTION_TRANSITION = "com.greendome.adhkar.VEHICLE_TRANSITION"
    const val ACTION_SNAPSHOT = "com.greendome.adhkar.VEHICLE_SNAPSHOT"
    const val ACTION_SNAPSHOT_CANCEL = "com.greendome.adhkar.VEHICLE_SNAPSHOT_CANCEL"
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
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (!shouldMonitor(SettingsRepository(app))) return@postDelayed
                        requestCurrentVehicleSample(app)
                    }, 1_500L)
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
        stopSnapshot(app)
    }

    fun stopSnapshot(context: Context) {
        val app = context.applicationContext
        runCatching {
            ActivityRecognition.getClient(app).removeActivityUpdates(snapshotPending(app))
        }
        runCatching {
            app.getSystemService(AlarmManager::class.java)?.cancel(snapshotCancelPending(app))
        }
    }

    fun isConfidentVehicle(intent: Intent): Boolean {
        val result = ActivityRecognitionResult.extractResult(intent) ?: return false
        val top = result.mostProbableActivity
        return top.type == DetectedActivity.IN_VEHICLE && top.confidence >= VEHICLE_CONFIDENCE
    }

    private fun requestCurrentVehicleSample(context: Context) {
        val app = context.applicationContext
        if (!shouldMonitor(SettingsRepository(app))) return
        if (!RuntimePermissions.hasActivityRecognition(app)) return
        runCatching {
            ActivityRecognition.getClient(app)
                .requestActivityUpdates(5_000L, snapshotPending(app))
        }
        runCatching {
            app.getSystemService(AlarmManager::class.java)?.scheduleWakeup(
                System.currentTimeMillis() + SNAPSHOT_WINDOW_MS,
                snapshotCancelPending(app),
            )
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

    private fun snapshotPending(context: Context): PendingIntent {
        val intent = Intent(context, VehicleActivityReceiver::class.java).apply {
            action = ACTION_SNAPSHOT
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
        return PendingIntent.getBroadcast(context, SNAPSHOT_REQUEST, intent, flags)
    }

    private fun snapshotCancelPending(context: Context): PendingIntent {
        val intent = Intent(context, VehicleActivityReceiver::class.java).apply {
            action = ACTION_SNAPSHOT_CANCEL
        }
        return PendingIntent.getBroadcast(
            context,
            SNAPSHOT_CANCEL_REQUEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

class VehicleActivityReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            VehicleActivityScheduler.ACTION_SNAPSHOT_CANCEL -> {
                VehicleActivityScheduler.stopSnapshot(context)
                return
            }
            VehicleActivityScheduler.ACTION_SNAPSHOT -> {
                val inVehicle = VehicleActivityScheduler.isConfidentVehicle(intent)
                VehicleActivityScheduler.stopSnapshot(context)
                if (!inVehicle) return
                val pending = goAsync()
                scope.launch {
                    try {
                        AutoAzkarEventPlayer.playRiding(context.applicationContext)
                    } finally {
                        pending.finish()
                    }
                }
                return
            }
        }
        if (!ActivityTransitionResult.hasResult(intent)) return
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
