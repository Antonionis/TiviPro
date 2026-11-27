package com.tivipro.data.network

import com.tivipro.model.CastMember
import com.tivipro.model.Series
import com.tivipro.model.VodMovie
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

    fun formatCertificationDisplay(rawCert: String?): String {
        if (rawCert.isNullOrBlank()) return "+12"
        val clean = rawCert.trim().uppercase()
        return when {
            clean == "16" || clean == "+16" || clean == "16+" -> "+16"
            clean == "18" || clean == "+18" || clean == "18+" || clean.contains("NC-17") -> "+18"
            clean == "14" || clean == "+14" || clean == "14+" -> "+14"
            clean == "12" || clean == "+12" || clean == "12+" -> "+12"
            clean == "7" || clean == "+7" || clean == "7+" -> "+7"
            clean.contains("TV-MA") || clean == "R" -> "+16"
            clean.contains("TV-14") || clean.contains("PG-13") -> "+13"
            clean.contains("TP") || clean == "A" || clean == "G" || clean == "ALL" -> "TP"
            clean.startsWith("+") -> clean
            clean.all { it.isDigit() } -> "+$clean"
            else -> clean
        }
    }

    data class TmdbMovieDetail(
        val overview: String? = null,
        val rating: Double? = null,
        val posterPath: String? = null,
        val backdropPath: String? = null,
        val logoPath: String? = null,
        val genres: String? = null,
        val certification: String? = null,
        val runtime: Int? = null,
        val releaseDate: String? = null,
        val trailerUrl: String? = null,
        val cast: List<CastMember> = emptyList()
    )

    data class TmdbMovieKnownFor(
        val title: String,
        val posterPath: String? = null,
        val character: String? = null,
        val releaseYear: String? = null
    )

    data class TmdbSeriesDetail(
        val overview: String? = null,
        val rating: Double? = null,
        val posterPath: String? = null,
        val backdropPath: String? = null,
        val logoPath: String? = null,
        val genres: String? = null,
        val releaseDate: String? = null,
        val certification: String? = null
    )

    data class TmdbPersonDetail(
        val name: String,
        val biography: String? = null,
        val birthday: String? = null,
        val placeOfBirth: String? = null,
        val profilePath: String? = null,
        val knownForDepartment: String? = null,
        val knownForMovies: List<TmdbMovieKnownFor> = emptyList()
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

            val detailUrl = "https://api.themoviedb.org/3/movie/$tmdbId?api_key=$TMDB_API_KEY&append_to_response=videos,credits,images&language=es-ES"
            val detailRequest = Request.Builder().url(detailUrl).build()

            client.newCall(detailRequest).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body.string()
                val json = JSONObject(body)

                val overview = json.optString("overview", "").trim()
                val voteAvg = json.optDouble("vote_average", 0.0)
                val poster = json.optString("poster_path", "")
                val backdrop = json.optString("backdrop_path", "").ifBlank { poster }
                val releaseDate = json.optString("release_date", "")
                val runtime = json.optInt("runtime", 0)

                val ratingVal = if (!voteAvg.isNaN() && voteAvg > 0.0) voteAvg else null
                val posterUrl = if (poster.isNotBlank() && poster != "null") {
                    if (poster.startsWith("http")) poster else "https://image.tmdb.org/t/p/w500$poster"
                } else null
                val backdropUrl = if (backdrop.isNotBlank() && backdrop != "null") {
                    if (backdrop.startsWith("http")) backdrop else "https://image.tmdb.org/t/p/w1280$backdrop"
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

                // Extract clear transparent logo PNG
                var logoUrl: String? = null
                val imagesObj = json.optJSONObject("images")
                val logosArr = imagesObj?.optJSONArray("logos")
                if (logosArr != null && logosArr.length() > 0) {
                    val logoPath = logosArr.getJSONObject(0).optString("file_path", "")
                    if (logoPath.isNotBlank() && logoPath != "null") {
                        logoUrl = if (logoPath.startsWith("http")) logoPath else "https://image.tmdb.org/t/p/w500$logoPath"
                    }
                }

                // Extract certification for Movies
                var certStr: String? = null
                val relDatesObj = json.optJSONObject("release_dates")
                val relResults = relDatesObj?.optJSONArray("results")
                if (relResults != null) {
                    for (i in 0 until relResults.length()) {
                        val r = relResults.getJSONObject(i)
                        val country = r.optString("iso_3166_1", "")
                        if (country.equals("ES", ignoreCase = true) || (certStr == null && country.equals("US", ignoreCase = true))) {
                            val datesArr = r.optJSONArray("release_dates")
                            if (datesArr != null && datesArr.length() > 0) {
                                for (j in 0 until datesArr.length()) {
                                    val c = datesArr.getJSONObject(j).optString("certification", "")
                                    if (c.isNotBlank()) {
                                        certStr = c
                                        break
                                    }
                                }
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
                        val profilePath = c.optString("profile_path", "")
                        val profileUrl = if (profilePath.isNotBlank() && profilePath != "null") {
                            if (profilePath.startsWith("http")) profilePath
                            else "https://image.tmdb.org/t/p/w185$profilePath"
                        } else null

                        if (name.isNotBlank()) {
                            castList.add(
                                CastMember(
                                    name = name,
                                    role = if (character.isNotBlank()) character else "Actor",
                                    profileUrl = profileUrl
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
                    backdropPath = backdropUrl,
                    logoPath = logoUrl,
                    genres = genresStr,
                    certification = certStr,
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

    suspend fun fetchTmdbPersonDetail(actorName: String): TmdbPersonDetail? = withContext(Dispatchers.IO) {
        try {
            val encodedName = URLEncoder.encode(actorName.trim(), "UTF-8")
            val client = NetworkModule.getOkHttpClient()

            // 1. Search Person by Name
            val searchUrl = "https://api.themoviedb.org/3/search/person?api_key=$TMDB_API_KEY&query=$encodedName&language=es-ES"
            val searchRequest = Request.Builder().url(searchUrl).build()

            var personId = 0
            client.newCall(searchRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body.string()
                    val json = JSONObject(body)
                    val results = json.optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        personId = results.getJSONObject(0).optInt("id", 0)
                    }
                }
            }

            if (personId == 0) return@withContext null

            // 2. Fetch Person Details & Combined Movie/TV Credits
            val detailUrl = "https://api.themoviedb.org/3/person/$personId?api_key=$TMDB_API_KEY&append_to_response=combined_credits&language=es-ES"
            val detailRequest = Request.Builder().url(detailUrl).build()

            client.newCall(detailRequest).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body.string()
                val json = JSONObject(body)

                val name = json.optString("name", actorName)
                val biography = json.optString("biography", "").trim()
                val birthday = json.optString("birthday", "").ifBlank { null }
                val placeOfBirth = json.optString("place_of_birth", "").ifBlank { null }
                val profile = json.optString("profile_path", "")
                val department = json.optString("known_for_department", "Interpretación")

                val profileUrl = if (profile.isNotBlank() && profile != "null") {
                    if (profile.startsWith("http")) profile else "https://image.tmdb.org/t/p/w500$profile"
                } else null

                val knownForList = mutableListOf<TmdbMovieKnownFor>()
                val creditsObj = json.optJSONObject("combined_credits") ?: json.optJSONObject("movie_credits")
                val castArr = creditsObj?.optJSONArray("cast")
                if (castArr != null) {
                    val rawItems = mutableListOf<JSONObject>()
                    for (i in 0 until castArr.length()) {
                        rawItems.add(castArr.getJSONObject(i))
                    }
                    // Sort by popularity descending to get actor's top famous movies & series!
                    rawItems.sortByDescending { it.optDouble("popularity", 0.0) }

                    for (mObj in rawItems) {
                        if (knownForList.size >= 16) break
                        val mTitle = mObj.optString("title", "").ifBlank { mObj.optString("name", "") }
                        if (mTitle.isNotBlank()) {
                            val poster = mObj.optString("poster_path", "")
                            val pUrl = if (poster.isNotBlank() && poster != "null") {
                                if (poster.startsWith("http")) poster else "https://image.tmdb.org/t/p/w342$poster"
                            } else null
                            val relDate = mObj.optString("release_date", "").ifBlank { mObj.optString("first_air_date", "") }
                            val yr = if (relDate.length >= 4) relDate.take(4) else null

                            if (knownForList.none { it.title.equals(mTitle, ignoreCase = true) }) {
                                knownForList.add(
                                    TmdbMovieKnownFor(
                                        title = mTitle,
                                        posterPath = pUrl,
                                        character = mObj.optString("character", "").ifBlank { null },
                                        releaseYear = yr
                                    )
                                )
                            }
                        }
                    }
                }

                TmdbPersonDetail(
                    name = name,
                    biography = if (biography.isNotBlank()) biography else null,
                    birthday = birthday,
                    placeOfBirth = placeOfBirth,
                    profilePath = profileUrl,
                    knownForDepartment = if (department == "Acting") "Interpretación" else department,
                    knownForMovies = knownForList
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
            backdropUrl = detail.backdropPath ?: movie.backdropUrl,
            logoUrl = detail.logoPath ?: movie.logoUrl,
            genres = detail.genres ?: movie.genres,
            certification = detail.certification ?: movie.certification,
            durationMinutes = detail.runtime ?: movie.durationMinutes,
            releaseDate = detail.releaseDate ?: movie.releaseDate,
            trailerUrl = detail.trailerUrl ?: movie.trailerUrl,
            cast = if (detail.cast.isNotEmpty()) detail.cast else movie.cast
        )
    }

    suspend fun fetchTmdbSeriesDetail(rawTitle: String): TmdbSeriesDetail? = withContext(Dispatchers.IO) {
        try {
            val cleanedTitle = cleanTitleForTmdb(rawTitle)
            if (cleanedTitle.isBlank()) return@withContext null
            val encodedTitle = URLEncoder.encode(cleanedTitle, "UTF-8")
            val client = NetworkModule.getOkHttpClient()

            // 1. Search TV Series in TMDb
            val searchUrl = "https://api.themoviedb.org/3/search/tv?api_key=$TMDB_API_KEY&query=$encodedTitle&language=es-ES"
            val searchRequest = Request.Builder().url(searchUrl).build()

            var tvId = 0
            client.newCall(searchRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body.string()
                    val json = JSONObject(body)
                    val results = json.optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        tvId = results.getJSONObject(0).optInt("id", 0)
                    }
                }
            }

            if (tvId == 0) return@withContext null

            // 2. Fetch TV Series Details & Images (including transparent logo)
            val detailUrl = "https://api.themoviedb.org/3/tv/$tvId?api_key=$TMDB_API_KEY&append_to_response=images,content_ratings&language=es-ES"
            val detailRequest = Request.Builder().url(detailUrl).build()

            client.newCall(detailRequest).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body.string()
                val json = JSONObject(body)

                val overview = json.optString("overview", "").trim()
                val voteAvg = json.optDouble("vote_average", 0.0)
                val poster = json.optString("poster_path", "")
                val backdrop = json.optString("backdrop_path", "").ifBlank { poster }
                val firstAirDate = json.optString("first_air_date", "")

                val ratingVal = if (!voteAvg.isNaN() && voteAvg > 0.0) voteAvg else null
                val posterUrl = if (poster.isNotBlank() && poster != "null") {
                    if (poster.startsWith("http")) poster else "https://image.tmdb.org/t/p/w500$poster"
                } else null
                val backdropUrl = if (backdrop.isNotBlank() && backdrop != "null") {
                    if (backdrop.startsWith("http")) backdrop else "https://image.tmdb.org/t/p/w1280$backdrop"
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

                // Extract certification for TV Series (prioritizing Spain ES)
                var certStr: String? = null
                val ratingsObj = json.optJSONObject("content_ratings")
                val ratingsResults = ratingsObj?.optJSONArray("results")
                if (ratingsResults != null) {
                    for (i in 0 until ratingsResults.length()) {
                        val r = ratingsResults.getJSONObject(i)
                        if (r.optString("iso_3166_1", "").equals("ES", ignoreCase = true)) {
                            val c = r.optString("rating", "")
                            if (c.isNotBlank()) {
                                certStr = c
                                break
                            }
                        }
                    }
                    if (certStr == null) {
                        for (i in 0 until ratingsResults.length()) {
                            val r = ratingsResults.getJSONObject(i)
                            if (r.optString("iso_3166_1", "").equals("US", ignoreCase = true)) {
                                val c = r.optString("rating", "")
                                if (c.isNotBlank()) {
                                    certStr = c
                                    break
                                }
                            }
                        }
                    }
                }

                // Extract clear transparent logo PNG
                var logoUrl: String? = null
                val imagesObj = json.optJSONObject("images")
                val logosArr = imagesObj?.optJSONArray("logos")
                if (logosArr != null && logosArr.length() > 0) {
                    val logoPath = logosArr.getJSONObject(0).optString("file_path", "")
                    if (logoPath.isNotBlank() && logoPath != "null") {
                        logoUrl = if (logoPath.startsWith("http")) logoPath else "https://image.tmdb.org/t/p/w500$logoPath"
                    }
                }

                TmdbSeriesDetail(
                    overview = if (overview.isNotBlank()) overview else null,
                    rating = ratingVal,
                    posterPath = posterUrl,
                    backdropPath = backdropUrl,
                    logoPath = logoUrl,
                    genres = genresStr,
                    releaseDate = if (firstAirDate.isNotBlank()) firstAirDate else null,
                    certification = certStr
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun enrichSeries(series: Series): Series {
        val detail = fetchTmdbSeriesDetail(series.name) ?: return series
        return series.copy(
            overview = detail.overview ?: series.overview,
            rating = detail.rating ?: series.rating,
            posterUrl = if (series.posterUrl.isNullOrBlank()) detail.posterPath else series.posterUrl,
            backdropUrl = detail.backdropPath ?: series.backdropUrl,
            logoUrl = detail.logoPath ?: series.logoUrl,
            releaseDate = detail.releaseDate ?: series.releaseDate,
            genres = detail.genres ?: series.genres,
            certification = detail.certification ?: series.certification
        )
    }
}