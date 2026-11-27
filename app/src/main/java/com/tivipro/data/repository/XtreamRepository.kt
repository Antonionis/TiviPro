package com.tivipro.data.repository

import android.util.Base64
import org.json.JSONObject
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import com.tivipro.data.network.XtreamCodesService
import com.tivipro.model.AuthResponse
import com.tivipro.model.Category
import com.tivipro.model.CategoryType
import com.tivipro.model.Episode
import com.tivipro.model.EpgProgram
import com.tivipro.model.LiveChannel
import com.tivipro.model.ServerInfo
import com.tivipro.model.Series
import com.tivipro.model.VodMovie
import com.tivipro.model.XtreamUserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface XtreamRepository {
    suspend fun authenticate(serverUrl: String, username: String, password: String): Result<AuthResponse>
    suspend fun getLiveCategories(serverUrl: String, username: String, password: String): Result<List<Category>>
    suspend fun getLiveChannels(serverUrl: String, username: String, password: String, categoryId: String? = null): Result<List<LiveChannel>>
    suspend fun getVodCategories(serverUrl: String, username: String, password: String): Result<List<Category>>
    suspend fun getVodMovies(serverUrl: String, username: String, password: String, categoryId: String? = null): Result<List<VodMovie>>
    suspend fun getSeriesCategories(serverUrl: String, username: String, password: String): Result<List<Category>>
    suspend fun getSeriesList(serverUrl: String, username: String, password: String, categoryId: String? = null): Result<List<Series>>
    suspend fun getSeriesEpisodes(serverUrl: String, username: String, password: String, seriesId: String): Result<List<Episode>>
    suspend fun getShortEpg(serverUrl: String, username: String, password: String, streamId: String, limit: Int = 10): Result<List<EpgProgram>>

    fun buildLiveStreamUrl(serverUrl: String, username: String, password: String, streamId: String, extension: String = "ts"): String
    fun buildVodStreamUrl(serverUrl: String, username: String, password: String, streamId: String, extension: String = "mp4"): String
    fun buildSeriesStreamUrl(serverUrl: String, username: String, password: String, episodeId: String, extension: String = "mp4"): String
    fun buildCatchupStreamUrl(serverUrl: String, username: String, password: String, streamId: String, startTimestampSec: Long, durationMinutes: Long, extension: String = "ts"): String
}

