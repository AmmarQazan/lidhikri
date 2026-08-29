package com.greendome.adhkar.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.greendome.adhkar.R
import com.greendome.adhkar.ui.screens.SettingSwitch
import com.greendome.adhkar.util.LockScreenPermissions

@Composable
fun AutoReminderLockScreenSetting(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    requestLockScreenAccess: (onReady: () -> Unit) -> Unit,
    titleRes: Int,
    hintRes: Int,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var needsFullScreenPermission by remember {
        mutableStateOf(LockScreenPermissions.needsFullScreenIntentPermission(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                needsFullScreenPermission =
                    LockScreenPermissions.needsFullScreenIntentPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SettingSwitch(
        label = stringResource(titleRes),
        checked = enabled,
        onChange = { checked ->
            if (checked) {
                requestLockScreenAccess { onEnabledChange(true) }
            } else {
                onEnabledChange(false)
            }
        }
    )
    Text(
        stringResource(hintRes),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
    )
    if (enabled && needsFullScreenPermission) {
        Text(
            stringResource(R.string.lock_screen_fsi_degraded_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
        )
        TextButton(
            onClick = { LockScreenPermissions.openFullScreenIntentSettings(context) }
        ) {
            Text(stringResource(R.string.lock_screen_fsi_open_settings))
        }
    }
}
