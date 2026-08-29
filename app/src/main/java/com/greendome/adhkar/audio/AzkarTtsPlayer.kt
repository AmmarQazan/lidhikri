package com.greendome.adhkar.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.model.TtsVoiceGender
import com.greendome.adhkar.util.DeviceAudioGate
import java.util.concurrent.atomic.AtomicBoolean

enum class TtsPlaybackState { IDLE, PLAYING, PAUSED }

class AzkarTtsPlayer(
    context: Context,
    private val settingsProvider: () -> SettingsRepository = {
        SettingsRepository(context.applicationContext)
    }
) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = TextToSpeech(appContext, this)
    private val ready = AtomicBoolean(false)
    private var onComplete: (() -> Unit)? = null
    private val queue = mutableListOf<String>()
    private var running = false

    var speechRate: Float = 1f
        set(value) {
            field = value.coerceIn(0.5f, 2f)
            tts?.setSpeechRate(field)
        }

    var voiceGender: TtsVoiceGender = settingsProvider().ttsVoiceGender
        set(value) {
            field = value
            tts?.let { TtsVoiceSelector.apply(it, value) }
        }

    var state: TtsPlaybackState = TtsPlaybackState.IDLE
        private set

    var onStateChanged: ((TtsPlaybackState) -> Unit)? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            voiceGender = settingsProvider().ttsVoiceGender
            tts?.setSpeechRate(speechRate)
            ready.set(true)
            playNext()
        }
    }

    fun refreshVoice() {
        voiceGender = settingsProvider().ttsVoiceGender
    }

    fun speakAll(texts: List<String>, onDone: () -> Unit = {}) {
        stop()
        if (texts.isEmpty()) return
        if (DeviceAudioGate.shouldSuppressPlayback(appContext, settingsProvider())) {
            onDone()
            return
        }
        refreshVoice()
        onComplete = onDone
        queue.clear()
        queue.addAll(texts)
        running = true
        setState(TtsPlaybackState.PLAYING)
        playNext()
    }

    private fun playNext() {
        if (!running) return
        if (!ready.get()) return
        val next = if (queue.isEmpty()) {
            finishPlayback()
            return
        } else queue.removeAt(0)

        val engine = tts ?: return
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                if (running) playNext()
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                if (running) playNext()
            }
        })
        engine.speak(next, TextToSpeech.QUEUE_FLUSH, Bundle(), "azkar_${System.nanoTime()}")
    }

    private var resumeQueue: List<String>? = null

    fun pause() {
        if (state != TtsPlaybackState.PLAYING) return
        resumeQueue = queue.toList()
        running = false
        tts?.stop()
        queue.clear()
        setState(TtsPlaybackState.PAUSED)
    }

    fun resume() {
        if (state != TtsPlaybackState.PAUSED) return
        val pending = resumeQueue ?: run {
            finishPlayback()
            return
        }
        resumeQueue = null
        if (pending.isEmpty()) {
            finishPlayback()
            return
        }
        queue.clear()
        queue.addAll(pending)
        running = true
        setState(TtsPlaybackState.PLAYING)
        playNext()
    }

    fun stop() {
        running = false
        queue.clear()
        resumeQueue = null
        tts?.stop()
        if (state != TtsPlaybackState.IDLE) {
            setState(TtsPlaybackState.IDLE)
        }
        onComplete = null
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        ready.set(false)
    }

    private fun finishPlayback() {
        running = false
        setState(TtsPlaybackState.IDLE)
        onComplete?.invoke()
        onComplete = null
    }

    private fun setState(newState: TtsPlaybackState) {
        state = newState
        onStateChanged?.invoke(newState)
    }
}
