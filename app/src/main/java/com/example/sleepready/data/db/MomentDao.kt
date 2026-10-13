package com.example.sleepready.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MomentDao {

    @Query("SELECT * FROM moment_item ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<MomentEntity>>

    @Query("SELECT * FROM moment_item ORDER BY createdAt DESC, id DESC")
    suspend fun getAll(): List<MomentEntity>

    @Query("SELECT * FROM moment_item WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MomentEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: MomentEntity): Long

    @Update
    suspend fun update(entity: MomentEntity)

    @Query("DELETE FROM moment_item WHERE id = :id")
    suspend fun deleteById(id: Long)
}
