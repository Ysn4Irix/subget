package com.subget.app.data.repository

import com.subget.app.data.api.SubdlApiService
import com.subget.app.data.api.models.SubdlSearchResult
import com.subget.app.data.api.models.SubdlSubtitleItem

class SubtitleRepository(
    private val apiService: SubdlApiService = SubdlApiService(),
    private val settingsRepository: SettingsRepository
) {
    suspend fun searchSubtitlesWithMedia(
        query: String? = null,
        sdId: Int? = null,
        imdbId: String? = null,
        languages: String? = null,
        type: String? = null,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null
    ): Result<SubdlSearchResult> {
        val apiKey = settingsRepository.getApiKey()
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("API_KEY_MISSING"))
        }
        val effectiveLanguages = languages ?: settingsRepository.getPreferredLanguages()
        return apiService.searchSubtitlesWithMedia(
            apiKey = apiKey,
            filmName = query,
            sdId = sdId,
            imdbId = imdbId,
            languages = effectiveLanguages,
            type = type,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber
        )
    }

    suspend fun searchSubtitles(
        query: String? = null,
        sdId: Int? = null,
        imdbId: String? = null,
        languages: String? = null,
        type: String? = null,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null
    ): Result<List<SubdlSubtitleItem>> {
        val apiKey = settingsRepository.getApiKey()
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("API_KEY_MISSING"))
        }
        val effectiveLanguages = languages ?: settingsRepository.getPreferredLanguages()
        return apiService.searchSubtitles(
            apiKey = apiKey,
            filmName = query,
            sdId = sdId,
            imdbId = imdbId,
            languages = effectiveLanguages,
            type = type,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber
        )
    }

    suspend fun downloadArchive(url: String): Result<ByteArray> {
        return apiService.downloadArchive(url)
    }
}
