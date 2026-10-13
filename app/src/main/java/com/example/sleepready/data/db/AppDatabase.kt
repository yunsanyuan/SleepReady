package com.example.sleepready.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room database for the app.
 *
 * v4：新增 [TimerPresetEntity]、[MomentEntity]
 * v5：[KnowledgeItemEntity] 加 imagePathsJson；[TimerPresetEntity] 加 sortIndex/pinned/autoRestore*
 * v6：新增 [VerseEntity]（拾句板块）
 *
 * 仍使用 `fallbackToDestructiveMigration` 简化未发布版本之间的演进。
 */
@Database(
    entities = [
        KnowledgeItemEntity::class,
        ExecutionHistoryEntity::class,
        TimerConfigEntity::class,
        TimerPresetEntity::class,
        ScheduleEntity::class,
        MomentEntity::class,
        VerseEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun knowledgeItemDao(): KnowledgeItemDao
    abstract fun executionHistoryDao(): ExecutionHistoryDao
    abstract fun timerConfigDao(): TimerConfigDao
    abstract fun timerPresetDao(): TimerPresetDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun momentDao(): MomentDao
    abstract fun verseDao(): VerseDao

    companion object {
        const val DB_NAME = "sleep_ready.db"

        fun build(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            DB_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }
}
