package com.example.sleepready.repository

import com.example.sleepready.data.db.TimerPresetDao
import com.example.sleepready.data.db.TimerPresetEntity
import com.example.sleepready.domain.model.ActionPlan
import com.example.sleepready.domain.model.TimerPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

interface TimerPresetRepository {
    fun observeAll(): Flow<List<TimerPreset>>
    suspend fun getAll(): List<TimerPreset>
    suspend fun getById(id: Long): TimerPreset?
    suspend fun add(preset: TimerPreset): Long
    suspend fun update(preset: TimerPreset)
    suspend fun delete(id: Long)
    suspend fun touchLastUsed(id: Long, ts: Long)
    suspend fun setPinned(id: Long, pinned: Boolean)
    suspend fun reorder(orderedIds: List<Long>)
    suspend fun moveUp(id: Long)
    suspend fun moveDown(id: Long)
}

class TimerPresetRepositoryImpl(
    private val dao: TimerPresetDao,
    private val now: () -> Long = { System.currentTimeMillis() },
    private val json: Json = DEFAULT_JSON
) : TimerPresetRepository {

    override fun observeAll(): Flow<List<TimerPreset>> =
        dao.observeAll().map { rows -> rows.map(::toDomain) }

    override suspend fun getAll(): List<TimerPreset> = dao.getAll().map(::toDomain)

    override suspend fun getById(id: Long): TimerPreset? =
        dao.getById(id)?.let(::toDomain)

    override suspend fun add(preset: TimerPreset): Long {
        val ts = now()
        // 新加入的方案排在末尾
        val maxSort = dao.getAll().maxOfOrNull { it.sortIndex } ?: -1
        return dao.insert(
            preset.toEntity().copy(
                id = 0L,
                sortIndex = maxSort + 1,
                createdAt = ts,
                updatedAt = ts
            )
        )
    }

    override suspend fun update(preset: TimerPreset) {
        val existing = dao.getById(preset.id) ?: return
        dao.update(
            preset.toEntity().copy(
                createdAt = existing.createdAt,
                // 保留现有 sortIndex / pinned，避免 UI 编辑覆盖排序
                sortIndex = existing.sortIndex,
                pinned = existing.pinned,
                updatedAt = now()
            )
        )
    }

    override suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun touchLastUsed(id: Long, ts: Long) {
        dao.touchLastUsed(id, ts)
    }

    override suspend fun setPinned(id: Long, pinned: Boolean) {
        dao.setPinned(id, pinned, now())
    }

    override suspend fun reorder(orderedIds: List<Long>) {
        val ts = now()
        orderedIds.forEachIndexed { index, id -> dao.setSortIndex(id, index, ts) }
    }

    override suspend fun moveUp(id: Long) {
        val list = dao.getAll()
        val idx = list.indexOfFirst { it.id == id }
        if (idx <= 0) return
        // pinned/non-pinned 不互换（保持分组），仅在同组内交换
        val target = list[idx - 1]
        if (target.pinned != list[idx].pinned) return
        val orderedIds = list.toMutableList().apply {
            val tmp = this[idx]
            this[idx] = this[idx - 1]
            this[idx - 1] = tmp
        }.map { it.id }
        reorder(orderedIds)
    }

    override suspend fun moveDown(id: Long) {
        val list = dao.getAll()
        val idx = list.indexOfFirst { it.id == id }
        if (idx < 0 || idx >= list.lastIndex) return
        val target = list[idx + 1]
        if (target.pinned != list[idx].pinned) return
        val orderedIds = list.toMutableList().apply {
            val tmp = this[idx]
            this[idx] = this[idx + 1]
            this[idx + 1] = tmp
        }.map { it.id }
        reorder(orderedIds)
    }

    private fun toDomain(e: TimerPresetEntity): TimerPreset {
        val plan = runCatching {
            json.decodeFromString(ActionPlan.serializer(), e.planJson)
        }.getOrElse { ActionPlan.default() }
        return TimerPreset(
            id = e.id,
            name = e.name,
            durationMinutes = e.durationMinutes,
            plan = plan.normalized(),
            lastUsedAt = e.lastUsedAt,
            sortIndex = e.sortIndex,
            pinned = e.pinned,
            autoRestoreEnabled = e.autoRestoreEnabled,
            autoRestoreHour = e.autoRestoreHour,
            autoRestoreMinute = e.autoRestoreMinute,
            createdAt = e.createdAt,
            updatedAt = e.updatedAt
        )
    }

    private fun TimerPreset.toEntity(): TimerPresetEntity = TimerPresetEntity(
        id = id,
        name = name,
        durationMinutes = durationMinutes,
        planJson = json.encodeToString(ActionPlan.serializer(), plan.normalized()),
        lastUsedAt = lastUsedAt,
        sortIndex = sortIndex,
        pinned = pinned,
        autoRestoreEnabled = autoRestoreEnabled,
        autoRestoreHour = autoRestoreHour,
        autoRestoreMinute = autoRestoreMinute,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        private val DEFAULT_JSON = Json {
            ignoreUnknownKeys = true; encodeDefaults = true
        }
    }
}
