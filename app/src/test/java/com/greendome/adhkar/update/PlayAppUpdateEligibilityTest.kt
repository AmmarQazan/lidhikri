package com.greendome.adhkar.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayAppUpdateEligibilityTest {

    @Test
    fun playStoreInstallersAreRecognized() {
        assertTrue(PlayAppUpdateEligibility.isPlayInstaller("com.android.vending"))
        assertTrue(PlayAppUpdateEligibility.isPlayInstaller("com.google.android.feedback"))
        assertFalse(PlayAppUpdateEligibility.isPlayInstaller(null))
        assertFalse(PlayAppUpdateEligibility.isPlayInstaller("com.android.shell"))
    }

    @Test
    fun firstAvailableVersionPrompts() {
        assertTrue(
            PlayAppUpdateEligibility.shouldAutoPrompt(
                availableVersionCode = 8,
                snoozedVersionCode = 0,
                snoozeUntilMs = 0L,
                nowMs = 1_000L,
            )
        )
    }

    @Test
    fun snoozedSameVersionWaits() {
        assertFalse(
            PlayAppUpdateEligibility.shouldAutoPrompt(
                availableVersionCode = 8,
                snoozedVersionCode = 8,
                snoozeUntilMs = 2_000L,
                nowMs = 1_000L,
            )
        )
    }

    @Test
    fun snoozeExpiryPromptsAgain() {
        assertTrue(
            PlayAppUpdateEligibility.shouldAutoPrompt(
                availableVersionCode = 8,
                snoozedVersionCode = 8,
                snoozeUntilMs = 500L,
                nowMs = 1_000L,
            )
        )
    }

    @Test
    fun newerVersionIgnoresPreviousSnooze() {
        assertTrue(
            PlayAppUpdateEligibility.shouldAutoPrompt(
                availableVersionCode = 9,
                snoozedVersionCode = 8,
                snoozeUntilMs = 9_000L,
                nowMs = 1_000L,
            )
        )
    }

    @Test
    fun missingVersionDoesNotPrompt() {
        assertFalse(
            PlayAppUpdateEligibility.shouldAutoPrompt(
                availableVersionCode = 0,
                snoozedVersionCode = 0,
                snoozeUntilMs = 0L,
                nowMs = 1_000L,
            )
        )
    }
}
