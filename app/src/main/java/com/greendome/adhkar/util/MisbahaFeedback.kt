package com.greendome.adhkar.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.SoundEffectConstants
import android.view.View
import com.greendome.adhkar.data.model.MisbahaFeedbackMode

object MisbahaFeedback {
    private const val TONE_DURATION_MS = 35
    private const val VIBRATION_MS = 22L

    private var toneGenerator: ToneGenerator? = null

    fun perform(context: Context, mode: MisbahaFeedbackMode, view: View? = null) {
        when (mode) {
            MisbahaFeedbackMode.SILENT -> Unit
            MisbahaFeedbackMode.SOUND_ONLY -> playClick(context, view)
            MisbahaFeedbackMode.VIBRATION_ONLY -> vibrate(context)
            MisbahaFeedbackMode.SOUND_AND_VIBRATION -> {
                playClick(context, view)
                vibrate(context)
            }
        }
    }

    private fun playClick(context: Context, view: View?) {
        if (view != null) {
            view.playSoundEffect(SoundEffectConstants.CLICK)
            return
        }
        try {
            val generator = toneGenerator ?: ToneGenerator(
                AudioManager.STREAM_NOTIFICATION,
                55
            ).also { toneGenerator = it }
            generator.startTone(ToneGenerator.TONE_PROP_ACK, TONE_DURATION_MS)
        } catch (_: Exception) {
        }
    }

    private fun vibrate(context: Context) {
        val vibrator = vibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(VIBRATION_MS, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(VIBRATION_MS)
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
        toneGenerator?.release()
        toneGenerator = null
    }
}
