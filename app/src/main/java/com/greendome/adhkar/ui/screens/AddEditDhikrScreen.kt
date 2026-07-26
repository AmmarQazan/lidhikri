package com.greendome.adhkar.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.material3.Slider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.res.stringResource
import com.greendome.adhkar.audio.DhikrAudioPlayer
import com.greendome.adhkar.data.SettingsRepository
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.greendome.adhkar.R
import com.greendome.adhkar.audio.AudioDownloadManager
import com.greendome.adhkar.audio.VoiceRecorder
import com.greendome.adhkar.data.local.DhikrEntity
import com.greendome.adhkar.data.local.ReciterEntity
import com.greendome.adhkar.data.model.AudioSourceType
import com.greendome.adhkar.data.model.DhikrCategory
import com.greendome.adhkar.data.model.ScheduleType
import com.greendome.adhkar.ui.theme.GreenPrimary
import com.greendome.adhkar.ui.theme.GreenPrimaryDark
import com.greendome.adhkar.ui.theme.stringResourceDigits

@Composable
fun AddEditDhikrScreen(
    existing: DhikrEntity?,
    reciters: List<ReciterEntity>,
    isAdminDefault: Boolean = false,
    isAdminEdit: Boolean = false,
    adminTargetCategory: DhikrCategory? = null,
    adminNextSortOrder: Int = 0,
    showSchedule: Boolean = false,
    onSave: (DhikrEntity) -> Unit,
    onCancel: () -> Unit,
    onDelete: ((Long) -> Unit)? = null
) {
    val context = LocalContext.current
    val downloadManager = remember { AudioDownloadManager(context) }
    val voiceRecorder = remember { VoiceRecorder(context) }

    var textAr by remember { mutableStateOf(existing?.textAr ?: "") }
    var textEn by remember { mutableStateOf(existing?.textEn ?: "") }
    var textFr by remember { mutableStateOf(existing?.textFr ?: "") }
    var textEs by remember { mutableStateOf(existing?.textEs ?: "") }
    var audioType by remember { mutableStateOf(existing?.audioSourceType ?: AudioSourceType.NONE) }
    var audioPath by remember { mutableStateOf(existing?.audioPath) }
    var remoteUrl by remember { mutableStateOf(existing?.remoteAudioUrl ?: "") }
    var popup by remember { mutableStateOf(existing?.displayPopup ?: true) }
    var notification by remember { mutableStateOf(existing?.displayNotification ?: false) }
    var lockScreen by remember { mutableStateOf(existing?.displayLockScreen ?: true) }
    var audioOnly by remember { mutableStateOf(existing?.displayAudioOnly ?: false) }
    var audioText by remember { mutableStateOf(existing?.displayAudioText ?: true) }
    var isLongForm by remember { mutableStateOf(existing?.isLongForm ?: (adminTargetCategory == DhikrCategory.JAWAMI)) }
    var repeatCount by remember { mutableFloatStateOf((existing?.repeatCount ?: 1).toFloat()) }
    var isRecording by remember { mutableStateOf(false) }
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val settings = remember { SettingsRepository(context) }
    val previewPlayer = remember { DhikrAudioPlayer(context) }

    DisposableEffect(Unit) {
        onDispose { previewPlayer.stop() }
    }
    var scheduleForm by remember {
        mutableStateOf(existing?.let { scheduleFormFromEntity(it) } ?: ScheduleFormState())
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) { }
            audioPath = downloadManager.copyFromUri(it, "file_${System.currentTimeMillis()}.mp3")
            audioType = AudioSourceType.FILE
        }
    }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceRecorder.start()
            isRecording = true
        }
    }

    fun startRecording() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            voiceRecorder.start()
            isRecording = true
        } else {
            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun stopPreview() {
        previewPlayer.stop()
        isPreviewPlaying = false
    }

    fun previewAttachedAudio() {
        val path = audioPath ?: return
        if (isPreviewPlaying) {
            stopPreview()
            return
        }
        previewPlayer.play(path, settings) { isPreviewPlaying = false }
        isPreviewPlaying = true
    }

    val isUserDhikr = !isAdminDefault && (existing == null || !existing.isDefault)
    val userAudioOptions = listOf(
        AudioSourceType.RECORDED,
        AudioSourceType.FILE,
        AudioSourceType.NONE
    )
    val audioOptions = if (isUserDhikr) userAudioOptions else AudioSourceType.entries

    val canDelete = existing != null && !existing.isDefault && !isAdminDefault

    val isJawamiAdmin = (isAdminDefault || isAdminEdit) &&
        (adminTargetCategory == DhikrCategory.JAWAMI || existing?.category == DhikrCategory.JAWAMI)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            when {
                isAdminDefault && adminTargetCategory == DhikrCategory.JAWAMI -> stringResource(R.string.admin_add_jawami)
                isAdminDefault && adminTargetCategory == DhikrCategory.GENERAL -> stringResource(R.string.admin_add_short_tasbih)
                isAdminDefault && existing == null -> stringResource(R.string.admin_add_default)
                isAdminDefault || isAdminEdit -> stringResource(R.string.admin_edit_dhikr)
                existing == null -> stringResource(R.string.add_my_dhikr)
                else -> stringResource(R.string.edit_dhikr)
            }
        )
        if (isUserDhikr) {
            Card(
                colors = CardDefaults.cardColors(containerColor = GreenPrimary.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.phone_only_notice),
                    modifier = Modifier.padding(12.dp),
                    color = GreenPrimaryDark
                )
            }
        }
        SectionTitle(
            title = stringResource(R.string.dhikr_text_section),
            subtitle = if (isUserDhikr) stringResource(R.string.dhikr_text_section_hint) else null
        )
        OutlinedTextField(textAr, { textAr = it; saveError = null }, label = { Text(stringResource(R.string.text_arabic)) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(textEn, { textEn = it }, label = { Text(stringResource(R.string.text_english)) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(textFr, { textFr = it }, label = { Text(stringResource(R.string.text_french)) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(textEs, { textEs = it }, label = { Text(stringResource(R.string.text_spanish)) }, modifier = Modifier.fillMaxWidth())

        SectionTitle(
            title = stringResource(R.string.dhikr_audio_section),
            subtitle = stringResource(R.string.dhikr_audio_section_hint)
        )
        audioOptions.forEach { type ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = audioType == type, onClick = { audioType = type; saveError = null })
                Text(
                    when (type) {
                        AudioSourceType.BUILTIN -> stringResource(R.string.audio_builtin)
                        AudioSourceType.RECORDED -> stringResource(R.string.audio_record)
                        AudioSourceType.FILE -> stringResource(R.string.audio_file)
                        AudioSourceType.DOWNLOAD -> stringResource(R.string.audio_download)
                        AudioSourceType.NONE -> stringResource(R.string.audio_none)
                    }
                )
            }
        }

        when (audioType) {
            AudioSourceType.FILE -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { filePicker.launch(arrayOf("audio/*")) }) {
                    Text(stringResource(R.string.audio_file))
                }
                if (!audioPath.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { previewAttachedAudio() }) {
                            Icon(
                                if (isPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.preview_attached_voice),
                                tint = GreenPrimary
                            )
                        }
                        Text(stringResource(R.string.audio_file_attached), color = GreenPrimaryDark)
                    }
                }
            }
            AudioSourceType.RECORDED -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.dhikr_audio_record_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = GreenPrimaryDark
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = {
                        if (isRecording) {
                            audioPath = voiceRecorder.stop()?.absolutePath
                            audioType = AudioSourceType.RECORDED
                            isRecording = false
                            stopPreview()
                        } else {
                            startRecording()
                        }
                    }) {
                        Text(if (isRecording) stringResource(R.string.stop) else stringResource(R.string.record))
                    }
                    if (isRecording) {
                        Text(
                            stringResource(R.string.recording_in_progress),
                            modifier = Modifier.padding(start = 12.dp),
                            color = GreenPrimary
                        )
                    }
                }
                if (!audioPath.isNullOrBlank() && !isRecording) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { previewAttachedAudio() }) {
                            Icon(
                                if (isPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.preview_attached_voice),
                                tint = GreenPrimary
                            )
                        }
                        Text(
                            stringResource(R.string.recording_saved),
                            style = MaterialTheme.typography.bodyMedium,
                            color = GreenPrimaryDark
                        )
                    }
                }
            }
            AudioSourceType.DOWNLOAD -> OutlinedTextField(
                remoteUrl, { remoteUrl = it },
                label = { Text("URL") },
                modifier = Modifier.fillMaxWidth()
            )
            else -> Unit
        }

        if (saveError != null) {
            Text(saveError!!, color = MaterialTheme.colorScheme.error)
        }

        Text(stringResource(R.string.display_mode))
        Text(
            stringResource(R.string.display_mode_hint),
            style = MaterialTheme.typography.bodySmall,
            color = GreenPrimaryDark,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        DisplayCheck(
            stringResource(R.string.display_popup),
            popup,
            enabled = !notification && !audioOnly
        ) { checked ->
            popup = checked
            if (checked) audioOnly = false
        }
        DisplayCheck(
            label = stringResource(R.string.display_notification),
            checked = notification,
            enabled = !audioText && !audioOnly
        ) { checked ->
            notification = checked
            if (checked) {
                audioText = false
                audioOnly = false
                popup = false
                lockScreen = false
            }
        }
        Text(
            stringResource(R.string.display_notification_hint),
            style = MaterialTheme.typography.bodySmall,
            color = GreenPrimary,
            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
        )
        DisplayCheck(
            label = stringResource(R.string.display_lock),
            checked = lockScreen,
            enabled = !notification && !audioOnly
        ) { checked ->
            lockScreen = checked
            if (checked) audioOnly = false
        }
        Text(
            stringResource(R.string.display_lock_hint),
            style = MaterialTheme.typography.bodySmall,
            color = GreenPrimary,
            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
        )
        DisplayCheck(
            label = stringResource(R.string.display_audio_only),
            checked = audioOnly,
            enabled = !notification
        ) { checked ->
            audioOnly = checked
            if (checked) {
                notification = false
                popup = false
                lockScreen = false
                audioText = false
            }
        }
        DisplayCheck(
            label = stringResource(R.string.display_audio_text),
            checked = audioText,
            enabled = !notification && !audioOnly
        ) { checked ->
            audioText = checked
            if (checked) {
                notification = false
                audioOnly = false
            }
        }
        if (!isJawamiAdmin) {
            DisplayCheck(stringResource(R.string.read_mode), isLongForm) { isLongForm = it }
        }
        if (isJawamiAdmin) {
            Text(stringResourceDigits(R.string.azkar_repeat_label, repeatCount.toInt()))
            Slider(
                value = repeatCount,
                onValueChange = { repeatCount = it },
                valueRange = 1f..300f,
                steps = 20
            )
        }

        if (showSchedule) {
            ScheduleEditorSection(scheduleForm) { scheduleForm = it }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    when {
                        textAr.isBlank() -> saveError = context.getString(R.string.error_text_required)
                        audioType == AudioSourceType.RECORDED && audioPath.isNullOrBlank() ->
                            saveError = context.getString(R.string.error_recording_required)
                        audioType == AudioSourceType.FILE && audioPath.isNullOrBlank() ->
                            saveError = context.getString(R.string.error_audio_file_required)
                        notification && (audioText || audioOnly) ->
                            saveError = context.getString(R.string.error_notification_audio_conflict)
                        else -> {
                            stopPreview()
                            val resolvedCategory = when {
                                isAdminDefault && adminTargetCategory != null -> adminTargetCategory
                                scheduleForm.scheduleType != ScheduleType.ALWAYS && scheduleForm.hijriMonth == 10 -> DhikrCategory.EID
                                scheduleForm.scheduleType != ScheduleType.ALWAYS && scheduleForm.hijriMonth == 12 -> DhikrCategory.EID
                                isAdminEdit && existing != null -> existing.category
                                isAdminDefault -> DhikrCategory.SEASONAL
                                else -> DhikrCategory.CUSTOM
                            }
                            val resolvedLongForm = when {
                                resolvedCategory == DhikrCategory.JAWAMI -> true
                                isAdminEdit && existing != null -> existing.isLongForm
                                isAdminDefault -> isLongForm
                                else -> isLongForm
                            }
                            val base = (existing ?: DhikrEntity(textAr = textAr)).copy(
                        textAr = textAr,
                        textEn = textEn,
                        textFr = textFr,
                        textEs = textEs,
                        audioSourceType = audioType,
                        audioPath = audioPath,
                        remoteAudioUrl = remoteUrl.ifBlank { null },
                        isDefault = when {
                            isAdminDefault -> true
                            existing?.isDefault == true -> true
                            else -> false
                        },
                        isLongForm = resolvedLongForm,
                        repeatCount = if (resolvedCategory == DhikrCategory.JAWAMI) {
                            repeatCount.toInt().coerceAtLeast(1)
                        } else {
                            existing?.repeatCount ?: 1
                        },
                        sortOrder = existing?.sortOrder ?: adminNextSortOrder,
                        isEnabled = when {
                            resolvedCategory == DhikrCategory.JAWAMI && existing == null -> false
                            existing != null -> existing.isEnabled
                            else -> true
                        },
                        category = resolvedCategory,
                        displayPopup = popup,
                        displayNotification = notification,
                        displayLockScreen = lockScreen,
                        displayAudioOnly = audioOnly,
                        displayAudioText = audioText
                            )
                            onSave(if (showSchedule) base.applySchedule(scheduleForm) else base)
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.save)) }
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
        }
        if (canDelete && onDelete != null) {
            OutlinedButton(
                onClick = { onDelete(existing!!.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.delete_my_dhikr))
            }
        }
    }
}

@Composable
private fun DisplayCheck(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange, enabled = enabled)
        Text(label, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
    }
}
