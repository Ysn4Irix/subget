package com.subget.app.data.storage

import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class ExtractedSubtitle(
    val fileName: String,
    val content: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExtractedSubtitle
        return fileName == other.fileName && content.contentEquals(other.content)
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + content.contentHashCode()
        return result
    }
}

object ZipExtractor {

    private val SUBTITLE_EXTENSIONS = setOf("srt", "vtt", "sub", "ass")

    fun extractSubtitles(zipBytes: ByteArray): List<ExtractedSubtitle> {
        val extracted = mutableListOf<ExtractedSubtitle>()

        try {
            ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        // Zip Slip defense: use only the base filename
                        val fileName = File(entry.name).name
                        val extension = fileName.substringAfterLast('.', "").lowercase()

                        if (extension in SUBTITLE_EXTENSIONS && fileName.isNotBlank()) {
                            val content = zis.readBytes()
                            if (content.isNotEmpty()) {
                                extracted.add(ExtractedSubtitle(fileName, content))
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return extracted
    }
}
