package com.greendome.adhkar.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.greendome.adhkar.data.model.MisbahaStyle

class ElectronicMisbahaWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == MisbahaWidgetManager.ACTION_INCREMENT) {
            MisbahaWidgetManager.handleIntent(context, intent, goAsync())
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        MisbahaWidgetManager.update(context, appWidgetManager, appWidgetIds, MisbahaStyle.ELECTRONIC)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        MisbahaWidgetManager.update(
            context,
            appWidgetManager,
            intArrayOf(appWidgetId),
            MisbahaStyle.ELECTRONIC
        )
    }
}
