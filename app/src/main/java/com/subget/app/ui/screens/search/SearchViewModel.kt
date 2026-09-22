package com.subget.app.ui.screens.search

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subget.app.data.api.models.MediaPoster
import com.subget.app.data.api.models.SubdlSubtitleItem
import com.subget.app.data.repository.PosterRepository
import com.subget.app.data.repository.SettingsRepository
import com.subget.app.data.repository.SubtitleRepository
import com.subget.app.data.storage.SubtitleStorageManager
import com.subget.app.data.storage.ZipExtractor
import com.subget.app.ui.components.DownloadStatus
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

data class CachedSearchResult(
    val subtitles: List<SubdlSubtitleItem>,
    val mediaPoster: MediaPoster?,
    val sdId: Int?,
    val imdbId: String?,
    val filmTitle: String?,
    val fetchedSeasons: Set<Int>,
    val selectedSeason: Int?,
    val selectedEpisode: Int?
)

enum class SubtitleSortOption(val displayName: String) {
    BEST_MATCH("Best Match"),
    NEWEST("Newest"),
    ALPHABETICAL("A-Z")
}

enum class QualityFilterOption(
    val label: String,
    val dropdownLabel: String,
    val tierName: String?
) {
    ALL("Quality", "All Qualities", null),
    UHD_4K("4K UHD", "4K UHD", "4K UHD"),
    BLURAY_1080P("1080p", "1080p / BluRay", "1080p / BluRay"),
    WEBDL_720P("720p", "720p / WEB-DL", "WEB-DL / 720p"),
    OTHER("Other", "Other Releases", "Other Releases")
}

