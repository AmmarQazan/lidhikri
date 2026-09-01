package com.greendome.adhkar.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.greendome.adhkar.R
import com.greendome.adhkar.audio.AudioDownloadManager
import com.greendome.adhkar.audio.DhikrAudioPlayer
import com.greendome.adhkar.audio.VoiceRecorder
import com.greendome.adhkar.data.BundledAdhanSeed
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AdhanAudioEntity
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.model.AutoReminderPresentation
import com.greendome.adhkar.prayer.AdhanAudioResolver
import com.greendome.adhkar.prayer.AdhanSoundMode
import com.greendome.adhkar.prayer.AsrMadhabPref
import com.greendome.adhkar.prayer.CalculationMethodPref
import com.greendome.adhkar.prayer.DstMode
import com.greendome.adhkar.prayer.PrayerAlertSettings
import com.greendome.adhkar.prayer.PrayerConfig
import com.greendome.adhkar.prayer.PrayerCountryDefaults
import com.greendome.adhkar.prayer.PrayerName
import com.greendome.adhkar.prayer.PrayerTimesCalculator
import com.greendome.adhkar.prayer.TimezoneMode
import com.greendome.adhkar.service.PrayerAlarms
import com.greendome.adhkar.ui.components.AutoReminderLockScreenSetting
import com.greendome.adhkar.ui.components.rememberLockScreenAccessRequester
import com.greendome.adhkar.ui.theme.formatLocalizedDigits
import com.greendome.adhkar.widget.PrayerTimesWidgetManager
import java.io.File
import java.util.TimeZone

@Composable
fun PrayerSettingsHubScreen(
    settings: SettingsRepository,
    onOpenTimes: () -> Unit,
    onOpenAdhan: () -> Unit,
    onOpenRespect: () -> Unit,
    onOpenQibla: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var respectOn by remember { mutableStateOf(settings.respectPrayerTime) }
    SettingsSubScreenScaffold(
        title = stringResource(R.string.settings_prayer_title),
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
                    title = stringResource(R.string.settings_prayer_title),
                    subtitle = stringResource(R.string.settings_prayer_hint)
                )
            }
            item {
                SettingSwitch(stringResource(R.string.prayer_respect_enable), respectOn) {
                    respectOn = it
                    settings.respectPrayerTime = it
                    PrayerAlarms.rescheduleAll(context)
                }
            }
            item {
                SettingsNavCard(
                    title = stringResource(R.string.prayer_times_section),
                    subtitle = stringResource(R.string.prayer_times_section_hint),
                    onClick = onOpenTimes
                )
            }
            item {
                SettingsNavCard(
                    title = stringResource(R.string.adhan_section),
                    subtitle = stringResource(R.string.adhan_section_hint),
                    onClick = onOpenAdhan
                )
            }
            item {
                SettingsNavCard(
                    title = stringResource(R.string.prayer_respect_title),
                    subtitle = stringResource(R.string.prayer_respect_subtitle),
                    onClick = onOpenRespect
                )
            }
            item {
                SettingsNavCard(
                    title = stringResource(R.string.qibla_title),
                    subtitle = stringResource(R.string.qibla_subtitle),
                    onClick = onOpenQibla
                )
            }
        }
    }
}

