package com.example.sleepready.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 拾句 DAO。
 *
 * 提供两套接口：
 *  - `observeXxx`：返回 [Flow]，给 Kotlin/Compose UI 订阅
 *  - `getXxxSync`：suspend 返回 [List]，给 Java 层（[com.example.sleepready.verse.VerseRepository]）
 *    与 [com.example.sleepready.search.SearchEngine] 同步调用
 */
@Dao
interface VerseDao {

    @Query("SELECT * FROM verse_item ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<VerseEntity>>

    @Query("SELECT * FROM verse_item ORDER BY createdAt DESC, id DESC")
    suspend fun getAllSync(): List<VerseEntity>

    @Query("SELECT * FROM verse_item WHERE dynasty = :dynasty ORDER BY createdAt DESC, id DESC")
    fun observeByDynasty(dynasty: String): Flow<List<VerseEntity>>

    @Query("SELECT * FROM verse_item WHERE dynasty = :dynasty ORDER BY createdAt DESC, id DESC")
    suspend fun getByDynastySync(dynasty: String): List<VerseEntity>

    @Query("SELECT * FROM verse_item WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): VerseEntity?

    @Query("SELECT * FROM verse_item ORDER BY RANDOM() LIMIT 1")
    suspend fun pickRandom(): VerseEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: VerseEntity): Long

    @Update
    suspend fun update(entity: VerseEntity)

    @Query("DELETE FROM verse_item WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM verse_item")
    suspend fun count(): Int
}
