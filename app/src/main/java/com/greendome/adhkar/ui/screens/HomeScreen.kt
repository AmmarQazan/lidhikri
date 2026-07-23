package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.util.CollectionScheduleHelper
import java.util.Calendar
import com.greendome.adhkar.ui.theme.formatLocalizedDigits
import com.greendome.adhkar.ui.theme.GreenPrimary
import com.greendome.adhkar.ui.theme.GreenPrimaryDark
import com.greendome.adhkar.ui.theme.stringResourceDigits

@Composable
fun HomeScreen(
    isServiceOn: Boolean,
    todayTasbihCount: Int,
    todayAzkarCount: Int,
    todayMisbahaCount: Int,
    minutesUntilNext: Int,
    autoAzkarEnabled: Boolean,
    azkarCollections: List<AdhkarCollectionEntity>,
    appLang: String,
    onToggleService: () -> Unit,
    onToggleAutoAzkar: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onAdminSecretTap: (() -> Unit)? = null
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { DomeHeader(onLongPress = onAdminSecretTap) }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(18.dp)) {
                        SectionTitle(
                            title = stringResource(R.string.auto_tasbih_section),
                            subtitle = stringResource(R.string.auto_tasbih_hint)
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    if (isServiceOn) stringResource(R.string.auto_tasbih_on)
                                    else stringResource(R.string.auto_tasbih_off),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isServiceOn) GreenPrimary
                                    else androidx.compose.ui.graphics.Color.Gray
                                )
                                if (isServiceOn) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        stringResourceDigits(R.string.next_reminder, minutesUntilNext),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Switch(checked = isServiceOn, onCheckedChange = { onToggleService() })
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(18.dp)) {
                        SectionTitle(
                            title = stringResource(R.string.auto_azkar_section),
                            subtitle = stringResource(R.string.auto_azkar_hint)
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (autoAzkarEnabled) stringResource(R.string.auto_azkar_on)
                                else stringResource(R.string.auto_azkar_off),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (autoAzkarEnabled) GreenPrimary
                                else androidx.compose.ui.graphics.Color.Gray,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            Switch(checked = autoAzkarEnabled, onCheckedChange = onToggleAutoAzkar)
                        }
                        if (autoAzkarEnabled) {
                            Spacer(Modifier.height(8.dp))
                            NextAzkarScheduleLine(
                                collections = azkarCollections,
                                appLang = appLang
                            )
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = androidx.compose.ui.graphics.Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            TodayStatLine(
                                label = stringResource(R.string.today_stats_tasbih_title),
                                count = todayTasbihCount,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.padding(horizontal = 6.dp))
                            TodayStatLine(
                                label = stringResource(R.string.today_stats_azkar_title),
                                count = todayAzkarCount,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.padding(horizontal = 6.dp))
                            TodayStatLine(
                                label = stringResource(R.string.today_stats_misbaha_title),
                                count = todayMisbahaCount,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            stringResource(R.string.today_stats_hint),
                            color = androidx.compose.ui.graphics.Color.Gray,
                            modifier = Modifier.padding(top = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 0.95f
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayStatLine(
    label: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = GreenPrimaryDark,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            count.formatLocalizedDigits(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = GreenPrimary,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        )
    }
}

@Composable
private fun NextAzkarScheduleLine(
    collections: List<AdhkarCollectionEntity>,
    appLang: String
) {
    val next = remember(collections) { CollectionScheduleHelper.findNextEnabled(collections) }
    val text = if (next == null) {
        stringResource(R.string.next_azkar_none)
    } else {
        val (collection, triggerAt) = next
        val title = if (appLang == "ar") collection.titleAr
        else collection.titleEn.ifBlank { collection.titleAr }
        val trigger = Calendar.getInstance().apply { timeInMillis = triggerAt }
        val now = Calendar.getInstance()
        val tomorrow = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val timeLabel = when {
            trigger.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                trigger.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) -> {
                stringResourceDigits(
                    R.string.next_azkar_time_today,
                    trigger.get(Calendar.HOUR_OF_DAY),
                    trigger.get(Calendar.MINUTE)
                )
            }
            trigger.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) &&
                trigger.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR) -> {
                stringResourceDigits(
                    R.string.next_azkar_time_tomorrow,
                    trigger.get(Calendar.HOUR_OF_DAY),
                    trigger.get(Calendar.MINUTE)
                )
            }
            else -> {
                stringResourceDigits(
                    R.string.next_azkar_time_later,
                    trigger.get(Calendar.DAY_OF_MONTH),
                    trigger.get(Calendar.MONTH) + 1,
                    trigger.get(Calendar.HOUR_OF_DAY),
                    trigger.get(Calendar.MINUTE)
                )
            }
        }
        stringResource(R.string.next_azkar_summary, title, timeLabel)
    }
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = GreenPrimaryDark,
        fontWeight = FontWeight.Medium
    )
}
