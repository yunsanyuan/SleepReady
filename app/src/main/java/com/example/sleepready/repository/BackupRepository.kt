package com.example.sleepready.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.sleepready.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 全库备份与恢复（v7）。
 *
 * 备份内容：
 *   db/sleep_ready.db          Room 数据库主文件（含全部 7 张表）
 *   db/sleep_ready.db-wal      WAL 日志（如有）
 *   db/sleep_ready.db-shm      共享内存（如有）
 *   files/moments/[uuid].jpg   时光图片
 *   files/knowledge/[uuid].jpg 心动图片
 *
 * 备份输出文件名：`shike_full_<yyyyMMdd_HHmmss>.zip`，写入系统下载目录
 * （API 29+ 通过 MediaStore.Downloads；旧版回退到 filesDir/exports/）。
 *
 * 恢复流程：
 *   1. 用户用 ACTION_OPEN_DOCUMENT 选 zip
 *   2. 校验包含 db/sleep_ready.db
 *   3. 关闭 Room 数据库（清 [DatabaseRef]）
 *   4. 替换 db 三件套
 *   5. 清空并解压 files/moments + files/knowledge
 *   6. 调用方负责强杀进程让用户重新打开应用
 */
object BackupRepository {

    private const val DB_PREFIX = "db/"
    private const val FILES_PREFIX = "files/"
    private val IMAGE_DIRS = listOf("moments", "knowledge")

    /**
     * 导出整库到下载目录。返回最终文件名（成功）或 Throwable（失败）。
     */
    suspend fun exportFullBackup(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(System.currentTimeMillis())
            val filename = "shike_full_$timestamp.zip"
            val output = openDownloadStream(context, filename)
                ?: return@withContext Result.failure(Throwable("无法写入下载目录"))

            // 关闭数据库以确保 WAL 已 checkpoint
            try { DatabaseRef.closeIfOpen() } catch (_: Throwable) {}

            ZipOutputStream(output).use { zip ->
                // db 三件套
                val dbFile = context.getDatabasePath(AppDatabase.DB_NAME)
                if (dbFile.exists()) zipFile(zip, dbFile, DB_PREFIX + AppDatabase.DB_NAME)
                File(dbFile.absolutePath + "-wal").takeIf { it.exists() }?.let {
                    zipFile(zip, it, DB_PREFIX + AppDatabase.DB_NAME + "-wal")
                }
                File(dbFile.absolutePath + "-shm").takeIf { it.exists() }?.let {
                    zipFile(zip, it, DB_PREFIX + AppDatabase.DB_NAME + "-shm")
                }

                // 私有目录下的图片
                for (sub in IMAGE_DIRS) {
                    val dir = File(context.filesDir, sub)
                    if (!dir.isDirectory) continue
                    dir.listFiles()?.forEach { f ->
                        if (f.isFile) zipFile(zip, f, FILES_PREFIX + sub + "/" + f.name)
                    }
                }
            }
            Result.success(filename)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /**
     * 从用户选择的 zip 恢复。成功后请由调用方杀进程让用户重新打开。
     */
    suspend fun importFullBackup(context: Context, zipUri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                // 先把 zip 复制到临时文件，避免一边读一边覆盖目标
                val tmpZip = File(context.cacheDir, "import_backup_${System.currentTimeMillis()}.zip")
                resolver.openInputStream(zipUri)?.use { input ->
                    FileOutputStream(tmpZip).use { out -> input.copyTo(out) }
                } ?: return@withContext Result.failure(Throwable("无法读取所选文件"))

                // 校验：必须包含 db/sleep_ready.db
                if (!hasEntry(tmpZip, DB_PREFIX + AppDatabase.DB_NAME)) {
                    tmpZip.delete()
                    return@withContext Result.failure(Throwable("不是有效的拾刻备份"))
                }

                // 关闭数据库
                try { DatabaseRef.closeIfOpen() } catch (_: Throwable) {}

                // 删除当前 db 三件套（防止 wal/shm 残留与新 db 不一致）
                val dbFile = context.getDatabasePath(AppDatabase.DB_NAME)
                listOf(
                    dbFile,
                    File(dbFile.absolutePath + "-wal"),
                    File(dbFile.absolutePath + "-shm"),
                    File(dbFile.absolutePath + "-journal")
                ).forEach { runCatching { if (it.exists()) it.delete() } }

                // 清空图片目录（恢复时直接以 zip 为准）
                IMAGE_DIRS.forEach { sub ->
                    val dir = File(context.filesDir, sub)
                    if (dir.isDirectory) {
                        dir.listFiles()?.forEach { runCatching { it.delete() } }
                    }
                }

                // 解压
                ZipInputStream(FileInputStream(tmpZip)).use { zin ->
                    var entry: ZipEntry? = zin.nextEntry
                    while (entry != null) {
                        val target: File? = when {
                            entry.name.startsWith(DB_PREFIX) -> {
                                val name = entry.name.removePrefix(DB_PREFIX)
                                File(dbFile.parentFile, name)
                            }
                            entry.name.startsWith(FILES_PREFIX) -> {
                                val rel = entry.name.removePrefix(FILES_PREFIX)
                                File(context.filesDir, rel)
                            }
                            else -> null
                        }
                        if (target != null && !entry.isDirectory) {
                            target.parentFile?.mkdirs()
                            FileOutputStream(target).use { out -> zin.copyTo(out) }
                        }
                        zin.closeEntry()
                        entry = zin.nextEntry
                    }
                }
                tmpZip.delete()
                Result.success(Unit)
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    private fun hasEntry(zip: File, name: String): Boolean {
        return try {
            ZipInputStream(FileInputStream(zip)).use { zin ->
                var e: ZipEntry? = zin.nextEntry
                while (e != null) {
                    if (e.name == name) return@use true
                    zin.closeEntry()
                    e = zin.nextEntry
                }
                false
            }
        } catch (_: Throwable) { false }
    }

    private fun zipFile(zip: ZipOutputStream, file: File, entryName: String) {
        zip.putNextEntry(ZipEntry(entryName))
        FileInputStream(file).use { it.copyTo(zip) }
        zip.closeEntry()
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
}

/**
 * Room 数据库的进程级引用持有者。在备份/恢复前关闭数据库，避免文件被锁。
 */
object DatabaseRef {
    @Volatile
    private var db: AppDatabase? = null

    fun set(database: AppDatabase) { db = database }

    fun closeIfOpen() {
        try {
            db?.close()
        } finally {
            db = null
        }
    }
}