class XtreamRepositoryImpl(
    private val serviceProvider: (Boolean) -> XtreamCodesService
) : XtreamRepository {

    constructor(service: XtreamCodesService) : this({ service })
    constructor() : this({ doh -> com.tivipro.data.network.NetworkModule.createXtreamCodesService(doh) })

    private val service: XtreamCodesService
        get() = serviceProvider(com.tivipro.data.network.NetworkModule.isDohEnabled.value)

    companion object {
        fun createDefaultService(): XtreamCodesService {
            return com.tivipro.data.network.NetworkModule.createXtreamCodesService()
        }
    }

    private fun normalizePlayerApiUrl(serverUrl: String): String {
        var clean = serverUrl.trim().removeSuffix("/").removeSuffix("/player_api.php").trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "http://$clean"
        }
        return "$clean/player_api.php"
    }

    private fun formatBaseServerUrl(serverUrl: String): String {
        var clean = serverUrl.trim().removeSuffix("/").removeSuffix("/player_api.php").trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "http://$clean"
        }
        return clean
    }

    override suspend fun authenticate(
        serverUrl: String,
        username: String,
        password: String
    ): Result<AuthResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = normalizePlayerApiUrl(serverUrl)
            val client = com.tivipro.data.network.NetworkModule.getOkHttpClient()

            val httpUrl = try {
                endpoint.toHttpUrlOrNull()?.newBuilder()
                    ?.addQueryParameter("username", username.trim())
                    ?.addQueryParameter("password", password.trim())
                    ?.build()
            } catch (_: Exception) { null }

            val targetUrl = httpUrl?.toString() ?: "$endpoint?username=${java.net.URLEncoder.encode(username.trim(), "UTF-8")}&password=${java.net.URLEncoder.encode(password.trim(), "UTF-8")}"

            var request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "IPTVSmartersPro")
                .header("Accept", "*/*")
                .build()

            var response = try {
                client.newCall(request).execute()
            } catch (e: Exception) {
                if (targetUrl.startsWith("https://")) {
                    val httpTarget = targetUrl.replace("https://", "http://")
                    request = Request.Builder().url(httpTarget).header("User-Agent", "IPTVSmartersPro").build()
                    client.newCall(request).execute()
                } else throw e
            }

            if (!response.isSuccessful && targetUrl.startsWith("https://")) {
                val httpTarget = targetUrl.replace("https://", "http://")
                try {
                    val httpReq = Request.Builder().url(httpTarget).header("User-Agent", "IPTVSmartersPro").build()
                    val httpResp = client.newCall(httpReq).execute()
                    if (httpResp.isSuccessful) {
                        response = httpResp
                    }
                } catch (_: Exception) {}
            }

            val responseBodyStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val code = response.code
                val msg = if (code == 502) {
                    "Servidor IPTV inaccesible (HTTP 502 Bad Gateway). Compruebe la URL y puerto."
                } else {
                    "Fallo de conexión con el servidor (HTTP $code)"
                }
                throw Exception(msg)
            }

            // Parse response body with fail-safe JSONObject fallback
            val json = try {
                JSONObject(responseBodyStr)
            } catch (_: Exception) {
                val firstBrace = responseBodyStr.indexOf('{')
                val lastBrace = responseBodyStr.lastIndexOf('}')
                if (firstBrace >= 0 && lastBrace > firstBrace) {
                    JSONObject(responseBodyStr.substring(firstBrace, lastBrace + 1))
                } else null
            } ?: throw Exception("El servidor no devolvió una respuesta válida.")

            val userInfoObj = json.optJSONObject("user_info")
            val serverInfoObj = json.optJSONObject("server_info")

            val authVal = userInfoObj?.optInt("auth", 1) ?: json.optInt("auth", 1)
            if (authVal == 0) {
                val msg = userInfoObj?.optString("message") ?: "Usuario o contraseña incorrectos"
                throw Exception(msg)
            }

            val userInfo = XtreamUserInfo(
                username = userInfoObj?.optString("username", username)?.ifBlank { username } ?: username,
                password = userInfoObj?.optString("password", password)?.ifBlank { password } ?: password,
                message = userInfoObj?.optString("message"),
                auth = authVal,
                status = userInfoObj?.optString("status", "Active") ?: "Active",
                expDate = userInfoObj?.optString("exp_date"),
                isTrial = userInfoObj?.optString("is_trial"),
                activeCons = userInfoObj?.optString("active_cons", "0") ?: "0",
                createdAt = userInfoObj?.optString("created_at"),
                maxConnections = userInfoObj?.optString("max_connections", "1") ?: "1",
                allowedOutputFormats = listOf("m3u8", "ts")
            )

            val serverInfo = ServerInfo(
                url = serverInfoObj?.optString("url") ?: formatBaseServerUrl(serverUrl),
                port = serverInfoObj?.optString("port"),
                httpsPort = serverInfoObj?.optString("https_port"),
                serverProtocol = serverInfoObj?.optString("server_protocol"),
                rtmpPort = serverInfoObj?.optString("rtmp_port"),
                timezone = serverInfoObj?.optString("timezone"),
                now = serverInfoObj?.optLong("timestamp", 0L)
            )

            AuthResponse(userInfo = userInfo, serverInfo = serverInfo)
        }
    }

    private fun cleanCategoryName(rawName: String?): String {
        if (rawName.isNullOrBlank()) return "Uncategorized"
        var clean = rawName
            .replace(Regex("""(?i)^\s*EU\s*-\s*ES\b\s*:?"""), "")
            .replace(Regex("""(?i)^\s*EU-ES\b\s*:?"""), "")
            .replace(Regex("""(?i)^\s*EU\s+ES-?\s*"""), "")
            .replace(Regex("""(?i)^\s*ES\s*-\s*ES\b\s*:?"""), "")
            .replace(Regex("""(?i)^\s*ES\s*-\s*"""), "")
            .replace(Regex("""(?i)^\s*\|?\s*ES\s*\|\s*"""), "")
            .trim()

        clean = when (clean.uppercase()) {
            "MCINEMA", "M CINEMA", "M-CINEMA" -> "CINE"
            "M SERIES", "MSERIES", "M-SERIES" -> "SERIES"
            "24/7 ALQUILLER" -> "24/7 ALQUILER"
            "ANIMACCION", "ANIMACCIÓN" -> "ANIMACIÓN"
            else -> clean
        }

        return clean.ifBlank { rawName.trim() }
    }

    private fun cleanChannelDisplayName(rawName: String?): String {
        if (rawName.isNullOrBlank()) return "Channel"
        return rawName
            .replace(Regex("""(?i)^\s*\((?:ES|ESP)\)\s*"""), "")
            .replace(Regex("""(?i)^\s*\[(?:ES|ESP)]\s*"""), "")
            .replace(Regex("""(?i)^\s*ES\s*-\s*"""), "")
            .replace(Regex("""(?i)^\s*ES-\s*"""), "")
            .replace(Regex("""(?i)^\s*ES:\s*"""), "")
            .replace(Regex("""(?i)^\s*ES\|\s*"""), "")
            .replace(Regex("""(?i)^\s*ES\s+4k-?\s*"""), "")
            .replace(Regex("""(?i)^\s*ES\s+fhd-?\s*"""), "")
            .replace(Regex("""(?i)^\s*ES\s+hd-?\s*"""), "")
            .replace(Regex("""(?i)^\s*ES\s+(?=[A-Za-z0-9])"""), "")
            .trim()
            .ifBlank { rawName.trim() }
    }

    override suspend fun getLiveCategories(
        serverUrl: String,
        username: String,
        password: String
    ): Result<List<Category>> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = normalizePlayerApiUrl(serverUrl)
            val response = service.getLiveCategories(endpoint, username, password)
            val list = response.body() ?: emptyList()
            list.mapNotNull { dto ->
                val id = dto.categoryId ?: return@mapNotNull null
                Category(id = id, name = cleanCategoryName(dto.categoryName), type = CategoryType.LIVE)
            }
        }
    }

    override suspend fun getLiveChannels(
        serverUrl: String,
        username: String,
        password: String,
        categoryId: String?
    ): Result<List<LiveChannel>> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = normalizePlayerApiUrl(serverUrl)
            val response = service.getLiveStreams(endpoint, username, password, categoryId = categoryId)
            val list = response.body() ?: emptyList()
            val baseUrl = formatBaseServerUrl(serverUrl)

            list.mapNotNull { dto ->
                val rawId = safeToString(dto.streamId)
                if (rawId.isBlank()) return@mapNotNull null
                val streamUrl = buildLiveStreamUrl(baseUrl, username, password, rawId, "ts")

                val archiveVal = when (val a = dto.tvArchive) {
                    is Number -> a.toInt()
                    is String -> a.toIntOrNull() ?: 0
                    else -> 0
                }
                val durationVal = when (val d = dto.tvArchiveDuration) {
                    is Number -> d.toInt()
                    is String -> d.toIntOrNull() ?: 0
                    else -> 0
                }

                LiveChannel(
                    id = rawId,
                    name = cleanChannelDisplayName(dto.name),
                    iconUrl = dto.streamIcon,
                    streamUrl = streamUrl,
                    categoryId = dto.categoryId ?: "0",
                    epgId = dto.epgChannelId,
                    streamType = dto.streamType ?: "live",
                    channelNumber = dto.num,
                    tvArchive = archiveVal,
                    tvArchiveDuration = durationVal
                )
            }
        }
    }

    override fun buildCatchupStreamUrl(
        serverUrl: String,
        username: String,
        password: String,
        streamId: String,
        startTimestampSec: Long,
        durationMinutes: Long,
        extension: String
    ): String {
        val base = formatBaseServerUrl(serverUrl)
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = startTimestampSec * 1000L
        }
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd:HH-mm", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val startFormatted = sdf.format(cal.time)
        return "$base/timeshift/$username/$password/$durationMinutes/$startFormatted/$streamId.$extension"
    }

    override suspend fun getVodCategories(
        serverUrl: String,
        username: String,
        password: String
    ): Result<List<Category>> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = normalizePlayerApiUrl(serverUrl)
            val response = service.getVodCategories(endpoint, username, password)
            val list = response.body() ?: emptyList()
            list.mapNotNull { dto ->
                val id = dto.categoryId ?: return@mapNotNull null
                Category(id = id, name = cleanCategoryName(dto.categoryName), type = CategoryType.VOD)
            }
        }
    }

    private fun fixPosterUrl(baseUrl: String, rawUrl: String?): String? {
        if (rawUrl.isNullOrBlank()) return null
        val clean = rawUrl.trim()
        if (clean.startsWith("http://") || clean.startsWith("https://")) {
            return clean
        }
        val cleanBase = baseUrl.trim().removeSuffix("/")
        val cleanPath = if (clean.startsWith("/")) clean else "/$clean"
        return "$cleanBase$cleanPath"
    }

    override suspend fun getVodMovies(
        serverUrl: String,
        username: String,
        password: String,
        categoryId: String?
    ): Result<List<VodMovie>> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = normalizePlayerApiUrl(serverUrl)
            val response = service.getVodStreams(endpoint, username, password, categoryId = categoryId)
            val list = response.body() ?: emptyList()
            val baseUrl = formatBaseServerUrl(serverUrl)

            list.mapNotNull { dto ->
                val rawId = safeToString(dto.streamId)
                if (rawId.isBlank()) return@mapNotNull null
                val ext = dto.containerExtension?.ifBlank { "mp4" } ?: "mp4"
                val streamUrl = buildVodStreamUrl(baseUrl, username, password, rawId, ext)

                VodMovie(
                    id = rawId,
                    name = dto.name ?: "Movie $rawId",
                    posterUrl = fixPosterUrl(baseUrl, dto.streamIcon),
                    streamUrl = streamUrl,
                    categoryId = dto.categoryId ?: "0",
                    rating = safeToDouble(dto.rating),
                    containerExtension = ext,
                    releaseDate = dto.added
                )
            }
        }
    }

    override suspend fun getSeriesCategories(
        serverUrl: String,
        username: String,
        password: String
    ): Result<List<Category>> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = normalizePlayerApiUrl(serverUrl)
            val response = service.getSeriesCategories(endpoint, username, password)
            val list = response.body() ?: emptyList()
            list.mapNotNull { dto ->
                val id = dto.categoryId ?: return@mapNotNull null
                Category(id = id, name = cleanCategoryName(dto.categoryName), type = CategoryType.SERIES)
            }
        }
    }

    override suspend fun getSeriesList(
        serverUrl: String,
        username: String,
        password: String,
        categoryId: String?
    ): Result<List<Series>> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = normalizePlayerApiUrl(serverUrl)
            val response = service.getSeries(endpoint, username, password, categoryId = categoryId)
            val list = response.body() ?: emptyList()
            val baseUrl = formatBaseServerUrl(serverUrl)

            list.mapNotNull { dto ->
                val rawId = safeToString(dto.seriesId)
                if (rawId.isBlank()) return@mapNotNull null

                Series(
                    id = rawId,
                    name = dto.name ?: "Series $rawId",
                    posterUrl = fixPosterUrl(baseUrl, dto.cover),
                    categoryId = dto.categoryId ?: "0",
                    rating = safeToDouble(dto.rating),
                    overview = dto.overview
                )
            }
        }
    }

    override suspend fun getSeriesEpisodes(
        serverUrl: String,
        username: String,
        password: String,
        seriesId: String
    ): Result<List<Episode>> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = normalizePlayerApiUrl(serverUrl)
            val baseUrl = formatBaseServerUrl(serverUrl)
            val client = com.tivipro.data.network.NetworkModule.getOkHttpClient()

            val targetUrl = "$endpoint?username=${java.net.URLEncoder.encode(username.trim(), "UTF-8")}&password=${java.net.URLEncoder.encode(password.trim(), "UTF-8")}&action=get_series_info&series_id=$seriesId"

            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "IPTVSmartersPro")
                .header("Accept", "*/*")
                .build()

            val response = try {
                client.newCall(request).execute()
            } catch (_: Exception) { null }

            val responseBodyStr = response?.body?.string() ?: ""

            val resultEpisodes = mutableListOf<Episode>()

            val json = try {
                if (responseBodyStr.isNotBlank()) JSONObject(responseBodyStr) else null
            } catch (_: Exception) {
                val firstBrace = responseBodyStr.indexOf('{')
                val lastBrace = responseBodyStr.lastIndexOf('}')
                if (firstBrace >= 0 && lastBrace > firstBrace) {
                    try { JSONObject(responseBodyStr.substring(firstBrace, lastBrace + 1)) } catch (_: Exception) { null }
                } else null
            }

            if (json != null) {
                val episodesObj = json.optJSONObject("episodes")
                val episodesArr = json.optJSONArray("episodes")

                if (episodesObj != null) {
                    val seasonKeys = episodesObj.keys()
                    while (seasonKeys.hasNext()) {
                        val seasonKey = seasonKeys.next()
                        val epArray = episodesObj.optJSONArray(seasonKey) ?: continue
                        for (i in 0 until epArray.length()) {
                            val epObj = epArray.optJSONObject(i) ?: continue
                            val ep = parseEpisodeFromJsonObject(epObj, baseUrl, username, password)
                            if (ep != null) resultEpisodes.add(ep)
                        }
                    }
                } else if (episodesArr != null) {
                    for (i in 0 until episodesArr.length()) {
                        val epObj = episodesArr.optJSONObject(i) ?: continue
                        val ep = parseEpisodeFromJsonObject(epObj, baseUrl, username, password)
                        if (ep != null) resultEpisodes.add(ep)
                    }
                }
            }

            // Fallback to Moshi service if JSONObject produced no episodes
            if (resultEpisodes.isEmpty()) {
                val moshiResp = try {
                    service.getSeriesInfo(endpoint, username, password, seriesId = seriesId)
                } catch (_: Exception) { null }

                moshiResp?.body()?.episodes?.forEach { (_, epList) ->
                    epList.forEach { dto ->
                        val idStr = safeToString(dto.id)
                        if (idStr.isNotBlank()) {
                            val ext = dto.containerExtension?.ifBlank { "mp4" } ?: "mp4"
                            val epNum = safeToInt(dto.episodeNum) ?: 1
                            val sNum = safeToInt(dto.season) ?: 1
                            val streamUrl = if (!dto.directSource.isNullOrBlank()) {
                                dto.directSource
                            } else {
                                buildSeriesStreamUrl(baseUrl, username, password, idStr, ext)
                            }
                            val infoMap = dto.info as? Map<*, *>
                            val rawImage = dto.movieImage
                                ?: dto.stillPath
                                ?: dto.cover
                                ?: dto.coverBig
                                ?: (infoMap?.get("movie_image") as? String)
                                ?: (infoMap?.get("still_path") as? String)
                                ?: (infoMap?.get("cover") as? String)
                                ?: (infoMap?.get("cover_big") as? String)

                            val resolvedIconUrl = if (!rawImage.isNullOrBlank()) {
                                if (rawImage.startsWith("http://") || rawImage.startsWith("https://")) {
                                    rawImage
                                } else if (rawImage.startsWith("/")) {
                                    "https://image.tmdb.org/t/p/w500$rawImage"
                                } else {
                                    "https://image.tmdb.org/t/p/w500/$rawImage"
                                }
                            } else null

                            resultEpisodes.add(
                                Episode(
                                    id = idStr,
                                    title = dto.title ?: "Episodio $epNum",
                                    episodeNum = epNum,
                                    seasonNum = sNum,
                                    streamUrl = streamUrl,
                                    containerExtension = ext,
                                    duration = dto.duration,
                                    overview = dto.overview,
                                    iconUrl = resolvedIconUrl
                                )
                            )
                        }
                    }
                }
            }

            resultEpisodes
        }
    }

    private fun parseEpisodeFromJsonObject(
        obj: JSONObject,
        baseUrl: String,
        username: String,
        password: String
    ): Episode? {
        val idStr = safeToString(obj.opt("id") ?: obj.opt("stream_id"))
        if (idStr.isBlank()) return null

        val ext = obj.optString("container_extension", "mp4").ifBlank { "mp4" }
        val epNum = obj.optInt("episode_num", 1).let { if (it <= 0) 1 else it }
        val sNum = obj.optInt("season", 1).let { if (it <= 0) 1 else it }
        val title = obj.optString("title").ifBlank { "Episodio $epNum" }
        val directSource = obj.optString("direct_source")

        val streamUrl = if (directSource.isNotBlank()) {
            directSource
        } else {
            buildSeriesStreamUrl(baseUrl, username, password, idStr, ext)
        }

        val infoObj = obj.optJSONObject("info")
        val rawImage = obj.optString("movie_image").ifBlank {
            obj.optString("still_path").ifBlank {
                obj.optString("cover").ifBlank {
                    infoObj?.optString("movie_image")?.ifBlank {
                        infoObj?.optString("still_path")
                    } ?: ""
                }
            }
        }

        val resolvedIconUrl = if (rawImage.isNotBlank()) {
            if (rawImage.startsWith("http://") || rawImage.startsWith("https://")) {
                rawImage
            } else if (rawImage.startsWith("/")) {
                "https://image.tmdb.org/t/p/w500$rawImage"
            } else {
                "https://image.tmdb.org/t/p/w500/$rawImage"
            }
        } else null

        return Episode(
            id = idStr,
            title = title,
            episodeNum = epNum,
            seasonNum = sNum,
            streamUrl = streamUrl,
            containerExtension = ext,
            duration = obj.optString("duration").ifBlank { null },
            overview = obj.optString("overview").ifBlank { null },
            iconUrl = resolvedIconUrl
        )
    }

    override suspend fun getShortEpg(
        serverUrl: String,
        username: String,
        password: String,
        streamId: String,
        limit: Int
    ): Result<List<EpgProgram>> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = normalizePlayerApiUrl(serverUrl)
            val response = service.getShortEpg(endpoint, username, password, streamId = streamId, limit = limit)
            val listings = response.body()?.epgListings ?: emptyList()

            listings.mapNotNull { dto ->
                val startTs = safeToLong(dto.startTimestamp) ?: return@mapNotNull null
                val stopTs = safeToLong(dto.stopTimestamp) ?: return@mapNotNull null

                EpgProgram(
                    id = dto.id ?: "epg_${streamId}_$startTs",
                    channelId = dto.epgId ?: streamId,
                    title = safeDecodeText(dto.title) ?: "Program",
                    description = safeDecodeText(dto.description),
                    startTimestamp = startTs,
                    endTimestamp = stopTs
                )
            }
        }
    }

    override fun buildLiveStreamUrl(
        serverUrl: String,
        username: String,
        password: String,
        streamId: String,
        extension: String
    ): String {
        val base = formatBaseServerUrl(serverUrl)
        if (extension.isBlank()) {
            return "$base/live/$username/$password/$streamId"
        }
        val ext = extension.removePrefix(".")
        return "$base/live/$username/$password/$streamId.$ext"
    }

    override fun buildVodStreamUrl(
        serverUrl: String,
        username: String,
        password: String,
        streamId: String,
        extension: String
    ): String {
        val base = formatBaseServerUrl(serverUrl)
        if (extension.isBlank()) {
            return "$base/movie/$username/$password/$streamId"
        }
        val ext = extension.removePrefix(".")
        return "$base/movie/$username/$password/$streamId.$ext"
    }

    override fun buildSeriesStreamUrl(
        serverUrl: String,
        username: String,
        password: String,
        episodeId: String,
        extension: String
    ): String {
        val base = formatBaseServerUrl(serverUrl)
        if (extension.isBlank()) {
            return "$base/series/$username/$password/$episodeId"
        }
        val ext = extension.removePrefix(".")
        return "$base/series/$username/$password/$episodeId.$ext"
    }

    private fun safeToString(value: Any?): String {
        return when (value) {
            null -> ""
            is String -> value
            is Double -> value.toLong().toString()
            else -> value.toString()
        }
    }

    private fun safeToInt(value: Any?): Int? {
        return when (value) {
            null -> null
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> value.toString().toIntOrNull()
        }
    }

    private fun safeToDouble(value: Any?): Double? {
        return when (value) {
            null -> null
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }
    }

    private fun safeToLong(value: Any?): Long? {
        return when (value) {
            null -> null
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
            else -> null
        }
    }

    private fun safeDecodeText(text: String?): String? {
        if (text.isNullOrBlank()) return text
        if (text.contains(" ") || text.contains("\n") || text.contains("\r")) return text
        return try {
            val bytes = Base64.decode(text, Base64.DEFAULT)
            val decoded = String(bytes, Charsets.UTF_8)
            if (decoded.none { it.code in 0..8 || it.code in 11..12 || it.code in 14..31 }) {
                decoded
            } else {
                text
            }
        } catch (_: Exception) {
            text
        }
    }
}
