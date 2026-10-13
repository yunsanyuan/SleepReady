package com.example.sleepready.data.db

import androidx.room.TypeConverter
import com.example.sleepready.domain.model.ExecutionEntry
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Room TypeConverters backed by kotlinx.serialization. The execution_history.entries
 * column is stored as a JSON string of List<ExecutionEntry>.
 */
class Converters {

    @TypeConverter
    fun executionEntriesToJson(entries: List<ExecutionEntry>): String =
        json.encodeToString(entriesSerializer, entries)

    @TypeConverter
    fun executionEntriesFromJson(payload: String?): List<ExecutionEntry> {
        if (payload.isNullOrEmpty()) return emptyList()
        return json.decodeFromString(entriesSerializer, payload)
    }

    companion object {
        // Lenient defaults so a future enum addition doesn't blow up parsing of older rows.
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
        private val entriesSerializer = ListSerializer(ExecutionEntry.serializer())
    }
}
