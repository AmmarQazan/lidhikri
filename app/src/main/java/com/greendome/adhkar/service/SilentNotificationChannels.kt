package com.greendome.adhkar.service

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

/** قناة واحدة فقط — إشعار الخدمة الخلفية، بدون إشعارات تسبيح */
object SilentNotificationChannels {
    const val SERVICE = "adhkar_service_silent_v2"
    const val TEXT_REMINDER = "adhkar_text_reminder_v1"
    const val DHIKR_OF_DAY = "dhikr_of_day_v1"

    private val REMOVED_CHANNEL_IDS = listOf(
        "adhkar_service_silent",
        "adhkar_reminder_silent_v3",
        "adhkar_reminder_popup_v1",
        "service",
        "reminder",
        "reminder_silent",
        "reminder_no_sound"
    )

    /** معرّفات إشعارات التسبيح القديمة كانت تُستخدم كـ dhikrId.toInt() */
    private const val LEGACY_ALERT_ID_MIN = 1
    private const val LEGACY_ALERT_ID_MAX = 2_000

    fun ensureCreated(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java)
        REMOVED_CHANNEL_IDS.forEach { mgr.deleteNotificationChannel(it) }
        cancelDhikrAlerts(context)

        mgr.createNotificationChannel(
            NotificationChannel(
                SERVICE,
                context.getString(com.greendome.adhkar.R.string.service_channel),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
                description = context.getString(com.greendome.adhkar.R.string.service_channel_hint)
            }
        )
        mgr.createNotificationChannel(
            NotificationChannel(
                TEXT_REMINDER,
                context.getString(com.greendome.adhkar.R.string.text_reminder_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
                setShowBadge(false)
                description = context.getString(com.greendome.adhkar.R.string.text_reminder_channel_hint)
            }
        )
        mgr.createNotificationChannel(
            NotificationChannel(
                DHIKR_OF_DAY,
                context.getString(com.greendome.adhkar.R.string.dhikr_of_day_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                description = context.getString(com.greendome.adhkar.R.string.dhikr_of_day_channel_hint)
            }
        )
    }

    /** إلغاء أي إشعار تسبيح قديم ما زال معلقاً في النظام */
    fun cancelDhikrAlerts(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mgr.activeNotifications
                .filter { it.id != 42 && it.id != 77 }
                .filter { it.notification.channelId != TEXT_REMINDER }
                .forEach { posted ->
                    mgr.cancel(posted.tag, posted.id)
                }
        }
        for (id in LEGACY_ALERT_ID_MIN..2_000) {
            mgr.cancel(id)
        }
    }

    fun applySilentDefaults(builder: NotificationCompat.Builder): NotificationCompat.Builder =
        builder
            .setSilent(true)
            .setDefaults(0)
            .setSound(null)
            .setVibrate(null)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
}

object ServiceRunningHelper {
    fun isRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        return manager.getRunningServices(Int.MAX_VALUE).any {
            it.service.className == serviceClass.name
        }
    }
}
