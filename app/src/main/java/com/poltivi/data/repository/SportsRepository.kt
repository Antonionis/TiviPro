package com.poltivi.data.repository

import com.poltivi.data.network.NetworkModule
import com.poltivi.data.parser.XmlTvParseResult
import com.poltivi.data.parser.XmlTvParser
import com.poltivi.model.LiveChannel
import com.poltivi.model.SportsEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.util.Locale
import java.util.TimeZone

interface SportsRepository {
    suspend fun getSportsEvents(liveChannels: List<LiveChannel> = emptyList()): List<SportsEvent>
    suspend fun getXmlTvData(): XmlTvParseResult
}

class SportsRepositoryImpl : SportsRepository {

    private val xmlTvParser = XmlTvParser()
    private var cachedXmlTvResult: XmlTvParseResult? = null
    private var lastFetchTimeMs: Long = 0L

    override suspend fun getXmlTvData(): XmlTvParseResult {
        val now = System.currentTimeMillis()
        val cached = cachedXmlTvResult
        // Cache EPG XML for 4 hours (14_400_000 ms) or session
        if (cached != null && (now - lastFetchTimeMs < 14_400_000L) && cached.programs.isNotEmpty()) {
            return cached
        }

        return withContext(Dispatchers.IO) {
            try {
                val client = NetworkModule.getOkHttpClient()
                val request = Request.Builder()
                    .url("https://raw.githubusercontent.com/davidmuma/EPG_dobleM/master/guiaiptv.xml")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStream = response.body.byteStream()
                        val nowSeconds = System.currentTimeMillis() / 1000L
                        val minTime = nowSeconds - 86400L  // -1 day
                        val maxTime = nowSeconds + 172800L // +2 days

                        val parsed = xmlTvParser.parse(
                            inputStream = bodyStream,
                            minTimeSeconds = minTime,
                            maxTimeSeconds = maxTime,
                            maxChannels = 200
                        )
                        if (parsed.programs.isNotEmpty()) {
                            cachedXmlTvResult = parsed
                            lastFetchTimeMs = now
                            return@withContext parsed
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            cachedXmlTvResult ?: XmlTvParseResult()
        }
    }

    private fun normalizeDateString(rawDate: String, currentYear: Int): String? {
        val clean = rawDate.replace("*", "").trim()
        if (clean.isBlank()) return null
        val ymd = Regex("""^(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})$""").find(clean)
        if (ymd != null) {
            val (y, m, d) = ymd.destructured
            return String.format(Locale.US, "%04d-%02d-%02d", y.toInt(), m.toInt(), d.toInt())
        }
        val dmy = Regex("""^(\d{1,2})[-/.](\d{1,2})[-/.](\d{4})$""").find(clean)
        if (dmy != null) {
            val (d, m, y) = dmy.destructured
            return String.format(Locale.US, "%04d-%02d-%02d", y.toInt(), m.toInt(), d.toInt())
        }
        val dm = Regex("""^(\d{1,2})[-/.](\d{1,2})$""").find(clean)
        if (dm != null) {
            val (d, m) = dm.destructured
            return String.format(Locale.US, "%04d-%02d-%02d", currentYear, m.toInt(), d.toInt())
        }
        return null
    }

    private fun normalizeHourString(rawHour: String): String {
        val clean = rawHour.replace("*", "").trim().replace(".", ":")
        val match = Regex("""^(\d{1,2}):(\d{2})""").find(clean)
        return if (match != null) {
            val (h, m) = match.destructured
            String.format(Locale.US, "%02d:%02d", h.toInt(), m.toInt())
        } else {
            clean
        }
    }

    private suspend fun fetchSportsProgTxt(liveChannels: List<LiveChannel>): List<SportsEvent> = withContext(Dispatchers.IO) {
        val events = mutableListOf<SportsEvent>()
        try {
            val client = NetworkModule.getOkHttpClient()
            val request = Request.Builder()
                .url("http://84.235.239.152/prog/prog.txt")
                .header("User-Agent", "PolTivi-App")
                .header("Cache-Control", "no-cache, no-store")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val content = response.body.string()
            val lines = content.lines()

            val tz = TimeZone.getTimeZone("Europe/Madrid")
            val cal = java.util.Calendar.getInstance(tz)
            val currentYear = cal.get(java.util.Calendar.YEAR)
            val yyyySdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = tz }
            val fullSdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { timeZone = tz }

            val fileDates = mutableListOf<String>()
            for (line in lines) {
                val dateMatch = Regex("""(?i)FECHA:\s*([0-9\-/.]+)""").find(line)
                if (dateMatch != null) {
                    val raw = dateMatch.groupValues[1]
                    val norm = normalizeDateString(raw, currentYear)
                    if (norm != null && !fileDates.contains(norm)) {
                        fileDates.add(norm)
                    }
                }
            }

            val todayFormatted = yyyySdf.format(cal.time)
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            val tomorrowFormatted = yyyySdf.format(cal.time)
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            val pasadoFormatted = yyyySdf.format(cal.time)

            val todayDateStr = if (fileDates.contains(todayFormatted)) todayFormatted else fileDates.getOrNull(0) ?: todayFormatted
            val tomorrowDateStr = if (fileDates.contains(tomorrowFormatted)) tomorrowFormatted else fileDates.getOrNull(1) ?: tomorrowFormatted
            val pasadoDateStr = if (fileDates.contains(pasadoFormatted)) pasadoFormatted else fileDates.getOrNull(2) ?: pasadoFormatted

            var currentDateStr: String? = null
            val nowMs = System.currentTimeMillis()
            var counter = 1

            for (rawLine in lines) {
                val cleanLine = rawLine.replace("*", "").trim()
                if (cleanLine.isBlank() || cleanLine.startsWith("=") || cleanLine.contains(":---") || cleanLine.contains("|---|")) {
                    continue
                }

                val dateMatch = Regex("""(?i)FECHA:\s*([0-9\-/.]+)""").find(cleanLine)
                if (dateMatch != null) {
                    val raw = dateMatch.groupValues[1]
                    val norm = normalizeDateString(raw, currentYear)
                    if (norm != null) {
                        currentDateStr = norm
                    }
                    continue
                }

                val effDateStr = currentDateStr ?: todayDateStr
                if (effDateStr.isBlank()) continue

                val bracketMatch = Regex("""^\[(\d{1,2}:\d{2})]\s*(?:\[([^\]]+)])?\s*(.*?)(?:\s*\|\s*(.*))?$""").find(cleanLine)

                var normHour = "00:00"
                var sportCategory = "Deportes"
                var fullTitle = ""
                var channel = "TV"

                if (bracketMatch != null) {
                    normHour = bracketMatch.groupValues[1]
                    val cat = bracketMatch.groupValues[2]
                    if (cat.isNotBlank()) sportCategory = cat
                    fullTitle = bracketMatch.groupValues[3].trim()
                    channel = bracketMatch.groupValues[4].trim().ifBlank { "TV" }
                } else {
                    val rawParts = cleanLine.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                    if (rawParts.size < 2) continue

                    normHour = rawParts.firstNotNullOfOrNull { part ->
                        if (part.matches(Regex("""^\d{1,2}[:.]\d{2}$"""))) normalizeHourString(part) else null
                    } ?: "00:00"

                    val remainingParts = rawParts.filter { part ->
                        normalizeDateString(part, currentYear) == null &&
                        !part.matches(Regex("""^\d{1,2}[:.]\d{2}$""")) &&
                        !part.uppercase(Locale.ROOT).startsWith("FECHA")
                    }

                    when {
                        remainingParts.size >= 3 -> {
                            sportCategory = remainingParts[0]
                            fullTitle = remainingParts[1]
                            channel = remainingParts[2]
                        }
                        remainingParts.size == 2 -> {
                            fullTitle = remainingParts[0]
                            channel = remainingParts[1]
                        }
                        remainingParts.size == 1 -> {
                            fullTitle = remainingParts[0]
                        }
                        else -> continue
                    }
                }

                if (fullTitle.isBlank() || fullTitle.uppercase(Locale.ROOT).contains("EVENTO") || fullTitle.uppercase(Locale.ROOT).contains("DEPORTE")) {
                    continue
                }

                val dateTab = when (effDateStr) {
                    todayDateStr -> "Hoy"
                    tomorrowDateStr -> "Mañana"
                    pasadoDateStr -> "Pasado Mañana"
                    else -> {
                        if (effDateStr < todayDateStr) "Hoy" else "Pasado Mañana"
                    }
                }

                var competition = "Evento Deportivo"
                var eventTitle = fullTitle
                if (fullTitle.contains(":")) {
                    val compParts = fullTitle.split(":", limit = 2)
                    competition = compParts[0].trim()
                    eventTitle = compParts[1].trim()
                }

                var startMs = 0L
                var isLive = false
                try {
                    val parsedDate = fullSdf.parse("$effDateStr $normHour")
                    if (parsedDate != null) {
                        startMs = parsedDate.time
                        if (nowMs >= startMs && nowMs < (startMs + 2 * 3600 * 1000L)) {
                            isLive = true
                        }
                    }
                } catch (_: Exception) {}

                val matchedChannel = liveChannels.firstOrNull { ch ->
                    val chName = ch.name.lowercase().replace(" ", "").replace("+", "").replace(".", "")
                    val targetName = channel.lowercase().replace(" ", "").replace("+", "").replace(".", "")
                    chName.contains(targetName) || targetName.contains(chName)
                }

                events.add(
                    SportsEvent(
                        id = "prog_${effDateStr}_$counter",
                        title = eventTitle,
                        competition = competition,
                        sportCategory = sportCategory,
                        dateTab = dateTab,
                        eventDateStr = effDateStr,
                        dateTimeDisplay = normHour,
                        channelName = channel,
                        channelId = matchedChannel?.id,
                        streamUrl = matchedChannel?.streamUrl ?: "",
                        iconUrl = matchedChannel?.iconUrl,
                        isLive = isLive,
                        startTimestamp = startMs
                    )
                )
                counter++
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext events
    }

    override suspend fun getSportsEvents(liveChannels: List<LiveChannel>): List<SportsEvent> = withContext(Dispatchers.IO) {
        fetchSportsProgTxt(liveChannels)
    }
}
