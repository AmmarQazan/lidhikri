package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.service.AutoAzkarMonitorStatus
import com.greendome.adhkar.service.AutoAzkarSkip
import com.greendome.adhkar.ui.theme.formatLocalizedDigits

@Composable
fun HomeAzkarMonitorStatus(
    status: String,
    needsBackground: Boolean,
    lastEnterAt: Long,
    lastExitAt: Long,
    playError: String,
    skipReason: String = "",
    onRequestBackground: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(homeStatusRes(status)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (needsBackground) {
            if (onRequestBackground != null) {
                TextButton(
                    onClick = onRequestBackground,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_azkar_status_need_background),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.home_azkar_status_need_background),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        skipReasonText(skipReason)
        Text(
            text = stringResource(R.string.home_azkar_last_enter, formatMonitorTime(lastEnterAt)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = stringResource(R.string.home_azkar_last_exit, formatMonitorTime(lastExitAt)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
        if (playError.isNotBlank()) {
            Text(
                text = stringResource(R.string.auto_azkar_play_failed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun RidingAzkarMonitorStatus(
    status: String,
    lastPlayAt: Long,
    playError: String,
    skipReason: String = "",
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(ridingStatusRes(status)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        skipReasonText(skipReason)
        Text(
            text = stringResource(R.string.riding_azkar_last_play, formatMonitorTime(lastPlayAt)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (playError.isNotBlank()) {
            Text(
                text = stringResource(R.string.auto_azkar_play_failed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun skipReasonText(skipReason: String) {
    val res = when (skipReason) {
        AutoAzkarSkip.COOLDOWN -> R.string.auto_azkar_skip_cooldown
        AutoAzkarSkip.NO_ITEMS -> R.string.auto_azkar_skip_no_items
        AutoAzkarSkip.FRESH_REGISTER -> R.string.auto_azkar_skip_fresh_register
        else -> return
    }
    Text(
        text = stringResource(res),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun formatMonitorTime(millis: Long): String {
    if (millis <= 0L) return stringResource(R.string.home_azkar_never)
    return android.text.format.DateFormat.format("yyyy-MM-dd HH:mm", millis)
        .toString()
        .formatLocalizedDigits()
}

private fun homeStatusRes(status: String): Int = when (status) {
    AutoAzkarMonitorStatus.GEOFENCE -> R.string.home_azkar_status_geofence
    AutoAzkarMonitorStatus.PROXIMITY -> R.string.home_azkar_status_proximity
    AutoAzkarMonitorStatus.WAITING -> R.string.home_azkar_status_waiting
    AutoAzkarMonitorStatus.NO_FINE -> R.string.home_azkar_status_no_fine
    AutoAzkarMonitorStatus.NO_PLACE -> R.string.home_azkar_status_no_place
    AutoAzkarMonitorStatus.FAILED -> R.string.home_azkar_status_failed
    AutoAzkarMonitorStatus.LOCATION_OFF -> R.string.home_azkar_status_location_off
    else -> R.string.home_azkar_status_off
}

private fun ridingStatusRes(status: String): Int = when (status) {
    AutoAzkarMonitorStatus.OK -> R.string.riding_azkar_status_ok
    AutoAzkarMonitorStatus.WAITING -> R.string.riding_azkar_status_waiting
    AutoAzkarMonitorStatus.NO_ACTIVITY -> R.string.riding_azkar_status_no_activity
    AutoAzkarMonitorStatus.FAILED -> R.string.riding_azkar_status_failed
    else -> R.string.riding_azkar_status_off
}
