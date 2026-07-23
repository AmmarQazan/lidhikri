package com.greendome.adhkar.audio

import android.content.Context
import android.media.AudioAttributes as PlatformAudioAttributes
import android.media.AudioManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.model.VolumeMode

object DhikrVolumeResolver {

    fun playerAudioAttributes(mode: VolumeMode): AudioAttributes = when (mode) {
        VolumeMode.MANUAL -> AudioAttributes.Builder()
            .setUsage(C.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()
        VolumeMode.MEDIA -> AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()
        VolumeMode.RING -> AudioAttributes.Builder()
            .setUsage(C.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(C.AUDIO_CONTENT_TYPE_SONIFICATION)
            .build()
    }

    fun focusAudioAttributes(mode: VolumeMode): PlatformAudioAttributes = when (mode) {
        VolumeMode.MANUAL -> PlatformAudioAttributes.Builder()
            .setUsage(PlatformAudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(PlatformAudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        VolumeMode.MEDIA -> PlatformAudioAttributes.Builder()
            .setUsage(PlatformAudioAttributes.USAGE_MEDIA)
            .setContentType(PlatformAudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        VolumeMode.RING -> PlatformAudioAttributes.Builder()
            .setUsage(PlatformAudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(PlatformAudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }

    fun playerVolume(settings: SettingsRepository): Float = when (settings.volumeMode) {
        VolumeMode.MANUAL -> settings.volume.coerceIn(0f, 1f)
        VolumeMode.MEDIA, VolumeMode.RING -> 1f
    }

    fun systemStreamPercent(context: Context, mode: VolumeMode): Int? {
        val stream = when (mode) {
            VolumeMode.MEDIA -> AudioManager.STREAM_MUSIC
            VolumeMode.RING -> AudioManager.STREAM_RING
            VolumeMode.MANUAL -> return null
        }
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(stream)
        if (max <= 0) return 0
        return am.getStreamVolume(stream) * 100 / max
    }
}
