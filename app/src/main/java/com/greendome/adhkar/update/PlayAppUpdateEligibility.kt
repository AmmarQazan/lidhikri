package com.greendome.adhkar.update

object PlayAppUpdateEligibility {
    const val SNOOZE_MS = 24L * 60L * 60L * 1000L

    fun isPlayInstaller(installerPackage: String?): Boolean =
        installerPackage == "com.android.vending" ||
            installerPackage == "com.google.android.feedback"

    /**
     * بعد «لاحقاً» تُخفى النافذة ليوم كامل لنفس الإصدار، ثم تُطلب من جديد.
     * إصدار أحدث يُظهر النافذة فوراً حتى قبل انتهاء اليوم.
     */
    fun shouldAutoPrompt(
        availableVersionCode: Int,
        snoozedVersionCode: Int,
        snoozeUntilMs: Long,
        nowMs: Long,
    ): Boolean {
        if (availableVersionCode <= 0) return false
        if (availableVersionCode != snoozedVersionCode) return true
        return nowMs >= snoozeUntilMs
    }
}
