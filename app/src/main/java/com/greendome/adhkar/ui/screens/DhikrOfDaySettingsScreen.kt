package com.greendome.adhkar.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.model.DhikrOfDayDisplayMode
import com.greendome.adhkar.util.RuntimePermissions
import com.greendome.adhkar.widget.DhikrOfDayManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DhikrOfDaySettingsScreen(
    settings: SettingsRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var enabled by remember { mutableStateOf(settings.dhikrOfDayEnabled) }
    var displayMode by remember { mutableStateOf(settings.dhikrOfDayDisplayMode) }
    var todayText by remember { mutableStateOf("") }
    var pendingLockScreenEnable by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (pendingLockScreenEnable) {
            pendingLockScreenEnable = false
            if (granted) {
                enabled = true
                settings.dhikrOfDayEnabled = true
                DhikrOfDayManager.refreshAsync(context)
            }
        }
    }

    fun refreshPreview() {
        scope.launch {
            todayText = withContext(Dispatchers.IO) {
                DhikrOfDayManager.getTodayText(context)
            }
        }
    }

    LaunchedEffect(enabled, displayMode) {
        if (enabled) {
            DhikrOfDayManager.refreshAsync(context)
            refreshPreview()
        } else {
            todayText = ""
        }
    }

    fun applyEnabled(value: Boolean) {
        if (value && displayMode == DhikrOfDayDisplayMode.LOCK_SCREEN &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !RuntimePermissions.hasPostNotifications(context)
        ) {
            pendingLockScreenEnable = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        enabled = value
        settings.dhikrOfDayEnabled = value
        if (value) {
            DhikrOfDayManager.refreshAsync(context)
            refreshPreview()
        } else {
            DhikrOfDayManager.refreshAsync(context)
            todayText = ""
        }
    }

    fun applyDisplayMode(mode: DhikrOfDayDisplayMode) {
        if (mode == DhikrOfDayDisplayMode.LOCK_SCREEN &&
            enabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !RuntimePermissions.hasPostNotifications(context)
        ) {
            pendingLockScreenEnable = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        displayMode = mode
        settings.dhikrOfDayDisplayMode = mode
        if (enabled) {
            DhikrOfDayManager.refreshAsync(context)
        }
    }

    SettingsSubScreenScaffold(
        title = stringResource(R.string.settings_dhikr_of_day_title),
        onBack = onBack,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.settings_dhikr_of_day_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                SettingSwitch(
                    label = stringResource(R.string.dhikr_of_day_enable),
                    checked = enabled,
                    onChange = ::applyEnabled
                )
            }

            if (enabled) {
                item {
                    SectionTitle(
                        title = stringResource(R.string.dhikr_of_day_display_section),
                        subtitle = stringResource(R.string.dhikr_of_day_display_hint)
                    )
                }
                item {
                    DhikrOfDayDisplayModeSelector(
                        selected = displayMode,
                        onSelected = ::applyDisplayMode
                    )
                }

                if (displayMode == DhikrOfDayDisplayMode.HOME_WIDGET) {
                    item {
                        Button(
                            onClick = {
                                val pinned = DhikrOfDayManager.requestPinWidget(context)
                                if (!pinned) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.dhikr_of_day_widget_manual_hint),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.dhikr_of_day_add_widget))
                        }
                    }
                    item {
                        Text(
                            stringResource(R.string.dhikr_of_day_widget_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    item {
                        Text(
                            stringResource(R.string.dhikr_of_day_lock_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    SectionTitle(title = stringResource(R.string.dhikr_of_day_preview_section))
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = todayText.ifBlank { stringResource(R.string.dhikr_of_day_loading) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                item {
                    Button(
                        onClick = {
                            DhikrOfDayManager.forceNewDhikr(context)
                            refreshPreview()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.dhikr_of_day_refresh))
                    }
                }
            }
        }
    }
}

@Composable
private fun DhikrOfDayDisplayModeSelector(
    selected: DhikrOfDayDisplayMode,
    onSelected: (DhikrOfDayDisplayMode) -> Unit,
    modifier: Modifier = Modifier
) {
    DhikrOfDayDisplayModeOption(
        label = stringResource(R.string.dhikr_of_day_mode_home_widget),
        selected = selected == DhikrOfDayDisplayMode.HOME_WIDGET,
        onSelect = { onSelected(DhikrOfDayDisplayMode.HOME_WIDGET) },
        modifier = modifier
    )
    DhikrOfDayDisplayModeOption(
        label = stringResource(R.string.dhikr_of_day_mode_lock_screen),
        selected = selected == DhikrOfDayDisplayMode.LOCK_SCREEN,
        onSelect = { onSelected(DhikrOfDayDisplayMode.LOCK_SCREEN) },
        modifier = modifier
    )
}

@Composable
private fun DhikrOfDayDisplayModeOption(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
