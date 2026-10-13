package com.example.sleepready.ui.timer

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.example.sleepready.service.OneShotEvent
import com.example.sleepready.service.TimerState
import com.example.sleepready.service.TimerStateHolder
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Bridges the process-singleton [TimerStateHolder] (written by the foreground
 * [com.example.sleepready.service.SleepReadyTimerService]) to the countdown UI.
 * UI controls are translated into Intents back to the service.
 *
 * Validates Requirements 10.4, 11.1, 11.2, 11.3, 11.4. No properties are
 * directly checked here — the ViewModel is purely a subscription/dispatch
 * layer; +15 cap (P11) and Cancel-no-history (P12) are validated against the
 * service.
 */
class CountdownViewModel(app: Application) : AndroidViewModel(app) {

    /** Snapshot of countdown / executing / finished state for the UI. */
    val state: StateFlow<TimerState> = TimerStateHolder.state

    /** Fire-and-forget events surfaced to the UI (e.g. [OneShotEvent.LimitReached]). */
    val events: SharedFlow<OneShotEvent> = TimerStateHolder.events

    /** Send `ACTION_CANCEL` to the timer service (Requirements 11.1, 11.2). */
    fun cancel() = sendCommand(ACTION_CANCEL)

    /** Send `ACTION_EXTEND` to the timer service (Requirements 11.1, 11.3, 11.4). */
    fun extend() = sendCommand(ACTION_EXTEND)

    private fun sendCommand(action: String) {
        val ctx = getApplication<Application>()
        // Use Class.forName so this ViewModel does not statically depend on
        // SleepReadyTimerService (which lives in a sibling task). Resolution
        // happens at runtime once the service class is on the classpath.
        val serviceClass = Class.forName(SERVICE_CLASS_NAME)
        val intent = Intent(ctx, serviceClass).apply { this.action = action }
        ContextCompat.startForegroundService(ctx, intent)
    }

    companion object {
        const val ACTION_CANCEL = "com.example.sleepready.timer.CANCEL"
        const val ACTION_EXTEND = "com.example.sleepready.timer.EXTEND"
        private const val SERVICE_CLASS_NAME =
            "com.example.sleepready.service.SleepReadyTimerService"
    }
}
