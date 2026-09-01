package com.greendome.adhkar.util

import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.data.model.CollectionDayMode
import com.greendome.adhkar.prayer.PrayerConfig
import com.greendome.adhkar.prayer.PrayerName
import com.greendome.adhkar.prayer.PrayerTimesCalculator
import java.time.Instant
import java.time.ZoneId
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.util.Calendar
import java.util.TimeZone

object CollectionScheduleHelper {
    private val DAY_BITS = intArrayOf(
        Calendar.SUNDAY,
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY,
        Calendar.SATURDAY
    )

    private val HIJRI_MONTHS_AR = listOf(
        "", "محرم", "صفر", "ربيع الأول", "ربيع الثاني", "جمادى الأولى", "جمادى الآخرة",
        "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    fun isDayEnabled(mask: Int, dayOfWeek: Int): Boolean {
        val index = DAY_BITS.indexOf(dayOfWeek)
        if (index < 0) return false
        return mask and (1 shl index) != 0
    }

    fun toggleDay(mask: Int, dayOfWeek: Int, enabled: Boolean): Int {
        val index = DAY_BITS.indexOf(dayOfWeek)
        if (index < 0) return mask
        val bit = 1 shl index
        return if (enabled) mask or bit else mask and bit.inv()
    }

    fun allDaysEnabled(): Int = 127

    fun fridayOnlyMask(): Int = 1 shl DAY_BITS.indexOf(Calendar.FRIDAY)

    fun isDayAllowed(collection: AdhkarCollectionEntity, calendar: Calendar): Boolean =
        isDayAllowed(collection, calendar.timeInMillis)

    fun isDayAllowed(
        collection: AdhkarCollectionEntity,
        millis: Long,
        items: List<AzkarItemEntity> = emptyList(),
        item: AzkarItemEntity? = null,
    ): Boolean {
        if (item?.hasOwnHijri() == true) return item.matchesHijri(millis)
        return when (collection.dayMode) {
            CollectionDayMode.WEEKDAYS -> {
                val cal = Calendar.getInstance().apply { timeInMillis = millis }
                isDayEnabled(collection.weekDaysMask, cal.get(Calendar.DAY_OF_WEEK))
            }
            CollectionDayMode.HIJRI -> isHijriMatch(collection, millis)
            CollectionDayMode.ITEM_HIJRI -> {
                val pool = if (item != null) listOf(item) else items
                pool.any { it.hasOwnHijri() && it.matchesHijri(millis) }
            }
        }
    }

    fun isHijriRangeMatch(month: Int, dayStart: Int, dayEnd: Int, millis: Long): Boolean {
        if (month < 1) return false
        val hijri = hijriOf(millis)
        if (hijri.get(ChronoField.MONTH_OF_YEAR) != month) return false
        val day = hijri.get(ChronoField.DAY_OF_MONTH)
        val start = if (dayStart > 0) dayStart else 1
        val end = if (dayEnd > 0) dayEnd else start
        return day in start..end
    }

    fun isHijriMatch(collection: AdhkarCollectionEntity, millis: Long): Boolean =
        isHijriRangeMatch(collection.hijriMonth, collection.hijriDayStart, collection.hijriDayEnd, millis)

    fun hijriSummaryAr(month: Int, dayStart: Int, dayEnd: Int): String {
        if (month < 1) return ""
        val monthName = HIJRI_MONTHS_AR.getOrElse(month) { "شهر $month" }
        val start = if (dayStart > 0) dayStart else 1
        val end = if (dayEnd > 0) dayEnd else start
        return if (start == end) "$start $monthName" else "$start–$end $monthName"
    }

    fun hijriSummaryAr(collection: AdhkarCollectionEntity): String =
        hijriSummaryAr(collection.hijriMonth, collection.hijriDayStart, collection.hijriDayEnd)

    fun hijriSummaryAr(item: AzkarItemEntity): String =
        hijriSummaryAr(item.hijriMonth, item.hijriDayStart, item.hijriDayEnd)

    fun dayLabels(lang: String): List<String> {
        val symbols = java.text.DateFormatSymbols(AppLanguages.locale(lang))
        val weekdays = symbols.weekdays
        return DAY_BITS.map { weekdays[it] }
    }

    fun dayLabelsAr(): List<String> = dayLabels("ar")

    fun formatSchedule(collection: AdhkarCollectionEntity, lang: String): String {
        val daysText = when (collection.dayMode) {
            CollectionDayMode.HIJRI -> hijriSummaryAr(collection).ifBlank {
                if (lang == "ar") "تاريخ هجري" else "Hijri date"
            }
            CollectionDayMode.ITEM_HIJRI -> if (lang == "ar") "أيام السنة الهجرية" else "Hijri dates of the year"
            CollectionDayMode.WEEKDAYS -> {
                val labels = dayLabels(lang)
                val days = labels
                    .mapIndexed { index, label -> if (collection.weekDaysMask and (1 shl index) != 0) label else null }
                    .filterNotNull()
                if (days.size == 7) everyDayLabel(lang) else {
                    val sep = if (AppLanguages.isRtl(lang)) "، " else ", "
                    days.joinToString(sep)
                }
            }
        }
        val hour = collection.scheduleHour.toString().padStart(2, '0')
        val minute = collection.scheduleMinute.toString().padStart(2, '0')
        return "$daysText — $hour:$minute"
    }

