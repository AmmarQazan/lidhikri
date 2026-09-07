package com.greendome.adhkar.service

import android.app.KeyguardManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.greendome.adhkar.R
import com.greendome.adhkar.audio.DhikrAudioPlayer
import com.greendome.adhkar.data.AdhanAzkar
import com.greendome.adhkar.data.model.DisplayModes
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.prayer.AdhanAudioResolver
import com.greendome.adhkar.prayer.AdhanEventKind
import com.greendome.adhkar.prayer.AdhanSoundMode
import com.greendome.adhkar.prayer.AdhanVibrator
import com.greendome.adhkar.prayer.PrayerName
import com.greendome.adhkar.ui.adhan.AdhanActivity

class AdhanPlaybackService : Service() {
    private var player: DhikrAudioPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingAfterAzkar: Runnable? = null
    private var playingPrayer: PrayerName? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        player = DhikrAudioPlayer(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                finishPlayback()
                PrayerPhoneSilent.enter(this)
                return START_NOT_STICKY
            }
        }
        val prayer = runCatching {
            PrayerName.valueOf(intent?.getStringExtra(AdhanAlarmScheduler.EXTRA_PRAYER).orEmpty())
        }.getOrNull() ?: run {
            stopSelf()
            return START_NOT_STICKY
        }
        AdhanAlertNotifier.ensureChannel(this)
        val settings = SettingsRepository(this)
        val modes = settings.adhanPresentation.toDisplayModes()
        val locked = isKeyguardLocked()
        val wantLock = locked && settings.adhanAutoLockScreenEnabled &&
            !modes.audioOnly && !modes.showsTextViaNotification()
        val wantPopup = modes.showsTextViaPopup()
        val wantScreen = wantPopup || wantLock
        startForeground(
            AdhanAlertNotifier.NOTIF_ADHAN,
            buildNotification(prayer, fullScreen = wantScreen, ongoing = modes.playsAudio())
        )
        val duplicate = !AdhanPlaybackGuard.shouldStartNewPlayback(
            currentPrayer = playingPrayer,
            incomingPrayer = prayer,
            isPlaying = playingPrayer != null,
        )
        if (duplicate) {
            if (wantScreen) openScreen(prayer)
            return START_NOT_STICKY
        }
        playingPrayer = prayer
        yieldToAdhan()
        play(prayer, settings, modes, wantScreen)
        return START_NOT_STICKY
    }

    private fun yieldToAdhan() {
        AdhkarReminderService.abortActiveReminder(this)
        if (AzkarCollectionPlayService.isPlaying()) {
            AzkarCollectionPlayService.stopAutoAzkar(this)
        }
    }

    private fun play(
        prayer: PrayerName,
        settings: SettingsRepository,
        modes: DisplayModes,
        wantScreen: Boolean,
    ) {
        val alert = settings.adhanAlert(prayer)
        if (settings.adhanVibrate) AdhanVibrator.pulse(this)

        val silent = alert.resolvedSoundMode() == AdhanSoundMode.SILENT
        val wantAudio = modes.playsAudio() && !silent
        val wantNotif = modes.showsTextViaNotification()
        playingPrayer = prayer

        if (wantNotif) {
            AdhanAlertNotifier.show(this, prayer, AdhanEventKind.ADHAN, fullScreen = false)
        }
        if (wantScreen) {
            openScreen(prayer)
        }

        if (wantAudio) {
            val path = AdhanAudioResolver.resolveBlocking(this, prayer, alert)
            if (path != null) {
                player?.playAdhan(
                    pathOrUri = path,
                    settings = settings,
                    overrideSilent = alert.overrideSilent,
                ) {
                    scheduleAfterAzkar(settings, alert.afterAdhanAzkar)
                }
                return
            }
        }

        if (canPlayAfterAzkar(settings, alert.afterAdhanAzkar)) {
            scheduleAfterAzkar(settings, enabled = true)
            return
        }
        if (wantNotif) {
            stopForeground(STOP_FOREGROUND_DETACH)
        }
        playingPrayer = null
        PrayerPhoneSilent.enter(this)
        stopSelf()
    }

    private fun isKeyguardLocked(): Boolean {
        val keyguard = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        return keyguard.isKeyguardLocked
    }

    private fun canPlayAfterAzkar(settings: SettingsRepository, enabled: Boolean): Boolean =
        enabled && settings.afterAdhanAzkarEnabled

    private fun scheduleAfterAzkar(settings: SettingsRepository, enabled: Boolean) {
        cancelPendingAfterAzkar()
        if (!canPlayAfterAzkar(settings, enabled)) {
            finishPlayback()
            PrayerPhoneSilent.enter(this)
            return
        }
        playingPrayer = null
        player?.stop()
        sendBroadcast(Intent(ACTION_FINISHED).setPackage(packageName))
        val task = Runnable {
            pendingAfterAzkar = null
            startAfterAzkar()
            finishPlayback()
        }
        pendingAfterAzkar = task
        mainHandler.postDelayed(task, AdhanAzkar.AFTER_END_DELAY_MS)
    }

    private fun startAfterAzkar() {
        ContextCompat.startForegroundService(
            this,
            Intent(this, AzkarCollectionPlayService::class.java).apply {
                putExtra(AzkarCollectionPlayService.EXTRA_COLLECTION_ID, AdhanAzkar.COLLECTION_ID)
                putExtra(AzkarCollectionPlayService.EXTRA_FORCE_PLAY, true)
            }
        )
    }

    private fun cancelPendingAfterAzkar() {
        pendingAfterAzkar?.let { mainHandler.removeCallbacks(it) }
        pendingAfterAzkar = null
    }

    private fun openScreen(prayer: PrayerName) {
        val screen = Intent(this, AdhanActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AdhanAlarmScheduler.EXTRA_PRAYER, prayer.name)
        }
        startActivity(screen)
    }

    private fun buildNotification(
        prayer: PrayerName,
        fullScreen: Boolean,
        ongoing: Boolean,
    ): Notification {
        val screen = Intent(this, AdhanActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AdhanAlarmScheduler.EXTRA_PRAYER, prayer.name)
        }
        val content = PendingIntent.getActivity(
            this,
            810,
            screen,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this,
            811,
            Intent(this, AdhanPlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(this, AdhanAlertNotifier.CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                getString(R.string.adhan_now_title, AdhanAlertNotifier.prayerLabel(this, prayer))
            )
            .setContentText(getString(R.string.adhan_now_text))
            .setContentIntent(content)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(ongoing)
            .addAction(0, getString(R.string.adhan_stop), stop)
        if (fullScreen) {
            builder.setFullScreenIntent(content, true)
        }
        return builder.build()
    }

    private fun finishPlayback() {
        cancelPendingAfterAzkar()
        playingPrayer = null
        player?.stop()
        sendBroadcast(Intent(ACTION_FINISHED).setPackage(packageName))
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        cancelPendingAfterAzkar()
        playingPrayer = null
        player?.stop()
        player = null
        sendBroadcast(Intent(ACTION_FINISHED).setPackage(packageName))
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.greendome.adhkar.ADHAN_START"
        const val ACTION_STOP = "com.greendome.adhkar.ADHAN_STOP"
        const val ACTION_FINISHED = "com.greendome.adhkar.ADHAN_FINISHED"
        const val ADHAN_AZKAR_ID = AdhanAzkar.COLLECTION_ID

        @Volatile
        private var instance: AdhanPlaybackService? = null

        fun isPlaying(): Boolean = instance?.playingPrayer != null

        fun stop(context: Context) {
            val app = context.applicationContext
            val intent = Intent(app, AdhanPlaybackService::class.java).setAction(ACTION_STOP)
            runCatching { app.startService(intent) }
            runCatching { app.stopService(Intent(app, AdhanPlaybackService::class.java)) }
        }
    }
}
