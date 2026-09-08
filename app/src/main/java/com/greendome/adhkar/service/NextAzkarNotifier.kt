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
import com.greendome.adhkar.util.LocaleHelper
import com.greendome.adhkar.util.NextAzkarSchedule
import com.greendome.adhkar.util.formatDigits

object NextAzkarNotifier {
    fun sync(context: Context) {
        SilentNotificationChannels.ensureCreated(context)
        val app = context.applicationContext
        val settings = SettingsRepository(app)
        val localized = LocaleHelper.wrap(app, settings.appLanguage)
        val line = NextAzkarSchedule.notificationLine(localized)
        if (line.isNullOrBlank()) {
            cancel(app)
            return
        }
        val text = line.formatDigits(settings.numberDigitStyle)
        app.getSystemService(NotificationManager::class.java)
            .notify(SilentNotificationChannels.NEXT_AZKAR_NOTIFICATION_ID, build(app, localized, text))
    }

    private fun build(context: Context, localized: Context, text: String): Notification {
        val open = PendingIntent.getActivity(
            context,
            SilentNotificationChannels.NEXT_AZKAR_NOTIFICATION_ID,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return SilentNotificationChannels.applyAppIcon(
            NotificationCompat.Builder(context, SilentNotificationChannels.NEXT_AZKAR),
            context,
        )
            .setContentTitle(localized.getString(R.string.next_azkar_title))
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
            .build()
    }

    fun cancel(context: Context) {
        context.applicationContext.getSystemService(NotificationManager::class.java)
            .cancel(SilentNotificationChannels.NEXT_AZKAR_NOTIFICATION_ID)
    }
}
