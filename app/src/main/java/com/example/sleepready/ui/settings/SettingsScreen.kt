package com.example.sleepready.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sleepready.domain.model.AutomationStrategy
import com.example.sleepready.domain.model.ColorScheme
import com.example.sleepready.domain.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenStatistics: () -> Unit = {}
) {
    val mode by viewModel.themeMode.collectAsStateWithLifecycle()
    val scheme by viewModel.colorScheme.collectAsStateWithLifecycle()
    val automation by viewModel.automationStrategy.collectAsStateWithLifecycle()
    val exactAlarmGranted by viewModel.exactAlarmGranted.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }

    // 双重确认 + 重启对话框状态
    var importStep by remember { mutableStateOf<ImportStep>(ImportStep.None) }

    val pickZip = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importFullBackup(uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsUiEvent.ExportSuccess ->
                    snackbar.showSnackbar("已导出到下载目录：${event.filename}")
                is SettingsUiEvent.ExportFailed ->
                    snackbar.showSnackbar("导出失败：${event.message}")
                SettingsUiEvent.ImportSuccess ->
                    importStep = ImportStep.RestartReady
                is SettingsUiEvent.ImportFailed ->
                    snackbar.showSnackbar("恢复失败：${event.message}")
            }
        }
    }

    LaunchedEffect(Unit) {
        // 进入设置页时刷新一次精确闹钟权限状态
        viewModel.refreshExactAlarmPermission()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "搜索")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 精确闹钟权限引导（API 31+ 默认未授）
            if (!exactAlarmGranted) {
                SectionCard("精确提醒") {
                    Text(
                        text = "Android 12 起，精确闹钟权限默认关闭。" +
                            "若不授予，印记的提醒可能会延迟几分钟才到达。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { viewModel.openExactAlarmSettings() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Filled.Alarm, contentDescription = null)
                        Text("  去授予精确提醒权限")
                    }
                }
            }

            SectionCard("主题模式") {
                Column(modifier = Modifier.selectableGroup()) {
                    RadioRow("跟随系统", mode == ThemeMode.SYSTEM) { viewModel.setThemeMode(ThemeMode.SYSTEM) }
                    RadioRow("浅色", mode == ThemeMode.LIGHT) { viewModel.setThemeMode(ThemeMode.LIGHT) }
                    RadioRow("深色（夜间模式）", mode == ThemeMode.DARK) { viewModel.setThemeMode(ThemeMode.DARK) }
                }
            }

            SectionCard("配色方案") {
                Column(modifier = Modifier.selectableGroup()) {
                    ColorScheme.values().forEach { s ->
                        RadioRow(
                            label = s.displayName + if (s == ColorScheme.DEFAULT) "（默认）" else "",
                            selected = scheme == s
                        ) { viewModel.setColorScheme(s) }
                    }
                }
            }

            SectionCard("入梦自动化执行策略") {
                Column(modifier = Modifier.selectableGroup()) {
                    AutomationStrategy.values().forEach { s ->
                        RadioRow(
                            label = s.displayName + if (s == AutomationStrategy.DEFAULT) "（默认）" else "",
                            selected = automation == s
                        ) { viewModel.setAutomationStrategy(s) }
                    }
                }
                Text(
                    text = "「能静默的先执行」：定时到期后立即完成所有不需要点屏幕的开关，" +
                        "其余开关等你解锁手机后自动跳转设置页继续。\n" +
                        "「全部等待解锁」：到期后不立即执行任何开关，全部留到解锁瞬间统一完成。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionCard("数据备份") {
                Text(
                    text = "导出整个数据库（印记 / 心动 / 时光 / 拾句 / 入梦方案）+ 应用图片到 zip 包，保存到系统下载目录。可用于换机迁移。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { viewModel.exportFullBackup() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Text("  导出全部数据")
                }
                OutlinedButton(
                    onClick = { importStep = ImportStep.FirstConfirm },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Restore, contentDescription = null)
                    Text("  从备份恢复")
                }
            }

            SectionCard("数据统计") {
                Text(
                    text = "查看各板块条目数、最近活动、连续打卡天数。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onOpenStatistics,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Filled.BarChart, contentDescription = null)
                    Text("  打开数据统计")
                }
            }
        }
    }

    // 恢复流程：第一次确认 → 第二次确认（提示重启）→ 选 zip → 完成后弹重启对话框
    when (importStep) {
        ImportStep.FirstConfirm -> AlertDialog(
            onDismissRequest = { importStep = ImportStep.None },
            title = { Text("恢复备份") },
            text = { Text("恢复将完全覆盖当前数据，所有未备份的内容会丢失。是否继续？") },
            confirmButton = {
                TextButton(onClick = { importStep = ImportStep.SecondConfirm }) {
                    Text("继续")
                }
            },
            dismissButton = {
                TextButton(onClick = { importStep = ImportStep.None }) { Text("取消") }
            }
        )
        ImportStep.SecondConfirm -> AlertDialog(
            onDismissRequest = { importStep = ImportStep.None },
            title = { Text("最后确认") },
            text = { Text("此操作不可撤销，应用恢复完成后将自动退出，需要你重新打开。") },
            confirmButton = {
                TextButton(onClick = {
                    importStep = ImportStep.None
                    pickZip.launch(arrayOf("application/zip", "*/*"))
                }) { Text("选择备份文件") }
            },
            dismissButton = {
                TextButton(onClick = { importStep = ImportStep.None }) { Text("取消") }
            }
        )
        ImportStep.RestartReady -> AlertDialog(
            onDismissRequest = { /* 必须确认 */ },
            title = { Text("恢复完成") },
            text = { Text("数据已恢复，应用将退出。请重新打开拾刻继续使用。") },
            confirmButton = {
                TextButton(onClick = {
                    importStep = ImportStep.None
                    viewModel.killAndRestart()
                }) { Text("好") }
            }
        )
        ImportStep.None -> {}
    }
}

private sealed interface ImportStep {
    data object None : ImportStep
    data object FirstConfirm : ImportStep
    data object SecondConfirm : ImportStep
    data object RestartReady : ImportStep
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
