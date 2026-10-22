package com.poltivi.model

import com.poltivi.data.repository.DataSourceMode

data class SavedPlaylist(
    val id: String,
    val name: String,
    val mode: DataSourceMode = DataSourceMode.XTREAM,
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val m3uUrl: String = "",
    val isActive: Boolean = false
)
