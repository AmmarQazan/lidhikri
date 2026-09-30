package com.greendome.adhkar.service

import com.greendome.adhkar.prayer.PrayerName

internal object AdhanPlaybackGuard {
    fun shouldStartNewPlayback(
        currentPrayer: PrayerName?,
        incomingPrayer: PrayerName,
        isPlaying: Boolean,
    ): Boolean {
        if (!isPlaying) return true
        return currentPrayer != incomingPrayer
    }

    fun rescheduleFromMillis(firedAtMillis: Long, nowMillis: Long): Long =
        if (firedAtMillis > 0L) firedAtMillis else nowMillis

    /**
     * إذا رُفض تشغيل الصوت: محاولة واحدة بعد مهلة، ثم يُتجاوز هذا الموعد
     * حتى لا تتوقف السلسلة ولا تُعاد المحاولة بلا نهاية.
     */
    fun missedPlayback(started: Boolean, isRetry: Boolean): MissedPlayback = when {
        started -> MissedPlayback.NONE
        isRetry -> MissedPlayback.GIVE_UP
        else -> MissedPlayback.RETRY_ONCE
    }
}

internal enum class MissedPlayback {
    NONE,
    RETRY_ONCE,
    GIVE_UP,
}

internal const val PLAYBACK_RETRY_DELAY_MS = 60_000L
