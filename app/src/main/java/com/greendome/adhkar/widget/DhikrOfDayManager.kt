package com.greendome.adhkar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.greendome.adhkar.MainActivity
import com.greendome.adhkar.R
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.model.DhikrOfDayDisplayMode
import com.greendome.adhkar.data.model.DhikrOfDayTextColor
import com.greendome.adhkar.data.model.MisbahaWidgetBackground
import com.greendome.adhkar.service.SilentNotificationChannels
import com.greendome.adhkar.util.LocaleHelper
import com.greendome.adhkar.util.RuntimePermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

object DhikrOfDayManager {
    private const val PREFS = "dhikr_of_day"
    private const val KEY_DATE = "date"
    private const val KEY_TEXT = "text"
    private const val KEY_LANG = "lang"
    const val NOTIFICATION_ID = SilentNotificationChannels.DHIKR_OF_DAY_NOTIFICATION_ID

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun refreshAsync(context: Context) {
        scope.launch { refresh(context) }
    }

    suspend fun refresh(context: Context) {
        val appContext = context.applicationContext
        SilentNotificationChannels.ensureCreated(appContext)
        val settings = SettingsRepository(appContext)
        val lang = settings.appLanguage
        val localized = LocaleHelper.wrap(appContext, lang)
        val text = ensureTodayDhikr(appContext, lang)
        updateAllWidgets(localized, text)
        when (settings.dhikrOfDayDisplayMode) {
            DhikrOfDayDisplayMode.HOME_WIDGET -> {
                clearLockScreenNotification(appContext)
            }
            DhikrOfDayDisplayMode.LOCK_SCREEN -> {
                if (settings.dhikrOfDayEnabled) {
                    showLockScreenNotification(localized, text)
                } else {
                    clearLockScreenNotification(appContext)
                }
            }
        }
    }

    suspend fun getTodayText(context: Context): String {
        val settings = SettingsRepository(context)
        return ensureTodayDhikr(context.applicationContext, settings.appLanguage)
    }

