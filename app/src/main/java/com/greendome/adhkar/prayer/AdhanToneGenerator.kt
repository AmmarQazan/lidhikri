package com.greendome.adhkar.prayer

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.sin

object AdhanToneGenerator {
    private const val SAMPLE_RATE = 22050

    fun shortFile(context: Context): File = ensure(context, "adhan_short.wav", bursts = 4)

    fun defaultFile(context: Context): File = ensure(context, "adhan_default.wav", bursts = 12)

    private fun ensure(context: Context, name: String, bursts: Int): File {
        val file = File(context.filesDir, "adhan_tones").apply { mkdirs() }.resolve(name)
        if (file.length() > 1000L) return file
        file.writeBytes(wav(bursts))
        return file
    }

    private fun wav(bursts: Int): ByteArray {
        val pcm = ByteArrayOutputStream()
        repeat(bursts) { index ->
            val freq = if (index % 4 == 3) 620.0 else 780.0
            writeTone(pcm, freq, 0.42)
            writeSilence(pcm, 0.22)
            if (index % 4 == 3) writeSilence(pcm, 0.35)
        }
        val samples = pcm.toByteArray()
        val out = ByteArrayOutputStream()
        fun intLe(value: Int) {
            out.write(value and 0xFF)
            out.write(value shr 8 and 0xFF)
            out.write(value shr 16 and 0xFF)
            out.write(value shr 24 and 0xFF)
        }
        fun shortLe(value: Int) {
            out.write(value and 0xFF)
            out.write(value shr 8 and 0xFF)
        }
        out.write("RIFF".toByteArray())
        intLe(36 + samples.size)
        out.write("WAVE".toByteArray())
        out.write("fmt ".toByteArray())
        intLe(16)
        shortLe(1)
        shortLe(1)
        intLe(SAMPLE_RATE)
        intLe(SAMPLE_RATE * 2)
        shortLe(2)
        shortLe(16)
        out.write("data".toByteArray())
        intLe(samples.size)
        out.write(samples)
        return out.toByteArray()
    }

    private fun writeTone(out: ByteArrayOutputStream, freq: Double, seconds: Double) {
        val n = (SAMPLE_RATE * seconds).toInt()
        val fade = (SAMPLE_RATE * 0.02).toInt().coerceAtLeast(1)
        for (i in 0 until n) {
            val env = when {
                i < fade -> i.toDouble() / fade
                i > n - fade -> (n - i).toDouble() / fade
                else -> 1.0
            }
            val sample = (sin(2 * Math.PI * freq * i / SAMPLE_RATE) * 0.55 * env * Short.MAX_VALUE).toInt()
            out.write(sample and 0xFF)
            out.write(sample shr 8 and 0xFF)
        }
    }

    private fun writeSilence(out: ByteArrayOutputStream, seconds: Double) {
        val n = (SAMPLE_RATE * seconds).toInt()
        repeat(n) {
            out.write(0)
            out.write(0)
        }
    }
}
