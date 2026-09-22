package com.subget.app.data.repository

import android.util.Log
import com.subget.app.data.api.models.MediaPoster
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class PosterRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = ConcurrentHashMap<String, MediaPoster>()

    suspend fun getMediaPosterByImdbId(
        imdbId: String,
        type: String? = null,
        titleFallback: String? = null,
        yearFallback: String? = null
    ): MediaPoster? = withContext(Dispatchers.IO) {
        val cleanId = imdbId.trim()
        if (cleanId.isBlank()) return@withContext null

        val cacheKey = "imdb:$cleanId"
        cache[cacheKey]?.let { return@withContext it }

        val resolvedType = when (type?.lowercase()?.trim()) {
            "tv", "series", "tvseries", "show" -> "series"
            "movie", "film" -> "movie"
            else -> null
        }

        if (resolvedType != null) {
            val poster = fetchFromCinemetaMeta(cleanId, resolvedType, titleFallback, yearFallback)
            if (poster?.posterUrl != null) {
                cache[cacheKey] = poster
                return@withContext poster
            }
        } else {
            val poster = coroutineScope {
                val seriesDef = async { fetchFromCinemetaMeta(cleanId, "series", titleFallback, yearFallback) }
                val movieDef = async { fetchFromCinemetaMeta(cleanId, "movie", titleFallback, yearFallback) }
                val seriesPoster = seriesDef.await()
                if (seriesPoster?.posterUrl != null) {
                    return@coroutineScope seriesPoster
                }
                val moviePoster = movieDef.await()
                if (moviePoster?.posterUrl != null) {
                    return@coroutineScope moviePoster
                }
                null
            }
            if (poster != null) {
                cache[cacheKey] = poster
                return@withContext poster
            }
        }

        // If title fallback exists, try text search as fallback
        if (!titleFallback.isNullOrBlank()) {
            val fallbackPoster = getMediaPoster(titleFallback)
            if (fallbackPoster != null) {
                cache[cacheKey] = fallbackPoster
                return@withContext fallbackPoster
            }
        }

        null
    }

    suspend fun getMediaPoster(rawQuery: String): MediaPoster? = withContext(Dispatchers.IO) {
        val cleanQuery = sanitizeQuery(rawQuery)
        if (cleanQuery.isBlank()) return@withContext null

        val cacheKey = cleanQuery.lowercase()
        cache[cacheKey]?.let { return@withContext it }

        coroutineScope {
            // Concurrently query movie and series catalogs
            val movieDeferred = async { fetchFromCinemeta(cleanQuery, "movie") }
            val seriesDeferred = async { fetchFromCinemeta(cleanQuery, "series") }

            val moviePoster = movieDeferred.await()
            val seriesPoster = seriesDeferred.await()

            // Pick best candidate between movie and series
            val bestCinemeta = chooseBestCinemeta(cleanQuery, moviePoster, seriesPoster)
            if (bestCinemeta?.posterUrl != null) {
                cache[cacheKey] = bestCinemeta
                return@coroutineScope bestCinemeta
            }

            // Attempt 3: TVmaze API (fallback for TV shows)
            val tvmazePoster = fetchFromTVmaze(cleanQuery)
            if (tvmazePoster?.posterUrl != null) {
                cache[cacheKey] = tvmazePoster
                return@coroutineScope tvmazePoster
            }

            null
        }
    }

    private fun chooseBestCinemeta(
        query: String,
        movie: MediaPoster?,
        series: MediaPoster?
    ): MediaPoster? {
        if (movie == null) return series
        if (series == null) return movie

        val q = query.lowercase().trim()
        val movieTitle = movie.title.lowercase().trim()
        val seriesTitle = series.title.lowercase().trim()

        val movieExact = movieTitle == q || movieTitle.contains(q) || q.contains(movieTitle)
        val seriesExact = seriesTitle == q || seriesTitle.contains(q) || q.contains(seriesTitle)

        return when {
            seriesExact && !movieExact -> series
            movieExact && !seriesExact -> movie
            query.contains(Regex("""(?i)\b(s\d\d|season|ep\d\d|episode)\b""")) -> series
            else -> movie
        }
    }

    private fun fetchFromCinemetaMeta(
        id: String,
        type: String,
        nameFallback: String? = null,
        yearFallback: String? = null
    ): MediaPoster? {
        try {
            val metaUrl = "https://v3-cinemeta.strem.io/meta/$type/$id.json"
            val metaReq = Request.Builder()
                .url(metaUrl)
                .header("User-Agent", "Subget-Android/1.0")
                .build()
            val metaResp = client.newCall(metaReq).execute()
            if (!metaResp.isSuccessful) return null

            val metaBody = metaResp.body?.string() ?: return null
            val root = json.parseToJsonElement(metaBody).jsonObject
            val metaObj = root["meta"]?.jsonObject ?: return null

            val name = metaObj["name"]?.jsonPrimitive?.content ?: nameFallback ?: "Unknown"
            val releaseInfo = metaObj["releaseInfo"]?.jsonPrimitive?.content
                ?: metaObj["year"]?.jsonPrimitive?.content
                ?: yearFallback
            val posterUrl = metaObj["poster"]?.jsonPrimitive?.content
            val description = metaObj["description"]?.jsonPrimitive?.content
            val itemType = if (type == "movie") "Movie" else "TV Series"

            val videos = metaObj["videos"]?.jsonArray
            val seriesSeasons = if (type != "movie" && videos != null) {
                videos.mapNotNull { it.jsonObject["season"]?.jsonPrimitive?.content?.toIntOrNull() }
                    .filter { it > 0 }
                    .distinct()
                    .sorted()
            } else {
                emptyList()
            }

            return MediaPoster(
                title = name,
                year = releaseInfo,
                posterUrl = posterUrl,
                description = description?.take(220)?.let { if (it.length >= 220) "$it..." else it },
                mediaType = itemType,
                imdbId = id,
                seriesSeasons = seriesSeasons
            )
        } catch (e: Exception) {
            Log.d("PosterRepository", "Cinemeta meta lookup failed for $id ($type): ${e.message}")
            return null
        }
    }

    private fun fetchFromCinemeta(query: String, type: String): MediaPoster? {
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://v3-cinemeta.strem.io/catalog/$type/top/search=$encoded.json"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Subget-Android/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null

            val body = response.body?.string() ?: return null
            val root = json.parseToJsonElement(body).jsonObject
            val metas = root["metas"]?.jsonArray ?: return null
            if (metas.isEmpty()) return null

            // Find best matching item in list
            val q = query.lowercase().trim()
            var selected = metas[0].jsonObject
            for (elem in metas) {
                val obj = elem.jsonObject
                val name = obj["name"]?.jsonPrimitive?.content?.lowercase()?.trim() ?: ""
                if (name == q) {
                    selected = obj
                    break
                }
            }

            val id = selected["id"]?.jsonPrimitive?.content
            val name = selected["name"]?.jsonPrimitive?.content ?: query
            val releaseInfo = selected["releaseInfo"]?.jsonPrimitive?.content
            val posterUrl = selected["poster"]?.jsonPrimitive?.content
            val itemType = if (type == "movie") "Movie" else "TV Series"

            // Try to get synopsis from meta endpoint if id exists
            var metaPoster: MediaPoster? = null
            if (!id.isNullOrBlank()) {
                metaPoster = fetchFromCinemetaMeta(id, type, name, releaseInfo)
            }

            return MediaPoster(
                title = name,
                year = releaseInfo,
                posterUrl = metaPoster?.posterUrl ?: posterUrl,
                description = metaPoster?.description ?: (selected["description"]?.jsonPrimitive?.content?.take(220)?.let { if (it.length >= 220) "$it..." else it }),
                mediaType = itemType,
                imdbId = id,
                seriesSeasons = metaPoster?.seriesSeasons ?: emptyList()
            )
        } catch (e: Exception) {
            Log.d("PosterRepository", "Cinemeta lookup failed for $query ($type): ${e.message}")
            return null
        }
    }

    private fun fetchFromTVmaze(query: String): MediaPoster? {
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.tvmaze.com/singlesearch/shows?q=$encoded"
            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null

            val body = response.body?.string() ?: return null
            val obj = json.parseToJsonElement(body).jsonObject

            val title = obj["name"]?.jsonPrimitive?.content ?: query
            val premiered = obj["premiered"]?.jsonPrimitive?.content
            val year = premiered?.take(4)

            val imageObj = obj["image"]?.jsonObject
            val posterUrl = imageObj?.get("original")?.jsonPrimitive?.content
                ?: imageObj?.get("medium")?.jsonPrimitive?.content

            val summaryRaw = obj["summary"]?.jsonPrimitive?.content
            val cleanSummary = summaryRaw?.replace(Regex("<[^>]*>"), "")?.trim()

            return MediaPoster(
                title = title,
                year = year,
                posterUrl = posterUrl,
                description = cleanSummary?.take(220)?.let { if (it.length >= 220) "$it..." else it },
                mediaType = "TV Series"
            )
        } catch (e: Exception) {
            Log.d("PosterRepository", "TVmaze lookup failed for $query: ${e.message}")
            return null
        }
    }

    private fun sanitizeQuery(query: String): String {
        return query
            .replace(Regex("""(?i)\b(1080p|720p|2160p|4k|bluray|blu-ray|web-dl|webrip|x264|x265|hevc|aac|dts|hdrip|dvdrip|remux)\b.*"""), "")
            .replace(Regex("""[\[\].()_-]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
