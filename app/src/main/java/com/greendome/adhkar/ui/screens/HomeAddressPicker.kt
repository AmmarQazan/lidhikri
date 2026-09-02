package com.greendome.adhkar.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.prayer.DeviceLocation
import com.greendome.adhkar.prayer.PlaceLocator
import com.greendome.adhkar.prayer.PrayerLocation
import com.greendome.adhkar.ui.theme.AppAccentGreen
import com.greendome.adhkar.ui.theme.AppFilledButtonColors
import kotlinx.coroutines.launch

@Composable
fun HomeAddressPicker(
    current: PrayerLocation?,
    onPicked: (PrayerLocation) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    var locating by remember { mutableStateOf(false) }

    fun locate() {
        if (locating) return
        locating = true
        status = null
        scope.launch {
            try {
                resolveHomeGps(context, onPicked) { status = it }
            } finally {
                locating = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val fine = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            DeviceLocation.hasFinePermission(context)
        when {
            fine -> locate()
            DeviceLocation.hasPermission(context) -> {
                status = context.getString(R.string.home_azkar_need_precise)
            }
            else -> status = context.getString(R.string.home_azkar_permission_denied)
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (current != null) {
            Text(
                text = buildString {
                    append(current.cityName)
                    if (current.countryName.isNotBlank()) {
                        append(" — ")
                        append(current.countryName)
                    }
                },
                style = MaterialTheme.typography.titleMedium,
                color = AppAccentGreen()
            )
        } else {
            Text(
                text = stringResource(R.string.home_azkar_location_unset),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(
            onClick = {
                status = null
                if (DeviceLocation.hasFinePermission(context)) {
                    locate()
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            enabled = !locating,
            colors = AppFilledButtonColors(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(
                    if (locating) R.string.home_azkar_gps_locating
                    else R.string.home_azkar_use_gps
                )
            )
        }
        status?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private suspend fun resolveHomeGps(
    context: android.content.Context,
    onPicked: (PrayerLocation) -> Unit,
    onStatus: (String?) -> Unit
) {
    onStatus(context.getString(R.string.home_azkar_gps_locating))
    val location = DeviceLocation.requestCurrent(
        context,
        timeoutMs = 20_000L,
        maxAgeMs = 0L,
        preferGps = true,
    )
    if (location == null) {
        onStatus(context.getString(R.string.home_azkar_gps_failed))
        return
    }
    val resolved = PlaceLocator.reverse(context, location.latitude, location.longitude)
        .copy(latitude = location.latitude, longitude = location.longitude)
    onPicked(resolved)
    onStatus(context.getString(R.string.home_azkar_saved))
}
