package com.example.sleepready.ui.settings

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sleepready.data.prefs.PreferencesRepository
import com.example.sleepready.domain.model.AutomationStrategy
import com.example.sleepready.domain.model.ColorScheme
import com.example.sleepready.domain.model.GuidedExecutionMode
import com.example.sleepready.domain.model.ThemeMode
import com.example.sleepready.repository.BackupRepository
import com.example.sleepready.repository.MomentRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

sealed interface SettingsUiEvent {
    data class ExportSuccess(val filename: String) : SettingsUiEvent
    data class ExportFailed(val message: String) : SettingsUiEvent
    data object ImportSuccess : SettingsUiEvent
    data class ImportFailed(val message: String) : SettingsUiEvent
}

/**
 * Settings backing model（v7）。
 *
 *  - 主题 / 配色 / 自动化策略 已有
 *  - 全库备份导出：[exportFullBackup]
 *  - 全库备份恢复：[importFullBackup]，成功后由 UI 提示用户重启
 *  - 精确闹钟权限：[canScheduleExactAlarms] + [openExactAlarmSettings]
 */
class SettingsViewModel(
    application: Application,
    private val prefs: PreferencesRepository,
    private val momentRepo: MomentRepository
) : AndroidViewModel(application) {

    val themeMode: StateFlow<ThemeMode> = prefs.observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.themeMode)

    val colorScheme: StateFlow<ColorScheme> = prefs.observeColorScheme()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.colorScheme)

    val automationStrategy: StateFlow<AutomationStrategy> = prefs.observeAutomationStrategy()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.automationStrategy)

    val guidedExecutionMode: StateFlow<GuidedExecutionMode> = prefs.observeGuidedExecutionMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.guidedExecutionMode)

    private val _events = MutableSharedFlow<SettingsUiEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<SettingsUiEvent> = _events.asSharedFlow()

    private val _exactAlarmGranted = MutableStateFlow(checkExactAlarmPermission())
    val exactAlarmGranted: StateFlow<Boolean> = _exactAlarmGranted.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.themeMode = mode
        AppCompatDelegate.setDefaultNightMode(mode.toNightMode())
    }

    fun setColorScheme(scheme: ColorScheme) { prefs.colorScheme = scheme }
    fun setAutomationStrategy(strategy: AutomationStrategy) { prefs.automationStrategy = strategy }
    fun setGuidedExecutionMode(mode: GuidedExecutionMode) { prefs.guidedExecutionMode = mode }

    /** 导出整库到下载目录。 */
    fun exportFullBackup() {
        viewModelScope.launch {
            val ctx = getApplication<Application>().applicationContext
            val result = BackupRepository.exportFullBackup(ctx)
            result.onSuccess { _events.tryEmit(SettingsUiEvent.ExportSuccess(it)) }
                .onFailure { _events.tryEmit(SettingsUiEvent.ExportFailed(it.message ?: "导出失败")) }
        }
    }

    /** 从用户选择的 zip 恢复整库。成功后调用 [killAndRestart]。 */
    fun importFullBackup(uri: Uri) {
        viewModelScope.launch {
            val ctx = getApplication<Application>().applicationContext
            val result = BackupRepository.importFullBackup(ctx, uri)
            result.onSuccess {
                _events.tryEmit(SettingsUiEvent.ImportSuccess)
                // 给 UI 一点时间显示提示再杀进程；UI 端会调用 killAndRestart
            }.onFailure {
                _events.tryEmit(SettingsUiEvent.ImportFailed(it.message ?: "恢复失败"))
            }
        }
    }

    /** 杀掉当前进程；下次冷启动会读到新的 db 与图片。 */
    fun killAndRestart() {
        Process.killProcess(Process.myPid())
        exitProcess(0)
    }

    fun refreshExactAlarmPermission() {
        _exactAlarmGranted.value = checkExactAlarmPermission()
    }

    private fun checkExactAlarmPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val ctx = getApplication<Application>().applicationContext
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return am.canScheduleExactAlarms()
    }

    /** 引导用户去系统设置授予「闹钟和提醒」权限。 */
    fun openExactAlarmSettings() {
        val ctx = getApplication<Application>().applicationContext
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${ctx.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else null
        runCatching { intent?.let { ctx.startActivity(it) } }
    }
}

private fun ThemeMode.toNightMode(): Int = when (this) {
    ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
    ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
    ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
}
