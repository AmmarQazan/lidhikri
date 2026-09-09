package com.greendome.adhkar.service

import com.greendome.adhkar.data.HomeAzkar
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoAzkarTriggersTest {

    @Test
    fun homeCooldownBlocksRepeats() {
        val first = 1_000_000L
        assertFalse(AutoAzkarTriggers.Home.canPlay(first + 60_000L, first))
        assertTrue(AutoAzkarTriggers.Home.canPlay(first + AutoAzkarTriggers.Home.COOLDOWN_MS, first))
    }

    @Test
    fun ridingCooldownBlocksRepeats() {
        val first = 2_000_000L
        assertFalse(
            AutoAzkarTriggers.Riding.canPlay(first + 60_000L, first, inTrip = false)
        )
        assertTrue(
            AutoAzkarTriggers.Riding.canPlay(
                first + AutoAzkarTriggers.Riding.COOLDOWN_MS,
                first,
                inTrip = false,
            )
        )
    }

    @Test
    fun ridingInTripBlocksUntilStaleEvenAfterCooldown() {
        val first = 3_000_000L
        val afterCooldown = first + AutoAzkarTriggers.Riding.COOLDOWN_MS
        assertFalse(AutoAzkarTriggers.Riding.canPlay(afterCooldown, first, inTrip = true))
        assertFalse(AutoAzkarTriggers.Riding.tripIsStale(afterCooldown, first, inTrip = true))
        val staleAt = first + AutoAzkarTriggers.Riding.TRIP_STALE_MS
        assertTrue(AutoAzkarTriggers.Riding.tripIsStale(staleAt, first, inTrip = true))
        assertTrue(AutoAzkarTriggers.Riding.canPlay(staleAt, first, inTrip = true))
    }

    @Test
    fun homeCoordinatesIgnoreBlankLabelAndRejectZeroZero() {
        assertTrue(HomeAzkar.hasCoordinates(24.7136, 46.6753))
        assertFalse(HomeAzkar.hasCoordinates(0.0, 0.0))
        assertFalse(HomeAzkar.hasCoordinates(91.0, 46.0))
        assertFalse(HomeAzkar.hasCoordinates(24.0, 181.0))
    }
}
