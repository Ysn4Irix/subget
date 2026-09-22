package com.subget.app.data.api.models

import kotlinx.serialization.Serializable

@Serializable
data class MediaPoster(
    val title: String,
    val year: String? = null,
    val posterUrl: String? = null,
    val description: String? = null,
    val mediaType: String? = null,
    val imdbId: String? = null,
    val seriesSeasons: List<Int> = emptyList()
)

