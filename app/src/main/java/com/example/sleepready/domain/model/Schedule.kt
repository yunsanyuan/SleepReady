package com.example.sleepready.domain.model

import kotlinx.serialization.Serializable

/** 印记类型。日程 / 生日 / 纪念日 / 倒数日。 */
@Serializable
enum class ScheduleType(val storageValue: String, val displayName: String) {
    EVENT("event", "日程"),
    BIRTHDAY("birthday", "生日"),
    ANNIVERSARY("anniversary", "纪念日"),
    COUNTDOWN("countdown", "倒数日");

    companion object {
        fun fromStorage(value: String): ScheduleType =
            values().firstOrNull { it.storageValue == value } ?: EVENT
    }
}

/** 重复规则。 */
@Serializable
enum class RepeatRule(val storageValue: String, val displayName: String) {
    NEVER("never", "不重复"),
    DAILY("daily", "每天"),
    WEEKDAYS("weekdays", "周一至周五"),
    WEEKLY("weekly", "每周"),
    MONTHLY("monthly", "每月"),
    YEARLY("yearly", "每年"),
    CUSTOM("custom", "自定义");

    companion object {
        fun fromStorage(value: String): RepeatRule =
            values().firstOrNull { it.storageValue == value } ?: NEVER
    }
}

/** 自定义重复单位。 */
@Serializable
enum class CustomRepeatUnit(val storageValue: String, val displayName: String) {
    DAY("day", "天"),
    WEEK("week", "周"),
    MONTH("month", "月"),
    YEAR("year", "年");

    companion object {
        fun fromStorage(value: String?): CustomRepeatUnit =
            values().firstOrNull { it.storageValue == value } ?: DAY
    }
}

/**
 * 印记领域模型。
 *
 * 字段语义按类型划分（参考小米日历）：
 *  - EVENT 日程：[targetTimeMillis] = 开始；[endTimeMillis] = 结束；[isAllDay]、[location]
 *  - BIRTHDAY 生日：[targetTimeMillis] = 生日日期；[hasYear] = 是否带具体年份；[targetIsLunar]
 *  - ANNIVERSARY 纪念日：[targetTimeMillis] = 起算日；[hasYear] 默认 true；[targetIsLunar]
 *  - COUNTDOWN 倒数日：[targetTimeMillis] = 目标日；[targetIsLunar]
 *
 * 通用：
 *  - [reminderOffsetsMinutes] 多个提醒节点（分钟数，正数 = 提前 X 分钟）
 *  - [repeatRule] / [customRepeatInterval] / [customRepeatUnit] / [weekdaysMask] / [monthlyDay]
 *
 * 列表展示规则：先按 [priority] 升序，同优先级按下一次发生时间升序，无时间排末尾。
 */
@Serializable
data class Schedule(
    val id: Long,
    val title: String,
    val type: ScheduleType,
    val priority: Int = DEFAULT_PRIORITY,
    val targetTimeMillis: Long?,
    /** 日程的结束时间；非 EVENT 类型恒为 null。 */
    val endTimeMillis: Long? = null,
    /** EVENT 全天事件标记。 */
    val isAllDay: Boolean = false,
    /** 录入时是否使用农历（仅影响显示语义，存储仍为公历 epoch ms）。 */
    val targetIsLunar: Boolean = false,
    /** BIRTHDAY/ANNIVERSARY：是否带年份；false 时仅按月日循环。 */
    val hasYear: Boolean = true,
    /** EVENT 地点。 */
    val location: String? = null,
    val repeatRule: RepeatRule = RepeatRule.NEVER,
    val customRepeatInterval: Int? = null,
    val customRepeatUnit: CustomRepeatUnit? = null,
    /** 周几位掩码：bit0=周一 … bit6=周日。仅 WEEKLY/CUSTOM(WEEK) 时使用。 */
    val weekdaysMask: Int = 0,
    /** 每月第几日（1..31）；仅 MONTHLY/CUSTOM(MONTH) 时使用。null = 跟随 target 当日。 */
    val monthlyDay: Int? = null,
    /** 提醒节点的「提前分钟数」列表，按升序使用。 */
    val reminderOffsetsMinutes: List<Long> = DEFAULT_REMINDER_OFFSETS_MINUTES,
    /** 是否开启「闹钟强提醒」：到点全屏循环响铃直到用户手动关闭。 */
    val useAlarmRing: Boolean = false,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        const val MIN_PRIORITY = 1
        const val MAX_PRIORITY = 10
        const val DEFAULT_PRIORITY = 5

        /** 默认提醒：3 天 / 1 天 / 3 小时（分钟数）。 */
        val DEFAULT_REMINDER_OFFSETS_MINUTES: List<Long> = listOf(
            3L * 24 * 60,
            1L * 24 * 60,
            3L * 60
        )

        /** Scheduler 取消时迭代的最大 reminder 数量。 */
        const val MAX_REMINDERS_PER_SCHEDULE = 10
    }
}
