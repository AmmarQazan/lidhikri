package com.greendome.adhkar.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.greendome.adhkar.R
import com.greendome.adhkar.audio.AzkarPlaybackResolver
import com.greendome.adhkar.audio.AzkarTtsPlayer
import com.greendome.adhkar.audio.DhikrAudioPlayer
import com.greendome.adhkar.audio.playResolved
import com.greendome.adhkar.data.DailyStatsRepository
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.ui.overlay.OverlayActivity
import com.greendome.adhkar.ui.overlay.OverlayWindow
import com.greendome.adhkar.util.AzkarDailyPicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AzkarCollectionPlayService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var ttsPlayer: AzkarTtsPlayer? = null
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
        ttsPlayer = AzkarTtsPlayer(this) { SettingsRepository(this) }
        audioPlayer = DhikrAudioPlayer(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_AUTO_AZKAR -> {
                stopPlayback()
                return START_NOT_STICKY
            }
        }

        val collectionId = intent?.getStringExtra(EXTRA_COLLECTION_ID) ?: run {
            stopSelf()
            return START_NOT_STICKY
        }
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
            val settings = SettingsRepository(this@AzkarCollectionPlayService)
            if (!settings.autoAzkarEnabled) {
                stopSelf()
                return@launch
            }
            val db = AdhkarDatabase.get(this@AzkarCollectionPlayService)
            val collection = withContext(Dispatchers.IO) { db.collectionDao().getById(collectionId) }
            val items = withContext(Dispatchers.IO) { db.azkarItemDao().getByCollection(collectionId) }
            if (collection == null || items.isEmpty() || !collection.autoPlayAllowed || !collection.autoPlayEnabled || !collection.useTtsAutoPlay) {
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
            val sectionTitle = collection.titleAr
            val displayText = picked.textAr
            val texts = List(picked.repeatCount.coerceAtLeast(1)) { picked.textAr }

            DailyStatsRepository(db).incrementAzkarToday()

            showAutoAzkarPopup(sectionTitle, displayText)

            val playable = withContext(Dispatchers.IO) {
                AzkarPlaybackResolver.resolvePlayable(this@AzkarCollectionPlayService, picked)
            }
            if (playable != null) {
                audioPlayer?.playResolved(playable, settings) {
                    OverlayWindow.dismiss(applicationContext)
                    stopSelf()
                }
            } else {
                ttsPlayer?.speakAll(texts) {
                    OverlayWindow.dismiss(applicationContext)
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
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
        ttsPlayer?.stop()
        OverlayWindow.dismiss(applicationContext)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        audioPlayer?.stop()
        ttsPlayer?.shutdown()
        OverlayWindow.dismiss(applicationContext)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_COLLECTION_ID = "collection_id"
        const val ACTION_STOP_AUTO_AZKAR = "stop_auto_azkar"

        @Volatile
        private var instance: AzkarCollectionPlayService? = null

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
