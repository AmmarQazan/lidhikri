package com.greendome.adhkar.audio

import android.content.Context
import android.media.AudioManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.model.VolumeMode
import com.greendome.adhkar.data.model.VoiceSettingsTarget
import com.greendome.adhkar.util.DeviceAudioGate
import com.greendome.adhkar.util.FlipToStopMonitor
import com.greendome.adhkar.util.PlaybackRespectMonitor
import java.io.File

class CallStateMonitor(private val appContext: Context) {
    private val telephony = appContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    @Volatile var isInCall: Boolean = false
        private set

    private val legacyListener = object : PhoneStateListener() {
        @Deprecated("Deprecated in Java")
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            isInCall = state != TelephonyManager.CALL_STATE_IDLE
        }
    }

  @RequiresApi(Build.VERSION_CODES.S)
    private val modernCallback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            isInCall = state != TelephonyManager.CALL_STATE_IDLE
        }
    }

    fun start() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephony.registerTelephonyCallback(ContextCompat.getMainExecutor(appContext), modernCallback)
            } else {
                @Suppress("DEPRECATION")
                telephony.listen(legacyListener, PhoneStateListener.LISTEN_CALL_STATE)
            }
        } catch (_: SecurityException) { }
    }

    fun stop() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephony.unregisterTelephonyCallback(modernCallback)
            } else {
                @Suppress("DEPRECATION")
                telephony.listen(legacyListener, PhoneStateListener.LISTEN_NONE)
            }
        } catch (_: Exception) { }
    }

    fun isVoipOrCallActive(): Boolean {
        if (isInCall) return true
        val mode = audioManager.mode
        return mode == AudioManager.MODE_IN_CALL ||
            mode == AudioManager.MODE_IN_COMMUNICATION ||
            mode == AudioManager.MODE_RINGTONE
    }

}

class DhikrAudioPlayer(private val context: Context) {
    private var player: ExoPlayer? = null
    private val flipMonitor = FlipToStopMonitor(context) { stop() }
    private var respectMonitor: PlaybackRespectMonitor? = null

    fun play(
        pathOrUri: String,
        settings: SettingsRepository,
        voiceProfile: VoiceSettingsTarget = VoiceSettingsTarget.TASBIH,
        onComplete: () -> Unit = {}
    ) {
        stop()
        if (DeviceAudioGate.shouldSuppressPlayback(context, settings, userInitiated = true)) {
            onComplete()
            return
        }
        val mode = DhikrVolumeResolver.volumeMode(settings, voiceProfile)
        val attrs = DhikrVolumeResolver.playerAudioAttributes(mode)
        val player = PlaybackExoPlayer.create(context).also { player = it }
        player.setAudioAttributes(attrs, DhikrVolumeResolver.shouldHandleAudioFocus(mode))
        player.setMediaItem(MediaItem.fromUri(playbackUri(pathOrUri)))
        player.volume = DhikrVolumeResolver.playerVolume(settings, voiceProfile)
        listenForCompletion(player, onComplete)
        startRespectMonitor(settings, userInitiated = true)
        startFlipMonitorIfEnabled(settings)
        player.prepare()
        player.play()
    }

    fun playAsset(
        assetPath: String,
        settings: SettingsRepository,
        voiceProfile: VoiceSettingsTarget = VoiceSettingsTarget.TASBIH,
        onComplete: () -> Unit = {}
    ) {
        stop()
        if (DeviceAudioGate.shouldSuppressPlayback(context, settings, userInitiated = true)) {
            onComplete()
            return
        }
        val cached = cachedAssetFile(context, assetPath)
        if (cached != null) {
            play(cached.absolutePath, settings, voiceProfile, onComplete)
            return
        }
        val mode = DhikrVolumeResolver.volumeMode(settings, voiceProfile)
        val attrs = DhikrVolumeResolver.playerAudioAttributes(mode)
        val player = PlaybackExoPlayer.create(context).also { player = it }
        player.setAudioAttributes(attrs, DhikrVolumeResolver.shouldHandleAudioFocus(mode))
        player.setMediaItem(MediaItem.fromUri("asset:///$assetPath"))
        player.volume = DhikrVolumeResolver.playerVolume(settings, voiceProfile)
        listenForCompletion(player, onComplete)
        startRespectMonitor(settings, userInitiated = true)
        startFlipMonitorIfEnabled(settings)
        player.prepare()
        player.play()
    }

