package com.greendome.adhkar.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
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
        if (!promoteForeground()) {
            stopSelf()
            return
        }
        startTicker()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP || !promoteForeground()) {
            ticker?.cancel()
            stopForeground(STOP_FOREGROUND_REMOVE)
            NextAdhanNotifier.cancel(this)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun promoteForeground(): Boolean {
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
        startForeground(SilentNotificationChannels.NEXT_ADHAN_NOTIFICATION_ID, notification)
        return true
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
                startForeground(
                    SilentNotificationChannels.NEXT_ADHAN_NOTIFICATION_ID,
                    NextAdhanNotifier.build(this@NextAdhanService, status = status),
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
                    app.startService(
                        Intent(app, NextAdhanService::class.java).apply { action = ACTION_STOP },
                    )
                } else {
                    NextAdhanNotifier.cancel(app)
                }
                return
            }
            runCatching {
                ContextCompat.startForegroundService(
                    app,
                    Intent(app, NextAdhanService::class.java).apply { action = ACTION_START },
                )
            }.onFailure {
                NextAdhanNotifier.show(app, status)
            }
        }
    }
}
