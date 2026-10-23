package com.tivipro.model

data class CastMember(
    val name: String,
    val role: String? = null
)

data class VodMovie(
    val id: String,
    val name: String,
    val posterUrl: String? = null,
    val streamUrl: String,
    val categoryId: String,
    val rating: Double? = null,
    val containerExtension: String? = "mp4",
    val description: String? = null,
    val releaseDate: String? = null,
    val durationMinutes: Int? = null,
    val isFavorite: Boolean = false,
    val genres: String? = null,
    val trailerUrl: String? = null,
    val cast: List<CastMember> = emptyList()
)
