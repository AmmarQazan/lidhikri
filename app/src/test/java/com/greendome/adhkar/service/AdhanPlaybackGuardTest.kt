package com.greendome.adhkar.service

import com.greendome.adhkar.prayer.PrayerName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdhanPlaybackGuardTest {
    @Test
    fun firstStartAlwaysPlays() {
        assertTrue(
            AdhanPlaybackGuard.shouldStartNewPlayback(
                currentPrayer = null,
                incomingPrayer = PrayerName.DHUHR,
                isPlaying = false,
            )
        )
    }

    @Test
    fun duplicateStartOfSamePrayerIsIgnored() {
        assertFalse(
            AdhanPlaybackGuard.shouldStartNewPlayback(
                currentPrayer = PrayerName.MAGHRIB,
                incomingPrayer = PrayerName.MAGHRIB,
                isPlaying = true,
            )
        )
    }

    @Test
    fun nextPrayerReplacesCurrentPlayback() {
        assertTrue(
            AdhanPlaybackGuard.shouldStartNewPlayback(
                currentPrayer = PrayerName.ASR,
                incomingPrayer = PrayerName.MAGHRIB,
                isPlaying = true,
            )
        )
    }

    @Test
    fun rescheduleUsesFiredEventTimeSoEarlyAlarmDoesNotRepeat() {
        val fajr = 1_000_000L
        val twoSecondsEarly = fajr - 2_000L
        assertEquals(fajr, AdhanPlaybackGuard.rescheduleFromMillis(fajr, twoSecondsEarly))
    }

    @Test
    fun rescheduleFallsBackToNowWhenEventTimeMissing() {
        val now = 5_000L
        assertEquals(now, AdhanPlaybackGuard.rescheduleFromMillis(0L, now))
    }
}