    fun forceNewDhikr(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_DATE)
            .remove(KEY_LANG)
            .remove(KEY_TEXT)
            .apply()
        refreshAsync(context)
    }

    private suspend fun ensureTodayDhikr(context: Context, lang: String): String {
        val today = LocalDate.now().toString()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val storedDate = prefs.getString(KEY_DATE, null)
        val storedLang = prefs.getString(KEY_LANG, null)
        val storedText = prefs.getString(KEY_TEXT, null)
        if (storedDate == today && storedLang == lang && !storedText.isNullOrBlank()) {
            return storedText
        }

        val db = AdhkarDatabase.get(context)
        val azkarTexts = db.azkarItemDao().getAll().map { it.localizedText(lang) }
        val dhikrTexts = db.dhikrDao().getEnabledList().map { it.localizedText(lang) }
        val candidates = (azkarTexts + dhikrTexts)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        val text = candidates.randomOrNull()
            ?: LocaleHelper.wrap(context, lang).getString(R.string.dhikr_of_day_fallback)

        prefs.edit()
            .putString(KEY_DATE, today)
            .putString(KEY_LANG, lang)
            .putString(KEY_TEXT, text)
            .apply()
        return text
    }

    fun updateAllWidgets(context: Context, text: String) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, DhikrOfDayWidgetProvider::class.java)
        val ids = appWidgetManager.getAppWidgetIds(component)
        if (ids.isEmpty()) return

        val views = buildRemoteViews(context, text)
        ids.forEach { id ->
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    fun buildRemoteViews(context: Context, text: String): RemoteViews {
        val settings = SettingsRepository(context)
        val localized = LocaleHelper.wrap(context, settings.appLanguage)
        val views = RemoteViews(context.packageName, R.layout.widget_dhikr_of_day)
        val displayText = text.ifBlank { localized.getString(R.string.dhikr_of_day_widget_empty) }
        val background = settings.dhikrOfDayWidgetBackground
        val textColor = settings.dhikrOfDayWidgetTextColor
        val fontSp = settings.dhikrOfDayWidgetFontSizeSp.toFloat()
        val titleSp = (fontSp - 1f).coerceAtLeast(11f)
        views.setInt(R.id.widget_root, "setBackgroundResource", background.drawableRes())
        views.setTextViewText(R.id.widget_title, localized.getString(R.string.dhikr_of_day_title))
        views.setTextViewText(R.id.widget_dhikr_text, displayText)
        views.setTextColor(R.id.widget_title, textColor.resolveTitle(context, background))
        views.setTextColor(R.id.widget_dhikr_text, textColor.resolveBody(context, background))
        views.setTextViewTextSize(R.id.widget_title, TypedValue.COMPLEX_UNIT_SP, titleSp)
        views.setTextViewTextSize(R.id.widget_dhikr_text, TypedValue.COMPLEX_UNIT_SP, fontSp)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        return views
    }

    private fun showLockScreenNotification(context: Context, text: String) {
        if (!RuntimePermissions.hasPostNotifications(context)) return
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = SilentNotificationChannels.applyAppIcon(
            NotificationCompat.Builder(context, SilentNotificationChannels.DHIKR_OF_DAY),
            context,
        )
            .setContentTitle(context.getString(R.string.dhikr_of_day_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun clearLockScreenNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    fun requestPinWidget(context: Context): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return false
        val appWidgetManager = AppWidgetManager.getInstance(context)
        if (!appWidgetManager.isRequestPinAppWidgetSupported) return false
        val provider = ComponentName(context, DhikrOfDayWidgetProvider::class.java)
        return appWidgetManager.requestPinAppWidget(provider, null, null)
    }
}

private fun DhikrOfDayTextColor.resolveTitle(
    context: Context,
    background: MisbahaWidgetBackground,
): Int = when (this) {
    DhikrOfDayTextColor.AUTO -> background.dhikrTitleColor(context)
    else -> resolveFixed(context)
}

private fun DhikrOfDayTextColor.resolveBody(
    context: Context,
    background: MisbahaWidgetBackground,
): Int = when (this) {
    DhikrOfDayTextColor.AUTO -> background.dhikrBodyColor(context)
    else -> resolveFixed(context)
}

private fun DhikrOfDayTextColor.resolveFixed(context: Context): Int = when (this) {
    DhikrOfDayTextColor.AUTO,
    DhikrOfDayTextColor.BLACK -> ContextCompat.getColor(context, R.color.text_primary)
    DhikrOfDayTextColor.WHITE -> 0xFFFFFFFF.toInt()
    DhikrOfDayTextColor.GOLD -> ContextCompat.getColor(context, R.color.gold_dome)
    DhikrOfDayTextColor.GREEN -> 0xFF1F5A3F.toInt()
    DhikrOfDayTextColor.CREAM -> 0xFFF3E6C8.toInt()
    DhikrOfDayTextColor.BROWN -> 0xFF6D4C41.toInt()
    DhikrOfDayTextColor.NAVY -> 0xFF1A365D.toInt()
    DhikrOfDayTextColor.TEAL -> 0xFF0F766E.toInt()
    DhikrOfDayTextColor.MAROON -> 0xFF8B3A2A.toInt()
    DhikrOfDayTextColor.GRAY -> 0xFF5A5A5A.toInt()
    DhikrOfDayTextColor.AMBER -> 0xFFC67A2A.toInt()
}

private fun MisbahaWidgetBackground.dhikrTitleColor(context: Context): Int = when (this) {
    MisbahaWidgetBackground.TRANSPARENT -> contrastTextColor()
    else -> ContextCompat.getColor(context, R.color.gold_dome)
}

private fun MisbahaWidgetBackground.dhikrBodyColor(context: Context): Int = when (this) {
    MisbahaWidgetBackground.DARK,
    MisbahaWidgetBackground.TRANSPARENT -> contrastTextColor()
    else -> ContextCompat.getColor(context, R.color.text_primary)
}
