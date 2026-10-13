package com.example.sleepready.permission

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.sleepready.domain.model.SwitchAction
import com.example.sleepready.domain.model.SwitchItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centralized permission gateway. Owned by [com.example.sleepready.MainActivity];
 * exposed to the Compose tree through [LocalPermissionManager].
 *
 * Resolution rules (per design "Permission-Denial Flow" and the user-confirmed
 * clarification):
 * - Runtime denials → re-invoke the [runtimeLauncher] (system dialog).
 * - Special denials → start the bundled settings [Intent].
 *
 * Requirements: 8.3, 8.4, 8.5, 23.1, 23.2, 23.3.
 */
class PermissionManager(private val activity: ComponentActivity) {

    private val runtimeLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            pendingResume?.invoke(granted)
            pendingResume = null
            refresh()
        }

    private val notificationsLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationsPendingResume?.invoke(granted)
            notificationsPendingResume = null
            refresh()
        }

    private var pendingResume: ((Boolean) -> Unit)? = null
    private var notificationsPendingResume: ((Boolean) -> Unit)? = null

    private val _states = MutableStateFlow<Map<SwitchItem, PermissionState>>(emptyMap())
    val states: StateFlow<Map<SwitchItem, PermissionState>> = _states.asStateFlow()

    init {
        // Refresh whenever the activity resumes — captures user grants made in Settings.
        activity.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        })
        refresh()
    }

    /** Returns the live permission state for [item]/[action]. */
    fun stateFor(item: SwitchItem, action: SwitchAction): PermissionState =
        PermissionMatrix.stateFor(activity, item, action)

    /**
     * Recomputes the permission map for every [SwitchItem] (using its current
     * action as TURN_OFF — semantics are the same for runtime checks).
     */
    fun refresh() {
        _states.value = SwitchItem.values().associateWith { item ->
            stateFor(item, SwitchAction.TURN_OFF)
        }
    }

    /**
     * Initiates the appropriate grant flow for [item] / [action].
     * - Runtime → launches the system dialog; [onDone] fires with the result.
     * - Special → opens the matching settings page; [onDone] fires immediately
     *   with `false` (the actual state will be re-read on ON_RESUME).
     */
    fun request(item: SwitchItem, action: SwitchAction, onDone: (Boolean) -> Unit = {}) {
        when (val state = stateFor(item, action)) {
            PermissionState.Granted, PermissionState.NotRequired -> onDone(true)
            is PermissionState.DeniedRuntime -> {
                pendingResume = onDone
                runtimeLauncher.launch(state.permission)
            }
            is PermissionState.DeniedSpecial -> {
                runCatching {
                    activity.startActivity(state.settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                onDone(false)
            }
        }
    }

    /** Request POST_NOTIFICATIONS on API 33+. No-op on lower SDKs. */
    fun requestNotifications(onDone: (Boolean) -> Unit = {}) {
        when (val req = PermissionMatrix.notificationsPermission()) {
            PermissionRequirement.None -> onDone(true)
            is PermissionRequirement.Runtime -> {
                notificationsPendingResume = onDone
                notificationsLauncher.launch(req.permission)
            }
            else -> onDone(false)
        }
    }
}

/** CompositionLocal for injecting the host's [PermissionManager] into Compose. */
val LocalPermissionManager = staticCompositionLocalOf<PermissionManager> {
    error("PermissionManager not provided. Wrap content in CompositionLocalProvider(LocalPermissionManager provides ...).")
}
