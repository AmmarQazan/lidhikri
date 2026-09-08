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
import com.greendome.adhkar.data.AdhanAzkar
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
    private var playingCollectionId: String? = null

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
        playingCollectionId = collectionId
        val forcePlay = incoming.getBooleanExtra(EXTRA_FORCE_PLAY, false)
        startForeground(
            NOTIF_ID,
            NotificationCompat.Builder(this, CHANNEL)
                .let { SilentNotificationChannels.applyAppIcon(it, this) }
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
            if (!forcePlay && PrayerRespectGate.collidesWithAdhan(this@AzkarCollectionPlayService)) {
                stopSelf()
                return@launch
            }
            if (!forcePlay && AdhanPlaybackService.isPlaying()) {
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
            val itemId = incoming.getLongExtra(EXTRA_ITEM_ID, 0L)
            val inheritGroup = incoming.getBooleanExtra(EXTRA_INHERIT, false)
            val now = System.currentTimeMillis()
            val scoped = when {
                itemId > 0L -> items.filter { it.id == itemId }
                inheritGroup -> items.filter { it.inheritsCollectionTime() }.ifEmpty { items }
                else -> items
            }
            val candidates = scoped.filter { !it.hasOwnHijri() || it.matchesHijri(now) }
            val picked = AzkarDailyPicker.pick(
                this@AzkarCollectionPlayService,
                collectionId + if (itemId > 0L) "#$itemId" else if (inheritGroup) "#inherit" else "",
                candidates,
                settings.autoAzkarRandomMode && itemId <= 0L
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
        val builder = SilentNotificationChannels.applyAppIcon(
            NotificationCompat.Builder(this, SilentNotificationChannels.TEXT_REMINDER),
            this,
        )
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
            return
        }
        runCatching {
            startActivity(OverlayActivity.autoAzkarIntent(this, sectionTitle, text))
        }
        showFullScreenAzkar(sectionTitle, text)
    }

    private fun showFullScreenAzkar(sectionTitle: String, text: String) {
        if (!RuntimePermissions.hasPostNotifications(this)) return
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.createNotificationChannel(
            NotificationChannel(
                FULLSCREEN_CHANNEL,
                getString(R.string.azkar_play_channel),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
        )
        val screen = PendingIntent.getActivity(
            this,
            FULLSCREEN_REQUEST,
            OverlayActivity.autoAzkarIntent(this, sectionTitle, text),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = SilentNotificationChannels.applyAppIcon(
            NotificationCompat.Builder(this, FULLSCREEN_CHANNEL),
            this,
        )
            .setContentTitle(sectionTitle)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(screen)
            .setFullScreenIntent(screen, true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSilent(true)
            .build()
        mgr.notify(FULLSCREEN_NOTIF, notification)
    }

    private fun stopPlayback() {
        audioPlayer?.stop()
        OverlayWindow.dismiss(applicationContext)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        val finishedAdhanAzkar = playingCollectionId == AdhanAzkar.COLLECTION_ID
        if (instance === this) instance = null
        audioPlayer?.stop()
        OverlayWindow.dismiss(applicationContext)
        super.onDestroy()
        if (finishedAdhanAzkar) {
            PrayerPhoneSilent.enter(this)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_COLLECTION_ID = "collection_id"
        const val EXTRA_ITEM_ID = "item_id"
        const val EXTRA_INHERIT = "inherit_group"
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
        private const val FULLSCREEN_CHANNEL = "azkar_fullscreen"
        private const val FULLSCREEN_NOTIF = 78
        private const val FULLSCREEN_REQUEST = 811
    }
}
