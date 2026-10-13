package com.example.sleepready.domain.model

import kotlinx.serialization.Serializable

/**
 * Saved timer configuration: a duration plus the per-switch action plan.
 *
 * Validates Property P15 (round-trip persistence) and Property P11 (+15 cap)
 * via the constants exposed in the companion object.
 */
@Serializable
data class TimerConfig(
    val durationMinutes: Int,
    val plan: ActionPlan
) {
    companion object {
        const val MIN_DURATION_MINUTES = 1
        const val MAX_DURATION_MINUTES = 24 * 60 // 24 小时上限
        const val EXTEND_STEP_MINUTES = 15
        /** 快捷预设：15 / 30 / 45 分钟 / 1 小时 / 2 小时 / 8 小时。 */
        val PRESET_DURATIONS: List<Int> = listOf(15, 30, 45, 60, 120, 480)
        const val DEFAULT_DURATION_MINUTES = 30

        /** Coerces [minutes] into [MIN_DURATION_MINUTES]..[MAX_DURATION_MINUTES]. */
        fun clampMinutes(minutes: Int): Int =
            minutes.coerceIn(MIN_DURATION_MINUTES, MAX_DURATION_MINUTES)

        /** First-launch default: 30 minutes + DnD on. */
        fun default(): TimerConfig = TimerConfig(
            durationMinutes = DEFAULT_DURATION_MINUTES,
            plan = ActionPlan.default()
        )
    }
}
