package com.example.sleepready.service.notif

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.sleepready.domain.model.SwitchAction
import com.example.sleepready.domain.model.SwitchItem
import com.example.sleepready.permission.PermissionMatrix
import com.example.sleepready.permission.PermissionRequirement

/**
 * Centralised builder/poster for every notification surface used by the timer
 * subsystem. Three channels are registered eagerly in [init]:
 *  - [CHANNEL_COUNTDOWN]  (low importance, ongoing countdown notification)
 *  - [CHANNEL_EXECUTING]  (default importance, "executing"/"summary")
 *  - [CHANNEL_GUIDED_HINT](high importance, post-only guided action hints)
 *
 * Permission gating: the [PermissionMatrix.notificationsPermission] entry is the
 * single source of truth for whether the app may post notifications. On API 33+
 * (TIRAMISU) the user must have granted POST_NOTIFICATIONS. When the permission
 * is missing every `post*` helper degrades to a no-op so the timer Service still
 * runs and the UI keeps working off [com.example.sleepready.service.TimerStateHolder].
 *
 * Note: the `build*` helpers always return a [Notification]. The foreground
 * service must still pass an instance to `startForeground(...)` even when the
 * user has denied POST_NOTIFICATIONS — Android will silently elide the visual
 * affordance but the service stays alive.
 *
 * Validates Requirements: 12.1, 12.3, 12.4, 12.5, 16.2, 18.1, 18.2, 18.3.
 */
class TimerNotifications(private val context: Context) {

    private val manager: NotificationManagerCompat = NotificationManagerCompat.from(context)

    init {
        registerChannels()
    }

    // -- Channel registration -------------------------------------------------

