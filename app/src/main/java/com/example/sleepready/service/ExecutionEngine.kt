package com.example.sleepready.service

import android.app.KeyguardManager
import android.content.Context
import com.example.sleepready.domain.model.ActionPlan
import com.example.sleepready.domain.model.AutomationStrategy
import com.example.sleepready.domain.model.ExecutionEntry
import com.example.sleepready.domain.model.ExecutionResult
import com.example.sleepready.domain.model.SwitchAction
import com.example.sleepready.domain.model.SwitchItem
import kotlinx.coroutines.delay

/**
 * 顺序执行 [ActionPlan]，按 [SwitchItem.executionOrder] 排序。
 *
 * 分层执行（v2）：
 *  - [SwitchItem.Tier.SILENT] 类（DnD/Mute/Vibrate/PowerSave/EyeComfort/Flashlight/ScreenRecording）：
 *    一定执行（除非 strategy=DEFER_ALL，则全部 DEFERRED）。
 *  - [SwitchItem.Tier.GUIDED] 类（Wi‑Fi/蓝牙/飞行/移动数据/定位/NFC）：
 *    根据 [strategy] 和当前 keyguard 状态决定立即执行还是 DEFERRED。
 *
 * 返回的 [ExecutionEntry] 列表中：
 *  - 已执行的项 → SUCCESS / SKIPPED / PERMISSION_DENIED / REQUIRES_USER_ACTION / FAILED
 *  - 延后执行的项 → DEFERRED（消息为「等待解锁后执行」）
 *
 * Properties：
 *  - P7 — 输出顺序仍等于 executionOrder
 *  - P8 — NONE 仍产生 SKIPPED
 *  - P9 — 两个非 NONE 的项之间有 ≥ 300ms 间隔
 *  - P10 — 单项失败不影响其他项
 */
class ExecutionEngine(
    private val systemActions: SystemActions,
    private val onStepProgress: ((processed: Int, current: SwitchItem) -> Unit)? = null
) {

    /**
     * @param plan 用户配置的计划
     * @param strategy 自动化策略
     * @param isLockedNow 当前 keyguard 是否锁定（service 层在调用前检查，避免引入 Context 依赖）
     */
    suspend fun run(
        plan: ActionPlan,
        strategy: AutomationStrategy = AutomationStrategy.LAYERED,
        isLockedNow: Boolean = false
    ): List<ExecutionEntry> {
        val normalized = plan.normalized()
        val results = ArrayList<ExecutionEntry>(SwitchItem.executionOrder.size)
        var firstNonNoneEmitted = false

        for ((index, item) in SwitchItem.executionOrder.withIndex()) {
            val action = normalized.actionFor(item)
            if (action == SwitchAction.NONE) {
                results += ExecutionEntry(item, SwitchAction.NONE, ExecutionResult.SKIPPED, null)
                continue
            }

            val shouldDefer = shouldDefer(item, strategy, isLockedNow)
            if (shouldDefer) {
                results += ExecutionEntry(
                    item, action, ExecutionResult.DEFERRED, "等待解锁后执行"
                )
                continue
            }

            // P9: 300ms gap BEFORE the next non-NONE item.
            if (firstNonNoneEmitted) delay(STEP_DELAY_MS)
            firstNonNoneEmitted = true

            val entry = try {
                applyOne(item, action)
            } catch (sec: SecurityException) {
                ExecutionEntry(item, action, ExecutionResult.PERMISSION_DENIED, sec.message)
            } catch (t: Throwable) {
                ExecutionEntry(item, action, ExecutionResult.FAILED, t.message ?: t::class.simpleName)
            }
            results += entry
            onStepProgress?.invoke(index + 1, item)
        }
        return results
    }

    /**
     * 仅执行 GUIDED 类的延后项（解锁后回放使用）。
     * 返回的 entries 列表里**只**包含 deferredItems 的执行结果（不含 NONE/SILENT 项）。
     */
    suspend fun runDeferred(
        deferredItems: Map<SwitchItem, SwitchAction>
    ): List<ExecutionEntry> {
        val results = ArrayList<ExecutionEntry>(deferredItems.size)
        var firstEmitted = false
        // 仍按 executionOrder 输出，保证顺序稳定
        for (item in SwitchItem.executionOrder) {
            val action = deferredItems[item] ?: continue
            if (action == SwitchAction.NONE) continue
            if (firstEmitted) delay(STEP_DELAY_MS)
            firstEmitted = true
            val entry = try {
                applyOne(item, action)
            } catch (sec: SecurityException) {
                ExecutionEntry(item, action, ExecutionResult.PERMISSION_DENIED, sec.message)
            } catch (t: Throwable) {
                ExecutionEntry(item, action, ExecutionResult.FAILED, t.message ?: t::class.simpleName)
            }
            results += entry
        }
        return results
    }

    private fun applyOne(item: SwitchItem, action: SwitchAction): ExecutionEntry = when (item) {
        SwitchItem.DO_NOT_DISTURB   -> systemActions.applyDnd(action)
        SwitchItem.MUTE             -> systemActions.applyMute(action)
        SwitchItem.VIBRATE          -> systemActions.applyVibrate(action)
        SwitchItem.BLUETOOTH        -> systemActions.applyBluetooth(action)
        SwitchItem.FLASHLIGHT       -> systemActions.applyFlashlight(action)
        SwitchItem.SCREEN_RECORDING -> systemActions.applyScreenRecording(action)
        SwitchItem.POWER_SAVE       -> systemActions.applyPowerSave(action)
        SwitchItem.EYE_COMFORT      -> systemActions.applyEyeComfort(action)
        SwitchItem.AIRPLANE_MODE,
        SwitchItem.WIFI,
        SwitchItem.MOBILE_DATA,
        SwitchItem.LOCATION,
        SwitchItem.NFC              -> systemActions.openSettingsFor(item, action)
    }

    private fun shouldDefer(
        item: SwitchItem,
        strategy: AutomationStrategy,
        isLockedNow: Boolean
    ): Boolean = when (strategy) {
        AutomationStrategy.DEFER_ALL -> true
        AutomationStrategy.LAYERED ->
            item.tier == SwitchItem.Tier.GUIDED && isLockedNow
    }

    companion object {
        const val STEP_DELAY_MS: Long = 300L

        /** 工具：从 Context 判断当前是否锁屏（API 22+ isDeviceLocked，更准确）。 */
        fun isDeviceLocked(context: Context): Boolean {
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                ?: return false
            return km.isDeviceLocked
        }
    }
}
