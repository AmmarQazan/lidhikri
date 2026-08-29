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
    audioPlayer: DhikrAudioPlayer,
    settings: SettingsRepository,
    isCancelled: () -> Boolean = { false },
    onMissingAudio: () -> Unit = {},
): Boolean {
    var playedAny = false
    for (item in items) {
        if (isCancelled()) return playedAny
        repeat(item.repeatCount.coerceAtLeast(1)) {
            if (isCancelled()) return playedAny
            val playable = AzkarPlaybackResolver.resolvePlayable(context, item) ?: run {
                onMissingAudio()
                return@repeat
            }
            playedAny = true
            suspendCancellableCoroutine { cont ->
                audioPlayer.playResolved(playable, settings) { cont.resume(Unit) }
            }
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
                audioPlayer.playResolved(playable, settings) { cont.resume(Unit) }
            }
        }
    }
    return playedAny
}
