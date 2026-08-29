package com.greendome.adhkar.util

import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import java.util.Calendar

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

    fun dayLabels(lang: String): List<String> {
        val symbols = java.text.DateFormatSymbols(AppLanguages.locale(lang))
        val weekdays = symbols.weekdays
        return DAY_BITS.map { weekdays[it] }
    }

    fun dayLabelsAr(): List<String> = dayLabels("ar")

    fun formatSchedule(collection: AdhkarCollectionEntity, lang: String): String {
        val labels = dayLabels(lang)
        val days = labels
            .mapIndexed { index, label -> if (collection.weekDaysMask and (1 shl index) != 0) label else null }
            .filterNotNull()
        val daysText = if (days.size == 7) everyDayLabel(lang) else {
            val sep = if (AppLanguages.isRtl(lang)) "، " else ", "
            days.joinToString(sep)
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

    fun nextTriggerAt(collection: AdhkarCollectionEntity, fromMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = fromMillis }
        repeat(8) { offset ->
            val candidate = Calendar.getInstance().apply {
                timeInMillis = fromMillis
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                set(Calendar.HOUR_OF_DAY, collection.scheduleHour)
                set(Calendar.MINUTE, collection.scheduleMinute)
            }
            if (!isDayEnabled(collection.weekDaysMask, candidate.get(Calendar.DAY_OF_WEEK))) return@repeat
            if (candidate.timeInMillis > fromMillis) return candidate.timeInMillis
        }
        return fromMillis + 24 * 60 * 60 * 1000L
    }

    fun findNextEnabled(
        collections: List<AdhkarCollectionEntity>,
        fromMillis: Long = System.currentTimeMillis(),
        extraTriggers: List<Pair<AdhkarCollectionEntity, Long>> = emptyList()
    ): Pair<AdhkarCollectionEntity, Long>? {
        val scheduled = collections
            .filter { it.autoPlayAllowed && it.autoPlayEnabled }
            .map { it to nextTriggerAt(it, fromMillis) }
        val extras = extraTriggers.filter { it.second > fromMillis }
        return (scheduled + extras).minByOrNull { it.second }
    }

    /** هل موعد تشغيل هذا القسم الآن (نفس الساعة والدقيقة ويوم الأسبوع)؟ */
    fun isDueAt(
        collection: AdhkarCollectionEntity,
        calendar: Calendar = Calendar.getInstance()
    ): Boolean {
        if (!collection.autoPlayAllowed || !collection.autoPlayEnabled) return false
        if (!isDayEnabled(collection.weekDaysMask, calendar.get(Calendar.DAY_OF_WEEK))) return false
        return calendar.get(Calendar.HOUR_OF_DAY) == collection.scheduleHour &&
            calendar.get(Calendar.MINUTE) == collection.scheduleMinute
    }

    fun isAnyDueAt(
        collections: Iterable<AdhkarCollectionEntity>,
        calendar: Calendar = Calendar.getInstance()
    ): Boolean = collections.any { isDueAt(it, calendar) }
}
