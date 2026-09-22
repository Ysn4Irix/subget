package com.subget.app

import com.subget.app.data.api.models.SearchSuggestion
import com.subget.app.ui.screens.search.SearchUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionsTest {

    @Test
    fun testSearchSuggestionDataModel() {
        val suggestion = SearchSuggestion(
            title = "Monarch: Legacy of Monsters",
            year = "2023",
            mediaType = "TV Series",
            imdbId = "tt17220216",
            posterUrl = "https://images.metahub.space/poster/medium/tt17220216/img"
        )

        assertEquals("Monarch: Legacy of Monsters", suggestion.title)
        assertEquals("2023", suggestion.year)
        assertEquals("TV Series", suggestion.mediaType)
        assertEquals("tt17220216", suggestion.imdbId)
        assertNotNull(suggestion.posterUrl)
        assertTrue(suggestion.posterUrl!!.startsWith("https://"))
    }

    @Test
    fun testSearchSuggestionOptionalDefaults() {
        val minimal = SearchSuggestion(title = "Inception")

        assertEquals("Inception", minimal.title)
        assertNull(minimal.year)
        assertNull(minimal.mediaType)
        assertNull(minimal.imdbId)
        assertNull(minimal.posterUrl)
    }

    @Test
    fun testSuggestionsTriggerThreshold() {
        fun shouldFetchSuggestions(query: String): Boolean {
            return query.trim().length >= 2
        }

        assertFalse(shouldFetchSuggestions(""))
        assertFalse(shouldFetchSuggestions(" "))
        assertFalse(shouldFetchSuggestions("m"))
        assertFalse(shouldFetchSuggestions(" m "))

        assertTrue(shouldFetchSuggestions("mo"))
        assertTrue(shouldFetchSuggestions("mon"))
        assertTrue(shouldFetchSuggestions("monarch"))
    }

    @Test
    fun testSearchUiStateSuggestionsLifecycle() {
        // Initial state
        val initial = SearchUiState()
        assertFalse(initial.showSuggestions)
        assertFalse(initial.isSuggestionsLoading)
        assertTrue(initial.suggestions.isEmpty())

        // Suggestions loading state while typing
        val loading = initial.copy(
            query = "mo",
            isSuggestionsLoading = true
        )
        assertTrue(loading.isSuggestionsLoading)
        assertFalse(loading.showSuggestions)

        // Suggestions arrived
        val suggestionsList = listOf(
            SearchSuggestion(title = "Monarch: Legacy of Monsters", year = "2023", mediaType = "TV Series", imdbId = "tt17220216"),
            SearchSuggestion(title = "Moana", year = "2016", mediaType = "Movie", imdbId = "tt3521164")
        )
        val loaded = loading.copy(
            suggestions = suggestionsList,
            showSuggestions = true,
            isSuggestionsLoading = false
        )
        assertFalse(loaded.isSuggestionsLoading)
        assertTrue(loaded.showSuggestions)
        assertEquals(2, loaded.suggestions.size)

        // Suggestion selected by user (search input cleared, suggestions closed)
        val selected = loaded.copy(
            query = "",
            showSuggestions = false,
            suggestions = emptyList()
        )
        assertFalse(selected.showSuggestions)
        assertTrue(selected.suggestions.isEmpty())
        assertEquals("", selected.query)
    }

    @Test
    fun testSuggestionsDismissal() {
        val active = SearchUiState(
            query = "mo",
            showSuggestions = true,
            suggestions = listOf(
                SearchSuggestion(title = "Moana", year = "2016", mediaType = "Movie")
            )
        )
        assertTrue(active.showSuggestions)

        val dismissed = active.copy(showSuggestions = false)
        assertFalse(dismissed.showSuggestions)
        // Suggestions list retained or dismissed smoothly
        assertEquals(1, dismissed.suggestions.size)
    }
}
