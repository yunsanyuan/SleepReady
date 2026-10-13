package com.example.sleepready.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import com.example.sleepready.di.appContainer
import com.example.sleepready.domain.model.ActionPlan
import com.example.sleepready.domain.model.ExecutionHistory
import com.example.sleepready.domain.model.SwitchItem
import com.example.sleepready.service.notif.TimerNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Foreground [Service] that owns the bedtime countdown lifecycle and, on timeout,
 * delegates to the [ExecutionEngine] to apply the saved [ActionPlan].
 *
 * Action handling (Requirements 9.3 / 11.2 / 11.3 / 11.4 / 12.x / 13.x / 14.1 / 18.x / 19.x):
 *  - [ACTION_START]   — promote to foreground, load [com.example.sleepready.domain.model.TimerConfig],
 *                       start the 1-second ticker, and schedule a Doze-resilient
 *                       `AlarmManager.setExactAndAllowWhileIdle` to fire [ACTION_TIMEOUT].
 *  - [ACTION_CANCEL]  — cancel alarm + ticker, remove the countdown notification, and
 *                       reset [TimerStateHolder] to Idle. **No history row is written**
 *                       (Property P12).
 *  - [ACTION_EXTEND]  — adds 15 minutes to the running countdown unless that would
 *                       exceed the 120-minute cap (Property P11), in which case
 *                       [OneShotEvent.LimitReached] is emitted instead.
 *  - [ACTION_TIMEOUT] — runs [ExecutionEngine.run] on the saved plan, persists a single
 *                       [ExecutionHistory] row (Property P13), swaps the foreground
 *                       notification for the summary, then `stopForeground(DETACH)` so
 *                       the summary remains visible after the service stops.
 *
 * Double-trigger guard: [finished] is a CAS-protected [AtomicBoolean]; whichever of
 * the ticker or the AlarmManager broadcast wins gets to run [handleTimeout], the other
 * is a no-op (design.md "Edge Cases — 双触发").
 */
