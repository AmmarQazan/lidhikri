package com.greendome.adhkar.service

import android.content.Context
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.widget.PrayerTimesWidgetManager

object PrayerAlarms {
    fun rescheduleAll(context: Context) {
        AfterPrayerAlarmScheduler.reschedule(context)
        AdhanAlarmScheduler.reschedule(context)
        PrayerPhoneSilent.reschedule(context)
        PrayerTimesWidgetManager.updateAll(context)
        val settings = SettingsRepository(context)
        if (settings.isServiceEnabled) {
            ReminderScheduler.scheduleNext(context)
            AdhkarReminderService.refreshNotification(context)
        }
        NextAzkarNotifier.sync(context)
    }
}
