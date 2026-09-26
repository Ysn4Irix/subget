package com.subget.app

import com.subget.app.data.api.models.SubdlSubtitleItem
import com.subget.app.ui.screens.search.QualityFilterOption
import com.subget.app.ui.screens.search.SearchUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QualityFilterTest {

    private val sampleItems = listOf(
        SubdlSubtitleItem(
            releaseName = "Inception.2010.2160p.UHD.BluRay.x265",
            lang = "English",
            language = "en",
            url = "/sub1.zip"
        ),
        SubdlSubtitleItem(
            releaseName = "Inception.2010.1080p.BluRay.x264",
            lang = "English",
            language = "en",
            url = "/sub2.zip"
        ),
        SubdlSubtitleItem(
            releaseName = "Inception.2010.720p.WEB-DL",
            lang = "English",
            language = "en",
            url = "/sub3.zip"
        ),
        SubdlSubtitleItem(
            releaseName = "Inception.2010.DVDrip",
            lang = "English",
            language = "en",
            url = "/sub4.zip"
        )
    )

    @Test
    fun testQualityCounts() {
        val state = SearchUiState(subtitles = sampleItems)
        val counts = state.qualityCounts

        assertEquals(4, counts[QualityFilterOption.ALL])
        assertEquals(1, counts[QualityFilterOption.UHD_4K])
        assertEquals(1, counts[QualityFilterOption.BLURAY_1080P])
        assertEquals(1, counts[QualityFilterOption.WEBDL_720P])
        assertEquals(1, counts[QualityFilterOption.OTHER])
    }

    @Test
    fun testQualityFilterAll() {
        val state = SearchUiState(
            subtitles = sampleItems,
            selectedQuality = QualityFilterOption.ALL
        )
        val groups = state.filteredAndSortedGroups

        assertEquals(4, groups.size)
        assertTrue(groups.containsKey("4K UHD"))
        assertTrue(groups.containsKey("1080p / BluRay"))
        assertTrue(groups.containsKey("WEB-DL / 720p"))
        assertTrue(groups.containsKey("Other Releases"))
    }

    @Test
    fun testQualityFilter4K() {
        val state = SearchUiState(
            subtitles = sampleItems,
            selectedQuality = QualityFilterOption.UHD_4K
        )
        val groups = state.filteredAndSortedGroups

        assertEquals(1, groups.size)
        assertTrue(groups.containsKey("4K UHD"))
        assertEquals(1, groups["4K UHD"]?.size)
        assertEquals("Inception.2010.2160p.UHD.BluRay.x265", groups["4K UHD"]?.first()?.displayTitle)
    }

    @Test
    fun testQualityFilter1080p() {
        val state = SearchUiState(
            subtitles = sampleItems,
            selectedQuality = QualityFilterOption.BLURAY_1080P
        )
        val groups = state.filteredAndSortedGroups

        assertEquals(1, groups.size)
        assertTrue(groups.containsKey("1080p / BluRay"))
        assertEquals(1, groups["1080p / BluRay"]?.size)
    }
}
