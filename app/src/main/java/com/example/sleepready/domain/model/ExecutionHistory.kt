package com.example.sleepready.domain.model

/** Domain model mirroring the [com.example.sleepready.data.db.ExecutionHistoryEntity] row. */
data class ExecutionHistory(
    val id: Long,
    val startedAt: Long,
    val finishedAt: Long,
    val durationMinutes: Int,
    val entries: List<ExecutionEntry>
)
