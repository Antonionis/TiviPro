package com.tivipro.data.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface XtreamCodesService {

    @GET
    suspend fun authenticate(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String
    ): Response<XtreamAuthDto>

    @GET
    suspend fun getLiveCategories(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_categories"
    ): Response<List<XtreamCategoryDto>>

    @GET
    suspend fun getLiveStreams(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_streams",
        @Query("category_id") categoryId: String? = null
    ): Response<List<XtreamLiveStreamDto>>

    @GET
    suspend fun getVodCategories(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_categories"
    ): Response<List<XtreamCategoryDto>>

    @GET
    suspend fun getVodStreams(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_streams",
        @Query("category_id") categoryId: String? = null
    ): Response<List<XtreamVodStreamDto>>

    @GET
    suspend fun getSeriesCategories(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_categories"
    ): Response<List<XtreamCategoryDto>>

    @GET
    suspend fun getSeries(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series",
        @Query("category_id") categoryId: String? = null
    ): Response<List<XtreamSeriesDto>>

    @GET
    suspend fun getSeriesInfo(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_info",
        @Query("series_id") seriesId: String
    ): Response<XtreamSeriesInfoResponseDto>

    @GET
    suspend fun getShortEpg(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_short_epg",
        @Query("stream_id") streamId: String,
        @Query("limit") limit: Int = 10
    ): Response<XtreamEpgResponseDto>
}
