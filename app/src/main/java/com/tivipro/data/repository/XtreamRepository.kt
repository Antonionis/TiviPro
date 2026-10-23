package com.tivipro.data.repository

import android.util.Base64
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
        var clean = serverUrl.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "http://$clean"
        }

        try {
            val uri = java.net.URI(clean)
            val scheme = uri.scheme ?: "http"
            val host = uri.host ?: ""
            val port = uri.port
            val portStr = if (port != -1 && port != 80 && port != 443) ":$port" else ""
            if (host.isNotBlank()) {
                clean = "$scheme://$host$portStr"
            }
        } catch (_: Exception) {}

        return "$clean/player_api.php"
    }

    private fun formatBaseServerUrl(serverUrl: String): String {
        var clean = serverUrl.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "http://$clean"
        }

        try {
            val uri = java.net.URI(clean)
            val scheme = uri.scheme ?: "http"
            val host = uri.host ?: ""
            val port = uri.port
            val portStr = if (port != -1 && port != 80 && port != 443) ":$port" else ""
            if (host.isNotBlank()) {
                return "$scheme://$host$portStr"
            }
        } catch (_: Exception) {}

        return clean.removeSuffix("/player_api.php").removeSuffix("/")
    }

    override suspend fun authenticate(
        serverUrl: String,
        username: String,
        password: String
    ): Result<AuthResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = normalizePlayerApiUrl(serverUrl)
            var response = try {
                service.authenticate(endpoint, username, password)
            } catch (e: Exception) {
                if (endpoint.startsWith("https://")) {
                    val httpEndpoint = endpoint.replace("https://", "http://")
                    service.authenticate(httpEndpoint, username, password)
                } else throw e
            }

            if (!response.isSuccessful && endpoint.startsWith("https://")) {
                val httpEndpoint = endpoint.replace("https://", "http://")
                try {
                    val httpResp = service.authenticate(httpEndpoint, username, password)
                    if (httpResp.isSuccessful) {
                        response = httpResp
                    }
                } catch (_: Exception) {}
            }

            if (!response.isSuccessful) {
                val code = response.code()
                val msg = if (code == 502) {
                    "Servidor IPTV inaccesible (HTTP 502 Bad Gateway). Compruebe la URL y puerto."
                } else {
                    "Fallo de conexión con el servidor (HTTP $code)"
                }
                throw Exception(msg)
            }

            val body = response.body() ?: throw Exception("Fallo de conexión con el servidor")

            val userInfoDto = body.userInfo
            val serverInfoDto = body.serverInfo

            if (userInfoDto != null && userInfoDto.auth == 0) {
                throw Exception("Usuario o contraseña incorrectos")
            }

            val userInfo = XtreamUserInfo(
                username = userInfoDto?.username?.ifBlank { username } ?: username,
                password = userInfoDto?.password?.ifBlank { password } ?: password,
                message = userInfoDto?.message,
                auth = userInfoDto?.auth ?: 1,
                status = userInfoDto?.status ?: "Active",
                expDate = userInfoDto?.expDate,
                isTrial = userInfoDto?.isTrial,
                activeCons = userInfoDto?.activeCons ?: "0",
                createdAt = userInfoDto?.createdAt,
                maxConnections = userInfoDto?.maxConnections ?: "1",
                allowedOutputFormats = userInfoDto?.allowedOutputFormats ?: listOf("m3u8", "ts")
            )

            val serverInfo = ServerInfo(
                url = serverInfoDto?.url ?: formatBaseServerUrl(serverUrl),
                port = serverInfoDto?.port,
                httpsPort = serverInfoDto?.httpsPort,
                serverProtocol = serverInfoDto?.serverProtocol,
                rtmpPort = serverInfoDto?.rtmpPort,
                timezone = serverInfoDto?.timezone,
                now = serverInfoDto?.timestamp
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
            val response = service.getSeriesInfo(endpoint, username, password, seriesId = seriesId)
            val body = response.body() ?: return@runCatching emptyList()
            val baseUrl = formatBaseServerUrl(serverUrl)
            val resultEpisodes = mutableListOf<Episode>()

            body.episodes?.forEach { (_, epList) ->
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

                        resultEpisodes.add(
                            Episode(
                                id = idStr,
                                title = dto.title ?: "Episode $epNum",
                                episodeNum = epNum,
                                seasonNum = sNum,
                                streamUrl = streamUrl,
                                containerExtension = ext,
                                duration = dto.duration,
                                overview = dto.overview
                            )
                        )
                    }
                }
            }
            resultEpisodes
        }
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
