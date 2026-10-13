package com.example.sleepready.permission

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.sleepready.domain.model.SwitchAction
import com.example.sleepready.domain.model.SwitchItem

/**
 * Description of what (if anything) is needed to perform a given switch action.
 * The PermissionManager turns this into a concrete [PermissionState] at request time.
 */
sealed interface PermissionRequirement {
    /** No permission is required for this switch/action combo. */
    data object None : PermissionRequirement

    /** A standard runtime permission. */
    data class Runtime(val permission: String) : PermissionRequirement

    /**
     * A "special" permission whose grant flow is to deep-link into a system
     * settings page. Resolved with [Intent] supplied at runtime — typically built
     * via the helpers in [PermissionMatrix].
     */
    data class Special(val intentAction: String) : PermissionRequirement

    /**
     * The user must initiate a one-shot consent flow, e.g.
     * MediaProjectionManager.createScreenCaptureIntent(). The PermissionManager
     * will hand off to a dedicated launcher.
     */
    data object UserInitiated : PermissionRequirement
}

/**
 * Static mapping between SwitchItem/Action and the permission requirement.
 * Pure logic — no Android Context required for the lookup itself.
 */
object PermissionMatrix {

    fun requirementFor(item: SwitchItem, action: SwitchAction): PermissionRequirement {
        if (action == SwitchAction.NONE) return PermissionRequirement.None
        return when (item) {
            SwitchItem.DO_NOT_DISTURB ->
                PermissionRequirement.Special(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            // MUTE / VIBRATE both call AudioManager.setRingerMode, which the
            // platform classifies under DnD policy access on API 23+.
            SwitchItem.MUTE,
            SwitchItem.VIBRATE ->
                PermissionRequirement.Special(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            SwitchItem.BLUETOOTH ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    PermissionRequirement.Runtime(Manifest.permission.BLUETOOTH_CONNECT)
                else
                    PermissionRequirement.None
            SwitchItem.FLASHLIGHT ->
                PermissionRequirement.Runtime(Manifest.permission.CAMERA)
            SwitchItem.SCREEN_RECORDING ->
                PermissionRequirement.UserInitiated
            // POWER_SAVE: PowerManager.setPowerSaveModeEnabled is restricted on
            // most non-system apps. We try the API and fall back to settings on
            // SecurityException; declare "no in-process permission" here so the
            // status card stays clean and the user only sees a hint when the
            // execution actually fails.
            SwitchItem.POWER_SAVE -> PermissionRequirement.None
            // EYE_COMFORT: Settings.System.putInt requires WRITE_SETTINGS.
            // That one is granted via a special settings page (not a runtime
            // dialog), so we model it as a Special requirement.
            SwitchItem.EYE_COMFORT ->
                PermissionRequirement.Special(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            // Guided switches: no in-process permission, just deep link to settings
            SwitchItem.AIRPLANE_MODE,
            SwitchItem.WIFI,
            SwitchItem.MOBILE_DATA,
            SwitchItem.LOCATION,
            SwitchItem.NFC -> PermissionRequirement.None
        }
    }

    /**
     * Notification posting requirement — API 33+ exposes POST_NOTIFICATIONS as a
     * runtime permission; lower SDKs grant it implicitly. Used by
     * [com.example.sleepready.service.notif.TimerNotifications] to decide whether
     * notifications must be no-op'd.
     */
    fun notificationsPermission(): PermissionRequirement =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            PermissionRequirement.Runtime(Manifest.permission.POST_NOTIFICATIONS)
        else
            PermissionRequirement.None

    /** Resolves the runtime requirement for a switch/action against the current context. */
    fun stateFor(context: Context, item: SwitchItem, action: SwitchAction): PermissionState {
        return when (val req = requirementFor(item, action)) {
            PermissionRequirement.None -> PermissionState.NotRequired
            PermissionRequirement.UserInitiated ->
                // Without an active MediaProjection token, treat as "needs user action".
                PermissionState.DeniedSpecial(buildScreenRecordingHintIntent())
            is PermissionRequirement.Runtime -> {
                val granted = ContextCompat.checkSelfPermission(context, req.permission) ==
                    PackageManager.PERMISSION_GRANTED
                if (granted) PermissionState.Granted
                else PermissionState.DeniedRuntime(req.permission)
            }
            is PermissionRequirement.Special -> {
                if (item == SwitchItem.DO_NOT_DISTURB ||
                    item == SwitchItem.MUTE ||
                    item == SwitchItem.VIBRATE
                ) {
                    val nm = context.getSystemService(NotificationManager::class.java)
                    val granted = nm?.isNotificationPolicyAccessGranted == true
                    if (granted) PermissionState.Granted
                    else PermissionState.DeniedSpecial(Intent(req.intentAction))
                } else if (item == SwitchItem.EYE_COMFORT) {
                    val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Settings.System.canWrite(context)
                    } else {
                        true
                    }
                    if (granted) {
                        PermissionState.Granted
                    } else {
                        val intent = Intent(req.intentAction).apply {
                            data = android.net.Uri.parse("package:${context.packageName}")
                        }
                        PermissionState.DeniedSpecial(intent)
                    }
                } else {
                    PermissionState.DeniedSpecial(Intent(req.intentAction))
                }
            }
        }
    }

    /** Settings deep-links for guided switches. */
    fun guidedSettingsIntent(item: SwitchItem): Intent? = when (item) {
        SwitchItem.AIRPLANE_MODE -> Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
        SwitchItem.WIFI -> Intent(Settings.ACTION_WIFI_SETTINGS)
        SwitchItem.MOBILE_DATA -> Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
        SwitchItem.LOCATION -> Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        SwitchItem.NFC -> Intent(Settings.ACTION_NFC_SETTINGS)
        else -> null
    }

    private fun buildScreenRecordingHintIntent(): Intent =
        Intent(Settings.ACTION_DISPLAY_SETTINGS)
}
