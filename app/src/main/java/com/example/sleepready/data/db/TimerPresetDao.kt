package com.example.sleepready.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerPresetDao {

    /**
     * 排序优先级：pinned 置顶 → 手动 sortIndex 升序 → 上次启动时间倒序 → 创建时间倒序。
     */
    @Query(
        """
        SELECT * FROM timer_preset
        ORDER BY pinned DESC, sortIndex ASC, lastUsedAt DESC, createdAt DESC, id DESC
        """
    )
    fun observeAll(): Flow<List<TimerPresetEntity>>

    @Query(
        """
        SELECT * FROM timer_preset
        ORDER BY pinned DESC, sortIndex ASC, lastUsedAt DESC, createdAt DESC, id DESC
        """
    )
    suspend fun getAll(): List<TimerPresetEntity>

    @Query("SELECT * FROM timer_preset WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TimerPresetEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: TimerPresetEntity): Long

    @Update
    suspend fun update(entity: TimerPresetEntity)

    @Query("DELETE FROM timer_preset WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE timer_preset SET lastUsedAt = :ts WHERE id = :id")
    suspend fun touchLastUsed(id: Long, ts: Long)

    @Query("UPDATE timer_preset SET pinned = :pinned, updatedAt = :ts WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean, ts: Long)

    @Query("UPDATE timer_preset SET sortIndex = :sortIndex, updatedAt = :ts WHERE id = :id")
    suspend fun setSortIndex(id: Long, sortIndex: Int, ts: Long)
}
