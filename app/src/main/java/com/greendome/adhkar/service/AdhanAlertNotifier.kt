package com.greendome.adhkar.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.greendome.adhkar.MainActivity
import com.greendome.adhkar.R
import com.greendome.adhkar.prayer.AdhanEventKind
import com.greendome.adhkar.prayer.PrayerName
import com.greendome.adhkar.ui.adhan.AdhanActivity

object AdhanAlertNotifier {
    const val CHANNEL = "adhan_alarm_v1"
    const val PRE_CHANNEL = "adhan_pre_v1"
    const val NOTIF_ADHAN = 91
    const val NOTIF_PRE = 92
    const val NOTIF_IQAMA = 93

    fun ensureChannel(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java)
        mgr.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                context.getString(R.string.adhan_channel),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                enableVibration(true)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                description = context.getString(R.string.adhan_channel_hint)
            }
        )
        mgr.createNotificationChannel(
            NotificationChannel(
                PRE_CHANNEL,
                context.getString(R.string.adhan_pre_channel),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                enableVibration(true)
                setBypassDnd(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                description = context.getString(R.string.adhan_pre_channel_hint)
            }
        )
    }

    fun show(
        context: Context,
        prayer: PrayerName,
        kind: AdhanEventKind,
        fullScreen: Boolean = kind == AdhanEventKind.ADHAN,
    ) {
        ensureChannel(context)
        val title = when (kind) {
            AdhanEventKind.PRE -> context.getString(R.string.adhan_pre_title, prayerLabel(context, prayer))
            AdhanEventKind.ADHAN -> context.getString(R.string.adhan_now_title, prayerLabel(context, prayer))
            AdhanEventKind.IQAMA -> context.getString(R.string.adhan_iqama_title, prayerLabel(context, prayer))
        }
        val text = when (kind) {
            AdhanEventKind.PRE -> context.getString(R.string.adhan_pre_text)
            AdhanEventKind.ADHAN -> context.getString(R.string.adhan_now_text)
            AdhanEventKind.IQAMA -> context.getString(R.string.adhan_iqama_text)
        }
        val content = PendingIntent.getActivity(
            context,
            800 + kind.ordinal,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val channel = if (kind == AdhanEventKind.PRE) PRE_CHANNEL else CHANNEL
        val category = if (kind == AdhanEventKind.PRE) {
            NotificationCompat.CATEGORY_REMINDER
        } else {
            NotificationCompat.CATEGORY_ALARM
        }
        val builder = SilentNotificationChannels.applyAppIcon(
            NotificationCompat.Builder(context, channel),
            context,
        )
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(content)
            .setAutoCancel(true)
            .setCategory(category)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(kind != AdhanEventKind.ADHAN)
        if (fullScreen && kind == AdhanEventKind.ADHAN) {
            val fullScreen = PendingIntent.getActivity(
                context,
                810,
                Intent(context, AdhanActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(AdhanAlarmScheduler.EXTRA_PRAYER, prayer.name)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setFullScreenIntent(fullScreen, true)
        }
        val id = when (kind) {
            AdhanEventKind.PRE -> NOTIF_PRE
            AdhanEventKind.ADHAN -> NOTIF_ADHAN
            AdhanEventKind.IQAMA -> NOTIF_IQAMA
        }
        context.getSystemService(NotificationManager::class.java).notify(id, builder.build())
    }

    fun prayerLabel(context: Context, prayer: PrayerName): String = context.getString(
        when (prayer) {
            PrayerName.FAJR -> R.string.prayer_name_fajr
            PrayerName.DHUHR -> R.string.prayer_name_dhuhr
            PrayerName.ASR -> R.string.prayer_name_asr
            PrayerName.MAGHRIB -> R.string.prayer_name_maghrib
            PrayerName.ISHA -> R.string.prayer_name_isha
        }
    )
}
