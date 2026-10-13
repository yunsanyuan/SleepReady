package com.example.sleepready.service.macro

import com.example.sleepready.domain.model.ExecutionEntry
import com.example.sleepready.domain.model.ExecutionResult
import com.example.sleepready.domain.model.SwitchAction
import com.example.sleepready.domain.model.SwitchItem
import com.example.sleepready.repository.MacroRepository
import com.example.sleepready.service.SystemActions
import kotlinx.coroutines.delay

/**
 * 解锁后用「我录制的宏」回放 GUIDED 类开关。
 *
 * v3：把固定 700ms delay 换成「轮询等待目标节点出现」。最长 [NODE_WAIT_MAX_MS]，
 * 出现就立刻 click，超时则降级到坐标点击或返回 FAILED。
 */
class MacroPlayer(
    private val macroRepo: MacroRepository,
    private val systemActions: SystemActions
) {

    suspend fun runDeferred(items: Map<SwitchItem, SwitchAction>): List<ExecutionEntry> {
        val out = ArrayList<ExecutionEntry>(items.size)
        for (item in SwitchItem.executionOrder) {
            val action = items[item] ?: continue
            if (action == SwitchAction.NONE) continue
            out += playOne(item, action)
        }
        return out
    }

    private suspend fun playOne(item: SwitchItem, action: SwitchAction): ExecutionEntry {
        val macro = macroRepo.getMacro(item)
        if (macro == null || macro.isEmpty()) {
            return systemActions.openSettingsFor(item, action)
        }
        val service = MacroAccessibilityService.instance
            ?: return systemActions.openSettingsFor(item, action)

        macro.openIntentAction?.let { systemActions.openSettingsActionRaw(it) }

        var allOk = true
        for ((idx, step) in macro.steps.withIndex()) {
            val ok = waitAndClick(service, step) ||
                service.clickByCoordinate(step.signature.centerX, step.signature.centerY)
            if (!ok) allOk = false
            if (idx < macro.steps.lastIndex) delay(step.postDelayMs.coerceAtLeast(50L))
        }

        return if (allOk) {
            ExecutionEntry(item, action, ExecutionResult.SUCCESS, "由宏自动完成")
        } else {
            ExecutionEntry(
                item, action, ExecutionResult.FAILED,
                "宏回放失败：界面元素未找到，建议重新录制"
            )
        }
    }

    /**
     * 轮询等待目标节点出现并点击。最长等 [NODE_WAIT_MAX_MS]，每 [POLL_INTERVAL_MS] 试一次。
     */
    private suspend fun waitAndClick(
        service: MacroAccessibilityService,
        step: Step
    ): Boolean {
        val deadline = System.currentTimeMillis() + NODE_WAIT_MAX_MS
        while (System.currentTimeMillis() < deadline) {
            if (service.clickBySignature(step.signature)) return true
            delay(POLL_INTERVAL_MS)
        }
        return false
    }

    companion object {
        const val NODE_WAIT_MAX_MS: Long = 3000L
        const val POLL_INTERVAL_MS: Long = 150L
    }
}
