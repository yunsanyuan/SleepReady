package com.example.sleepready.repository

import com.example.sleepready.data.db.ScheduleDao
import com.example.sleepready.data.db.ScheduleEntity
import com.example.sleepready.domain.model.CustomRepeatUnit
import com.example.sleepready.domain.model.RepeatRule
import com.example.sleepready.domain.model.Schedule
import com.example.sleepready.domain.model.ScheduleType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

interface ScheduleRepository {
    fun observeAll(): Flow<List<Schedule>>
    fun observeByType(type: ScheduleType): Flow<List<Schedule>>
    suspend fun add(schedule: Schedule): Long
    suspend fun update(schedule: Schedule)
    suspend fun delete(id: Long)
    suspend fun getById(id: Long): Schedule?
    suspend fun getAllActive(): List<Schedule>
}

class ScheduleRepositoryImpl(
    private val dao: ScheduleDao,
    private val now: () -> Long = { System.currentTimeMillis() }
) : ScheduleRepository {

    override fun observeAll(): Flow<List<Schedule>> =
        dao.observeAll().map { rows -> rows.map(::toDomain) }

    override fun observeByType(type: ScheduleType): Flow<List<Schedule>> =
        dao.observeByType(type.storageValue).map { rows -> rows.map(::toDomain) }

    override suspend fun add(schedule: Schedule): Long {
        val ts = now()
        return dao.insert(
            schedule.toEntity().copy(
                id = 0L,
                createdAt = if (schedule.createdAt == 0L) ts else schedule.createdAt,
                updatedAt = ts
            )
        )
    }

    override suspend fun update(schedule: Schedule) {
        val existing = dao.getById(schedule.id) ?: return
        dao.update(
            schedule.toEntity().copy(
                createdAt = existing.createdAt,
                updatedAt = now()
            )
        )
    }

    override suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun getById(id: Long): Schedule? = dao.getById(id)?.let(::toDomain)

    override suspend fun getAllActive(): List<Schedule> {
        val nowMs = now()
        return dao.getAllSync().map(::toDomain).filter { (it.targetTimeMillis ?: 0L) > nowMs }
    }
}

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
private val longListSerializer = ListSerializer(Long.serializer())

private fun decodeOffsets(payload: String?): List<Long> {
    if (payload.isNullOrBlank()) return Schedule.DEFAULT_REMINDER_OFFSETS_MINUTES
    return runCatching { json.decodeFromString(longListSerializer, payload) }
        .getOrElse { Schedule.DEFAULT_REMINDER_OFFSETS_MINUTES }
}

private fun encodeOffsets(values: List<Long>): String =
    json.encodeToString(longListSerializer, values)

private fun toDomain(e: ScheduleEntity): Schedule = Schedule(
    id = e.id,
    title = e.title,
    type = ScheduleType.fromStorage(e.type),
    priority = e.priority,
    targetTimeMillis = e.targetTimeMillis,
    endTimeMillis = e.endTimeMillis,
    isAllDay = e.isAllDay,
    targetIsLunar = e.targetIsLunar,
    hasYear = e.hasYear,
    location = e.location,
    repeatRule = RepeatRule.fromStorage(e.repeatRule),
    customRepeatInterval = e.customRepeatInterval,
    customRepeatUnit = e.customRepeatUnit?.let { CustomRepeatUnit.fromStorage(it) },
    weekdaysMask = e.weekdaysMask,
    monthlyDay = e.monthlyDay,
    reminderOffsetsMinutes = decodeOffsets(e.reminderOffsetsMinutesJson),
    useAlarmRing = e.useAlarmRing,
    note = e.note,
    createdAt = e.createdAt,
    updatedAt = e.updatedAt
)

private fun Schedule.toEntity(): ScheduleEntity = ScheduleEntity(
    id = id,
    title = title,
    type = type.storageValue,
    priority = priority,
    targetTimeMillis = targetTimeMillis,
    endTimeMillis = endTimeMillis,
    isAllDay = isAllDay,
    targetIsLunar = targetIsLunar,
    hasYear = hasYear,
    location = location,
    repeatRule = repeatRule.storageValue,
    customRepeatInterval = customRepeatInterval,
    customRepeatUnit = customRepeatUnit?.storageValue,
    weekdaysMask = weekdaysMask,
    monthlyDay = monthlyDay,
    reminderOffsetsMinutesJson = encodeOffsets(reminderOffsetsMinutes),
    useAlarmRing = useAlarmRing,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt
)
