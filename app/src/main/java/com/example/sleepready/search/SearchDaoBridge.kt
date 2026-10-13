package com.example.sleepready.search

import com.example.sleepready.data.db.KnowledgeItemDao
import com.example.sleepready.data.db.KnowledgeItemEntity
import com.example.sleepready.data.db.MomentDao
import com.example.sleepready.data.db.MomentEntity
import com.example.sleepready.data.db.ScheduleDao
import com.example.sleepready.data.db.ScheduleEntity
import com.example.sleepready.data.db.VerseDao
import com.example.sleepready.data.db.VerseEntity
import kotlinx.coroutines.runBlocking

/**
 * 把 4 个 DAO 的 suspend `getAllSync` 方法包成阻塞调用，方便 Java 侧
 * [SearchEngine] 在 IO 线程上同步调用。
 */
class SearchDaoBridge(
    private val scheduleDao: ScheduleDao,
    private val knowledgeDao: KnowledgeItemDao,
    private val momentDao: MomentDao,
    private val verseDao: VerseDao
) {
    fun getSchedules(): List<ScheduleEntity> = runBlocking { scheduleDao.getAllSync() }
    fun getKnowledge(): List<KnowledgeItemEntity> = runBlocking { knowledgeDao.getAllSync() }
    fun getMoments(): List<MomentEntity> = runBlocking { momentDao.getAll() }
    fun getVerses(): List<VerseEntity> = runBlocking { verseDao.getAllSync() }
}
