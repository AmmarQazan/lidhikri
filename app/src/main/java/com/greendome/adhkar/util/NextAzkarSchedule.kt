package com.greendome.adhkar.util

import android.content.Context
import com.greendome.adhkar.R
import com.greendome.adhkar.data.AzkarFavorites
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.data.model.ClockHourFormat
import com.greendome.adhkar.data.model.NumberDigitStyle
import com.greendome.adhkar.prayer.PrayerConfig
import com.greendome.adhkar.prayer.PrayerQuietWindows
import com.greendome.adhkar.prayer.PrayerRespectGate
import java.util.Calendar
import kotlinx.coroutines.runBlocking

object NextAzkarSchedule {
    fun resolve(
        collections: List<AdhkarCollectionEntity>,
        items: List<AzkarItemEntity>,
        prayerConfig: PrayerConfig,
        fromMillis: Long = System.currentTimeMillis(),
    ): Pair<AdhkarCollectionEntity, Long>? {
        val extras = buildList {
            val afterPrayer = collections.find {
                it.id == PrayerRespectGate.AFTER_PRAYER_COLLECTION_ID
            }
            val afterPrayerAt = PrayerQuietWindows.nextAfterPrayerTriggerAt(prayerConfig, fromMillis)
            if (afterPrayer != null && afterPrayerAt != null) {
                add(afterPrayer to afterPrayerAt)
            }
            val itemsByCollection = items.groupBy { it.collectionId }
            collections.forEach { collection ->
                addAll(
                    AzkarItemSchedule.extraTriggers(
                        collection,
                        itemsByCollection[collection.id].orEmpty(),
                        prayerConfig,
                        fromMillis,
                    )
                )
            }
        }
        return CollectionScheduleHelper.findNextEnabled(collections, fromMillis, extras)
    }

    fun formatLine(
        context: Context,
        collection: AdhkarCollectionEntity,
        triggerAt: Long,
        lang: String,
        clockHourFormat: ClockHourFormat,
        numberDigitStyle: NumberDigitStyle,
        nowMillis: Long = System.currentTimeMillis(),
    ): String {
        val title = collectionTitle(context, collection, lang)
        val timeLabel = timeLabel(context, triggerAt, clockHourFormat, nowMillis)
            .formatDigits(numberDigitStyle)
        return context.getString(R.string.next_azkar_summary, title, timeLabel)
            .formatDigits(numberDigitStyle)
    }

    fun notificationLine(context: Context): String? {
        val settings = SettingsRepository(context)
        if (!settings.autoAzkarEnabled) return null
        val db = AdhkarDatabase.get(context)
        val (collections, items) = runBlocking {
            db.collectionDao().getAll() to db.azkarItemDao().getAll()
        }
        val next = resolve(collections, items, settings.prayerConfig()) ?: return null
        return formatLine(
            context = context,
            collection = next.first,
            triggerAt = next.second,
            lang = settings.appLanguage,
            clockHourFormat = settings.azkarClockHourFormat,
            numberDigitStyle = settings.numberDigitStyle,
        )
    }

    internal fun relativeDay(triggerAt: Long, nowMillis: Long): RelativeDay {
        val trigger = Calendar.getInstance().apply { timeInMillis = triggerAt }
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val tomorrow = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return when {
            sameDay(trigger, now) -> RelativeDay.TODAY
            sameDay(trigger, tomorrow) -> RelativeDay.TOMORROW
            else -> RelativeDay.LATER
        }
    }

    private fun timeLabel(
        context: Context,
        triggerAt: Long,
        clockHourFormat: ClockHourFormat,
        nowMillis: Long,
    ): String {
        val trigger = Calendar.getInstance().apply { timeInMillis = triggerAt }
        val clock = formatClockTime(
            trigger.get(Calendar.HOUR_OF_DAY),
            trigger.get(Calendar.MINUTE),
            clockHourFormat,
            context.getString(R.string.clock_period_am),
            context.getString(R.string.clock_period_pm),
        )
        return when (relativeDay(triggerAt, nowMillis)) {
            RelativeDay.TODAY -> context.getString(R.string.next_azkar_time_today, clock)
            RelativeDay.TOMORROW -> context.getString(R.string.next_azkar_time_tomorrow, clock)
            RelativeDay.LATER -> context.getString(
                R.string.next_azkar_time_later,
                trigger.get(Calendar.DAY_OF_MONTH),
                trigger.get(Calendar.MONTH) + 1,
                clock,
            )
        }
    }

    private fun collectionTitle(
        context: Context,
        collection: AdhkarCollectionEntity,
        lang: String,
    ): String = if (collection.id == AzkarFavorites.COLLECTION_ID) {
        context.getString(R.string.azkar_favorites_title)
    } else {
        collection.localizedTitle(lang)
    }

    private fun sameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    enum class RelativeDay { TODAY, TOMORROW, LATER }
}
