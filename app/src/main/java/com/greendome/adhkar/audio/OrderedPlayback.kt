package com.greendome.adhkar.audio

import android.content.Context
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.data.local.DhikrEntity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend fun playAzkarItemsOrdered(
    context: Context,
    items: List<AzkarItemEntity>,
    useTts: Boolean,
    audioPlayer: DhikrAudioPlayer,
    tts: AzkarTtsPlayer,
    settings: SettingsRepository,
    isCancelled: () -> Boolean = { false },
) {
    for (item in items) {
        if (isCancelled()) return
        repeat(item.repeatCount.coerceAtLeast(1)) {
            if (isCancelled()) return
            val playable = AzkarPlaybackResolver.resolvePlayable(context, item)
            if (playable != null) {
                suspendCancellableCoroutine { cont ->
                    audioPlayer.playResolved(playable, settings) { cont.resume(Unit) }
                }
            } else if (useTts && item.textAr.isNotBlank()) {
                suspendCancellableCoroutine { cont ->
                    tts.speakAll(listOf(item.textAr)) { cont.resume(Unit) }
                }
            }
        }
    }
}

suspend fun playDhikrItemsOrdered(
    context: Context,
    items: List<DhikrEntity>,
    useTts: Boolean,
    audioPlayer: DhikrAudioPlayer,
    tts: AzkarTtsPlayer,
    settings: SettingsRepository,
    textFor: (DhikrEntity) -> String,
    isCancelled: () -> Boolean = { false },
) {
    for (item in items) {
        if (isCancelled()) return
        val playable = DhikrPlaybackResolver.resolvePlayable(context, item)
        if (playable != null) {
            suspendCancellableCoroutine { cont ->
                audioPlayer.playResolved(playable, settings) { cont.resume(Unit) }
            }
        } else if (useTts) {
            val text = textFor(item).trim()
            if (text.isNotBlank()) {
                suspendCancellableCoroutine { cont ->
                    tts.speakAll(listOf(text)) { cont.resume(Unit) }
                }
            }
        }
    }
}
