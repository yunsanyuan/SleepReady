package com.example.sleepready.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExecutionHistoryDao {

    @Query("SELECT * FROM execution_history ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<ExecutionHistoryEntity>>

    @Query("SELECT * FROM execution_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ExecutionHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ExecutionHistoryEntity): Long

    @Update
    suspend fun update(entity: ExecutionHistoryEntity)

    @Query("SELECT COUNT(*) FROM execution_history")
    suspend fun count(): Int

    @Query("DELETE FROM execution_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM execution_history")
    suspend fun deleteAll()
}
