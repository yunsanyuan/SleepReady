package com.example.sleepready.ui.schedule

import com.example.sleepready.domain.model.Schedule
import com.example.sleepready.domain.model.ScheduleType
import java.util.Calendar
import java.util.GregorianCalendar
import kotlin.math.abs

/**
 * 列表卡片右上角的「头条数字」摘要。
 *
 * - 倒数日：还有 N 天 / 今天 / 已过 N 天
 * - 纪念日：今天是第 N 天（包含起算日为第 1 天）
 * - 生日：还有 N 天 / 今天生日
 * - 日程：返回 null（已在主时间区显示开始时间）
 */
object ScheduleSummaries {

    fun headlineFor(schedule: Schedule, now: Long = System.currentTimeMillis()): String? {
        val target = schedule.targetTimeMillis ?: return null
        return when (schedule.type) {
            ScheduleType.COUNTDOWN -> {
                val days = daysFromTodayToDate(now, target)
                when {
                    days > 0 -> "还有 $days 天"
                    days == 0 -> "就是今天"
                    else -> "已过期"
                }
            }
            ScheduleType.ANNIVERSARY -> {
                val days = daysFromTodayToDate(now, target)
                when {
                    days < 0 -> "第 ${abs(days) + 1} 天" // 起算日 = 第 1 天
                    days == 0 -> "今天起算"
                    else -> "尚未开始"
                }
            }
            ScheduleType.BIRTHDAY -> {
                val nextOccur = nextBirthdayMillis(target, now)
                val days = daysFromTodayToDate(now, nextOccur)
                when {
                    days == 0 -> "今天生日"
                    days > 0 -> "还有 $days 天"
                    else -> "—"
                }
            }
            ScheduleType.EVENT -> null
        }
    }

    /** 仅按「日历日」计算两个 epoch 的整天差（target 日 − 今天）。 */
    private fun daysFromTodayToDate(nowMs: Long, targetMs: Long): Int {
        val today = atStartOfDay(nowMs)
        val target = atStartOfDay(targetMs)
        val diff = (target - today) / (24L * 60 * 60 * 1000)
        return diff.toInt()
    }

    private fun atStartOfDay(ms: Long): Long {
        val cal = GregorianCalendar().apply {
            timeInMillis = ms
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /** 找出 [birthdayMs] 在今年或明年的下一次发生（按月日比对）。 */
    private fun nextBirthdayMillis(birthdayMs: Long, nowMs: Long): Long {
        val source = GregorianCalendar().apply { timeInMillis = birthdayMs }
        val month = source.get(Calendar.MONTH)
        val day = source.get(Calendar.DAY_OF_MONTH)
        val now = GregorianCalendar().apply { timeInMillis = nowMs }
        val candidate = GregorianCalendar().apply {
            clear()
            set(now.get(Calendar.YEAR), month, day)
        }
        // 已过今年的生日 → 推到明年
        val today = atStartOfDay(nowMs)
        if (candidate.timeInMillis < today) {
            candidate.add(Calendar.YEAR, 1)
        }
        return candidate.timeInMillis
    }
}
