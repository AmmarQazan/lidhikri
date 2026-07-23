package com.greendome.adhkar.audio

import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.greendome.adhkar.data.model.TtsVoiceGender
import java.util.Locale

object TtsVoiceSelector {
    private val femaleHints = listOf("female", "woman", "fem", "-f-", "_f_", "girl")
    private val maleHints = listOf("male", "man", "masc", "-m-", "_m_", "boy")

    fun apply(tts: TextToSpeech, gender: TtsVoiceGender): Boolean {
        tts.language = Locale("ar")
        val voice = pickVoice(tts, gender)
        if (voice != null) {
            tts.voice = voice
            tts.setPitch(if (gender == TtsVoiceGender.MALE) 0.92f else 1.08f)
            return true
        }
        tts.setPitch(if (gender == TtsVoiceGender.MALE) 0.85f else 1.15f)
        return false
    }

    private fun pickVoice(tts: TextToSpeech, gender: TtsVoiceGender): Voice? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return null
        val voices = tts.voices?.filter { voice ->
            voice.locale.language == "ar" &&
                voice.name != null &&
                !voice.isNetworkConnectionRequired
        }.orEmpty()
        if (voices.isEmpty()) return null

        val hints = if (gender == TtsVoiceGender.MALE) maleHints else femaleHints
        val opposite = if (gender == TtsVoiceGender.MALE) femaleHints else maleHints

        voices.firstOrNull { voice -> hints.any { voice.name.contains(it, ignoreCase = true) } }
            ?.let { return it }

        val withoutOpposite = voices.filter { voice ->
            opposite.none { voice.name.contains(it, ignoreCase = true) }
        }
        val pool = withoutOpposite.ifEmpty { voices }
        return if (gender == TtsVoiceGender.MALE) {
            pool.minByOrNull { it.name }
        } else {
            pool.maxByOrNull { it.name }
        }
    }
}
