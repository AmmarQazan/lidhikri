package com.greendome.adhkar.util

import android.content.Context
import com.greendome.adhkar.service.PrayerPhoneSilent
import com.greendome.adhkar.ui.overlay.OverlayWindow

object AppSetupNeeds {
    fun notificationsReady(context: Context): Boolean =
        RuntimePermissions.hasPostNotifications(context)

    fun overlayReady(context: Context): Boolean =
        OverlayWindow.hasPermission(context)

    fun lockScreenReady(context: Context): Boolean =
        !LockScreenPermissions.needsFullScreenIntentPermission(context)

    fun prayerSilentPolicyReady(context: Context): Boolean =
        PrayerPhoneSilent.hasPolicyAccess(context)

    fun allReady(context: Context): Boolean =
        notificationsReady(context) &&
            overlayReady(context) &&
            lockScreenReady(context) &&
            prayerSilentPolicyReady(context)
}
