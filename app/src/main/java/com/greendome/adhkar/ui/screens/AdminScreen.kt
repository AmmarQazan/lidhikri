package com.greendome.adhkar.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.DhikrEntity
import com.greendome.adhkar.data.local.PendingPublishChangeEntity
import com.greendome.adhkar.data.model.AudioSourceType
import com.greendome.adhkar.data.model.ScheduleType
import com.greendome.adhkar.sync.PendingPublishCounts
import com.greendome.adhkar.sync.RemoteContentPublisher
import com.greendome.adhkar.ui.theme.AppCardColors
import com.greendome.adhkar.ui.theme.GoldDome
import com.greendome.adhkar.ui.theme.GreenPrimary
import com.greendome.adhkar.ui.theme.stringResourceDigits
import com.greendome.adhkar.util.DhikrScheduleMatcher
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun AdminScreen(
    isLoggedIn: Boolean,
    settings: SettingsRepository,
    onLogin: (String) -> Boolean,
    onLogout: () -> Unit,
    onClose: () -> Unit = {},
    onOpenDhikrSection: (AdminDhikrBucket) -> Unit,
    onManageAzkar: () -> Unit,
    onManageReciters: () -> Unit,
    onManageAdhanAudio: () -> Unit,
    onManagePrayerDefaults: () -> Unit,
    dhikrList: List<DhikrEntity>,
    azkarCollectionCount: Int,
    pendingPublishChanges: List<PendingPublishChangeEntity>,
    onImport: (List<DhikrEntity>) -> Unit,
    onPublishUpdates: suspend (onProgress: (String) -> Unit) -> RemoteContentPublisher.PublishResult
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var loggedIn by remember { mutableStateOf(isLoggedIn) }
    var pinError by remember { mutableStateOf(false) }
    var firebaseEmail by remember { mutableStateOf(settings.firebaseAdminEmail.orEmpty()) }
    var firebasePassword by remember { mutableStateOf(settings.firebaseAdminPassword.orEmpty()) }
    var publishInProgress by remember { mutableStateOf(false) }
    var publishStatus by remember { mutableStateOf<String?>(null) }
    var publishError by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            } ?: return@rememberLauncherForActivityResult
            val arr = JSONArray(text)
            val imported = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                DhikrEntity(
                    textAr = o.getString("textAr"),
                    textEn = o.optString("textEn"),
                    textFr = o.optString("textFr"),
                    textEs = o.optString("textEs"),
                    textTr = o.optString("textTr"),
                    textUr = o.optString("textUr"),
                    textId = o.optString("textId"),
                    textHi = o.optString("textHi"),
                    isDefault = true,
                    audioSourceType = AudioSourceType.valueOf(o.optString("audioSourceType", "NONE")),
                    remoteAudioUrl = o.optString("remoteAudioUrl").ifBlank { null },
                    audioPath = o.optString("audioPath").ifBlank { null },
                    scheduleType = ScheduleType.valueOf(o.optString("scheduleType", "ALWAYS")),
                    timeStartHour = o.optInt("timeStartHour", -1),
                    timeStartMinute = o.optInt("timeStartMinute", 0),
                    timeEndHour = o.optInt("timeEndHour", -1),
                    timeEndMinute = o.optInt("timeEndMinute", 0),
                    hijriMonth = o.optInt("hijriMonth", -1),
                    hijriDayStart = o.optInt("hijriDayStart", -1),
                    hijriDayEnd = o.optInt("hijriDayEnd", -1),
                    scheduleLabelAr = o.optString("scheduleLabelAr")
                )
            }
            onImport(imported)
        } catch (_: Exception) { }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        if (!loggedIn) {
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it; pinError = false },
                label = { Text(stringResource(R.string.admin_pin)) },
                modifier = Modifier.fillMaxWidth(),
                isError = pinError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true
            )
            if (pinError) {
                Text(
                    stringResource(R.string.admin_pin_wrong),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(
                onClick = {
                    val ok = onLogin(pin)
                    loggedIn = ok
                    pinError = !ok
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.admin_login))
            }
            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.cancel))
            }
        } else {
            Text(stringResource(R.string.nav_admin), fontWeight = FontWeight.Bold)
            Text(stringResourceDigits(R.string.today_hijri, DhikrScheduleMatcher.todayHijriFormatted()))

            Text(
                stringResource(R.string.admin_publish_section),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                stringResource(R.string.admin_remote_sync_hint),
                style = MaterialTheme.typography.bodySmall,
                color = GoldDome
            )
            OutlinedTextField(
                value = firebaseEmail,
                onValueChange = { firebaseEmail = it },
                label = { Text(stringResource(R.string.admin_firebase_email_label)) },
                placeholder = { Text(stringResource(R.string.admin_firebase_email_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !publishInProgress
            )
            OutlinedTextField(
                value = firebasePassword,
                onValueChange = { firebasePassword = it },
                label = { Text(stringResource(R.string.admin_firebase_password_label)) },
                placeholder = { Text(stringResource(R.string.admin_firebase_password_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                enabled = !publishInProgress
            )
            val pendingCounts = remember(pendingPublishChanges) {
                PendingPublishCounts.from(pendingPublishChanges)
            }
            Text(
                stringResource(R.string.admin_pending_title),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                pendingSummaryText(pendingCounts),
                style = MaterialTheme.typography.bodySmall,
                color = GoldDome
            )
            Button(
                onClick = {
                    settings.firebaseAdminEmail = firebaseEmail.trim()
                    settings.firebaseAdminPassword = firebasePassword
                    publishInProgress = true
                    publishError = false
                    publishStatus = null
                    scope.launch {
                        val result = onPublishUpdates { publishStatus = it }
                        publishInProgress = false
                        when (result) {
                            is RemoteContentPublisher.PublishResult.Success -> {
                                publishError = false
                                publishStatus = if (result.audioUploaded > 0) {
                                    context.getString(
                                        R.string.admin_publish_success_audio,
                                        result.version,
                                        result.audioUploaded
                                    )
                                } else {
                                    context.getString(
                                        R.string.admin_publish_success,
                                        result.version
                                    )
                                }
                            }
                            is RemoteContentPublisher.PublishResult.Failed -> {
                                publishError = true
                                publishStatus = result.reason
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !publishInProgress && firebaseEmail.isNotBlank() && firebasePassword.isNotBlank()
            ) {
                if (publishInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    if (publishInProgress) stringResource(R.string.admin_publish_in_progress)
                    else stringResource(R.string.admin_publish_updates)
                )
            }
            publishStatus?.let { status ->
                Text(
                    status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (publishError) MaterialTheme.colorScheme.error else GreenPrimary
                )
            }

            Button(onClick = { importLauncher.launch(arrayOf("application/json")) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.admin_import))
            }
            Button(
                onClick = {
                    val arr = JSONArray()
                    dhikrList.filter { it.isDefault }.forEach { d -> arr.put(dhikrToJson(d)) }
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, arr.toString())
                    }
                    context.startActivity(Intent.createChooser(share, null))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.admin_export)) }
            Text(
                stringResource(R.string.admin_export_dhikr_hint),
                style = MaterialTheme.typography.bodySmall,
                color = GreenPrimary
            )

            Button(onClick = onManageReciters, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.admin_manage_reciters))
            }
            Button(onClick = onManageAdhanAudio, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.admin_manage_adhan))
            }
            Button(onClick = onManagePrayerDefaults, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.admin_prayer_defaults))
            }

            Text(
                stringResource(R.string.admin_content_management),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            AdminSectionMenuCard(
                title = stringResource(R.string.builtin_short_tasbih),
                count = adminDhikrItems(dhikrList, AdminDhikrBucket.SHORT_TASBIH).size,
                onClick = { onOpenDhikrSection(AdminDhikrBucket.SHORT_TASBIH) }
            )
            AdminSectionMenuCard(
                title = stringResource(R.string.builtin_jawami),
                count = adminDhikrItems(dhikrList, AdminDhikrBucket.JAWAMI).size,
                onClick = { onOpenDhikrSection(AdminDhikrBucket.JAWAMI) }
            )
            AdminSectionMenuCard(
                title = stringResource(R.string.admin_manage_other_builtin),
                count = adminDhikrItems(dhikrList, AdminDhikrBucket.OTHER_BUILTIN).size,
                onClick = { onOpenDhikrSection(AdminDhikrBucket.OTHER_BUILTIN) }
            )
            AdminSectionMenuCard(
                title = stringResource(R.string.admin_azkar_sections),
                count = azkarCollectionCount,
                onClick = onManageAzkar
            )

            Button(onClick = { loggedIn = false; onLogout() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.cancel))
            }
        }
        }
    }
}

