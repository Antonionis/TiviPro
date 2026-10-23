package com.tivipro.model

data class SeasonInfo(
    val seasonNumber: Int,
    val name: String? = null,
    val episodeCount: Int = 0,
    val coverUrl: String? = null
)

data class Series(
    val id: String,
    val name: String,
    val posterUrl: String? = null,
    val categoryId: String,
    val rating: Double? = null,
    val overview: String? = null,
    val seasons: List<SeasonInfo> = emptyList(),
    val isFavorite: Boolean = false
)
