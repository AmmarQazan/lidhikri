package com.greendome.adhkar.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.prayer.PrayerQuietWindows
import com.greendome.adhkar.prayer.PrayerRespectGate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object AfterPrayerAlarmScheduler {
    private const val REQUEST = 7001

    fun reschedule(context: Context) {
        val settings = SettingsRepository(context)
        val config = settings.prayerConfig()
        if (!config.enabled || !config.afterPrayerReminder) {
            cancel(context)
            return
        }
        val triggerAt = PrayerQuietWindows.nextAfterPrayerTriggerAt(config) ?: run {
            cancel(context)
            return
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pending(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pending(context))
    }

    private fun pending(context: Context): PendingIntent {
        val intent = Intent(context, AfterPrayerAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class AfterPrayerAlarmReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        scope.launch {
            try {
                val settings = SettingsRepository(context)
                val config = settings.prayerConfig()
                if (config.enabled && config.afterPrayerReminder) {
                    PrayerPhoneSilent.exit(context)
                    val play = Intent(context, AzkarCollectionPlayService::class.java).apply {
                        putExtra(
                            AzkarCollectionPlayService.EXTRA_COLLECTION_ID,
                            PrayerRespectGate.AFTER_PRAYER_COLLECTION_ID
                        )
                        putExtra(AzkarCollectionPlayService.EXTRA_FORCE_PLAY, true)
                    }
                    context.startForegroundService(play)
                }
                AfterPrayerAlarmScheduler.reschedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}
