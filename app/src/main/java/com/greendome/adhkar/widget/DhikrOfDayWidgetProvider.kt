package com.greendome.adhkar.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

class DhikrOfDayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        DhikrOfDayManager.refreshAsync(context)
    }

    override fun onEnabled(context: Context) {
        DhikrOfDayManager.refreshAsync(context)
    }

    override fun onDisabled(context: Context) {
        // يبقى الإشعار أو الذكر محفوظاً حسب إعدادات المستخدم
    }
}
