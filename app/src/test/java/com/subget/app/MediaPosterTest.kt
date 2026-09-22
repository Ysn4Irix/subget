package com.subget.app

import com.subget.app.data.api.models.MediaPoster
import com.subget.app.data.api.models.SubdlSubtitleItem
import com.subget.app.ui.screens.search.SearchUiState
import com.subget.app.ui.screens.search.SubtitleSortOption
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPosterTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testMediaPosterSerialization() {
        val poster = MediaPoster(
            title = "Inception",
            year = "2010",
            posterUrl = "https://upload.wikimedia.org/wikipedia/en/7/7f/Inception_ver3.jpg",
            description = "A thief who steals corporate secrets through dream-sharing technology.",
            mediaType = "Movie"
        )

        val str = json.encodeToString(MediaPoster.serializer(), poster)
        val decoded = json.decodeFromString<MediaPoster>(str)

        assertEquals("Inception", decoded.title)
        assertEquals("2010", decoded.year)
        assertEquals("Movie", decoded.mediaType)
        assertNotNull(decoded.posterUrl)
    }

    @Test
    fun testQualityGroupingAndHiFiltering() {
        val sub1 = SubdlSubtitleItem(releaseName = "Inception.2010.2160p.UHD", hi = false)
        val sub2 = SubdlSubtitleItem(releaseName = "Inception.2010.1080p.BluRay", hi = true)
        val sub3 = SubdlSubtitleItem(releaseName = "Inception.2010.720p.WEB-DL", hi = false)
        val sub4 = SubdlSubtitleItem(releaseName = "Inception.Custom.DVDRip", hi = true)

        val allItems = listOf(sub1, sub2, sub3, sub4)

        // Test normal grouping
        val normalState = SearchUiState(subtitles = allItems, hiOnly = false)
        val normalGroups = normalState.filteredAndSortedGroups

        assertTrue(normalGroups.containsKey("4K UHD"))
        assertTrue(normalGroups.containsKey("1080p / BluRay"))
        assertTrue(normalGroups.containsKey("WEB-DL / 720p"))
        assertTrue(normalGroups.containsKey("Other Releases"))
        assertEquals(1, normalGroups["4K UHD"]?.size)

        // Test HI Only filtering
        val hiState = SearchUiState(subtitles = allItems, hiOnly = true)
        val hiGroups = hiState.filteredAndSortedGroups

        assertEquals(2, hiGroups.values.flatten().size)
        assertTrue(hiGroups["1080p / BluRay"]?.any { it.releaseName == "Inception.2010.1080p.BluRay" } == true)
        assertTrue(hiGroups["Other Releases"]?.any { it.releaseName == "Inception.Custom.DVDRip" } == true)
    }

    @Test
    fun testSortOptions() {
        val subA = SubdlSubtitleItem(releaseName = "Alpha Release", season = 1, episode = 1)
        val subZ = SubdlSubtitleItem(releaseName = "Zulu Release", season = 2, episode = 5)

        val stateAlphabetical = SearchUiState(
            subtitles = listOf(subZ, subA),
            sortOption = SubtitleSortOption.ALPHABETICAL
        )
        val alphaOrder = stateAlphabetical.filteredAndSortedGroups.values.flatten()
        assertEquals("Alpha Release", alphaOrder[0].displayTitle)
        assertEquals("Zulu Release", alphaOrder[1].displayTitle)

        val stateNewest = SearchUiState(
            subtitles = listOf(subA, subZ),
            sortOption = SubtitleSortOption.NEWEST
        )
        val newestOrder = stateNewest.filteredAndSortedGroups.values.flatten()
        assertEquals("Zulu Release", newestOrder[0].displayTitle)
        assertEquals("Alpha Release", newestOrder[1].displayTitle)
    }

    @Test
    fun testMediaPosterWithRating() {
        val poster = MediaPoster(
            title = "Inception",
            year = "2010",
            posterUrl = "https://images.metahub.space/poster/small/tt1375666/img",
            description = "A thief who steals corporate secrets.",
            mediaType = "Movie",
            imdbId = "tt1375666",
            rating = "8.8"
        )

        val str = json.encodeToString(MediaPoster.serializer(), poster)
        val decoded = json.decodeFromString<MediaPoster>(str)

        assertEquals("Inception", decoded.title)
        assertEquals("8.8", decoded.rating)
    }

    @Test
    fun testFormatRating() {
        val repo = com.subget.app.data.repository.PosterRepository()
        assertEquals("8.8", repo.formatRating("8.8"))
        assertEquals("7.0", repo.formatRating("7"))
        assertEquals("7.0", repo.formatRating("7.0"))
        assertEquals("8.8", repo.formatRating("8.8/10"))
        assertEquals("5.5", repo.formatRating("5.5"))
        assertEquals(null, repo.formatRating("0"))
        assertEquals(null, repo.formatRating("0.0"))
        assertEquals(null, repo.formatRating("N/A"))
        assertEquals(null, repo.formatRating(null))
        assertEquals(null, repo.formatRating(""))
        assertEquals(null, repo.formatRating("-1"))
        assertEquals(null, repo.formatRating("12.5"))
    }

    @Test
    fun testPosterRepositoryExtractsRatingFromCinemeta() = kotlinx.coroutines.runBlocking {
        val repo = com.subget.app.data.repository.PosterRepository()
        val poster = repo.getMediaPosterByImdbId("tt1375666", type = "movie")
        assertNotNull(poster)
        assertEquals("8.8", poster?.rating)
    }
}
