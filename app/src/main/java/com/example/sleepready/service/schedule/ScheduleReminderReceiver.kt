package com.example.sleepready.service.schedule

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.sleepready.di.appContainer
import com.example.sleepready.permission.DegradedReminder
import kotlinx.coroutines.runBlocking

/**
 * 收到 [ScheduleReminderScheduler] 安排的 alarm 后分流。
 *
 * v9 双保险：
 *  - 优先从 extras 读取 scheduleId / title
 *  - 如果 extras 丢失（FLAG_IMMUTABLE 冻结 bug），从 data URI 解析 scheduleId，
 *    并从数据库补全 title（同步读取，BroadcastReceiver 生命周期内完成）
 */
class ScheduleReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REMIND = "com.example.sleepready.schedule.REMIND"
        const val EXTRA_SCHEDULE_ID = "schedule_id"
        const val EXTRA_OFFSET_INDEX = "offset_index"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TARGET_MS = "target_ms"
        const val EXTRA_OFFSET_MS = "offset_ms"
        const val EXTRA_USE_ALARM_RING = "use_alarm_ring"

        const val CHANNEL_ID = "channel_schedule_reminder_v2"
        private const val TAG = "ScheduleReminder"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive action=${intent.action} data=${intent.data}")
        if (intent.action != ACTION_REMIND) return

        var scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
        val offsetIndex = intent.getIntExtra(EXTRA_OFFSET_INDEX, 0)
        var title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val offsetMs = intent.getLongExtra(EXTRA_OFFSET_MS, 0L)
        var useAlarmRing = intent.getBooleanExtra(EXTRA_USE_ALARM_RING, false)

        // ★ 双保险：extras 丢失时从 data URI 和数据库恢复
        if (scheduleId < 0 || title.isBlank()) {
            val recovered = recoverFromDataUri(context, intent)
            if (recovered != null) {
                if (scheduleId < 0) scheduleId = recovered.first
                if (title.isBlank()) title = recovered.second
                useAlarmRing = recovered.third
                Log.w(TAG, "recovered from data URI: id=$scheduleId title='$title' alarmRing=$useAlarmRing")
            }
        }

        Log.d(TAG, "id=$scheduleId offset=$offsetIndex title='$title' alarmRing=$useAlarmRing offsetMs=$offsetMs")
        if (scheduleId < 0 || title.isBlank()) {
            Log.e(TAG, "FATAL: cannot recover schedule info, giving up")
            return
        }

        if (useAlarmRing) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val hasFsPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.USE_FULL_SCREEN_INTENT
                ) == PackageManager.PERMISSION_GRANTED
                if (hasFsPermission) {
                    try {
                        AlarmRingService.start(context, scheduleId, title)
                    } catch (e: Exception) {
                        Log.e(TAG, "AlarmRingService start failed, fallback", e)
                        triggerAlarmFallback(context, scheduleId, title)
                    }
                } else {
                    Log.w(TAG, "USE_FULL_SCREEN_INTENT not granted, fallback alarm ring")
                    triggerAlarmFallback(context, scheduleId, title)
                }
            } else {
                try {
                    AlarmRingService.start(context, scheduleId, title)
                } catch (e: Exception) {
                    Log.e(TAG, "AlarmRingService start failed (pre-14), fallback", e)
                    triggerAlarmFallback(context, scheduleId, title)
                }
            }
            return
        }

        ensureChannel(context)
        if (!canPostNotifications(context)) {
            Log.w(TAG, "POST_NOTIFICATIONS not granted, degraded reminder")
            DegradedReminder.trigger(context, title)
            return
        }

        val leadText = leadTextFor(offsetMs)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("印记提醒：$title")
            .setContentText(leadText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_SOUND)
            .setVibrate(longArrayOf(0, 400, 200, 400, 200, 400))
            .build()
        try {
            NotificationManagerCompat.from(context)
                .notify((scheduleId * 10 + offsetIndex).toInt(), notification)
            Log.d(TAG, "notification posted for $scheduleId/$offsetIndex")
        } catch (sec: SecurityException) {
            Log.e(TAG, "SecurityException on notify", sec)
        }
    }

    private fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "印记提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "在印记目标时间前发出的提醒通知（震动 + 通知栏 head-up）"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 400)
            enableLights(true)
            setBypassDnd(true)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (sound != null) {
                setSound(
                    sound,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
        }
        nm.createNotificationChannel(channel)
        Log.d(TAG, "channel created: $CHANNEL_ID")
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 闹钟强提醒回退路径：先尝试发高优先级通知，失败则降级为振动+Toast。
     */
    private fun triggerAlarmFallback(context: Context, scheduleId: Long, title: String) {
        // 优先走通知（带声音+振动），如果通知权限也没有就走纯振动
        if (canPostNotifications(context)) {
            DegradedReminder.triggerAlarmRingFallback(context, scheduleId, title)
        } else {
            Log.w(TAG, "alarm fallback: no notification permission, vibration only")
            DegradedReminder.triggerVibration(context)
            DegradedReminder.triggerToast(context, title)
        }
    }

    /**
     * 双保险：从 data URI ("sleepready://schedule/{id}/{idx}") 解析 scheduleId，
     * 并从数据库补全 title 和 useAlarmRing。
     * 使用 runBlocking 在主线程等待 DB 查询（PK 查询极快，< 50ms）。
     */
    private fun recoverFromDataUri(context: Context, intent: Intent): Triple<Long, String, Boolean>? {
        val data = intent.data ?: return null
        val path = data.path ?: return null
        // path = "/schedule/123/0"
        val parts = path.trimStart('/').split("/")
        if (parts.size < 2 || parts[0] != "schedule") return null
        val id = parts[1].toLongOrNull() ?: return null

        return try {
            runBlocking {
                val schedule = context.appContainer.scheduleRepo.getById(id)
                if (schedule != null) {
                    Triple(schedule.id, schedule.title, schedule.useAlarmRing)
                } else {
                    Log.w(TAG, "recoverFromDataUri: schedule $id not found in DB")
                    null
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "recoverFromDataUri failed", t)
            null
        }
    }

    private fun leadTextFor(offsetMs: Long): String = when {
        offsetMs >= 24L * 60 * 60 * 1000 * 3 -> "还有 3 天"
        offsetMs >= 24L * 60 * 60 * 1000 -> "还有 1 天"
        offsetMs >= 60L * 60 * 1000 -> "还有 ${offsetMs / (60 * 60 * 1000)} 小时"
        offsetMs > 0 -> "即将到来"
        else -> "时间到了"
    }
}
