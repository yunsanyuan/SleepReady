package com.example.sleepready.repository

import com.example.sleepready.data.db.ExecutionHistoryDao
import com.example.sleepready.data.db.ExecutionHistoryEntity
import com.example.sleepready.domain.model.ExecutionEntry
import com.example.sleepready.domain.model.ExecutionHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * History repository for the execution history pane (Requirements 19.2, 20.1).
 *
 * Wraps [ExecutionHistoryDao] and translates between the persistence-layer
 * [ExecutionHistoryEntity] (which stores entries as a JSON string) and the
 * domain-layer [ExecutionHistory] (which exposes a typed `List<ExecutionEntry>`).
 *
 * Properties P12 / P13 rely on this repo:
 *  - P12: cancelling a timer must not produce a new row → `count()` must remain unchanged.
 *  - P13: a normally-finished timer inserts exactly one row whose `entries` round-trip
 *    losslessly through serialization.
 */
interface HistoryRepository {
    fun observeAll(): Flow<List<ExecutionHistory>>
    suspend fun getById(id: Long): ExecutionHistory?
    suspend fun insert(record: ExecutionHistory): Long
    suspend fun replaceEntries(id: Long, entries: List<ExecutionEntry>, finishedAt: Long? = null)
    suspend fun count(): Int
    suspend fun delete(id: Long)
    suspend fun clearAll()
}

class HistoryRepositoryImpl(
    private val dao: ExecutionHistoryDao,
    private val json: Json = DEFAULT_JSON
) : HistoryRepository {

    private val entriesSerializer = ListSerializer(ExecutionEntry.serializer())

    override fun observeAll(): Flow<List<ExecutionHistory>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getById(id: Long): ExecutionHistory? =
        dao.getById(id)?.toDomain()

    override suspend fun insert(record: ExecutionHistory): Long {
        val entity = ExecutionHistoryEntity(
            id = 0,
            startedAt = record.startedAt,
            finishedAt = record.finishedAt,
            durationMinutes = record.durationMinutes,
            entriesJson = json.encodeToString(entriesSerializer, record.entries)
        )
        return dao.insert(entity)
    }

    override suspend fun replaceEntries(id: Long, entries: List<ExecutionEntry>, finishedAt: Long?) {
        val existing = dao.getById(id) ?: return
        dao.update(
            existing.copy(
                entriesJson = json.encodeToString(entriesSerializer, entries),
                finishedAt = finishedAt ?: existing.finishedAt
            )
        )
    }

    override suspend fun count(): Int = dao.count()

    override suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun clearAll() {
        dao.deleteAll()
    }

    private fun ExecutionHistoryEntity.toDomain(): ExecutionHistory = ExecutionHistory(
        id = id,
        startedAt = startedAt,
        finishedAt = finishedAt,
        durationMinutes = durationMinutes,
        entries = if (entriesJson.isEmpty()) emptyList()
        else json.decodeFromString(entriesSerializer, entriesJson)
    )

    companion object {
        // Lenient defaults so a future enum addition doesn't blow up parsing of older rows
        // — mirrors the policy used by `data.db.Converters`.
        private val DEFAULT_JSON = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
