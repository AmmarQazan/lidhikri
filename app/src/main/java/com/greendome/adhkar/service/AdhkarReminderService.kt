package com.greendome.adhkar.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.app.KeyguardManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.greendome.adhkar.MainActivity
import com.greendome.adhkar.R
import com.greendome.adhkar.util.LocaleHelper
import com.greendome.adhkar.util.RuntimePermissions
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
import com.greendome.adhkar.prayer.PrayerRespectGate
import com.greendome.adhkar.util.CollectionScheduleHelper
import com.greendome.adhkar.util.NextAzkarSchedule
import com.greendome.adhkar.util.DeviceAudioGate
import com.greendome.adhkar.data.model.VoiceSettingsTarget
import com.greendome.adhkar.ui.overlay.OverlayActivity
import com.greendome.adhkar.ui.overlay.OverlayWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdhkarReminderService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var settings: SettingsRepository
    private lateinit var dhikrRepo: DhikrRepository
    private lateinit var statsRepo: DailyStatsRepository
    private lateinit var audioPlayer: DhikrAudioPlayer
    private lateinit var callMonitor: CallStateMonitor
    private var notificationJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        settings = SettingsRepository(this)
        dhikrRepo = DhikrRepository(AdhkarDatabase.get(this))
        statsRepo = DailyStatsRepository(AdhkarDatabase.get(this))
        audioPlayer = DhikrAudioPlayer(this)
        callMonitor = CallStateMonitor(this)
        callMonitor.start()
        SilentNotificationChannels.ensureCreated(this)
        SilentNotificationChannels.cancelLegacyAlertIds(this)
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
        SilentNotificationChannels.cancelTransientReminderAlerts(this)

        if (AzkarCollectionPlayService.isPlaying()) {
            rescheduleAndUpdateNotification()
            return
        }

        if (isAutoAzkarDueNow()) {
            rescheduleAndUpdateNotification()
            return
        }

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
        val modes = settings.tasbihPresentation.toDisplayModes()
        val lang = settings.appLanguage
        val text = dhikr.localizedText(lang)

        if (modes.showsTextViaNotification()) {
            showSilentTextNotification(dhikr.id, text)
        } else {
            val locked = isKeyguardLocked()
            var textShown = false
            if (locked &&
                settings.tasbihAutoLockScreenEnabled &&
                !modes.audioOnly
            ) {
                LockScreenReminderPresenter.showTasbih(this, dhikr.id, text)
                textShown = true
            }
            if (!locked && modes.showsTextViaPopup()) {
                showPopup(dhikr.id, text)
                textShown = true
            }
            if (!textShown && modes.audioWithText && !modes.audioOnly) {
                showPopup(dhikr.id, text)
            }
        }

        if (modes.playsAudio()) {
            playAudioFor(dhikr)
        }

        rescheduleAndUpdateNotification()
    }

    private suspend fun isAutoAzkarDueNow(): Boolean {
        if (!settings.autoAzkarEnabled) return false
        val collections = withContext(Dispatchers.IO) {
            AdhkarDatabase.get(this@AdhkarReminderService).collectionDao().getAutoEnabled()
        }
        return CollectionScheduleHelper.isAnyDueAt(collections)
    }

    private fun abortActiveReminder() {
        audioPlayer.stop()
        OverlayWindow.dismiss(this)
        SilentNotificationChannels.cancelTransientReminderAlerts(this)
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
                val toMinute = 60_000L - (now % 60_000L)
                val toQuietChange = PrayerRespectGate.nextStatusChangeAt(this@AdhkarReminderService, now)
                    ?.minus(now)
                val wait = if (toQuietChange != null && toQuietChange in 1 until toMinute) {
                    toQuietChange
                } else {
                    toMinute
                }
                delay(wait.coerceAtLeast(1L))
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
        if (DeviceAudioGate.shouldSuppressPlayback(this, settings)) return true
        if (ReminderScheduler.isOutsideTasbihWindow(this)) return true
        if (PrayerRespectGate.isQuiet(this)) return true
        return false
    }

    private fun pickDhikr(list: List<DhikrEntity>): DhikrEntity {
        if (settings.randomMode) return list.random()
        val index = settings.sequentialIndex % list.size
        settings.sequentialIndex = index + 1
        return list[index]
    }

    private suspend fun playAudioFor(dhikr: DhikrEntity) {
        if (!dhikr.isEligibleForAutoTasbih()) return
        if (DeviceAudioGate.shouldSuppressPlayback(this, settings)) return
        val playable = DhikrPlaybackResolver.resolvePlayable(this, dhikr) ?: return
        audioPlayer.playResolved(playable, settings, VoiceSettingsTarget.TASBIH)
    }

    private fun showSilentTextNotification(dhikrId: Long, text: String) {
        if (!RuntimePermissions.hasPostNotifications(this)) return
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
        val notification = SilentNotificationChannels.applyTextReminderDefaults(builder).build()
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.notify(dhikrId.toInt(), notification)
    }

    private fun isKeyguardLocked(): Boolean {
        val keyguard = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        return keyguard.isKeyguardLocked
    }

    private fun showPopup(dhikrId: Long, text: String) {
        if (OverlayWindow.hasPermission(this)) {
            runCatching { OverlayWindow.showTasbih(this, text) }
                .onFailure { startOverlayActivity(dhikrId, text) }
            return
        }
        startOverlayActivity(dhikrId, text)
    }

    private fun startOverlayActivity(dhikrId: Long, text: String) {
        runCatching { startActivity(OverlayActivity.tasbihPopupIntent(this, dhikrId, text)) }
    }

    private fun buildServiceNotification(): Notification {
        val localized = localizedContext()
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val pausedForPrayer = PrayerRespectGate.isQuiet(this)
        val title = localized.getString(
            if (pausedForPrayer) R.string.auto_tasbih_paused_prayer
            else R.string.auto_tasbih_on
        )
        val tasbihLine = localized.getString(
            R.string.next_reminder,
            ReminderScheduler.minutesUntilNext(this)
        ).formatDigits(settings.numberDigitStyle)
        val azkarLine = NextAzkarSchedule.notificationLine(localized)
        val body = if (azkarLine.isNullOrBlank()) tasbihLine else "$tasbihLine\n$azkarLine"
        var builder = NotificationCompat.Builder(this, SilentNotificationChannels.SERVICE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setOngoing(true)
        return SilentNotificationChannels.applySilentDefaults(builder).build()
    }

    override fun onDestroy() {
        if (instance === this) instance = null
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
        private const val NOTIF_SERVICE = SilentNotificationChannels.SERVICE_NOTIFICATION_ID

        @Volatile
        private var instance: AdhkarReminderService? = null

        fun abortActiveReminder(context: Context) {
            instance?.abortActiveReminder()
            OverlayWindow.dismiss(context)
            SilentNotificationChannels.cancelTransientReminderAlerts(context)
        }

        fun refreshNotification(context: Context) {
            val settings = SettingsRepository(context)
            if (!settings.isServiceEnabled) return
            val intent = Intent(context, AdhkarReminderService::class.java).apply {
                action = ACTION_REFRESH
            }
            if (ServiceRunningHelper.isRunning(context, AdhkarReminderService::class.java)) {
                context.startService(intent)
            }
        }
    }
}
