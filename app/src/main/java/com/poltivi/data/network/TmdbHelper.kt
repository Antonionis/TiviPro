package com.poltivi.data.network

import com.poltivi.model.CastMember
import com.poltivi.model.Series
import com.poltivi.model.VodMovie
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

    data class TmdbMovieDetail(
        val overview: String? = null,
        val rating: Double? = null,
        val posterPath: String? = null,
        val genres: String? = null,
        val runtime: Int? = null,
        val releaseDate: String? = null,
        val trailerUrl: String? = null,
        val cast: List<CastMember> = emptyList()
    )

    private fun cleanTitleForTmdb(rawTitle: String): String {
        var title = rawTitle
        title = title.replace(Regex("""\[.*?]"""), " ")
        title = title.replace(Regex("""\((?:\d{4}|4K|2K|HD|FHD|UHD|1080p|720p|Castellano|ES|Multi|Dual|Latino|Sub|Subtitulado)\)""", RegexOption.IGNORE_CASE), " ")
        title = title.replace(Regex("""\b(?:19|20)\d{2}\b"""), " ")
        title = title.replace(Regex("""(?i)\b(4k|fhd|hd|sd|uhd|hdr|1080p|720p|castellano|español|latino|dual|multi|subtitulado|sub|x264|x265|hevc)\b"""), " ")
        title = title.replace(Regex("""[^\p{L}\p{N}\s:-]"""), " ")
        return title.replace(Regex("""\s+"""), " ").trim()
    }

    private fun removeAccents(text: String): String {
        val normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
        return normalized.replace(Regex("""\p{InCombiningDiacriticalMarks}+"""), "")
    }

    private fun extractYearFromTitle(rawTitle: String): Int? {
        val match = Regex("""\b((?:19|20)\d{2})\b""").find(rawTitle)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    suspend fun fetchTmdbInfo(title: String, isMovie: Boolean): TmdbInfo? = withContext(Dispatchers.IO) {
        try {
            val cleaned = cleanTitleForTmdb(title)
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

    suspend fun fetchTmdbMovieDetail(rawTitle: String): TmdbMovieDetail? = withContext(Dispatchers.IO) {
        try {
            val cleanedTitle = cleanTitleForTmdb(rawTitle)
            if (cleanedTitle.isBlank()) return@withContext null
            val targetYear = extractYearFromTitle(rawTitle)

            val client = NetworkModule.getOkHttpClient()
            val searchQueries = mutableListOf(
                cleanedTitle,
                removeAccents(cleanedTitle)
            ).distinct()

            var tmdbId = 0

            for (q in searchQueries) {
                if (tmdbId != 0) break
                val encodedTitle = URLEncoder.encode(q, "UTF-8")
                val searchUrl = "https://api.themoviedb.org/3/search/movie?api_key=$TMDB_API_KEY&query=$encodedTitle&language=es-ES"
                val searchRequest = Request.Builder().url(searchUrl).build()

                client.newCall(searchRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body.string()
                        val json = JSONObject(body)
                        val results = json.optJSONArray("results")
                        if (results != null && results.length() > 0) {
                            if (targetYear != null) {
                                for (i in 0 until results.length()) {
                                    val item = results.getJSONObject(i)
                                    val releaseDate = item.optString("release_date", "")
                                    if (releaseDate.startsWith(targetYear.toString())) {
                                        tmdbId = item.optInt("id", 0)
                                        break
                                    }
                                }
                            }
                            if (tmdbId == 0) {
                                tmdbId = results.getJSONObject(0).optInt("id", 0)
                            }
                        }
                    }
                }
            }

            if (tmdbId == 0) return@withContext null

            val detailUrl = "https://api.themoviedb.org/3/movie/$tmdbId?api_key=$TMDB_API_KEY&append_to_response=videos,credits&language=es-ES"
            val detailRequest = Request.Builder().url(detailUrl).build()

            client.newCall(detailRequest).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body.string()
                val json = JSONObject(body)

                val overview = json.optString("overview", "").trim()
                val voteAvg = json.optDouble("vote_average", 0.0)
                val poster = json.optString("poster_path", "")
                val releaseDate = json.optString("release_date", "")
                val runtime = json.optInt("runtime", 0)

                val ratingVal = if (!voteAvg.isNaN() && voteAvg > 0.0) voteAvg else null
                val posterUrl = if (poster.isNotBlank() && poster != "null") {
                    "https://image.tmdb.org/t/p/w500$poster"
                } else null

                val genresArr = json.optJSONArray("genres")
                val genresList = mutableListOf<String>()
                if (genresArr != null) {
                    for (i in 0 until genresArr.length()) {
                        val g = genresArr.getJSONObject(i).optString("name", "")
                        if (g.isNotBlank()) genresList.add(g)
                    }
                }
                val genresStr = if (genresList.isNotEmpty()) genresList.joinToString(", ") else null

                val videosArr = json.optJSONObject("videos")?.optJSONArray("results")
                var trailerUrl: String? = null
                if (videosArr != null) {
                    for (i in 0 until videosArr.length()) {
                        val v = videosArr.getJSONObject(i)
                        val site = v.optString("site", "")
                        val type = v.optString("type", "")
                        val key = v.optString("key", "")
                        if (site.equals("YouTube", ignoreCase = true) && key.isNotBlank()) {
                            if (type.equals("Trailer", ignoreCase = true) || trailerUrl == null) {
                                trailerUrl = "https://www.youtube.com/watch?v=$key"
                                if (type.equals("Trailer", ignoreCase = true)) break
                            }
                        }
                    }
                }

                val creditsObj = json.optJSONObject("credits")
                val castList = mutableListOf<CastMember>()

                val castArr = creditsObj?.optJSONArray("cast")
                if (castArr != null) {
                    for (i in 0 until minOf(castArr.length(), 10)) {
                        val c = castArr.getJSONObject(i)
                        val name = c.optString("name", "")
                        val character = c.optString("character", "")
                        if (name.isNotBlank()) {
                            castList.add(
                                CastMember(
                                    name = name,
                                    role = if (character.isNotBlank()) character else "Actor"
                                )
                            )
                        }
                    }
                }

                val crewArr = creditsObj?.optJSONArray("crew")
                if (crewArr != null) {
                    for (i in 0 until crewArr.length()) {
                        val c = crewArr.getJSONObject(i)
                        val name = c.optString("name", "")
                        val job = c.optString("job", "")
                        if (name.isNotBlank() && (job.equals("Director", ignoreCase = true) || job.equals("Writer", ignoreCase = true) || job.equals("Screenplay", ignoreCase = true))) {
                            if (castList.none { it.name.equals(name, ignoreCase = true) }) {
                                castList.add(CastMember(name = name, role = job))
                            }
                        }
                    }
                }

                TmdbMovieDetail(
                    overview = if (overview.isNotBlank()) overview else null,
                    rating = ratingVal,
                    posterPath = posterUrl,
                    genres = genresStr,
                    runtime = if (runtime > 0) runtime else null,
                    releaseDate = if (releaseDate.isNotBlank()) releaseDate else null,
                    trailerUrl = trailerUrl,
                    cast = castList
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun enrichMovie(movie: VodMovie): VodMovie {
        val detail = fetchTmdbMovieDetail(movie.name) ?: return movie
        return movie.copy(
            description = detail.overview ?: movie.description,
            rating = detail.rating ?: movie.rating,
            posterUrl = if (movie.posterUrl.isNullOrBlank()) detail.posterPath else movie.posterUrl,
            genres = detail.genres ?: movie.genres,
            durationMinutes = detail.runtime ?: movie.durationMinutes,
            releaseDate = detail.releaseDate ?: movie.releaseDate,
            trailerUrl = detail.trailerUrl ?: movie.trailerUrl,
            cast = if (detail.cast.isNotEmpty()) detail.cast else movie.cast
        )
    }

    suspend fun enrichSeries(series: Series): Series {
        val cleaned = cleanTitleForTmdb(series.name)
        val info = fetchTmdbInfo(cleaned, isMovie = false) ?: return series
        return series.copy(
            overview = info.overview ?: series.overview,
            rating = info.rating ?: series.rating,
            posterUrl = if (series.posterUrl.isNullOrBlank()) info.posterPath else series.posterUrl
        )
    }
}