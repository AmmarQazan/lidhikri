package com.greendome.adhkar.sync

import com.greendome.adhkar.update.PlayAppUpdateEligibility

object ContentUpdateEligibility {
    fun shouldPrompt(
        localVersion: Int,
        remoteVersion: Int,
        snoozedVersion: Int,
        snoozeUntilMs: Long,
        nowMs: Long,
    ): Boolean {
        if (remoteVersion <= localVersion) return false
        return PlayAppUpdateEligibility.shouldAutoPrompt(
            availableVersionCode = remoteVersion,
            snoozedVersionCode = snoozedVersion,
            snoozeUntilMs = snoozeUntilMs,
            nowMs = nowMs,
        )
    }
}
