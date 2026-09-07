package com.greendome.adhkar.service

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.prayer.PrayerConfig
import com.greendome.adhkar.prayer.PrayerQuietWindows

/**
 * يحوّل الهاتف إلى الرجاج بعد الأذان وأذكار ما بعد الأذان حتى موعد أذكار ما بعد الصلاة.
 * رنين بلا صوت مع اهتزاز — يحتاج إذن وضع عدم الإزعاج لتغيير وضع الرنين.
 */
object PrayerPhoneSilent {
    private const val EXIT_REQUEST = 7201
    private const val ENTER_REQUEST = 7202

    const val ACTION_EXIT = "com.greendome.adhkar.PRAYER_PHONE_SILENT_EXIT"
    const val ACTION_ENTER = "com.greendome.adhkar.PRAYER_PHONE_SILENT_ENTER"

    fun hasPolicyAccess(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        return manager.isNotificationPolicyAccessGranted
    }

    fun openPolicySettings(context: Context): Boolean =
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        }.getOrDefault(false)

    fun enter(context: Context) {
        val app = context.applicationContext
        val settings = SettingsRepository(app)
        if (!settings.silentDuringFardPrayer) return
        if (!hasPolicyAccess(app)) return
        if (AdhanPlaybackService.isPlaying()) return
        val manager = app.getSystemService(NotificationManager::class.java) ?: return
        val audio = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (!settings.prayerPhoneSilentActive) {
            settings.prayerPhoneSilentPrevFilter = manager.currentInterruptionFilter
            settings.prayerPhoneSilentPrevRinger = audio.ringerMode
            settings.prayerPhoneSilentActive = true
            runCatching {
                if (manager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE) {
                    manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                }
                audio.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            }
        }
        scheduleExit(app, settings.prayerConfig())
    }

    fun exit(context: Context) {
        val app = context.applicationContext
        val settings = SettingsRepository(app)
        if (!settings.prayerPhoneSilentActive) {
            cancel(app, EXIT_REQUEST, ACTION_EXIT)
            return
        }
        val manager = app.getSystemService(NotificationManager::class.java)
        val audio = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val previousFilter = settings.prayerPhoneSilentPrevFilter
        val previousRinger = settings.prayerPhoneSilentPrevRinger
        runCatching {
            manager?.setInterruptionFilter(
                if (previousFilter > 0) previousFilter else NotificationManager.INTERRUPTION_FILTER_ALL
            )
        }
        runCatching {
            if (previousRinger >= AudioManager.RINGER_MODE_SILENT) {
                audio.ringerMode = previousRinger
            }
        }
        settings.prayerPhoneSilentActive = false
        settings.prayerPhoneSilentPrevFilter = -1
        settings.prayerPhoneSilentPrevRinger = -1
        cancel(app, EXIT_REQUEST, ACTION_EXIT)
    }

    fun sync(context: Context) {
        val app = context.applicationContext
        val settings = SettingsRepository(app)
        if (!settings.silentDuringFardPrayer) {
            exit(app)
            cancel(app, ENTER_REQUEST, ACTION_ENTER)
            return
        }
        val config = settings.prayerConfig()
        val now = System.currentTimeMillis()
        val endAt = PrayerQuietWindows.nextPhoneSilentEndAt(config, now - 1L)
        if (settings.prayerPhoneSilentActive && (endAt == null || now >= endAt)) {
            exit(app)
        }
        reschedule(app)
    }

    fun reschedule(context: Context) {
        val app = context.applicationContext
        val settings = SettingsRepository(app)
        if (!settings.silentDuringFardPrayer) {
            cancel(app, EXIT_REQUEST, ACTION_EXIT)
            cancel(app, ENTER_REQUEST, ACTION_ENTER)
            return
        }
        val config = settings.prayerConfig()
        scheduleExit(app, config)
        scheduleEnterIfNoAdhan(app, config)
    }

    private fun scheduleExit(context: Context, config: PrayerConfig) {
        val endAt = PrayerQuietWindows.nextPhoneSilentEndAt(config) ?: run {
            cancel(context, EXIT_REQUEST, ACTION_EXIT)
            return
        }
        setExact(context, EXIT_REQUEST, ACTION_EXIT, endAt)
    }

    private fun scheduleEnterIfNoAdhan(context: Context, config: PrayerConfig) {
        val now = System.currentTimeMillis()
        val window = PrayerQuietWindows.windowsAround(config, now)
            .firstOrNull { it.startMillis > now }
        if (window == null || !shouldEnterAtPrayerClock(config, window.prayer)) {
            cancel(context, ENTER_REQUEST, ACTION_ENTER)
            return
        }
        setExact(context, ENTER_REQUEST, ACTION_ENTER, window.startMillis)
    }

    private fun shouldEnterAtPrayerClock(config: PrayerConfig, prayer: com.greendome.adhkar.prayer.PrayerName): Boolean {
        if (!config.adhanActive) return true
        return !config.alert(prayer).adhanEnabled
    }

    private fun setExact(context: Context, request: Int, action: String, atMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pending(context, request, action)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, atMillis, pending)
        }
    }

    private fun cancel(context: Context, request: Int, action: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pending(context, request, action))
    }

    private fun pending(context: Context, request: Int, action: String): PendingIntent {
        val intent = Intent(context, PrayerPhoneSilentReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            request,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class PrayerPhoneSilentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            PrayerPhoneSilent.ACTION_EXIT -> PrayerPhoneSilent.exit(context)
            PrayerPhoneSilent.ACTION_ENTER -> PrayerPhoneSilent.enter(context)
        }
        PrayerPhoneSilent.reschedule(context)
    }
}
