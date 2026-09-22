package com.subget.app

import com.subget.app.data.api.models.SubdlSubtitleItem
import com.subget.app.ui.screens.search.QualityFilterOption
import com.subget.app.ui.screens.search.SearchUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeasonEpisodeFilterTest {

    @Test
    fun testSeasonEpisodeRegexExtraction() {
        // SxxExx format
        val item1 = SubdlSubtitleItem(releaseName = "Severance.S01E07.1080p.WEB-DL.x264")
        assertEquals(1, item1.effectiveSeason)
        assertEquals(7, item1.effectiveEpisode)
        assertEquals("S01E07", item1.formattedSeasonEpisode)

        // NxNN format
        val item2 = SubdlSubtitleItem(releaseName = "The.Bear.2x04.Honeydew.720p.mkv")
        assertEquals(2, item2.effectiveSeason)
        assertEquals(4, item2.effectiveEpisode)
        assertEquals("S02E04", item2.formattedSeasonEpisode)

        // Season and Episode words format
        val item3 = SubdlSubtitleItem(releaseName = "Loki Season 2 Episode 3 1080p")
        assertEquals(2, item3.effectiveSeason)
        assertEquals(3, item3.effectiveEpisode)
        assertEquals("S02E03", item3.formattedSeasonEpisode)

        // Season pack (no episode)
        val item4 = SubdlSubtitleItem(releaseName = "Breaking.Bad.S05.Complete.BluRay")
        assertEquals(5, item4.effectiveSeason)
        assertNull(item4.effectiveEpisode)
        assertEquals("Season 5", item4.formattedSeasonEpisode)

        // API fields precedence over title
        val item5 = SubdlSubtitleItem(
            releaseName = "Custom.Title.Without.Tags",
            season = 3,
            episode = 9
        )
        assertEquals(3, item5.effectiveSeason)
        assertEquals(9, item5.effectiveEpisode)
        assertEquals("S03E09", item5.formattedSeasonEpisode)

        // Movie (no season or episode)
        val movie = SubdlSubtitleItem(releaseName = "Inception.2010.1080p.BluRay.x264")
        assertNull(movie.effectiveSeason)
        assertNull(movie.effectiveEpisode)
        assertNull(movie.formattedSeasonEpisode)
    }

    private val sampleSeriesItems = listOf(
        SubdlSubtitleItem(
            releaseName = "Severance.S01E01.1080p.WEB-DL",
            lang = "English",
            season = 1,
            episode = 1
        ),
        SubdlSubtitleItem(
            releaseName = "Severance.S01E01.720p.WEB-DL",
            lang = "English",
            season = 1,
            episode = 1
        ),
        SubdlSubtitleItem(
            releaseName = "Severance.S01E02.1080p.WEB-DL",
            lang = "English",
            season = 1,
            episode = 2,
            hi = true
        ),
        SubdlSubtitleItem(
            releaseName = "Severance.S02E01.2160p.UHD.HDR",
            lang = "English",
            season = 2,
            episode = 1
        ),
        SubdlSubtitleItem(
            releaseName = "Severance.S02E02.1080p.WEB-DL",
            lang = "English",
            season = 2,
            episode = 2
        )
    )

    @Test
    fun testAvailableSeasonsAndEpisodes() {
        val stateAll = SearchUiState(subtitles = sampleSeriesItems)
        assertEquals(listOf(1, 2), stateAll.availableSeasons)
        assertEquals(listOf(1, 2), stateAll.availableEpisodes)

        // Scope to Season 1
        val stateS1 = SearchUiState(subtitles = sampleSeriesItems, selectedSeason = 1)
        assertEquals(listOf(1, 2), stateS1.availableEpisodes)

        // Scope to Season 2
        val stateS2 = SearchUiState(subtitles = sampleSeriesItems, selectedSeason = 2)
        assertEquals(listOf(1, 2), stateS2.availableEpisodes)
    }

    @Test
    fun testSeasonCounts() {
        val state = SearchUiState(subtitles = sampleSeriesItems)
        val counts = state.seasonCounts

        assertEquals(5, counts[null]) // All seasons
        assertEquals(3, counts[1])    // Season 1: 3 releases
        assertEquals(2, counts[2])    // Season 2: 2 releases
    }

    @Test
    fun testEpisodeCountsCascading() {
        // Season 1 selected
        val stateS1 = SearchUiState(subtitles = sampleSeriesItems, selectedSeason = 1)
        val countsS1 = stateS1.episodeCounts

        assertEquals(3, countsS1[null]) // All episodes in S1
        assertEquals(2, countsS1[1])    // S01E01 has 2 releases (1080p + 720p)
        assertEquals(1, countsS1[2])    // S01E02 has 1 release
    }

    @Test
    fun testFilterBySeasonOnly() {
        val state = SearchUiState(
            subtitles = sampleSeriesItems,
            selectedSeason = 1
        )
        val groups = state.filteredAndSortedGroups

        val totalItems = groups.values.sumOf { it.size }
        assertEquals(3, totalItems)
        assertTrue(groups.values.flatten().all { it.effectiveSeason == 1 })
    }

    @Test
    fun testFilterBySeasonAndEpisode() {
        val state = SearchUiState(
            subtitles = sampleSeriesItems,
            selectedSeason = 1,
            selectedEpisode = 1
        )
        val groups = state.filteredAndSortedGroups

        val totalItems = groups.values.sumOf { it.size }
        assertEquals(2, totalItems) // S01E01 1080p and 720p
        assertTrue(groups.values.flatten().all { it.effectiveSeason == 1 && it.effectiveEpisode == 1 })
    }

    @Test
    fun testCombinedSeasonQualityAndHiFilter() {
        val state = SearchUiState(
            subtitles = sampleSeriesItems,
            selectedSeason = 1,
            selectedEpisode = 2,
            hiOnly = true,
            selectedQuality = QualityFilterOption.BLURAY_1080P
        )
        val groups = state.filteredAndSortedGroups

        val totalItems = groups.values.sumOf { it.size }
        assertEquals(1, totalItems)
        val item = groups.values.flatten().first()
        assertEquals(1, item.effectiveSeason)
        assertEquals(2, item.effectiveEpisode)
        assertEquals(true, item.hi)
    }

    @Test
    fun testParseQuery() {
        // Season word
        val p1 = com.subget.app.ui.screens.search.SearchViewModel.parseQuery("monarch legacy of monsters season 2")
        assertEquals("monarch legacy of monsters", p1.cleanTitle)
        assertEquals(2, p1.seasonNumber)
        assertNull(p1.episodeNumber)

        // Sxx format
        val p2 = com.subget.app.ui.screens.search.SearchViewModel.parseQuery("monarch legacy of monsters s2")
        assertEquals("monarch legacy of monsters", p2.cleanTitle)
        assertEquals(2, p2.seasonNumber)
        assertNull(p2.episodeNumber)

        // SxxExx format
        val p3 = com.subget.app.ui.screens.search.SearchViewModel.parseQuery("monarch legacy of monsters s02e04")
        assertEquals("monarch legacy of monsters", p3.cleanTitle)
        assertEquals(2, p3.seasonNumber)
        assertEquals(4, p3.episodeNumber)

        // Season and Episode words
        val p4 = com.subget.app.ui.screens.search.SearchViewModel.parseQuery("severance season 1 episode 7")
        assertEquals("severance", p4.cleanTitle)
        assertEquals(1, p4.seasonNumber)
        assertEquals(7, p4.episodeNumber)

        // NxNN format
        val p5 = com.subget.app.ui.screens.search.SearchViewModel.parseQuery("breaking bad 2x04")
        assertEquals("breaking bad", p5.cleanTitle)
        assertEquals(2, p5.seasonNumber)
        assertEquals(4, p5.episodeNumber)

        // Movie sequel - must NOT match season
        val p6 = com.subget.app.ui.screens.search.SearchViewModel.parseQuery("avatar 2")
        assertEquals("avatar 2", p6.cleanTitle)
        assertNull(p6.seasonNumber)
        assertNull(p6.episodeNumber)
    }

    @Test
    fun testMultiSeasonMergingFromMediaPoster() {
        // Initial state: only Season 1 subtitles returned by SubDL
        val s1OnlySubtitles = sampleSeriesItems.filter { it.effectiveSeason == 1 }
        
        // MediaPoster from Cinemeta has both Season 1 and Season 2
        val poster = com.subget.app.data.api.models.MediaPoster(
            title = "Monarch: Legacy of Monsters",
            seriesSeasons = listOf(1, 2)
        )

        val state = SearchUiState(
            subtitles = s1OnlySubtitles,
            mediaPoster = poster
        )

        // availableSeasons must include BOTH Season 1 and Season 2
        assertEquals(listOf(1, 2), state.availableSeasons)

        // Season counts shows 3 items in Season 1, 0 items in Season 2 initially
        val counts = state.seasonCounts
        assertEquals(3, counts[1])
        assertEquals(0, counts[2])

        // After Season 2 subtitles are fetched and merged
        val mergedSubtitles = s1OnlySubtitles + sampleSeriesItems.filter { it.effectiveSeason == 2 }
        val updatedState = state.copy(subtitles = mergedSubtitles)

        assertEquals(listOf(1, 2), updatedState.availableSeasons)
        val updatedCounts = updatedState.seasonCounts
        assertEquals(3, updatedCounts[1])
        assertEquals(2, updatedCounts[2])
    }
}
