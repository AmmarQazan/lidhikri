package com.greendome.adhkar.util

import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.data.model.CollectionDayMode
import com.greendome.adhkar.prayer.PrayerConfig

object AzkarItemSchedule {
    data class Trigger(
        val itemIds: List<Long>,
        val at: Long,
        val skipQuiet: Boolean,
        val inheritGroup: Boolean,
    )

    fun nextTriggerAt(
        collection: AdhkarCollectionEntity,
        item: AzkarItemEntity?,
        config: PrayerConfig,
        fromMillis: Long = System.currentTimeMillis(),
        hijriPool: List<AzkarItemEntity> = emptyList(),
    ): Long {
        val useOwnClock = item != null && item.scheduleHour >= 0
        val hour = if (useOwnClock) item.scheduleHour else collection.scheduleHour
        val minute = if (useOwnClock) item.scheduleMinute else collection.scheduleMinute
        val dayAllowed = dayMatcher(collection, item, hijriPool)
        val prayer = item?.prayerNameOrNull()
        if (prayer != null) {
            val prayerBased = CollectionScheduleHelper.nextMatchingDayPrayerOffset(
                collection,
                config,
                prayer,
                item.prayerOffsetMinutes,
                fromMillis,
                dayAllowed,
            )
            if (prayerBased != null) return distinctFromCollectionTime(collection, prayerBased, fromMillis, dayAllowed)
        }
        return CollectionScheduleHelper.nextTriggerAt(collection, fromMillis, hour, minute, dayAllowed)
    }

    fun triggers(
        collection: AdhkarCollectionEntity,
        items: List<AzkarItemEntity>,
        config: PrayerConfig,
        fromMillis: Long = System.currentTimeMillis(),
    ): List<Trigger> {
        if (!collection.autoPlayAllowed || !collection.autoPlayEnabled) return emptyList()
        if (collection.dayMode == CollectionDayMode.ITEM_HIJRI) {
            return blessedDayTriggers(collection, items, config, fromMillis)
        }
        val inherit = items.filter { it.inheritsCollectionTime() }
        val custom = items.filter { !it.inheritsCollectionTime() }
        val result = mutableListOf<Trigger>()
        if (inherit.isNotEmpty() || custom.isEmpty()) {
            val group = inherit.ifEmpty { items }
            if (group.isNotEmpty()) {
                result += Trigger(
                    itemIds = group.map { it.id },
                    at = nextTriggerAt(collection, null, config, fromMillis),
                    skipQuiet = group.any { it.skipQuietWindow },
                    inheritGroup = true,
                )
            }
        }
        custom.forEach { item ->
            result += Trigger(
                itemIds = listOf(item.id),
                at = nextTriggerAt(collection, item, config, fromMillis),
                skipQuiet = item.skipQuietWindow,
                inheritGroup = false,
            )
        }
        return result.sortedBy { it.at }
    }

    fun extraTriggers(
        collection: AdhkarCollectionEntity,
        items: List<AzkarItemEntity>,
        config: PrayerConfig,
        fromMillis: Long = System.currentTimeMillis(),
    ): List<Pair<AdhkarCollectionEntity, Long>> {
        if (!collection.autoPlayAllowed || !collection.autoPlayEnabled) return emptyList()
        val all = triggers(collection, items, config, fromMillis)
        val selected = if (collection.dayMode == CollectionDayMode.ITEM_HIJRI) {
            all
        } else {
            all.filter { !it.inheritGroup }
        }
        return selected.map { collection to it.at }
    }

    private fun blessedDayTriggers(
        collection: AdhkarCollectionEntity,
        items: List<AzkarItemEntity>,
        config: PrayerConfig,
        fromMillis: Long,
    ): List<Trigger> {
        val inherit = items.filter { it.inheritsCollectionTime() }
        val custom = items.filter { !it.inheritsCollectionTime() }
        val result = mutableListOf<Trigger>()
        if (inherit.isNotEmpty()) {
            result += Trigger(
                itemIds = inherit.map { it.id },
                at = nextTriggerAt(collection, null, config, fromMillis, inherit),
                skipQuiet = inherit.any { it.skipQuietWindow },
                inheritGroup = true,
            )
        }
        custom.forEach { item ->
            result += Trigger(
                itemIds = listOf(item.id),
                at = nextTriggerAt(collection, item, config, fromMillis, listOf(item)),
                skipQuiet = item.skipQuietWindow,
                inheritGroup = false,
            )
        }
        return result.sortedBy { it.at }
    }

    private fun dayMatcher(
        collection: AdhkarCollectionEntity,
        item: AzkarItemEntity?,
        hijriPool: List<AzkarItemEntity>,
    ): ((Long) -> Boolean)? {
        if (item?.hasOwnHijri() == true) return { millis -> item.matchesHijri(millis) }
        if (collection.dayMode != CollectionDayMode.ITEM_HIJRI) return null
        val pool = hijriPool.ifEmpty { listOfNotNull(item) }
        if (pool.isEmpty()) return { false }
        return { millis -> pool.any { it.hasOwnHijri() && it.matchesHijri(millis) } }
    }

    private fun distinctFromCollectionTime(
        collection: AdhkarCollectionEntity,
        raw: Long,
        fromMillis: Long,
        dayAllowed: ((Long) -> Boolean)?,
    ): Long {
        val kahfAt = CollectionScheduleHelper.nextTriggerAt(
            collection,
            fromMillis,
            dayAllowed = dayAllowed,
        )
        return if (kahfAt == raw) raw + 2 * 60_000L else raw
    }
}
