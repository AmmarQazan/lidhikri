package com.greendome.adhkar.service

import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.greendome.adhkar.R
import com.greendome.adhkar.ui.overlay.OverlayActivity
import com.greendome.adhkar.util.LockScreenPermissions
import com.greendome.adhkar.util.RuntimePermissions

/** عرض التسبيح أو الأذكار على شاشة القفل — مساران منفصلان */
object LockScreenReminderPresenter {
    private const val TASBIH_LOCK_SCREEN_REQUEST_CODE = 90_000
    private const val AZKAR_LOCK_SCREEN_REQUEST_CODE = 95_000
    private const val AZKAR_NOTIFICATION_ID_OFFSET = 15_000

    fun showTasbih(context: Context, reminderId: Long, text: String) {
        val appContext = context.applicationContext
        val intent = OverlayActivity.lockScreenTasbihIntent(appContext, reminderId, text)
        val notifId = SilentNotificationChannels.LOCK_SCREEN_NOTIFICATION_ID_BASE + reminderId.toInt()
        present(
            context = appContext,
            reminderId = reminderId,
            text = text,
            fullScreenIntent = intent,
            notificationId = notifId,
            requestCodeBase = TASBIH_LOCK_SCREEN_REQUEST_CODE,
            notificationTitle = appContext.getString(R.string.auto_tasbih_on)
        )
    }

    fun showAzkar(context: Context, reminderId: Long, sectionTitle: String, text: String) {
        val appContext = context.applicationContext
        val intent = OverlayActivity.lockScreenAutoAzkarIntent(appContext, sectionTitle, text, reminderId)
        val notifId = AZKAR_NOTIFICATION_ID_OFFSET + reminderId.toInt()
        present(
            context = appContext,
            reminderId = reminderId,
            text = text,
            fullScreenIntent = intent,
            notificationId = notifId,
            requestCodeBase = AZKAR_LOCK_SCREEN_REQUEST_CODE,
            notificationTitle = sectionTitle.ifBlank { appContext.getString(R.string.auto_azkar_on) }
        )
    }

    private fun present(
        context: Context,
        reminderId: Long,
        text: String,
        fullScreenIntent: Intent,
        notificationId: Int,
        requestCodeBase: Int,
        notificationTitle: String
    ) {
        if (!isKeyguardLocked(context)) return
        showFullScreenNotification(
            context = context,
            notificationId = notificationId,
            notificationTitle = notificationTitle,
            text = text,
            fullScreenIntent = fullScreenIntent,
            requestCode = requestCodeBase + reminderId.toInt()
        )
    }

    private fun isKeyguardLocked(context: Context): Boolean {
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return keyguard.isKeyguardLocked
    }

    private fun showFullScreenNotification(
        context: Context,
        notificationId: Int,
        notificationTitle: String,
        text: String,
        fullScreenIntent: Intent,
        requestCode: Int
    ) {
        if (!RuntimePermissions.hasPostNotifications(context)) return
        val contentPending = PendingIntent.getActivity(
            context,
            requestCode,
            fullScreenIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(context, SilentNotificationChannels.LOCK_SCREEN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notificationTitle)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPending)
            .setSound(null)
            .setVibrate(null)
            .setDefaults(0)
            .setOnlyAlertOnce(true)
        if (LockScreenPermissions.canUseFullScreenIntent(context)) {
            builder.setFullScreenIntent(contentPending, true)
        }
        context.getSystemService(NotificationManager::class.java)
            .notify(notificationId, builder.build())
    }
}
