package com.subget.app.data.storage

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SubtitleStorageManager(private val context: Context) {

    suspend fun saveSubtitle(subtitle: ExtractedSubtitle): Result<Uri> = withContext(Dispatchers.IO) {
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

    private fun saveWithMediaStore(subtitle: ExtractedSubtitle): Result<Uri> {
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

            return Result.success(uri)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            return Result.failure(e)
        }
    }

    @Suppress("DEPRECATION")
    private fun saveWithLegacyStorage(subtitle: ExtractedSubtitle): Result<Uri> {
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
        return Result.success(uri)
    }

    suspend fun getDownloadedSubtitles(): List<DownloadedSubtitle> = withContext(Dispatchers.IO) {
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
                                uri = Uri.fromFile(file),
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
                                    uri = contentUri,
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

    suspend fun deleteSubtitle(item: DownloadedSubtitle): Boolean = withContext(Dispatchers.IO) {
        try {
            // Attempt ContentResolver delete
            if (item.uri.scheme == "content") {
                val count = context.contentResolver.delete(item.uri, null, null)
                if (count > 0) return@withContext true
            }

            // Attempt direct File delete
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val subgetFile = File(File(downloadsDir, "Subget"), item.fileName)
            if (subgetFile.exists()) {
                return@withContext subgetFile.delete()
            }

            if (item.uri.scheme == "file") {
                item.uri.path?.let { path ->
                    val f = File(path)
                    if (f.exists()) return@withContext f.delete()
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    fun createShareIntent(uri: Uri, fileName: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = getMimeType(fileName)
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun createViewIntent(uri: Uri, fileName: String): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, getMimeType(fileName))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        }
    }

    private fun formatDate(timestampMillis: Long): String {
        if (timestampMillis <= 0) return "Recently"
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy • HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestampMillis))
    }

    private fun getMimeType(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "srt" -> "application/x-subrip"
            "vtt" -> "text/vtt"
            "sub" -> "text/plain"
            "ass" -> "text/plain"
            else -> "text/plain"
        }
    }
}

data class DownloadedSubtitle(
    val uri: Uri,
    val fileName: String,
    val fileSize: Long,
    val formattedSize: String,
    val lastModified: Long,
    val formattedDate: String
)
