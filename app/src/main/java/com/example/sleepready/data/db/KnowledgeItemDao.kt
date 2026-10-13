package com.example.sleepready.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 心动板块条目 DAO。`category` 列存「逗号分隔的多个 storageValue」字符串，
 * 因此按分类筛选时用 `LIKE` 模糊匹配带逗号边界（首/尾插逗号防止 `want_to_read`
 * 误匹配未来某个以 `read` 结尾的值）。
 */
@Dao
interface KnowledgeItemDao {

    @Query("SELECT * FROM knowledge_item ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<KnowledgeItemEntity>>

    @Query("SELECT * FROM knowledge_item ORDER BY updatedAt DESC")
    suspend fun getAllSync(): List<KnowledgeItemEntity>

    /** 单分类筛选：匹配 `,$category,` 在 `,...,` 化的列内即认为命中。 */
    @Query(
        """
        SELECT * FROM knowledge_item
        WHERE (',' || category || ',') LIKE ('%,' || :category || ',%')
        ORDER BY updatedAt DESC
        """
    )
    fun observeByCategory(category: String): Flow<List<KnowledgeItemEntity>>

    @Query("SELECT * FROM knowledge_item WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): KnowledgeItemEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: KnowledgeItemEntity): Long

    @Update
    suspend fun update(entity: KnowledgeItemEntity)

    @Query("UPDATE knowledge_item SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, updatedAt: Long)

    /** 从未完成（idle / paused）池中随机抽一条。 */
    @Query(
        """
        SELECT * FROM knowledge_item
        WHERE status IN ('idle', 'paused')
        ORDER BY RANDOM()
        LIMIT 1
        """
    )
    suspend fun pickRandomActive(): KnowledgeItemEntity?

    @Query("DELETE FROM knowledge_item WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM knowledge_item")
    suspend fun count(): Int
}
