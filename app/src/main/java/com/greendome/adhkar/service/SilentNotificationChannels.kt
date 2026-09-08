package com.greendome.adhkar.service

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/** قنوات الإشعارات الصامتة — خدمة خلفية، تذكيرات، وشاشة القفل */
object SilentNotificationChannels {
    const val SERVICE = "adhkar_service_silent_v2"
    const val TEXT_REMINDER = "adhkar_text_reminder_v2"
    const val DHIKR_OF_DAY = "dhikr_of_day_v2"
    const val LOCK_SCREEN = "adhkar_lock_screen_v1"
    const val NEXT_ADHAN = "next_adhan_status_v1"
    const val NEXT_AZKAR = "next_azkar_status_v1"

    const val SERVICE_NOTIFICATION_ID = 42
    const val AZKAR_PLAY_NOTIFICATION_ID = 77
    const val DHIKR_OF_DAY_NOTIFICATION_ID = 88
    const val NEXT_ADHAN_NOTIFICATION_ID = 8_901
    const val NEXT_AZKAR_NOTIFICATION_ID = 8_902
    const val LOCK_SCREEN_NOTIFICATION_ID_BASE = 9_000

    private val PROTECTED_NOTIFICATION_IDS = setOf(
        SERVICE_NOTIFICATION_ID,
        AZKAR_PLAY_NOTIFICATION_ID,
        DHIKR_OF_DAY_NOTIFICATION_ID,
        NEXT_ADHAN_NOTIFICATION_ID,
        NEXT_AZKAR_NOTIFICATION_ID,
        AdhanAlertNotifier.NOTIF_ADHAN,
        AdhanAlertNotifier.NOTIF_PRE,
        AdhanAlertNotifier.NOTIF_IQAMA,
    )

    private val REMOVED_CHANNEL_IDS = listOf(
        "adhkar_service_silent",
        "adhkar_reminder_silent_v3",
        "adhkar_reminder_popup_v1",
        "adhkar_text_reminder_v1",
        "dhikr_of_day_v1",
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
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                description = context.getString(com.greendome.adhkar.R.string.text_reminder_channel_hint)
            }
        )
        mgr.createNotificationChannel(
            NotificationChannel(
                DHIKR_OF_DAY,
                context.getString(com.greendome.adhkar.R.string.dhikr_of_day_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                description = context.getString(com.greendome.adhkar.R.string.dhikr_of_day_channel_hint)
            }
        )
        mgr.createNotificationChannel(
            NotificationChannel(
                LOCK_SCREEN,
                context.getString(com.greendome.adhkar.R.string.lock_screen_channel),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                description = context.getString(com.greendome.adhkar.R.string.lock_screen_channel_hint)
            }
        )
        mgr.createNotificationChannel(
            NotificationChannel(
                NEXT_ADHAN,
                context.getString(com.greendome.adhkar.R.string.next_adhan_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                description = context.getString(com.greendome.adhkar.R.string.next_adhan_channel_hint)
            }
        )
        mgr.createNotificationChannel(
            NotificationChannel(
                NEXT_AZKAR,
                context.getString(com.greendome.adhkar.R.string.next_azkar_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                description = context.getString(com.greendome.adhkar.R.string.next_azkar_channel_hint)
            }
        )
    }

    /** تنظيف معرّفات إشعارات التسبيح القديمة فقط — دون المساس بذكر اليوم أو شاشة القفل */
    fun cancelLegacyAlertIds(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java)
        for (id in LEGACY_ALERT_ID_MIN..LEGACY_ALERT_ID_MAX) {
            if (id !in PROTECTED_NOTIFICATION_IDS) {
                mgr.cancel(id)
            }
        }
    }

    /** إلغاء تذكير التسبيح/الذكر الحالي قبل عرض تذكير جديد */
    fun cancelTransientReminderAlerts(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mgr.activeNotifications
                .filter { posted ->
                    val channelId = posted.notification.channelId
                    channelId == TEXT_REMINDER || channelId == LOCK_SCREEN
                }
                .forEach { posted ->
                    mgr.cancel(posted.tag, posted.id)
                }
        }
        for (id in LEGACY_ALERT_ID_MIN..LEGACY_ALERT_ID_MAX) {
            if (id !in PROTECTED_NOTIFICATION_IDS) {
                mgr.cancel(id)
            }
        }
    }

    @Deprecated("استخدم cancelLegacyAlertIds أو cancelTransientReminderAlerts")
    fun cancelDhikrAlerts(context: Context) = cancelLegacyAlertIds(context)

    fun applyAppIcon(builder: NotificationCompat.Builder, context: Context): NotificationCompat.Builder =
        builder
            .setSmallIcon(com.greendome.adhkar.R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, com.greendome.adhkar.R.color.green_primary))

    fun applyTextReminderDefaults(builder: NotificationCompat.Builder): NotificationCompat.Builder =
        builder
            .setSilent(true)
            .setDefaults(0)
            .setSound(null)
            .setVibrate(null)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

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
