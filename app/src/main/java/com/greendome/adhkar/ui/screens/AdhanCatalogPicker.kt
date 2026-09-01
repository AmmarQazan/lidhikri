package com.greendome.adhkar.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.audio.DhikrAudioPlayer
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AdhanAudioEntity
import com.greendome.adhkar.prayer.AdhanAudioResolver
import com.greendome.adhkar.prayer.PrayerName
import com.greendome.adhkar.service.OfflineDownloadHelper
import com.greendome.adhkar.ui.theme.AppCardColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdhanCatalogPickerScreen(
    prayer: PrayerName,
    catalog: List<AdhanAudioEntity>,
    lang: String,
    onBack: () -> Unit,
    onBound: (AdhanAudioEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val preview = remember { DhikrAudioPlayer(context) }
    val settings = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var maqam by remember { mutableStateOf("") }
    var downloadingId by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val failText = stringResource(R.string.adhan_download_failed)
    val unknownMuezzin = stringResource(R.string.adhan_unknown_muezzin)

    DisposableEffect(Unit) {
        onDispose { preview.stop() }
    }

    val suitable = remember(catalog, prayer) {
        AdhanAudioResolver.suitableFor(catalog, prayer)
    }
    val countries = remember(suitable, lang) {
        suitable.map { it.localizedCountry(lang) }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val cities = remember(suitable, lang, country) {
        suitable
            .filter { country.isBlank() || it.localizedCountry(lang) == country }
            .map { it.localizedCity(lang) }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
    val maqamat = remember(suitable, lang, country, city) {
        suitable
            .filter { country.isBlank() || it.localizedCountry(lang) == country }
            .filter { city.isBlank() || it.localizedCity(lang) == city }
            .map { it.localizedMaqam(lang) }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
    val filtered = remember(suitable, query, country, city, maqam, lang, unknownMuezzin) {
        val needle = query.trim()
        suitable.filter { item ->
            val matchesCountry = country.isBlank() || item.localizedCountry(lang) == country
            val matchesCity = city.isBlank() || item.localizedCity(lang) == city
            val matchesMaqam = maqam.isBlank() || item.localizedMaqam(lang) == maqam
            val haystack = listOf(
                item.displayMuezzin(lang).ifBlank { unknownMuezzin },
                item.localizedCountry(lang),
                item.localizedCity(lang),
                item.localizedMaqam(lang),
                item.localizedName(lang),
            ).joinToString(" ")
            matchesCountry && matchesCity && matchesMaqam &&
                (needle.isBlank() || haystack.contains(needle, ignoreCase = true))
        }
    }

    SettingsSubScreenScaffold(
        title = stringResource(R.string.adhan_browse_voices),
        onBack = onBack,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.adhan_search_muezzin)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            item {
                PrayerEnumDropdown(
                    label = stringResource(R.string.adhan_filter_country),
                    value = country.ifBlank { stringResource(R.string.adhan_all_countries) },
                    options = listOf("" to stringResource(R.string.adhan_all_countries)) +
                        countries.map { it to it }
                ) {
                    country = it
                    city = ""
                    maqam = ""
                }
            }
            if (cities.isNotEmpty()) {
                item {
                    PrayerEnumDropdown(
                        label = stringResource(R.string.adhan_filter_city),
                        value = city.ifBlank { stringResource(R.string.adhan_all_cities) },
                        options = listOf("" to stringResource(R.string.adhan_all_cities)) +
                            cities.map { it to it }
                    ) { city = it; maqam = "" }
                }
            }
            item {
                PrayerEnumDropdown(
                    label = stringResource(R.string.adhan_filter_maqam),
                    value = maqam.ifBlank { stringResource(R.string.adhan_all_maqamat) },
                    options = listOf("" to stringResource(R.string.adhan_all_maqamat)) +
                        maqamat.map { it to it }
                ) { maqam = it }
            }
            error?.let {
                item {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
            items(filtered, key = { it.id }) { item ->
                val ready = AdhanAudioResolver.hasLocalPlayback(item, context)
                val canDownload = !ready && !item.remoteUrl.isNullOrBlank()
                val busy = downloadingId == item.id
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = AppCardColors()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            item.displayMuezzin(lang).ifBlank { unknownMuezzin },
                            fontWeight = FontWeight.Medium
                        )
                        val place = listOf(item.localizedCity(lang), item.localizedCountry(lang))
                            .filter { it.isNotBlank() }
                            .distinct()
                            .joinToString(" — ")
                        if (place.isNotBlank()) {
                            Text(
                                place,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val maqamLabel = item.localizedMaqam(lang)
                        if (maqamLabel.isNotBlank()) {
                            Text(
                                maqamLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            stringResource(
                                if (ready && item.isBundled()) R.string.admin_adhan_bundled
                                else if (ready) R.string.admin_adhan_on_firebase
                                else R.string.adhan_needs_download
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (busy) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(stringResource(R.string.adhan_downloading))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        error = null
                                        val path = prepareAdhanPreview(context, item) { downloadingId = it }
                                        if (path == null) {
                                            error = failText
                                            return@launch
                                        }
                                        preview.playAdhan(path, settings, overrideSilent = true)
                                    }
                                },
                                enabled = !busy && (
                                    ready || !item.remoteUrl.isNullOrBlank() ||
                                        AdhanAudioResolver.playbackUri(item, context) != null
                                    ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.adhan_preview))
                            }
                            Button(
                                onClick = {
                                    if (ready) {
                                        onBound(item)
                                        return@Button
                                    }
                                    val url = item.remoteUrl
                                    if (url.isNullOrBlank()) return@Button
                                    downloadingId = item.id
                                    error = null
                                    scope.launch {
                                        val path = OfflineDownloadHelper.downloadAdhan(context, item.id, url)
                                        downloadingId = null
                                        if (path == null) {
                                            error = failText
                                        } else {
                                            onBound(item.copy(localPath = path, isDownloaded = true))
                                        }
                                    }
                                },
                                enabled = !busy && (ready || canDownload),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    stringResource(
                                        if (ready) R.string.adhan_bind else R.string.adhan_download_to_use
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun prepareAdhanPreview(
    context: Context,
    item: AdhanAudioEntity,
    onDownloading: (Long?) -> Unit,
): String? {
    val uri = AdhanAudioResolver.playbackUri(item, context)
    if (uri != null && !AdhanAudioResolver.isRemoteUrl(uri)) return uri
    val url = item.remoteUrl?.takeIf { it.isNotBlank() }
        ?: uri?.takeIf { AdhanAudioResolver.isRemoteUrl(it) }
        ?: return null
    onDownloading(item.id)
    return try {
        OfflineDownloadHelper.downloadAdhan(context, item.id, url)
    } finally {
        onDownloading(null)
    }
}
