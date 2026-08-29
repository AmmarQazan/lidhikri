package com.greendome.adhkar.update

import android.content.Context

class PlayAppUpdateStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun snooze(versionCode: Int, nowMs: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putLong(KEY_SNOOZE_UNTIL, nowMs + PlayAppUpdateEligibility.SNOOZE_MS)
            .putInt(KEY_SNOOZE_VERSION, versionCode)
            .apply()
    }

    fun snoozeUntilMs(): Long = prefs.getLong(KEY_SNOOZE_UNTIL, 0L)

    fun snoozeVersionCode(): Int = prefs.getInt(KEY_SNOOZE_VERSION, 0)

    companion object {
        private const val PREFS = "play_app_update"
        private const val KEY_SNOOZE_UNTIL = "snooze_until"
        private const val KEY_SNOOZE_VERSION = "snooze_version"
    }
}
