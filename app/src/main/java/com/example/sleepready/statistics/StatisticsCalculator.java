package com.example.sleepready.statistics;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 数据统计的纯算法工具类（无 IO，可在任意线程调用）。
 *
 * 由 {@link StatisticsRepository} 调用以聚合各板块的统计数据。
 */
public final class StatisticsCalculator {

    private StatisticsCalculator() { /* utility class */ }

    /**
     * 把一组时间戳按"距今多少天"分桶到 {@code int[daysWindow]} 中。
     * 索引 0 = 今天、索引 daysWindow-1 = (daysWindow-1) 天前。落在窗口外的时间戳忽略。
     */
    public static int[] bucketByDay(List<Long> timestamps, int daysWindow) {
        int[] out = new int[Math.max(daysWindow, 1)];
        if (timestamps == null || timestamps.isEmpty()) return out;
        long now = System.currentTimeMillis();
        long todayStart = startOfDay(now);
        long msPerDay = 24L * 60 * 60 * 1000;
        for (Long ts : timestamps) {
            if (ts == null) continue;
            long dayStart = startOfDay(ts);
            long delta = todayStart - dayStart;
            if (delta < 0) continue; // 未来时间忽略
            int idx = (int) (delta / msPerDay);
            if (idx >= 0 && idx < out.length) out[idx]++;
        }
        return out;
    }

    /**
     * 按某个 String key 分组并计数，保留插入顺序。
     */
    public static <T> Map<String, Integer> groupByKey(List<T> items, KeyExtractor<T> extractor) {
        Map<String, Integer> out = new LinkedHashMap<>();
        if (items == null) return out;
        for (T item : items) {
            String key = extractor.extract(item);
            if (key == null) continue;
            Integer cur = out.get(key);
            out.put(key, cur == null ? 1 : cur + 1);
        }
        return out;
    }

    /**
     * 按 value 倒序取 top N（同 value 保持原顺序）。
     */
    public static Map<String, Integer> topN(Map<String, Integer> source, int n) {
        if (source == null || source.isEmpty() || n <= 0) return new LinkedHashMap<>();
        // 用 TreeMap 反向排序 value
        TreeMap<Integer, java.util.LinkedList<String>> byValue =
                new TreeMap<>((a, b) -> Integer.compare(b, a));
        for (Map.Entry<String, Integer> e : source.entrySet()) {
            byValue.computeIfAbsent(e.getValue(), k -> new java.util.LinkedList<>()).add(e.getKey());
        }
        Map<String, Integer> out = new LinkedHashMap<>();
        outer:
        for (Map.Entry<Integer, java.util.LinkedList<String>> e : byValue.entrySet()) {
            for (String key : e.getValue()) {
                out.put(key, e.getKey());
                if (out.size() >= n) break outer;
            }
        }
        return out;
    }

    /**
     * 计算"连续打卡"天数：从今天往回数，timestamps 中至少有一个落在每一天，则连续 +1，
     * 第一个空白日终止。
     */
    public static int streakDays(List<Long> timestamps) {
        if (timestamps == null || timestamps.isEmpty()) return 0;
        java.util.HashSet<Long> dayStarts = new java.util.HashSet<>();
        for (Long ts : timestamps) {
            if (ts == null) continue;
            dayStarts.add(startOfDay(ts));
        }
        long msPerDay = 24L * 60 * 60 * 1000;
        long cursor = startOfDay(System.currentTimeMillis());
        int streak = 0;
        while (dayStarts.contains(cursor)) {
            streak++;
            cursor -= msPerDay;
        }
        return streak;
    }

    /** 把 epoch ms 归一到当天 00:00（本地时区）。 */
    public static long startOfDay(long ms) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ms);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /** 函数式接口（兼容 Java 8+）。 */
    public interface KeyExtractor<T> {
        String extract(T item);
    }
}
