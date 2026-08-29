package com.greendome.adhkar.update

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.greendome.adhkar.R
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

val LocalPlayAppUpdate = staticCompositionLocalOf<PlayAppUpdateController?> { null }

@Composable
fun rememberPlayAppUpdateController(): PlayAppUpdateController {
    val context = LocalContext.current
    val activity = context as Activity
    val controller = remember(activity) { PlayAppUpdateController(activity) }
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        controller.onUpdateFlowResult(result.resultCode)
    }
    DisposableEffect(controller, launcher) {
        controller.attach(launcher)
        onDispose { controller.detach() }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, controller) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch { controller.onResume() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return controller
}

@Composable
fun ProvidePlayAppUpdate(
    controller: PlayAppUpdateController,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalPlayAppUpdate provides controller) {
        content()
        PlayAppUpdateDialogs(controller)
    }
}

@Composable
fun PlayAppUpdateDialogs(controller: PlayAppUpdateController) {
    if (controller.showAvailableDialog) {
        AlertDialog(
            onDismissRequest = { controller.snoozeFromDialog() },
            title = { Text(stringResource(R.string.app_update_dialog_title)) },
            text = { Text(stringResource(R.string.app_update_dialog_message)) },
            confirmButton = {
                TextButton(onClick = { controller.applyFromDialog() }) {
                    Text(stringResource(R.string.app_update_now))
                }
            },
            dismissButton = {
                TextButton(onClick = { controller.snoozeFromDialog() }) {
                    Text(stringResource(R.string.app_update_later))
                }
            },
        )
    }
    if (controller.showRestartDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.app_update_downloaded_title)) },
            text = { Text(stringResource(R.string.app_update_downloaded_message)) },
            confirmButton = {
                TextButton(onClick = { controller.restartToComplete() }) {
                    Text(stringResource(R.string.app_update_restart))
                }
            },
        )
    }
}
