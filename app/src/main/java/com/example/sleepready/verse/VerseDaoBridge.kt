package com.example.sleepready.verse

import com.example.sleepready.data.db.VerseDao
import com.example.sleepready.data.db.VerseEntity
import kotlinx.coroutines.runBlocking

/**
 * Kotlin → Java 桥接层。把 [VerseDao] 的所有 `suspend` 方法包成阻塞调用，
 * 这样 [VerseRepository]（Java）可以直接调用它而不用与 Kotlin 协程的
 * `Continuation` 字节码打交道。
 *
 * 使用约束：调用方必须保证已在工作线程上（仓库的 Java 调用方都被
 * `Dispatchers.IO` 包裹，因此 [runBlocking] 在这里只是阻塞当前 IO worker
 * 线程，不会阻塞主线程）。
 */
class VerseDaoBridge(private val dao: VerseDao) {

    fun getAllSync(): List<VerseEntity> = runBlocking { dao.getAllSync() }

    fun getByDynastySync(dynasty: String): List<VerseEntity> =
        runBlocking { dao.getByDynastySync(dynasty) }

    fun getByIdSync(id: Long): VerseEntity? = runBlocking { dao.getById(id) }

    fun pickRandomSync(): VerseEntity? = runBlocking { dao.pickRandom() }

    fun insertSync(entity: VerseEntity): Long = runBlocking { dao.insert(entity) }

    fun updateSync(entity: VerseEntity) = runBlocking { dao.update(entity) }

    fun deleteByIdSync(id: Long) = runBlocking { dao.deleteById(id) }

    fun countSync(): Int = runBlocking { dao.count() }
}
