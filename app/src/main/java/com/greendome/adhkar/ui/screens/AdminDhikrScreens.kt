package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.greendome.adhkar.data.local.DhikrEntity
import com.greendome.adhkar.data.model.DhikrCategory
import com.greendome.adhkar.data.model.ScheduleType
import com.greendome.adhkar.ui.theme.GoldDome
import com.greendome.adhkar.ui.theme.GreenPrimary
import com.greendome.adhkar.ui.theme.AppCardColors
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
    val shortTasbih = builtIn.filter { it.isBuiltinShortTasbih() }
    val jawami = builtIn.filter { it.isJawamiSectionItem() }
    val otherBuiltIn = builtIn.filter { it.isEidTakbir() }
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
    onEditDhikr: (DhikrEntity) -> Unit,
    onDeleteDhikr: ((DhikrEntity) -> Unit)? = null
) {
    val filtered = adminDhikrItems(items, bucket)
    var pendingDelete by remember { mutableStateOf<DhikrEntity?>(null) }

    pendingDelete?.let { dhikr ->
        val deleteTitle = when (bucket) {
            AdminDhikrBucket.JAWAMI -> R.string.admin_delete_jawami
            else -> R.string.admin_delete_short_tasbih
        }
        val deleteConfirm = when (bucket) {
            AdminDhikrBucket.JAWAMI -> R.string.admin_delete_jawami_confirm
            else -> R.string.admin_delete_short_tasbih_confirm
        }
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(deleteTitle)) },
            text = { Text(stringResource(deleteConfirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteDhikr?.invoke(dhikr)
                        pendingDelete = null
                    }
                ) {
                    Text(stringResource(deleteTitle))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                filtered.forEach { dhikr ->
                    AdminDhikrCard(
                        dhikr = dhikr,
                        onEditDhikr = onEditDhikr,
                        onDeleteDhikr = if (onDeleteDhikr != null) {
                            { pendingDelete = dhikr }
                        } else {
                            null
                        },
                        deleteLabel = when (bucket) {
                            AdminDhikrBucket.JAWAMI -> R.string.admin_delete_jawami
                            else -> R.string.admin_delete_short_tasbih
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminDhikrCard(
    dhikr: DhikrEntity,
    onEditDhikr: (DhikrEntity) -> Unit,
    onDeleteDhikr: (() -> Unit)? = null,
    deleteLabel: Int = R.string.admin_delete_short_tasbih,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = AppCardColors()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .clickable { onEditDhikr(dhikr) }
                    .padding(12.dp)
            ) {
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
            if (onDeleteDhikr != null) {
                IconButton(onClick = onDeleteDhikr) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(deleteLabel)
                    )
                }
            }
        }
    }
}