data class SearchUiState(
    val query: String = "",
    val selectedLanguage: String? = null,
    val isLoading: Boolean = false,
    val isPosterLoading: Boolean = false,
    val subtitles: List<SubdlSubtitleItem> = emptyList(),
    val mediaPoster: MediaPoster? = null,
    val recentSearches: List<String> = emptyList(),
    val sortOption: SubtitleSortOption = SubtitleSortOption.BEST_MATCH,
    val hiOnly: Boolean = false,
    val selectedQuality: QualityFilterOption = QualityFilterOption.ALL,
    val selectedSeason: Int? = null,
    val selectedEpisode: Int? = null,
    val selectedSubtitleForDetails: SubdlSubtitleItem? = null,
    val errorMessage: String? = null,
    val hasApiKey: Boolean = false,
    val isFirstLaunch: Boolean = false,
    val downloadStatuses: Map<String, DownloadStatus> = emptyMap(),
    val savedUris: Map<String, Uri> = emptyMap(),
    val snackbarMessage: String? = null
) {
    val availableSeasons: List<Int>
        get() {
            val fromSubs = subtitles.mapNotNull { it.effectiveSeason }
            val fromPoster = mediaPoster?.seriesSeasons ?: emptyList()
            return (fromSubs + fromPoster).distinct().sorted()
        }

    val availableEpisodes: List<Int>
        get() {
            val items = if (selectedSeason != null) {
                subtitles.filter { it.effectiveSeason == selectedSeason }
            } else {
                subtitles
            }
            return items.mapNotNull { it.effectiveEpisode }.distinct().sorted()
        }

    val seasonCounts: Map<Int?, Int>
        get() {
            var items = subtitles
            if (hiOnly) items = items.filter { it.hi == true }
            if (selectedQuality != QualityFilterOption.ALL) {
                items = items.filter { item ->
                    val title = item.displayTitle.uppercase()
                    val tier = when {
                        title.contains("2160P") || title.contains("4K") || title.contains("UHD") -> "4K UHD"
                        title.contains("1080P") || title.contains("BLURAY") || title.contains("BLU-RAY") -> "1080p / BluRay"
                        title.contains("720P") || title.contains("WEB-DL") || title.contains("WEBDL") || title.contains("WEBRIP") -> "WEB-DL / 720p"
                        else -> "Other Releases"
                    }
                    tier == selectedQuality.tierName
                }
            }
            val map = mutableMapOf<Int?, Int>()
            map[null] = items.size
            for (season in availableSeasons) {
                map[season] = items.count { it.effectiveSeason == season }
            }
            return map
        }

    val episodeCounts: Map<Int?, Int>
        get() {
            var items = subtitles
            if (hiOnly) items = items.filter { it.hi == true }
            if (selectedSeason != null) {
                items = items.filter { it.effectiveSeason == selectedSeason }
            }
            if (selectedQuality != QualityFilterOption.ALL) {
                items = items.filter { item ->
                    val title = item.displayTitle.uppercase()
                    val tier = when {
                        title.contains("2160P") || title.contains("4K") || title.contains("UHD") -> "4K UHD"
                        title.contains("1080P") || title.contains("BLURAY") || title.contains("BLU-RAY") -> "1080p / BluRay"
                        title.contains("720P") || title.contains("WEB-DL") || title.contains("WEBDL") || title.contains("WEBRIP") -> "WEB-DL / 720p"
                        else -> "Other Releases"
                    }
                    tier == selectedQuality.tierName
                }
            }
            val map = mutableMapOf<Int?, Int>()
            map[null] = items.size
            for (episode in availableEpisodes) {
                map[episode] = items.count { it.effectiveEpisode == episode }
            }
            return map
        }

    val filteredAndSortedGroups: Map<String, List<SubdlSubtitleItem>>
        get() {
            var items = subtitles
            if (hiOnly) {
                items = items.filter { it.hi == true }
            }
            if (selectedSeason != null) {
                items = items.filter { it.effectiveSeason == selectedSeason }
            }
            if (selectedEpisode != null) {
                items = items.filter { it.effectiveEpisode == selectedEpisode }
            }
            items = when (sortOption) {
                SubtitleSortOption.BEST_MATCH -> items
                SubtitleSortOption.NEWEST -> items.sortedWith(
                    compareByDescending<SubdlSubtitleItem> { (it.effectiveSeason ?: 0) * 1000 + (it.effectiveEpisode ?: 0) }
                        .thenByDescending { it.displayTitle }
                )
                SubtitleSortOption.ALPHABETICAL -> items.sortedBy { it.displayTitle.lowercase() }
            }

            val groups = LinkedHashMap<String, MutableList<SubdlSubtitleItem>>()
            for (item in items) {
                val title = item.displayTitle.uppercase()
                val tier = when {
                    title.contains("2160P") || title.contains("4K") || title.contains("UHD") -> "4K UHD"
                    title.contains("1080P") || title.contains("BLURAY") || title.contains("BLU-RAY") -> "1080p / BluRay"
                    title.contains("720P") || title.contains("WEB-DL") || title.contains("WEBDL") || title.contains("WEBRIP") -> "WEB-DL / 720p"
                    else -> "Other Releases"
                }
                if (selectedQuality == QualityFilterOption.ALL || selectedQuality.tierName == tier) {
                    groups.getOrPut(tier) { mutableListOf() }.add(item)
                }
            }
            return groups
        }

    val qualityCounts: Map<QualityFilterOption, Int>
        get() {
            var items = subtitles
            if (hiOnly) {
                items = items.filter { it.hi == true }
            }
            if (selectedSeason != null) {
                items = items.filter { it.effectiveSeason == selectedSeason }
            }
            if (selectedEpisode != null) {
                items = items.filter { it.effectiveEpisode == selectedEpisode }
            }
            var c4k = 0
            var c1080p = 0
            var c720p = 0
            var cOther = 0

            for (item in items) {
                val title = item.displayTitle.uppercase()
                when {
                    title.contains("2160P") || title.contains("4K") || title.contains("UHD") -> c4k++
                    title.contains("1080P") || title.contains("BLURAY") || title.contains("BLU-RAY") -> c1080p++
                    title.contains("720P") || title.contains("WEB-DL") || title.contains("WEBDL") || title.contains("WEBRIP") -> c720p++
                    else -> cOther++
                }
            }

            return mapOf(
                QualityFilterOption.ALL to items.size,
                QualityFilterOption.UHD_4K to c4k,
                QualityFilterOption.BLURAY_1080P to c1080p,
                QualityFilterOption.WEBDL_720P to c720p,
                QualityFilterOption.OTHER to cOther
            )
        }
}

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val subtitleRepository: SubtitleRepository,
    private val settingsRepository: SettingsRepository,
    private val storageManager: SubtitleStorageManager,
    private val posterRepository: PosterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SearchUiState(
            hasApiKey = settingsRepository.hasApiKey(),
            isFirstLaunch = settingsRepository.isFirstLaunch(),
            recentSearches = settingsRepository.getRecentSearches()
        )
    )
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _queryFlow = MutableStateFlow("")
    private var activeSearchJob: Job? = null

    private val searchCache = object : LinkedHashMap<String, CachedSearchResult>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedSearchResult>?): Boolean {
            return size > 25
        }
    }

    private fun buildCacheKey(query: String, language: String?): String {
        return "${query.trim().lowercase()}|${language ?: "all"}"
    }

    init {
        viewModelScope.launch {
            settingsRepository.apiKeyFlow.collect { key ->
                _uiState.value = _uiState.value.copy(
                    hasApiKey = key.isNotBlank()
                )
            }
        }
        viewModelScope.launch {
            settingsRepository.recentSearchesFlow.collect { list ->
                _uiState.value = _uiState.value.copy(
                    recentSearches = list
                )
            }
        }
        viewModelScope.launch {
            _queryFlow
                .debounce(500L)
                .distinctUntilChanged()
                .collectLatest { query ->
                    val trimmed = query.trim()
                    if (trimmed.length >= 2) {
                        executeSearch(rawQuery = trimmed, isAutoSearch = true)
                    }
                }
        }
    }

    private var currentSdId: Int? = null
    private var currentImdbId: String? = null
    private var currentFilmTitle: String? = null
    private val fetchedSeasons = mutableSetOf<Int>()

    fun onQueryChanged(newQuery: String) {
        val trimmed = newQuery.trim()
        val isBelowThreshold = trimmed.length < 2

        if (isBelowThreshold) {
            activeSearchJob?.cancel()
            currentSdId = null
            currentImdbId = null
            currentFilmTitle = null
            fetchedSeasons.clear()
        }

        _uiState.value = _uiState.value.copy(
            query = newQuery,
            errorMessage = null,
            isLoading = if (isBelowThreshold) false else _uiState.value.isLoading,
            isPosterLoading = if (isBelowThreshold) false else _uiState.value.isPosterLoading,
            subtitles = if (isBelowThreshold) emptyList() else _uiState.value.subtitles,
            mediaPoster = if (isBelowThreshold) null else _uiState.value.mediaPoster,
            selectedQuality = if (isBelowThreshold) QualityFilterOption.ALL else _uiState.value.selectedQuality,
            selectedSeason = if (isBelowThreshold) null else _uiState.value.selectedSeason,
            selectedEpisode = if (isBelowThreshold) null else _uiState.value.selectedEpisode
        )

        _queryFlow.value = newQuery
    }

    fun clearQuery() {
        activeSearchJob?.cancel()
        currentSdId = null
        currentImdbId = null
        currentFilmTitle = null
        fetchedSeasons.clear()
        _queryFlow.value = ""
        _uiState.value = _uiState.value.copy(
            query = "",
            subtitles = emptyList(),
            mediaPoster = null,
            isLoading = false,
            isPosterLoading = false,
            errorMessage = null,
            selectedQuality = QualityFilterOption.ALL,
            selectedSeason = null,
            selectedEpisode = null
        )
    }

    fun onLanguageSelected(code: String?) {
        _uiState.value = _uiState.value.copy(selectedLanguage = code)
        if (_uiState.value.query.trim().length >= 2) {
            searchSubtitles()
        }
    }

    fun setSortOption(option: SubtitleSortOption) {
        _uiState.value = _uiState.value.copy(sortOption = option)
    }

    fun setQualityFilter(option: QualityFilterOption) {
        _uiState.value = _uiState.value.copy(selectedQuality = option)
    }

    fun setSeasonFilter(season: Int?) {
        val currentState = _uiState.value
        val newEpisode = if (season != null && currentState.selectedEpisode != null) {
            val episodesInNewSeason = currentState.subtitles
                .filter { it.effectiveSeason == season }
                .mapNotNull { it.effectiveEpisode }
            if (currentState.selectedEpisode in episodesInNewSeason) currentState.selectedEpisode else null
        } else {
            currentState.selectedEpisode
        }
        _uiState.value = currentState.copy(
            selectedSeason = season,
            selectedEpisode = newEpisode
        )

        if (season != null && season !in fetchedSeasons) {
            fetchedSeasons.add(season)
            viewModelScope.launch {
                fetchAndMergeSeason(season)
            }
        }
    }

    fun setEpisodeFilter(episode: Int?) {
        _uiState.value = _uiState.value.copy(selectedEpisode = episode)
    }

    fun resetAllFilters() {
        _uiState.value = _uiState.value.copy(
            selectedQuality = QualityFilterOption.ALL,
            selectedSeason = null,
            selectedEpisode = null,
            hiOnly = false
        )
    }

    fun toggleHiOnly() {
        _uiState.value = _uiState.value.copy(hiOnly = !_uiState.value.hiOnly)
    }

    fun onRecentSearchClicked(query: String) {
        _queryFlow.value = query
        _uiState.value = _uiState.value.copy(query = query)
        executeSearch(rawQuery = query, isAutoSearch = false)
    }

    fun removeRecentSearch(query: String) {
        settingsRepository.removeRecentSearch(query)
    }

    fun clearRecentSearches() {
        settingsRepository.clearRecentSearches()
    }

    fun selectSubtitleForDetails(item: SubdlSubtitleItem?) {
        if (item != null && _uiState.value.query.isNotBlank()) {
            settingsRepository.addRecentSearch(_uiState.value.query.trim())
        }
        _uiState.value = _uiState.value.copy(selectedSubtitleForDetails = item)
    }

    fun searchSubtitles() {
        val rawQuery = _uiState.value.query.trim()
        if (rawQuery.isBlank()) return
        executeSearch(rawQuery = rawQuery, isAutoSearch = false)
    }

    private fun executeSearch(rawQuery: String, isAutoSearch: Boolean) {
        val query = rawQuery.trim()
        if (query.isBlank()) return

        if (!settingsRepository.hasApiKey()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "API key required. Please set your SubDL API key in Settings."
            )
            return
        }

        // Add query to recent searches on explicit user action
        if (!isAutoSearch) {
            settingsRepository.addRecentSearch(query)
        }

        val cacheKey = buildCacheKey(query, _uiState.value.selectedLanguage)
        synchronized(searchCache) {
            val cached = searchCache[cacheKey]
            if (cached != null) {
                currentSdId = cached.sdId
                currentImdbId = cached.imdbId
                currentFilmTitle = cached.filmTitle
                fetchedSeasons.clear()
                fetchedSeasons.addAll(cached.fetchedSeasons)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isPosterLoading = false,
                    subtitles = cached.subtitles,
                    mediaPoster = cached.mediaPoster,
                    selectedQuality = QualityFilterOption.ALL,
                    selectedSeason = cached.selectedSeason,
                    selectedEpisode = cached.selectedEpisode,
                    errorMessage = if (cached.subtitles.isEmpty()) "No subtitles found for \"$query\"." else null
                )
                return
            }
        }

        val parsed = parseQuery(query)
        val cleanTitle = parsed.cleanTitle
        val querySeason = parsed.seasonNumber
        val queryEpisode = parsed.episodeNumber

        currentSdId = null
        currentImdbId = null
        currentFilmTitle = cleanTitle
        fetchedSeasons.clear()

        activeSearchJob?.cancel()
        activeSearchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                isPosterLoading = true,
                errorMessage = null,
                mediaPoster = null,
                selectedQuality = QualityFilterOption.ALL,
                selectedSeason = querySeason,
                selectedEpisode = queryEpisode
            )

            // Fast preliminary search using clean query title
            val quickPosterJob = launch {
                try {
                    val poster = posterRepository.getMediaPoster(cleanTitle)
                    if (poster != null && _uiState.value.mediaPoster == null) {
                        _uiState.value = _uiState.value.copy(mediaPoster = poster)
                        if (poster.seriesSeasons.isNotEmpty()) {
                            autoFetchOtherSeasons(poster.seriesSeasons)
                        }
                    }
                } catch (_: Exception) {
                }
            }

            val searchResult = subtitleRepository.searchSubtitlesWithMedia(
                query = cleanTitle,
                seasonNumber = querySeason,
                episodeNumber = queryEpisode,
                languages = _uiState.value.selectedLanguage
            )

            searchResult.fold(
                onSuccess = { resultData ->
                    val matchedMedia = resultData.media
                    val items = resultData.subtitles

                    currentSdId = matchedMedia?.sdId
                    currentImdbId = matchedMedia?.imdbId
                    if (!matchedMedia?.name.isNullOrBlank()) {
                        currentFilmTitle = matchedMedia?.name
                    }

                    if (querySeason != null) {
                        fetchedSeasons.add(querySeason)
                    } else {
                        items.mapNotNull { it.effectiveSeason }.forEach { fetchedSeasons.add(it) }
                        if (fetchedSeasons.isEmpty()) fetchedSeasons.add(1)
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        subtitles = items,
                        errorMessage = if (items.isEmpty()) "No subtitles found for \"$query\"." else null
                    )

                    fun updateCache() {
                        synchronized(searchCache) {
                            searchCache[cacheKey] = CachedSearchResult(
                                subtitles = _uiState.value.subtitles,
                                mediaPoster = _uiState.value.mediaPoster,
                                sdId = currentSdId,
                                imdbId = currentImdbId,
                                filmTitle = currentFilmTitle,
                                fetchedSeasons = fetchedSeasons.toSet(),
                                selectedSeason = querySeason,
                                selectedEpisode = queryEpisode
                            )
                        }
                    }

                    updateCache()

                    // Match Image Retrieval with exact media returned by SubDL
                    if (matchedMedia != null) {
                        launch {
                            try {
                                val exactPoster = if (!matchedMedia.imdbId.isNullOrBlank()) {
                                    posterRepository.getMediaPosterByImdbId(
                                        imdbId = matchedMedia.imdbId,
                                        type = matchedMedia.type,
                                        titleFallback = matchedMedia.name,
                                        yearFallback = matchedMedia.yearString
                                    )
                                } else if (!matchedMedia.name.isNullOrBlank()) {
                                    posterRepository.getMediaPoster(matchedMedia.name)
                                } else null

                                if (exactPoster != null) {
                                    _uiState.value = _uiState.value.copy(
                                        mediaPoster = exactPoster,
                                        isPosterLoading = false
                                    )
                                    updateCache()
                                    if (exactPoster.seriesSeasons.isNotEmpty()) {
                                        autoFetchOtherSeasons(exactPoster.seriesSeasons)
                                    }
                                } else {
                                    quickPosterJob.join()
                                    _uiState.value = _uiState.value.copy(isPosterLoading = false)
                                    updateCache()
                                }
                            } catch (_: Exception) {
                                quickPosterJob.join()
                                _uiState.value = _uiState.value.copy(isPosterLoading = false)
                                updateCache()
                            }
                        }
                    } else {
                        launch {
                            quickPosterJob.join()
                            _uiState.value = _uiState.value.copy(isPosterLoading = false)
                            updateCache()
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isPosterLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to search subtitles"
                    )
                }
            )
        }
    }

    private fun autoFetchOtherSeasons(allSeasons: List<Int>) {
        val seasonsToFetch = allSeasons.filter { it !in fetchedSeasons }.take(3)
        for (season in seasonsToFetch) {
            fetchedSeasons.add(season)
            viewModelScope.launch {
                fetchAndMergeSeason(season)
            }
        }
    }

    private suspend fun fetchAndMergeSeason(season: Int) {
        val title = currentFilmTitle ?: _uiState.value.query
        val result = subtitleRepository.searchSubtitles(
            query = title,
            sdId = currentSdId,
            imdbId = currentImdbId,
            seasonNumber = season,
            languages = _uiState.value.selectedLanguage
        )
        result.onSuccess { newItems ->
            if (newItems.isNotEmpty()) {
                val current = _uiState.value.subtitles
                val merged = (current + newItems).distinctBy {
                    it.fullDownloadUrl.ifBlank { "${it.season}_${it.episode}_${it.displayTitle}" }
                }
                _uiState.value = _uiState.value.copy(subtitles = merged)
                val cacheKey = buildCacheKey(_uiState.value.query, _uiState.value.selectedLanguage)
                synchronized(searchCache) {
                    searchCache[cacheKey]?.let { old ->
                        searchCache[cacheKey] = old.copy(
                            subtitles = merged,
                            fetchedSeasons = fetchedSeasons.toSet()
                        )
                    }
                }
            }
        }
    }

    fun downloadSubtitle(item: SubdlSubtitleItem) {
        if (_uiState.value.query.isNotBlank()) {
            settingsRepository.addRecentSearch(_uiState.value.query.trim())
        }
        val downloadUrl = item.fullDownloadUrl
        if (downloadUrl.isBlank()) {
            _uiState.value = _uiState.value.copy(
                snackbarMessage = "No download link available for this subtitle."
            )
            return
        }

        val statuses = _uiState.value.downloadStatuses.toMutableMap()
        statuses[downloadUrl] = DownloadStatus.Downloading
        _uiState.value = _uiState.value.copy(downloadStatuses = statuses)

        viewModelScope.launch {
            val downloadResult = subtitleRepository.downloadArchive(downloadUrl)

            downloadResult.fold(
                onSuccess = { zipBytes ->
                    val statusMap = _uiState.value.downloadStatuses.toMutableMap()
                    statusMap[downloadUrl] = DownloadStatus.Extracting
                    _uiState.value = _uiState.value.copy(downloadStatuses = statusMap)

                    val extractedList = ZipExtractor.extractSubtitles(zipBytes)
                    if (extractedList.isEmpty()) {
                        statusMap[downloadUrl] = DownloadStatus.Failed("No subtitles found in archive")
                        _uiState.value = _uiState.value.copy(
                            downloadStatuses = statusMap,
                            snackbarMessage = "Archive did not contain any .srt/.vtt files"
                        )
                        return@launch
                    }

                    var lastSavedUri: Uri? = null
                    var lastFileName = ""

                    for (sub in extractedList) {
                        val saveResult = storageManager.saveSubtitle(sub)
                        saveResult.onSuccess { uri ->
                            lastSavedUri = uri
                            lastFileName = sub.fileName
                        }
                    }

                    if (lastSavedUri != null) {
                        statusMap[downloadUrl] = DownloadStatus.Completed(lastFileName)
                        val uris = _uiState.value.savedUris.toMutableMap()
                        uris[downloadUrl] = lastSavedUri!!

                        _uiState.value = _uiState.value.copy(
                            downloadStatuses = statusMap,
                            savedUris = uris,
                            snackbarMessage = "Saved $lastFileName to Download/Subget/"
                        )
                    } else {
                        statusMap[downloadUrl] = DownloadStatus.Failed("Failed to save to storage")
                        _uiState.value = _uiState.value.copy(
                            downloadStatuses = statusMap,
                            snackbarMessage = "Failed to write subtitle to Download/Subget/"
                        )
                    }
                },
                onFailure = { error ->
                    val statusMap = _uiState.value.downloadStatuses.toMutableMap()
                    statusMap[downloadUrl] = DownloadStatus.Failed(error.localizedMessage ?: "Download failed")
                    _uiState.value = _uiState.value.copy(
                        downloadStatuses = statusMap,
                        snackbarMessage = "Download failed: ${error.localizedMessage}"
                    )
                }
            )
        }
    }

    fun shareSubtitle(item: SubdlSubtitleItem, context: Context) {
        val uri = _uiState.value.savedUris[item.fullDownloadUrl] ?: return
        val shareIntent = storageManager.createShareIntent(uri, item.displayTitle)
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Subtitle With"))
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    data class ParsedQuery(
        val cleanTitle: String,
        val seasonNumber: Int? = null,
        val episodeNumber: Int? = null
    )

    companion object {
        fun parseQuery(rawQuery: String): ParsedQuery {
            var clean = rawQuery.trim()
            var season: Int? = null
            var episode: Int? = null

            // 1. Check for SxxExx or Season X Episode Y
            val sePattern = Regex("""(?i)\b(?:s|season\s*)(\d{1,2})\s*(?:e|ep|episode\s*)(\d{1,2})\b""")
            val seMatch = sePattern.find(clean)
            if (seMatch != null) {
                season = seMatch.groupValues[1].toIntOrNull()
                episode = seMatch.groupValues[2].toIntOrNull()
                clean = clean.removeRange(seMatch.range).trim()
            } else {
                // 2. Check for NxNN format (e.g., 2x04)
                val xPattern = Regex("""\b(\d{1,2})x(\d{1,2})\b""")
                val xMatch = xPattern.find(clean)
                if (xMatch != null) {
                    season = xMatch.groupValues[1].toIntOrNull()
                    episode = xMatch.groupValues[2].toIntOrNull()
                    clean = clean.removeRange(xMatch.range).trim()
                } else {
                    // 3. Check for Season X or S0X / Sx
                    val sPattern = Regex("""(?i)\b(?:season\s*|s)(\d{1,2})\b""")
                    val sMatch = sPattern.find(clean)
                    if (sMatch != null) {
                        season = sMatch.groupValues[1].toIntOrNull()
                        clean = clean.removeRange(sMatch.range).trim()
                    }

                    // 4. Check for Episode Y or Ep Y / Ex
                    val ePattern = Regex("""(?i)\b(?:episode\s*|ep\s*|e)(\d{1,2})\b""")
                    val eMatch = ePattern.find(clean)
                    if (eMatch != null) {
                        episode = eMatch.groupValues[1].toIntOrNull()
                        clean = clean.removeRange(eMatch.range).trim()
                    }
                }
            }

            clean = clean.replace(Regex("""[-:_,\s]+$"""), "").trim()
            if (clean.isBlank()) clean = rawQuery.trim()

            return ParsedQuery(
                cleanTitle = clean,
                seasonNumber = season,
                episodeNumber = episode
            )
        }
    }

    class Factory(
        private val subtitleRepository: SubtitleRepository,
        private val settingsRepository: SettingsRepository,
        private val storageManager: SubtitleStorageManager,
        private val posterRepository: PosterRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SearchViewModel(subtitleRepository, settingsRepository, storageManager, posterRepository) as T
        }
    }
}
