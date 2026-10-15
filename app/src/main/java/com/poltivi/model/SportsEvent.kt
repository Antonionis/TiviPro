package com.poltivi.model

data class SportsEvent(
    val id: String,
    val title: String,
    val competition: String,
    val sportCategory: String,
    val dateTab: String,
    val dateTimeDisplay: String,
    val channelName: String,
    val channelId: String? = null,
    val streamUrl: String = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
    val iconUrl: String? = null
)
