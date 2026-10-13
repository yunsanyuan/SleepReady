package com.example.sleepready.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 日程表 DAO。
 *
 * 排序规则：先按 [ScheduleEntity.priority] 升序（数值越低越靠前），
 * 同优先级再按 [ScheduleEntity.targetTimeMillis] 升序；空目标时间排在最后
 * （SQLite 默认把 NULL 排在最前，因此这里用 `IS NULL` 把它推到最后）。
 */
@Dao
interface ScheduleDao {

    @Query(
        """
        SELECT * FROM schedule_item
        ORDER BY priority ASC,
                 CASE WHEN targetTimeMillis IS NULL THEN 1 ELSE 0 END,
                 targetTimeMillis ASC,
                 id ASC
        """
    )
    fun observeAll(): Flow<List<ScheduleEntity>>

    @Query(
        """
        SELECT * FROM schedule_item
        ORDER BY priority ASC,
                 CASE WHEN targetTimeMillis IS NULL THEN 1 ELSE 0 END,
                 targetTimeMillis ASC,
                 id ASC
        """
    )
    suspend fun getAllSync(): List<ScheduleEntity>

    @Query(
        """
        SELECT * FROM schedule_item
        WHERE type = :type
        ORDER BY priority ASC,
                 CASE WHEN targetTimeMillis IS NULL THEN 1 ELSE 0 END,
                 targetTimeMillis ASC,
                 id ASC
        """
    )
    fun observeByType(type: String): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedule_item WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ScheduleEntity): Long

    @Update
    suspend fun update(entity: ScheduleEntity)

    @Query("DELETE FROM schedule_item WHERE id = :id")
    suspend fun deleteById(id: Long)
}
