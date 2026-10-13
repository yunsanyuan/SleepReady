package com.example.sleepready.domain.model

import kotlinx.serialization.Serializable

/**
 * 入梦预设方案。每条独立保存名称、时长和 ActionPlan；用户在入梦页可以
 * 创建多套（深度睡眠 / 午休 / 起床 等），互不影响。
 *
 * 启动时由 ViewModel 把 [plan]/[durationMinutes] 拷进单行的 TimerConfig
 * 表，再由前台 Service 读取——这样不必改动 Service 的协议。
 */
@Serializable
data class TimerPreset(
    val id: Long,
    val name: String,
    val durationMinutes: Int,
    val plan: ActionPlan,
    val lastUsedAt: Long,
    val sortIndex: Int = 0,
    val pinned: Boolean = false,
    /** 起床后自动恢复：开关。 */
    val autoRestoreEnabled: Boolean = false,
    /** 起床后自动恢复：触发时刻（24h），默认 07:00。 */
    val autoRestoreHour: Int = 7,
    val autoRestoreMinute: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        fun newDefault(name: String): TimerPreset = TimerPreset(
            id = 0L,
            name = name,
            durationMinutes = TimerConfig.DEFAULT_DURATION_MINUTES,
            plan = ActionPlan.default(),
            lastUsedAt = 0L,
            sortIndex = 0,
            pinned = false,
            autoRestoreEnabled = false,
            autoRestoreHour = 7,
            autoRestoreMinute = 0,
            createdAt = 0L,
            updatedAt = 0L
        )
    }
}
