package com.subget.app.data.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Serializable
data class SubdlMediaResult(
    @SerialName("sd_id") val sdId: Int? = null,
    val id: String? = null,
    val name: String? = null,
    val type: String? = null,
    @SerialName("imdb_id") val imdbId: String? = null,
    @SerialName("tmdb_id") val tmdbId: Int? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    val year: JsonElement? = null
) {
    val yearString: String?
        get() = when (val y = year) {
            is JsonPrimitive -> y.contentOrNull
            else -> null
        }
}

@Serializable
data class SubdlResponse(
    val status: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val results: List<SubdlMediaResult>? = null,
    val subtitles: List<SubdlSubtitleItem>? = null
) {
    val allSubtitles: List<SubdlSubtitleItem>
        get() = subtitles ?: emptyList()

    val primaryMedia: SubdlMediaResult?
        get() = results?.firstOrNull()
}

data class SubdlSearchResult(
    val media: SubdlMediaResult? = null,
    val subtitles: List<SubdlSubtitleItem> = emptyList()
)

@Serializable
data class SubdlSubtitleItem(
    @SerialName("release_name") val releaseName: String? = null,
    val name: String? = null,
    val lang: String? = null,
    val language: String? = null,
    val author: String? = null,
    val url: String? = null,
    @SerialName("download_url") val downloadUrl: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val hi: Boolean? = false,
    val type: String? = null,
    val comment: String? = null
) {
    val displayTitle: String
        get() = when {
            !releaseName.isNullOrBlank() -> releaseName
            !name.isNullOrBlank() -> name
            else -> "Subtitle"
        }

    val displayLanguage: String
        get() = when {
            !lang.isNullOrBlank() -> lang
            !language.isNullOrBlank() -> language
            else -> "Unknown"
        }

    val fullDownloadUrl: String
        get() {
            val target = (downloadUrl ?: url ?: "").trim()
            return when {
                target.startsWith("http://", ignoreCase = true) || target.startsWith("https://", ignoreCase = true) -> target
                target.startsWith("/") -> "https://dl.subdl.com$target"
                target.isNotEmpty() -> "https://dl.subdl.com/$target"
                else -> ""
            }
        }

    val effectiveSeason: Int?
        get() {
            if (season != null) return season
            val title = displayTitle
            REGEX_S_E.find(title)?.let { return it.groupValues[1].toIntOrNull() }
            REGEX_X.find(title)?.let { return it.groupValues[1].toIntOrNull() }
            REGEX_SEASON.find(title)?.let { return it.groupValues[1].toIntOrNull() }
            return null
        }

    val effectiveEpisode: Int?
        get() {
            if (episode != null) return episode
            val title = displayTitle
            REGEX_S_E.find(title)?.let { return it.groupValues[2].toIntOrNull() }
            REGEX_X.find(title)?.let { return it.groupValues[2].toIntOrNull() }
            REGEX_EPISODE.find(title)?.let { return it.groupValues[1].toIntOrNull() }
            return null
        }

    val formattedSeasonEpisode: String?
        get() {
            val s = effectiveSeason
            val e = effectiveEpisode
            return when {
                s != null && e != null -> "S%02dE%02d".format(s, e)
                s != null -> "Season %d".format(s)
                e != null -> "Episode %d".format(e)
                else -> null
            }
        }

    companion object {
        private val REGEX_S_E = Regex("""(?i)\bS(\d{1,2})\s*E(\d{1,3})\b""")
        private val REGEX_X = Regex("""(?i)\b(\d{1,2})x(\d{1,3})\b""")
        private val REGEX_SEASON = Regex("""(?i)\b(?:season|s)\s*(\d{1,2})\b""")
        private val REGEX_EPISODE = Regex("""(?i)\b(?:episode|ep)\s*(\d{1,3})\b""")
    }
}

data class SearchSuggestion(
    val title: String,
    val year: String? = null,
    val mediaType: String? = null,
    val imdbId: String? = null,
    val posterUrl: String? = null
)

