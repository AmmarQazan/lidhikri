package com.greendome.adhkar.audio

import android.content.Context
import android.media.AudioManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
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
import com.greendome.adhkar.data.model.MediaRespectPart
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
    private var ownPlaybackMarked = false
    private var pendingComplete: (() -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var playbackSession = 0
    @Volatile var isAborted: Boolean = false
        private set

    private fun completeOnce() {
        val callback = pendingComplete
        pendingComplete = null
        callback?.invoke()
    }

    fun resetForNewPlayback() {
        isAborted = false
        pendingComplete = null
        releasePlayer()
    }

    private fun releasePlayer() {
        playbackSession++
        endOwnPlaybackSession()
        stopRespectMonitor()
        stopFlipMonitor()
        val old = player
        player = null
        old?.release()
    }

    private fun beginOwnPlaybackSession() {
        if (ownPlaybackMarked) return
        ownPlaybackMarked = true
        DeviceAudioGate.beginOwnPlayback()
    }

    private fun endOwnPlaybackSession() {
        if (!ownPlaybackMarked) return
        ownPlaybackMarked = false
        DeviceAudioGate.endOwnPlayback()
    }

    fun play(
        pathOrUri: String,
        settings: SettingsRepository,
        voiceProfile: VoiceSettingsTarget = VoiceSettingsTarget.TASBIH,
        mediaPart: MediaRespectPart? = null,
        onComplete: () -> Unit = {}
    ) {
        releasePlayer()
        isAborted = false
        pendingComplete = null
        if (DeviceAudioGate.shouldSuppressPlayback(
                context,
                settings,
                userInitiated = true,
                mediaPart = mediaPart,
            )
        ) {
            onComplete()
            return
        }
        val mode = DhikrVolumeResolver.volumeMode(settings, voiceProfile)
        val attrs = DhikrVolumeResolver.playerAudioAttributes(mode)
        val player = PlaybackExoPlayer.create(context).also { player = it }
        player.setAudioAttributes(attrs, shouldTakeAudioFocus(settings, mode, mediaPart))
        player.setMediaItem(MediaItem.fromUri(playbackUri(pathOrUri)))
        player.volume = DhikrVolumeResolver.playerVolume(settings, voiceProfile)
        listenForCompletion(player, onComplete)
        beginOwnPlaybackSession()
        startRespectMonitor(settings, userInitiated = true, mediaPart = mediaPart)
        startFlipMonitorIfEnabled(settings)
        player.prepare()
        player.play()
    }

    fun playAsset(
        assetPath: String,
        settings: SettingsRepository,
        voiceProfile: VoiceSettingsTarget = VoiceSettingsTarget.TASBIH,
        mediaPart: MediaRespectPart? = null,
        onComplete: () -> Unit = {}
    ) {
        releasePlayer()
        isAborted = false
        pendingComplete = null
        if (DeviceAudioGate.shouldSuppressPlayback(
                context,
                settings,
                userInitiated = true,
                mediaPart = mediaPart,
            )
        ) {
            onComplete()
            return
        }
        val cached = cachedAssetFile(context, assetPath)
        if (cached != null) {
            play(cached.absolutePath, settings, voiceProfile, mediaPart, onComplete)
            return
        }
        val mode = DhikrVolumeResolver.volumeMode(settings, voiceProfile)
        val attrs = DhikrVolumeResolver.playerAudioAttributes(mode)
        val player = PlaybackExoPlayer.create(context).also { player = it }
        player.setAudioAttributes(attrs, shouldTakeAudioFocus(settings, mode, mediaPart))
        player.setMediaItem(MediaItem.fromUri("asset:///$assetPath"))
        player.volume = DhikrVolumeResolver.playerVolume(settings, voiceProfile)
        listenForCompletion(player, onComplete)
        beginOwnPlaybackSession()
        startRespectMonitor(settings, userInitiated = true, mediaPart = mediaPart)
        startFlipMonitorIfEnabled(settings)
        player.prepare()
        player.play()
    }

    fun playSequence(
        items: List<PlayableAudio>,
        settings: SettingsRepository,
        voiceProfile: VoiceSettingsTarget = VoiceSettingsTarget.TASBIH,
        mediaPart: MediaRespectPart? = null,
        onItemStart: (index: Int) -> Unit = {},
        onComplete: () -> Unit = {},
    ) {
        releasePlayer()
        isAborted = false
        pendingComplete = null
        if (items.isEmpty()) {
            onComplete()
            return
        }
        if (DeviceAudioGate.shouldSuppressPlayback(
                context,
                settings,
                userInitiated = true,
                mediaPart = mediaPart,
            )
        ) {
            onComplete()
            return
        }
        val mode = DhikrVolumeResolver.volumeMode(settings, voiceProfile)
        val attrs = DhikrVolumeResolver.playerAudioAttributes(mode)
        val player = PlaybackExoPlayer.create(context).also { player = it }
        player.setAudioAttributes(attrs, shouldTakeAudioFocus(settings, mode, mediaPart))
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
        fun finish() {
            if (finished) return
            finished = true
            pendingComplete = null
            endOwnPlaybackSession()
            stopFlipMonitor()
            onComplete()
        }
        pendingComplete = { finish() }
        val session = playbackSession
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                markItemStarted(player.currentMediaItemIndex)
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    mainHandler.post { if (session == playbackSession) finish() }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                mainHandler.post { if (session == playbackSession) finish() }
            }
        })
        markItemStarted(0)
        beginOwnPlaybackSession()
        startRespectMonitor(settings, userInitiated = true, mediaPart = mediaPart)
        startFlipMonitorIfEnabled(settings)
        player.prepare()
        player.play()
    }

    fun playAdhan(
        pathOrUri: String,
        settings: SettingsRepository,
        overrideSilent: Boolean,
        mediaPart: MediaRespectPart? = null,
        onComplete: () -> Unit = {},
    ) {
        releasePlayer()
        isAborted = false
        pendingComplete = null
        if (DeviceAudioGate.shouldSuppressPlayback(
                context,
                settings,
                userInitiated = false,
                ignoreQuietMode = overrideSilent,
                mediaPart = mediaPart,
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
        beginOwnPlaybackSession()
        startRespectMonitor(
            settings,
            userInitiated = false,
            ignoreQuietMode = overrideSilent,
            mediaPart = mediaPart,
        )
        startFlipMonitorIfEnabled(settings)
        player.prepare()
        player.play()
    }

    fun stop() {
        isAborted = true
        releasePlayer()
        completeOnce()
    }

    /** إيقاف بلا نداء الاكتمال، حتى لا يُحسب الإيقاف اليدوي انتهاءً طبيعياً. */
    fun cancel() {
        isAborted = true
        pendingComplete = null
        releasePlayer()
    }

    fun finishCurrentItem() {
        releasePlayer()
        completeOnce()
    }

    private fun listenForCompletion(player: ExoPlayer, onComplete: () -> Unit) {
        val session = playbackSession
        pendingComplete = onComplete
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state != Player.STATE_ENDED) return
                endOwnPlaybackSession()
                stopFlipMonitor()
                mainHandler.post {
                    if (session != playbackSession) return@post
                    completeOnce()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                endOwnPlaybackSession()
                stopFlipMonitor()
                mainHandler.post {
                    if (session != playbackSession) return@post
                    completeOnce()
                }
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

    /** مع احترام هذا الجزء لا نطلب تركيز الصوت، حتى لا يُوقف يوتيوب إن فشل الاكتشاف. */
    private fun shouldTakeAudioFocus(
        settings: SettingsRepository,
        mode: VolumeMode,
        mediaPart: MediaRespectPart?,
    ): Boolean {
        if (mediaPart != null && settings.respectsOtherAppAudio(mediaPart)) return false
        return DhikrVolumeResolver.shouldHandleAudioFocus(mode)
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
        mediaPart: MediaRespectPart? = null,
    ) {
        stopRespectMonitor()
        val monitor = PlaybackRespectMonitor(
            context = context,
            settings = settings,
            userInitiated = userInitiated,
            ignoreQuietMode = ignoreQuietMode,
            mediaPart = mediaPart,
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
