package com.example.sleepready.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.sleepready.di.appContainer
import com.example.sleepready.domain.model.ExecutionEntry
import com.example.sleepready.domain.model.ExecutionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 监听 [Intent.ACTION_USER_PRESENT]（用户解锁）：如果 prefs.pendingGuidedPlan 不空，
 * 立即用 [ExecutionEngine.runDeferred] 回放 GUIDED 项，结果合并进 prefs.pendingHistoryId
 * 对应的历史记录，再发汇总通知。
 *
 * 注：BroadcastReceiver 的 onReceive 主线程 + 10s 时限。这里调 goAsync 拿到 PendingResult
 * 让协程在后台完成。
 */
class UnlockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_USER_PRESENT) return
        val container = context.applicationContext.appContainer
        val prefs = container.prefs
        val pending = prefs.pendingGuidedPlan
        if (pending.isEmpty()) return

        val pendingHistoryId = prefs.pendingHistoryId
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val mode = prefs.guidedExecutionMode
                val replayEntries: List<ExecutionEntry> = when (mode) {
                    com.example.sleepready.domain.model.GuidedExecutionMode.MACRO_REPLAY -> {
                        com.example.sleepready.service.macro.MacroPlayer(
                            macroRepo = container.macroRepo,
                            systemActions = container.systemActions
                        ).runDeferred(pending)
                    }
                    com.example.sleepready.domain.model.GuidedExecutionMode.SETTINGS_INTENT -> {
                        ExecutionEngine(systemActions = container.systemActions).runDeferred(pending)
                    }
                }

                // 把回放结果合并回历史记录：把原来 DEFERRED 行替换为新结果
                if (pendingHistoryId > 0L) {
                    val existing = container.historyRepo.getById(pendingHistoryId)
                    if (existing != null) {
                        val replayMap = replayEntries.associateBy { it.switchItem }
                        val merged = existing.entries.map { entry ->
                            if (entry.result == ExecutionResult.DEFERRED) {
                                replayMap[entry.switchItem] ?: entry
                            } else entry
                        }
                        container.historyRepo.replaceEntries(
                            id = pendingHistoryId,
                            entries = merged,
                            finishedAt = System.currentTimeMillis()
                        )
                    }
                }

                val successCount = replayEntries.count {
                    it.result == ExecutionResult.SUCCESS
                }
                val manualCount = replayEntries.count {
                    it.result == ExecutionResult.REQUIRES_USER_ACTION ||
                        it.result == ExecutionResult.PERMISSION_DENIED ||
                        it.result == ExecutionResult.FAILED
                }
                container.timerNotifications.postUnlockSummary(
                    historyId = pendingHistoryId,
                    autoDone = successCount,
                    manual = manualCount
                )

                // 清状态
                prefs.clearPendingGuidedPlan()
                prefs.pendingHistoryId = 0L
            } finally {
                pendingResult.finish()
            }
        }
    }
}
