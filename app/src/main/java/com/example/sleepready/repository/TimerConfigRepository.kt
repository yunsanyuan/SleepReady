package com.example.sleepready.repository

import com.example.sleepready.data.db.TimerConfigDao
import com.example.sleepready.data.db.TimerConfigEntity
import com.example.sleepready.data.prefs.PreferencesRepository
import com.example.sleepready.domain.model.ActionPlan
import com.example.sleepready.domain.model.TimerConfig
import kotlinx.serialization.json.Json

/**
 * Persists the single user [TimerConfig].
 *
 * Per Requirement 25.6 the structured plan lives in Room (single-row
 * `timer_config` table) while [PreferencesRepository.lastTimerDurationMinutes]
 * mirrors only the duration so widgets / quick-start flows can read it cheaply
 * without touching the database.
 *
 * Validates Property P15 (round-trip persistence).
 */
interface TimerConfigRepository {
    /** Returns the saved config, or `null` on first launch (Requirement 6.3). */
    suspend fun load(): TimerConfig?

    /**
     * Writes [config] to Room and mirrors `config.durationMinutes` into prefs
     * as `last_timer_duration_minutes` (Requirement 9.2).
     */
    suspend fun save(config: TimerConfig)
}

class TimerConfigRepositoryImpl(
    private val dao: TimerConfigDao,
    private val prefs: PreferencesRepository,
    private val json: Json = DEFAULT_JSON
) : TimerConfigRepository {

    override suspend fun load(): TimerConfig? {
        val row = dao.load() ?: return null
        val plan = runCatching {
            json.decodeFromString(ActionPlan.serializer(), row.planJson)
        }.getOrElse { ActionPlan.default() }
        return TimerConfig(
            durationMinutes = row.durationMinutes,
            plan = plan.normalized()
        )
    }

    override suspend fun save(config: TimerConfig) {
        val normalized = config.copy(plan = config.plan.normalized())
        val entity = TimerConfigEntity(
            id = TimerConfigEntity.SINGLE_ROW_ID,
            durationMinutes = normalized.durationMinutes,
            planJson = json.encodeToString(ActionPlan.serializer(), normalized.plan),
            updatedAt = System.currentTimeMillis()
        )
        dao.upsert(entity)
        prefs.lastTimerDurationMinutes = normalized.durationMinutes
    }

    companion object {
        private val DEFAULT_JSON = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
