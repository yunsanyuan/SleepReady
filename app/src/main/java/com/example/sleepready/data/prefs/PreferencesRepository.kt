package com.example.sleepready.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.sleepready.domain.model.AutomationStrategy
import com.example.sleepready.domain.model.ColorScheme
import com.example.sleepready.domain.model.GuidedExecutionMode
import com.example.sleepready.domain.model.SwitchAction
import com.example.sleepready.domain.model.SwitchItem
import com.example.sleepready.domain.model.ThemeMode
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * SharedPreferences 仓库。v8 起拆成两个独立文件：
 *
 *  - `ui_prefs`：用户偏好（主题、配色、自动化策略、上次时长、onboarding 标志）
 *  - `runtime_prefs`：运行态（runningPresetId / pendingGuidedPlan / pendingHistoryId）
 *
 * 这样：
 *   1. 用户偏好不会因为运行态写入频繁而被反复覆盖；
 *   2. 即使 runtime_prefs 在某些 ROM 极端情况下丢失，UI 偏好仍然完好。
 */
class PreferencesRepository(context: Context) {

    private val ui: SharedPreferences =
        context.applicationContext.getSharedPreferences(UI_PREFS, Context.MODE_PRIVATE)

    private val runtime: SharedPreferences =
        context.applicationContext.getSharedPreferences(RUNTIME_PREFS, Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = ThemeMode.fromStorage(ui.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.storageValue))
        set(value) = ui.edit { putString(KEY_THEME_MODE, value.storageValue) }

    var colorScheme: ColorScheme
        get() = ColorScheme.fromStorage(ui.getString(KEY_COLOR_SCHEME, ColorScheme.DEFAULT.storageValue))
        set(value) = ui.edit { putString(KEY_COLOR_SCHEME, value.storageValue) }

    var lastTimerDurationMinutes: Int
        get() = ui.getInt(KEY_LAST_TIMER_DURATION, DEFAULT_DURATION_MINUTES)
        set(value) = ui.edit { putInt(KEY_LAST_TIMER_DURATION, value) }

    var onboardingShown: Boolean
        get() = ui.getBoolean(KEY_ONBOARDING_SHOWN, false)
        set(value) = ui.edit { putBoolean(KEY_ONBOARDING_SHOWN, value) }

    var automationStrategy: AutomationStrategy
        get() = AutomationStrategy.fromStorage(
            ui.getString(KEY_AUTOMATION_STRATEGY, AutomationStrategy.DEFAULT.storageValue)
        )
        set(value) = ui.edit { putString(KEY_AUTOMATION_STRATEGY, value.storageValue) }

    var guidedExecutionMode: GuidedExecutionMode
        get() = GuidedExecutionMode.fromStorage(
            ui.getString(KEY_GUIDED_EXEC_MODE, GuidedExecutionMode.DEFAULT.storageValue)
        )
        set(value) = ui.edit { putString(KEY_GUIDED_EXEC_MODE, value.storageValue) }

    /** 当前正在运行的入梦预设 id；0 = 未运行。 */
    var runningPresetId: Long
        get() = runtime.getLong(KEY_RUNNING_PRESET_ID, 0L)
        set(value) = runtime.edit { putLong(KEY_RUNNING_PRESET_ID, value) }

    /**
     * 待解锁后执行的 B 类计划（GuidedItem→Action）。空 = 无待执行。
     * 结构：JSON object，键为 SwitchItem.name，值为 SwitchAction.name。
     */
    var pendingGuidedPlan: Map<SwitchItem, SwitchAction>
        get() {
            val raw = runtime.getString(KEY_PENDING_GUIDED_PLAN, null) ?: return emptyMap()
            return runCatching {
                val rawMap = json.decodeFromString(stringMapSerializer, raw)
                rawMap.mapNotNull { (k, v) ->
                    runCatching {
                        SwitchItem.valueOf(k) to SwitchAction.valueOf(v)
                    }.getOrNull()
                }.toMap()
            }.getOrElse { emptyMap() }
        }
        set(value) {
            val rawMap = value.entries.associate { (k, v) -> k.name to v.name }
            val payload = json.encodeToString(stringMapSerializer, rawMap)
            runtime.edit { putString(KEY_PENDING_GUIDED_PLAN, payload) }
        }

    fun clearPendingGuidedPlan() {
        runtime.edit { remove(KEY_PENDING_GUIDED_PLAN) }
    }

    /** 历史记录上次的 startedAt（用于解锁回放完后回填同一条记录）。 */
    var pendingHistoryId: Long
        get() = runtime.getLong(KEY_PENDING_HISTORY_ID, 0L)
        set(value) = runtime.edit { putLong(KEY_PENDING_HISTORY_ID, value) }

