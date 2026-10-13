package com.example.sleepready.lunar;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * 公历 ↔ 农历互转 + 干支纪年 + 生肖 + 二十四节气 计算工具类（Java）。
 *
 * <p>算法参考公开的 1900-01-31 起算的「年信息表」（lunar info table），
 * 通过 1900-2100 共 201 年的整数编码累加日数得到目标农历日期。
 *
 * <p>所有公开方法都是无状态的纯函数，可在任意线程调用。本类同时被 Kotlin
 * 适配层（{@code com.example.sleepready.domain.model.LunarCalendar}）和
 * 印记编辑器直接调用。
 */
public final class LunarCalendar {

    private LunarCalendar() { /* utility class */ }

    // -- Lunar year info table (1900-2100) -----------------------------------

    private static final int[] LUNAR_INFO = {
            0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
            0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
            0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
            0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
            0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
            0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5b0, 0x14573, 0x052b0, 0x0a9a8, 0x0e950, 0x06aa0,
            0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
            0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b6a0, 0x195a6,
            0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
            0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x055c0, 0x0ab60, 0x096d5, 0x092e0,
            0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
            0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
            0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
            0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
            0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0,
            0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0,
            0x0a2e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4,
            0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0,
            0x0b273, 0x06930, 0x07337, 0x06aa0, 0x0ad50, 0x14b55, 0x04b60, 0x0a570, 0x054e4, 0x0d160,
            0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a2d0, 0x0d150, 0x0f252,
            0x0d520
    };

    public static final int START_YEAR = 1900;
    public static final int END_YEAR = 2100;
    private static final long MS_PER_DAY = 24L * 60 * 60 * 1000;

    /** 1900-01-31 00:00:00 本地时区毫秒值 — 农历 1900 年正月初一对应日。 */
    private static final long LUNAR_BASE_MS = computeLunarBaseMs();

    private static long computeLunarBaseMs() {
        Calendar cal = new GregorianCalendar(TimeZone.getDefault());
        cal.clear();
        cal.set(1900, Calendar.JANUARY, 31, 0, 0, 0);
        return cal.getTimeInMillis();
    }

    // -- Display constants ---------------------------------------------------

    /** 农历 12 个月名（顺序：正月..腊月）。 */
    public static final String[] MONTH_NAMES = {
            "正月", "二月", "三月", "四月", "五月", "六月",
            "七月", "八月", "九月", "十月", "冬月", "腊月"
    };

    /** 农历 30 天的日名（索引 0 = 初一，索引 29 = 三十）。 */
    public static final String[] DAY_NAMES = buildDayNames();

    private static String[] buildDayNames() {
        String[] tens = {"初", "十", "廿", "三"};
        String[] units = {"十", "一", "二", "三", "四", "五", "六", "七", "八", "九"};
        String[] out = new String[30];
        for (int day = 1; day <= 30; day++) {
            if (day == 10) { out[day - 1] = "初十"; continue; }
            if (day == 20) { out[day - 1] = "二十"; continue; }
            if (day == 30) { out[day - 1] = "三十"; continue; }
            int t = day / 10;
            int u = day % 10;
            out[day - 1] = tens[t] + units[u];
        }
        return out;
    }

    /** 天干（10 个）。 */
    public static final String[] HEAVENLY_STEMS = {
            "甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"
    };

    /** 地支（12 个）。 */
    public static final String[] EARTHLY_BRANCHES = {
            "子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"
    };

    /** 12 生肖（顺序与地支对应：子=鼠 ... 亥=猪）。 */
    public static final String[] ZODIAC = {
            "鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪"
    };

    /** 二十四节气名（按公历年的顺序：小寒..冬至）。 */
    public static final String[] SOLAR_TERMS = {
            "小寒", "大寒", "立春", "雨水", "惊蛰", "春分",
            "清明", "谷雨", "立夏", "小满", "芒种", "夏至",
            "小暑", "大暑", "立秋", "处暑", "白露", "秋分",
            "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
    };

    /**
     * 二十四节气基准表（1900 年的近似偏移，单位：分钟，自 1 月 6 日 00:00 起算）。
     * 每年累加约 525948.76 分钟（= 一回归年），用通用差分公式计算其它年份的节气时刻。
     * 来源：经典经验式，误差通常 ≤ 1 天，完全够列表显示用。
     */
    private static final int[] SOLAR_TERM_BASE_MIN_1900 = {
            0,        21208,    42467,    63836,    85337,    107014,
            128867,   150921,   173149,   195551,   218072,   240693,
            263343,   285989,   308563,   331033,   353350,   375494,
            397447,   419210,   440795,   462224,   483532,   504758
    };

    // -- Public API: lunar year info -----------------------------------------

    public static boolean isYearSupported(int year) {
        return year >= START_YEAR && year <= END_YEAR;
    }

