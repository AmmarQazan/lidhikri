package com.greendome.adhkar.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.greendome.adhkar.MainActivity
import com.greendome.adhkar.R
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.prayer.NextAdhanStatus
import com.greendome.adhkar.util.LocaleHelper
import com.greendome.adhkar.util.formatDigits

object NextAdhanNotifier {
    fun show(context: Context, status: NextAdhanStatus? = null): Notification? {
        SilentNotificationChannels.ensureCreated(context)
        val app = context.applicationContext
        val settings = SettingsRepository(app)
        val resolved = status ?: NextAdhanStatus.resolve(settings.prayerConfig())
        if (resolved == null) {
            cancel(app)
            return null
        }
        val notification = build(app, settings, resolved)
        app.getSystemService(NotificationManager::class.java)
            .notify(SilentNotificationChannels.NEXT_ADHAN_NOTIFICATION_ID, notification)
        return notification
    }

    fun build(
        context: Context,
        settings: SettingsRepository = SettingsRepository(context),
        status: NextAdhanStatus,
    ): Notification {
        val localized = LocaleHelper.wrap(context, settings.appLanguage)
        val prayer = AdhanAlertNotifier.prayerLabel(localized, status.prayer)
        val text = if (status.minutesRemaining <= 0) {
            localized.getString(R.string.next_adhan_soon, prayer)
        } else {
            localized.getString(R.string.next_adhan_in, prayer, status.minutesRemaining)
        }.formatDigits(settings.numberDigitStyle)
        val open = PendingIntent.getActivity(
            context,
            SilentNotificationChannels.NEXT_ADHAN_NOTIFICATION_ID,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, SilentNotificationChannels.NEXT_ADHAN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(localized.getString(R.string.next_adhan_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setDefaults(0)
            .setSound(null)
            .setVibrate(null)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        return builder.build()
    }

    fun cancel(context: Context) {
        context.applicationContext.getSystemService(NotificationManager::class.java)
            .cancel(SilentNotificationChannels.NEXT_ADHAN_NOTIFICATION_ID)
    }
}
