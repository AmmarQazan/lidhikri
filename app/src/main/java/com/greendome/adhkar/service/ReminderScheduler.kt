package com.greendome.adhkar.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.prayer.PrayerQuietWindows
import com.greendome.adhkar.prayer.PrayerRespectGate
import com.greendome.adhkar.util.CollectionScheduleHelper
import com.greendome.adhkar.util.TasbihWindow
import java.util.Calendar
import kotlinx.coroutines.runBlocking

object ReminderScheduler {
    fun scheduleNext(context: Context) {
        val now = System.currentTimeMillis()
        val azkarCollections = loadAutoAzkarCollections(context)
        val triggerAt = nextTasbihTriggerAt(context, now, azkarCollections)

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
        val now = System.currentTimeMillis()
        val azkarCollections = loadAutoAzkarCollections(context)
        val triggerAt = nextTasbihTriggerAt(context, now, azkarCollections)
        return ((triggerAt - now) / 60_000).toInt().coerceAtLeast(0)
    }

    private fun loadAutoAzkarCollections(context: Context): List<AdhkarCollectionEntity> {
        val settings = SettingsRepository(context)
        if (!settings.autoAzkarEnabled) return emptyList()
        return runBlocking {
            AdhkarDatabase.get(context).collectionDao().getAutoEnabled()
        }
    }

    /** يتخطى فترات التسبيح التي تتزامن مع ذكر تلقائي أو أذكار ما بعد الصلاة */
    private fun nextTasbihTriggerAt(
        context: Context,
        fromMillis: Long,
        azkarCollections: List<AdhkarCollectionEntity>
    ): Long {
        val settings = SettingsRepository(context)
        val intervalMs = settings.intervalMinutes.coerceAtLeast(1) * 60_000L
        val window = TasbihWindow.from(settings)
        val prayerConfig = settings.prayerConfig()
        var triggerAt = window.nextTriggerAfter(fromMillis, intervalMs)
        val maxSlots = (24 * 60 * 60 * 1000L / intervalMs).toInt().coerceAtMost(2000)
        repeat(maxSlots) {
            triggerAt = PrayerRespectGate.delayPastQuiet(context, triggerAt)
            val cal = Calendar.getInstance().apply { timeInMillis = triggerAt }
            val blockedByAzkar = CollectionScheduleHelper.isAnyDueAt(azkarCollections, cal)
            val blockedByAfterPrayer = PrayerQuietWindows.collidesWithAfterPrayer(
                prayerConfig,
                triggerAt
            )
            if (!blockedByAzkar && !blockedByAfterPrayer) return triggerAt
            triggerAt = window.nextTriggerAfter(triggerAt, intervalMs)
        }
        return triggerAt
    }

    fun isOutsideTasbihWindow(context: Context): Boolean {
        val settings = SettingsRepository(context)
        return !TasbihWindow.from(settings).contains(Calendar.getInstance())
    }
}
