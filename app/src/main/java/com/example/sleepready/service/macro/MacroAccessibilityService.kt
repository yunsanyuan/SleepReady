package com.example.sleepready.service.macro

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 宏录制 / 回放无障碍服务。
 *
 *  - 录制：在用户点击系统设置页时记录被点击节点的关键签名（text/contentDescription/
 *    viewIdResourceName/className），由 [com.example.sleepready.repository.MacroRepository]
 *    持久化。
 *  - 回放：解锁后由 [com.example.sleepready.service.UnlockReceiver] 调用，按宏脚本依次
 *    在当前可达的窗口里 lookup 目标节点并 performAction(CLICK)，节点不可达时降级到
 *    用录制时的中心坐标做 [GestureDescription] 模拟点击。
 *
 * 注意：本服务仅在「宏回放」窗口活跃；平时连接也只用于录制。在 [onUnbind] 时不再
 * 长期占用资源。所有具体录制逻辑由 [MacroPlayer]（同包）协调。
 */
class MacroAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 录制流程订阅这里的事件；当前不挂任何监听，等待 MacroRecorder/Player 安装回调。
    }

    override fun onInterrupt() { /* no-op */ }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        if (instance === this) instance = null
        return super.onUnbind(intent)
    }

    /**
     * 在当前窗口中查找匹配 [signature] 的节点；找到 → performAction(ACTION_CLICK)，返回 true。
     * 找不到 → 返回 false，由调用方决定是否降级到坐标点击。
     *
     * v5：国产 ROM 兼容降级 —— 当标准 viewId/text/contentDescription 都未命中时，
     * 遍历所有 Switch / ToggleButton / SwitchCompat 节点，按关键词模糊匹配它们的
     * text 或 contentDescription（[switchKeywordsFor]）。这样在小米/华为/OPPO/vivo
     * 等定制 ROM 上即使节点 id 完全不同，也能根据「Wi-Fi / WLAN / 无线 / 蓝牙 /
     * Bluetooth / 飞行 / Airplane / 移动数据 / Mobile / 定位 / Location / NFC」等
     * 通用关键词找到目标开关。
     */
    fun clickBySignature(signature: NodeSignature): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findNode(root, signature) ?: findToggleByKeywords(root, signature)
        return node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
    }

    /**
     * 用关键词列表 [keywords]（任一命中即可）尝试找当前页面里的开关类节点。
     * 主要给 [com.example.sleepready.service.SystemActions] 的 GUIDED 项打开
     * 设置页后做最后一搏；不依赖宏录制的 NodeSignature。
     */
    fun clickByKeywords(keywords: List<String>): Boolean {
        if (keywords.isEmpty()) return false
        val root = rootInActiveWindow ?: return false
        val node = findToggleByKeywordList(root, keywords) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    /** 用录制时的中心坐标做 [GestureDescription] 模拟点击（最后一招）。 */
    fun clickByCoordinate(x: Float, y: Float, durationMs: Long = 60L): Boolean {
        val path = Path().apply { moveTo(x, y); lineTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs.coerceAtLeast(1L))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    private fun findNode(
        root: AccessibilityNodeInfo,
        sig: NodeSignature
    ): AccessibilityNodeInfo? {
        // 1) 先按 viewIdResourceName 找（最稳定）
        sig.viewIdResourceName?.takeIf { it.isNotBlank() }?.let { id ->
            root.findAccessibilityNodeInfosByViewId(id).firstOrNull()?.let { return it }
        }
        // 2) 按 text 找
        sig.text?.takeIf { it.isNotBlank() }?.let { text ->
            root.findAccessibilityNodeInfosByText(text).firstOrNull { node ->
                sig.className == null || node.className == sig.className
            }?.let { return it }
        }
        // 3) 按 contentDescription 递归找
        sig.contentDescription?.takeIf { it.isNotBlank() }?.let { desc ->
            return findByContentDescription(root, desc)
        }
        return null
    }

    private fun findByContentDescription(
        node: AccessibilityNodeInfo,
        target: String
    ): AccessibilityNodeInfo? {
        if (node.contentDescription?.toString() == target) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findByContentDescription(child, target)?.let { return it }
        }
        return null
    }

    /**
     * v5 降级：根据 signature 的文字/描述推断关键词，
     * 然后在当前窗口里查找含这些关键词的 Switch/ToggleButton 节点。
     */
    private fun findToggleByKeywords(
        root: AccessibilityNodeInfo,
        sig: NodeSignature
    ): AccessibilityNodeInfo? {
        val hint = sig.text ?: sig.contentDescription ?: return null
        val keywords = switchKeywordsFor(hint)
        if (keywords.isEmpty()) return null
        return findToggleByKeywordList(root, keywords)
    }

    /**
     * 遍历整棵节点树寻找：
     *  - className 为 Switch / ToggleButton / SwitchCompat / CompoundButton 的可点击节点
     *  - 节点本身或其祖先 / 兄弟节点的 text 或 contentDescription 模糊命中任一关键词
     */
    private fun findToggleByKeywordList(
        root: AccessibilityNodeInfo,
        keywords: List<String>
    ): AccessibilityNodeInfo? {
        val candidates = mutableListOf<AccessibilityNodeInfo>()
        collectToggleNodes(root, candidates)
        if (candidates.isEmpty()) return null
        // 按关键词命中度优先排序：自身或最近的祖先 row 的 text/desc 含关键词的最优先
        return candidates.firstOrNull { node ->
            keywordMatchesNode(node, keywords)
        }
    }

    private fun collectToggleNodes(
        node: AccessibilityNodeInfo,
        out: MutableList<AccessibilityNodeInfo>
    ) {
        val cls = node.className?.toString().orEmpty()
        if (TOGGLE_CLASSES.any { cls.endsWith(it) || cls.contains(it) }) {
            out += node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectToggleNodes(child, out)
        }
    }

    private fun keywordMatchesNode(
        node: AccessibilityNodeInfo,
        keywords: List<String>
    ): Boolean {
        // 自身
        if (containsAnyKeyword(node.text?.toString(), keywords)) return true
        if (containsAnyKeyword(node.contentDescription?.toString(), keywords)) return true
        // 向上 3 层祖先（节点常被包在 ListItem / FrameLayout 里）
        var current: AccessibilityNodeInfo? = node.parent
        var depth = 0
        while (current != null && depth < 3) {
            if (containsAnyKeyword(current.text?.toString(), keywords)) return true
            if (containsAnyKeyword(current.contentDescription?.toString(), keywords)) return true
            // 也扫一下父节点的所有直系子节点（覆盖 row 中的 title TextView）
            for (i in 0 until current.childCount) {
                val sib = current.getChild(i) ?: continue
                if (containsAnyKeyword(sib.text?.toString(), keywords)) return true
                if (containsAnyKeyword(sib.contentDescription?.toString(), keywords)) return true
            }
            current = current.parent
            depth++
        }
        return false
    }

    private fun containsAnyKeyword(value: String?, keywords: List<String>): Boolean {
        if (value.isNullOrBlank()) return false
        return keywords.any { value.contains(it, ignoreCase = true) }
    }

    /**
     * 根据原 hint 推断这是哪个开关，返回该开关所有可能的关键词。
     */
    private fun switchKeywordsFor(hint: String): List<String> {
        val lower = hint.lowercase()
        return when {
            lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("wlan") ||
                hint.contains("无线") -> listOf("Wi-Fi", "WLAN", "WiFi", "无线")
            lower.contains("bluetooth") || hint.contains("蓝牙") -> listOf("蓝牙", "Bluetooth")
            lower.contains("airplane") || hint.contains("飞行") || hint.contains("飞航") ->
                listOf("飞行", "飞航", "Airplane")
            lower.contains("mobile") || lower.contains("data") ||
                hint.contains("移动数据") || hint.contains("流量") ||
                hint.contains("数据连接") -> listOf("移动数据", "流量", "数据连接", "Mobile data", "Cellular")
            lower.contains("location") || hint.contains("位置") || hint.contains("定位") ->
                listOf("定位", "位置", "Location")
            lower.contains("nfc") -> listOf("NFC")
            else -> emptyList()
        }
    }

    companion object {
        @Volatile
        var instance: MacroAccessibilityService? = null
            private set

        /** 节点类名后缀关键字，用于识别 Switch 类节点。 */
        private val TOGGLE_CLASSES = listOf(
            "Switch", "ToggleButton", "SwitchCompat", "SwitchMaterial",
            "CheckBox", "CompoundButton"
        )
    }
}
