package com.greendome.adhkar.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.util.CollectionScheduleHelper

object CollectionAlarmScheduler {
    private const val REQUEST_BASE = 5000
    private const val POSTPONED_BASE = 6000

    fun rescheduleAll(context: Context, collections: List<AdhkarCollectionEntity>) {
        collections.forEach { collection ->
            if (collection.autoPlayAllowed && collection.autoPlayEnabled) schedule(context, collection)
            else cancel(context, collection.id)
        }
    }

    fun schedule(context: Context, collection: AdhkarCollectionEntity) {
        if (!collection.autoPlayAllowed || !collection.autoPlayEnabled) {
            cancel(context, collection.id)
            return
        }
        val triggerAt = CollectionScheduleHelper.nextTriggerAt(collection)
        setAlarm(context, collection.id, triggerAt, postponed = false)
    }

    fun schedulePostponed(context: Context, collectionId: String, triggerAt: Long) {
        setAlarm(context, collectionId, triggerAt, postponed = true)
    }

    private fun setAlarm(
        context: Context,
        collectionId: String,
        triggerAt: Long,
        postponed: Boolean
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, CollectionAlarmReceiver::class.java).apply {
            putExtra(CollectionAlarmReceiver.EXTRA_COLLECTION_ID, collectionId)
            putExtra(CollectionAlarmReceiver.EXTRA_POSTPONED, postponed)
        }
        val request = (if (postponed) POSTPONED_BASE else REQUEST_BASE) + collectionId.hashCode()
        val pending = PendingIntent.getBroadcast(
            context,
            request,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancel(context: Context, collectionId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(REQUEST_BASE, POSTPONED_BASE).forEach { base ->
            val intent = Intent(context, CollectionAlarmReceiver::class.java)
            val pending = PendingIntent.getBroadcast(
                context,
                base + collectionId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pending)
        }
    }
}
