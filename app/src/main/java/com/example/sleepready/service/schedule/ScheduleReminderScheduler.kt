package com.example.sleepready.service.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.sleepready.domain.model.Schedule

/**
 * 安排 / 取消单个印记的提醒。
 *
 * v9 修复（核心 Bug：PendingIntent FLAG_IMMUTABLE 导致 extras 冻结）：
 *  取消路径构建 Pi 时不带 extras → FLAG_IMMUTABLE 冻结 → 后续 schedule 无法
 *  写入 extras → Receiver 收到空 extras → scheduleId=-1 → 静默 return。
 *  修复：始终写入所有 extras（取消时用默认值），确保 Pi 的 extras 结构一致。
 *
 *  1. 提醒偏移过期 → 保底 target 时间 offset=0 准时提醒。
 *  2. API 31+ 精确权限 → setExactAndAllowWhileIdle；无权限降级 setAndAllowWhileIdle。
 *     API < 31 → setAlarmClock（Doze 穿透力最强）。
 *  3. Receiver 双保险：data URI 回退解析 scheduleId。
 */
object ScheduleReminderScheduler {

    private const val TAG = "ScheduleReminder"

    /**
     * 安排提醒。注意：不在开头调用 cancel()，因为：
     *  - setExactAndAllowWhileIdle 会自动覆盖同 PendingIntent 的旧闹钟
     *  - 提前 cancel 会创建 FLAG_IMMUTABLE Pi（空 extras）→ 冻结 → 后续 schedule 无法写入正确数据
     */
    fun schedule(context: Context, schedule: Schedule) {
        val target = schedule.targetTimeMillis ?: return
        val now = System.currentTimeMillis()
        if (target <= now) {
            Log.w(TAG, "schedule(${schedule.id}): target=$target <= now=$now, skip")
            return
        }
        val am = context.getSystemService(AlarmManager::class.java) ?: run {
            Log.w(TAG, "schedule(${schedule.id}): AlarmManager null"); return
        }

        val offsetsMin = schedule.reminderOffsetsMinutes
        var registeredAny = false
        var count = 0
        offsetsMin.forEachIndexed { idx, minutes ->
            if (idx >= Schedule.MAX_REMINDERS_PER_SCHEDULE - 1) return@forEachIndexed
            val offsetMs = minutes * 60_000L
            val triggerAt = target - offsetMs
            if (triggerAt <= now) return@forEachIndexed
            registerOne(context, am, schedule, idx, triggerAt, offsetMs)
            registeredAny = true
            count++
        }

        if (!registeredAny) {
            // 保底：在 target 时间点准时响一次
            registerOne(
                context, am, schedule,
                offsetIndex = Schedule.MAX_REMINDERS_PER_SCHEDULE - 1,
                triggerAt = target,
                offsetMs = 0L
            )
            count++
        }

        val diffSec = (target - now) / 1000
        Log.i(TAG, "✓ scheduled id=${schedule.id} title='${schedule.title}' " +
                "target=$target (${diffSec}s later) alarms=$count alarmRing=${schedule.useAlarmRing} " +
                "offsets=$offsetsMin")
    }

    fun cancel(context: Context, scheduleId: Long) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        repeat(Schedule.MAX_REMINDERS_PER_SCHEDULE) { idx ->
            // 用 FLAG_NO_CREATE 避免创建新 Pi（防止空 extras 的 Pi 污染缓存）
            val intent = Intent(context, ScheduleReminderReceiver::class.java).apply {
                action = ScheduleReminderReceiver.ACTION_REMIND
                data = android.net.Uri.parse("sleepready://schedule/$scheduleId/$idx")
            }
            val requestCode = (scheduleId * Schedule.MAX_REMINDERS_PER_SCHEDULE + idx).toInt()
            val flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            val pi = PendingIntent.getBroadcast(context, requestCode, intent, flags)
            if (pi != null) {
                am.cancel(pi)
                pi.cancel()
            }
        }
        Log.d(TAG, "cancel($scheduleId)")
    }

    private fun registerOne(
        context: Context,
        am: AlarmManager,
        schedule: Schedule,
        offsetIndex: Int,
        triggerAt: Long,
        offsetMs: Long
    ) {
        val pi = buildPendingIntent(
            context = context,
            scheduleId = schedule.id,
            offsetIndex = offsetIndex,
            forCancel = false,
            title = schedule.title,
            targetMs = schedule.targetTimeMillis ?: 0L,
            offsetMs = offsetMs,
            useAlarmRing = schedule.useAlarmRing
        )

        // Android 12+：检测精确闹钟权限
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try { am.canScheduleExactAlarms() } catch (_: Throwable) { false }
        } else true

        try {
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                Log.d(TAG, "✓ exact id=${schedule.id} idx=$offsetIndex trigger=$triggerAt offset=$offsetMs")
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                // API < 31：没有精确闹钟权限概念，setAlarmClock 是最高优先级方式
                val info = try {
                    val showIntent = Intent(context, ScheduleReminderReceiver::class.java).apply {
                        action = ScheduleReminderReceiver.ACTION_REMIND
                    }
                    AlarmManager.AlarmClockInfo(
                        triggerAt,
                        PendingIntent.getBroadcast(context, 0, showIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    )
                } catch (_: Throwable) { null }

                if (info != null) {
                    am.setAlarmClock(info, pi)
                    Log.d(TAG, "○ alarm-clock id=${schedule.id} idx=$offsetIndex trigger=$triggerAt")
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                    Log.w(TAG, "△ inexact id=${schedule.id} idx=$offsetIndex trigger=$triggerAt")
                }
            } else {
                // API 31+ 无精确权限：降级为 setAndAllowWhileIdle（延迟最多 15 分钟）
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                Log.w(TAG, "△ inexact id=${schedule.id} idx=$offsetIndex trigger=$triggerAt (无精确权限，可能延迟)")
            }
        } catch (sec: SecurityException) {
            Log.e(TAG, "SecurityException scheduling id=${schedule.id}/$offsetIndex", sec)
            try {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } catch (_: Throwable) {}
        }
    }

    private fun buildPendingIntent(
        context: Context,
        scheduleId: Long,
        offsetIndex: Int,
        forCancel: Boolean,
        title: String = "",
        targetMs: Long = 0L,
        offsetMs: Long = 0L,
        useAlarmRing: Boolean = false
    ): PendingIntent {
        val intent = Intent(context, ScheduleReminderReceiver::class.java).apply {
            action = ScheduleReminderReceiver.ACTION_REMIND
            // 唯一 data URI 区分不同 (scheduleId, offsetIndex)
            data = android.net.Uri.parse("sleepready://schedule/$scheduleId/$offsetIndex")
            // ★ 始终写入所有 extras，避免 FLAG_IMMUTABLE 冻结空 extras
            putExtra(ScheduleReminderReceiver.EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(ScheduleReminderReceiver.EXTRA_OFFSET_INDEX, offsetIndex)
            putExtra(ScheduleReminderReceiver.EXTRA_TITLE, title)
            putExtra(ScheduleReminderReceiver.EXTRA_TARGET_MS, targetMs)
            putExtra(ScheduleReminderReceiver.EXTRA_OFFSET_MS, offsetMs)
            putExtra(ScheduleReminderReceiver.EXTRA_USE_ALARM_RING, useAlarmRing)
        }
        val requestCode = (scheduleId * Schedule.MAX_REMINDERS_PER_SCHEDULE + offsetIndex).toInt()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }
}
