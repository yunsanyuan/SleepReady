package com.example.sleepready.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TimerConfigDao {

    /** Upsert the single row with id = 1. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TimerConfigEntity)

    @Query("SELECT * FROM timer_config WHERE id = 1 LIMIT 1")
    suspend fun load(): TimerConfigEntity?
}
