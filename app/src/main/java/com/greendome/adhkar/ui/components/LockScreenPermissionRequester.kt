package com.greendome.adhkar.ui.components

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.greendome.adhkar.R
import com.greendome.adhkar.util.LockScreenPermissions
import com.greendome.adhkar.util.RuntimePermissions

@Composable
fun rememberLockScreenAccessRequester(
    requireFullScreenIntent: Boolean = true
): (onReady: () -> Unit) -> Unit {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showFullScreenDialog by remember { mutableStateOf(false) }
    var awaitingSettingsReturn by remember { mutableStateOf(false) }

    fun finishPending(forceWithoutFullScreen: Boolean = false) {
        val action = pendingAction ?: return
        if (!RuntimePermissions.hasPostNotifications(context)) {
            pendingAction = null
            return
        }
        if (requireFullScreenIntent &&
            LockScreenPermissions.needsFullScreenIntentPermission(context) &&
            !forceWithoutFullScreen
        ) {
            showFullScreenDialog = true
            return
        }
        if (LockScreenPermissions.needsFullScreenIntentPermission(context) && forceWithoutFullScreen) {
            Toast.makeText(
                context,
                context.getString(R.string.lock_screen_fsi_still_needed),
                Toast.LENGTH_LONG
            ).show()
        }
        pendingAction = null
        showFullScreenDialog = false
        action()
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            finishPending()
        } else {
            pendingAction = null
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && awaitingSettingsReturn) {
                awaitingSettingsReturn = false
                if (pendingAction != null) {
                    if (LockScreenPermissions.canUseFullScreenIntent(context)) {
                        finishPending()
                    } else {
                        showFullScreenDialog = true
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showFullScreenDialog) {
        AlertDialog(
            onDismissRequest = {
                showFullScreenDialog = false
                pendingAction = null
            },
            title = { Text(stringResource(R.string.lock_screen_fsi_title)) },
            text = { Text(stringResource(R.string.lock_screen_fsi_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFullScreenDialog = false
                        awaitingSettingsReturn = true
                        if (!LockScreenPermissions.openFullScreenIntentSettings(context)) {
                            awaitingSettingsReturn = false
                            Toast.makeText(
                                context,
                                context.getString(R.string.lock_screen_fsi_settings_failed),
                                Toast.LENGTH_LONG
                            ).show()
                            showFullScreenDialog = true
                        }
                    }
                ) {
                    Text(stringResource(R.string.lock_screen_fsi_open_settings))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        finishPending(forceWithoutFullScreen = true)
                    }
                ) {
                    Text(stringResource(R.string.lock_screen_fsi_continue_anyway))
                }
            }
        )
    }

    return { onReady ->
        pendingAction = onReady
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !RuntimePermissions.hasPostNotifications(context) -> {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            requireFullScreenIntent &&
                LockScreenPermissions.needsFullScreenIntentPermission(context) -> {
                showFullScreenDialog = true
            }
            else -> finishPending()
        }
    }
}
