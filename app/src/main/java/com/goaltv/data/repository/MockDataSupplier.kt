package com.goaltv.data.repository

import com.goaltv.model.Category
import com.goaltv.model.Episode
import com.goaltv.model.EpgProgram
import com.goaltv.model.LiveChannel
import com.goaltv.model.Series
import com.goaltv.model.SportsEvent
import com.goaltv.model.VodMovie

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
