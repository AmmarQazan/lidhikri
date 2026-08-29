package com.greendome.adhkar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.TypedValue
import android.widget.RemoteViews
import com.greendome.adhkar.R
import com.greendome.adhkar.data.DailyStatsRepository
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.model.MisbahaStyle
import com.greendome.adhkar.data.model.MisbahaWidgetBackground
import com.greendome.adhkar.ui.screens.toElectronicColors
import com.greendome.adhkar.review.InAppReviewTracker
import com.greendome.adhkar.util.LocaleHelper
import com.greendome.adhkar.util.MisbahaFeedback
import com.greendome.adhkar.util.formatDigits
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object MisbahaWidgetManager {
    const val ACTION_INCREMENT = "com.greendome.adhkar.widget.MISBAHA_INCREMENT"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()

    fun increment(context: Context, pendingResult: BroadcastReceiver.PendingResult? = null) {
        val appContext = context.applicationContext
        var counted = false
        synchronized(lock) {
            val settings = SettingsRepository(appContext)
            val target = settings.misbahaWidgetTarget
            val current = settings.misbahaWidgetCount
            if (!settings.misbahaRepeatEnabled && current >= target) {
                updateAll(appContext)
                pendingResult?.finish()
                return
            }
            settings.misbahaWidgetCount = current + 1
            if (settings.misbahaRepeatEnabled && settings.misbahaWidgetCount >= target) {
                settings.misbahaWidgetCount = 0
            }
            counted = true
            MisbahaFeedback.perform(appContext, settings.misbahaFeedbackMode)
        }
        if (!counted) {
            pendingResult?.finish()
            return
        }
        scope.launch {
            try {
                DailyStatsRepository(AdhkarDatabase.get(appContext)).incrementMisbahaToday()
                InAppReviewTracker(appContext).addTasbih(1)
                updateAll(appContext)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    fun resetCount(context: Context) {
        synchronized(lock) {
            SettingsRepository(context.applicationContext).misbahaWidgetCount = 0
        }
        updateAll(context)
    }

    fun updateAll(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        update(
            appContext,
            manager,
            manager.getAppWidgetIds(ComponentName(appContext, TraditionalMisbahaWidgetProvider::class.java)),
            MisbahaStyle.TRADITIONAL
        )
        update(
            appContext,
            manager,
            manager.getAppWidgetIds(ComponentName(appContext, ElectronicMisbahaWidgetProvider::class.java)),
            MisbahaStyle.ELECTRONIC
        )
    }

    fun update(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        style: MisbahaStyle,
    ) {
        if (appWidgetIds.isEmpty()) return
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildRemoteViews(context, appWidgetManager, id, style))
        }
    }

    fun requestPin(context: Context, style: MisbahaStyle): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val appWidgetManager = AppWidgetManager.getInstance(context)
        if (!appWidgetManager.isRequestPinAppWidgetSupported) return false
        val providerClass = if (style == MisbahaStyle.TRADITIONAL) {
            TraditionalMisbahaWidgetProvider::class.java
        } else {
            ElectronicMisbahaWidgetProvider::class.java
        }
        return appWidgetManager.requestPinAppWidget(ComponentName(context, providerClass), null, null)
    }

    fun handleIntent(
        context: Context,
        intent: Intent,
        pendingResult: BroadcastReceiver.PendingResult,
    ): Boolean {
        if (intent.action != ACTION_INCREMENT) return false
        increment(context, pendingResult)
        return true
    }

    private fun buildRemoteViews(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        style: MisbahaStyle,
    ): RemoteViews {
        val settings = SettingsRepository(context)
        val localized = LocaleHelper.wrap(context, settings.appLanguage)
        val count = settings.misbahaWidgetCount
        val target = settings.misbahaWidgetTarget
        val remaining = (target - count).coerceAtLeast(0)
        val digits = settings.numberDigitStyle
        val layout = if (style == MisbahaStyle.TRADITIONAL) {
            R.layout.widget_misbaha_traditional
        } else {
            R.layout.widget_misbaha_electronic
        }
        val views = RemoteViews(context.packageName, layout)
        val background = settings.misbahaWidgetBackground
        views.setInt(R.id.widget_misbaha_root, "setBackgroundResource", background.drawableRes())
        val contrast = background.contrastTextColor()

        val size = widgetSizePx(context, appWidgetManager, appWidgetId, style)
        if (style == MisbahaStyle.TRADITIONAL) {
            views.setTextViewText(
                R.id.widget_misbaha_progress,
                localized.getString(
                    R.string.misbaha_progress,
                    count.formatDigits(digits),
                    target.formatDigits(digits)
                )
            )
            views.setTextViewText(
                R.id.widget_misbaha_remaining,
                localized.getString(R.string.misbaha_remaining, remaining.formatDigits(digits))
            )
            views.setTextColor(R.id.widget_misbaha_progress, contrast)
            views.setTextColor(R.id.widget_misbaha_remaining, contrast)
            val beads = MisbahaWidgetRenderer.drawTraditionalBeads(
                width = size.first,
                height = size.second,
                count = count,
                target = target
            )
            views.setImageViewBitmap(R.id.widget_misbaha_beads, beads)
        } else {
            views.setTextViewText(
                R.id.widget_misbaha_count,
                count.formatDigits(digits)
            )
            views.setTextViewText(
                R.id.widget_misbaha_progress,
                localized.getString(R.string.misbaha_counter_of, target.formatDigits(digits))
            )
            val colors = settings.misbahaElectronicTheme.toElectronicColors()
            val graphic = MisbahaWidgetRenderer.drawElectronicGraphic(
                width = size.first,
                height = size.second,
                count = count,
                target = target,
                colors = colors,
            )
            views.setImageViewBitmap(R.id.widget_misbaha_graphic, graphic)
            val electronicText = if (background == MisbahaWidgetBackground.DARK) contrast else colors.text
            views.setTextColor(R.id.widget_misbaha_count, electronicText)
            views.setTextColor(R.id.widget_misbaha_progress, electronicText)
        }

        val clickIntent = Intent(context, providerClass(style)).apply {
            action = ACTION_INCREMENT
            setPackage(context.packageName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            4000 + appWidgetId + style.ordinal * 1000,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_misbaha_root, pendingIntent)
        views.setOnClickPendingIntent(R.id.widget_misbaha_center, pendingIntent)
        views.setOnClickPendingIntent(R.id.widget_misbaha_progress, pendingIntent)
        if (style == MisbahaStyle.ELECTRONIC) {
            views.setOnClickPendingIntent(R.id.widget_misbaha_stage, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_misbaha_graphic, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_misbaha_count, pendingIntent)
        } else {
            views.setOnClickPendingIntent(R.id.widget_misbaha_beads, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_misbaha_remaining, pendingIntent)
        }
        return views
    }

    private fun providerClass(style: MisbahaStyle) = if (style == MisbahaStyle.TRADITIONAL) {
        TraditionalMisbahaWidgetProvider::class.java
    } else {
        ElectronicMisbahaWidgetProvider::class.java
    }

    private fun widgetSizePx(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        style: MisbahaStyle,
    ): Pair<Int, Int> {
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val defaultWidth = if (style == MisbahaStyle.TRADITIONAL) 220 else 130
        val defaultHeight = if (style == MisbahaStyle.TRADITIONAL) 110 else 130
        val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
            .takeIf { it > 0 } ?: defaultWidth
        val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
            .takeIf { it > 0 } ?: defaultHeight
        val density = context.resources.displayMetrics
        val widthPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widthDp.toFloat(), density).toInt()
        val heightPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, heightDp.toFloat(), density).toInt()
        return widthPx.coerceIn(160, 900) to heightPx.coerceIn(88, 900)
    }
}

internal fun MisbahaWidgetBackground.drawableRes(): Int = when (this) {
    MisbahaWidgetBackground.WHITE -> R.drawable.widget_misbaha_background
    MisbahaWidgetBackground.CREAM -> R.drawable.widget_misbaha_background_cream
    MisbahaWidgetBackground.GREEN -> R.drawable.widget_misbaha_background_green
    MisbahaWidgetBackground.DARK -> R.drawable.widget_misbaha_background_dark
    MisbahaWidgetBackground.TRANSPARENT -> R.drawable.widget_misbaha_background_transparent
}

internal fun MisbahaWidgetBackground.contrastTextColor(): Int = when (this) {
    MisbahaWidgetBackground.DARK -> 0xFFF3E6C8.toInt()
    else -> 0xFF1F5A3F.toInt()
}
