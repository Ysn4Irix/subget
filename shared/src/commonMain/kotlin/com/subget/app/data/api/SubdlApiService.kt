package com.subget.app.data.api

import com.subget.app.data.api.models.SubdlResponse
import com.subget.app.data.api.models.SubdlSubtitleItem
import com.subget.app.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class SubdlApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun searchSubtitlesWithMedia(
        apiKey: String,
        filmName: String? = null,
        sdId: Int? = null,
        imdbId: String? = null,
        languages: String? = null,
        type: String? = null,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
        subsPerPage: Int = 30,
        fullSeason: Boolean = false
    ): Result<com.subget.app.data.api.models.SubdlSearchResult> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("SubDL API key is required"))
        }
        if (filmName.isNullOrBlank() && sdId == null && imdbId.isNullOrBlank()) {
            return@withContext Result.success(com.subget.app.data.api.models.SubdlSearchResult())
        }

        val urlBuilder = "https://api.subdl.com/api/v1/subtitles"
            .toHttpUrlOrNull()
            ?.newBuilder()
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid base URL"))

        urlBuilder.addQueryParameter("api_key", apiKey.trim())
        if (!filmName.isNullOrBlank()) {
            urlBuilder.addQueryParameter("film_name", filmName.trim())
        }
        if (sdId != null) {
            urlBuilder.addQueryParameter("sd_id", sdId.toString())
        }
        if (!imdbId.isNullOrBlank()) {
            urlBuilder.addQueryParameter("imdb_id", imdbId.trim())
        }
        if (!languages.isNullOrBlank()) {
            urlBuilder.addQueryParameter("languages", languages.trim())
        }
        if (!type.isNullOrBlank()) {
            urlBuilder.addQueryParameter("type", type.trim())
        }
        if (seasonNumber != null) {
            urlBuilder.addQueryParameter("season_number", seasonNumber.toString())
        }
        if (episodeNumber != null) {
            urlBuilder.addQueryParameter("episode_number", episodeNumber.toString())
        }
        urlBuilder.addQueryParameter("subs_per_page", subsPerPage.coerceIn(1, 30).toString())
        if (fullSeason) {
            urlBuilder.addQueryParameter("full_season", "1")
        }

        val url = urlBuilder.build()
        AppLogger.d("SubdlApiService", "Request URL: $url")
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Subget/1.0")
            .header("Accept", "application/json")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                AppLogger.d("SubdlApiService", "Response code: ${response.code}, body: $body")
                if (!response.isSuccessful) {
                    val errorMsg = when (response.code) {
                        401 -> "Invalid SubDL API key. Please check your key in Settings."
                        404 -> "No subtitles found for this title."
                        429 -> "Rate limit exceeded. Please wait a moment."
                        else -> "Server error (${response.code})"
                    }
                    return@withContext Result.failure(IOException(errorMsg))
                }

                if (body.isNullOrBlank()) {
                    return@withContext Result.success(com.subget.app.data.api.models.SubdlSearchResult())
                }

                val subdlResponse = json.decodeFromString<SubdlResponse>(body)
                if (!subdlResponse.status && subdlResponse.error != null) {
                    return@withContext Result.failure(IOException(subdlResponse.error))
                }

                val rawItems = subdlResponse.allSubtitles
                val enrichedItems = if (seasonNumber != null) {
                    rawItems.map { item ->
                        if (item.season == null) item.copy(season = seasonNumber) else item
                    }
                } else {
                    rawItems
                }

                Result.success(
                    com.subget.app.data.api.models.SubdlSearchResult(
                        media = subdlResponse.primaryMedia,
                        subtitles = enrichedItems
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchSubtitles(
        apiKey: String,
        filmName: String? = null,
        sdId: Int? = null,
        imdbId: String? = null,
        languages: String? = null,
        type: String? = null,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null
    ): Result<List<SubdlSubtitleItem>> {
        return searchSubtitlesWithMedia(
            apiKey = apiKey,
            filmName = filmName,
            sdId = sdId,
            imdbId = imdbId,
            languages = languages,
            type = type,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber
        ).map { it.subtitles }
    }

    suspend fun downloadArchive(downloadUrl: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        if (downloadUrl.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Download URL cannot be empty"))
        }

        val request = Request.Builder()
            .url(downloadUrl)
            .header("User-Agent", "Subget/1.0")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Download failed with HTTP ${response.code}"))
                }
                val bytes = response.body?.bytes()
                    ?: return@withContext Result.failure(IOException("Empty response body from download"))
                Result.success(bytes)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
