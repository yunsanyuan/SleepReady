package com.example.sleepready.domain.model

import kotlinx.serialization.Serializable

/**
 * One row in an [ExecutionHistory] record: which switch was processed, what action
 * was attempted, and what result the engine produced.
 */
@Serializable
data class ExecutionEntry(
    val switchItem: SwitchItem,
    val action: SwitchAction,
    val result: ExecutionResult,
    val message: String? = null
)
