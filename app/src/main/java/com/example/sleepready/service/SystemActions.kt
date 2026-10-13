package com.example.sleepready.service

import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.example.sleepready.domain.model.ExecutionEntry
import com.example.sleepready.domain.model.ExecutionResult
import com.example.sleepready.domain.model.SwitchAction
import com.example.sleepready.domain.model.SwitchItem
import com.example.sleepready.permission.PermissionMatrix

/**
 * Encapsulates all system API calls used by the ExecutionEngine. Each method
 * returns an [ExecutionEntry] and never throws — exceptions are converted to
 * PERMISSION_DENIED (SecurityException) or FAILED (any other Throwable),
 * supporting Property P10 (single-item failure isolation).
 *
 * Task 10.3 will append `openSettingsFor` to this same class for guided-action
 * switches (AirplaneMode / WiFi / MobileData / Location / NFC); the
 * [openSettingsIntent] helper is declared `internal` so it can be reused.
 */
class SystemActions(
    private val context: Context,
    private val screenRecordingController: ScreenRecordingController,
    private val onGuidedHint: ((SwitchItem, SwitchAction) -> Unit)? = null
) {

    // -- DND ------------------------------------------------------------------
    fun applyDnd(action: SwitchAction): ExecutionEntry =
        wrap(SwitchItem.DO_NOT_DISTURB, action) {
            if (action == SwitchAction.NONE) return@wrap ExecutionResult.SKIPPED to null
            val nm = context.getSystemService(NotificationManager::class.java)
                ?: throw IllegalStateException("NotificationManager unavailable")
            if (!nm.isNotificationPolicyAccessGranted) {
                throw SecurityException("ACCESS_NOTIFICATION_POLICY not granted")
            }
            val filter = when (action) {
                SwitchAction.TURN_ON -> NotificationManager.INTERRUPTION_FILTER_PRIORITY
                SwitchAction.TURN_OFF -> NotificationManager.INTERRUPTION_FILTER_ALL
                SwitchAction.NONE -> return@wrap ExecutionResult.SKIPPED to null
            }
            nm.setInterruptionFilter(filter)
            ExecutionResult.SUCCESS to null
        }

    // -- Bluetooth ------------------------------------------------------------
    fun applyBluetooth(action: SwitchAction): ExecutionEntry =
        wrap(SwitchItem.BLUETOOTH, action) {
            if (action == SwitchAction.NONE) return@wrap ExecutionResult.SKIPPED to null
            // API 33+: BluetoothAdapter.enable()/disable() are restricted; deep-link to settings.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                openSettingsIntent(Settings.ACTION_BLUETOOTH_SETTINGS)
                return@wrap ExecutionResult.REQUIRES_USER_ACTION to "请在设置页中切换蓝牙"
            }
            val adapter: BluetoothAdapter = context.getSystemService(BluetoothManager::class.java)
                ?.adapter
                ?: throw IllegalStateException("Bluetooth not available")
            @Suppress("DEPRECATION", "MissingPermission")
            val ok = when (action) {
                SwitchAction.TURN_ON -> adapter.enable()
                SwitchAction.TURN_OFF -> adapter.disable()
                else -> true
            }
            if (ok) ExecutionResult.SUCCESS to null
            else ExecutionResult.FAILED to "BluetoothAdapter rejected the request"
        }

    // -- Flashlight -----------------------------------------------------------
    fun applyFlashlight(action: SwitchAction): ExecutionEntry =
        wrap(SwitchItem.FLASHLIGHT, action) {
            if (action == SwitchAction.NONE) return@wrap ExecutionResult.SKIPPED to null
            val cm = context.getSystemService(CameraManager::class.java)
                ?: throw IllegalStateException("CameraManager unavailable")
            val cameraId = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return@wrap ExecutionResult.FAILED to "本机没有可控制的手电筒"
            val enabled = action == SwitchAction.TURN_ON
            cm.setTorchMode(cameraId, enabled)
            ExecutionResult.SUCCESS to null
        }

    // -- Screen recording (only TURN_OFF is meaningful) -----------------------
    fun applyScreenRecording(action: SwitchAction): ExecutionEntry {
        if (action != SwitchAction.TURN_OFF) {
            return ExecutionEntry(
                switchItem = SwitchItem.SCREEN_RECORDING,
                action = action,
                result = ExecutionResult.SKIPPED
            )
        }
        return screenRecordingController.stopIfHeld()
    }

    // -- Mute (RINGER_MODE_SILENT) -------------------------------------------
    fun applyMute(action: SwitchAction): ExecutionEntry =
        wrap(SwitchItem.MUTE, action) {
            if (action == SwitchAction.NONE) return@wrap ExecutionResult.SKIPPED to null
            requireDndAccess()
            val am = audioManagerOrThrow()
            val mode = when (action) {
                SwitchAction.TURN_ON -> AudioManager.RINGER_MODE_SILENT
                SwitchAction.TURN_OFF -> AudioManager.RINGER_MODE_NORMAL
                SwitchAction.NONE -> return@wrap ExecutionResult.SKIPPED to null
            }
            am.ringerMode = mode
            ExecutionResult.SUCCESS to null
        }

    // -- Vibrate (RINGER_MODE_VIBRATE) ---------------------------------------
    fun applyVibrate(action: SwitchAction): ExecutionEntry =
        wrap(SwitchItem.VIBRATE, action) {
            if (action == SwitchAction.NONE) return@wrap ExecutionResult.SKIPPED to null
            requireDndAccess()
            val am = audioManagerOrThrow()
            val mode = when (action) {
                SwitchAction.TURN_ON -> AudioManager.RINGER_MODE_VIBRATE
                SwitchAction.TURN_OFF -> AudioManager.RINGER_MODE_NORMAL
                SwitchAction.NONE -> return@wrap ExecutionResult.SKIPPED to null
            }
            am.ringerMode = mode
            ExecutionResult.SUCCESS to null
        }

    // -- Power save ----------------------------------------------------------
    fun applyPowerSave(action: SwitchAction): ExecutionEntry {
        if (action == SwitchAction.NONE) {
            return ExecutionEntry(SwitchItem.POWER_SAVE, action, ExecutionResult.SKIPPED)
        }
        // PowerManager.setPowerSaveModeEnabled requires DEVICE_POWER (signature
        // permission). Try via reflection so we can degrade gracefully on the
        // (overwhelming) majority of devices that reject it; OEM-restricted
        // builds end up routing the user to battery settings instead.
        return try {
            val pm = context.getSystemService(PowerManager::class.java)
                ?: throw IllegalStateException("PowerManager unavailable")
            val enabled = action == SwitchAction.TURN_ON
            val method = PowerManager::class.java.getMethod(
                "setPowerSaveModeEnabled", java.lang.Boolean.TYPE
            )
            val ok = method.invoke(pm, enabled) as? Boolean ?: false
            if (ok) {
                ExecutionEntry(SwitchItem.POWER_SAVE, action, ExecutionResult.SUCCESS)
            } else {
                openSettingsIntent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
                onGuidedHint?.invoke(SwitchItem.POWER_SAVE, action)
                ExecutionEntry(
                    SwitchItem.POWER_SAVE, action,
                    ExecutionResult.REQUIRES_USER_ACTION,
                    "请在电池设置中切换省电模式"
                )
            }
        } catch (sec: SecurityException) {
            openOptional(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            onGuidedHint?.invoke(SwitchItem.POWER_SAVE, action)
            ExecutionEntry(
                SwitchItem.POWER_SAVE, action,
                ExecutionResult.REQUIRES_USER_ACTION,
                "厂商限制，请在电池设置中切换"
            )
        } catch (t: Throwable) {
            openOptional(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            onGuidedHint?.invoke(SwitchItem.POWER_SAVE, action)
            ExecutionEntry(
                SwitchItem.POWER_SAVE, action,
                ExecutionResult.REQUIRES_USER_ACTION,
                t.message ?: "请在电池设置中切换省电模式"
            )
        }
    }

    // -- Eye comfort / night mode -------------------------------------------
    fun applyEyeComfort(action: SwitchAction): ExecutionEntry {
        if (action == SwitchAction.NONE) {
            return ExecutionEntry(SwitchItem.EYE_COMFORT, action, ExecutionResult.SKIPPED)
        }
        val canWrite =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                Settings.System.canWrite(context)
        if (!canWrite) {
            // No WRITE_SETTINGS — open the system grant page so the user can
            // toggle the perm, plus the display settings as a manual fallback.
            openOptional(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            onGuidedHint?.invoke(SwitchItem.EYE_COMFORT, action)
            return ExecutionEntry(
                SwitchItem.EYE_COMFORT, action,
                ExecutionResult.REQUIRES_USER_ACTION,
                "请授予「修改系统设置」权限或在显示设置中切换"
            )
        }
        return try {
            val value = if (action == SwitchAction.TURN_ON) 1 else 0
            Settings.System.putInt(context.contentResolver, KEY_NIGHT_MODE, value)
            ExecutionEntry(SwitchItem.EYE_COMFORT, action, ExecutionResult.SUCCESS)
        } catch (sec: SecurityException) {
            openOptional(Settings.ACTION_DISPLAY_SETTINGS)
            onGuidedHint?.invoke(SwitchItem.EYE_COMFORT, action)
            ExecutionEntry(
                SwitchItem.EYE_COMFORT, action,
                ExecutionResult.PERMISSION_DENIED,
                sec.message
            )
        } catch (t: Throwable) {
            openOptional(Settings.ACTION_DISPLAY_SETTINGS)
            onGuidedHint?.invoke(SwitchItem.EYE_COMFORT, action)
            ExecutionEntry(
                SwitchItem.EYE_COMFORT, action,
                ExecutionResult.REQUIRES_USER_ACTION,
                t.message ?: "请在显示设置中切换护眼模式"
            )
        }
    }

    // -- Helpers --------------------------------------------------------------

    /** Convenience wrapper that catches all throwables to satisfy Property P10. */
    private inline fun wrap(
        item: SwitchItem,
        action: SwitchAction,
        block: () -> Pair<ExecutionResult, String?>
    ): ExecutionEntry =
        try {
            val (result, message) = block()
            ExecutionEntry(item, action, result, message)
        } catch (sec: SecurityException) {
            ExecutionEntry(item, action, ExecutionResult.PERMISSION_DENIED, sec.message)
        } catch (t: Throwable) {
            ExecutionEntry(item, action, ExecutionResult.FAILED, t.message ?: t::class.simpleName)
        }

    /** Internal helper reused by Bluetooth fallback and by task 10.3's openSettingsFor. */
    internal fun openSettingsIntent(action: String) {
        val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** 公开版本，给 [com.example.sleepready.service.macro.MacroPlayer] 在回放前预先打开页面用。 */
    fun openSettingsActionRaw(action: String) {
        runCatching { openSettingsIntent(action) }
    }

    /** Like [openSettingsIntent] but swallows ActivityNotFoundException. */
    private fun openOptional(action: String) {
        runCatching { openSettingsIntent(action) }
    }

    private fun audioManagerOrThrow(): AudioManager =
        context.getSystemService(AudioManager::class.java)
            ?: throw IllegalStateException("AudioManager unavailable")

    private fun requireDndAccess() {
        val nm = context.getSystemService(NotificationManager::class.java)
            ?: throw IllegalStateException("NotificationManager unavailable")
        if (!nm.isNotificationPolicyAccessGranted) {
            throw SecurityException("ACCESS_NOTIFICATION_POLICY not granted")
        }
    }

    // -- Guided switches (require user interaction in system settings) --------
    /**
     * Opens the appropriate system settings page for a guided [SwitchItem]
     * (AirplaneMode / WiFi / MobileData / Location / NFC).
     *
     * Behaviour:
     *  - [SwitchAction.NONE] returns [ExecutionResult.SKIPPED].
     *  - No matching settings intent → [ExecutionResult.FAILED] with hint text.
     *  - On success → [ExecutionResult.REQUIRES_USER_ACTION] and an optional
     *    [onGuidedHint] callback (wired by AppContainer to TimerNotifications
     *    in task 12.1) is invoked so a notification can be posted.
     *  - [ActivityNotFoundException] → [ExecutionResult.FAILED]("未找到对应设置页").
     *  - Any other [Throwable] → [ExecutionResult.FAILED] with the throwable's
     *    message, preserving Property P10's failure isolation.
     */
    fun openSettingsFor(item: SwitchItem, action: SwitchAction): ExecutionEntry {
        if (action == SwitchAction.NONE) {
            return ExecutionEntry(item, action, ExecutionResult.SKIPPED)
        }
        val intent = PermissionMatrix.guidedSettingsIntent(item)
            ?: return ExecutionEntry(
                item, action, ExecutionResult.FAILED, "未找到对应设置页"
            )
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            onGuidedHint?.invoke(item, action)
            ExecutionEntry(
                item, action, ExecutionResult.REQUIRES_USER_ACTION,
                "请在系统设置页中切换 ${item.name}"
            )
        } catch (anf: ActivityNotFoundException) {
            ExecutionEntry(item, action, ExecutionResult.FAILED, "未找到对应设置页")
        } catch (t: Throwable) {
            ExecutionEntry(item, action, ExecutionResult.FAILED, t.message ?: t::class.simpleName)
        }
    }

    companion object {
        /** AOSP-standard secure setting key for night-light / eye comfort. */
        private const val KEY_NIGHT_MODE = "night_mode"
    }
}
