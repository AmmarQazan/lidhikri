package com.greendome.adhkar.audio

import android.content.Context
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.data.local.DhikrEntity
import com.greendome.adhkar.data.model.VoiceSettingsTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

suspend fun playAzkarItemsOrdered(
    context: Context,
    items: List<AzkarItemEntity>,
    audioPlayer: DhikrAudioPlayer,
    settings: SettingsRepository,
    isCancelled: () -> Boolean = { false },
    onMissingAudio: () -> Unit = {},
    onItemStart: (AzkarItemEntity) -> Unit = {},
    shouldSkipCurrent: () -> Boolean = { false },
    onSkipConsumed: () -> Unit = {},
): Boolean {
    var playedAny = false
    for (item in items) {
        if (isCancelled()) return playedAny
        if (audioPlayer.isAborted) return playedAny
        withContext(Dispatchers.Main.immediate) { onItemStart(item) }
        val times = item.repeatCount.coerceAtLeast(1)
        var rep = 0
        while (rep < times) {
            if (isCancelled()) return playedAny
            if (audioPlayer.isAborted) return playedAny
            val skipNow = withContext(Dispatchers.Main.immediate) { shouldSkipCurrent() }
            if (skipNow) {
                withContext(Dispatchers.Main.immediate) { onSkipConsumed() }
                break
            }
            val playable = AzkarPlaybackResolver.resolvePlayable(context, item)
            if (playable == null) {
                onMissingAudio()
                break
            }
            playedAny = true
            suspendCancellableCoroutine { cont ->
                cont.invokeOnCancellation { audioPlayer.stop() }
                audioPlayer.playResolved(playable, settings, VoiceSettingsTarget.AZKAR) {
                    if (cont.isActive) cont.resume(Unit)
                }
            }
            if (audioPlayer.isAborted) return playedAny
            val skipAfter = withContext(Dispatchers.Main.immediate) { shouldSkipCurrent() }
            if (skipAfter) {
                withContext(Dispatchers.Main.immediate) { onSkipConsumed() }
                break
            }
            rep++
        }
    }
    return playedAny
}

suspend fun playDhikrItemsOrdered(
    context: Context,
    items: List<DhikrEntity>,
    audioPlayer: DhikrAudioPlayer,
    settings: SettingsRepository,
    isCancelled: () -> Boolean = { false },
    onMissingAudio: () -> Unit = {},
): Boolean {
    var playedAny = false
    for (item in items) {
        if (isCancelled()) return playedAny
        val playable = DhikrPlaybackResolver.resolvePlayable(context, item)
        if (playable == null) {
            onMissingAudio()
        } else {
            playedAny = true
            suspendCancellableCoroutine { cont ->
                cont.invokeOnCancellation { audioPlayer.stop() }
                audioPlayer.playResolved(playable, settings) {
                    if (cont.isActive) cont.resume(Unit)
                }
            }
        }
    }
    return playedAny
}
