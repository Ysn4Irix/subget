package com.subget.app.data.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class AndroidSubtitleStorageManager(private val context: Context) : SubtitleStorageManager {

    override suspend fun saveSubtitle(subtitle: ExtractedSubtitle): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveWithMediaStore(subtitle)
            } else {
                saveWithLegacyStorage(subtitle)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveWithMediaStore(subtitle: ExtractedSubtitle): Result<String> {
        val resolver = context.contentResolver
        val mimeType = getMimeType(subtitle.fileName)

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, subtitle.fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/Subget")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val uri = resolver.insert(collection, contentValues)
            ?: return Result.failure(IOException("Failed to create MediaStore entry for ${subtitle.fileName}"))

        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(subtitle.content)
                outputStream.flush()
            } ?: return Result.failure(IOException("Failed to open output stream for $uri"))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            return Result.success(uri.toString())
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            return Result.failure(e)
        }
    }

    @Suppress("DEPRECATION")
    private fun saveWithLegacyStorage(subtitle: ExtractedSubtitle): Result<String> {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val subgetDir = File(downloadsDir, "Subget")
        if (!subgetDir.exists() && !subgetDir.mkdirs()) {
            return Result.failure(IOException("Failed to create directory: ${subgetDir.absolutePath}"))
        }

        val targetFile = File(subgetDir, subtitle.fileName)
        FileOutputStream(targetFile).use { fos ->
            fos.write(subtitle.content)
            fos.flush()
        }

        val uri = Uri.fromFile(targetFile)
        return Result.success(uri.toString())
    }

    override suspend fun getDownloadedSubtitles(): List<DownloadedSubtitle> = withContext(Dispatchers.IO) {
        val list = mutableListOf<DownloadedSubtitle>()
        val seenNames = mutableSetOf<String>()

        // Scan 1: Direct File access to Download/Subget
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val subgetDir = File(downloadsDir, "Subget")
            if (subgetDir.exists() && subgetDir.isDirectory) {
                val files = subgetDir.listFiles() ?: emptyArray()
                for (file in files) {
                    if (file.isFile && (file.name.endsWith(".srt", ignoreCase = true) || file.name.endsWith(".vtt", ignoreCase = true))) {
                        seenNames.add(file.name)
                        list.add(
                            DownloadedSubtitle(
                                id = Uri.fromFile(file).toString(),
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
        } catch (e: Exception) {
            // MediaStore will cover it
        }

        // Scan 2: MediaStore Query for Download/Subget or app-created downloads
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val projection = arrayOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.SIZE,
                    MediaStore.MediaColumns.DATE_MODIFIED,
                    MediaStore.MediaColumns.RELATIVE_PATH
                )

                val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
                val selectionArgs = arrayOf("Download/Subget%")

                context.contentResolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val name = cursor.getString(nameCol) ?: continue
                        val size = cursor.getLong(sizeCol)
                        val dateSec = cursor.getLong(dateCol)
                        val contentUri = Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString())

                        if (!seenNames.contains(name)) {
                            seenNames.add(name)
                            list.add(
                                DownloadedSubtitle(
                                    id = contentUri.toString(),
                                    fileName = name,
                                    fileSize = size,
                                    formattedSize = formatFileSize(size),
                                    lastModified = dateSec * 1000L,
                                    formattedDate = formatDate(dateSec * 1000L)
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore MediaStore query errors
            }
        }

        list.sortedByDescending { it.lastModified }
    }

    override suspend fun deleteSubtitle(item: DownloadedSubtitle): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(item.id)
            if (uri.scheme == "content") {
                val count = context.contentResolver.delete(uri, null, null)
                if (count > 0) return@withContext true
            }

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val subgetFile = File(File(downloadsDir, "Subget"), item.fileName)
            if (subgetFile.exists()) {
                return@withContext subgetFile.delete()
            }

            if (uri.scheme == "file") {
                uri.path?.let { path ->
                    val f = File(path)
                    if (f.exists()) return@withContext f.delete()
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        fun formatFileSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
                else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
            }
        }

        fun formatDate(timestampMillis: Long): String {
            if (timestampMillis <= 0) return "Recently"
            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy • HH:mm", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(timestampMillis))
        }

        fun getMimeType(fileName: String): String {
            return when (fileName.substringAfterLast('.', "").lowercase()) {
                "srt" -> "application/x-subrip"
                "vtt" -> "text/vtt"
                "sub" -> "text/plain"
                "ass" -> "text/plain"
                else -> "text/plain"
            }
        }
    }
}
