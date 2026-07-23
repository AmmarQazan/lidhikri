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

    fun dayLabelsAr(): List<String> = listOf(
        "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"
    )

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
        fromMillis: Long = System.currentTimeMillis()
    ): Pair<AdhkarCollectionEntity, Long>? {
        return collections
            .filter { it.autoPlayAllowed && it.autoPlayEnabled }
            .map { it to nextTriggerAt(it, fromMillis) }
            .minByOrNull { it.second }
    }

    fun formatScheduleAr(collection: AdhkarCollectionEntity): String {
        val days = dayLabelsAr()
            .mapIndexed { index, label -> if (collection.weekDaysMask and (1 shl index) != 0) label else null }
            .filterNotNull()
        val daysText = if (days.size == 7) "كل يوم" else days.joinToString("، ")
        val hour = collection.scheduleHour.toString().padStart(2, '0')
        val minute = collection.scheduleMinute.toString().padStart(2, '0')
        return "$daysText — $hour:$minute"
    }
}