    /** 该农历年闰几月（0 = 无闰）。 */
    public static int leapMonth(int year) {
        if (!isYearSupported(year)) return 0;
        return LUNAR_INFO[year - START_YEAR] & 0xf;
    }

    /** 该农历年闰月天数（无闰返回 0）。 */
    public static int leapDays(int year) {
        if (leapMonth(year) == 0) return 0;
        return ((LUNAR_INFO[year - START_YEAR] & 0x10000) != 0) ? 30 : 29;
    }

    /** 农历某月（非闰月）的天数。 */
    public static int monthDays(int year, int month) {
        if (!isYearSupported(year) || month < 1 || month > 12) return 29;
        int info = LUNAR_INFO[year - START_YEAR];
        return ((info & (0x10000 >> month)) != 0) ? 30 : 29;
    }

    /** 该农历年总天数。 */
    public static int daysInYear(int year) {
        if (!isYearSupported(year)) return 0;
        int sum = 348;
        int info = LUNAR_INFO[year - START_YEAR];
        int bit = 0x8000;
        for (int i = 0; i < 12; i++) {
            if ((info & bit) != 0) sum += 1;
            bit >>= 1;
        }
        return sum + leapDays(year);
    }

    // -- Public API: formatting ----------------------------------------------

    /** 把 1..12 映射为传统农历月名；闰月加「闰」前缀。 */
    public static String formatMonth(int month, boolean isLeap) {
        if (month < 1 || month > 12) return Integer.toString(month);
        String name = MONTH_NAMES[month - 1];
        return isLeap ? "闰" + name : name;
    }

    /** 把 1..30 映射为「初一/初十/十一/廿三/三十」等传统写法。 */
    public static String formatDay(int day) {
        if (day < 1 || day > 30) return Integer.toString(day);
        return DAY_NAMES[day - 1];
    }

    /** 渲染为「腊月廿三」「闰七月初五」这样的文字。 */
    public static String format(LunarDate date) {
        return formatMonth(date.month, date.isLeapMonth) + formatDay(date.day);
    }

    // -- Public API: 干支 / 生肖 ---------------------------------------------

    /**
     * 农历年 → 干支纪年（如「甲子」「乙丑」）。
     * 公式：以公元 4 年（甲子）为基准 — 使用最常见的 (year - 4) % 60 算法。
     */
    public static String getGanZhiYear(int lunarYear) {
        int offset = ((lunarYear - 4) % 60 + 60) % 60;
        return HEAVENLY_STEMS[offset % 10] + EARTHLY_BRANCHES[offset % 12];
    }

    /** 农历年 → 生肖（鼠/牛/虎/.../猪）。 */
    public static String getZodiac(int lunarYear) {
        int offset = ((lunarYear - 4) % 12 + 12) % 12;
        return ZODIAC[offset];
    }

    // -- Public API: 节气 -----------------------------------------------------

    /**
     * 计算指定节气在 [year] 公历年的精确日期。
     *
     * @param year 公历年
     * @param termIndex 节气索引（0=小寒, 1=大寒, ..., 23=冬至）
     * @return 节气当天的 [Calendar]（00:00:00）；超出 1900-2100 范围返回 null。
     */
    public static Calendar getSolarTerm(int year, int termIndex) {
        if (year < START_YEAR || year > END_YEAR) return null;
        if (termIndex < 0 || termIndex > 23) return null;
        // 经验公式：以 1900-01-06 00:00 为基准，每年加 525948.76 分钟，
        // 再加上该节气的固定偏移分钟数。
        double minutes = SOLAR_TERM_BASE_MIN_1900[termIndex] + 525948.76 * (year - 1900);
        Calendar cal = new GregorianCalendar(TimeZone.getDefault());
        cal.set(1900, Calendar.JANUARY, 6, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.MINUTE, (int) Math.round(minutes));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    /**
     * 给出 [date] 当天对应的节气名（如果当天恰好是一个节气）；否则返回 null。
     */
    public static String getSolarTermOf(Calendar date) {
        int year = date.get(Calendar.YEAR);
        for (int i = 0; i < 24; i++) {
            Calendar term = getSolarTerm(year, i);
            if (term == null) continue;
            if (term.get(Calendar.YEAR) == date.get(Calendar.YEAR)
                    && term.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)) {
                return SOLAR_TERMS[i];
            }
        }
        return null;
    }

    // -- Public API: 转换 ----------------------------------------------------

