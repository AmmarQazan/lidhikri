package com.greendome.adhkar.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.audio.TtsPlaybackState
import com.greendome.adhkar.ui.theme.GreenPrimaryDark

@Composable
fun PlaybackControlBar(
    playbackState: TtsPlaybackState,
    canPause: Boolean,
    canResume: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    playEnabled: Boolean,
    modifier: Modifier = Modifier,
    playLabel: String = stringResource(R.string.azkar_play_tts),
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(
            onClick = onStop,
            enabled = playbackState != TtsPlaybackState.IDLE,
            modifier = Modifier.size(40.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = GreenPrimaryDark,
                disabledContentColor = GreenPrimaryDark.copy(alpha = 0.3f),
            ),
        ) {
            Icon(
                Icons.Default.Stop,
                contentDescription = stringResource(R.string.azkar_stop),
            )
        }
        IconButton(
            onClick = { if (canResume) onResume() else onPause() },
            enabled = canPause || canResume,
            modifier = Modifier.size(40.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = GreenPrimaryDark,
                disabledContentColor = GreenPrimaryDark.copy(alpha = 0.3f),
            ),
        ) {
            Icon(
                imageVector = if (canResume) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = stringResource(
                    if (canResume) R.string.azkar_resume else R.string.azkar_pause
                ),
            )
        }
        OutlinedButton(
            onClick = onPlay,
            enabled = playEnabled,
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                playLabel,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }
    }
}
