package com.greendome.adhkar.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.greendome.adhkar.MainActivity
import com.greendome.adhkar.R
import com.greendome.adhkar.util.LocaleHelper
import com.greendome.adhkar.util.formatDigits
import com.greendome.adhkar.audio.CallStateMonitor
import com.greendome.adhkar.audio.DhikrAudioPlayer
import com.greendome.adhkar.audio.DhikrPlaybackResolver
import com.greendome.adhkar.audio.playResolved
import com.greendome.adhkar.data.DailyStatsRepository
import com.greendome.adhkar.data.DhikrRepository
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.DhikrEntity
import com.greendome.adhkar.data.model.AudioSourceType
import com.greendome.adhkar.data.model.ReminderDisplayStyle
import com.greendome.adhkar.ui.overlay.OverlayActivity
import com.greendome.adhkar.ui.overlay.OverlayWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AdhkarReminderService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var settings: SettingsRepository
    private lateinit var dhikrRepo: DhikrRepository
    private lateinit var statsRepo: DailyStatsRepository
    private lateinit var audioPlayer: DhikrAudioPlayer
    private lateinit var callMonitor: CallStateMonitor
    private val audioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }
    private var notificationJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
        dhikrRepo = DhikrRepository(AdhkarDatabase.get(this))
        statsRepo = DailyStatsRepository(AdhkarDatabase.get(this))
        audioPlayer = DhikrAudioPlayer(this)
        callMonitor = CallStateMonitor(this)
        callMonitor.start()
        SilentNotificationChannels.ensureCreated(this)
        SilentNotificationChannels.cancelDhikrAlerts(this)
        updateServiceNotification()
        startNotificationTicker()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                settings.isServiceEnabled = true
                rescheduleAndUpdateNotification()
            }
            ACTION_STOP -> {
                settings.isServiceEnabled = false
                notificationJob?.cancel()
                ReminderScheduler.cancel(this)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_REFRESH -> updateServiceNotification()
            ACTION_TRIGGER -> scope.launch { triggerDhikr() }
        }
        return START_STICKY
    }

    private suspend fun triggerDhikr() {
        SilentNotificationChannels.cancelDhikrAlerts(this)

        if (shouldSkipPlayback()) {
            rescheduleAndUpdateNotification()
            return
        }

        val list = dhikrRepo.getActiveNowList()
        if (list.isEmpty()) {
            rescheduleAndUpdateNotification()
            return
        }

        val dhikr = pickDhikr(list)
        statsRepo.incrementTasbihToday()
        val modes = resolveDisplayModes(dhikr)
        val lang = settings.appLanguage
        val text = dhikr.localizedText(lang)

        if (modes.showsTextViaNotification()) {
            showSilentTextNotification(dhikr.id, text)
        } else {
            var textShown = false
            if (modes.showsTextViaLockScreen()) {
                showLockScreen(dhikr.id, text)
                textShown = true
            }
            if (modes.showsTextViaPopup()) {
                showPopup(dhikr.id, text)
                textShown = true
            }
            if (!textShown && modes.audioWithText && !modes.audioOnly) {
                showPopup(dhikr.id, text)
            }
        }

        if (modes.playsAudio() && dhikr.audioSourceType != AudioSourceType.NONE) {
            playAudioFor(dhikr)
        }

        rescheduleAndUpdateNotification()
    }

    private fun rescheduleAndUpdateNotification() {
        ReminderScheduler.scheduleNext(this)
        updateServiceNotification()
    }

    private fun startNotificationTicker() {
        notificationJob?.cancel()
        notificationJob = scope.launch {
            while (true) {
                updateServiceNotification()
                val now = System.currentTimeMillis()
                delay(60_000 - (now % 60_000))
            }
        }
    }

    private fun updateServiceNotification() {
        startForeground(NOTIF_SERVICE, buildServiceNotification())
    }

    private fun localizedContext(): Context =
        LocaleHelper.wrap(this, settings.appLanguage)

    private fun shouldSkipPlayback(): Boolean {
        if (settings.pauseDuringCalls && callMonitor.isVoipOrCallActive()) return true
        if (settings.pauseDuringMedia && audioManager.isMusicActive) return true
        if (ReminderScheduler.isInSleepWindow(this)) return true
        return false
    }

    private fun pickDhikr(list: List<DhikrEntity>): DhikrEntity {
        if (settings.randomMode) return list.random()
        val index = settings.sequentialIndex % list.size
        settings.sequentialIndex = index + 1
        return list[index]
    }

    /** يطبّق الإعداد العام (الافتراضي: نافذة منبثقة فقط) فوق إعدادات الذكر */
    private fun resolveDisplayModes(dhikr: DhikrEntity) = when (settings.reminderDisplayStyle) {
        ReminderDisplayStyle.POPUP_ONLY -> dhikr.displayModes().copy(
            popup = true,
            notification = false,
            lockScreen = false
        )
        ReminderDisplayStyle.NOTIFICATION_ONLY -> dhikr.displayModes().copy(
            popup = false,
            notification = true,
            lockScreen = false,
            audioOnly = false,
            audioWithText = false
        )
        ReminderDisplayStyle.BOTH -> dhikr.displayModes().copy(
            popup = true,
            notification = true
        )
    }

    private suspend fun playAudioFor(dhikr: DhikrEntity) {
        val playable = DhikrPlaybackResolver.resolvePlayable(this, dhikr) ?: return
        audioPlayer.playResolved(playable, settings)
    }

    private fun showSilentTextNotification(dhikrId: Long, text: String) {
        val open = PendingIntent.getActivity(
            this,
            dhikrId.toInt(),
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(this, SilentNotificationChannels.TEXT_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
        val notification = SilentNotificationChannels.applySilentDefaults(builder).build()
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.notify(dhikrId.toInt(), notification)
    }

    private fun showLockScreen(dhikrId: Long, text: String) {
        runCatching { startActivity(OverlayActivity.lockScreenIntent(this, dhikrId, text)) }
    }

    private fun showPopup(dhikrId: Long, text: String) {
        if (OverlayWindow.hasPermission(this)) {
            runCatching { OverlayWindow.show(this, text) }
                .onFailure { startOverlayActivity(dhikrId, text) }
            return
        }
        startOverlayActivity(dhikrId, text)
    }

    private fun startOverlayActivity(dhikrId: Long, text: String) {
        runCatching { startActivity(overlayIntent(dhikrId, text)) }
    }

    private fun overlayIntent(dhikrId: Long, text: String) =
        Intent(this, OverlayActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra(OverlayActivity.EXTRA_TEXT, text)
            putExtra(OverlayActivity.EXTRA_DHIKR_ID, dhikrId)
        }

    private fun buildServiceNotification(): Notification {
        val localized = localizedContext()
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        var builder = NotificationCompat.Builder(this, SilentNotificationChannels.SERVICE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(localized.getString(R.string.auto_tasbih_on))
            .setContentText(
                localized.getString(
                    R.string.next_reminder,
                    ReminderScheduler.minutesUntilNext(this)
                ).formatDigits(settings.numberDigitStyle)
            )
            .setContentIntent(open)
            .setOngoing(true)
        return SilentNotificationChannels.applySilentDefaults(builder).build()
    }

    override fun onDestroy() {
        notificationJob?.cancel()
        callMonitor.stop()
        audioPlayer.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        const val ACTION_REFRESH = "refresh"
        const val ACTION_TRIGGER = "trigger"
        private const val NOTIF_SERVICE = 42
    }
}
