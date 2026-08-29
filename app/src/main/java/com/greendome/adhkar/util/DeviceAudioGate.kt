package com.greendome.adhkar.util

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import com.greendome.adhkar.data.SettingsRepository

object DeviceAudioGate {

    /** الهاتف صامت أو اهتزاز فقط */
    fun isRingerMuted(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_SILENT,
            AudioManager.RINGER_MODE_VIBRATE -> true
            else -> false
        }
    }

    /** وضع عدم الإزعاج يمنع التنبيهات */
    fun isDndBlocking(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        val notificationManager = context.getSystemService(NotificationManager::class.java)
            ?: return false
        if (notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE) {
            return true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            notificationManager.currentInterruptionFilter == INTERRUPTION_FILTER_ALARMS_ONLY
        ) {
            return true
        }
        return false
    }

    /**
     * مكالمة هاتفية أو مكالمة صوت/فيديو داخل تطبيق
     * (واتساب، تيليجرام، سيجنال، مسنجر، ميت، سكايب، …)
     */
    fun isCallOrCommunicationActive(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        when (audioManager.mode) {
            AudioManager.MODE_IN_CALL,
            AudioManager.MODE_IN_COMMUNICATION,
            AudioManager.MODE_RINGTONE -> return true
        }
        return hasActiveUsage(
            audioManager,
            AudioAttributes.USAGE_VOICE_COMMUNICATION,
            AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING,
        )
    }

    /**
     * تطبيق آخر يشغّل صوتاً:
     * يوتيوب/موسيقى، تسجيلات واتساب وتيليجرام وسيجنال ومسنجر،
     * أو أي بث وسائط/كلام من تطبيق آخر.
     */
    fun isOtherAppAudioPlaying(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        @Suppress("DEPRECATION")
        if (audioManager.isMusicActive) return true
        when (audioManager.mode) {
            AudioManager.MODE_IN_CALL,
            AudioManager.MODE_IN_COMMUNICATION -> return true
        }
        return hasActiveUsage(
            audioManager,
            AudioAttributes.USAGE_MEDIA,
            AudioAttributes.USAGE_GAME,
            AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE,
            AudioAttributes.USAGE_ASSISTANT,
            AudioAttributes.USAGE_VOICE_COMMUNICATION,
            AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING,
        ) || hasActiveSpeechPlayback(audioManager)
    }

    /**
     * لا تشغّل صوت التطبيق عند:
     * - الصامت / عدم الإزعاج (حسب الإعداد)
     * - مكالمة هاتف أو صوت/فيديو (إعداد «إيقاف أثناء المكالمات»)
     * - صوت من تطبيق آخر: يوتيوب، تسجيلات، مكالمات التطبيقات (إعداد التخطي)
     */
    fun shouldSuppressPlayback(context: Context, settings: SettingsRepository): Boolean {
        if (isRingerMuted(context)) return true
        if (settings.respectQuietMode && isDndBlocking(context)) return true
        if (settings.pauseDuringCalls && isCallOrCommunicationActive(context)) return true
        if (settings.pauseDuringMedia && isOtherAppAudioPlaying(context)) return true
        return false
    }

    @Deprecated("Use shouldSuppressPlayback(context, settings)")
    fun shouldSuppressPlayback(context: Context, respectQuietMode: Boolean = true): Boolean {
        if (isRingerMuted(context)) return true
        return respectQuietMode && isDndBlocking(context)
    }

    private fun hasActiveUsage(audioManager: AudioManager, vararg usages: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val wanted = usages.toHashSet()
        return audioManager.activePlaybackConfigurations.any { config ->
            config.audioAttributes.usage in wanted
        }
    }

    /** بث كلام من تطبيق آخر حتى لو كان usage غير معتاد */
    private fun hasActiveSpeechPlayback(audioManager: AudioManager): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return audioManager.activePlaybackConfigurations.any { config ->
            val attrs = config.audioAttributes
            attrs.contentType == AudioAttributes.CONTENT_TYPE_SPEECH &&
                attrs.usage != AudioAttributes.USAGE_ALARM &&
                attrs.usage != AudioAttributes.USAGE_NOTIFICATION &&
                attrs.usage != AudioAttributes.USAGE_NOTIFICATION_EVENT &&
                attrs.usage != AudioAttributes.USAGE_NOTIFICATION_RINGTONE
        }
    }

    private const val INTERRUPTION_FILTER_ALARMS_ONLY = 4
}
