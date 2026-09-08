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
import com.greendome.adhkar.prayer.NextAdhanBodyKind
import com.greendome.adhkar.prayer.NextAdhanStatus
import com.greendome.adhkar.prayer.PrayerTimesCalculator
import com.greendome.adhkar.util.LocaleHelper
import com.greendome.adhkar.util.formatClockTime
import com.greendome.adhkar.util.formatDigits
import java.time.Instant

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
        val text = formatStatusText(localized, settings, status)
        val open = PendingIntent.getActivity(
            context,
            SilentNotificationChannels.NEXT_ADHAN_NOTIFICATION_ID,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = SilentNotificationChannels.applyAppIcon(
            NotificationCompat.Builder(context, SilentNotificationChannels.NEXT_ADHAN),
            context,
        )
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

    internal fun formatStatusText(
        localized: Context,
        settings: SettingsRepository,
        status: NextAdhanStatus,
    ): String {
        val prayer = AdhanAlertNotifier.prayerLabel(localized, status.prayer)
        val text = when (status.bodyKind()) {
            NextAdhanBodyKind.SOON ->
                localized.getString(R.string.next_adhan_soon, prayer)
            NextAdhanBodyKind.MINUTES ->
                localized.getString(R.string.next_adhan_in, prayer, status.minutesRemaining)
            NextAdhanBodyKind.HOURS ->
                localized.getString(
                    R.string.next_adhan_in_h,
                    prayer,
                    status.hoursRemaining,
                    clockLabel(localized, settings, status),
                )
            NextAdhanBodyKind.HOURS_AND_MINUTES ->
                localized.getString(
                    R.string.next_adhan_in_hm,
                    prayer,
                    status.hoursRemaining,
                    status.minutesPastHour,
                    clockLabel(localized, settings, status),
                )
        }
        return text.formatDigits(settings.numberDigitStyle)
    }

    private fun clockLabel(
        localized: Context,
        settings: SettingsRepository,
        status: NextAdhanStatus,
    ): String {
        val zone = PrayerTimesCalculator.zoneId(settings.prayerConfig())
        val local = Instant.ofEpochMilli(status.atMillis).atZone(zone)
        return formatClockTime(
            local.hour,
            local.minute,
            settings.azkarClockHourFormat,
            localized.getString(R.string.clock_period_am),
            localized.getString(R.string.clock_period_pm),
            separator = ".",
        )
    }

    fun cancel(context: Context) {
        context.applicationContext.getSystemService(NotificationManager::class.java)
            .cancel(SilentNotificationChannels.NEXT_ADHAN_NOTIFICATION_ID)
    }
}
