package com.greendome.adhkar.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.ui.theme.AppCardColors
import com.greendome.adhkar.ui.theme.AppCardElevation
import com.greendome.adhkar.ui.theme.AppCardOutline
import com.greendome.adhkar.ui.theme.AppCardShape

@Composable
fun AutoAzkarScheduleExplainer(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        colors = AppCardColors(),
        elevation = AppCardElevation(),
        border = AppCardOutline(),
        shape = AppCardShape(),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.auto_azkar_schedule_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                SettingsCardExpandHint(expanded = expanded)
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.auto_azkar_schedule_one_dhikr),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "• ${stringResource(R.string.auto_azkar_mode_random_detail)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "• ${stringResource(R.string.auto_azkar_mode_sequential_detail)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

