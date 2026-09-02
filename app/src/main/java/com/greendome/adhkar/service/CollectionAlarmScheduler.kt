package com.greendome.adhkar.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.prayer.PrayerQuietWindows
import com.greendome.adhkar.util.AzkarItemSchedule

object CollectionAlarmScheduler {
    private const val REQUEST_BASE = 5000
    private const val POSTPONED_BASE = 6000
    private const val SLOT_SPREAD = 97
    private const val MAX_TRIGGERS = 32

    fun rescheduleAll(context: Context, collections: List<AdhkarCollectionEntity>) {
        collections.forEach { collection ->
            if (collection.autoPlayAllowed && collection.autoPlayEnabled) schedule(context, collection)
            else cancel(context, collection.id)
        }
    }

    fun schedule(
        context: Context,
        collection: AdhkarCollectionEntity,
        items: List<AzkarItemEntity> = emptyList(),
    ) {
        if (!collection.autoPlayAllowed || !collection.autoPlayEnabled) {
            cancel(context, collection.id)
            return
        }
        cancel(context, collection.id, includePostponed = false)
        val config = SettingsRepository(context).prayerConfig()
        AzkarItemSchedule.triggers(collection, items, config).forEachIndexed { index, trigger ->
            if (index >= MAX_TRIGGERS) return@forEachIndexed
            val triggerAt = if (PrayerQuietWindows.collidesWithAdhan(config, trigger.at)) {
                PrayerQuietWindows.delayPastAdhan(config, trigger.at)
            } else {
                trigger.at
            }
            setAlarm(
                context = context,
                collectionId = collection.id,
                triggerAt = triggerAt,
                postponed = false,
                triggerIndex = index,
                itemId = trigger.itemIds.singleOrNull() ?: 0L,
                inheritGroup = trigger.inheritGroup,
                skipQuiet = trigger.skipQuiet,
            )
        }
    }

    fun schedulePostponed(
        context: Context,
        collectionId: String,
        triggerAt: Long,
        triggerIndex: Int,
        itemId: Long,
        inheritGroup: Boolean,
        skipQuiet: Boolean,
    ) {
        setAlarm(
            context = context,
            collectionId = collectionId,
            triggerAt = triggerAt,
            postponed = true,
            triggerIndex = triggerIndex,
            itemId = itemId,
            inheritGroup = inheritGroup,
            skipQuiet = skipQuiet,
        )
    }

    private fun setAlarm(
        context: Context,
        collectionId: String,
        triggerAt: Long,
        postponed: Boolean,
        triggerIndex: Int,
        itemId: Long,
        inheritGroup: Boolean,
        skipQuiet: Boolean,
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, CollectionAlarmReceiver::class.java).apply {
            putExtra(CollectionAlarmReceiver.EXTRA_COLLECTION_ID, collectionId)
            putExtra(CollectionAlarmReceiver.EXTRA_POSTPONED, postponed)
            putExtra(CollectionAlarmReceiver.EXTRA_TRIGGER_INDEX, triggerIndex)
            putExtra(CollectionAlarmReceiver.EXTRA_ITEM_ID, itemId)
            putExtra(CollectionAlarmReceiver.EXTRA_INHERIT, inheritGroup)
            putExtra(CollectionAlarmReceiver.EXTRA_SKIP_QUIET, skipQuiet)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode(collectionId, postponed, triggerIndex),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancel(context: Context, collectionId: String, includePostponed: Boolean = true) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val postponedFlags = if (includePostponed) listOf(false, true) else listOf(false)
        postponedFlags.forEach { postponed ->
            repeat(MAX_TRIGGERS) { index ->
                val intent = Intent(context, CollectionAlarmReceiver::class.java)
                val pending = PendingIntent.getBroadcast(
                    context,
                    requestCode(collectionId, postponed, index),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pending)
            }
        }
    }

    private fun requestCode(collectionId: String, postponed: Boolean, triggerIndex: Int): Int {
        val base = if (postponed) POSTPONED_BASE else REQUEST_BASE
        return base + collectionId.hashCode() + (triggerIndex + 1) * SLOT_SPREAD
    }
}
