package com.greendome.adhkar.review

import android.content.Context
import java.time.LocalDate

class InAppReviewTracker(
    context: Context,
    private val clock: () -> LocalDate = { LocalDate.now() },
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val tasbihCount: Int
        get() = prefs.getInt(KEY_TASBIH, 0)

    val consecutiveDays: Int
        get() = prefs.getInt(KEY_STREAK, 0)

    val alreadyRequested: Boolean
        get() = prefs.getBoolean(KEY_REQUESTED, false)

    fun addTasbih(count: Int = 1) {
        if (count <= 0) return
        prefs.edit().putInt(KEY_TASBIH, tasbihCount + count).apply()
    }

    fun recordUsageToday() {
        val today = clock().toString()
        val last = prefs.getString(KEY_LAST_DATE, null)
        val next = InAppReviewEligibility.nextConsecutiveDays(last, today, consecutiveDays)
        prefs.edit()
            .putString(KEY_LAST_DATE, today)
            .putInt(KEY_STREAK, next)
            .apply()
    }

    fun isEligible(trigger: InAppReviewTrigger): Boolean =
        InAppReviewEligibility.shouldPrompt(
            alreadyRequested = alreadyRequested,
            tasbihCount = tasbihCount,
            consecutiveDays = consecutiveDays,
            trigger = trigger,
        )

    fun shouldSkipCooldown(nowMs: Long = System.currentTimeMillis()): Boolean {
        if (alreadyRequested) return true
        val lastFail = prefs.getLong(KEY_FAIL_AT, 0L)
        return lastFail > 0L && nowMs - lastFail < FAIL_COOLDOWN_MS
    }

    fun markRequested() {
        prefs.edit()
            .putBoolean(KEY_REQUESTED, true)
            .remove(KEY_FAIL_AT)
            .apply()
    }

    fun markAttemptFailed(nowMs: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_FAIL_AT, nowMs).apply()
    }

    companion object {
        private const val PREFS = "in_app_review"
        private const val KEY_TASBIH = "lifetime_tasbih"
        private const val KEY_LAST_DATE = "last_usage_date"
        private const val KEY_STREAK = "consecutive_days"
        private const val KEY_REQUESTED = "flow_requested"
        private const val KEY_FAIL_AT = "last_fail_at"
        private const val FAIL_COOLDOWN_MS = 24L * 60L * 60L * 1000L
    }
}
