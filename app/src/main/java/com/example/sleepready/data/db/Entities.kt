package com.example.sleepready.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Knowledge inbox entity (Requirement 1.1). Strings are used for [category] and
 * [status] so the storage values stay stable across enum refactors; the Repository
 * layer maps them to [com.example.sleepready.domain.model.KnowledgeCategory] and
 * [com.example.sleepready.domain.model.KnowledgeStatus].
 */
@Entity(
    tableName = "knowledge_item",
    indices = [
        Index("status"),
        Index("category"),
        Index("updatedAt")
    ]
)
data class KnowledgeItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    val link: String?,
    val note: String?,
    val status: String,
    /** v5：图片路径 JSON 数组（最多 3 张，绝对路径，存于 filesDir/knowledge/）。 */
    @ColumnInfo(name = "imagePathsJson", defaultValue = "[]") val imagePathsJson: String = "[]",
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Execution history entity (Requirement 19.1). [entriesJson] is a JSON array of
 * [com.example.sleepready.domain.model.ExecutionEntry] managed by [Converters].
 */
@Entity(
    tableName = "execution_history",
    indices = [Index("startedAt")]
)
data class ExecutionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val finishedAt: Long,
    val durationMinutes: Int,
    @ColumnInfo(name = "entriesJson") val entriesJson: String
)

/**
 * 单行入梦预设的「当前正在运行」快照。Service 启动倒计时时只读这一行；
 * 用户从预设列表点「启动」会先把对应预设拷贝进 id=1，然后再 startService。
 */
@Entity(tableName = "timer_config")
data class TimerConfigEntity(
    @PrimaryKey val id: Int = 1,
    val durationMinutes: Int,
    val planJson: String,
    val updatedAt: Long
) {
    companion object {
        const val SINGLE_ROW_ID = 1
    }
}

/**
 * 入梦预设方案列表中的一行。每条预设独立保存名称、时长和 ActionPlan。
 */
@Entity(
    tableName = "timer_preset",
    indices = [Index("lastUsedAt"), Index("createdAt"), Index("sortIndex"), Index("pinned")]
)
data class TimerPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val durationMinutes: Int,
    val planJson: String,
    /** 上次启动时间（用于排序，未启动过 = 0）。 */
    val lastUsedAt: Long,
    /** v5：手动排序索引（越小越靠前）。 */
    @ColumnInfo(name = "sortIndex", defaultValue = "0") val sortIndex: Int = 0,
    /** v5：是否置顶。置顶项始终排在最前。 */
    @ColumnInfo(name = "pinned", defaultValue = "0") val pinned: Boolean = false,
    /** v5：起床后自动恢复开关 + 时间（小时分钟）。 */
    @ColumnInfo(name = "autoRestoreEnabled", defaultValue = "0") val autoRestoreEnabled: Boolean = false,
    @ColumnInfo(name = "autoRestoreHour", defaultValue = "7") val autoRestoreHour: Int = 7,
    @ColumnInfo(name = "autoRestoreMinute", defaultValue = "0") val autoRestoreMinute: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * 时光记录。图片以「应用私有目录文件路径」JSON 数组形式保存（绝对路径，
 * 创建时由仓库层从 ContentResolver 复制进 filesDir/moments/）。
 */
@Entity(
    tableName = "moment_item",
    indices = [Index("createdAt"), Index("eventTimeMillis")]
)
data class MomentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String?,
    /** 用户填的事件时间，可为空。 */
    val eventTimeMillis: Long?,
    /** JSON array of String, e.g. `["/data/user/0/.../moments/abc.jpg"]`. */
    @ColumnInfo(name = "imagePathsJson") val imagePathsJson: String,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * 印记实体。所有时间字段均为公历 epoch ms（农历输入在写入前由
 * [com.example.sleepready.domain.model.LunarCalendar] 转换）。
 *
 * 数据库 v3：扩展 endTimeMillis / isAllDay / hasYear / location /
 * weekdaysMask / monthlyDay / reminderOffsetsMinutesJson 等字段。
 */
@Entity(
    tableName = "schedule_item",
    indices = [
        Index("priority"),
        Index("targetTimeMillis"),
        Index("type")
    ]
)
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String,
    val priority: Int,
    val targetTimeMillis: Long?,
    val endTimeMillis: Long?,
    val isAllDay: Boolean,
    val targetIsLunar: Boolean,
    val hasYear: Boolean,
    val location: String?,
    val repeatRule: String,
    val customRepeatInterval: Int?,
    val customRepeatUnit: String?,
    val weekdaysMask: Int,
    val monthlyDay: Int?,
    /** JSON array of Long, e.g. `[4320, 1440, 180]`. */
    @ColumnInfo(name = "reminderOffsetsMinutesJson") val reminderOffsetsMinutesJson: String,
    /** v7：是否使用闹钟铃声提醒（到点循环响铃直到用户关闭）。 */
    @ColumnInfo(name = "useAlarmRing", defaultValue = "0") val useAlarmRing: Boolean = false,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long
)


/**
 * 拾句（古诗）板块条目实体。
 */
@Entity(
    tableName = "verse_item",
    indices = [Index("createdAt"), Index("dynasty")]
)
data class VerseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String,
    val dynasty: String,
    val content: String,
    val notes: String?,
    val createdAt: Long
)
