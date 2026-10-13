package com.example.sleepready.repository

import android.content.Context
import android.net.Uri
import com.example.sleepready.data.db.MomentDao
import com.example.sleepready.data.db.MomentEntity
import com.example.sleepready.domain.model.Moment
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

interface MomentRepository {
    fun observeAll(): Flow<List<Moment>>
    suspend fun getAll(): List<Moment>
    suspend fun getById(id: Long): Moment?
    suspend fun add(
        title: String,
        body: String?,
        eventTimeMillis: Long?,
        imageSourceUris: List<Uri>
    ): Long

    suspend fun update(
        id: Long,
        title: String,
        body: String?,
        eventTimeMillis: Long?,
        keepImagePaths: List<String>,
        newImageSourceUris: List<Uri>
    )

    suspend fun delete(id: Long)
}

/**
 * 时光记录仓库。图片复制到 `filesDir/moments/<uuid>.jpg`，删除条目时清理对应文件。
 */
class MomentRepositoryImpl(
    private val context: Context,
    private val dao: MomentDao,
    private val now: () -> Long = { System.currentTimeMillis() },
    private val json: Json = DEFAULT_JSON
) : MomentRepository {

    private val mediaDir: File by lazy {
        File(context.filesDir, "moments").apply { if (!exists()) mkdirs() }
    }

    override fun observeAll(): Flow<List<Moment>> =
        dao.observeAll().map { rows -> rows.map(::toDomain) }

    override suspend fun getAll(): List<Moment> = dao.getAll().map(::toDomain)

    override suspend fun getById(id: Long): Moment? = dao.getById(id)?.let(::toDomain)

    override suspend fun add(
        title: String,
        body: String?,
        eventTimeMillis: Long?,
        imageSourceUris: List<Uri>
    ): Long {
        require(title.isNotBlank()) { "title required" }
        val savedPaths = withContext(Dispatchers.IO) {
            imageSourceUris.mapNotNull { uri -> copyUriToPrivateFile(uri) }
        }
        val ts = now()
        val entity = MomentEntity(
            id = 0L,
            title = title.trim(),
            body = body?.takeIf { it.isNotBlank() }?.trim(),
            eventTimeMillis = eventTimeMillis,
            imagePathsJson = encode(savedPaths),
            createdAt = ts,
            updatedAt = ts
        )
        return dao.insert(entity)
    }

    override suspend fun update(
        id: Long,
        title: String,
        body: String?,
        eventTimeMillis: Long?,
        keepImagePaths: List<String>,
        newImageSourceUris: List<Uri>
    ) {
        require(title.isNotBlank()) { "title required" }
        val existing = dao.getById(id) ?: return
        val oldPaths = decode(existing.imagePathsJson)
        // 删掉用户没保留的旧图片
        withContext(Dispatchers.IO) {
            oldPaths.filter { it !in keepImagePaths }.forEach { File(it).delete() }
        }
        val newSavedPaths = withContext(Dispatchers.IO) {
            newImageSourceUris.mapNotNull { copyUriToPrivateFile(it) }
        }
        dao.update(
            existing.copy(
                title = title.trim(),
                body = body?.takeIf { it.isNotBlank() }?.trim(),
                eventTimeMillis = eventTimeMillis,
                imagePathsJson = encode(keepImagePaths + newSavedPaths),
                updatedAt = now()
            )
        )
    }

    override suspend fun delete(id: Long) {
        val existing = dao.getById(id) ?: return
        withContext(Dispatchers.IO) {
            decode(existing.imagePathsJson).forEach { File(it).delete() }
        }
        dao.deleteById(id)
    }

    private fun copyUriToPrivateFile(uri: Uri): String? = runCatching {
        val target = File(mediaDir, "${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri).use { input ->
            if (input == null) return@runCatching null
            FileOutputStream(target).use { output ->
                input.copyTo(output)
            }
        }
        target.absolutePath
    }.getOrNull()

    private fun toDomain(e: MomentEntity): Moment = Moment(
        id = e.id,
        title = e.title,
        body = e.body,
        eventTimeMillis = e.eventTimeMillis,
        imagePaths = decode(e.imagePathsJson),
        createdAt = e.createdAt,
        updatedAt = e.updatedAt
    )

    private fun encode(paths: List<String>): String =
        json.encodeToString(stringListSerializer, paths)

    private fun decode(payload: String?): List<String> {
        if (payload.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString(stringListSerializer, payload) }
            .getOrElse { emptyList() }
    }

    companion object {
        private val DEFAULT_JSON = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        private val stringListSerializer = ListSerializer(String.serializer())
    }
}
