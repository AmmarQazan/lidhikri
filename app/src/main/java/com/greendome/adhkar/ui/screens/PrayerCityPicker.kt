package com.greendome.adhkar.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.greendome.adhkar.prayer.CityLocator
import com.greendome.adhkar.prayer.DeviceLocation
import com.greendome.adhkar.prayer.LocationMode
import com.greendome.adhkar.prayer.PrayerLocation
import com.greendome.adhkar.prayer.searchKnownCities
import com.greendome.adhkar.ui.theme.AppAccentGreen
import com.greendome.adhkar.ui.theme.AppFilledButtonColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PrayerCityPicker(
    current: PrayerLocation?,
    onPicked: (PrayerLocation, LocationMode) -> Unit,
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

    fun pick(city: PrayerLocation, mode: LocationMode) {
        lastSavedName = city.cityName
        onPicked(city, mode)
        query = city.cityName
        results = emptyList()
        status = context.getString(R.string.prayer_city_saved)
        keyboard?.hide()
    }

    fun runFullSearch() {
        val q = query.trim()
        if (q.length < 2 || searching) return
        searching = true
        status = null
        keyboard?.hide()
        scope.launch {
            val found = CityLocator.search(context, q)
            searching = false
            when {
                found.isEmpty() -> {
                    results = emptyList()
                    status = context.getString(R.string.prayer_city_none)
                }
                found.size == 1 -> pick(found.first(), LocationMode.MANUAL)
                else -> {
                    results = found
                    status = context.getString(R.string.prayer_city_tap_to_save)
                }
            }
        }
    }

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2 || q == lastSavedName?.trim() || q == current?.cityName?.trim()) {
            if (q.length < 2) results = emptyList()
            return@LaunchedEffect
        }
        delay(160)
        results = searchKnownCities(q).take(8)
        if (results.isNotEmpty()) {
            status = context.getString(R.string.prayer_city_tap_to_save)
        }
    }

    val gps = rememberGpsLocate(
        locatingMessage = stringResource(R.string.prayer_gps_locating),
        failedMessage = stringResource(R.string.prayer_gps_failed),
        preferGps = false,
        onStatus = { status = it },
        onLocation = { location ->
            val resolved = CityLocator.reverse(context, location.latitude, location.longitude)
            pick(resolved, LocationMode.GPS)
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            DeviceLocation.hasPermission(context)
        if (granted) {
            gps.request()
        } else {
            status = context.getString(R.string.prayer_location_denied)
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
                text = stringResource(R.string.prayer_location_unset),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.prayer_city_search)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { runFullSearch() })
        )
        results.forEach { city ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { pick(city, LocationMode.MANUAL) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = listOf(city.cityName, city.countryName).filter { it.isNotBlank() }
                        .joinToString(" — "),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        Button(
            onClick = { runFullSearch() },
            enabled = query.trim().length >= 2 && !searching,
            colors = AppFilledButtonColors(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.prayer_city_find))
        }
        Button(
            onClick = {
                status = null
                if (DeviceLocation.hasPermission(context)) {
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
            colors = AppFilledButtonColors(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.prayer_use_gps))
        }
        status?.let {
            val failed = it == stringResource(R.string.prayer_gps_failed) ||
                it == stringResource(R.string.prayer_location_denied)
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = if (failed) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.prayer_travel_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
