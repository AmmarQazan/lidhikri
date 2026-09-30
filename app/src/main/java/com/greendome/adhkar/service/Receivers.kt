package com.greendome.adhkar.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.greendome.adhkar.data.CollectionRepository
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.prayer.PrayerRespectGate
import com.greendome.adhkar.widget.DhikrOfDayManager
import com.greendome.adhkar.widget.PrayerTimesWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val settings = SettingsRepository(context)
        if (!settings.isServiceEnabled) return
        if (ReminderScheduler.isOutsideTasbihWindow(context)) {
            ReminderScheduler.scheduleNext(context)
            return
        }
        if (PrayerRespectGate.isQuiet(context) || PrayerRespectGate.collidesWithAdhan(context) ||
            AdhanPlaybackService.isPlaying()
        ) {
            ReminderScheduler.scheduleNext(context)
            AdhkarReminderService.refreshNotification(context)
            return
        }
        SilentNotificationChannels.cancelTransientReminderAlerts(context)
        val serviceIntent = Intent(context, AdhkarReminderService::class.java).apply {
            action = AdhkarReminderService.ACTION_TRIGGER
        }
        val delivered = if (ServiceRunningHelper.isRunning(context, AdhkarReminderService::class.java)) {
            ForegroundServiceStarts.deliver(context, serviceIntent)
        } else {
            ForegroundServiceStarts.start(context, serviceIntent)
        }
        if (!delivered) {
            ReminderScheduler.scheduleNext(context)
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val settings = SettingsRepository(context)
        if (settings.isServiceEnabled) {
            ReminderScheduler.scheduleNext(context)
            ForegroundServiceStarts.start(
                context,
                Intent(context, AdhkarReminderService::class.java).apply {
                    action = AdhkarReminderService.ACTION_START
                }
            )
        }
        AfterPrayerAlarmScheduler.reschedule(context)
        AdhanAlarmScheduler.reschedule(context)
        PrayerPhoneSilent.sync(context)
        HomeGeofenceScheduler.register(context)
        VehicleActivityScheduler.register(context)
        DhikrOfDayManager.refreshAsync(context)
        PrayerTimesWidgetManager.updateAll(context)
        NextAzkarNotifier.sync(context)
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                CollectionRepository(AdhkarDatabase.get(app), app).rescheduleAllAlarms()
            } finally {
                pending.finish()
            }
        }
    }
}
