package com.example.sleepready.domain.model

import com.example.sleepready.lunar.LunarCalendar as JavaLunar

/**
 * 农历适配层。核心算法已移到 Java 侧 [com.example.sleepready.lunar.LunarCalendar]，
 * 这个 Kotlin object 仅作为薄壳维持现有调用签名（项目里 ScheduleEditor / ScheduleCard /
 * Summaries 都通过这里访问）。
 *
 * 新代码鼓励直接调用 Java 版本以获得 [JavaLunar.getGanZhiYear] / [JavaLunar.getZodiac] /
 * [JavaLunar.getSolarTermOf] 等扩展能力。
 */
object LunarCalendar {

    /** 与 Java 版同名的值对象（保持 Kotlin 调用方既有 destructure 习惯）。 */
    data class LunarDate(
        val year: Int,
        val month: Int,
        val day: Int,
        val isLeapMonth: Boolean
    ) {
        companion object {
            fun fromJava(j: JavaLunar.LunarDate): LunarDate =
                LunarDate(j.year, j.month, j.day, j.isLeapMonth)
        }
    }

    fun isYearSupported(year: Int): Boolean = JavaLunar.isYearSupported(year)

    fun leapMonth(year: Int): Int = JavaLunar.leapMonth(year)

    fun formatMonth(month: Int, isLeap: Boolean): String =
        JavaLunar.formatMonth(month, isLeap)

    fun formatDay(day: Int): String = JavaLunar.formatDay(day)

    fun format(date: LunarDate): String =
        JavaLunar.formatMonth(date.month, date.isLeapMonth) + JavaLunar.formatDay(date.day)

    fun lunarToSolarMillis(
        lunarYear: Int,
        lunarMonth: Int,
        lunarDay: Int,
        isLeap: Boolean,
        hour: Int = 0,
        minute: Int = 0
    ): Long = JavaLunar.lunarToSolarMillis(lunarYear, lunarMonth, lunarDay, isLeap, hour, minute)

    fun solarMillisToLunar(millis: Long): LunarDate? {
        val j = JavaLunar.solarMillisToLunar(millis) ?: return null
        return LunarDate.fromJava(j)
    }
}
