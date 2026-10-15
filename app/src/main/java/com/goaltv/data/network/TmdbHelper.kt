package com.goaltv.data.network

import com.goaltv.model.Series
import com.goaltv.model.VodMovie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

object TmdbHelper {

    private const val TMDB_API_KEY = "276d9385afda1d3cd5d628553d515b9e"

    data class TmdbInfo(
        val overview: String? = null,
        val rating: Double? = null,
        val posterPath: String? = null
    )

    private fun cleanTitle(title: String): String {
        return title.split("(")[0].trim()
    }

    suspend fun fetchTmdbInfo(title: String, isMovie: Boolean): TmdbInfo? = withContext(Dispatchers.IO) {
        try {
            val cleaned = cleanTitle(title)
            if (cleaned.isBlank()) return@withContext null

            val searchType = if (isMovie) "movie" else "tv"
            val encodedTitle = URLEncoder.encode(cleaned, "UTF-8")
            val url = "https://api.themoviedb.org/3/search/$searchType?api_key=$TMDB_API_KEY&query=$encodedTitle&language=es-ES"

            val client = NetworkModule.getOkHttpClient()
            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body.string()
                val json = JSONObject(body)
                val results = json.optJSONArray("results") ?: return@withContext null
                if (results.length() == 0) return@withContext null

                val match = results.getJSONObject(0)
                val overview = match.optString("overview", "").trim()
                val voteAvg = match.optDouble("vote_average", 0.0)
                val poster = match.optString("poster_path", "")

                val ratingVal = if (!voteAvg.isNaN() && voteAvg > 0.0) voteAvg else null
                val posterUrl = if (poster.isNotBlank() && poster != "null") {
                    "https://image.tmdb.org/t/p/w500$poster"
                } else null

                TmdbInfo(
                    overview = if (overview.isNotBlank()) overview else null,
                    rating = ratingVal,
                    posterPath = posterUrl
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun enrichMovie(movie: VodMovie): VodMovie {
        val info = fetchTmdbInfo(movie.name, isMovie = true) ?: return movie
        return movie.copy(
            description = info.overview ?: movie.description,
            rating = info.rating ?: movie.rating,
            posterUrl = if (movie.posterUrl.isNullOrBlank()) info.posterPath else movie.posterUrl
        )
    }

    suspend fun enrichSeries(series: Series): Series {
        val info = fetchTmdbInfo(series.name, isMovie = false) ?: return series
        return series.copy(
            overview = info.overview ?: series.overview,
            rating = info.rating ?: series.rating,
            posterUrl = if (series.posterUrl.isNullOrBlank()) info.posterPath else series.posterUrl
        )
    }
}
