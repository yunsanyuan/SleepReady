package com.example.sleepready.ui.history.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sleepready.domain.model.ExecutionEntry
import com.example.sleepready.domain.model.ExecutionHistory
import com.example.sleepready.domain.model.ExecutionResult
import com.example.sleepready.domain.model.SwitchAction
import com.example.sleepready.domain.model.SwitchItem
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpandableHistoryCard(
    record: ExecutionHistory,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val successCount = record.entries.count { it.result == ExecutionResult.SUCCESS }
    val failureCount = record.entries.count {
        it.result == ExecutionResult.FAILED || it.result == ExecutionResult.PERMISSION_DENIED
    }
    val pendingCount = record.entries.count { it.result == ExecutionResult.REQUIRES_USER_ACTION }
    val formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .combinedClickable(
                onClick = { expanded = !expanded },
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 2.dp
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatter.format(Date(record.startedAt)),
                    style = MaterialTheme.typography.titleMedium
                )
                Text("${record.durationMinutes} 分钟", style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = "成功 $successCount · 失败 $failureCount · 待手动 $pendingCount",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    record.entries.forEach { EntryRow(it) }
                }
            }
        }
    }
}

@Composable
private fun EntryRow(entry: ExecutionEntry) {
    val resultText = when (entry.result) {
        ExecutionResult.SUCCESS -> "成功"
        ExecutionResult.SKIPPED -> "跳过"
        ExecutionResult.PERMISSION_DENIED -> "权限被拒"
        ExecutionResult.REQUIRES_USER_ACTION -> "请手动操作"
        ExecutionResult.DEFERRED -> "等待解锁"
        ExecutionResult.FAILED -> "失败"
    }
    val actionText = when (entry.action) {
        SwitchAction.TURN_ON -> "开启"
        SwitchAction.TURN_OFF -> "关闭"
        SwitchAction.NONE -> "未执行"
    }
    Column {
        Text(
            text = "${labelFor(entry.switchItem)} · $actionText · $resultText",
            style = MaterialTheme.typography.bodyMedium
        )
        if (!entry.message.isNullOrBlank()) {
            Text(
                text = entry.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun labelFor(item: SwitchItem): String = when (item) {
    SwitchItem.DO_NOT_DISTURB -> "勿扰模式"
    SwitchItem.MUTE -> "静音模式"
    SwitchItem.VIBRATE -> "振动模式"
    SwitchItem.AIRPLANE_MODE -> "飞行模式"
    SwitchItem.WIFI -> "Wi-Fi"
    SwitchItem.BLUETOOTH -> "蓝牙"
    SwitchItem.MOBILE_DATA -> "移动数据"
    SwitchItem.LOCATION -> "定位"
    SwitchItem.FLASHLIGHT -> "手电筒"
    SwitchItem.SCREEN_RECORDING -> "屏幕录制"
    SwitchItem.NFC -> "NFC"
    SwitchItem.POWER_SAVE -> "省电模式"
    SwitchItem.EYE_COMFORT -> "护眼模式"
}
