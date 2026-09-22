package com.subget.app

import com.subget.app.data.storage.ZipExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipExtractorTest {

    @Test
    fun testExtractSubtitlesFiltersAndCleansNames() {
        val srtContent = "1\n00:00:01,000 --> 00:00:04,000\nHello World!".toByteArray()
        val vttContent = "WEBVTT\n\n00:01.000 --> 00:04.000\nSubtitle in VTT".toByteArray()
        val nfoContent = "Movie info release".toByteArray()

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            // Subtitle entry 1
            zos.putNextEntry(ZipEntry("movie.1080p.srt"))
            zos.write(srtContent)
            zos.closeEntry()

            // Subtitle entry 2 with directory path (testing Zip Slip defense)
            zos.putNextEntry(ZipEntry("../../nested/folder/movie.vtt"))
            zos.write(vttContent)
            zos.closeEntry()

            // Non-subtitle file
            zos.putNextEntry(ZipEntry("info.nfo"))
            zos.write(nfoContent)
            zos.closeEntry()
        }

        val zipBytes = baos.toByteArray()
        val extracted = ZipExtractor.extractSubtitles(zipBytes)

        assertEquals(2, extracted.size)

        val first = extracted.first { it.fileName == "movie.1080p.srt" }
        assertEquals(String(srtContent), String(first.content))

        // Ensure Zip Slip attempt is stripped to only the leaf name "movie.vtt"
        val second = extracted.first { it.fileName == "movie.vtt" }
        assertEquals(String(vttContent), String(second.content))
    }

    @Test
    fun testEmptyOrInvalidZipReturnsEmptyList() {
        val invalidBytes = "Not a zip file".toByteArray()
        val extracted = ZipExtractor.extractSubtitles(invalidBytes)
        assertTrue(extracted.isEmpty())
    }
}
