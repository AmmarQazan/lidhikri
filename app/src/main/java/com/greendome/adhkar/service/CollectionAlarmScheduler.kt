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
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, CollectionAlarmReceiver::class.java).apply {
            putExtra(CollectionAlarmReceiver.EXTRA_COLLECTION_ID, collection.id)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_BASE + collection.id.hashCode(),
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
        val intent = Intent(context, CollectionAlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_BASE + collectionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }
}
