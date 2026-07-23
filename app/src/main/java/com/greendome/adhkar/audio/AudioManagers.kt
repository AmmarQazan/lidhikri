package com.greendome.adhkar.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.model.VolumeMode
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
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null

    fun play(pathOrUri: String, settings: SettingsRepository, onComplete: () -> Unit = {}) {
        stop()
        val mode = settings.volumeMode
        val attrs = DhikrVolumeResolver.playerAudioAttributes(mode)
        val player = ExoPlayer.Builder(context).build().also { player = it }
        val uri = when {
            pathOrUri.startsWith("content:") ||
                pathOrUri.startsWith("file:") ||
                pathOrUri.startsWith("http://") ||
                pathOrUri.startsWith("https://") -> Uri.parse(pathOrUri)
            else -> Uri.fromFile(File(pathOrUri))
        }
        player.setAudioAttributes(attrs, false)
        player.setMediaItem(MediaItem.fromUri(uri))
        player.volume = DhikrVolumeResolver.playerVolume(settings)
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    abandonFocus()
                    onComplete()
                }
            }
        })
        if (requestFocus(mode)) {
            player.prepare()
            player.play()
        }
    }

    fun playAsset(assetPath: String, settings: SettingsRepository, onComplete: () -> Unit = {}) {
        stop()
        val mode = settings.volumeMode
        val attrs = DhikrVolumeResolver.playerAudioAttributes(mode)
        val player = ExoPlayer.Builder(context).build().also { player = it }
        player.setAudioAttributes(attrs, false)
        player.setMediaItem(MediaItem.fromUri("asset:///$assetPath"))
        player.volume = DhikrVolumeResolver.playerVolume(settings)
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    abandonFocus()
                    onComplete()
                }
            }
        })
        if (requestFocus(mode)) {
            player.prepare()
            player.play()
        }
    }

    fun stop() {
        player?.release()
        player = null
        abandonFocus()
    }

    private fun requestFocus(mode: VolumeMode): Boolean {
        val attrs = DhikrVolumeResolver.focusAudioAttributes(mode)
        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener { change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    stop()
                }
            }
            .build()
        val result = audioManager.requestAudioFocus(focusRequest!!)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
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
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun copyFromUri(uri: Uri, fileName: String): String? {
        return try {
            val file = File(audioDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun listOfflineFiles(): List<File> = audioDir.listFiles()?.toList() ?: emptyList()
}
