package com.greendome.adhkar.ui.screens

import android.app.Activity
import android.content.Intent
import android.location.Location
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.greendome.adhkar.R
import com.greendome.adhkar.prayer.DeviceLocation
import kotlinx.coroutines.launch

class GpsLocateHandle(
    val request: () -> Unit,
)

@Composable
fun rememberGpsLocate(
    locatingMessage: String,
    failedMessage: String,
    preferGps: Boolean = false,
    timeoutMs: Long = 12_000L,
    maxAgeMs: Long = 15 * 60_000L,
    onStatus: (String?) -> Unit,
    onLocation: suspend (Location) -> Unit,
): GpsLocateHandle {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun fetch() {
        scope.launch {
            onStatus(locatingMessage)
            val location = DeviceLocation.requestCurrent(
                context,
                timeoutMs = timeoutMs,
                maxAgeMs = maxAgeMs,
                preferGps = preferGps,
            )
            if (location == null) {
                onStatus(failedMessage)
                return@launch
            }
            onLocation(location)
        }
    }

    val enableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK || DeviceLocation.isEnabled(context)) {
            fetch()
        } else {
            onStatus(failedMessage)
        }
    }

    fun request() {
        scope.launch {
            when (val state = DeviceLocation.checkSettings(context, preferGps)) {
                DeviceLocation.SettingsState.Enabled -> fetch()
                is DeviceLocation.SettingsState.ResolutionRequired -> {
                    onStatus(context.getString(R.string.prayer_turn_on_location))
                    runCatching {
                        enableLauncher.launch(
                            IntentSenderRequest.Builder(state.sender).build()
                        )
                    }.onFailure {
                        openLocationSettings(context)
                        onStatus(context.getString(R.string.prayer_turn_on_location))
                    }
                }
                DeviceLocation.SettingsState.Unavailable -> {
                    openLocationSettings(context)
                    onStatus(context.getString(R.string.prayer_turn_on_location))
                }
            }
        }
    }

    return GpsLocateHandle(request = { request() })
}

private fun openLocationSettings(context: android.content.Context) {
    runCatching {
        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }
}
