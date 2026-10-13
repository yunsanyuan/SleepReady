package com.example.sleepready.search;

import com.example.sleepready.data.db.KnowledgeItemEntity;
import com.example.sleepready.data.db.MomentEntity;
import com.example.sleepready.data.db.ScheduleEntity;
import com.example.sleepready.data.db.VerseEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 全局搜索引擎（Java 实现）。
 *
 * <p>同步阻塞地从 4 个板块的 DAO 拉数据，对每条记录的相关字段做关键词匹配，
 * 按相关性排序后返回 {@link SearchableItem} 列表。
 *
 * <p>相关性评分（数字越大越靠前）：
 * <ul>
 *   <li>标题完全匹配：100</li>
 *   <li>标题包含：70</li>
 *   <li>正文 / 备注 / 其它字段包含：40</li>
 *   <li>未命中：0（不入结果）</li>
 * </ul>
 *
 * <p>支持中文逐字匹配（直接 {@link String#contains(CharSequence)} 即可）。
 * 简单分词：把 {@code query} 拆成单个非空 token（按空白），任一 token 命中即可，
 * 命中越多得分越高。
 */
public final class SearchEngine {

    private static final int SCORE_TITLE_EXACT = 100;
    private static final int SCORE_TITLE_PARTIAL = 70;
    private static final int SCORE_CONTENT_PARTIAL = 40;
    private static final int SCORE_PER_EXTRA_TOKEN = 5;

    private final SearchDaoBridge bridge;

    public SearchEngine(SearchDaoBridge bridge) {
        this.bridge = bridge;
    }

    /**
     * 执行搜索。空查询返回空列表。
     */
    public List<SearchableItem> search(String query) {
        if (query == null) return Collections.emptyList();
        String normalized = query.trim();
        if (normalized.isEmpty()) return Collections.emptyList();

        String[] tokens = tokenize(normalized);
        List<SearchableItem> out = new ArrayList<>();

        // 印记
        for (ScheduleEntity s : safe(bridge.getSchedules())) {
            int score = scoreSchedule(s, normalized, tokens);
            if (score > 0) {
                String snippet = buildScheduleSnippet(s);
                out.add(new SearchableItem(
                        SearchableItem.Source.SCHEDULE,
                        s.getId(),
                        s.getTitle(),
                        snippet,
                        "schedule",
                        score
                ));
            }
        }

        // 心动
        for (KnowledgeItemEntity k : safe(bridge.getKnowledge())) {
            int score = scoreKnowledge(k, normalized, tokens);
            if (score > 0) {
                String snippet = nullSafe(k.getNote());
                out.add(new SearchableItem(
                        SearchableItem.Source.KNOWLEDGE,
                        k.getId(),
                        k.getTitle(),
                        snippet,
                        "explore",
                        score
                ));
            }
        }

        // 时光
        for (MomentEntity m : safe(bridge.getMoments())) {
            int score = scoreMoment(m, normalized, tokens);
            if (score > 0) {
                String snippet = nullSafe(m.getBody());
                out.add(new SearchableItem(
                        SearchableItem.Source.MOMENT,
                        m.getId(),
                        m.getTitle(),
                        snippet,
                        "moment",
                        score
                ));
            }
        }

        // 拾句
        for (VerseEntity v : safe(bridge.getVerses())) {
            int score = scoreVerse(v, normalized, tokens);
            if (score > 0) {
                String snippet = v.getAuthor() + " · " + truncate(v.getContent(), 60);
                out.add(new SearchableItem(
                        SearchableItem.Source.VERSE,
                        v.getId(),
                        v.getTitle(),
                        snippet,
                        "verse",
                        score
                ));
            }
        }

        // 排序：score 降序；同分按 source 自然顺序稳定
        Collections.sort(out, new Comparator<SearchableItem>() {
            @Override
            public int compare(SearchableItem a, SearchableItem b) {
                int s = Integer.compare(b.getRelevance(), a.getRelevance());
                if (s != 0) return s;
                return a.getSource().ordinal() - b.getSource().ordinal();
            }
        });
        return out;
    }

    // -- Scoring -------------------------------------------------------------

    private int scoreSchedule(ScheduleEntity s, String query, String[] tokens) {
        int titleScore = scoreTitle(s.getTitle(), query, tokens);
        int contentScore = 0;
        if (s.getNote() != null && containsAny(s.getNote(), query, tokens)) {
            contentScore = SCORE_CONTENT_PARTIAL;
        }
        if (s.getLocation() != null && containsAny(s.getLocation(), query, tokens)) {
            contentScore = Math.max(contentScore, SCORE_CONTENT_PARTIAL);
        }
        return Math.max(titleScore, contentScore) + tokenBonus(s.getTitle(), tokens);
    }

    private int scoreKnowledge(KnowledgeItemEntity k, String query, String[] tokens) {
        int titleScore = scoreTitle(k.getTitle(), query, tokens);
        int contentScore = 0;
        if (k.getNote() != null && containsAny(k.getNote(), query, tokens)) {
            contentScore = SCORE_CONTENT_PARTIAL;
        }
        return Math.max(titleScore, contentScore) + tokenBonus(k.getTitle(), tokens);
    }

    private int scoreMoment(MomentEntity m, String query, String[] tokens) {
        int titleScore = scoreTitle(m.getTitle(), query, tokens);
        int contentScore = 0;
        if (m.getBody() != null && containsAny(m.getBody(), query, tokens)) {
            contentScore = SCORE_CONTENT_PARTIAL;
        }
        return Math.max(titleScore, contentScore) + tokenBonus(m.getTitle(), tokens);
    }

    private int scoreVerse(VerseEntity v, String query, String[] tokens) {
        int titleScore = scoreTitle(v.getTitle(), query, tokens);
        int contentScore = 0;
        if (containsAny(v.getAuthor(), query, tokens)) contentScore = SCORE_CONTENT_PARTIAL;
        if (containsAny(v.getContent(), query, tokens)) {
            contentScore = Math.max(contentScore, SCORE_CONTENT_PARTIAL);
        }
        if (v.getNotes() != null && containsAny(v.getNotes(), query, tokens)) {
            contentScore = Math.max(contentScore, SCORE_CONTENT_PARTIAL);
        }
        return Math.max(titleScore, contentScore) + tokenBonus(v.getTitle(), tokens);
    }

    private int scoreTitle(String title, String query, String[] tokens) {
        if (title == null || title.isEmpty()) return 0;
        String tLower = title.toLowerCase();
        String qLower = query.toLowerCase();
        if (tLower.equals(qLower)) return SCORE_TITLE_EXACT;
        if (tLower.contains(qLower)) return SCORE_TITLE_PARTIAL;
        // 任一 token 命中也算部分匹配
        for (String tok : tokens) {
            if (tLower.contains(tok.toLowerCase())) return SCORE_TITLE_PARTIAL;
        }
        return 0;
    }

    private int tokenBonus(String title, String[] tokens) {
        if (title == null || tokens.length <= 1) return 0;
        int hits = 0;
        String tLower = title.toLowerCase();
        for (String tok : tokens) {
            if (tLower.contains(tok.toLowerCase())) hits++;
        }
        return hits > 1 ? (hits - 1) * SCORE_PER_EXTRA_TOKEN : 0;
    }

    private boolean containsAny(String text, String query, String[] tokens) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        if (lower.contains(query.toLowerCase())) return true;
        for (String tok : tokens) {
            if (lower.contains(tok.toLowerCase())) return true;
        }
        return false;
    }

    // -- Helpers -------------------------------------------------------------

    private static String[] tokenize(String query) {
        // 简单分词：按空白拆，过滤空段
        String[] parts = query.trim().split("\\s+");
        List<String> out = new ArrayList<>();
        for (String p : parts) if (!p.isEmpty()) out.add(p);
        return out.toArray(new String[0]);
    }

    private static String buildScheduleSnippet(ScheduleEntity s) {
        StringBuilder sb = new StringBuilder();
        sb.append(s.getType());
        if (s.getNote() != null && !s.getNote().isEmpty()) {
            sb.append(" · ").append(truncate(s.getNote(), 50));
        }
        if (s.getLocation() != null && !s.getLocation().isEmpty()) {
            sb.append(" · 📍").append(s.getLocation());
        }
        return sb.toString();
    }

    private static String truncate(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, max) + "…";
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }

    private static <T> List<T> safe(List<T> list) {
        return list == null ? Collections.<T>emptyList() : list;
    }
}
