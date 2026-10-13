package com.example.sleepready.ui.schedule.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 多提醒节点编辑器。每条提醒以「提前 X 分钟」存储。预设包括：
 *   准点 / 5 分钟 / 30 分钟 / 1 小时 / 3 小时 / 1 天 / 3 天 / 7 天。
 */
@Composable
fun ReminderListEditor(
    offsetsMinutes: List<Long>,
    onChange: (List<Long>) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPresets by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("提醒", style = MaterialTheme.typography.titleSmall)
            if (offsetsMinutes.isEmpty()) {
                Text(
                    text = "未设置提醒",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                offsetsMinutes.sorted().forEach { offset ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = describeOffset(offset),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            IconButton(onClick = {
                                onChange(offsetsMinutes.toMutableList().also { it.remove(offset) })
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "移除")
                            }
                        }
                    }
                }
            }
            Row {
                TextButton(onClick = { showPresets = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("  添加新提醒")
                }
                DropdownMenu(
                    expanded = showPresets,
                    onDismissRequest = { showPresets = false }
                ) {
                    PRESETS.forEach { (minutes, label) ->
                        val disabled = offsetsMinutes.contains(minutes)
                        DropdownMenuItem(
                            text = { Text(label) },
                            enabled = !disabled,
                            onClick = {
                                onChange((offsetsMinutes + minutes).distinct().sorted())
                                showPresets = false
                            }
                        )
                    }
                }
            }
        }
    }
}

private val PRESETS: List<Pair<Long, String>> = listOf(
    0L to "准点",
    5L to "提前 5 分钟",
    30L to "提前 30 分钟",
    60L to "提前 1 小时",
    180L to "提前 3 小时",
    24L * 60 to "提前 1 天",
    3L * 24 * 60 to "提前 3 天",
    7L * 24 * 60 to "提前 7 天"
)

private fun describeOffset(minutes: Long): String = when {
    minutes <= 0L -> "准点"
    minutes < 60L -> "提前 $minutes 分钟"
    minutes < 24L * 60 -> {
        val h = minutes / 60
        val m = minutes % 60
        if (m == 0L) "提前 $h 小时" else "提前 $h 小时 $m 分钟"
    }
    else -> {
        val d = minutes / (24L * 60)
        val rem = minutes % (24L * 60)
        if (rem == 0L) "提前 $d 天" else "提前 $d 天 ${rem / 60} 小时"
    }
}
