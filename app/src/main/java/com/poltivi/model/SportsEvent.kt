package com.poltivi.model

data class SportsEvent(
    val id: String,
    val title: String,
    val competition: String,
    val sportCategory: String,
    val dateTab: String,
    val eventDateStr: String = "",
    val dateTimeDisplay: String,
    val channelName: String,
    val channelId: String? = null,
    val streamUrl: String = "",
    val iconUrl: String? = null,
    val isLive: Boolean = false,
    val startTimestamp: Long = 0L
)