    /**
     * 起床后自动恢复：被改变开关的反向 ActionPlan（GuidedItem→Action）。
     * 与 [pendingGuidedPlan] 共用编码格式但语义不同（这是恢复用的反向操作）。
     */
    var autoRestorePlan: Map<SwitchItem, SwitchAction>
        get() {
            val raw = runtime.getString(KEY_AUTO_RESTORE_PLAN, null) ?: return emptyMap()
            return runCatching {
                val rawMap = json.decodeFromString(stringMapSerializer, raw)
                rawMap.mapNotNull { (k, v) ->
                    runCatching {
                        SwitchItem.valueOf(k) to SwitchAction.valueOf(v)
                    }.getOrNull()
                }.toMap()
            }.getOrElse { emptyMap() }
        }
        set(value) {
            val rawMap = value.entries.associate { (k, v) -> k.name to v.name }
            val payload = json.encodeToString(stringMapSerializer, rawMap)
            runtime.edit { putString(KEY_AUTO_RESTORE_PLAN, payload) }
        }

    fun clearAutoRestorePlan() {
        runtime.edit { remove(KEY_AUTO_RESTORE_PLAN) }
    }

    fun observeThemeMode(): Flow<ThemeMode> = observeUi(KEY_THEME_MODE) { themeMode }
    fun observeColorScheme(): Flow<ColorScheme> = observeUi(KEY_COLOR_SCHEME) { colorScheme }
    fun observeRunningPresetId(): Flow<Long> = observeRuntime(KEY_RUNNING_PRESET_ID) { runningPresetId }
    fun observeAutomationStrategy(): Flow<AutomationStrategy> =
        observeUi(KEY_AUTOMATION_STRATEGY) { automationStrategy }
    fun observeGuidedExecutionMode(): Flow<GuidedExecutionMode> =
        observeUi(KEY_GUIDED_EXEC_MODE) { guidedExecutionMode }

    private inline fun <T> observeUi(watchedKey: String, crossinline read: () -> T): Flow<T> =
        observeOn(ui, watchedKey, read)

    private inline fun <T> observeRuntime(watchedKey: String, crossinline read: () -> T): Flow<T> =
        observeOn(runtime, watchedKey, read)

    private inline fun <T> observeOn(
        prefs: SharedPreferences,
        watchedKey: String,
        crossinline read: () -> T
    ): Flow<T> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == watchedKey) {
                trySend(read())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
        .onStart { emit(read()) }
        .distinctUntilChanged()

    /**
     * 旧版本（v1.7 之前）所有键都在 sleep_ready_prefs 里。这里把残留运行态 / UI 偏好
     * 平移到对应的新文件，迁移完成后删掉旧文件。仅在第一次启动 v8 时执行一次。
     */
    init {
        val legacy = context.applicationContext
            .getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        if (legacy.all.isNotEmpty()) {
            // UI 偏好搬到 ui，运行态搬到 runtime
            legacy.all.forEach { (key, value) ->
                when (key) {
                    KEY_THEME_MODE, KEY_COLOR_SCHEME, KEY_LAST_TIMER_DURATION,
                    KEY_ONBOARDING_SHOWN, KEY_AUTOMATION_STRATEGY, KEY_GUIDED_EXEC_MODE ->
                        ui.edit { putAny(key, value) }
                    KEY_RUNNING_PRESET_ID, KEY_PENDING_GUIDED_PLAN, KEY_PENDING_HISTORY_ID ->
                        runtime.edit { putAny(key, value) }
                }
            }
            legacy.edit { clear() }
        }
    }

    private fun SharedPreferences.Editor.putAny(key: String, value: Any?) {
        when (value) {
            is String -> putString(key, value)
            is Int -> putInt(key, value)
            is Long -> putLong(key, value)
            is Boolean -> putBoolean(key, value)
            is Float -> putFloat(key, value)
            else -> { /* drop unknown */ }
        }
    }

    companion object {
        private const val LEGACY_PREFS_NAME = "sleep_ready_prefs"
        private const val UI_PREFS = "ui_prefs"
        private const val RUNTIME_PREFS = "runtime_prefs"

        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_COLOR_SCHEME = "color_scheme"
        const val KEY_LAST_TIMER_DURATION = "last_timer_duration_minutes"
        const val KEY_ONBOARDING_SHOWN = "onboarding_shown"
        const val KEY_RUNNING_PRESET_ID = "running_preset_id"
        const val KEY_AUTOMATION_STRATEGY = "automation_strategy"
        const val KEY_GUIDED_EXEC_MODE = "guided_execution_mode"
        const val KEY_PENDING_GUIDED_PLAN = "pending_guided_plan"
        const val KEY_PENDING_HISTORY_ID = "pending_history_id"
        const val KEY_AUTO_RESTORE_PLAN = "auto_restore_plan"
        const val DEFAULT_DURATION_MINUTES = 30

        private val json = Json { ignoreUnknownKeys = true }
        private val stringMapSerializer =
            MapSerializer(String.serializer(), String.serializer())
    }
}
