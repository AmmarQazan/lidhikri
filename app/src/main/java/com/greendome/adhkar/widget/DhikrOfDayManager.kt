package com.greendome.adhkar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.greendome.adhkar.MainActivity
import com.greendome.adhkar.R
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.model.DhikrOfDayDisplayMode
import com.greendome.adhkar.service.SilentNotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

object DhikrOfDayManager {
    private const val PREFS = "dhikr_of_day"
    private const val KEY_DATE = "date"
    private const val KEY_TEXT = "text"
    const val NOTIFICATION_ID = 88

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun refreshAsync(context: Context) {
        scope.launch { refresh(context) }
    }

    suspend fun refresh(context: Context) {
        val appContext = context.applicationContext
        SilentNotificationChannels.ensureCreated(appContext)
        val settings = SettingsRepository(appContext)
        if (!settings.dhikrOfDayEnabled) {
            clearLockScreenNotification(appContext)
            updateAllWidgets(appContext, "")
            return
        }

        val text = ensureTodayDhikr(appContext, settings.appLanguage)
        when (settings.dhikrOfDayDisplayMode) {
            DhikrOfDayDisplayMode.HOME_WIDGET -> {
                clearLockScreenNotification(appContext)
                updateAllWidgets(appContext, text)
            }
            DhikrOfDayDisplayMode.LOCK_SCREEN -> {
                showLockScreenNotification(appContext, text)
                updateAllWidgets(appContext, text)
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
            .apply()
        refreshAsync(context)
    }

    private suspend fun ensureTodayDhikr(context: Context, lang: String): String {
        val today = LocalDate.now().toString()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val storedDate = prefs.getString(KEY_DATE, null)
        val storedText = prefs.getString(KEY_TEXT, null)
        if (storedDate == today && !storedText.isNullOrBlank()) {
            return storedText
        }

        val db = AdhkarDatabase.get(context)
        val azkarTexts = db.azkarItemDao().getAll().map { it.textAr }
        val dhikrTexts = db.dhikrDao().getEnabledList().map { it.localizedText(lang) }
        val candidates = (azkarTexts + dhikrTexts)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        val text = candidates.randomOrNull()
            ?: context.getString(R.string.dhikr_of_day_fallback)

        prefs.edit()
            .putString(KEY_DATE, today)
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
        val views = RemoteViews(context.packageName, R.layout.widget_dhikr_of_day)
        val displayText = text.ifBlank { context.getString(R.string.dhikr_of_day_widget_empty) }
        views.setTextViewText(R.id.widget_title, context.getString(R.string.dhikr_of_day_title))
        views.setTextViewText(R.id.widget_dhikr_text, displayText)

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
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, SilentNotificationChannels.DHIKR_OF_DAY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.dhikr_of_day_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
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
