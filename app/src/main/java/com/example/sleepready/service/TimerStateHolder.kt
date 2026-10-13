package com.example.sleepready.service

import com.example.sleepready.domain.model.ActionPlan
import com.example.sleepready.domain.model.SwitchItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** Snapshot of the timer service's lifecycle as observed by the UI. */
sealed interface TimerState {
    data object Idle : TimerState

    /** Counting down. */
    data class Running(
        val totalMs: Long,
        val remainingMs: Long,
        val plan: ActionPlan
    ) : TimerState

    /** Countdown reached zero; ExecutionEngine is processing the plan. */
    data class Executing(
        val planSize: Int,
        val processed: Int,
        val currentItem: SwitchItem? = null
    ) : TimerState

    /** Execution flow is done — history record is available at [historyId]. */
    data class Finished(val historyId: Long) : TimerState
}

/** Fire-and-forget events surfaced to the UI (Snackbars, toasts, error messages). */
sealed interface OneShotEvent {
    /** "+15 minutes" tap exceeded the 120-minute cap (Requirement 11.4). */
    data object LimitReached : OneShotEvent

    /** startForegroundService failed (Requirement 9.4). */
    data class StartFailed(val message: String) : OneShotEvent
}

/**
 * Process-singleton holding the timer state. The UI reads it via Compose
 * `collectAsStateWithLifecycle`; the foreground service writes to it.
 */
object TimerStateHolder {

    private val _state = MutableStateFlow<TimerState>(TimerState.Idle)
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<OneShotEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<OneShotEvent> = _events.asSharedFlow()

    /** Service-side mutator. */
    fun update(next: TimerState) {
        _state.value = next
    }

    /** Service-side event emitter. */
    fun emit(event: OneShotEvent) {
        _events.tryEmit(event)
    }

    /** Reset back to Idle (used on cancel / completion). */
    fun reset() {
        _state.value = TimerState.Idle
    }
}
