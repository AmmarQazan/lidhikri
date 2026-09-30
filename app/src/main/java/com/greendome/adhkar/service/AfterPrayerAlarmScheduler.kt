package com.greendome.adhkar.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.prayer.PrayerQuietWindows
import com.greendome.adhkar.prayer.PrayerRespectGate

object AfterPrayerAlarmScheduler {
    private const val REQUEST = 7001
    const val EXTRA_TRIGGER_AT = "trigger_at"
    const val EXTRA_RETRY = "playback_retry"

    fun reschedule(context: Context) {
        synchronized(this) {
            val settings = SettingsRepository(context)
            val config = settings.prayerConfig()
            if (!config.enabled || !config.afterPrayerReminder) {
                cancel(context)
                return
            }
            val now = System.currentTimeMillis()
            val triggerAt = PrayerQuietWindows.nextAfterPrayerTriggerAt(
                config,
                fromMillis = now,
                notBefore = maxOf(now, settings.afterPrayerLastHandledAt),
            ) ?: run {
                cancel(context)
                return
            }
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.scheduleWakeup(triggerAt, pending(context, triggerAt))
        }
    }

    fun markHandled(context: Context, scheduledAt: Long) {
        val settings = SettingsRepository(context)
        val config = settings.prayerConfig()
        val justDue = PrayerQuietWindows.afterPrayerTriggerJustDue(config) ?: 0L
        val handled = maxOf(scheduledAt, justDue)
        if (handled > settings.afterPrayerLastHandledAt) {
            settings.afterPrayerLastHandledAt = handled
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pending(context, 0L))
    }

    /** محاولة صوت واحدة. لا تُمسح عند حجز موعد الصلاة التالية. */
    fun schedulePlaybackRetry(context: Context, triggerAt: Long) {
        val app = context.applicationContext
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.scheduleWakeup(
            System.currentTimeMillis() + PLAYBACK_RETRY_DELAY_MS,
            retryPending(app, triggerAt),
        )
    }

    private fun retryPending(context: Context, triggerAt: Long): PendingIntent {
        val intent = Intent(context, AfterPrayerAlarmReceiver::class.java).apply {
            putExtra(EXTRA_TRIGGER_AT, triggerAt)
            putExtra(EXTRA_RETRY, true)
        }
        return PendingIntent.getBroadcast(
            context,
            RETRY_REQUEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun pending(context: Context, triggerAt: Long): PendingIntent {
        val intent = Intent(context, AfterPrayerAlarmReceiver::class.java).apply {
            putExtra(EXTRA_TRIGGER_AT, triggerAt)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private const val RETRY_REQUEST = 7002
}

class AfterPrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext
        val settings = SettingsRepository(app)
        val config = settings.prayerConfig()
        if (!config.enabled || !config.afterPrayerReminder) {
            AfterPrayerAlarmScheduler.reschedule(app)
            return
        }
        val scheduledAt = intent?.getLongExtra(AfterPrayerAlarmScheduler.EXTRA_TRIGGER_AT, 0L) ?: 0L
        val isRetry = intent?.getBooleanExtra(AfterPrayerAlarmScheduler.EXTRA_RETRY, false) == true
        PrayerPhoneSilent.exit(app)
        val play = Intent(app, AzkarCollectionPlayService::class.java).apply {
            putExtra(
                AzkarCollectionPlayService.EXTRA_COLLECTION_ID,
                PrayerRespectGate.AFTER_PRAYER_COLLECTION_ID
            )
            putExtra(AzkarCollectionPlayService.EXTRA_FORCE_PLAY, true)
        }
        val started = ForegroundServiceStarts.start(app, play)
        if (AdhanPlaybackGuard.missedPlayback(started, isRetry) == MissedPlayback.RETRY_ONCE) {
            AfterPrayerAlarmScheduler.schedulePlaybackRetry(app, scheduledAt)
        }
        synchronized(AfterPrayerAlarmScheduler) {
            AfterPrayerAlarmScheduler.markHandled(app, scheduledAt)
            AfterPrayerAlarmScheduler.reschedule(app)
        }
    }
}
