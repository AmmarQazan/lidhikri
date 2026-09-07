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

@Composable
fun HomeAddressPicker(
    current: PrayerLocation?,
    onPicked: (PrayerLocation) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf<String?>(null) }
    val locatingLabel = stringResource(R.string.home_azkar_gps_locating)
    val locating = status == locatingLabel || status == stringResource(R.string.prayer_turn_on_location)

    val gps = rememberGpsLocate(
        locatingMessage = locatingLabel,
        failedMessage = stringResource(R.string.home_azkar_gps_failed),
        preferGps = true,
        timeoutMs = 15_000L,
        maxAgeMs = 60_000L,
        onStatus = { status = it },
        onLocation = { location ->
            val resolved = PlaceLocator.reverse(context, location.latitude, location.longitude)
                .copy(latitude = location.latitude, longitude = location.longitude)
            onPicked(resolved)
            status = context.getString(R.string.home_azkar_saved)
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val fine = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            DeviceLocation.hasFinePermission(context)
        when {
            fine -> gps.request()
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
                    gps.request()
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
            val failed = it == stringResource(R.string.home_azkar_gps_failed) ||
                it == stringResource(R.string.home_azkar_permission_denied) ||
                it == stringResource(R.string.home_azkar_need_precise)
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = if (failed) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
