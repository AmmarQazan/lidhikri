package com.greendome.adhkar.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.prayer.NextAdhanStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NextAdhanService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var ticker: Job? = null

    override fun onCreate() {
        super.onCreate()
        SilentNotificationChannels.ensureCreated(this)
        if (!promoteStatus()) {
            acknowledgeThenStop()
            return
        }
        startTicker()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP || !promoteStatus()) {
            acknowledgeThenStop()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun promoteStatus(): Boolean {
        val status = NextAdhanStatus.resolve(SettingsRepository(this).prayerConfig())
        val notification = if (status == null) {
            null
        } else {
            NextAdhanNotifier.build(this, status = status)
        }
        if (notification == null) {
            NextAdhanNotifier.cancel(this)
            return false
        }
        promoteForeground(
            SilentNotificationChannels.NEXT_ADHAN_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        return true
    }

    private fun acknowledgeThenStop() {
        promoteForeground(
            SilentNotificationChannels.NEXT_ADHAN_NOTIFICATION_ID,
            SilentNotificationChannels.shell(this, SilentNotificationChannels.NEXT_ADHAN),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        ticker?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        NextAdhanNotifier.cancel(this)
        stopSelf()
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (true) {
                val status = NextAdhanStatus.resolve(SettingsRepository(this@NextAdhanService).prayerConfig())
                if (status == null) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    NextAdhanNotifier.cancel(this@NextAdhanService)
                    stopSelf()
                    return@launch
                }
                promoteForeground(
                    SilentNotificationChannels.NEXT_ADHAN_NOTIFICATION_ID,
                    NextAdhanNotifier.build(this@NextAdhanService, status = status),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
                val now = System.currentTimeMillis()
                val toMinute = 60_000L - (now % 60_000L)
                val toAdhan = status.atMillis - now
                val wait = if (toAdhan in 1 until toMinute) toAdhan else toMinute
                delay(wait.coerceAtLeast(1L))
            }
        }
    }

    override fun onDestroy() {
        ticker?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"

        fun sync(context: Context) {
            val app = context.applicationContext
            SilentNotificationChannels.ensureCreated(app)
            val status = NextAdhanStatus.resolve(SettingsRepository(app).prayerConfig())
            if (status == null) {
                if (ServiceRunningHelper.isRunning(app, NextAdhanService::class.java)) {
                    val stop = Intent(app, NextAdhanService::class.java).apply { action = ACTION_STOP }
                    if (!ForegroundServiceStarts.deliver(app, stop)) {
                        ForegroundServiceStarts.start(app, stop)
                    }
                } else {
                    NextAdhanNotifier.cancel(app)
                }
                return
            }
            if (!ForegroundServiceStarts.start(
                    app,
                    Intent(app, NextAdhanService::class.java).apply { action = ACTION_START },
                )
            ) {
                NextAdhanNotifier.show(app, status)
            }
        }
    }
}
