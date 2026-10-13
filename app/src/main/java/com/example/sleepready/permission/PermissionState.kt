package com.example.sleepready.permission

import android.content.Intent

/**
 * Snapshot of the platform-level permission status for a given
 * [com.example.sleepready.domain.model.SwitchItem] / action pair.
 *
 * Resolution rules (per design "Permission-Denial Flow"):
 * - [DeniedRuntime] → invoke `runtimePermissionLauncher.launch(permission)` to show the
 *   system dialog.
 * - [DeniedSpecial] → start the bundled [Intent] so the user can toggle the special
 *   permission in system settings (e.g., DnD access, exact alarm settings).
 */
sealed interface PermissionState {

    /** The permission is held (or none is needed) — proceed without prompting. */
    data object Granted : PermissionState

    /**
     * A standard runtime permission has been denied. Holds the manifest
     * permission string that must be passed to `RequestPermission()`.
     */
    data class DeniedRuntime(val permission: String) : PermissionState

    /**
     * A special permission (DnD access, exact alarm, MediaProjection token, etc.)
     * cannot be granted via the runtime dialog. Holds an [Intent] that should be
     * launched to open the corresponding system settings page.
     */
    data class DeniedSpecial(val settingsIntent: Intent) : PermissionState

    /** No permission is required for this combination of switch and action. */
    data object NotRequired : PermissionState
}
