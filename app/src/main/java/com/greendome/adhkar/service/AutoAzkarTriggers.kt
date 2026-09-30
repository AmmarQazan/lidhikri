package com.greendome.adhkar.service

object AutoAzkarTriggers {
    object Home {
        const val COOLDOWN_MS = 3 * 60_000L
        const val REGISTER_GRACE_MS = 30_000L

        fun canPlay(now: Long, lastEventAt: Long): Boolean =
            now - lastEventAt >= COOLDOWN_MS

        /** دخول فوري بعد تسجيل السياج يعني أن المستخدم داخل المنزل، فيُتجاهل. */
        fun isFreshRegister(now: Long, armedAt: Long): Boolean =
            armedAt > 0L && now >= armedAt && now - armedAt < REGISTER_GRACE_MS
    }

    object Riding {
        const val COOLDOWN_MS = 15 * 60_000L

        /** مهلة التكرار تكفي. دخول مركبة بعد المهلة يُشغّل الذكر حتى لو ضاع حدث النزول. */
        fun canPlay(now: Long, lastPlayAt: Long): Boolean =
            now - lastPlayAt >= COOLDOWN_MS
    }
}

object AutoAzkarSkip {
    const val COOLDOWN = "cooldown"
    const val NO_ITEMS = "no_items"
    const val FRESH_REGISTER = "fresh_register"
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
    const val LOCATION_OFF = "location_off"
}
