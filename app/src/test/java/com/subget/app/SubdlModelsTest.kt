package com.subget.app

import com.subget.app.data.api.models.SubdlResponse
import com.subget.app.data.api.models.SubdlSubtitleItem
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubdlModelsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun testSubdlResponseDeserialization() {
        val sampleJson = """
            {
                "status": true,
                "subtitles": [
                    {
                        "release_name": "Inception.2010.1080p.BluRay.x264-SPARKS",
                        "name": "Inception.2010.1080p.BluRay.x264-SPARKS.srt",
                        "lang": "English",
                        "language": "en",
                        "author": "subdl_admin",
                        "url": "/subtitle/312019-3112260.zip",
                        "season": null,
                        "episode": null,
                        "hi": false,
                        "type": "movie"
                    }
                ]
            }
        """.trimIndent()

        val response = json.decodeFromString<SubdlResponse>(sampleJson)

        assertTrue(response.status)
        assertEquals(1, response.allSubtitles.size)

        val item = response.allSubtitles[0]
        assertEquals("Inception.2010.1080p.BluRay.x264-SPARKS", item.displayTitle)
        assertEquals("English", item.displayLanguage)
        assertEquals("https://dl.subdl.com/subtitle/312019-3112260.zip", item.fullDownloadUrl)
        assertFalse(item.hi ?: true)
    }

    @Test
    fun testSubdlErrorResponse() {
        val errorJson = """
            {
                "status": false,
                "error": "Invalid API key"
            }
        """.trimIndent()

        val response = json.decodeFromString<SubdlResponse>(errorJson)

        assertFalse(response.status)
        assertEquals("Invalid API key", response.error)
        assertTrue(response.allSubtitles.isEmpty())
    }

    @Test
    fun testFullDownloadUrlNormalization() {
        val item1 = SubdlSubtitleItem(url = "https://custom.domain.com/sub.zip")
        assertEquals("https://custom.domain.com/sub.zip", item1.fullDownloadUrl)

        val item2 = SubdlSubtitleItem(url = "subtitle/12345.zip")
        assertEquals("https://dl.subdl.com/subtitle/12345.zip", item2.fullDownloadUrl)

        val item3 = SubdlSubtitleItem(url = null, downloadUrl = "https://dl.subdl.com/sub.zip")
        assertEquals("https://dl.subdl.com/sub.zip", item3.fullDownloadUrl)
    }

    @Test
    fun testSubdlResponseWithMatchedMedia() {
        val sampleJson = """
            {
                "status": true,
                "results": [
                    {
                        "sd_id": 12345,
                        "type": "tv",
                        "name": "Monarch: Legacy of Monsters",
                        "imdb_id": "tt17220216",
                        "tmdb_id": 123456,
                        "year": 2023
                    }
                ],
                "subtitles": [
                    {
                        "release_name": "Monarch.Legacy.of.Monsters.S01E01.1080p.WEB-DL",
                        "lang": "English",
                        "url": "/subtitle/111.zip"
                    }
                ]
            }
        """.trimIndent()

        val response = json.decodeFromString<SubdlResponse>(sampleJson)

        assertTrue(response.status)
        assertEquals(1, response.allSubtitles.size)
        org.junit.Assert.assertNotNull(response.primaryMedia)
        assertEquals("Monarch: Legacy of Monsters", response.primaryMedia?.name)
        assertEquals("tt17220216", response.primaryMedia?.imdbId)
        assertEquals("tv", response.primaryMedia?.type)
        assertEquals("2023", response.primaryMedia?.yearString)
    }
}
