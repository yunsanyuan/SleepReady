package com.example.sleepready.service.macro

import com.example.sleepready.domain.model.SwitchItem
import kotlinx.serialization.Serializable

/**
 * 宏录制系统的数据模型。
 *
 * [Macro] = 一个 SwitchItem（如 WIFI / BLUETOOTH）对应的一组 [Step]。
 * [Step] 描述一次点击：先打开 [openIntentAction]（系统设置页），再依次 click 节点。
 *
 * [NodeSignature] 用于在不同分辨率/语言下稳定 lookup 节点：优先 viewIdResourceName，
 * 其次 text + className，再次 contentDescription，最后降级到坐标。
 */
@Serializable
data class NodeSignature(
    val text: String? = null,
    val contentDescription: String? = null,
    val viewIdResourceName: String? = null,
    val className: String? = null,
    /** 录制时该节点中心点的屏幕坐标（fallback）。 */
    val centerX: Float = 0f,
    val centerY: Float = 0f
)

@Serializable
data class Step(
    val signature: NodeSignature,
    /** 在该步骤之后等待多久（毫秒），用于让 UI 切换稳定。 */
    val postDelayMs: Long = 200L
)

@Serializable
data class Macro(
    val switchItem: SwitchItem,
    /** 第一步要打开的 settings intent action（如 ACTION_WIFI_SETTINGS）。null = 已在前台。 */
    val openIntentAction: String? = null,
    val steps: List<Step> = emptyList(),
    val updatedAt: Long = 0L
) {
    fun isEmpty(): Boolean = steps.isEmpty()
}
