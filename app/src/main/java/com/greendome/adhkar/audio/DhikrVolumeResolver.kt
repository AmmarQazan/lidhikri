package com.greendome.adhkar.audio

import android.content.Context
import android.media.AudioAttributes as PlatformAudioAttributes
import android.media.AudioManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.model.VolumeMode
import com.greendome.adhkar.data.model.VoiceSettingsTarget

object DhikrVolumeResolver {

    /**
     * Media3 يسمح بتركيز الصوت التلقائي فقط لـ USAGE_MEDIA و USAGE_GAME.
     * وضع الرنين يستخدم USAGE_NOTIFICATION_RINGTONE فيسقط التشغيل إن طُلب التركيز.
     */
    fun shouldHandleAudioFocus(mode: VolumeMode): Boolean = when (mode) {
        VolumeMode.MANUAL, VolumeMode.MEDIA -> true
        VolumeMode.RING -> false
    }

    fun playerAudioAttributes(mode: VolumeMode): AudioAttributes = when (mode) {
        // يدوي: نفس مسار الوسائط مع ضبط مستوى المشغّل داخلياً (لا صوت النظام/التنبيهات)
        VolumeMode.MANUAL -> AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
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
            .setUsage(PlatformAudioAttributes.USAGE_MEDIA)
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

    fun volumeMode(settings: SettingsRepository, profile: VoiceSettingsTarget = VoiceSettingsTarget.TASBIH): VolumeMode =
        settings.volumeModeFor(profile)

    fun playerVolume(
        settings: SettingsRepository,
        profile: VoiceSettingsTarget = VoiceSettingsTarget.TASBIH
    ): Float = when (volumeMode(settings, profile)) {
        VolumeMode.MANUAL -> settings.volumeFor(profile).coerceIn(0f, 1f)
        VolumeMode.MEDIA, VolumeMode.RING -> 1f
    }

    @Deprecated("Use playerVolume(settings, profile)")
    fun playerVolume(settings: SettingsRepository): Float =
        playerVolume(settings, VoiceSettingsTarget.TASBIH)

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
