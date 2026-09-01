package com.greendome.adhkar.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.prayer.DeviceLocation
import com.greendome.adhkar.prayer.PlaceLocator
import com.greendome.adhkar.prayer.PrayerLocation
import com.greendome.adhkar.ui.theme.AppAccentGreen
import com.greendome.adhkar.ui.theme.AppFilledButtonColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeAddressPicker(
    current: PrayerLocation?,
    onPicked: (PrayerLocation) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var query by remember { mutableStateOf(current?.cityName.orEmpty()) }
    var results by remember { mutableStateOf<List<PrayerLocation>>(emptyList()) }
    var status by remember { mutableStateOf<String?>(null) }
    var searching by remember { mutableStateOf(false) }
    var lastSavedName by remember { mutableStateOf(current?.cityName) }

    fun pick(place: PrayerLocation) {
        lastSavedName = place.cityName
        onPicked(place)
        query = place.cityName
        results = emptyList()
        status = context.getString(R.string.home_azkar_saved)
        keyboard?.hide()
    }

    fun runSearch() {
        val q = query.trim()
        if (q.length < 2 || searching) return
        searching = true
        status = null
        keyboard?.hide()
        scope.launch {
            val found = PlaceLocator.search(context, q, DeviceLocation.lastKnown(context))
            searching = false
            if (found.isEmpty()) {
                results = emptyList()
                status = context.getString(R.string.home_azkar_none)
            } else {
                results = found
                status = context.getString(R.string.home_azkar_tap_to_save)
            }
        }
    }

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2 || q == lastSavedName?.trim() || q == current?.cityName?.trim()) {
            if (q.length < 2) results = emptyList()
            return@LaunchedEffect
        }
        delay(500)
        val found = PlaceLocator.search(context, q, DeviceLocation.lastKnown(context))
        results = found.take(8)
        if (results.isNotEmpty()) {
            status = context.getString(R.string.home_azkar_tap_to_save)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            scope.launch { resolveHomeGps(context, ::pick) { status = it } }
        } else {
            status = context.getString(R.string.home_azkar_permission_denied)
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
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.home_azkar_search)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { runSearch() })
        )
        results.forEach { place ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { pick(place) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = listOf(place.cityName, place.countryName).filter { it.isNotBlank() }
                        .joinToString(" — "),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        Button(
            onClick = { runSearch() },
            enabled = query.trim().length >= 2 && !searching,
            colors = AppFilledButtonColors(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.home_azkar_find))
        }
        Button(
            onClick = {
                status = null
                if (DeviceLocation.hasFinePermission(context) || DeviceLocation.hasPermission(context)) {
                    scope.launch { resolveHomeGps(context, ::pick) { status = it } }
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            colors = AppFilledButtonColors(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.home_azkar_use_gps))
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
    val location = DeviceLocation.requestCurrent(context)
    if (location == null) {
        onStatus(context.getString(R.string.home_azkar_gps_failed))
        return
    }
    onPicked(PlaceLocator.reverse(context, location.latitude, location.longitude))
}
