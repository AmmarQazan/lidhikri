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
}
