package com.subget.app.data.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DesktopSubtitleStorageManager(
    private val baseDir: File = getDefaultDownloadsDirectory()
) : SubtitleStorageManager {

    init {
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
    }

    override suspend fun saveSubtitle(subtitle: ExtractedSubtitle): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!baseDir.exists()) {
                baseDir.mkdirs()
            }
            val targetFile = File(baseDir, subtitle.fileName)
            targetFile.writeBytes(subtitle.content)
            Result.success(targetFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDownloadedSubtitles(): List<DownloadedSubtitle> = withContext(Dispatchers.IO) {
        val list = mutableListOf<DownloadedSubtitle>()
        if (baseDir.exists() && baseDir.isDirectory) {
            val files = baseDir.listFiles() ?: emptyArray()
            for (file in files) {
                if (file.isFile && isSubtitleFile(file.name)) {
                    list.add(
                        DownloadedSubtitle(
                            id = file.absolutePath,
                            fileName = file.name,
                            fileSize = file.length(),
                            formattedSize = formatFileSize(file.length()),
                            lastModified = file.lastModified(),
                            formattedDate = formatDate(file.lastModified())
                        )
                    )
                }
            }
        }
        list.sortedByDescending { it.lastModified }
    }

    override suspend fun deleteSubtitle(item: DownloadedSubtitle): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(item.id)
            if (file.exists()) {
                file.delete()
            } else {
                val directFile = File(baseDir, item.fileName)
                if (directFile.exists()) directFile.delete() else false
            }
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        fun getDefaultDownloadsDirectory(): File {
            val userHome = System.getProperty("user.home") ?: "."
            return File(userHome, "Downloads" + File.separator + "Subget")
        }

        private fun isSubtitleFile(name: String): Boolean {
            val ext = name.substringAfterLast('.', "").lowercase()
            return ext in setOf("srt", "vtt", "sub", "ass")
        }

        fun formatFileSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
                else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
            }
        }

        fun formatDate(timestampMillis: Long): String {
            if (timestampMillis <= 0) return "Recently"
            val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
            return sdf.format(Date(timestampMillis))
        }
    }
}
