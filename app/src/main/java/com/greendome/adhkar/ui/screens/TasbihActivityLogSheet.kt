package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.ui.theme.AppAccentGreen
import com.greendome.adhkar.ui.theme.CreamBackground
import com.greendome.adhkar.ui.theme.GreenLight
import com.greendome.adhkar.ui.theme.GreenPrimary
import com.greendome.adhkar.ui.theme.GreenPrimaryDark
import com.greendome.adhkar.ui.theme.stringResourceDigits
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val PERIOD_OPTIONS = listOf(1, 3, 6)
private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihActivityLogSheet(
    activityData: Map<String, Int>,
    onDismiss: () -> Unit,
    onPeriodChange: (Int) -> Unit,
) {
    var selectedMonths by remember { mutableIntStateOf(3) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.activity_log_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppAccentGreen(),
                )
                FilterChip(
                    selected = true,
                    onClick = {},
                    enabled = false,
                    label = {
                        Text(
                            stringResourceDigits(R.string.activity_log_period_months, selectedMonths),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PERIOD_OPTIONS.forEach { months ->
                    FilterChip(
                        selected = selectedMonths == months,
                        onClick = {
                            selectedMonths = months
                            onPeriodChange(months)
                        },
                        label = {
                            Text(stringResourceDigits(R.string.activity_log_period_months, months))
                        },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            val todayTotal = activityData[LocalDate.now().format(DATE_FORMAT)] ?: 0
            Text(
                text = stringResourceDigits(R.string.activity_log_today_total, todayTotal),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))

            TasbihActivityHeatmap(
                activityData = activityData,
                monthsBack = selectedMonths,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            ActivityLegend()
        }
    }
}

@Composable
private fun TasbihActivityHeatmap(
    activityData: Map<String, Int>,
    monthsBack: Int,
    modifier: Modifier = Modifier,
) {
    val end = LocalDate.now()
    val start = end.minusMonths(monthsBack.toLong())
    val gridStart = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    val maxCount = activityData.values.maxOrNull()?.coerceAtLeast(1) ?: 1

    val weeks = mutableListOf<List<LocalDate?>>()
    var weekStart = gridStart
    while (!weekStart.isAfter(end)) {
        val week = (0 until 7).map { offset ->
            val day = weekStart.plusDays(offset.toLong())
            if (day.isBefore(start) || day.isAfter(end)) null else day
        }
        weeks += week
        weekStart = weekStart.plusWeeks(1)
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        weeks.forEach { week ->
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                week.forEach { day ->
                    if (day == null) {
                        Spacer(Modifier.size(14.dp))
                    } else {
                        val count = activityData[day.format(DATE_FORMAT)] ?: 0
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(heatmapColor(count, maxCount)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.activity_log_less),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(6.dp))
        val levels = listOf(0, 1, 3, 6, 10)
        val max = 10
        levels.forEach { level ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 1.dp)
                    .size(12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(heatmapColor(level, max)),
            )
        }
        Spacer(Modifier.size(6.dp))
        Text(
            text = stringResource(R.string.activity_log_more),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun heatmapColor(count: Int, maxCount: Int): Color {
    if (count <= 0) return Color(0xFFE8E4DC)
    val ratio = count.toFloat() / maxCount.coerceAtLeast(1)
    return when {
        ratio <= 0.25f -> GreenLight.copy(alpha = 0.35f)
        ratio <= 0.5f -> GreenLight.copy(alpha = 0.6f)
        ratio <= 0.75f -> GreenPrimary.copy(alpha = 0.75f)
        else -> GreenPrimaryDark
    }
}