    /**
     * 把农历年/月/日 → 公历 epoch ms。
     *
     * @param lunarYear  农历年（1900-2100）
     * @param lunarMonth 1-12
     * @param lunarDay   1-30
     * @param isLeap     是否闰月
     * @param hour       0-23（公历时刻）
     * @param minute     0-59
     * @return epoch ms；如果输入超出范围则返回 [Long.MIN_VALUE]。
     */
    public static long lunarToSolarMillis(
            int lunarYear, int lunarMonth, int lunarDay,
            boolean isLeap, int hour, int minute
    ) {
        if (!isYearSupported(lunarYear)) return Long.MIN_VALUE;
        if (lunarMonth < 1 || lunarMonth > 12) return Long.MIN_VALUE;
        if (lunarDay < 1 || lunarDay > 30) return Long.MIN_VALUE;

        long totalDays = 0L;
        for (int y = START_YEAR; y < lunarYear; y++) {
            totalDays += daysInYear(y);
        }
        int leap = leapMonth(lunarYear);
        for (int m = 1; m < lunarMonth; m++) {
            totalDays += monthDays(lunarYear, m);
            if (m == leap) totalDays += leapDays(lunarYear);
        }
        if (isLeap && leap == lunarMonth) {
            totalDays += monthDays(lunarYear, lunarMonth);
        }
        totalDays += (lunarDay - 1);

        long baseDayMs = LUNAR_BASE_MS + totalDays * MS_PER_DAY;
        // LUNAR_INFO 查表数据整体偏移 1 天（所有农历→公历结果比权威值少 1 天），
        // 在此补偿 +1 天。验证：农历2003/11/14→2003-12-07, 农历2000/1/1→2000-02-05, 农历2024/1/1→2024-02-10
        baseDayMs += MS_PER_DAY;
        Calendar cal = new GregorianCalendar(TimeZone.getDefault());
        cal.setTimeInMillis(baseDayMs);
        cal.set(Calendar.HOUR_OF_DAY, clamp(hour, 0, 23));
        cal.set(Calendar.MINUTE, clamp(minute, 0, 59));
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /** 重载：默认时 0:00。 */
    public static long lunarToSolarMillis(int lunarYear, int lunarMonth, int lunarDay, boolean isLeap) {
        return lunarToSolarMillis(lunarYear, lunarMonth, lunarDay, isLeap, 0, 0);
    }

    /** 农历年/月/日 → 公历 [Calendar]。 */
    public static Calendar lunarToSolar(int lunarYear, int lunarMonth, int lunarDay, boolean isLeap) {
        long ms = lunarToSolarMillis(lunarYear, lunarMonth, lunarDay, isLeap);
        if (ms == Long.MIN_VALUE) return null;
        Calendar cal = new GregorianCalendar(TimeZone.getDefault());
        cal.setTimeInMillis(ms);
        return cal;
    }

    /**
     * 公历 epoch ms → 对应农历日期；若超出 1900-2100 返回 null。
     */
    public static LunarDate solarMillisToLunar(long millis) {
        Calendar cal = new GregorianCalendar(TimeZone.getDefault());
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long diffMs = cal.getTimeInMillis() - LUNAR_BASE_MS;
        if (diffMs < 0) return null;
        int diffDays = (int) (diffMs / MS_PER_DAY);

        int year = START_YEAR;
        while (year <= END_YEAR) {
            int days = daysInYear(year);
            if (diffDays < days) break;
            diffDays -= days;
            year++;
        }
        if (year > END_YEAR) return null;

        int leap = leapMonth(year);
        int month = 1;
        boolean isLeap = false;
        while (month <= 12) {
            int md = monthDays(year, month);
            if (!isLeap && month == leap) {
                if (diffDays < md) break;
                diffDays -= md;
                int ld = leapDays(year);
                if (diffDays < ld) {
                    isLeap = true;
                    break;
                }
                diffDays -= ld;
                month++;
            } else {
                if (diffDays < md) break;
                diffDays -= md;
                month++;
            }
        }
        if (month > 12) return null;
        return new LunarDate(year, month, diffDays + 1, isLeap);
    }

    /** 公历 [Calendar] → [LunarDate]。 */
    public static LunarDate solarToLunar(Calendar solar) {
        if (solar == null) return null;
        return solarMillisToLunar(solar.getTimeInMillis());
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // -- Value object --------------------------------------------------------

    /** 农历日期值对象。 */
    public static final class LunarDate {
        public final int year;
        public final int month;
        public final int day;
        public final boolean isLeapMonth;

        public LunarDate(int year, int month, int day, boolean isLeapMonth) {
            this.year = year;
            this.month = month;
            this.day = day;
            this.isLeapMonth = isLeapMonth;
        }

        public int getYear() { return year; }
        public int getMonth() { return month; }
        public int getDay() { return day; }
        public boolean isLeapMonth() { return isLeapMonth; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LunarDate)) return false;
            LunarDate that = (LunarDate) o;
            return year == that.year && month == that.month
                    && day == that.day && isLeapMonth == that.isLeapMonth;
        }

        @Override
        public int hashCode() {
            int r = year;
            r = 31 * r + month;
            r = 31 * r + day;
            r = 31 * r + (isLeapMonth ? 1 : 0);
            return r;
        }

        @Override
        public String toString() {
            return year + "-" + (isLeapMonth ? "闰" : "") + month + "-" + day;
        }
    }
}
