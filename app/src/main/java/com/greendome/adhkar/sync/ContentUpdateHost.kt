package com.greendome.adhkar.sync

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.AdhkarApplication
import com.greendome.adhkar.R
import kotlinx.coroutines.launch

@Composable
fun rememberContentUpdateController(): ContentUpdateController {
    val app = LocalContext.current.applicationContext as AdhkarApplication
    return remember(app) { ContentUpdateController(app) }
}

@Composable
fun ContentUpdateDialogs(controller: ContentUpdateController) {
    if (!controller.showDialog) return
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!controller.applying) controller.later() },
        title = { Text(stringResource(R.string.content_update_dialog_title)) },
        text = {
            Column {
                Text(stringResource(R.string.content_update_dialog_message))
                val error = controller.applyError
                if (!error.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { scope.launch { controller.apply() } },
                enabled = !controller.applying,
            ) {
                if (controller.applying) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text(
                    if (controller.applying) stringResource(R.string.content_update_applying)
                    else stringResource(R.string.content_update_now)
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = { controller.later() },
                enabled = !controller.applying,
            ) {
                Text(stringResource(R.string.app_update_later))
            }
        },
    )
}
