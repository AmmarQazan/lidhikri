package com.greendome.adhkar.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.prayer.AdhanEvent
import com.greendome.adhkar.prayer.AdhanEventKind
import com.greendome.adhkar.prayer.AdhanEvents
import com.greendome.adhkar.prayer.PrayerName
import com.greendome.adhkar.prayer.PrayerTimesCalculator
import com.greendome.adhkar.widget.PrayerTimesWidgetManager

object AdhanAlarmScheduler {
    private const val REQUEST = 7101

    fun reschedule(context: Context, fromMillis: Long = System.currentTimeMillis()) {
        val app = context.applicationContext
        cancel(app)
        val config = SettingsRepository(app).prayerConfig()
        val next = AdhanEvents.next(config, fromMillis, skip = AdhanFiredStore.last(app))
        if (next != null) {
            val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pending = pending(app, next.prayer, next.kind, next.atMillis)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.atMillis, pending)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, next.atMillis, pending)
            }
        }
        PrayerTimesWidgetManager.updateAll(app)
        NextAdhanService.sync(app)
    }

    fun cancel(context: Context) {
        val app = context.applicationContext
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        PrayerName.entries.forEach { prayer ->
            AdhanEventKind.entries.forEach { kind ->
                alarmManager.cancel(pending(app, prayer, kind, atMillis = 0L))
            }
        }
    }

    private fun pending(
        context: Context,
        prayer: PrayerName,
        kind: AdhanEventKind,
        atMillis: Long,
    ): PendingIntent {
        val intent = Intent(context, AdhanAlarmReceiver::class.java).apply {
            putExtra(EXTRA_PRAYER, prayer.name)
            putExtra(EXTRA_KIND, kind.name)
            putExtra(EXTRA_AT_MILLIS, atMillis)
        }
        val request = REQUEST + prayer.ordinal * 10 + kind.ordinal
        return PendingIntent.getBroadcast(
            context,
            request,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    const val EXTRA_PRAYER = "prayer"
    const val EXTRA_KIND = "kind"
    const val EXTRA_AT_MILLIS = "at_millis"
}

class AdhanAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prayer = runCatching {
            PrayerName.valueOf(intent?.getStringExtra(AdhanAlarmScheduler.EXTRA_PRAYER).orEmpty())
        }.getOrNull()
        val kind = runCatching {
            AdhanEventKind.valueOf(intent?.getStringExtra(AdhanAlarmScheduler.EXTRA_KIND).orEmpty())
        }.getOrNull()
        val firedAt = intent?.getLongExtra(AdhanAlarmScheduler.EXTRA_AT_MILLIS, 0L) ?: 0L
        if (prayer != null && kind != null) {
            val event = AdhanEvent(
                prayer,
                kind,
                if (firedAt > 0L) firedAt else System.currentTimeMillis(),
            )
            val zone = PrayerTimesCalculator.zoneId(SettingsRepository(context).prayerConfig())
            if (!AdhanFiredStore.alreadyHandled(context, event, zone)) {
                AdhanFiredStore.remember(context, event)
                when (kind) {
                    AdhanEventKind.ADHAN -> {
                        val play = Intent(context, AdhanPlaybackService::class.java).apply {
                            action = AdhanPlaybackService.ACTION_START
                            putExtra(AdhanAlarmScheduler.EXTRA_PRAYER, prayer.name)
                        }
                        context.startForegroundService(play)
                    }
                    AdhanEventKind.PRE,
                    AdhanEventKind.IQAMA -> AdhanAlertNotifier.show(context, prayer, kind)
                }
            }
        }
        val fromMillis = AdhanPlaybackGuard.rescheduleFromMillis(
            firedAtMillis = firedAt,
            nowMillis = System.currentTimeMillis(),
        )
        AdhanAlarmScheduler.reschedule(context, fromMillis)
    }
}
