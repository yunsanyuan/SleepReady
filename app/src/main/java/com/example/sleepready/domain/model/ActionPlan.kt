package com.example.sleepready.domain.model

import kotlinx.serialization.Serializable

/**
 * User-configured action plan. Each [SwitchItem] is mapped to a [SwitchAction].
 * Properties P5 (round-trip JSON) and P6 (ScreenRecording cannot be TURN_ON) hold
 * after [normalized] has been applied.
 */
@Serializable
data class ActionPlan(val map: Map<SwitchItem, SwitchAction> = emptyMap()) {

    /**
     * Normalizes the plan:
     * - Ensures every SwitchItem has an explicit entry, defaulting to NONE.
     * - Forces SCREEN_RECORDING to NONE when the user picked TURN_ON
     *   (the app cannot meaningfully start a screen recording — see Requirement 7.5).
     */
    fun normalized(): ActionPlan {
        val full = SwitchItem.values().associateWith { item ->
            val current = map[item] ?: SwitchAction.NONE
            if (item == SwitchItem.SCREEN_RECORDING && current == SwitchAction.TURN_ON) {
                SwitchAction.NONE
            } else {
                current
            }
        }
        return ActionPlan(full)
    }

    /** Convenience accessor that always falls back to NONE for unset items. */
    fun actionFor(item: SwitchItem): SwitchAction = map[item] ?: SwitchAction.NONE

    companion object {
        /** First-launch default: every switch is NONE; the user opts in explicitly. */
        fun default(): ActionPlan = ActionPlan(emptyMap()).normalized()
    }
}
