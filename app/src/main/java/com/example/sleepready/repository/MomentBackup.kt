package com.example.sleepready.repository

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.sleepready.domain.model.Moment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 时光数据导出。把 moment_item 全部条目 + 私有目录下的图片 打包成 zip 写入
 * 系统下载目录（API 29+ MediaStore；旧版回退 filesDir/exports/）。
 *
 * zip 结构：
 *   moments.json              （数组，每元素含 title/body/eventTimeMillis/createdAt/imageNames）
 *   images/<uuid>.jpg
 */
object MomentBackup {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun exportToDownloads(context: Context, repo: MomentRepository): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val moments = repo.getAll()
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(System.currentTimeMillis())
                val filename = "shike_moments_$timestamp.zip"

                val output = openDownloadStream(context, filename)
                    ?: return@withContext Result.failure(Throwable("无法写入下载目录"))

                ZipOutputStream(output).use { zip ->
                    val payload = json.encodeToString(
                        ListSerializer(MomentDto.serializer()),
                        moments.map { MomentDto.from(it) }
                    )
                    zip.putNextEntry(ZipEntry("moments.json"))
                    zip.write(payload.toByteArray())
                    zip.closeEntry()

                    val collected = mutableSetOf<String>()
                    moments.forEach { m ->
                        m.imagePaths.forEach { path ->
                            val f = File(path)
                            if (f.exists() && collected.add(f.name)) {
                                zip.putNextEntry(ZipEntry("images/${f.name}"))
                                FileInputStream(f).use { it.copyTo(zip) }
                                zip.closeEntry()
                            }
                        }
                    }
                }
                Result.success(filename)
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    private fun openDownloadStream(context: Context, filename: String): OutputStream? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return null
            resolver.openOutputStream(uri)
        } else {
            val dir = File(context.filesDir, "exports").apply { mkdirs() }
            File(dir, filename).outputStream()
        }
    }

    @kotlinx.serialization.Serializable
    private data class MomentDto(
        val id: Long,
        val title: String,
        val body: String?,
        val eventTimeMillis: Long?,
        val imageNames: List<String>,
        val createdAt: Long,
        val updatedAt: Long
    ) {
        companion object {
            fun from(m: Moment): MomentDto = MomentDto(
                id = m.id,
                title = m.title,
                body = m.body,
                eventTimeMillis = m.eventTimeMillis,
                imageNames = m.imagePaths.map { File(it).name },
                createdAt = m.createdAt,
                updatedAt = m.updatedAt
            )
        }
    }
}
