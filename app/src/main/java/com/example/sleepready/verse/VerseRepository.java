package com.example.sleepready.verse;

import com.example.sleepready.data.db.VerseEntity;
import com.example.sleepready.domain.model.VerseItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 拾句板块仓库（Java 实现）。
 *
 * <p>本类同步阻塞地调用 {@link VerseDaoBridge}（Kotlin 适配层把 suspend DAO 包成
 * 阻塞方法）。Kotlin {@code ViewModel} 通过 {@code withContext(Dispatchers.IO)}
 * 调用本类，避免阻塞主线程。
 *
 * <p>提供的方法：
 * <ul>
 *   <li>{@link #getAll()} 按 createdAt 倒序</li>
 *   <li>{@link #getByDynasty(String)} 按朝代筛选；"全部" 等价于 getAll()</li>
 *   <li>{@link #getById(long)} 按 id 取单条</li>
 *   <li>{@link #getRandom()} 随机一首</li>
 *   <li>{@link #insert(String, String, String, String, String)} 新增</li>
 *   <li>{@link #update(long, String, String, String, String, String)} 更新</li>
 *   <li>{@link #delete(long)} 删除</li>
 *   <li>{@link #count()} 总条数</li>
 * </ul>
 */
public final class VerseRepository {

    private final VerseDaoBridge bridge;

    public VerseRepository(VerseDaoBridge bridge) {
        this.bridge = bridge;
    }

    // -- Read ----------------------------------------------------------------

    public List<VerseItem> getAll() {
        return toDomainList(bridge.getAllSync());
    }

    public List<VerseItem> getByDynasty(String dynasty) {
        if (dynasty == null || dynasty.isEmpty() || "全部".equals(dynasty)) {
            return getAll();
        }
        return toDomainList(bridge.getByDynastySync(dynasty));
    }

    public VerseItem getById(long id) {
        VerseEntity e = bridge.getByIdSync(id);
        return e == null ? null : toDomain(e);
    }

    public VerseItem getRandom() {
        VerseEntity e = bridge.pickRandomSync();
        return e == null ? null : toDomain(e);
    }

    public int count() {
        return bridge.countSync();
    }

    // -- Write ---------------------------------------------------------------

    public long insert(String title, String author, String dynasty,
                       String content, String notes) {
        validate(title, author, dynasty, content);
        VerseEntity entity = new VerseEntity(
                0L,
                safeTrim(title),
                safeTrim(author),
                safeTrim(dynasty),
                safeTrim(content),
                trimToNullIfBlank(notes),
                System.currentTimeMillis()
        );
        return bridge.insertSync(entity);
    }

    public void update(long id, String title, String author, String dynasty,
                       String content, String notes) {
        validate(title, author, dynasty, content);
        VerseEntity existing = bridge.getByIdSync(id);
        if (existing == null) return;
        VerseEntity updated = new VerseEntity(
                existing.getId(),
                safeTrim(title),
                safeTrim(author),
                safeTrim(dynasty),
                safeTrim(content),
                trimToNullIfBlank(notes),
                existing.getCreatedAt()
        );
        bridge.updateSync(updated);
    }

    public void delete(long id) {
        bridge.deleteByIdSync(id);
    }

    // -- Helpers -------------------------------------------------------------

    private static List<VerseItem> toDomainList(List<VerseEntity> rows) {
        if (rows == null) return new ArrayList<>();
        List<VerseItem> out = new ArrayList<>(rows.size());
        for (VerseEntity e : rows) out.add(toDomain(e));
        return out;
    }

    private static VerseItem toDomain(VerseEntity e) {
        return new VerseItem(
                e.getId(),
                e.getTitle(),
                e.getAuthor(),
                e.getDynasty(),
                e.getContent(),
                e.getNotes(),
                e.getCreatedAt()
        );
    }

    private static void validate(String title, String author, String dynasty, String content) {
        if (isBlank(title)) throw new IllegalArgumentException("title required");
        if (isBlank(author)) throw new IllegalArgumentException("author required");
        if (isBlank(dynasty)) throw new IllegalArgumentException("dynasty required");
        if (isBlank(content)) throw new IllegalArgumentException("content required");
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String trimToNullIfBlank(String s) {
        if (isBlank(s)) return null;
        return s.trim();
    }
}
