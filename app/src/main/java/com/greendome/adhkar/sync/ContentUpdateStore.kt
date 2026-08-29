package com.greendome.adhkar.sync

import android.content.Context
import com.greendome.adhkar.update.PlayAppUpdateEligibility

class ContentUpdateStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun snooze(version: Int, nowMs: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putLong(KEY_SNOOZE_UNTIL, nowMs + PlayAppUpdateEligibility.SNOOZE_MS)
            .putInt(KEY_SNOOZE_VERSION, version)
            .apply()
    }

    fun snoozeUntilMs(): Long = prefs.getLong(KEY_SNOOZE_UNTIL, 0L)

    fun snoozeVersion(): Int = prefs.getInt(KEY_SNOOZE_VERSION, 0)

    companion object {
        private const val PREFS = "content_update_prompt"
        private const val KEY_SNOOZE_UNTIL = "snooze_until"
        private const val KEY_SNOOZE_VERSION = "snooze_version"
    }
}
