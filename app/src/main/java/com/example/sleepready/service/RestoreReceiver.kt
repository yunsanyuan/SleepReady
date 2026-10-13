package com.example.sleepready.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.sleepready.di.appContainer
import com.example.sleepready.domain.model.ActionPlan
import com.example.sleepready.domain.model.SwitchAction
import com.example.sleepready.domain.model.SwitchItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 起床后自动恢复广播。由 [RestoreScheduler] 用 AlarmManager 安排到指定时刻触发。
 * 收到后：
 *   1. 从 prefs.autoRestorePlan 取出反向 ActionPlan
 *   2. 用 ExecutionEngine.run(reversePlan, strategy, locked) 跑一次恢复
 *   3. 清空 prefs.autoRestorePlan
 *
 * 设计上不写入新的 history 记录（恢复不算一次完整执行），仅依赖 ExecutionEngine
 * 的常规失败处理与通知子系统。
 */
class RestoreReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_AUTO_RESTORE) return
        val container = context.applicationContext.appContainer
        val prefs = container.prefs
        val plan = prefs.autoRestorePlan
        if (plan.isEmpty()) return

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val ap = ActionPlan(plan as Map<SwitchItem, SwitchAction>)
                val strategy = prefs.automationStrategy
                val locked = ExecutionEngine.isDeviceLocked(context)
                ExecutionEngine(
                    systemActions = container.systemActions
                ).run(ap, strategy, locked)
                prefs.clearAutoRestorePlan()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_AUTO_RESTORE = "com.example.sleepready.timer.AUTO_RESTORE"
        const val REQUEST_CODE = 1100
    }
}

/**
 * 自动恢复调度器。计算下一次「指定时刻」的 epoch 毫秒，安排 AlarmManager
 * 的 setExactAndAllowWhileIdle 指向 [RestoreReceiver]。
 */
object RestoreScheduler {

    /**
     * 安排恢复任务。
     *
     * @param hour 目标小时（0..23）
     * @param minute 目标分钟（0..59）
     * @param reversePlan 反向 ActionPlan：被改变项 → 反向动作
     */
    fun schedule(
        context: Context,
        hour: Int,
        minute: Int,
        reversePlan: Map<SwitchItem, SwitchAction>
    ) {
        val prefs = context.applicationContext.appContainer.prefs
        prefs.autoRestorePlan = reversePlan
        val triggerAt = nextTriggerMillis(hour, minute)
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = buildPendingIntent(context)
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } catch (_: SecurityException) {
            // SCHEDULE_EXACT_ALARM 未授时静默退化，恢复不会触发。
        }
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(buildPendingIntent(context))
        context.applicationContext.appContainer.prefs.clearAutoRestorePlan()
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, RestoreReceiver::class.java)
            .setAction(RestoreReceiver.ACTION_AUTO_RESTORE)
        return PendingIntent.getBroadcast(
            context,
            RestoreReceiver.REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** 计算「下一次 hour:minute」的绝对时间戳（如果今天该时刻已过，则推到明天）。 */
    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }
}
