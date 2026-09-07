package com.greendome.adhkar.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.greendome.adhkar.data.SettingsRepository

/**
 * يوقف التشغيل عند مكالمة أو صوت تطبيق آخر أو الصامت أثناء الاستماع.
 */
class PlaybackRespectMonitor(
    private val context: Context,
    private val settings: SettingsRepository,
    private val userInitiated: Boolean,
    private val ignoreQuietMode: Boolean,
    private val onSuppress: () -> Unit,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private var active = false

    private val playbackCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        object : AudioManager.AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(
                configs: MutableList<android.media.AudioPlaybackConfiguration>?
            ) {
                checkNow()
            }
        }
    } else {
        null
    }

    private val ringerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            checkNow()
        }
    }

    private val poll = object : Runnable {
        override fun run() {
            if (!active) return
            checkNow()
            handler.postDelayed(this, POLL_MS)
        }
    }

    fun start() {
        if (active) return
        active = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            playbackCallback?.let { audioManager.registerAudioPlaybackCallback(it, handler) }
        }
        val filter = IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION)
        runCatching {
            ContextCompat.registerReceiver(
                context,
                ringerReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
        }
        handler.postDelayed(poll, POLL_MS)
    }

    fun stop() {
        if (!active) return
        active = false
        handler.removeCallbacks(poll)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            playbackCallback?.let { runCatching { audioManager.unregisterAudioPlaybackCallback(it) } }
        }
        runCatching { context.unregisterReceiver(ringerReceiver) }
    }

    private fun checkNow() {
        if (!active) return
        if (DeviceAudioGate.shouldSuppressPlayback(context, settings, userInitiated, ignoreQuietMode)) {
            stop()
            onSuppress()
        }
    }

    private companion object {
        const val POLL_MS = 1_000L
    }
}
