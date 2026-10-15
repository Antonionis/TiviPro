package com.goaltv.model

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
    val isFavorite: Boolean = false
)
