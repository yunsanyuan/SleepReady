package com.example.sleepready.repository

import android.content.Context
import android.net.Uri
import com.example.sleepready.data.db.KnowledgeItemDao
import com.example.sleepready.data.db.KnowledgeItemEntity
import com.example.sleepready.domain.model.KnowledgeCategory
import com.example.sleepready.domain.model.KnowledgeItem
import com.example.sleepready.domain.model.KnowledgeStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 心动板块仓库。
 *
 * 多分类存储：DB 仍用单列 `category`，但内容是「逗号分隔的 storageValue 列表」
 * （例如 `"want_to_learn,want_to_do"`）。toDomain/encode 在 ↔ 间转换。
 *
 * 图片：复制到 `filesDir/knowledge/<uuid>.jpg`，最多 [KnowledgeItem.MAX_IMAGES] 张；
 * 超出部分会被截断丢弃。删除条目时清理对应文件。
 */
interface KnowledgeRepository {
    fun observeAll(): Flow<List<KnowledgeItem>>
    fun observeByCategory(category: KnowledgeCategory): Flow<List<KnowledgeItem>>

    suspend fun add(
        title: String,
        categories: Set<KnowledgeCategory>,
        link: String?,
        note: String?,
        imageSourceUris: List<Uri> = emptyList()
    ): Result<Long>

    suspend fun setStatus(id: Long, status: KnowledgeStatus)

    suspend fun updateItem(
        id: Long,
        title: String,
        categories: Set<KnowledgeCategory>,
        link: String?,
        note: String?,
        keepImagePaths: List<String> = emptyList(),
        newImageSourceUris: List<Uri> = emptyList()
    ): Result<Unit>

    suspend fun delete(id: Long)

    suspend fun pickRandomActive(): KnowledgeItem?
    suspend fun getById(id: Long): KnowledgeItem?
}

class KnowledgeRepositoryImpl(
    private val context: Context,
    private val dao: KnowledgeItemDao,
    private val now: () -> Long = { System.currentTimeMillis() }
) : KnowledgeRepository {

    private val mediaDir: File by lazy {
        File(context.filesDir, "knowledge").apply { if (!exists()) mkdirs() }
    }

    override fun observeAll(): Flow<List<KnowledgeItem>> =
        dao.observeAll().map { rows -> rows.map(::toDomain) }

    override fun observeByCategory(category: KnowledgeCategory): Flow<List<KnowledgeItem>> =
        dao.observeByCategory(category.storageValue).map { rows -> rows.map(::toDomain) }

    override suspend fun add(
        title: String,
        categories: Set<KnowledgeCategory>,
        link: String?,
        note: String?,
        imageSourceUris: List<Uri>
    ): Result<Long> {
        if (title.isBlank()) {
            return Result.failure(IllegalArgumentException("title must not be blank"))
        }
        if (categories.isEmpty()) {
            return Result.failure(IllegalArgumentException("at least one category required"))
        }
        val savedPaths = withContext(Dispatchers.IO) {
            imageSourceUris
                .take(KnowledgeItem.MAX_IMAGES)
                .mapNotNull { copyUriToPrivateFile(it) }
        }
        val timestamp = now()
        val entity = KnowledgeItemEntity(
            id = 0L,
            title = title.trim(),
            category = encodeCategories(categories),
            link = link?.takeIf { it.isNotBlank() },
            note = note?.takeIf { it.isNotBlank() },
            status = KnowledgeStatus.IDLE.storageValue,
            imagePathsJson = encodeImagePaths(savedPaths),
            createdAt = timestamp,
            updatedAt = timestamp
        )
        val id = dao.insert(entity)
        return Result.success(id)
    }

    override suspend fun setStatus(id: Long, status: KnowledgeStatus) {
        dao.updateStatus(id, status.storageValue, now())
    }

    override suspend fun updateItem(
        id: Long,
        title: String,
        categories: Set<KnowledgeCategory>,
        link: String?,
        note: String?,
        keepImagePaths: List<String>,
        newImageSourceUris: List<Uri>
    ): Result<Unit> {
        if (title.isBlank()) {
            return Result.failure(IllegalArgumentException("title must not be blank"))
        }
        if (categories.isEmpty()) {
            return Result.failure(IllegalArgumentException("at least one category required"))
        }
        val existing = dao.getById(id)
            ?: return Result.failure(IllegalStateException("item $id not found"))
        val oldPaths = decodeImagePaths(existing.imagePathsJson)
        // 删除被移除的图片文件
        withContext(Dispatchers.IO) {
            oldPaths.filter { it !in keepImagePaths }.forEach { runCatching { File(it).delete() } }
        }
        val newSavedPaths = withContext(Dispatchers.IO) {
            val remainingSlots = (KnowledgeItem.MAX_IMAGES - keepImagePaths.size).coerceAtLeast(0)
            newImageSourceUris.take(remainingSlots).mapNotNull { copyUriToPrivateFile(it) }
        }
        val merged = (keepImagePaths + newSavedPaths).take(KnowledgeItem.MAX_IMAGES)
        val updated = existing.copy(
            title = title.trim(),
            category = encodeCategories(categories),
            link = link?.takeIf { it.isNotBlank() },
            note = note?.takeIf { it.isNotBlank() },
            imagePathsJson = encodeImagePaths(merged),
            updatedAt = now()
        )
        dao.update(updated)
        return Result.success(Unit)
    }

    override suspend fun delete(id: Long) {
        val existing = dao.getById(id)
        if (existing != null) {
            withContext(Dispatchers.IO) {
                decodeImagePaths(existing.imagePathsJson).forEach { runCatching { File(it).delete() } }
            }
        }
        dao.deleteById(id)
    }

    override suspend fun pickRandomActive(): KnowledgeItem? =
        dao.pickRandomActive()?.let(::toDomain)

    override suspend fun getById(id: Long): KnowledgeItem? =
        dao.getById(id)?.let(::toDomain)

    private fun copyUriToPrivateFile(uri: Uri): String? = runCatching {
        val target = File(mediaDir, "${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri).use { input ->
            if (input == null) return@runCatching null
            FileOutputStream(target).use { output -> input.copyTo(output) }
        }
        target.absolutePath
    }.getOrNull()

    /** "want_to_learn" / "want_to_learn,want_to_do" → Set。未知值忽略。 */
    private fun decodeCategories(raw: String): Set<KnowledgeCategory> {
        val parts = raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        return parts.mapNotNull { token ->
            runCatching { KnowledgeCategory.fromStorage(token) }.getOrNull()
        }.toSet()
    }

    private fun encodeCategories(set: Set<KnowledgeCategory>): String =
        KnowledgeCategory.values()
            .filter { it in set }
            .joinToString(",") { it.storageValue }

    private fun encodeImagePaths(paths: List<String>): String =
        json.encodeToString(stringListSerializer, paths)

    private fun decodeImagePaths(payload: String?): List<String> {
        if (payload.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString(stringListSerializer, payload) }
            .getOrElse { emptyList() }
    }

    private fun toDomain(e: KnowledgeItemEntity): KnowledgeItem = KnowledgeItem(
        id = e.id,
        title = e.title,
        categories = decodeCategories(e.category),
        link = e.link,
        note = e.note,
        status = KnowledgeStatus.fromStorage(e.status),
        imagePaths = decodeImagePaths(e.imagePathsJson),
        createdAt = e.createdAt,
        updatedAt = e.updatedAt
    )

    companion object {
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        private val stringListSerializer = ListSerializer(String.serializer())
    }
}
