package com.greendome.adhkar.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.widget.DhikrOfDayManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val settings = SettingsRepository(context)
        if (!settings.isServiceEnabled) return
        if (ReminderScheduler.isInSleepWindow(context)) {
            ReminderScheduler.scheduleNext(context)
            return
        }
        SilentNotificationChannels.cancelDhikrAlerts(context)
        val serviceIntent = Intent(context, AdhkarReminderService::class.java).apply {
            action = AdhkarReminderService.ACTION_TRIGGER
        }
        if (ServiceRunningHelper.isRunning(context, AdhkarReminderService::class.java)) {
            context.startService(serviceIntent)
        } else {
            context.startForegroundService(serviceIntent)
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val settings = SettingsRepository(context)
        if (settings.isServiceEnabled) {
            ReminderScheduler.scheduleNext(context)
            context.startForegroundService(
                Intent(context, AdhkarReminderService::class.java).apply {
                    action = AdhkarReminderService.ACTION_START
                }
            )
        }
        DhikrOfDayManager.refreshAsync(context)
    }
}
