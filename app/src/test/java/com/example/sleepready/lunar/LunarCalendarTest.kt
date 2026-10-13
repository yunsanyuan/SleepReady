package com.example.sleepready.lunar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

class LunarCalendarTest {

    private fun millisToDateStr(ms: Long): String {
        val cal = GregorianCalendar(TimeZone.getTimeZone("Asia/Shanghai"))
        cal.timeInMillis = ms
        return String.format(
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun dateToMillis(year: Int, month: Int, day: Int): Long {
        val cal = GregorianCalendar(TimeZone.getTimeZone("Asia/Shanghai"))
        cal.clear()
        cal.set(year, month - 1, day, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    @Test
    fun testLunarToSolar_1900_1_1() {
        val ms = LunarCalendar.lunarToSolarMillis(1900, 1, 1, false)
        assertEquals("1900-02-01", millisToDateStr(ms))
    }

    @Test
    fun testLunarToSolar_2000_1_1() {
        val ms = LunarCalendar.lunarToSolarMillis(2000, 1, 1, false)
        assertEquals("2000-02-05", millisToDateStr(ms))
    }

    @Test
    fun testLunarToSolar_2003_1_1() {
        val ms = LunarCalendar.lunarToSolarMillis(2003, 1, 1, false)
        assertEquals("2003-02-01", millisToDateStr(ms))
    }

    @Test
    fun testLunarToSolar_2003_11_14() {
        val ms = LunarCalendar.lunarToSolarMillis(2003, 11, 14, false)
        assertEquals("2003-12-07", millisToDateStr(ms))
    }

    @Test
    fun testLunarToSolar_2023_1_1() {
        val ms = LunarCalendar.lunarToSolarMillis(2023, 1, 1, false)
        assertEquals("2023-01-22", millisToDateStr(ms))
    }

    @Test
    fun testLunarToSolar_2024_1_1() {
        val ms = LunarCalendar.lunarToSolarMillis(2024, 1, 1, false)
        assertEquals("2024-02-10", millisToDateStr(ms))
    }

    @Test
    fun testLunarToSolar_2023_leap2_1() {
        val ms = LunarCalendar.lunarToSolarMillis(2023, 2, 1, true)
        assertEquals("2023-03-22", millisToDateStr(ms))
    }

    @Test
    fun testLunarToSolar_2025_leap6_1() {
        val ms = LunarCalendar.lunarToSolarMillis(2025, 6, 1, true)
        assertEquals("2025-07-25", millisToDateStr(ms))
    }

    @Test
    fun testSolarToLunar_2000_02_05() {
        val ld = LunarCalendar.solarMillisToLunar(dateToMillis(2000, 2, 5))
        assertNotNull(ld)
        assertEquals(2000, ld!!.year)
        assertEquals(1, ld.month)
        assertEquals(1, ld.day)
        assertEquals(false, ld.isLeapMonth)
    }

    @Test
    fun testSolarToLunar_2024_02_10() {
        val ld = LunarCalendar.solarMillisToLunar(dateToMillis(2024, 2, 10))
        assertNotNull(ld)
        assertEquals(2024, ld!!.year)
        assertEquals(1, ld.month)
        assertEquals(1, ld.day)
        assertEquals(false, ld.isLeapMonth)
    }

    @Test
    fun testSolarToLunar_2003_12_07() {
        val ld = LunarCalendar.solarMillisToLunar(dateToMillis(2003, 12, 7))
        assertNotNull(ld)
        assertEquals(2003, ld!!.year)
        assertEquals(11, ld.month)
        assertEquals(14, ld.day)
        assertEquals(false, ld.isLeapMonth)
    }

    @Test
    fun testRoundTrip_LunarToSolarToLunar() {
        val points = listOf(

            Triple(2000, 1, 1),
            Triple(2003, 1, 1),
            Triple(2003, 11, 14),
            Triple(2023, 1, 1),
            Triple(2024, 1, 1)
        )
        for ((y, m, d) in points) {
            val ms = LunarCalendar.lunarToSolarMillis(y, m, d, false)
            val ld = LunarCalendar.solarMillisToLunar(ms)
            assertNotNull("Round-trip failed for $y/$m/$d", ld)
            assertEquals("Year mismatch for $y/$m/$d", y, ld!!.year)
            assertEquals("Month mismatch for $y/$m/$d", m, ld.month)
            assertEquals("Day mismatch for $y/$m/$d", d, ld.day)
        }
    }

    @Test
    fun testRoundTrip_SolarToLunarToSolar() {
        val points = listOf(
            Triple(2000, 2, 5),
            Triple(2024, 2, 10),
            Triple(2003, 12, 7)
        )
        for ((y, m, d) in points) {
            val ms = dateToMillis(y, m, d)
            val ld = LunarCalendar.solarMillisToLunar(ms)
            assertNotNull(ld)
            val ms2 = LunarCalendar.lunarToSolarMillis(ld!!.year, ld.month, ld.day, ld.isLeapMonth)
            assertEquals("Round-trip date mismatch for $y-$m-$d", millisToDateStr(ms), millisToDateStr(ms2))
        }
    }

    @Test
    fun testGanZhiYear_Unchanged() {
        assertEquals("癸未", LunarCalendar.getGanZhiYear(2003))
        assertEquals("甲子", LunarCalendar.getGanZhiYear(1984))
        assertEquals("甲辰", LunarCalendar.getGanZhiYear(2024))
    }

    @Test
    fun testZodiac_Unchanged() {
        assertEquals("龙", LunarCalendar.getZodiac(2024))
        assertEquals("羊", LunarCalendar.getZodiac(2003))
        assertEquals("鼠", LunarCalendar.getZodiac(2020))
    }

    @Test
    fun testSolarTerm_Unchanged() {
        val term = LunarCalendar.getSolarTerm(2024, 0)
        assertNotNull(term)
        assertEquals(2024, term!![Calendar.YEAR])
    }

    @Test
    fun testFormat_Unchanged() {
        val ld = LunarCalendar.LunarDate(2003, 11, 14, false)
        assertEquals("冬月十四", LunarCalendar.format(ld))
        val ld2 = LunarCalendar.LunarDate(2024, 1, 1, false)
        assertEquals("正月初一", LunarCalendar.format(ld2))
    }
}
