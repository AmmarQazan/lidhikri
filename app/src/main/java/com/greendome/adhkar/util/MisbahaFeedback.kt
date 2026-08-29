package com.greendome.adhkar.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import com.greendome.adhkar.data.model.MisbahaFeedbackMode

object MisbahaFeedback {
    private const val TONE_DURATION_MS = 55
    private const val TONE_VOLUME = 80
    private const val VIBRATION_MS = 45L

    private val lock = Any()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var toneGenerator: ToneGenerator? = null
    private var toneStream: Int? = null

    fun perform(context: Context, mode: MisbahaFeedbackMode, @Suppress("UNUSED_PARAMETER") view: View? = null) {
        val appContext = context.applicationContext
        val run = {
            when (mode) {
                MisbahaFeedbackMode.SILENT -> Unit
                MisbahaFeedbackMode.SOUND_ONLY -> playClick(appContext)
                MisbahaFeedbackMode.VIBRATION_ONLY -> vibrate(appContext)
                MisbahaFeedbackMode.SOUND_AND_VIBRATION -> {
                    playClick(appContext)
                    vibrate(appContext)
                }
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            run()
        } else {
            mainHandler.post(run)
        }
    }

    private fun playClick(context: Context) {
        if (DeviceAudioGate.isRingerMuted(context)) return
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val streams = linkedSetOf<Int>()
        if (audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM) > 0) {
            streams += AudioManager.STREAM_SYSTEM
        }
        if (audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) > 0) {
            streams += AudioManager.STREAM_NOTIFICATION
        }
        streams += AudioManager.STREAM_MUSIC
        synchronized(lock) {
            for (stream in streams) {
                try {
                    if (toneGenerator(stream).startTone(ToneGenerator.TONE_PROP_BEEP, TONE_DURATION_MS)) {
                        return
                    }
                } catch (_: Exception) {
                    releaseLocked()
                }
            }
        }
    }

    private fun toneGenerator(stream: Int): ToneGenerator {
        val existing = toneGenerator
        if (existing != null && toneStream == stream) return existing
        existing?.release()
        toneGenerator = null
        toneStream = null
        return ToneGenerator(stream, TONE_VOLUME).also {
            toneGenerator = it
            toneStream = stream
        }
    }

    private fun vibrate(context: Context) {
        val vibrator = vibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        val effect = clickEffect()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                vibrator.vibrate(
                    effect,
                    VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_TOUCH)
                        .build()
                )
            } else {
                vibrator.vibrate(effect)
            }
        } catch (_: Exception) {
        }
    }

    private fun clickEffect(): VibrationEffect {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        } else {
            VibrationEffect.createOneShot(VIBRATION_MS, VibrationEffect.DEFAULT_AMPLITUDE)
        }
    }

    private fun vibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun release() {
        synchronized(lock) {
            releaseLocked()
        }
    }

    private fun releaseLocked() {
        toneGenerator?.release()
        toneGenerator = null
        toneStream = null
    }
}
