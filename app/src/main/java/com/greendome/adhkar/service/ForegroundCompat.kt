package com.greendome.adhkar.service

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * منبّه دقيق، وإن رُفض الإذن على أندرويد 12+ نرجع إلى منبّه مسموح في الخمول
 * حتى لا يسقط التطبيق أثناء إعادة الجدولة.
 */
internal fun AlarmManager.scheduleWakeup(triggerAt: Long, pending: PendingIntent) {
    try {
        setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
    } catch (_: SecurityException) {
        setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
    }
}

object ForegroundServiceStarts {
    /** تشغيل خدمة أمامية. يُرجع false إذا رفض أندرويد التشغيل من الخلفية. */
    fun start(context: Context, intent: Intent): Boolean =
        runCatching {
            ContextCompat.startForegroundService(context.applicationContext, intent)
        }.isSuccess

    /** إيصال أمر لخدمة تعمل. لا يرمي إذا كان التطبيق في الخلفية. */
    fun deliver(context: Context, intent: Intent): Boolean =
        runCatching {
            context.applicationContext.startService(intent)
        }.isSuccess
}

/**
 * يعلن الخدمة أمامية مع النوع المصرّح في البيان.
 * أندرويد 14 يرفض startForeground بلا نوع عندما يكون targetSdk حديثاً.
 */
fun Service.promoteForeground(id: Int, notification: Notification, type: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        startForeground(id, notification, type)
    } else if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
        type == ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
    ) {
        startForeground(id, notification, type)
    } else {
        startForeground(id, notification)
    }
}
