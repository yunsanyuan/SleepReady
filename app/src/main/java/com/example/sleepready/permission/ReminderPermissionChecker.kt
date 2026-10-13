package com.example.sleepready.permission

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Coarse, three-way permission state used by the reminder permission guide UI.
 *
 * Note: this is intentionally separate from [PermissionState] (the matrix-driven one used by
 * the入梦 ActionPlan switches) — the reminder system tracks *system-level* user-facing
 * settings (notification toggle, exact-alarm grant, FSI grant, ROM auto-start) which all
 * have only three states from the user's POV: granted / can prompt at runtime /
 * permanently blocked, jump to settings.
 */
sealed class ReminderPermissionState {
    data object Granted : ReminderPermissionState()
    data class Denied(val shouldRequestRuntime: Boolean) : ReminderPermissionState()
    data class PermanentlyDenied(val settingsIntent: Intent?) : ReminderPermissionState()
}

data class ROMPermissionStatus(
    val type: String,
    val permissionKey: String,
    val isGranted: Boolean,
    val settingsIntent: Intent?
)

data class PermissionStatus(
    val postNotifications: ReminderPermissionState,
    val scheduleExactAlarm: ReminderPermissionState,
    val useFullScreenIntent: ReminderPermissionState,
    val romPermissions: List<ROMPermissionStatus> = emptyList()
)

object ReminderPermissionChecker {

    fun detectAll(context: Context): PermissionStatus {
        val romPerms = try {
            ROMDetector.detectMissingPermissions(context)
        } catch (_: Throwable) { emptyList() }
        return PermissionStatus(
            postNotifications = detectPostNotifications(context),
            scheduleExactAlarm = detectScheduleExactAlarm(context),
            useFullScreenIntent = detectUseFullScreenIntent(context),
            romPermissions = romPerms
        )
    }

    fun detectPostNotifications(context: Context): ReminderPermissionState {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return ReminderPermissionState.Granted
        return try {
            when (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)) {
                PackageManager.PERMISSION_GRANTED -> ReminderPermissionState.Granted
                else -> {
                    val shouldRequest = try {
                        !context.getSystemService(android.app.NotificationManager::class.java)!!
                            .areNotificationsEnabled()
                    } catch (_: Throwable) { true }
                    if (shouldRequest) ReminderPermissionState.Denied(shouldRequestRuntime = true)
                    else ReminderPermissionState.PermanentlyDenied(buildAppDetailsIntent(context))
                }
            }
        } catch (_: Throwable) { ReminderPermissionState.Granted }
    }

    fun detectScheduleExactAlarm(context: Context): ReminderPermissionState {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return ReminderPermissionState.Granted
        return try {
            val am = context.getSystemService(AlarmManager::class.java)
                ?: return ReminderPermissionState.Granted
            if (am.canScheduleExactAlarms()) ReminderPermissionState.Granted
            else ReminderPermissionState.PermanentlyDenied(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            )
        } catch (_: Throwable) { ReminderPermissionState.Granted }
    }

    fun detectUseFullScreenIntent(context: Context): ReminderPermissionState {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return ReminderPermissionState.Granted
        return try {
            when (ContextCompat.checkSelfPermission(context, Manifest.permission.USE_FULL_SCREEN_INTENT)) {
                PackageManager.PERMISSION_GRANTED -> ReminderPermissionState.Granted
                else -> ReminderPermissionState.Denied(shouldRequestRuntime = true)
            }
        } catch (_: Throwable) { ReminderPermissionState.Granted }
    }

    fun toGuideItems(status: PermissionStatus, context: Context): List<PermissionGuideItem> {
        val items = mutableListOf<PermissionGuideItem>()
        when (val s = status.postNotifications) {
            is ReminderPermissionState.Denied -> items.add(PermissionGuideItem(
                key = "post_notifications",
                title = "通知权限",
                description = "允许拾刻发送提醒通知，否则到点不会弹窗提醒",
                priority = 0,
                actionType = ActionType.REQUEST_RUNTIME,
                runtimePermission = Manifest.permission.POST_NOTIFICATIONS
            ))
            is ReminderPermissionState.PermanentlyDenied -> items.add(PermissionGuideItem(
                key = "post_notifications",
                title = "通知权限",
                description = "通知权限已被拒绝，请在系统设置中开启",
                priority = 0,
                actionType = ActionType.GOTO_SETTINGS,
                settingsIntent = s.settingsIntent ?: buildAppDetailsIntent(context)
            ))
            else -> {}
        }
        when (val s = status.scheduleExactAlarm) {
            is ReminderPermissionState.PermanentlyDenied -> items.add(PermissionGuideItem(
                key = "schedule_exact_alarm",
                title = "精确闹钟权限",
                description = "开启后提醒可准时触发，否则可能延迟数分钟",
                priority = 1,
                actionType = ActionType.GOTO_SETTINGS,
                settingsIntent = s.settingsIntent
            ))
            else -> {}
        }
        when (status.useFullScreenIntent) {
            is ReminderPermissionState.Denied -> items.add(PermissionGuideItem(
                key = "use_full_screen_intent",
                title = "锁屏全屏权限",
                description = "允许闹钟在锁屏下全屏显示，否则仅显示通知栏提醒",
                priority = 1,
                actionType = ActionType.REQUEST_RUNTIME,
                runtimePermission = Manifest.permission.USE_FULL_SCREEN_INTENT
            ))
            else -> {}
        }
        val romItems = try {
            ROMDetector.toGuideItems(status.romPermissions)
        } catch (_: Throwable) { emptyList() }
        items.addAll(romItems)
        return items.sortedBy { it.priority }
    }

    private fun buildAppDetailsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}
