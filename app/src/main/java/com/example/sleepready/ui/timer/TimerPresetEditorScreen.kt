package com.example.sleepready.ui.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sleepready.domain.model.ActionPlan
import com.example.sleepready.domain.model.SwitchAction
import com.example.sleepready.domain.model.SwitchItem
import com.example.sleepready.domain.model.TimerConfig
import com.example.sleepready.domain.model.TimerPreset
import com.example.sleepready.permission.LocalPermissionManager
import com.example.sleepready.permission.PermissionState
import com.example.sleepready.ui.timer.components.ActionPlanList
import com.example.sleepready.ui.timer.components.DurationSelector
import com.example.sleepready.ui.timer.components.PermissionStatusCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerPresetEditorScreen(
    initial: TimerPreset?,
    permissionStates: Map<SwitchItem, PermissionState>,
    onBack: () -> Unit,
    onSave: (TimerPreset) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "新方案") }
    var duration by remember {
        mutableStateOf(initial?.durationMinutes ?: TimerConfig.DEFAULT_DURATION_MINUTES)
    }
    var plan by remember {
        mutableStateOf(initial?.plan ?: ActionPlan.default())
    }
    var autoRestoreEnabled by remember { mutableStateOf(initial?.autoRestoreEnabled ?: false) }
    var autoRestoreHour by remember { mutableStateOf(initial?.autoRestoreHour ?: 7) }
    var autoRestoreMinute by remember { mutableStateOf(initial?.autoRestoreMinute ?: 0) }
    var showTimePicker by remember { mutableStateOf(false) }

    val pm = LocalPermissionManager.current
    val snackbar = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initial == null) "新建方案" else "编辑方案") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        enabled = name.isNotBlank(),
                        onClick = {
                            val saved = (initial ?: TimerPreset.newDefault(name.trim())).copy(
                                name = name.trim(),
                                durationMinutes = TimerConfig.clampMinutes(duration),
                                plan = plan.normalized(),
                                autoRestoreEnabled = autoRestoreEnabled,
                                autoRestoreHour = autoRestoreHour,
                                autoRestoreMinute = autoRestoreMinute
                            )
                            onSave(saved)
                        }
                    ) { Text("保存") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("方案名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            PermissionStatusCard(
                plan = plan,
                states = permissionStates,
                onRequest = { item ->
                    pm?.request(item, plan.actionFor(item))
                }
            )

            Text("倒计时时长", style = MaterialTheme.typography.titleMedium)
            DurationSelector(
                selectedMinutes = duration,
                onSelect = { duration = TimerConfig.clampMinutes(it) }
            )

            Text("入梦时要做什么", style = MaterialTheme.typography.titleMedium)
            ActionPlanList(
                plan = plan,
                onChange = { item: SwitchItem, action: SwitchAction ->
                    val updated = plan.map.toMutableMap().apply { put(item, action) }
                    plan = ActionPlan(updated).normalized()
                }
            )

            // 起床后自动恢复
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("起床后自动恢复", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "到点后把被本方案改变的开关恢复到执行前的状态",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoRestoreEnabled,
                            onCheckedChange = { autoRestoreEnabled = it }
                        )
                    }
                    if (autoRestoreEnabled) {
                        TextButton(onClick = { showTimePicker = true }) {
                            Text("恢复时间：%02d:%02d".format(autoRestoreHour, autoRestoreMinute))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(
            initialHour = autoRestoreHour,
            initialMinute = autoRestoreMinute,
            is24Hour = true
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    autoRestoreHour = state.hour
                    autoRestoreMinute = state.minute
                    showTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            },
            title = { Text("选择恢复时间") },
            text = { TimePicker(state = state) }
        )
    }

    LaunchedEffect(Unit) { /* placeholder */ }
}
