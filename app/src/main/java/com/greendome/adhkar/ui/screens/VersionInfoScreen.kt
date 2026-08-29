package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.greendome.adhkar.sync.RemoteContentSync
import com.greendome.adhkar.sync.RemoteSyncResult
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.ui.theme.GoldDome
import com.greendome.adhkar.ui.theme.GreenPrimaryDark
import com.greendome.adhkar.ui.theme.stringResourceDigits
import com.greendome.adhkar.update.LocalPlayAppUpdate
import com.greendome.adhkar.update.PlayUpdateManualStatus
import com.greendome.adhkar.util.AppLanguages
import com.greendome.adhkar.util.AppUpdateInfoProvider
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun VersionInfoScreen(
    settings: SettingsRepository,
    lang: String,
    onBack: () -> Unit,
    onSyncContent: suspend () -> RemoteSyncResult,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var syncInProgress by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var syncError by remember { mutableStateOf(false) }
    var contentVersion by remember { mutableIntStateOf(settings.remoteContentVersion) }
    var lastSyncAt by remember { mutableLongStateOf(settings.lastRemoteSyncAt) }
    var lastSyncError by remember { mutableStateOf(settings.lastRemoteSyncError) }
    var remoteVersion by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current
    val appUpdateInfo = remember(lang) { AppUpdateInfoProvider.get(context, lang) }
    val playAppUpdate = LocalPlayAppUpdate.current

    fun refreshLocalSyncState() {
        contentVersion = settings.remoteContentVersion
        lastSyncAt = settings.lastRemoteSyncAt
        lastSyncError = settings.lastRemoteSyncError
    }

    suspend fun refreshRemoteVersion() {
        remoteVersion = RemoteContentSync.peekRemoteVersion()
    }

    LaunchedEffect(Unit) {
        refreshRemoteVersion()
    }

    val syncAtLabel = remember(lastSyncAt, lang) {
        formatSyncTimestamp(lastSyncAt, lang)
    }

    SettingsSubScreenScaffold(
        title = stringResource(R.string.version_info_section),
        onBack = onBack,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                VersionInfoCard(
                    appName = stringResource(R.string.app_name),
                    versionName = appUpdateInfo.versionName,
                    updatedAtLabel = appUpdateInfo.updatedAtLabel
                )
            }
            if (playAppUpdate != null) {
                item {
                    AppUpdateCard(
                        busy = playAppUpdate.busy,
                        status = playAppUpdate.lastManualStatus,
                        onCheckAndApply = {
                            scope.launch { playAppUpdate.checkNowAndApply() }
                        }
                    )
                }
            }
            item {
                ContentSyncCard(
                    contentVersion = contentVersion,
                    remoteVersion = remoteVersion,
                    syncAtLabel = syncAtLabel,
                    syncError = lastSyncError,
                    syncInProgress = syncInProgress,
                    syncMessage = syncMessage,
                    syncMessageIsError = syncError,
                    onCheckUpdates = {
                        syncInProgress = true
                        syncMessage = null
                        scope.launch {
                            when (val result = onSyncContent()) {
                                is RemoteSyncResult.Updated -> {
                                    syncError = false
                                    refreshLocalSyncState()
                                    refreshRemoteVersion()
                                    syncMessage = context.getString(
                                        R.string.content_sync_updated,
                                        result.version
                                    )
                                }
                                is RemoteSyncResult.UpToDate -> {
                                    syncError = false
                                    refreshLocalSyncState()
                                    remoteVersion = result.version
                                    syncMessage = context.getString(
                                        R.string.content_sync_up_to_date,
                                        result.version
                                    )
                                }
                                is RemoteSyncResult.Failed -> {
                                    syncError = true
                                    refreshLocalSyncState()
                                    syncMessage = result.reason
                                }
                            }
                            syncInProgress = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun VersionInfoCard(
    appName: String,
    versionName: String,
    updatedAtLabel: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.version_info_title, appName, versionName),
                style = MaterialTheme.typography.labelLarge,
                color = GreenPrimaryDark
            )
            VersionInfoRow(
                label = stringResource(R.string.version_info_version_label),
                value = versionName
            )
            VersionInfoRow(
                label = stringResource(R.string.version_info_updated_at),
                value = updatedAtLabel
            )
        }
    }
}

@Composable
private fun AppUpdateCard(
    busy: Boolean,
    status: PlayUpdateManualStatus?,
    onCheckAndApply: () -> Unit
) {
    val statusText = when (status) {
        PlayUpdateManualStatus.Checking -> stringResource(R.string.app_update_checking)
        PlayUpdateManualStatus.UpToDate -> stringResource(R.string.app_update_up_to_date)
        is PlayUpdateManualStatus.Available -> stringResource(R.string.app_update_available)
        PlayUpdateManualStatus.Downloaded -> stringResource(R.string.app_update_downloaded_message)
        PlayUpdateManualStatus.Cancelled -> stringResource(R.string.app_update_cancelled)
        PlayUpdateManualStatus.NotFromPlay -> stringResource(R.string.app_update_not_from_play)
        PlayUpdateManualStatus.Failed -> stringResource(R.string.app_update_failed)
        null -> null
    }
    val statusIsError = status is PlayUpdateManualStatus.Failed ||
        status is PlayUpdateManualStatus.Cancelled ||
        status is PlayUpdateManualStatus.NotFromPlay

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.app_update_section),
                style = MaterialTheme.typography.labelLarge,
                color = GreenPrimaryDark
            )
            Text(
                stringResource(R.string.app_update_section_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!statusText.isNullOrBlank()) {
                VersionInfoRow(
                    label = stringResource(R.string.content_sync_status_label),
                    value = statusText,
                    valueColor = if (statusIsError) {
                        MaterialTheme.colorScheme.error
                    } else if (status is PlayUpdateManualStatus.Available) {
                        GoldDome
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
            Button(
                onClick = onCheckAndApply,
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    if (busy) stringResource(R.string.app_update_checking)
                    else stringResource(R.string.app_update_check)
                )
            }
        }
    }
}

