package com.greendome.adhkar.prayer

import android.content.Context
import com.greendome.adhkar.data.SettingsRepository

object PrayerRespectGate {
    const val AFTER_PRAYER_COLLECTION_ID = "after_prayer"

    fun config(context: Context): PrayerConfig = SettingsRepository(context).prayerConfig()

    fun isQuiet(context: Context, atMillis: Long = System.currentTimeMillis()): Boolean =
        PrayerQuietWindows.isQuiet(config(context), atMillis)

    fun delayPastQuiet(context: Context, triggerAt: Long): Long =
        PrayerQuietWindows.delayPastQuiet(config(context), triggerAt)

    fun nextStatusChangeAt(context: Context, atMillis: Long = System.currentTimeMillis()): Long? =
        PrayerQuietWindows.nextStatusChangeAt(config(context), atMillis)
}
