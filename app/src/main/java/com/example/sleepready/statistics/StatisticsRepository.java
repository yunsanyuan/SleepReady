package com.example.sleepready.statistics;

import com.example.sleepready.data.db.KnowledgeItemEntity;
import com.example.sleepready.data.db.MomentEntity;
import com.example.sleepready.data.db.ScheduleEntity;
import com.example.sleepready.data.db.VerseEntity;
import com.example.sleepready.search.SearchDaoBridge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据统计仓库（Java）。聚合 4 个板块的同步快照并喂给 {@link StatisticsCalculator}。
 *
 * 复用 {@link SearchDaoBridge} 已经把 4 个 DAO 的 sync 方法包成阻塞调用。
 */
public final class StatisticsRepository {

    private final SearchDaoBridge bridge;

    public StatisticsRepository(SearchDaoBridge bridge) {
        this.bridge = bridge;
    }

    /** 各板块条目总数。 */
    public Counts getCounts() {
        return new Counts(
                bridge.getSchedules().size(),
                bridge.getKnowledge().size(),
                bridge.getMoments().size(),
                bridge.getVerses().size()
        );
    }

    /** 心动板块按状态分组的计数（idle / paused / done）。 */
    public Map<String, Integer> getKnowledgeStatusBreakdown() {
        return StatisticsCalculator.groupByKey(
                bridge.getKnowledge(),
                new StatisticsCalculator.KeyExtractor<KnowledgeItemEntity>() {
                    @Override
                    public String extract(KnowledgeItemEntity item) {
                        return labelForKnowledgeStatus(item.getStatus());
                    }
                }
        );
    }

    /** 印记按类型分组的计数（日程 / 生日 / 纪念日 / 倒数日）。 */
    public Map<String, Integer> getScheduleTypeBreakdown() {
        return StatisticsCalculator.groupByKey(
                bridge.getSchedules(),
                new StatisticsCalculator.KeyExtractor<ScheduleEntity>() {
                    @Override
                    public String extract(ScheduleEntity item) {
                        return labelForScheduleType(item.getType());
                    }
                }
        );
    }

    /** 拾句按朝代分组的 top 5。 */
    public Map<String, Integer> getVerseDynastyTop5() {
        Map<String, Integer> all = StatisticsCalculator.groupByKey(
                bridge.getVerses(),
                new StatisticsCalculator.KeyExtractor<VerseEntity>() {
                    @Override
                    public String extract(VerseEntity item) {
                        return item.getDynasty();
                    }
                }
        );
        return StatisticsCalculator.topN(all, 5);
    }

    /**
     * 最近 [days] 天每日新增数（合并 4 个板块的 createdAt）。返回数组长度 = days，
     * 索引 0 = 今天，索引 days-1 = (days-1) 天前。
     */
    public int[] getRecentActivity(int days) {
        List<Long> timestamps = collectAllCreatedAt();
        return StatisticsCalculator.bucketByDay(timestamps, days);
    }

    /** 全部板块合并的最长连续打卡天数（从今天往前数）。 */
    public int getOverallStreak() {
        return StatisticsCalculator.streakDays(collectAllCreatedAt());
    }

    /** 总条目数（4 个板块加起来）。 */
    public int getTotalCount() {
        Counts c = getCounts();
        return c.schedules + c.knowledge + c.moments + c.verses;
    }

    private List<Long> collectAllCreatedAt() {
        List<Long> out = new ArrayList<>();
        for (ScheduleEntity s : bridge.getSchedules()) out.add(s.getCreatedAt());
        for (KnowledgeItemEntity k : bridge.getKnowledge()) out.add(k.getCreatedAt());
        for (MomentEntity m : bridge.getMoments()) out.add(m.getCreatedAt());
        for (VerseEntity v : bridge.getVerses()) out.add(v.getCreatedAt());
        return out;
    }

    private static String labelForKnowledgeStatus(String storage) {
        switch (storage == null ? "" : storage) {
            case "idle":
            case "pending":
                return "想去做";
            case "paused":
            case "in_progress":
                return "在路上";
            case "done":
            case "archived":
                return "已抵达";
            default:
                return "其它";
        }
    }

    private static String labelForScheduleType(String storage) {
        switch (storage == null ? "" : storage) {
            case "event": return "日程";
            case "birthday": return "生日";
            case "anniversary": return "纪念日";
            case "countdown": return "倒数日";
            default: return "其它";
        }
    }

    /** 各板块条目数量值对象。 */
    public static final class Counts {
        public final int schedules;
        public final int knowledge;
        public final int moments;
        public final int verses;

        public Counts(int schedules, int knowledge, int moments, int verses) {
            this.schedules = schedules;
            this.knowledge = knowledge;
            this.moments = moments;
            this.verses = verses;
        }

        /** 给 Kotlin 用的便捷方法。 */
        public Map<String, Integer> asMap() {
            Map<String, Integer> m = new LinkedHashMap<>();
            m.put("印记", schedules);
            m.put("心动", knowledge);
            m.put("时光", moments);
            m.put("拾句", verses);
            return m;
        }

        public int getSchedules() { return schedules; }
        public int getKnowledge() { return knowledge; }
        public int getMoments() { return moments; }
        public int getVerses() { return verses; }
    }
}