@Composable
private fun ContentSyncCard(
    contentVersion: Int,
    remoteVersion: Int?,
    syncAtLabel: String,
    syncError: String?,
    syncInProgress: Boolean,
    syncMessage: String?,
    syncMessageIsError: Boolean,
    onCheckUpdates: () -> Unit
) {
    val updateAvailable = remoteVersion != null && remoteVersion > contentVersion

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.content_sync_section),
                style = MaterialTheme.typography.labelLarge,
                color = GreenPrimaryDark
            )
            Text(
                stringResource(R.string.content_sync_explain),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            VersionInfoRow(
                label = stringResource(R.string.content_version_label),
                value = if (contentVersion > 0) {
                    stringResourceDigits(R.string.content_version_value, contentVersion)
                } else {
                    stringResource(R.string.content_version_none)
                }
            )
            VersionInfoRow(
                label = stringResource(R.string.content_remote_version_label),
                value = when (remoteVersion) {
                    null -> stringResource(R.string.content_remote_version_unknown)
                    else -> stringResourceDigits(R.string.content_version_value, remoteVersion)
                }
            )
            if (updateAvailable) {
                Text(
                    stringResource(R.string.content_update_available),
                    style = MaterialTheme.typography.bodySmall,
                    color = GoldDome
                )
            }
            VersionInfoRow(
                label = stringResource(R.string.content_sync_at_label),
                value = syncAtLabel.ifBlank { stringResource(R.string.content_sync_never) }
            )
            if (!syncError.isNullOrBlank()) {
                VersionInfoRow(
                    label = stringResource(R.string.content_sync_error_label),
                    value = syncError,
                    valueColor = MaterialTheme.colorScheme.error
                )
            }
            if (!syncMessage.isNullOrBlank()) {
                VersionInfoRow(
                    label = stringResource(R.string.content_sync_status_label),
                    value = syncMessage,
                    valueColor = if (syncMessageIsError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
            OutlinedButton(
                onClick = onCheckUpdates,
                modifier = Modifier.fillMaxWidth(),
                enabled = !syncInProgress
            ) {
                Text(
                    if (syncInProgress) stringResource(R.string.content_sync_checking)
                    else stringResource(R.string.content_sync_check_updates)
                )
            }
        }
    }
}

@Composable
private fun VersionInfoRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).padding(end = 12.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatSyncTimestamp(epochMillis: Long, lang: String): String {
    if (epochMillis <= 0L) return ""
    val locale = AppLanguages.locale(lang)
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", locale)
    return formatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
}
