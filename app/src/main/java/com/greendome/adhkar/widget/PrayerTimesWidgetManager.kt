package com.greendome.adhkar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.greendome.adhkar.MainActivity
import com.greendome.adhkar.R
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.model.ClockHourFormat
import com.greendome.adhkar.prayer.PrayerName
import com.greendome.adhkar.prayer.PrayerTimesCalculator
import com.greendome.adhkar.util.LocaleHelper
import com.greendome.adhkar.util.formatClockTime
import java.time.Instant
import java.time.ZoneId

class PrayerTimesWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        PrayerTimesWidgetManager.updateAll(context)
    }
}

object PrayerTimesWidgetManager {
    fun updateAll(context: Context) {
        val app = context.applicationContext
        val manager = AppWidgetManager.getInstance(app)
        val ids = manager.getAppWidgetIds(ComponentName(app, PrayerTimesWidgetProvider::class.java))
        if (ids.isEmpty()) return
        ids.forEach { id -> manager.updateAppWidget(id, build(app, id)) }
    }

    fun requestPin(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return false
        return manager.requestPinAppWidget(
            ComponentName(context, PrayerTimesWidgetProvider::class.java),
            null,
            null
        )
    }

    private fun build(context: Context, appWidgetId: Int): RemoteViews {
        val settings = SettingsRepository(context)
        val localized = LocaleHelper.wrap(context, settings.appLanguage)
        val views = RemoteViews(context.packageName, R.layout.widget_prayer_times)
        val config = settings.prayerConfig()
        val times = PrayerTimesCalculator.timesFor(config)
        val zone = PrayerTimesCalculator.zoneId(config)
        val format = settings.azkarClockHourFormat
        val am = localized.getString(R.string.clock_period_am)
        val pm = localized.getString(R.string.clock_period_pm)
        val city = config.location?.cityName?.takeIf { it.isNotBlank() }
        val next = PrayerTimesCalculator.nextPrayer(config)
        val highlight = ContextCompat.getColor(localized, R.color.green_primary)
        val muted = ContextCompat.getColor(localized, R.color.text_muted)
        val primary = ContextCompat.getColor(localized, R.color.text_primary)

        views.setTextViewText(
            R.id.widget_prayer_city,
            city ?: localized.getString(R.string.prayer_times_need_city)
        )
        views.setViewVisibility(
            R.id.widget_prayer_city,
            if (city != null) View.VISIBLE else View.GONE
        )
        views.setViewVisibility(
            R.id.widget_prayer_chips,
            if (times != null) View.VISIBLE else View.GONE
        )
        views.setViewVisibility(
            R.id.widget_prayer_empty,
            if (times != null) View.GONE else View.VISIBLE
        )

        if (times != null) {
            listOf(
                Chip(R.id.widget_prayer_imsak_name, R.id.widget_prayer_imsak_time, R.string.prayer_name_imsak, times.imsakMillis, false),
                Chip(R.id.widget_prayer_fajr_name, R.id.widget_prayer_fajr_time, R.string.prayer_name_fajr, times.timeOf(PrayerName.FAJR), next?.prayer == PrayerName.FAJR),
                Chip(R.id.widget_prayer_sunrise_name, R.id.widget_prayer_sunrise_time, R.string.prayer_name_sunrise, times.sunriseMillis, false),
                Chip(R.id.widget_prayer_dhuhr_name, R.id.widget_prayer_dhuhr_time, R.string.prayer_name_dhuhr, times.timeOf(PrayerName.DHUHR), next?.prayer == PrayerName.DHUHR),
                Chip(R.id.widget_prayer_asr_name, R.id.widget_prayer_asr_time, R.string.prayer_name_asr, times.timeOf(PrayerName.ASR), next?.prayer == PrayerName.ASR),
                Chip(R.id.widget_prayer_maghrib_name, R.id.widget_prayer_maghrib_time, R.string.prayer_name_maghrib, times.timeOf(PrayerName.MAGHRIB), next?.prayer == PrayerName.MAGHRIB),
                Chip(R.id.widget_prayer_isha_name, R.id.widget_prayer_isha_time, R.string.prayer_name_isha, times.timeOf(PrayerName.ISHA), next?.prayer == PrayerName.ISHA),
            ).forEach { chip ->
                bindChip(views, localized, chip, zone, format, am, pm, highlight, muted, primary)
            }
        }

        val open = PendingIntent.getActivity(
            context,
            820 + appWidgetId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_prayer_root, open)
        return views
    }

    private data class Chip(
        val nameId: Int,
        val timeId: Int,
        val nameRes: Int,
        val millis: Long?,
        val highlight: Boolean,
    )

    private fun bindChip(
        views: RemoteViews,
        context: Context,
        chip: Chip,
        zone: ZoneId,
        format: ClockHourFormat,
        am: String,
        pm: String,
        highlightColor: Int,
        mutedColor: Int,
        timeColor: Int,
    ) {
        views.setTextViewText(chip.nameId, context.getString(chip.nameRes))
        views.setTextViewText(chip.timeId, chip.millis?.let { clock(it, zone, format, am, pm) } ?: "--:--")
        views.setTextColor(chip.nameId, if (chip.highlight) highlightColor else mutedColor)
        views.setTextColor(chip.timeId, if (chip.highlight) highlightColor else timeColor)
    }

    private fun clock(
        millis: Long,
        zone: ZoneId,
        format: ClockHourFormat,
        am: String,
        pm: String,
    ): String {
        val local = Instant.ofEpochMilli(millis).atZone(zone)
        return formatClockTime(local.hour, local.minute, format, am, pm)
    }
}