    fun playSequence(
        items: List<PlayableAudio>,
        settings: SettingsRepository,
        voiceProfile: VoiceSettingsTarget = VoiceSettingsTarget.TASBIH,
        onItemStart: (index: Int) -> Unit = {},
        onComplete: () -> Unit = {},
    ) {
        stop()
        if (items.isEmpty()) {
            onComplete()
            return
        }
        if (DeviceAudioGate.shouldSuppressPlayback(context, settings, userInitiated = true)) {
            onComplete()
            return
        }
        val mode = DhikrVolumeResolver.volumeMode(settings, voiceProfile)
        val attrs = DhikrVolumeResolver.playerAudioAttributes(mode)
        val player = PlaybackExoPlayer.create(context).also { player = it }
        player.setAudioAttributes(attrs, DhikrVolumeResolver.shouldHandleAudioFocus(mode))
        player.volume = DhikrVolumeResolver.playerVolume(settings, voiceProfile)
        player.setMediaItems(
            items.mapIndexed { index, item ->
                item.toMediaItem().buildUpon().setMediaId("tasbih-$index").build()
            }
        )
        var lastStartedIndex = -1
        var finished = false
        fun markItemStarted(index: Int) {
            if (index == lastStartedIndex || index !in items.indices) return
            lastStartedIndex = index
            onItemStart(index)
        }
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                markItemStarted(player.currentMediaItemIndex)
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED && !finished) {
                    finished = true
                    stopFlipMonitor()
                    onComplete()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (finished) return
                finished = true
                stopFlipMonitor()
                onComplete()
            }
        })
        markItemStarted(0)
        startRespectMonitor(settings, userInitiated = true)
        startFlipMonitorIfEnabled(settings)
        player.prepare()
        player.play()
    }

    fun playAdhan(
        pathOrUri: String,
        settings: SettingsRepository,
        overrideSilent: Boolean,
        onComplete: () -> Unit = {},
    ) {
        stop()
        if (DeviceAudioGate.shouldSuppressPlayback(
                context,
                settings,
                userInitiated = false,
                ignoreQuietMode = overrideSilent,
            )
        ) {
            onComplete()
            return
        }
        val attrs = AudioAttributes.Builder()
            .setUsage(C.USAGE_ALARM)
            .setContentType(C.AUDIO_CONTENT_TYPE_SONIFICATION)
            .build()
        val player = PlaybackExoPlayer.create(context).also { player = it }
        player.setAudioAttributes(attrs, false)
        player.setMediaItem(MediaItem.fromUri(playbackUri(pathOrUri)))
        player.volume = 1f
        listenForCompletion(player, onComplete)
        startRespectMonitor(settings, userInitiated = false, ignoreQuietMode = overrideSilent)
        startFlipMonitorIfEnabled(settings)
        player.prepare()
        player.play()
    }

    fun stop() {
        stopRespectMonitor()
        stopFlipMonitor()
        player?.release()
        player = null
    }

    private fun listenForCompletion(player: ExoPlayer, onComplete: () -> Unit) {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    stopFlipMonitor()
                    onComplete()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                stopFlipMonitor()
                onComplete()
            }
        })
    }

    private fun PlayableAudio.toMediaItem(): MediaItem = when (this) {
        is PlayableAudio.Asset -> {
            val cached = cachedAssetFile(context, path)
            if (cached != null) MediaItem.fromUri(Uri.fromFile(cached))
            else MediaItem.fromUri("asset:///$path")
        }
        is PlayableAudio.File -> MediaItem.fromUri(playbackUri(path))
    }

    private fun startFlipMonitorIfEnabled(settings: SettingsRepository) {
        if (settings.flipToStopPlayback) flipMonitor.start()
    }

    private fun stopFlipMonitor() {
        flipMonitor.stop()
    }

    private fun startRespectMonitor(
        settings: SettingsRepository,
        userInitiated: Boolean,
        ignoreQuietMode: Boolean = false,
    ) {
        stopRespectMonitor()
        val monitor = PlaybackRespectMonitor(
            context = context,
            settings = settings,
            userInitiated = userInitiated,
            ignoreQuietMode = ignoreQuietMode,
            onSuppress = { stop() },
        )
        respectMonitor = monitor
        monitor.start()
    }

    private fun stopRespectMonitor() {
        respectMonitor?.stop()
        respectMonitor = null
    }
}

class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun start(): File {
        stop()
        val dir = File(context.filesDir, "recordings").apply { mkdirs() }
        val file = File(dir, "rec_${System.currentTimeMillis()}.m4a")
        outputFile = file
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        return file
    }

    fun stop(): File? {
        try {
            recorder?.stop()
        } catch (_: Exception) { }
        recorder?.release()
        recorder = null
        return outputFile
    }
}

class AudioDownloadManager(private val context: Context) {
    private val audioDir = File(context.filesDir, "offline_audio").apply { mkdirs() }

    suspend fun download(url: String, fileName: String): String? {
        return try {
            val file = File(audioDir, fileName)
            val request = okhttp3.Request.Builder().url(url).build()
            val client = okhttp3.OkHttpClient()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.byteStream()?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
            }
            if (!file.isFile || !hasAudioMagic(file)) {
                file.delete()
                return null
            }
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun copyFromUri(uri: Uri, fileName: String): String? {
        return try {
            val file = File(audioDir, fileName)
            val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            if (copied <= 0L || !file.isFile || !hasAudioMagic(file)) {
                file.delete()
                return null
            }
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun listOfflineFiles(): List<File> = audioDir.listFiles()?.toList() ?: emptyList()
}
