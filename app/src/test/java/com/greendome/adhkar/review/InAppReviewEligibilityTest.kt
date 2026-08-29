package com.greendome.adhkar.review

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InAppReviewEligibilityTest {

    @Test
    fun firstOpenDoesNotPrompt() {
        assertFalse(
            InAppReviewEligibility.shouldPrompt(
                alreadyRequested = false,
                tasbihCount = 0,
                consecutiveDays = 1,
                trigger = InAppReviewTrigger.APP_OPEN,
            )
        )
    }

    @Test
    fun firstOpenDoesNotPromptEvenWithHundredTasbih() {
        assertFalse(
            InAppReviewEligibility.shouldPrompt(
                alreadyRequested = false,
                tasbihCount = 100,
                consecutiveDays = 1,
                trigger = InAppReviewTrigger.APP_OPEN,
            )
        )
    }

    @Test
    fun hundredTasbihPromptsFromCounter() {
        assertTrue(
            InAppReviewEligibility.shouldPrompt(
                alreadyRequested = false,
                tasbihCount = 100,
                consecutiveDays = 1,
                trigger = InAppReviewTrigger.TASBIH,
            )
        )
    }

    @Test
    fun ninetyNineTasbihDoesNotPrompt() {
        assertFalse(
            InAppReviewEligibility.shouldPrompt(
                alreadyRequested = false,
                tasbihCount = 99,
                consecutiveDays = 1,
                trigger = InAppReviewTrigger.TASBIH,
            )
        )
    }

    @Test
    fun threeConsecutiveDaysPromptsOnOpen() {
        assertTrue(
            InAppReviewEligibility.shouldPrompt(
                alreadyRequested = false,
                tasbihCount = 0,
                consecutiveDays = 3,
                trigger = InAppReviewTrigger.APP_OPEN,
            )
        )
    }

    @Test
    fun twoConsecutiveDaysDoesNotPromptOnOpen() {
        assertFalse(
            InAppReviewEligibility.shouldPrompt(
                alreadyRequested = false,
                tasbihCount = 0,
                consecutiveDays = 2,
                trigger = InAppReviewTrigger.APP_OPEN,
            )
        )
    }

    @Test
    fun alreadyRequestedNeverPrompts() {
        assertFalse(
            InAppReviewEligibility.shouldPrompt(
                alreadyRequested = true,
                tasbihCount = 500,
                consecutiveDays = 10,
                trigger = InAppReviewTrigger.APP_OPEN,
            )
        )
        assertFalse(
            InAppReviewEligibility.shouldPrompt(
                alreadyRequested = true,
                tasbihCount = 500,
                consecutiveDays = 10,
                trigger = InAppReviewTrigger.TASBIH,
            )
        )
    }

    @Test
    fun streakStartsAtOne() {
        assertEquals(1, InAppReviewEligibility.nextConsecutiveDays(null, "2026-08-29", 0))
        assertEquals(1, InAppReviewEligibility.nextConsecutiveDays("", "2026-08-29", 5))
    }

    @Test
    fun sameDayKeepsStreak() {
        assertEquals(2, InAppReviewEligibility.nextConsecutiveDays("2026-08-29", "2026-08-29", 2))
    }

    @Test
    fun nextDayIncrementsStreak() {
        assertEquals(3, InAppReviewEligibility.nextConsecutiveDays("2026-08-28", "2026-08-29", 2))
    }

    @Test
    fun gapResetsStreak() {
        assertEquals(1, InAppReviewEligibility.nextConsecutiveDays("2026-08-26", "2026-08-29", 2))
    }
}
