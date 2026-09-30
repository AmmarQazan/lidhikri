package com.greendome.adhkar.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
            alarmManager.scheduleWakeup(next.atMillis, pending)
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
    const val EXTRA_RETRY = "playback_retry"

    /** محاولة واحدة لا يمسحها إعادة جدولة السلسلة. */
    fun schedulePlaybackRetry(context: Context, event: AdhanEvent) {
        val app = context.applicationContext
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.scheduleWakeup(
            System.currentTimeMillis() + PLAYBACK_RETRY_DELAY_MS,
            retryPending(app, event),
        )
    }

    fun cancelPlaybackRetry(context: Context) {
        val app = context.applicationContext
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(
            retryPending(app, AdhanEvent(PrayerName.FAJR, AdhanEventKind.ADHAN, 0L)),
        )
    }

    private fun retryPending(context: Context, event: AdhanEvent): PendingIntent {
        val intent = Intent(context, AdhanAlarmReceiver::class.java).apply {
            putExtra(EXTRA_PRAYER, event.prayer.name)
            putExtra(EXTRA_KIND, event.kind.name)
            putExtra(EXTRA_AT_MILLIS, event.atMillis)
            putExtra(EXTRA_RETRY, true)
        }
        return PendingIntent.getBroadcast(
            context,
            RETRY_REQUEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private const val RETRY_REQUEST = 7191
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
        val isRetry = intent?.getBooleanExtra(AdhanAlarmScheduler.EXTRA_RETRY, false) == true
        var retryEvent: AdhanEvent? = null
        try {
            if (prayer != null && kind != null) {
                val event = AdhanEvent(
                    prayer,
                    kind,
                    if (firedAt > 0L) firedAt else System.currentTimeMillis(),
                )
                val zone = PrayerTimesCalculator.zoneId(SettingsRepository(context).prayerConfig())
                if (!AdhanFiredStore.alreadyHandled(context, event, zone)) {
                    when (kind) {
                        AdhanEventKind.ADHAN -> {
                            val play = Intent(context, AdhanPlaybackService::class.java).apply {
                                action = AdhanPlaybackService.ACTION_START
                                putExtra(AdhanAlarmScheduler.EXTRA_PRAYER, prayer.name)
                            }
                            val started = ForegroundServiceStarts.start(context, play)
                            when (AdhanPlaybackGuard.missedPlayback(started, isRetry)) {
                                MissedPlayback.NONE -> AdhanFiredStore.remember(context, event)
                                MissedPlayback.RETRY_ONCE -> retryEvent = event
                                MissedPlayback.GIVE_UP -> AdhanFiredStore.remember(context, event)
                            }
                        }
                        AdhanEventKind.PRE,
                        AdhanEventKind.IQAMA -> {
                            AdhanAlertNotifier.show(context, prayer, kind)
                            AdhanFiredStore.remember(context, event)
                        }
                    }
                }
            }
        } finally {
            val fromMillis = AdhanPlaybackGuard.rescheduleFromMillis(
                firedAtMillis = firedAt,
                nowMillis = System.currentTimeMillis(),
            )
            AdhanAlarmScheduler.reschedule(context, fromMillis)
            val pendingRetry = retryEvent
            if (pendingRetry != null) {
                AdhanAlarmScheduler.schedulePlaybackRetry(context, pendingRetry)
            } else if (kind == AdhanEventKind.ADHAN) {
                AdhanAlarmScheduler.cancelPlaybackRetry(context)
            }
        }
    }
}
