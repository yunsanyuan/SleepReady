package com.example.sleepready.ui.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sleepready.domain.model.ActionPlan
import com.example.sleepready.domain.model.SwitchAction
import com.example.sleepready.domain.model.SwitchItem
import com.example.sleepready.service.OneShotEvent
import com.example.sleepready.service.TimerState

/**
 * Full-screen countdown UI.
 * Validates Requirements 10.1, 10.2, 10.3, 10.4, 11.1, 11.2, 11.3, 11.4.
 * Property P11 (+15 cap) surfaces here as a Snackbar when the service emits
 * [OneShotEvent.LimitReached].
 */
@Composable
fun CountdownScreen(
    viewModel: CountdownViewModel,
    onFinished: (historyId: Long) -> Unit,
    onCancelled: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                OneShotEvent.LimitReached ->
                    snackbar.showSnackbar("已达 24 小时上限")
                is OneShotEvent.StartFailed ->
                    snackbar.showSnackbar("启动失败：${event.message}")
            }
        }
    }

    LaunchedEffect(state) {
        when (val s = state) {
            is TimerState.Finished -> onFinished(s.historyId)
            TimerState.Idle -> onCancelled()
            else -> {}
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        com.example.sleepready.ui.timer.components.BreathingHaloBackground(
            modifier = Modifier.padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                val (remainingMs, plan) = when (val s = state) {
                    is TimerState.Running -> s.remainingMs to s.plan
                    is TimerState.Executing -> 0L to ActionPlan.default()
                    else -> 0L to ActionPlan.default()
                }

                com.example.sleepready.ui.timer.components.CountdownDigits(remainingMs = remainingMs)

                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = summarize(plan),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(onClick = { viewModel.cancel() }) { Text("取消") }
                    Button(onClick = { viewModel.extend() }) { Text("+15 分钟") }
                }
            }
        }
    }
}

private fun summarize(plan: ActionPlan): String {
    val parts = SwitchItem.executionOrder.mapNotNull { item ->
        when (plan.actionFor(item)) {
            SwitchAction.TURN_ON -> "开启 ${labelFor(item)}"
            SwitchAction.TURN_OFF -> "关闭 ${labelFor(item)}"
            SwitchAction.NONE -> null
        }
    }
    return if (parts.isEmpty()) "倒计时结束后将不执行任何操作"
    else "倒计时结束后将：" + parts.joinToString("、")
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
