package com.greendome.adhkar.util

import com.greendome.adhkar.data.local.DhikrEntity
import com.greendome.adhkar.data.model.ScheduleType
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.util.Calendar

object DhikrScheduleMatcher {

  private val HIJRI_MONTHS_AR = listOf(
        "", "محرم", "صفر", "ربيع الأول", "ربيع الثاني", "جمادى الأولى", "جمادى الآخرة",
        "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    fun isActiveNow(dhikr: DhikrEntity, calendar: Calendar = Calendar.getInstance()): Boolean {
        if (dhikr.scheduleType == ScheduleType.ALWAYS) return true
        val timeOk = when (dhikr.scheduleType) {
            ScheduleType.TIME_RANGE, ScheduleType.TIME_AND_HIJRI -> isInTimeRange(dhikr, calendar)
            else -> true
        }
        val hijriOk = when (dhikr.scheduleType) {
            ScheduleType.HIJRI_RANGE, ScheduleType.TIME_AND_HIJRI -> isInHijriRange(dhikr)
            else -> true
        }
        return timeOk && hijriOk
    }

    private fun isInTimeRange(dhikr: DhikrEntity, calendar: Calendar): Boolean {
        if (dhikr.timeStartHour < 0 || dhikr.timeEndHour < 0) return true
        val nowMin = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val start = dhikr.timeStartHour * 60 + dhikr.timeStartMinute
        val end = dhikr.timeEndHour * 60 + dhikr.timeEndMinute
        return if (start <= end) nowMin in start..end else nowMin >= start || nowMin <= end
    }

    private fun isInHijriRange(dhikr: DhikrEntity): Boolean {
        if (dhikr.hijriMonth < 1) return true
        val hijri = HijrahDate.now()
        val month = hijri.get(ChronoField.MONTH_OF_YEAR)
        val day = hijri.get(ChronoField.DAY_OF_MONTH)
        if (month != dhikr.hijriMonth) return false
        val dayStart = if (dhikr.hijriDayStart > 0) dhikr.hijriDayStart else 1
        val dayEnd = if (dhikr.hijriDayEnd > 0) dhikr.hijriDayEnd else 30
        return day in dayStart..dayEnd
    }

    fun scheduleSummaryAr(dhikr: DhikrEntity): String {
        if (dhikr.scheduleLabelAr.isNotBlank()) return dhikr.scheduleLabelAr
        return when (dhikr.scheduleType) {
            ScheduleType.ALWAYS -> "دائماً"
            ScheduleType.TIME_RANGE -> formatTimeRange(dhikr)
            ScheduleType.HIJRI_RANGE -> formatHijriRange(dhikr)
            ScheduleType.TIME_AND_HIJRI -> "${formatTimeRange(dhikr)} + ${formatHijriRange(dhikr)}"
        }
    }

    private fun formatTimeRange(dhikr: DhikrEntity): String {
        if (dhikr.timeStartHour < 0) return ""
        return "${pad(dhikr.timeStartHour)}:${pad(dhikr.timeStartMinute)} – ${pad(dhikr.timeEndHour)}:${pad(dhikr.timeEndMinute)}"
    }

    private fun formatHijriRange(dhikr: DhikrEntity): String {
        if (dhikr.hijriMonth < 1) return ""
        val monthName = HIJRI_MONTHS_AR.getOrElse(dhikr.hijriMonth) { "شهر ${dhikr.hijriMonth}" }
        val start = if (dhikr.hijriDayStart > 0) dhikr.hijriDayStart else 1
        val end = if (dhikr.hijriDayEnd > 0) dhikr.hijriDayEnd else 30
        return "$start–$end $monthName"
    }

    private fun pad(n: Int) = n.toString().padStart(2, '0')

    fun todayHijriFormatted(): String {
        val h = HijrahDate.now()
        val month = HIJRI_MONTHS_AR.getOrElse(h.get(ChronoField.MONTH_OF_YEAR)) { "" }
        return "${h.get(ChronoField.DAY_OF_MONTH)} $month ${h.get(ChronoField.YEAR_OF_ERA)} هـ"
    }
}