@Composable
fun PrayerTimesSettingsScreen(
    settings: SettingsRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var location by remember { mutableStateOf(settings.prayerConfig().location) }
    var travelAuto by remember { mutableStateOf(settings.prayerTravelAutoUpdate) }
    var tzMode by remember { mutableStateOf(settings.prayerTimezoneMode) }
    var tzId by remember { mutableStateOf(settings.prayerTimezoneId.ifBlank { TimeZone.getDefault().id }) }
    var dstMode by remember { mutableStateOf(settings.prayerDstMode) }
    var method by remember { mutableStateOf(settings.prayerCalculationMethod) }
    var madhab by remember { mutableStateOf(settings.prayerAsrMadhab) }
    var imsak by remember { mutableIntStateOf(settings.prayerImsakOffsetMinutes) }
    var timesTick by remember { mutableIntStateOf(0) }

    fun persist() {
        PrayerAlarms.rescheduleAll(context)
        timesTick++
    }

    val config = remember(location, tzMode, tzId, dstMode, method, madhab, imsak, timesTick) {
        settings.prayerConfig()
    }
    val today = remember(config, timesTick) { PrayerTimesCalculator.timesFor(config) }
    val zone = remember(config, timesTick) { PrayerTimesCalculator.zoneId(config) }
    val format = settings.azkarClockHourFormat
    val am = stringResource(R.string.clock_period_am)
    val pm = stringResource(R.string.clock_period_pm)

    SettingsSubScreenScaffold(
        title = stringResource(R.string.prayer_times_section),
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
                    title = stringResource(R.string.prayer_location_section),
                    subtitle = stringResource(R.string.prayer_location_hint)
                )
            }
            item {
                PrayerCityPicker(
                    current = location,
                    onPicked = { picked, mode ->
                        location = picked
                        settings.setPrayerLocation(picked, mode)
                        persist()
                    }
                )
            }
            item {
                SettingSwitch(stringResource(R.string.prayer_travel_auto), travelAuto) {
                    travelAuto = it
                    settings.prayerTravelAutoUpdate = it
                    if (it && location != null) {
                        settings.prayerLocationMode = com.greendome.adhkar.prayer.LocationMode.GPS
                    }
                }
                Text(
                    stringResource(R.string.prayer_travel_auto_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            item {
                SectionTitle(
                    title = stringResource(R.string.prayer_time_section),
                    subtitle = stringResource(R.string.prayer_time_hint)
                )
            }
            item {
                PrayerEnumDropdown(
                    label = stringResource(R.string.prayer_timezone_mode),
                    value = timezoneModeLabel(tzMode),
                    options = TimezoneMode.entries.map { it to timezoneModeLabel(it) }
                ) {
                    tzMode = it
                    settings.prayerTimezoneMode = it
                    persist()
                }
            }
            if (tzMode == TimezoneMode.MANUAL) {
                item {
                    PrayerEnumDropdown(
                        label = stringResource(R.string.prayer_timezone),
                        value = tzId,
                        options = PrayerCountryDefaults.commonTimezones.map { it to it }
                    ) {
                        tzId = it
                        settings.prayerTimezoneId = it
                        persist()
                    }
                }
            }
            item {
                PrayerEnumDropdown(
                    label = stringResource(R.string.prayer_dst_mode),
                    value = dstModeLabel(dstMode),
                    options = DstMode.entries.map { it to dstModeLabel(it) }
                ) {
                    dstMode = it
                    settings.prayerDstMode = it
                    persist()
                }
                Text(
                    stringResource(R.string.prayer_dst_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            item {
                PrayerEnumDropdown(
                    label = stringResource(R.string.prayer_method),
                    value = methodLabel(method),
                    options = CalculationMethodPref.entries.map { it to methodLabel(it) }
                ) {
                    method = it
                    settings.prayerCalculationMethod = it
                    persist()
                }
            }
            item {
                PrayerEnumDropdown(
                    label = stringResource(R.string.prayer_madhab),
                    value = madhabLabel(madhab),
                    options = AsrMadhabPref.entries.map { it to madhabLabel(it) }
                ) {
                    madhab = it
                    settings.prayerAsrMadhab = it
                    persist()
                }
            }
            item {
                PrayerMinutesSlider(
                    label = stringResource(R.string.prayer_name_imsak),
                    minutes = imsak,
                    min = PrayerConfig.IMSAK_MIN,
                    max = PrayerConfig.IMSAK_MAX,
                    onChange = {
                        imsak = it
                        settings.prayerImsakOffsetMinutes = it
                        persist()
                    }
                )
                Text(
                    stringResource(R.string.prayer_imsak_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                val timesSubtitle = if (today == null) {
                    stringResource(R.string.prayer_times_need_city)
                } else {
                    buildString {
                        today.imsakMillis?.let {
                            append(stringResource(R.string.prayer_name_imsak))
                            append(' ')
                            append(formatPrayerInstant(it, zone, format, am, pm).formatLocalizedDigits())
                            append(" · ")
                        }
                        PrayerName.entries.forEachIndexed { index, prayer ->
                            if (index > 0) append(" · ")
                            if (prayer == PrayerName.DHUHR) {
                                today.sunriseMillis?.let {
                                    append(stringResource(R.string.prayer_name_sunrise))
                                    append(' ')
                                    append(formatPrayerInstant(it, zone, format, am, pm).formatLocalizedDigits())
                                    append(" · ")
                                }
                            }
                            val millis = today.timeOf(prayer) ?: return@forEachIndexed
                            append(prayerLabel(prayer))
                            append(' ')
                            append(formatPrayerInstant(millis, zone, format, am, pm).formatLocalizedDigits())
                        }
                    }
                }
                ExpandableSettingsCard(
                    title = stringResource(R.string.prayer_times_today),
                    subtitle = "${stringResource(R.string.prayer_offset_hint)}\n$timesSubtitle"
                ) {
                    if (today == null) {
                        Text(
                            stringResource(R.string.prayer_times_need_city),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        today.imsakMillis?.let {
                            Text(
                                "${stringResource(R.string.prayer_name_imsak)}  ${formatPrayerInstant(it, zone, format, am, pm).formatLocalizedDigits()}",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        PrayerName.entries.forEach { prayer ->
                            if (prayer == PrayerName.DHUHR) {
                                today.sunriseMillis?.let {
                                    Text(
                                        "${stringResource(R.string.prayer_name_sunrise)}  ${formatPrayerInstant(it, zone, format, am, pm).formatLocalizedDigits()}",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                            val millis = today.timeOf(prayer) ?: return@forEach
                            PrayerOffsetRow(
                                title = prayerLabel(prayer),
                                clock = formatPrayerInstant(millis, zone, format, am, pm).formatLocalizedDigits(),
                                offset = settings.prayerMinuteOffset(prayer),
                                onChange = { next ->
                                    settings.setPrayerMinuteOffset(prayer, next)
                                    persist()
                                }
                            )
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        val pinned = PrayerTimesWidgetManager.requestPin(context)
                        if (!pinned) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.prayer_widget_manual_hint),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.prayer_widget_add))
                }
            }
        }
    }
}

@Composable
fun AdhanSettingsScreen(
    settings: SettingsRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val downloader = remember { AudioDownloadManager(context) }
    val preview = remember { DhikrAudioPlayer(context) }
    val voiceRecorder = remember { VoiceRecorder(context) }
    var pendingPrayer by remember { mutableStateOf<PrayerName?>(null) }
    var pendingRecordPrayer by remember { mutableStateOf<PrayerName?>(null) }
    var recordingPrayer by remember { mutableStateOf<PrayerName?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var tick by remember { mutableIntStateOf(0) }
    var presentation by remember { mutableStateOf(settings.adhanPresentation) }
    var lockScreenEnabled by remember { mutableStateOf(settings.adhanAutoLockScreenEnabled) }
    var vibrateOn by remember { mutableStateOf(settings.adhanVibrate) }
    val requestLockScreenAccess = rememberLockScreenAccessRequester()
    val catalog by AdhkarDatabase.get(context).adhanAudioDao().observeActive()
        .collectAsState(initial = emptyList())
    val lang = settings.appLanguage
    var browsePrayer by remember { mutableStateOf<PrayerName?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            preview.stop()
            if (isRecording) voiceRecorder.stop()
        }
    }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val prayer = pendingRecordPrayer ?: return@rememberLauncherForActivityResult
        if (granted) {
            voiceRecorder.start()
            recordingPrayer = prayer
            isRecording = true
        }
        pendingRecordPrayer = null
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val prayer = pendingPrayer ?: return@rememberLauncherForActivityResult
        uri ?: return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) { }
        val path = downloader.copyFromUri(uri, "adhan_${prayer.name.lowercase()}.audio")
        if (path != null) {
            val current = settings.adhanAlert(prayer)
            settings.setAdhanAlert(
                prayer,
                current.copy(soundMode = AdhanSoundMode.CUSTOM, customPath = path)
            )
            tick++
            PrayerAlarms.rescheduleAll(context)
        }
        pendingPrayer = null
    }

    val browsing = browsePrayer
    if (browsing != null) {
        AdhanCatalogPickerScreen(
            prayer = browsing,
            catalog = catalog,
            lang = lang,
            onBack = { browsePrayer = null },
            onBound = { item ->
                val current = settings.adhanAlert(browsing)
                settings.setAdhanAlert(
                    browsing,
                    current.copy(
                        soundMode = AdhanSoundMode.CATALOG,
                        catalogId = item.id,
                    )
                )
                tick++
                PrayerAlarms.rescheduleAll(context)
                browsePrayer = null
            },
            modifier = modifier
        )
        return
    }

    SettingsSubScreenScaffold(
        title = stringResource(R.string.adhan_section),
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
                    title = stringResource(R.string.adhan_display_section),
                    subtitle = stringResource(R.string.adhan_display_hint)
                )
            }
            item {
                AutoReminderPresentationSelector(
                    selected = presentation,
                    onSelected = {
                        presentation = it
                        settings.adhanPresentation = it
                    }
                )
            }
            item {
                AutoReminderLockScreenSetting(
                    enabled = lockScreenEnabled,
                    onEnabledChange = {
                        lockScreenEnabled = it
                        settings.adhanAutoLockScreenEnabled = it
                    },
                    requestLockScreenAccess = requestLockScreenAccess,
                    titleRes = R.string.adhan_auto_lock_screen,
                    hintRes = R.string.adhan_auto_lock_screen_hint
                )
            }
            item {
                SettingSwitch(stringResource(R.string.adhan_vibrate), vibrateOn) {
                    vibrateOn = it
                    settings.adhanVibrate = it
                }
                Text(
                    stringResource(R.string.adhan_vibrate_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 8.dp)
                )
            }
            item {
                SectionTitle(
                    title = stringResource(R.string.adhan_section),
                    subtitle = stringResource(R.string.adhan_settings_hint)
                )
            }
            PrayerName.entries.forEach { prayer ->
                item {
                    val alert = remember(tick, prayer) { settings.adhanAlert(prayer) }
                    val suitable = remember(catalog, prayer) {
                        AdhanAudioResolver.suitableFor(catalog, prayer)
                    }
                    val soundChoice = currentAdhanSoundChoice(alert, suitable, prayer)
                    ExpandableSettingsCard(
                        title = prayerLabel(prayer),
                        subtitle = adhanSummary(alert, suitable, lang, prayer)
                    ) {
                        SettingSwitch(stringResource(R.string.adhan_for_prayer), alert.adhanEnabled) {
                            settings.setAdhanAlert(prayer, alert.copy(adhanEnabled = it))
                            tick++
                            PrayerAlarms.rescheduleAll(context)
                        }
                        PrayerEnumDropdown(
                            label = stringResource(R.string.adhan_sound),
                            value = adhanSoundChoiceLabel(soundChoice, suitable, lang, prayer),
                            options = adhanSoundChoices(suitable, lang, prayer, soundChoice)
                        ) { choice ->
                            settings.setAdhanAlert(prayer, alert.copy(
                                soundMode = choice.toMode(),
                                catalogId = when (choice) {
                                    is AdhanSoundChoice.Catalog -> choice.id
                                    AdhanSoundChoice.Default -> BundledAdhanSeed.defaultId(prayer)
                                    else -> 0L
                                },
                            ))
                            tick++
                            PrayerAlarms.rescheduleAll(context)
                        }
                        OutlinedButton(
                            onClick = { browsePrayer = prayer },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.adhan_browse_voices))
                        }
                        if (alert.customPath.isNotBlank()) {
                            Text(
                                File(alert.customPath).name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                pendingPrayer = prayer
                                picker.launch(arrayOf("audio/*"))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.adhan_pick_file))
                        }
                        Text(
                            stringResource(R.string.adhan_record_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                if (isRecording && recordingPrayer == prayer) {
                                    val path = voiceRecorder.stop()?.absolutePath
                                    isRecording = false
                                    recordingPrayer = null
                                    if (path != null) {
                                        settings.setAdhanAlert(
                                            prayer,
                                            alert.copy(
                                                soundMode = AdhanSoundMode.RECORDED,
                                                customPath = path,
                                            )
                                        )
                                        tick++
                                        PrayerAlarms.rescheduleAll(context)
                                    }
                                } else {
                                    if (isRecording) {
                                        voiceRecorder.stop()
                                        isRecording = false
                                        recordingPrayer = null
                                    }
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) {
                                        voiceRecorder.start()
                                        recordingPrayer = prayer
                                        isRecording = true
                                    } else {
                                        pendingRecordPrayer = prayer
                                        recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (isRecording && recordingPrayer == prayer) {
                                    stringResource(R.string.stop)
                                } else {
                                    stringResource(R.string.record)
                                }
                            )
                        }
                        if (isRecording && recordingPrayer == prayer) {
                            Text(
                                stringResource(R.string.recording_in_progress),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                settings.applyAdhanSoundToAll(prayer, catalog)
                                tick++
                                PrayerAlarms.rescheduleAll(context)
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.adhan_apply_compatible),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.adhan_apply_all))
                        }
                        OutlinedButton(
                            onClick = {
                                val path = AdhanAudioResolver.resolve(
                                    prayer = prayer,
                                    alert = alert,
                                    catalog = catalog,
                                    shortTone = {
                                        com.greendome.adhkar.prayer.AdhanToneGenerator.shortFile(context).absolutePath
                                    },
                                    defaultTone = {
                                        com.greendome.adhkar.prayer.AdhanToneGenerator.defaultFile(context).absolutePath
                                    },
                                    context = context,
                                )
                                if (path != null) {
                                    preview.playAdhan(path, settings, alert.overrideSilent)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.adhan_preview))
                        }
                        SettingSwitch(stringResource(R.string.adhan_after_azkar), alert.afterAdhanAzkar) {
                            settings.setAdhanAlert(prayer, alert.copy(afterAdhanAzkar = it))
                            tick++
                        }
                        SettingSwitch(stringResource(R.string.adhan_override_silent), alert.overrideSilent) {
                            settings.setAdhanAlert(prayer, alert.copy(overrideSilent = it))
                            tick++
                        }
                        PrayerMinutesSlider(
                            label = stringResource(R.string.adhan_before),
                            minutes = alert.notifyBeforeMinutes,
                            min = 0,
                            max = PrayerConfig.PRE_ADHAN_MAX,
                            onChange = {
                                settings.setAdhanAlert(prayer, alert.copy(notifyBeforeMinutes = it))
                                tick++
                                PrayerAlarms.rescheduleAll(context)
                            },
                            zeroMeansOff = true
                        )
                        PrayerMinutesSlider(
                            label = stringResource(R.string.adhan_iqama),
                            minutes = alert.iqamaMinutes,
                            min = 0,
                            max = PrayerConfig.IQAMA_MAX,
                            onChange = {
                                settings.setAdhanAlert(prayer, alert.copy(iqamaMinutes = it))
                                tick++
                                PrayerAlarms.rescheduleAll(context)
                            },
                            zeroMeansOff = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun adhanSummary(
    alert: PrayerAlertSettings,
    catalog: List<AdhanAudioEntity>,
    lang: String,
    prayer: PrayerName,
): String {
    val sound = adhanSoundChoiceLabel(currentAdhanSoundChoice(alert, catalog, prayer), catalog, lang, prayer)
    return if (alert.adhanEnabled) sound else stringResource(R.string.adhan_off)
}

private sealed class AdhanSoundChoice {
    data object Default : AdhanSoundChoice()
    data class Catalog(val id: Long) : AdhanSoundChoice()
    data object Short : AdhanSoundChoice()
    data object Silent : AdhanSoundChoice()
    data object Custom : AdhanSoundChoice()
    data object Recorded : AdhanSoundChoice()

    fun toMode(): AdhanSoundMode = when (this) {
        Default -> AdhanSoundMode.DEFAULT
        is Catalog -> AdhanSoundMode.CATALOG
        Short -> AdhanSoundMode.SHORT
        Silent -> AdhanSoundMode.SILENT
        Custom -> AdhanSoundMode.CUSTOM
        Recorded -> AdhanSoundMode.RECORDED
    }
}

private fun currentAdhanSoundChoice(
    alert: PrayerAlertSettings,
    catalog: List<AdhanAudioEntity>,
    prayer: PrayerName,
): AdhanSoundChoice = when (alert.resolvedSoundMode()) {
    AdhanSoundMode.CATALOG ->
        when {
            alert.catalogId == BundledAdhanSeed.defaultId(prayer) -> AdhanSoundChoice.Default
            catalog.any { it.id == alert.catalogId } -> AdhanSoundChoice.Catalog(alert.catalogId)
            else -> AdhanSoundChoice.Default
        }
    AdhanSoundMode.SHORT -> AdhanSoundChoice.Short
    AdhanSoundMode.SILENT -> AdhanSoundChoice.Silent
    AdhanSoundMode.CUSTOM -> AdhanSoundChoice.Custom
    AdhanSoundMode.RECORDED -> AdhanSoundChoice.Recorded
    AdhanSoundMode.DEFAULT -> AdhanSoundChoice.Default
}

@Composable
private fun adhanSoundChoices(
    catalog: List<AdhanAudioEntity>,
    lang: String,
    prayer: PrayerName,
    current: AdhanSoundChoice,
): List<Pair<AdhanSoundChoice, String>> = buildList {
    add(AdhanSoundChoice.Default to defaultAdhanLabel(catalog, lang, prayer))
    if (current is AdhanSoundChoice.Catalog) {
        val selected = catalog.find { it.id == current.id }
        if (selected != null) {
            add(current to selected.catalogLabel(lang))
        }
    }
    add(AdhanSoundChoice.Short to stringResource(R.string.adhan_sound_short))
    add(AdhanSoundChoice.Silent to stringResource(R.string.adhan_sound_silent))
    add(AdhanSoundChoice.Custom to stringResource(R.string.adhan_sound_custom))
    add(AdhanSoundChoice.Recorded to stringResource(R.string.adhan_sound_record))
}

@Composable
private fun defaultAdhanLabel(
    catalog: List<AdhanAudioEntity>,
    lang: String,
    prayer: PrayerName,
): String {
    val defaultVoice = catalog.find { it.id == BundledAdhanSeed.defaultId(prayer) }
        ?: catalog.find { it.id == BundledAdhanSeed.DEFAULT_ID }
    return if (defaultVoice != null) {
        stringResource(R.string.adhan_voice_default_label, defaultVoice.catalogLabel(lang))
    } else {
        stringResource(R.string.adhan_sound_default)
    }
}

@Composable
private fun adhanSoundChoiceLabel(
    choice: AdhanSoundChoice,
    catalog: List<AdhanAudioEntity>,
    lang: String,
    prayer: PrayerName,
): String = when (choice) {
    AdhanSoundChoice.Default -> defaultAdhanLabel(catalog, lang, prayer)
    is AdhanSoundChoice.Catalog ->
        catalog.find { it.id == choice.id }?.catalogLabel(lang)
            ?: stringResource(R.string.adhan_sound_default)
    AdhanSoundChoice.Short -> stringResource(R.string.adhan_sound_short)
    AdhanSoundChoice.Silent -> stringResource(R.string.adhan_sound_silent)
    AdhanSoundChoice.Custom -> stringResource(R.string.adhan_sound_custom)
    AdhanSoundChoice.Recorded -> stringResource(R.string.adhan_sound_record)
}

@Composable
fun QiblaSettingsScreen(
    settings: SettingsRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val location = settings.prayerConfig().location
    SettingsSubScreenScaffold(
        title = stringResource(R.string.qibla_title),
        onBack = onBack,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (location == null) {
                Text(
                    stringResource(R.string.prayer_times_need_city),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Text(location.cityName, style = MaterialTheme.typography.titleMedium)
                QiblaCompass(latitude = location.latitude, longitude = location.longitude)
                Text(
                    stringResource(R.string.qibla_hold_flat),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
