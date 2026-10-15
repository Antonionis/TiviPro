package com.poltivi.data.repository

import com.poltivi.model.Category
import com.poltivi.model.Episode
import com.poltivi.model.EpgProgram
import com.poltivi.model.LiveChannel
import com.poltivi.model.Series
import com.poltivi.model.SportsEvent
import com.poltivi.model.VodMovie

object MockDataSupplier {

    val liveCategories: List<Category> = emptyList()

    val vodCategories: List<Category> = emptyList()

    val seriesCategories: List<Category> = emptyList()

    val liveChannels: List<LiveChannel> = emptyList()

    val vodMovies: List<VodMovie> = emptyList()

    val seriesList: List<Series> = emptyList()

    val seriesEpisodesMap: Map<String, List<Episode>> = emptyMap()

    val sportsEvents: List<SportsEvent> = emptyList()

    fun getEpgForChannel(channelId: String): List<EpgProgram> = emptyList()
}
