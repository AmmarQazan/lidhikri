package com.greendome.adhkar.service

object AutoAzkarTriggers {
    object Home {
        const val COOLDOWN_MS = 3 * 60_000L

        fun canPlay(now: Long, lastEventAt: Long): Boolean =
            now - lastEventAt >= COOLDOWN_MS
    }

    object Riding {
        const val COOLDOWN_MS = 15 * 60_000L
        const val TRIP_STALE_MS = 2 * 60 * 60_000L

        fun canPlay(now: Long, lastPlayAt: Long, inTrip: Boolean): Boolean {
            if (now - lastPlayAt < COOLDOWN_MS) return false
            if (inTrip && now - lastPlayAt < TRIP_STALE_MS) return false
            return true
        }

        fun tripIsStale(now: Long, lastPlayAt: Long, inTrip: Boolean): Boolean =
            inTrip && now - lastPlayAt >= TRIP_STALE_MS
    }
}

object AutoAzkarMonitorStatus {
    const val OFF = "off"
    const val NO_FINE = "no_fine"
    const val NO_PLACE = "no_place"
    const val WAITING = "waiting"
    const val GEOFENCE = "geofence"
    const val PROXIMITY = "proximity"
    const val FAILED = "failed"
    const val NO_ACTIVITY = "no_activity"
    const val OK = "ok"
}
