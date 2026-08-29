package com.greendome.adhkar.review

import java.time.LocalDate

enum class InAppReviewTrigger {
    APP_OPEN,
    TASBIH,
}

object InAppReviewEligibility {
    const val TASBIH_THRESHOLD = 100
    const val CONSECUTIVE_DAYS_THRESHOLD = 3

    fun nextConsecutiveDays(lastUsageDateIso: String?, todayIso: String, currentStreak: Int): Int {
        if (lastUsageDateIso.isNullOrBlank()) return 1
        if (lastUsageDateIso == todayIso) return maxOf(currentStreak, 1)
        val last = runCatching { LocalDate.parse(lastUsageDateIso) }.getOrNull() ?: return 1
        val today = runCatching { LocalDate.parse(todayIso) }.getOrNull() ?: return 1
        return if (last.plusDays(1) == today) currentStreak + 1 else 1
    }

    fun shouldPrompt(
        alreadyRequested: Boolean,
        tasbihCount: Int,
        consecutiveDays: Int,
        trigger: InAppReviewTrigger,
    ): Boolean {
        if (alreadyRequested) return false
        return when (trigger) {
            InAppReviewTrigger.APP_OPEN -> consecutiveDays >= CONSECUTIVE_DAYS_THRESHOLD
            InAppReviewTrigger.TASBIH -> tasbihCount >= TASBIH_THRESHOLD
        }
    }
}
