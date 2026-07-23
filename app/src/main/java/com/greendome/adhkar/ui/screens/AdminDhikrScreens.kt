package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.local.DhikrEntity
import com.greendome.adhkar.data.model.DhikrCategory
import com.greendome.adhkar.data.model.ScheduleType
import com.greendome.adhkar.ui.theme.GoldDome
import com.greendome.adhkar.ui.theme.GreenPrimary
import com.greendome.adhkar.ui.theme.GreenPrimaryDark
import com.greendome.adhkar.ui.theme.stringResourceDigits
import com.greendome.adhkar.ui.theme.formatLocalizedDigits
import com.greendome.adhkar.util.DhikrScheduleMatcher

enum class AdminDhikrBucket {
    SHORT_TASBIH,
    JAWAMI,
    OTHER_BUILTIN
}

fun adminDhikrItems(list: List<DhikrEntity>, bucket: AdminDhikrBucket): List<DhikrEntity> {
    val builtIn = list.filter { it.isDefault }
    val shortTasbih = builtIn.filter { !it.isLongForm && it.category != DhikrCategory.JAWAMI }
    val jawami = builtIn.filter { it.category == DhikrCategory.JAWAMI }
    val otherBuiltIn = builtIn.filter { it !in shortTasbih && it !in jawami }
    return when (bucket) {
        AdminDhikrBucket.SHORT_TASBIH -> shortTasbih
        AdminDhikrBucket.JAWAMI -> jawami
        AdminDhikrBucket.OTHER_BUILTIN -> otherBuiltIn
    }.sortedBy { it.sortOrder }
}

@Composable
fun adminDhikrBucketTitle(bucket: AdminDhikrBucket): Int = when (bucket) {
    AdminDhikrBucket.SHORT_TASBIH -> R.string.builtin_short_tasbih
    AdminDhikrBucket.JAWAMI -> R.string.builtin_jawami
    AdminDhikrBucket.OTHER_BUILTIN -> R.string.admin_manage_other_builtin
}

@Composable
fun adminDhikrBucketHint(bucket: AdminDhikrBucket): Int = when (bucket) {
    AdminDhikrBucket.SHORT_TASBIH -> R.string.listen_section_hint
    AdminDhikrBucket.JAWAMI -> R.string.builtin_jawami_hint
    AdminDhikrBucket.OTHER_BUILTIN -> R.string.admin_manage_other_builtin_hint
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDhikrBrowseScreen(
    bucket: AdminDhikrBucket,
    items: List<DhikrEntity>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEditDhikr: (DhikrEntity) -> Unit
) {
    val filtered = adminDhikrItems(items, bucket)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(adminDhikrBucketTitle(bucket))) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onAdd) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(adminDhikrBucketHint(bucket)),
                style = MaterialTheme.typography.bodyMedium,
                color = GreenPrimary
            )
            Text(
                stringResource(R.string.admin_tap_to_edit),
                style = MaterialTheme.typography.labelMedium
            )
            if (filtered.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GreenPrimary.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.admin_dhikr_section_empty),
                        modifier = Modifier.padding(16.dp),
                        color = GreenPrimaryDark
                    )
                }
            } else {
                filtered.forEach { dhikr ->
                    AdminDhikrCard(dhikr, onEditDhikr)
                }
            }
        }
    }
}

@Composable
fun AdminDhikrCard(dhikr: DhikrEntity, onEditDhikr: (DhikrEntity) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditDhikr(dhikr) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(dhikr.textAr, maxLines = 2)
            if (dhikr.category == DhikrCategory.JAWAMI && dhikr.repeatCount > 1) {
                Text(
                    stringResourceDigits(R.string.azkar_repeat_label, dhikr.repeatCount),
                    color = GoldDome,
                    fontWeight = FontWeight.Medium
                )
            }
            if (dhikr.scheduleType != ScheduleType.ALWAYS) {
                Text(
                    DhikrScheduleMatcher.scheduleSummaryAr(dhikr).formatLocalizedDigits(),
                    color = GoldDome,
                    fontWeight = FontWeight.Medium
                )
                val active = DhikrScheduleMatcher.isActiveNow(dhikr)
                Text(
                    if (active) stringResource(R.string.schedule_active_now) else stringResource(R.string.schedule_inactive_now),
                    color = if (active) GreenPrimary else androidx.compose.ui.graphics.Color.Gray
                )
            } else if (dhikr.category != DhikrCategory.JAWAMI) {
                Text(stringResource(R.string.schedule_always), color = androidx.compose.ui.graphics.Color.Gray)
            }
        }
    }
}
