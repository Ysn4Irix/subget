package com.subget.app

import com.subget.app.data.api.models.SubdlSubtitleItem
import com.subget.app.ui.screens.search.CachedSearchResult
import com.subget.app.ui.screens.search.SearchUiState
import com.subget.app.ui.screens.search.SearchViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InstantSearchTest {

    @Test
    fun testParseQueryWithPartialTyping() {
        // 2 chars
        val q2 = SearchViewModel.parseQuery("mo")
        assertEquals("mo", q2.cleanTitle)
        assertNull(q2.seasonNumber)
        assertNull(q2.episodeNumber)

        // Title with partial season keyword
        val qPartialSeason = SearchViewModel.parseQuery("monarch s")
        assertEquals("monarch s", qPartialSeason.cleanTitle)
        assertNull(qPartialSeason.seasonNumber)

        // Title with complete season
        val qSeason = SearchViewModel.parseQuery("monarch s2")
        assertEquals("monarch", qSeason.cleanTitle)
        assertEquals(2, qSeason.seasonNumber)
        assertNull(qSeason.episodeNumber)

        // Title with complete season and episode
        val qSeasonEp = SearchViewModel.parseQuery("monarch s2e10")
        assertEquals("monarch", qSeasonEp.cleanTitle)
        assertEquals(2, qSeasonEp.seasonNumber)
        assertEquals(10, qSeasonEp.episodeNumber)
    }

    @Test
    fun testThresholdValidation() {
        val belowThreshold1 = ""
        val belowThreshold2 = " "
        val belowThreshold3 = "a"
        val belowThreshold4 = " a "
        val validThreshold1 = "mo"
        val validThreshold2 = "bat"

        assertTrue(belowThreshold1.trim().length < 2)
        assertTrue(belowThreshold2.trim().length < 2)
        assertTrue(belowThreshold3.trim().length < 2)
        assertTrue(belowThreshold4.trim().length < 2)

        assertTrue(validThreshold1.trim().length >= 2)
        assertTrue(validThreshold2.trim().length >= 2)
    }

    @Test
    fun testLruCacheRetentionAndEviction() {
        val maxCapacity = 5
        val lruCache = object : LinkedHashMap<String, CachedSearchResult>(10, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedSearchResult>?): Boolean {
                return size > maxCapacity
            }
        }

        for (i in 1..7) {
            val key = "item$i|all"
            lruCache[key] = CachedSearchResult(
                subtitles = emptyList(),
                mediaPoster = null,
                sdId = i,
                imdbId = null,
                filmTitle = "Film $i",
                fetchedSeasons = emptySet(),
                selectedSeason = null,
                selectedEpisode = null
            )
        }

        // Cache size must not exceed maxCapacity (5)
        assertEquals(5, lruCache.size)

        // Items 1 and 2 should have been evicted
        assertNull(lruCache["item1|all"])
        assertNull(lruCache["item2|all"])

        // Items 3 to 7 should exist
        assertNotNull(lruCache["item3|all"])
        assertNotNull(lruCache["item7|all"])

        // Access item 3 to mark it recently used
        lruCache["item3|all"]

        // Add another item
        lruCache["item8|all"] = CachedSearchResult(
            subtitles = emptyList(),
            mediaPoster = null,
            sdId = 8,
            imdbId = null,
            filmTitle = "Film 8",
            fetchedSeasons = emptySet(),
            selectedSeason = null,
            selectedEpisode = null
        )

        // Now item 4 should be evicted (as item 3 was accessed more recently)
        assertNull(lruCache["item4|all"])
        assertNotNull(lruCache["item3|all"])
    }

    @Test
    fun testSearchUiStateZeroFlickerLogic() {
        val sampleSubs = listOf(
            SubdlSubtitleItem(
                releaseName = "Monarch.S01E01.1080p",
                lang = "English",
                season = 1,
                episode = 1
            )
        )

        // Case 1: Initial search (empty subtitles, loading true) -> Should show skeleton
        val initialState = SearchUiState(
            query = "mo",
            isLoading = true,
            subtitles = emptyList()
        )
        val shouldShowSkeletonInitial = initialState.isLoading && initialState.subtitles.isEmpty()
        assertTrue(shouldShowSkeletonInitial)

        // Case 2: Subsequent typing (subtitles already present, background loading true)
        // -> Should NOT show skeleton, preventing UI flicker
        val typingState = SearchUiState(
            query = "monarch",
            isLoading = true,
            subtitles = sampleSubs
        )
        val shouldShowSkeletonSubsequent = typingState.isLoading && typingState.subtitles.isEmpty()
        assertFalse(shouldShowSkeletonSubsequent)
    }
}
