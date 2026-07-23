package com.greendome.adhkar.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.greendome.adhkar.data.SettingsRepository
import java.util.Calendar

object ReminderScheduler {
    fun scheduleNext(context: Context) {
        val settings = SettingsRepository(context)
        val intervalMs = settings.intervalMinutes.coerceAtLeast(1) * 60_000L
        val now = System.currentTimeMillis()
        val dayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val elapsed = now - dayStart
        val slotsPassed = elapsed / intervalMs
        val nextSlot = (slotsPassed + 1) * intervalMs
        val triggerAt = dayStart + nextSlot

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, 1001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, 1001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }

    fun minutesUntilNext(context: Context): Int {
        val settings = SettingsRepository(context)
        val intervalMs = settings.intervalMinutes.coerceAtLeast(1) * 60_000L
        val now = System.currentTimeMillis()
        val dayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val elapsed = now - dayStart
        val slotsPassed = elapsed / intervalMs
        val nextSlot = (slotsPassed + 1) * intervalMs
        return ((nextSlot - elapsed) / 60_000).toInt().coerceAtLeast(0)
    }

    fun isInSleepWindow(context: Context): Boolean {
        val s = SettingsRepository(context)
        val cal = Calendar.getInstance()
        val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val start = s.sleepStartHour * 60 + s.sleepStartMinute
        val end = s.sleepEndHour * 60 + s.sleepEndMinute
        return if (start > end) nowMin >= start || nowMin < end else nowMin in start until end
    }
}
