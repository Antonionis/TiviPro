package com.poltivi.model

data class Episode(
    val id: String,
    val title: String,
    val episodeNum: Int,
    val seasonNum: Int,
    val streamUrl: String,
    val containerExtension: String? = "mp4",
    val duration: String? = null,
    val overview: String? = null,
    val iconUrl: String? = null
)
