package com.greendome.adhkar.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import com.greendome.adhkar.R
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.prayer.AsrMadhabPref
import com.greendome.adhkar.prayer.CalculationMethodPref
import com.greendome.adhkar.prayer.CountryPrayerOverride
import com.greendome.adhkar.prayer.DstMode
import com.greendome.adhkar.prayer.PrayerCountryDefaults
import com.greendome.adhkar.sync.PrayerDefaultsPublisher
import com.greendome.adhkar.sync.PrayerDefaultsSync
import com.greendome.adhkar.ui.theme.AppCardColors
import com.greendome.adhkar.ui.theme.GoldDome
import com.greendome.adhkar.ui.theme.GreenPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPrayerDefaultsScreen(
    settings: SettingsRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tick by remember { mutableIntStateOf(0) }
    var editingCode by remember { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var publishInProgress by remember { mutableStateOf(false) }
    var publishStatus by remember { mutableStateOf<String?>(null) }
    var publishError by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (!settings.prayerDefaultsDirty) {
            PrayerDefaultsSync.syncIfNeeded(context, settings)
            tick++
        }
    }

    val rows = remember(tick) { PrayerCountryDefaults.snapshot() }
    val filteredRows = remember(rows, searchQuery) {
        rows.filter { (code, entry) -> countryMatchesQuery(code, entry.timezone, searchQuery) }
    }
    BackHandler(enabled = editingCode != null) { editingCode = null }
    val editing = editingCode?.let { code ->
        rows.firstOrNull { it.first == code } ?: (code to CountryPrayerOverride(
            method = PrayerCountryDefaults.methodFor(code),
            madhab = PrayerCountryDefaults.madhabFor(code),
            timezone = PrayerCountryDefaults.timezoneIdFor(code),
            dst = PrayerCountryDefaults.dstFor(code)
        ))
    }

    fun persistLocal() {
        settings.prayerDefaultsJson = PrayerCountryDefaults.persistJson(settings.prayerDefaultsVersion)
        settings.prayerDefaultsDirty = true
        tick++
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_prayer_defaults)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (editingCode != null) editingCode = null else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (editingCode == null) {
                        IconButton(onClick = { showAdd = true }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = stringResource(R.string.admin_prayer_defaults_add)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (editing != null) {
            CountryDefaultsEditor(
                code = editing.first,
                initial = editing.second,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                onSave = { entry ->
                    PrayerCountryDefaults.upsert(editing.first, entry)
                    persistLocal()
                    editingCode = null
                },
                onCancel = { editingCode = null }
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                stringResource(R.string.admin_prayer_defaults_hint),
                style = MaterialTheme.typography.bodySmall,
                color = GoldDome,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
            if (settings.prayerDefaultsDirty) {
                Text(
                    stringResource(R.string.admin_prayer_defaults_unpublished),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Button(
                onClick = {
                    publishInProgress = true
                    publishError = false
                    publishStatus = null
                    scope.launch {
                        val result = PrayerDefaultsPublisher.publish(settings) { publishStatus = it }
                        publishInProgress = false
                        when (result) {
                            is PrayerDefaultsPublisher.PublishResult.Success -> {
                                publishError = false
                                publishStatus = context.getString(
                                    R.string.admin_prayer_defaults_publish_success,
                                    result.version
                                )
                                tick++
                            }
                            is PrayerDefaultsPublisher.PublishResult.Failed -> {
                                publishError = true
                                publishStatus = result.reason
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !publishInProgress
            ) {
                if (publishInProgress) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    if (publishInProgress) stringResource(R.string.admin_publish_in_progress)
                    else stringResource(R.string.admin_prayer_defaults_publish)
                )
            }
            publishStatus?.let { status ->
                Text(
                    status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (publishError) MaterialTheme.colorScheme.error else GreenPrimary,
                    modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
                )
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp),
                placeholder = { Text(stringResource(R.string.admin_prayer_defaults_search)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.azkar_search_clear)
                            )
                        }
                    }
                },
                singleLine = true
            )
            if (filteredRows.isEmpty()) {
                Text(
                    stringResource(R.string.admin_prayer_defaults_search_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredRows, key = { it.first }) { (code, entry) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingCode = code },
                        shape = RoundedCornerShape(12.dp),
                        colors = AppCardColors()
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(PrayerCountryDefaults.countryLabel(code), fontWeight = FontWeight.Bold)
                            Text(
                                "${methodLabelText(entry.method)} · ${madhabLabelText(entry.madhab)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = GreenPrimary
                            )
                            Text(
                                "${entry.timezone} · ${dstLabelText(entry.dst)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var newCode by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(stringResource(R.string.admin_prayer_defaults_add)) },
            text = {
                OutlinedTextField(
                    value = newCode,
                    onValueChange = { newCode = it.filter { ch -> ch.isLetter() }.take(2).uppercase() },
                    label = { Text(stringResource(R.string.admin_prayer_country_code)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val code = newCode.trim().uppercase()
                        if (code.length == 2) {
                            editingCode = code
                            showAdd = false
                        }
                    },
                    enabled = newCode.trim().length == 2
                ) { Text(stringResource(R.string.admin_prayer_defaults_add)) }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun CountryDefaultsEditor(
    code: String,
    initial: CountryPrayerOverride,
    modifier: Modifier = Modifier,
    onSave: (CountryPrayerOverride) -> Unit,
    onCancel: () -> Unit
) {
    var method by remember(code) { mutableStateOf(initial.method) }
    var madhab by remember(code) { mutableStateOf(initial.madhab) }
    var timezone by remember(code) { mutableStateOf(initial.timezone) }
    var dst by remember(code) { mutableStateOf(initial.dst) }
    val timezones = remember(timezone) {
        (PrayerCountryDefaults.commonTimezones + timezone).distinct()
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(PrayerCountryDefaults.countryLabel(code), fontWeight = FontWeight.Bold)
        PrefDropdown(
            label = stringResource(R.string.prayer_method),
            value = methodLabelText(method),
            options = CalculationMethodPref.entries
                .filter { it != CalculationMethodPref.AUTO }
                .map { it to methodLabelText(it) }
        ) { method = it }
        PrefDropdown(
            label = stringResource(R.string.prayer_madhab),
            value = madhabLabelText(madhab),
            options = listOf(AsrMadhabPref.SHAFI, AsrMadhabPref.HANAFI)
                .map { it to madhabLabelText(it) }
        ) { madhab = it }
        PrefDropdown(
            label = stringResource(R.string.prayer_timezone),
            value = timezone,
            options = timezones.map { it to it }
        ) { timezone = it }
        PrefDropdown(
            label = stringResource(R.string.prayer_dst_mode),
            value = dstLabelText(dst),
            options = DstMode.entries.map { it to dstLabelText(it) }
        ) { dst = it }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.cancel))
            }
            Button(
                onClick = {
                    onSave(
                        CountryPrayerOverride(
                            method = method,
                            madhab = madhab,
                            timezone = timezone,
                            dst = dst
                        )
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> PrefDropdown(
    label: String,
    value: String,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (item, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun methodLabelText(method: CalculationMethodPref): String = stringResource(
    when (method) {
        CalculationMethodPref.AUTO -> R.string.prayer_method_auto
        CalculationMethodPref.MUSLIM_WORLD_LEAGUE -> R.string.prayer_method_mwl
        CalculationMethodPref.EGYPTIAN -> R.string.prayer_method_egyptian
        CalculationMethodPref.KARACHI -> R.string.prayer_method_karachi
        CalculationMethodPref.UMM_AL_QURA -> R.string.prayer_method_umm_al_qura
        CalculationMethodPref.DUBAI -> R.string.prayer_method_dubai
        CalculationMethodPref.MOON_SIGHTING_COMMITTEE -> R.string.prayer_method_moonsighting
        CalculationMethodPref.NORTH_AMERICA -> R.string.prayer_method_isna
        CalculationMethodPref.KUWAIT -> R.string.prayer_method_kuwait
        CalculationMethodPref.QATAR -> R.string.prayer_method_qatar
        CalculationMethodPref.SINGAPORE -> R.string.prayer_method_singapore
    }
)

@Composable
private fun madhabLabelText(madhab: AsrMadhabPref): String = stringResource(
    when (madhab) {
        AsrMadhabPref.AUTO -> R.string.prayer_madhab_auto
        AsrMadhabPref.SHAFI -> R.string.prayer_madhab_shafi
        AsrMadhabPref.HANAFI -> R.string.prayer_madhab_hanafi
    }
)

@Composable
private fun dstLabelText(mode: DstMode): String = stringResource(
    when (mode) {
        DstMode.AUTO -> R.string.prayer_dst_auto
        DstMode.ON -> R.string.prayer_dst_on
        DstMode.OFF -> R.string.prayer_dst_off
    }
)

private fun countryMatchesQuery(code: String, timezone: String, query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    val ar = PrayerCountryDefaults.countryLabel(code)
    val en = Locale("", code).getDisplayCountry(Locale.US)
    return code.contains(q, ignoreCase = true) ||
        ar.contains(q, ignoreCase = true) ||
        en.contains(q, ignoreCase = true) ||
        timezone.contains(q, ignoreCase = true)
}
