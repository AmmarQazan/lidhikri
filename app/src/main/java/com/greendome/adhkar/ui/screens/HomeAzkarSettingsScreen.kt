package com.greendome.adhkar.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.HomeAzkar
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.prayer.PrayerLocation
import com.greendome.adhkar.service.AutoAzkarEventPlayer
import com.greendome.adhkar.service.HomeGeofenceScheduler
import com.greendome.adhkar.util.RuntimePermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeAzkarSettingsScreen(
    settings: SettingsRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var location by remember { mutableStateOf(settings.homeLocation()) }
    var items by remember { mutableStateOf<List<AzkarItemEntity>>(emptyList()) }
    var enterKeys by remember { mutableStateOf(settings.homeEventKeys(HomeAzkar.Event.ENTER)) }
    var exitKeys by remember { mutableStateOf(settings.homeEventKeys(HomeAzkar.Event.EXIT)) }
    var showBackgroundPrompt by remember { mutableStateOf(false) }
    var statusTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        items = withContext(Dispatchers.IO) {
            AdhkarDatabase.get(context).azkarItemDao().getByCollection(HomeAzkar.COLLECTION_ID)
        }
        while (isActive) {
            statusTick++
            delay(2_000)
        }
    }

    fun persistAndRegister() {
        HomeGeofenceScheduler.register(context)
    }

    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        persistAndRegister()
    }

    val foregroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            persistAndRegister()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                !RuntimePermissions.hasBackgroundLocation(context)
            ) {
                showBackgroundPrompt = true
            }
        }
    }

    fun requestLocationIfNeeded() {
        when {
            !RuntimePermissions.hasFineLocation(context) -> {
                foregroundLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                !RuntimePermissions.hasBackgroundLocation(context) -> {
                showBackgroundPrompt = true
                persistAndRegister()
            }
            else -> persistAndRegister()
        }
    }

    fun saveLocation(picked: PrayerLocation) {
        location = picked
        settings.setHomeLocation(picked)
        requestLocationIfNeeded()
    }

    SettingsSubScreenScaffold(
        title = stringResource(R.string.home_azkar_settings_title),
        onBack = onBack,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SectionTitle(
                    title = stringResource(R.string.home_azkar_settings_title),
                    subtitle = stringResource(R.string.home_azkar_settings_subtitle)
                )
            }
            item {
                SectionTitle(
                    title = stringResource(R.string.home_azkar_location_section),
                    subtitle = stringResource(R.string.home_azkar_location_hint)
                )
            }
            item {
                HomeAddressPicker(
                    current = location,
                    onPicked = { saveLocation(it) }
                )
            }
            item {
                SectionTitle(title = stringResource(R.string.home_azkar_status_section))
            }
            item {
                val monitorStatus = remember(statusTick) { settings.homeMonitorStatus }
                val needsBackground = remember(statusTick) { settings.homeNeedsBackgroundPermission }
                val lastEnter = remember(statusTick) { settings.homeLastEnterAt }
                val lastExit = remember(statusTick) { settings.homeLastExitAt }
                val playError = remember(statusTick) { settings.homeLastPlayError }
                HomeAzkarMonitorStatus(
                    status = monitorStatus,
                    needsBackground = needsBackground,
                    lastEnterAt = lastEnter,
                    lastExitAt = lastExit,
                    playError = playError,
                )
            }
            item {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            AutoAzkarEventPlayer.playHome(
                                context,
                                HomeAzkar.Event.ENTER,
                                ignoreCooldown = true,
                                recordEvent = false,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.home_azkar_test_enter)) }
            }
            item {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            AutoAzkarEventPlayer.playHome(
                                context,
                                HomeAzkar.Event.EXIT,
                                ignoreCooldown = true,
                                recordEvent = false,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.home_azkar_test_exit)) }
            }
            item {
                SectionTitle(
                    title = stringResource(R.string.home_azkar_enter_section),
                    subtitle = stringResource(R.string.home_azkar_enter_hint)
                )
            }
            HomeAzkar.items().filter { it.event == HomeAzkar.Event.ENTER }.forEach { seed ->
                item(key = seed.id) {
                    HomeAzkarItemToggle(
                        seed = seed,
                        stored = items,
                        checked = seed.id in enterKeys,
                        onCheckedChange = { on ->
                            enterKeys = if (on) enterKeys + seed.id else enterKeys - seed.id
                            settings.setHomeEventKeys(HomeAzkar.Event.ENTER, enterKeys)
                        }
                    )
                }
            }
            item {
                SectionTitle(
                    title = stringResource(R.string.home_azkar_exit_section),
                    subtitle = stringResource(R.string.home_azkar_exit_hint)
                )
            }
            HomeAzkar.items().filter { it.event == HomeAzkar.Event.EXIT }.forEach { seed ->
                item(key = seed.id) {
                    HomeAzkarItemToggle(
                        seed = seed,
                        stored = items,
                        checked = seed.id in exitKeys,
                        onCheckedChange = { on ->
                            exitKeys = if (on) exitKeys + seed.id else exitKeys - seed.id
                            settings.setHomeEventKeys(HomeAzkar.Event.EXIT, exitKeys)
                        }
                    )
                }
            }
        }
    }

    if (showBackgroundPrompt && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        AlertDialog(
            onDismissRequest = { showBackgroundPrompt = false },
            title = { Text(stringResource(R.string.home_azkar_settings_title)) },
            text = { Text(stringResource(R.string.home_azkar_background_permission)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBackgroundPrompt = false
                        backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                ) { Text(stringResource(R.string.home_azkar_background_permission_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showBackgroundPrompt = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun HomeAzkarItemToggle(
    seed: HomeAzkar.ItemSeed,
    stored: List<AzkarItemEntity>,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val text = stored.find { seed.matches(it.textAr) }?.textAr ?: seed.text
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        SettingSwitch(
            label = seed.virtue.ifBlank { text.take(42) },
            checked = checked,
            onChange = onCheckedChange
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            modifier = Modifier.padding(top = 2.dp, end = 48.dp)
        )
    }
}
