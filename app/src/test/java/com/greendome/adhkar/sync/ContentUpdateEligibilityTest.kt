package com.greendome.adhkar.sync

import com.greendome.adhkar.update.PlayAppUpdateEligibility
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentUpdateEligibilityTest {

    @Test
    fun sameVersionDoesNotPrompt() {
        assertFalse(
            ContentUpdateEligibility.shouldPrompt(
                localVersion = 5,
                remoteVersion = 5,
                snoozedVersion = 0,
                snoozeUntilMs = 0L,
                nowMs = 1_000L,
            )
        )
    }

    @Test
    fun olderRemoteDoesNotPrompt() {
        assertFalse(
            ContentUpdateEligibility.shouldPrompt(
                localVersion = 6,
                remoteVersion = 5,
                snoozedVersion = 0,
                snoozeUntilMs = 0L,
                nowMs = 1_000L,
            )
        )
    }

    @Test
    fun newerRemotePrompts() {
        assertTrue(
            ContentUpdateEligibility.shouldPrompt(
                localVersion = 5,
                remoteVersion = 6,
                snoozedVersion = 0,
                snoozeUntilMs = 0L,
                nowMs = 1_000L,
            )
        )
    }

    @Test
    fun snoozedSameRemoteWaits() {
        assertFalse(
            ContentUpdateEligibility.shouldPrompt(
                localVersion = 5,
                remoteVersion = 6,
                snoozedVersion = 6,
                snoozeUntilMs = 2_000L,
                nowMs = 1_000L,
            )
        )
    }

    @Test
    fun newerThanSnoozePromptsImmediately() {
        assertTrue(
            ContentUpdateEligibility.shouldPrompt(
                localVersion = 5,
                remoteVersion = 7,
                snoozedVersion = 6,
                snoozeUntilMs = 9_000L,
                nowMs = 1_000L,
            )
        )
    }

    @Test
    fun afterOneDaySamePackPromptsAgain() {
        val snoozedAt = 1_000L
        val until = snoozedAt + PlayAppUpdateEligibility.SNOOZE_MS
        assertFalse(
            ContentUpdateEligibility.shouldPrompt(
                localVersion = 5,
                remoteVersion = 6,
                snoozedVersion = 6,
                snoozeUntilMs = until,
                nowMs = until - 1,
            )
        )
        assertTrue(
            ContentUpdateEligibility.shouldPrompt(
                localVersion = 5,
                remoteVersion = 6,
                snoozedVersion = 6,
                snoozeUntilMs = until,
                nowMs = until,
            )
        )
    }
}