@Composable
private fun pendingSummaryText(counts: PendingPublishCounts): String {
    if (counts.isEmpty) return stringResource(R.string.admin_pending_none)
    val parts = buildList {
        if (counts.reciters > 0) add(stringResource(R.string.admin_pending_reciter, counts.reciters))
        if (counts.dhikr > 0) add(stringResource(R.string.admin_pending_dhikr, counts.dhikr))
        if (counts.audio > 0) add(stringResource(R.string.admin_pending_audio, counts.audio))
        if (counts.collections > 0) add(stringResource(R.string.admin_pending_collection, counts.collections))
        if (counts.azkarItems > 0) add(stringResource(R.string.admin_pending_azkar, counts.azkarItems))
    }
    return parts.joinToString(" · ")
}

@Composable
private fun AdminSectionMenuCard(
    title: String,
    count: Int?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = AppCardColors()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            if (count != null) {
                Text(
                    stringResourceDigits(R.string.admin_section_count, count),
                    style = MaterialTheme.typography.bodySmall,
                    color = GreenPrimary
                )
            }
        }
    }
}

private fun dhikrToJson(d: DhikrEntity) = JSONObject().apply {
    put("textAr", d.textAr)
    put("textEn", d.textEn)
    put("textFr", d.textFr)
    put("textEs", d.textEs)
    put("textTr", d.textTr)
    put("textUr", d.textUr)
    put("textId", d.textId)
    put("textHi", d.textHi)
    put("audioSourceType", d.audioSourceType.name)
    put("remoteAudioUrl", d.remoteAudioUrl ?: "")
    put("audioPath", d.audioPath ?: "")
    put("scheduleType", d.scheduleType.name)
    put("timeStartHour", d.timeStartHour)
    put("timeStartMinute", d.timeStartMinute)
    put("timeEndHour", d.timeEndHour)
    put("timeEndMinute", d.timeEndMinute)
    put("hijriMonth", d.hijriMonth)
    put("hijriDayStart", d.hijriDayStart)
    put("hijriDayEnd", d.hijriDayEnd)
    put("scheduleLabelAr", d.scheduleLabelAr)
}
