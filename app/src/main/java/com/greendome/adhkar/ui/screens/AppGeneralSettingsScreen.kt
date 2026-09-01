package com.greendome.adhkar.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.service.OfflineDownloadHelper
import com.greendome.adhkar.util.AppLanguages
import com.greendome.adhkar.util.RuntimePermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppGeneralSettingsScreen(
    settings: SettingsRepository,
    onLanguageChanged: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pauseCalls by remember { mutableStateOf(settings.pauseDuringCalls) }
    var pauseMedia by remember { mutableStateOf(settings.pauseDuringMedia) }
    var respectQuiet by remember { mutableStateOf(settings.respectQuietMode) }
    var flipToStop by remember { mutableStateOf(settings.flipToStopPlayback) }
    var langExpanded by remember { mutableStateOf(false) }
    val languages = AppLanguages.pickerPairs()
    var selectedLang by remember { mutableStateOf(settings.appLanguage) }
    var downloading by remember { mutableStateOf(false) }
    var pendingPauseCallsEnable by remember { mutableStateOf(false) }

    val phoneStatePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (pendingPauseCallsEnable) {
            pendingPauseCallsEnable = false
            if (granted) {
                pauseCalls = true
                settings.pauseDuringCalls = true
            }
        }
    }

    fun updatePauseDuringCalls(enabled: Boolean) {
        if (enabled && !RuntimePermissions.hasReadPhoneState(context)) {
            pendingPauseCallsEnable = true
            phoneStatePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        } else {
            pauseCalls = enabled
            settings.pauseDuringCalls = enabled
        }
    }

    SettingsSubScreenScaffold(
        title = stringResource(R.string.settings_app_general_title),
        onBack = onBack,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (languages.size > 1) {
                item { FieldLabel(stringResource(R.string.language)) }
                item {
                    ExposedDropdownMenuBox(expanded = langExpanded, onExpandedChange = { langExpanded = it }) {
                        OutlinedTextField(
                            value = languages.find { it.first == selectedLang }?.second ?: selectedLang,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(langExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = langExpanded, onDismissRequest = { langExpanded = false }) {
                            languages.forEach { (code, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        if (selectedLang != code) {
                                            selectedLang = code
                                            settings.appLanguage = code
                                            onLanguageChanged()
                                        }
                                        langExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item { SectionTitle(title = stringResource(R.string.general_settings)) }
            item {
                SettingSwitch(stringResource(R.string.pause_during_calls), pauseCalls) {
                    updatePauseDuringCalls(it)
                }
                Text(
                    stringResource(R.string.pause_during_calls_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }
            item {
                SettingSwitch(stringResource(R.string.pause_during_media), pauseMedia) {
                    pauseMedia = it; settings.pauseDuringMedia = it
                }
                Text(
                    stringResource(R.string.pause_during_media_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }
            item {
                SettingSwitch(stringResource(R.string.respect_quiet_mode), respectQuiet) {
                    respectQuiet = it; settings.respectQuietMode = it
                }
                Text(
                    stringResource(R.string.respect_quiet_mode_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }
            item {
                SettingSwitch(stringResource(R.string.flip_to_stop_playback), flipToStop) {
                    flipToStop = it; settings.flipToStopPlayback = it
                }
                Text(
                    stringResource(R.string.flip_to_stop_playback_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }

            item { SectionTitle(title = stringResource(R.string.permissions_section)) }
            item {
                Button(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        })
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.battery_optimization))
                }
            }
            item {
                Button(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                        })
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.permission_overlay))
                }
            }
            item {
                Button(
                    onClick = {
                        downloading = true
                        scope.launch(Dispatchers.IO) {
                            OfflineDownloadHelper.downloadAllPending(context)
                            downloading = false
                        }
                    },
                    enabled = !downloading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (downloading) stringResource(R.string.downloading) else stringResource(R.string.download_all))
                }
            }
        }
    }
}