class SleepReadyTimerService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val finished = AtomicBoolean(false)

    // -- Mutable countdown state (only mutated on the main thread) ------------
    private var totalMs: Long = 0L
    private var endAtElapsedMs: Long = 0L
    private var startedAtWallClock: Long = 0L
    private var currentPlan: ActionPlan? = null
    private var startupJob: Job? = null
    private var executionJob: Job? = null
    private var foregroundStarted: Boolean = false

    // -- Lazy DI lookups (avoid touching AppContainer before onCreate) --------
    private val notifications by lazy { applicationContext.appContainer.timerNotifications }
    private val timerConfigRepo by lazy { applicationContext.appContainer.timerConfigRepo }
    private val timerPresetRepo by lazy { applicationContext.appContainer.timerPresetRepo }
    private val historyRepo by lazy { applicationContext.appContainer.historyRepo }
    private val systemActions by lazy { applicationContext.appContainer.systemActions }
    private val prefs by lazy { applicationContext.appContainer.prefs }

    /**
     * Ticker pulse: every [TICK_INTERVAL_MS] re-derive `remainingMs` from the
     * monotonic [SystemClock.elapsedRealtime] base so we are immune to wall-clock
     * jumps (DST, manual time change, etc.).
     */
    private val tickRunnable = object : Runnable {
        override fun run() {
            val plan = currentPlan ?: return
            val remaining = (endAtElapsedMs - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            TimerStateHolder.update(TimerState.Running(totalMs, remaining, plan))
            updateCountdownNotification(remaining)
            if (remaining <= 0L) {
                // Same code path as the AlarmManager broadcast — CAS-guarded inside.
                handleTimeout()
            } else {
                handler.postDelayed(this, TICK_INTERVAL_MS)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START   -> handleStart()
            ACTION_CANCEL  -> handleCancel()
            ACTION_EXTEND  -> handleExtend()
            ACTION_TIMEOUT -> handleTimeout()
            else           -> handleStart()
        }
        return START_NOT_STICKY
    }

    // -- ACTION_START ---------------------------------------------------------

    private fun handleStart() {
        // Promote to foreground IMMEDIATELY (within Android's 5 s grace window) using
        // a 0-remaining placeholder; the tick that follows refreshes the body text.
        val placeholder = notifications.buildCountdown(
            remainingMs = 0L,
            cancelPendingIntent = buildCancelPendingIntent()
        )
        startForegroundCompat(placeholder)

        // Load the saved TimerConfig off the main thread; start the ticker once we have it.
        startupJob?.cancel()
        startupJob = scope.launch {
            val cfg = runCatching { timerConfigRepo.load() }.getOrNull()
            if (cfg == null) {
                TimerStateHolder.emit(OneShotEvent.StartFailed("尚未保存任何定时配置"))
                stopForegroundAndSelf(detach = false)
                return@launch
            }
            currentPlan = cfg.plan
            totalMs = cfg.durationMinutes * 60_000L
            endAtElapsedMs = SystemClock.elapsedRealtime() + totalMs
            startedAtWallClock = System.currentTimeMillis()
            finished.set(false)
            TimerStateHolder.update(TimerState.Running(totalMs, totalMs, cfg.plan))
            updateCountdownNotification(totalMs)
            scheduleAlarm(endAtElapsedMs)
            handler.removeCallbacks(tickRunnable)
            handler.postDelayed(tickRunnable, TICK_INTERVAL_MS)
        }
    }

    // -- ACTION_CANCEL --------------------------------------------------------

    private fun handleCancel() {
        // Property P12: cancel must NOT write to the history table.
        startupJob?.cancel()
        executionJob?.cancel()
        cancelAlarm()
        handler.removeCallbacks(tickRunnable)
        TimerStateHolder.reset()
        notifications.cancelCountdown()
        stopForegroundAndSelf(detach = false)
    }

    // -- ACTION_EXTEND --------------------------------------------------------

    private fun handleExtend() {
        val plan = currentPlan ?: return
        val newTotal = totalMs + EXTEND_STEP_MS
        if (newTotal > MAX_TOTAL_MS) {
            // Property P11 — never exceed 120 minutes; tell the UI but don't change state.
            TimerStateHolder.emit(OneShotEvent.LimitReached)
            return
        }
        val nowRemaining = (endAtElapsedMs - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        val newRemaining = nowRemaining + EXTEND_STEP_MS
        totalMs = newTotal
        endAtElapsedMs = SystemClock.elapsedRealtime() + newRemaining
        cancelAlarm()
        scheduleAlarm(endAtElapsedMs)
        TimerStateHolder.update(TimerState.Running(totalMs, newRemaining, plan))
        updateCountdownNotification(newRemaining)
    }

    // -- ACTION_TIMEOUT -------------------------------------------------------

    private fun handleTimeout() {
        // CAS guards against the simultaneous tick / alarm race (design.md "双触发").
        if (!finished.compareAndSet(false, true)) return
        cancelAlarm()
        handler.removeCallbacks(tickRunnable)

        val plan = currentPlan
        if (plan == null) {
            // Should not happen — handleStart populates currentPlan before scheduling
            // anything. Fail closed: stop the service without writing history.
            stopForegroundAndSelf(detach = false)
            return
        }

        val planSize = SwitchItem.executionOrder.size
        TimerStateHolder.update(TimerState.Executing(planSize = planSize, processed = 0))
        notifications.cancelCountdown()
        notifications.postExecuting()

        executionJob = scope.launch {
            // Service-local engine instance so we can push per-step progress into
            // TimerStateHolder via the onStepProgress callback (the AppContainer's
            // shared engine has no callback wired up).
            val engine = ExecutionEngine(
                systemActions = systemActions,
                onStepProgress = { processed, current ->
                    TimerStateHolder.update(
                        TimerState.Executing(
                            planSize = planSize,
                            processed = processed,
                            currentItem = current
                        )
                    )
                }
            )
            val strategy = prefs.automationStrategy
            val locked = ExecutionEngine.isDeviceLocked(applicationContext)
            val entries = engine.run(plan, strategy, locked)

            val historyId = historyRepo.insert(
                ExecutionHistory(
                    id = 0L,
                    startedAt = startedAtWallClock,
                    finishedAt = System.currentTimeMillis(),
                    durationMinutes = (totalMs / 60_000L).toInt(),
                    entries = entries
                )
            )

            // 把 DEFERRED 项暂存到 prefs，等 USER_PRESENT 解锁后由 UnlockReceiver 回放
            val deferred = entries
                .filter { it.result == com.example.sleepready.domain.model.ExecutionResult.DEFERRED }
                .associate { it.switchItem to it.action }
            if (deferred.isNotEmpty()) {
                prefs.pendingGuidedPlan = deferred
                prefs.pendingHistoryId = historyId
                notifications.postPendingUnlock(deferred.size)
            } else {
                prefs.clearPendingGuidedPlan()
                prefs.pendingHistoryId = 0L
                notifications.postSummary(historyId)
            }

            // v5：起床后自动恢复 — 如果当前 running preset 启用了 autoRestore，
            // 则把"被本方案改变的开关"反向编码为恢复 plan，安排到指定时刻触发。
            val runningPresetId = prefs.runningPresetId
            if (runningPresetId != 0L) {
                val preset = runCatching { timerPresetRepo.getById(runningPresetId) }.getOrNull()
                if (preset?.autoRestoreEnabled == true) {
                    val reverse = buildReversePlan(plan)
                    if (reverse.isNotEmpty()) {
                        RestoreScheduler.schedule(
                            applicationContext,
                            preset.autoRestoreHour,
                            preset.autoRestoreMinute,
                            reverse
                        )
                    }
                }
            }

            TimerStateHolder.update(TimerState.Finished(historyId))
            // DETACH so the summary notification stays visible after the service stops.
            stopForegroundAndSelf(detach = true)
        }
    }

    // -- Foreground / notification helpers -----------------------------------

    private fun startForegroundCompat(notification: Notification) {
        // FOREGROUND_SERVICE_TYPE_SPECIAL_USE is required as a manifest-declared type
        // since API 34; on older releases the 2-arg overload is sufficient.
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                TimerNotifications.NOTIF_ID_COUNTDOWN,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(TimerNotifications.NOTIF_ID_COUNTDOWN, notification)
        }
        foregroundStarted = true
    }

    private fun stopForegroundAndSelf(detach: Boolean) {
        if (foregroundStarted) {
            stopForeground(if (detach) STOP_FOREGROUND_DETACH else STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
        stopSelf()
    }

    private fun updateCountdownNotification(remainingMs: Long) {
        val n = notifications.buildCountdown(
            remainingMs = remainingMs,
            cancelPendingIntent = buildCancelPendingIntent()
        )
        try {
            // Use the same id we passed to startForeground so Android updates the
            // existing foreground notification instead of stacking a duplicate.
            getSystemService(NotificationManager::class.java)
                ?.notify(TimerNotifications.NOTIF_ID_COUNTDOWN, n)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS revoked between checks — degrade silently per Req 23.x.
        }
    }

    private fun buildCancelPendingIntent(): PendingIntent {
        val intent = Intent(this, SleepReadyTimerService::class.java).setAction(ACTION_CANCEL)
        return PendingIntent.getService(
            this,
            REQ_CANCEL,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // -- AlarmManager helpers -------------------------------------------------

    private fun scheduleAlarm(endAtElapsedMs: Long) {
        val am = getSystemService(AlarmManager::class.java) ?: return
        // AlarmManager wants a wall-clock trigger (RTC_WAKEUP); convert from our
        // elapsedRealtime base.
        val triggerWallClock = System.currentTimeMillis() +
            (endAtElapsedMs - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        try {
            am.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerWallClock,
                buildAlarmPendingIntent()
            )
        } catch (_: SecurityException) {
            // SCHEDULE_EXACT_ALARM may be revoked on API 31+ — the ticker is the fallback.
        }
    }

    private fun cancelAlarm() {
        val am = getSystemService(AlarmManager::class.java) ?: return
        am.cancel(buildAlarmPendingIntent())
    }

    private fun buildAlarmPendingIntent(): PendingIntent {
        val intent = Intent(this, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_TIMEOUT)
        return PendingIntent.getBroadcast(
            this,
            AlarmReceiver.ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onDestroy() {
        // Defensive cleanup so the service releases everything if it's killed
        // outside our normal cancel/timeout paths (e.g., system-initiated stop,
        // task swiped away, or `stopService` from a future caller).
        handler.removeCallbacksAndMessages(null)
        startupJob?.cancel()
        executionJob?.cancel()
        cancelAlarm()
        notifications.cancelCountdown()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START   = "com.example.sleepready.timer.START"
        const val ACTION_CANCEL  = "com.example.sleepready.timer.CANCEL"
        const val ACTION_EXTEND  = "com.example.sleepready.timer.EXTEND"
        /** Mirrors [AlarmReceiver.ACTION_TIMEOUT] so the receiver can forward to us. */
        const val ACTION_TIMEOUT = AlarmReceiver.ACTION_TIMEOUT

        private const val REQ_CANCEL = 200
        private const val TICK_INTERVAL_MS = 1_000L
        private const val EXTEND_STEP_MS = 15L * 60_000L
        private const val MAX_TOTAL_MS = 24L * 60 * 60_000L // 24 小时上限

        /**
         * 把入梦 plan 里的每一项反向：TURN_ON → TURN_OFF；TURN_OFF → TURN_ON；NONE → 跳过。
         * 用于 v5 的"起床后自动恢复"。
         */
        internal fun buildReversePlan(
            plan: ActionPlan
        ): Map<com.example.sleepready.domain.model.SwitchItem, com.example.sleepready.domain.model.SwitchAction> {
            val out = LinkedHashMap<com.example.sleepready.domain.model.SwitchItem, com.example.sleepready.domain.model.SwitchAction>()
            SwitchItem.executionOrder.forEach { item ->
                when (plan.actionFor(item)) {
                    com.example.sleepready.domain.model.SwitchAction.TURN_ON ->
                        out[item] = com.example.sleepready.domain.model.SwitchAction.TURN_OFF
                    com.example.sleepready.domain.model.SwitchAction.TURN_OFF ->
                        out[item] = com.example.sleepready.domain.model.SwitchAction.TURN_ON
                    com.example.sleepready.domain.model.SwitchAction.NONE -> {}
                }
            }
            return out
        }
    }
}
