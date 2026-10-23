package com.tivipro.data.repository

import com.tivipro.model.Category
import com.tivipro.model.Episode
import com.tivipro.model.EpgProgram
import com.tivipro.model.LiveChannel
import com.tivipro.model.Series
import com.tivipro.model.SportsEvent
import com.tivipro.model.VodMovie

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
