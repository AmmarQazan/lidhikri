package com.greendome.adhkar.service

import android.content.Context
import com.greendome.adhkar.prayer.AdhanEvent
import com.greendome.adhkar.prayer.AdhanEventKind
import com.greendome.adhkar.prayer.AdhanEvents
import com.greendome.adhkar.prayer.PrayerName
import java.time.ZoneId

internal object AdhanFiredStore {
    private const val PREFS = "adhan_fired_events"
    private const val KEY_PRAYER = "prayer"
    private const val KEY_KIND = "kind"
    private const val KEY_AT = "at"

    fun last(context: Context): AdhanEvent? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val prayer = runCatching {
            PrayerName.valueOf(prefs.getString(KEY_PRAYER, null).orEmpty())
        }.getOrNull() ?: return null
        val kind = runCatching {
            AdhanEventKind.valueOf(prefs.getString(KEY_KIND, null).orEmpty())
        }.getOrNull() ?: return null
        val at = prefs.getLong(KEY_AT, 0L)
        if (at <= 0L) return null
        return AdhanEvent(prayer, kind, at)
    }

    fun remember(context: Context, event: AdhanEvent) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PRAYER, event.prayer.name)
            .putString(KEY_KIND, event.kind.name)
            .putLong(KEY_AT, event.atMillis)
            .apply()
    }

    fun alreadyHandled(context: Context, event: AdhanEvent, zone: ZoneId): Boolean {
        val previous = last(context) ?: return false
        return AdhanEvents.sameOccurrence(previous, event, zone)
    }
}