    private fun registerChannels() {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_COUNTDOWN,
                "入梦倒计时",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "倒计时进行中的常驻通知"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_EXECUTING,
                "执行进度",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "倒计时结束后顺序执行系统开关时的进度通知"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_GUIDED_HINT,
                "操作提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "需要你前往设置页手动切换的开关提醒"
            }
        )
    }

    // -- Builders -------------------------------------------------------------

    /**
     * Countdown notification. Title is fixed; body is the remaining time
     * formatted as MM:SS. Always includes a "Cancel" action that stops the
     * countdown when tapped.
     */
    fun buildCountdown(
        remainingMs: Long,
        cancelPendingIntent: PendingIntent,
        contentIntent: PendingIntent? = null
    ): Notification {
        val safeRemainingMs = remainingMs.coerceAtLeast(0L)
        val totalSeconds = safeRemainingMs / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        val timeText = "%02d:%02d".format(minutes, seconds)
        return NotificationCompat.Builder(context, CHANNEL_COUNTDOWN)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("入梦倒计时")
            .setContentText("剩余 $timeText")
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(contentIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "取消",
                cancelPendingIntent
            )
            .build()
    }

    /** Notification shown while the [com.example.sleepready.service.ExecutionEngine] runs. */
    fun buildExecuting(contentIntent: PendingIntent? = null): Notification =
        NotificationCompat.Builder(context, CHANNEL_EXECUTING)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("正在执行你的定时任务…")
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(contentIntent)
            .build()

    /**
     * Final summary notification. Tapping the body opens the History screen via
     * [contentIntent]; the foreground service detaches the notification with
     * `STOP_FOREGROUND_DETACH` so this remains visible after the service stops.
     */
    fun buildSummary(historyId: Long, contentIntent: PendingIntent? = null): Notification =
        NotificationCompat.Builder(context, CHANNEL_EXECUTING)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("所有操作已处理")
            .setContentText("安心休息/早安 ☀️")
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setExtras(android.os.Bundle().apply { putLong(EXTRA_HISTORY_ID, historyId) })
            .build()

    // -- Posting --------------------------------------------------------------

    /**
     * Posts an independent guided-action hint. Used by [SystemActions] when a
     * Guided_Action_Switch (AirplaneMode / WiFi / MobileData / Location / NFC)
     * needs the user to tap a toggle in the just-opened settings page.
     *
     * No-op when [SwitchAction.NONE] or POST_NOTIFICATIONS is not granted on
     * API 33+ (Requirement 23.x — denied permissions degrade gracefully).
     */
    fun postGuidedHint(item: SwitchItem, action: SwitchAction) {
        if (action == SwitchAction.NONE) return
        if (!canPostNotifications()) return
        val verb = when (action) {
            SwitchAction.TURN_ON -> "开启"
            SwitchAction.TURN_OFF -> "关闭"
            SwitchAction.NONE -> return
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_GUIDED_HINT)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("需要手动操作")
            .setContentText("请在设置页中$verb${labelFor(item)}")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        safeNotify(NOTIF_GUIDED_BASE + item.ordinal, notification)
    }

    /** Posts the executing/progress notification. No-op without POST_NOTIFICATIONS. */
    fun postExecuting(contentIntent: PendingIntent? = null) {
        if (!canPostNotifications()) return
        safeNotify(NOTIF_ID_EXECUTING, buildExecuting(contentIntent))
    }

    /** Posts the final summary notification. No-op without POST_NOTIFICATIONS. */
    fun postSummary(historyId: Long, contentIntent: PendingIntent? = null) {
        if (!canPostNotifications()) return
        safeNotify(NOTIF_ID_SUMMARY, buildSummary(historyId, contentIntent))
    }

    /** 倒计时结束、有 GUIDED 项延后到解锁后执行时显示的通知。 */
    fun postPendingUnlock(deferredCount: Int) {
        if (!canPostNotifications()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_GUIDED_HINT)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("部分操作将在解锁后自动完成")
            .setContentText("还剩 $deferredCount 项需要你解锁手机后才能继续")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        safeNotify(NOTIF_ID_PENDING_UNLOCK, notification)
    }

    fun cancelPendingUnlock() {
        manager.cancel(NOTIF_ID_PENDING_UNLOCK)
    }

    /** 解锁后执行完毕的汇总通知。 */
    fun postUnlockSummary(
        historyId: Long,
        autoDone: Int,
        manual: Int,
        contentIntent: PendingIntent? = null
    ) {
        cancelPendingUnlock()
        if (!canPostNotifications()) return
        val title = if (manual == 0) "已自动完成全部操作" else "$autoDone 项已自动完成"
        val text = if (manual == 0) "晚安 🌙"
        else "另有 $manual 项需要你在系统设置中手动操作"
        val notification = NotificationCompat.Builder(context, CHANNEL_EXECUTING)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setExtras(android.os.Bundle().apply { putLong(EXTRA_HISTORY_ID, historyId) })
            .build()
        safeNotify(NOTIF_ID_SUMMARY, notification)
    }

    fun cancelCountdown() {
        manager.cancel(NOTIF_ID_COUNTDOWN)
    }

    fun cancelExecuting() {
        manager.cancel(NOTIF_ID_EXECUTING)
    }

    fun cancelGuidedHint(item: SwitchItem) {
        manager.cancel(NOTIF_GUIDED_BASE + item.ordinal)
    }

    /**
     * Resolved permission state for posting notifications. Backed by
     * [PermissionMatrix.notificationsPermission] so the rule lives in one place
     * and is reusable from [com.example.sleepready.permission.PermissionManager].
     */
    fun canPostNotifications(): Boolean {
        return when (val req = PermissionMatrix.notificationsPermission()) {
            PermissionRequirement.None -> true
            is PermissionRequirement.Runtime ->
                ContextCompat.checkSelfPermission(context, req.permission) ==
                    PackageManager.PERMISSION_GRANTED
            else -> true
        }
    }

    /**
     * Wraps [NotificationManagerCompat.notify] with a SecurityException guard.
     * On API 33+ NotificationManagerCompat.notify is annotated with
     * `@RequiresPermission(POST_NOTIFICATIONS)`; we already short-circuit via
     * [canPostNotifications], but devices that revoke the permission between
     * checks could still throw — handle gracefully.
     */
    private fun safeNotify(id: Int, notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) return
            }
            manager.notify(id, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS revoked between check and call — degrade silently.
        }
    }

    companion object {
        const val CHANNEL_COUNTDOWN = "channel_countdown"
        const val CHANNEL_EXECUTING = "channel_executing"
        const val CHANNEL_GUIDED_HINT = "channel_guided_hint"

        const val NOTIF_ID_COUNTDOWN = 1001
        const val NOTIF_ID_EXECUTING = 1002
        const val NOTIF_ID_SUMMARY = 1003
        const val NOTIF_ID_PENDING_UNLOCK = 1004
        const val NOTIF_GUIDED_BASE = 2000

        const val EXTRA_HISTORY_ID = "com.example.sleepready.notif.HISTORY_ID"
    }
}

private fun labelFor(item: SwitchItem): String = when (item) {
    SwitchItem.DO_NOT_DISTURB -> "勿扰模式"
    SwitchItem.MUTE -> "静音模式"
    SwitchItem.VIBRATE -> "振动模式"
    SwitchItem.AIRPLANE_MODE -> "飞行模式"
    SwitchItem.WIFI -> "Wi-Fi"
    SwitchItem.BLUETOOTH -> "蓝牙"
    SwitchItem.MOBILE_DATA -> "移动数据"
    SwitchItem.LOCATION -> "定位"
    SwitchItem.FLASHLIGHT -> "手电筒"
    SwitchItem.SCREEN_RECORDING -> "屏幕录制"
    SwitchItem.NFC -> "NFC"
    SwitchItem.POWER_SAVE -> "省电模式"
    SwitchItem.EYE_COMFORT -> "护眼模式"
}
