package com.example.sleepready.search;

/**
 * 全局搜索结果条目（POJO）。
 *
 * <p>由 {@link SearchEngine} 产生、{@code SearchScreen.kt} 消费。
 * {@link #routeArg} 当前仅用于点击跳转目标板块（不做深链到具体条目），
 * 后续可扩展为 "板块/id"。
 */
public final class SearchableItem {

    /** 来源板块。和底部导航的 4 个 root + 拾句一一对应。 */
    public enum Source {
        SCHEDULE("印记"),
        KNOWLEDGE("心动"),
        MOMENT("时光"),
        VERSE("拾句");

        private final String displayName;

        Source(String displayName) { this.displayName = displayName; }

        public String getDisplayName() { return displayName; }
    }

    private final Source source;
    private final long id;
    private final String title;
    private final String snippet;
    private final String routeArg;
    private final int relevance;

    public SearchableItem(Source source, long id, String title,
                          String snippet, String routeArg, int relevance) {
        this.source = source;
        this.id = id;
        this.title = title == null ? "" : title;
        this.snippet = snippet == null ? "" : snippet;
        this.routeArg = routeArg == null ? "" : routeArg;
        this.relevance = relevance;
    }

    public Source getSource() { return source; }
    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getSnippet() { return snippet; }
    public String getRouteArg() { return routeArg; }
    public int getRelevance() { return relevance; }
}
