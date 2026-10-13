package com.example.sleepready.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class KnowledgeCategory(val storageValue: String) {
    WANT_TO_LEARN("want_to_learn"),
    WANT_TO_WATCH("want_to_watch"),
    WANT_TO_READ("want_to_read"),
    WANT_TO_DO("want_to_do");

    companion object {
        fun fromStorage(value: String): KnowledgeCategory =
            values().firstOrNull { it.storageValue == value }
                ?: error("Unknown KnowledgeCategory: $value")
    }
}

@Serializable
enum class KnowledgeStatus(val storageValue: String, val displayName: String) {
    IDLE("idle", "想去做"),
    PAUSED("paused", "在路上"),
    DONE("done", "已抵达");

    companion object {
        fun fromStorage(value: String): KnowledgeStatus =
            // Backwards compatibility: legacy rows used pending/in_progress/archived.
            when (value) {
                "idle", "pending" -> IDLE
                "paused", "in_progress" -> PAUSED
                "done", "archived" -> DONE
                else -> values().firstOrNull { it.storageValue == value } ?: IDLE
            }
    }
}

@Serializable
enum class SwitchItem {
    DO_NOT_DISTURB,
    MUTE,
    VIBRATE,
    AIRPLANE_MODE,
    WIFI,
    BLUETOOTH,
    MOBILE_DATA,
    LOCATION,
    FLASHLIGHT,
    SCREEN_RECORDING,
    NFC,
    POWER_SAVE,
    EYE_COMFORT;

    /**
     * 分层执行所属的「层级」。
     *
     *  - [Tier.SILENT]   A 类：可静默执行，不依赖前台界面（DnD / Mute / Vibrate /
     *                    PowerSave / EyeComfort / Flashlight API / ScreenRecording stop）。
     *  - [Tier.GUIDED]   B 类：需要在系统设置页中点击的开关（Wi‑Fi / 蓝牙 /
     *                    飞行模式 / 移动数据 / 定位 / NFC）。锁屏时延迟到 USER_PRESENT
     *                    后再执行。
     */
    val tier: Tier
        get() = when (this) {
            DO_NOT_DISTURB, MUTE, VIBRATE, FLASHLIGHT, SCREEN_RECORDING,
            POWER_SAVE, EYE_COMFORT -> Tier.SILENT
            BLUETOOTH, AIRPLANE_MODE, WIFI, MOBILE_DATA, LOCATION, NFC -> Tier.GUIDED
        }

    enum class Tier { SILENT, GUIDED }

    companion object {
        /**
         * Fixed execution order used by ExecutionEngine.
         * Validates Property P7: output sequence must equal this list.
         */
        val executionOrder: List<SwitchItem> = listOf(
            DO_NOT_DISTURB,
            MUTE,
            VIBRATE,
            AIRPLANE_MODE,
            WIFI,
            BLUETOOTH,
            MOBILE_DATA,
            LOCATION,
            FLASHLIGHT,
            SCREEN_RECORDING,
            NFC,
            POWER_SAVE,
            EYE_COMFORT
        )
    }
}

@Serializable
enum class SwitchAction { NONE, TURN_OFF, TURN_ON }

@Serializable
enum class ExecutionResult {
    SUCCESS,
    SKIPPED,
    PERMISSION_DENIED,
    REQUIRES_USER_ACTION,
    /** 已记录待执行；将在 USER_PRESENT 解锁时回放。 */
    DEFERRED,
    FAILED
}

@Serializable
enum class ThemeMode(val storageValue: String) {
    LIGHT("light"),
    DARK("dark"),
    SYSTEM("system");

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            values().firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}

/**
 * 配色方案。每套都有自己的浅色 + 深色双调；具体色值定义在
 * [com.example.sleepready.ui.theme.ColorPalettes]，按用户选择应用。
 *
 *  - [DAWN]   晨曦（默认）：浅紫 / 深靛蓝。柔和现代，长时间观看也不刺眼。
 *  - [FOREST] 林间：清新翠绿，配少量琥珀点缀。
 *  - [MIDNIGHT] 墨夜：典型夜间模式，墨蓝底 + 淡紫高亮，适合卧室。
 */
@Serializable
enum class ColorScheme(val storageValue: String, val displayName: String) {
    DAWN("dawn", "晨曦"),
    FOREST("forest", "林间"),
    MIDNIGHT("midnight", "墨夜");

    companion object {
        val DEFAULT: ColorScheme = DAWN
        fun fromStorage(value: String?): ColorScheme =
            values().firstOrNull { it.storageValue == value } ?: DEFAULT
    }
}

/**
 * 入梦倒计时到期后的自动化执行策略。
 *
 *  - [LAYERED]      默认：A 类（SILENT）立即静默执行；B 类（GUIDED）等到
 *                   USER_PRESENT 解锁瞬间再执行。
 *  - [DEFER_ALL]    全部延迟：到期后不立即执行任何开关，全部等到解锁瞬间统一执行。
 */
@Serializable
enum class AutomationStrategy(val storageValue: String, val displayName: String) {
    LAYERED("layered", "能静默的先执行，其余解锁后完成"),
    DEFER_ALL("defer_all", "全部等待解锁后统一执行");

    companion object {
        val DEFAULT: AutomationStrategy = LAYERED
        fun fromStorage(value: String?): AutomationStrategy =
            values().firstOrNull { it.storageValue == value } ?: DEFAULT
    }
}

/**
 * B 类（GUIDED）开关在解锁瞬间的执行模式。
 *
 *  - [SETTINGS_INTENT] 默认：跳转系统设置页 + 通知提醒（用户手动点）。
 *  - [MACRO_REPLAY]    使用「我录制的路径」：依赖无障碍服务回放用户录的宏。
 */
@Serializable
enum class GuidedExecutionMode(val storageValue: String, val displayName: String) {
    SETTINGS_INTENT("settings_intent", "跳转系统设置页"),
    MACRO_REPLAY("macro_replay", "使用我录制的路径");

    companion object {
        val DEFAULT: GuidedExecutionMode = SETTINGS_INTENT
        fun fromStorage(value: String?): GuidedExecutionMode =
            values().firstOrNull { it.storageValue == value } ?: DEFAULT
    }
}
