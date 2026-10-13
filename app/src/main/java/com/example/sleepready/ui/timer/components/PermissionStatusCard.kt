package com.example.sleepready.ui.timer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sleepready.domain.model.ActionPlan
import com.example.sleepready.domain.model.SwitchAction
import com.example.sleepready.domain.model.SwitchItem
import com.example.sleepready.permission.PermissionState

@Composable
fun PermissionStatusCard(
    plan: ActionPlan,
    states: Map<SwitchItem, PermissionState>,
    onRequest: (SwitchItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = SwitchItem.executionOrder.filter { plan.actionFor(it) != SwitchAction.NONE }
    if (rows.isEmpty()) return
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "权限状态",
                style = MaterialTheme.typography.titleMedium
            )
            rows.forEach { item ->
                val state = states[item] ?: PermissionState.NotRequired
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = labelFor(item),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    when (state) {
                        PermissionState.Granted, PermissionState.NotRequired ->
                            Text("✓", color = MaterialTheme.colorScheme.primary)
                        is PermissionState.DeniedRuntime, is PermissionState.DeniedSpecial ->
                            TextButton(onClick = { onRequest(item) }) {
                                Text("⚠ 需授权")
                            }
                    }
                }
            }
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
