package com.subget.app.data.storage

data class DownloadedSubtitle(
    val id: String,
    val fileName: String,
    val fileSize: Long,
    val formattedSize: String,
    val lastModified: Long,
    val formattedDate: String
)

interface SubtitleStorageManager {
    suspend fun saveSubtitle(subtitle: ExtractedSubtitle): Result<String>
    suspend fun getDownloadedSubtitles(): List<DownloadedSubtitle>
    suspend fun deleteSubtitle(item: DownloadedSubtitle): Boolean
}
