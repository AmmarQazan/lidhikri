package com.greendome.adhkar.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.app.KeyguardManager
import android.content.Intent
import com.greendome.adhkar.data.model.VoiceSettingsTarget
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.greendome.adhkar.MainActivity
import com.greendome.adhkar.R
import com.greendome.adhkar.audio.AzkarPlaybackResolver
import com.greendome.adhkar.audio.DhikrAudioPlayer
import com.greendome.adhkar.audio.playResolved
import com.greendome.adhkar.data.DailyStatsRepository
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.prayer.PrayerRespectGate
import com.greendome.adhkar.ui.overlay.OverlayActivity
import com.greendome.adhkar.ui.overlay.OverlayWindow
import com.greendome.adhkar.util.AzkarDailyPicker
import com.greendome.adhkar.util.DeviceAudioGate
import com.greendome.adhkar.util.RuntimePermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AzkarCollectionPlayService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var audioPlayer: DhikrAudioPlayer? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.azkar_play_channel), NotificationManager.IMPORTANCE_LOW).apply {
                setSound(null, null)
            }
        )
        audioPlayer = DhikrAudioPlayer(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_AUTO_AZKAR -> {
                stopPlayback()
                return START_NOT_STICKY
            }
        }

        val incoming = intent ?: run {
            stopSelf()
            return START_NOT_STICKY
        }
        val collectionId = incoming.getStringExtra(EXTRA_COLLECTION_ID) ?: run {
            stopSelf()
            return START_NOT_STICKY
        }
        val forcePlay = incoming.getBooleanExtra(EXTRA_FORCE_PLAY, false)
        startForeground(
            NOTIF_ID,
            NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.azkar_auto_playing))
                .setSilent(true)
                .setOngoing(true)
                .build()
        )
        scope.launch {
            AdhkarReminderService.abortActiveReminder(this@AzkarCollectionPlayService)
            val settings = SettingsRepository(this@AzkarCollectionPlayService)
            if (!forcePlay && !settings.autoAzkarEnabled) {
                stopSelf()
                return@launch
            }
            val db = AdhkarDatabase.get(this@AzkarCollectionPlayService)
            val collection = withContext(Dispatchers.IO) { db.collectionDao().getById(collectionId) }
            val items = withContext(Dispatchers.IO) { db.azkarItemDao().getByCollection(collectionId) }
            val allowed = forcePlay || (collection != null && collection.autoPlayAllowed && collection.autoPlayEnabled)
            if (collection == null || items.isEmpty() || !allowed) {
                stopSelf()
                return@launch
            }
            val picked = AzkarDailyPicker.pick(
                this@AzkarCollectionPlayService,
                collectionId,
                items,
                settings.autoAzkarRandomMode
            ) ?: run {
                stopSelf()
                return@launch
            }
            val sectionTitle = collection.localizedTitle(settings.appLanguage)
            val displayText = picked.localizedText(settings.appLanguage)
            val modes = if (collectionId == PrayerRespectGate.AFTER_PRAYER_COLLECTION_ID) {
                settings.afterPrayerPresentation.toDisplayModes()
            } else {
                settings.azkarPresentation.toDisplayModes()
            }

            DailyStatsRepository(db).incrementAzkarToday()

            if (modes.showsTextViaNotification()) {
                showSilentTextNotification(picked.id, sectionTitle, displayText)
            } else if (!modes.audioOnly) {
                showAutoAzkarText(settings, picked.id, sectionTitle, displayText)
            }

            val playable = withContext(Dispatchers.IO) {
                AzkarPlaybackResolver.resolvePlayable(this@AzkarCollectionPlayService, picked)
            }
            if (modes.playsAudio() &&
                playable != null &&
                !DeviceAudioGate.shouldSuppressPlayback(
                    this@AzkarCollectionPlayService,
                    settings
                )
            ) {
                audioPlayer?.playResolved(playable, settings, VoiceSettingsTarget.AZKAR) {
                    OverlayWindow.dismiss(applicationContext)
                    stopSelf()
                }
            } else {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun showSilentTextNotification(itemId: Long, title: String, text: String) {
        if (!RuntimePermissions.hasPostNotifications(this)) return
        val open = PendingIntent.getActivity(
            this,
            itemId.toInt(),
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(this, SilentNotificationChannels.TEXT_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
        val notification = SilentNotificationChannels.applyTextReminderDefaults(builder).build()
        getSystemService(NotificationManager::class.java).notify(itemId.toInt(), notification)
    }

    private fun showAutoAzkarText(
        settings: SettingsRepository,
        itemId: Long,
        sectionTitle: String,
        text: String
    ) {
        val keyguard = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        val locked = keyguard.isKeyguardLocked
        if (locked && settings.azkarAutoLockScreenEnabled) {
            LockScreenReminderPresenter.showAzkar(this, itemId, sectionTitle, text)
            return
        }
        showAutoAzkarPopup(sectionTitle, text)
    }

    private fun showAutoAzkarPopup(sectionTitle: String, text: String) {
        val onStop = { stopAutoAzkar(this@AzkarCollectionPlayService) }

        if (OverlayWindow.hasPermission(this)) {
            OverlayWindow.showAutoAzkar(
                context = this,
                sectionTitle = sectionTitle,
                text = text,
                onDismiss = { OverlayWindow.dismiss(applicationContext) },
                onStopAuto = onStop
            )
        } else {
            startActivity(OverlayActivity.autoAzkarIntent(this, sectionTitle, text))
        }
    }

    private fun stopPlayback() {
        audioPlayer?.stop()
        OverlayWindow.dismiss(applicationContext)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        audioPlayer?.stop()
        OverlayWindow.dismiss(applicationContext)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_COLLECTION_ID = "collection_id"
        const val EXTRA_FORCE_PLAY = "force_play"
        const val ACTION_STOP_AUTO_AZKAR = "stop_auto_azkar"

        @Volatile
        private var instance: AzkarCollectionPlayService? = null

        fun isPlaying(): Boolean = instance != null

        fun stopAutoAzkar(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, AzkarCollectionPlayService::class.java).apply {
                    action = ACTION_STOP_AUTO_AZKAR
                }
            )
        }

        private const val CHANNEL = "azkar_play"
        private const val NOTIF_ID = 77
    }
}