    fun formatScheduleAr(collection: AdhkarCollectionEntity): String = formatSchedule(collection, "ar")

    private fun everyDayLabel(lang: String): String = when (lang) {
        "ar" -> "كل يوم"
        "tr" -> "Her gün"
        "ur" -> "ہر روز"
        "id" -> "Setiap hari"
        "hi" -> "हर दिन"
        "fr" -> "Tous les jours"
        "es" -> "Todos los días"
        else -> "Every day"
    }

    fun nextTriggerAt(
        collection: AdhkarCollectionEntity,
        fromMillis: Long = System.currentTimeMillis(),
        hour: Int = collection.scheduleHour,
        minute: Int = collection.scheduleMinute,
        dayAllowed: ((Long) -> Boolean)? = null,
    ): Long {
        val maxOffset = if (collection.dayMode == CollectionDayMode.WEEKDAYS && dayAllowed == null) 8 else 400
        val allowed = dayAllowed ?: { millis -> isDayAllowed(collection, millis) }
        repeat(maxOffset + 1) { offset ->
            val candidate = Calendar.getInstance().apply {
                timeInMillis = fromMillis
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
                set(Calendar.MINUTE, minute.coerceIn(0, 59))
            }
            if (!allowed(candidate.timeInMillis)) return@repeat
            if (candidate.timeInMillis > fromMillis) return candidate.timeInMillis
        }
        return fromMillis + 24 * 60 * 60 * 1000L
    }

    fun nextMatchingDayPrayerOffset(
        collection: AdhkarCollectionEntity,
        config: PrayerConfig,
        prayer: PrayerName,
        offsetMinutes: Int,
        fromMillis: Long,
        dayAllowed: ((Long) -> Boolean)? = null,
    ): Long? {
        if (!config.hasTimes) return null
        val zone = TimeZone.getTimeZone(PrayerTimesCalculator.zoneId(config))
        val dayMs = 24 * 60 * 60 * 1000L
        val maxOffset = if (collection.dayMode == CollectionDayMode.WEEKDAYS && dayAllowed == null) 14 else 400
        val allowed = dayAllowed ?: { millis -> isDayAllowed(collection, millis) }
        for (offset in 0..maxOffset) {
            val dayMillis = fromMillis + offset * dayMs
            val cal = Calendar.getInstance(zone).apply { timeInMillis = dayMillis }
            if (!allowed(cal.timeInMillis)) continue
            val times = PrayerTimesCalculator.timesFor(config, dayMillis) ?: continue
            val prayerAt = times.timeOf(prayer) ?: continue
            val trigger = prayerAt + offsetMinutes * 60_000L
            if (trigger > fromMillis) return trigger
        }
        return null
    }

    fun findNextEnabled(
        collections: List<AdhkarCollectionEntity>,
        fromMillis: Long = System.currentTimeMillis(),
        extraTriggers: List<Pair<AdhkarCollectionEntity, Long>> = emptyList()
    ): Pair<AdhkarCollectionEntity, Long>? {
        val scheduled = collections
            .filter { it.autoPlayAllowed && it.autoPlayEnabled }
            .filter { it.dayMode != CollectionDayMode.ITEM_HIJRI }
            .map { it to nextTriggerAt(it, fromMillis) }
        val extras = extraTriggers.filter { it.second > fromMillis }
        return (scheduled + extras).minByOrNull { it.second }
    }

    /** هل موعد تشغيل هذا القسم الآن (نفس الساعة والدقيقة ويوم الظهور)؟ */
    fun isDueAt(
        collection: AdhkarCollectionEntity,
        calendar: Calendar = Calendar.getInstance()
    ): Boolean {
        if (!collection.autoPlayAllowed || !collection.autoPlayEnabled) return false
        if (!isDayAllowed(collection, calendar)) return false
        return calendar.get(Calendar.HOUR_OF_DAY) == collection.scheduleHour &&
            calendar.get(Calendar.MINUTE) == collection.scheduleMinute
    }

    fun isAnyDueAt(
        collections: Iterable<AdhkarCollectionEntity>,
        calendar: Calendar = Calendar.getInstance()
    ): Boolean = collections.any { isDueAt(it, calendar) }

    private fun hijriOf(millis: Long): HijrahDate {
        val local = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        return HijrahDate.from(local)
    }
}
