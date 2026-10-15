package com.goaltv.data.parser

import com.goaltv.model.Category
import com.goaltv.model.CategoryType
import com.goaltv.model.LiveChannel
import com.goaltv.model.Series
import com.goaltv.model.VodMovie
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID

data class M3UParseResult(
    val liveChannels: List<LiveChannel>,
    val vodMovies: List<VodMovie>,
    val seriesList: List<Series>,
    val categories: List<Category>
)

object M3UParser {

    fun parse(content: String): M3UParseResult {
        return parseReader(BufferedReader(content.reader()))
    }

    fun parseInputStream(inputStream: InputStream): M3UParseResult {
        return parseReader(BufferedReader(InputStreamReader(inputStream)))
    }

    private fun parseReader(reader: BufferedReader): M3UParseResult {
        val liveChannels = mutableListOf<LiveChannel>()
        val vodMovies = mutableListOf<VodMovie>()
        val seriesList = mutableListOf<Series>()
        val categoriesMap = mutableMapOf<String, Category>()

        var currentTvgId: String? = null
        var currentTvgName: String? = null
        var currentLogo: String? = null
        var currentGroup = "General"
        var currentTitle = ""
        var inItem = false

        reader.useLines { lines ->
            lines.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty()) return@forEach

                if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                    inItem = true
                    currentTvgId = extractAttribute(trimmed, "tvg-id")
                    currentTvgName = extractAttribute(trimmed, "tvg-name")
                    currentLogo = extractAttribute(trimmed, "tvg-logo")
                    currentGroup = extractAttribute(trimmed, "group-title")
                        ?: extractAttribute(trimmed, "tvg-group")
                        ?: "General"

                    val commaIndex = trimmed.lastIndexOf(',')
                    currentTitle = if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                        trimmed.substring(commaIndex + 1).trim()
                    } else {
                        currentTvgName ?: "Channel ${liveChannels.size + vodMovies.size + 1}"
                    }
                } else if (trimmed.startsWith("#EXTGRP:", ignoreCase = true)) {
                    val groupVal = trimmed.substringAfter("#EXTGRP:").trim()
                    if (groupVal.isNotEmpty()) {
                        currentGroup = groupVal
                    }
                } else if (!trimmed.startsWith("#")) {
                    if (inItem && (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("rtmp://"))) {
                        val streamUrl = trimmed
                        val displayName = currentTitle.ifBlank { currentTvgName ?: "Stream" }
                        val categoryId = slugify(currentGroup)

                        val itemType = determineItemType(currentGroup, streamUrl)
                        val catType = when (itemType) {
                            ItemType.LIVE -> CategoryType.LIVE
                            ItemType.VOD -> CategoryType.VOD
                            ItemType.SERIES -> CategoryType.SERIES
                        }

                        categoriesMap.putIfAbsent(
                            categoryId,
                            Category(id = categoryId, name = currentGroup, type = catType)
                        )

                        val itemId = currentTvgId?.ifBlank { null }
                            ?: "${categoryId}_${displayName.hashCode()}_${UUID.randomUUID().toString().take(6)}"

                        when (itemType) {
                            ItemType.LIVE -> {
                                liveChannels.add(
                                    LiveChannel(
                                        id = itemId,
                                        name = displayName,
                                        iconUrl = currentLogo,
                                        streamUrl = streamUrl,
                                        categoryId = categoryId,
                                        epgId = currentTvgId,
                                        streamType = if (streamUrl.contains(".m3u8")) "m3u8" else "live"
                                    )
                                )
                            }
                            ItemType.VOD -> {
                                val ext = getFileExtension(streamUrl) ?: "mp4"
                                vodMovies.add(
                                    VodMovie(
                                        id = itemId,
                                        name = displayName,
                                        posterUrl = currentLogo,
                                        streamUrl = streamUrl,
                                        categoryId = categoryId,
                                        containerExtension = ext,
                                        description = "M3U VOD: $displayName"
                                    )
                                )
                            }
                            ItemType.SERIES -> {
                                seriesList.add(
                                    Series(
                                        id = itemId,
                                        name = displayName,
                                        posterUrl = currentLogo,
                                        categoryId = categoryId,
                                        overview = "M3U Series: $displayName"
                                    )
                                )
                            }
                        }

                        // Reset item state
                        inItem = false
                        currentTvgId = null
                        currentTvgName = null
                        currentLogo = null
                        currentGroup = "General"
                        currentTitle = ""
                    }
                }
            }
        }

        return M3UParseResult(
            liveChannels = liveChannels,
            vodMovies = vodMovies,
            seriesList = seriesList,
            categories = categoriesMap.values.toList()
        )
    }

    private fun extractAttribute(line: String, attributeName: String): String? {
        val pattern = """$attributeName="([^"]*)"""".toRegex(RegexOption.IGNORE_CASE)
        val match = pattern.find(line)
        if (match != null && match.groupValues.size > 1) {
            val value = match.groupValues[1].trim()
            if (value.isNotEmpty()) return value
        }
        val singleQuotePattern = """$attributeName='([^']*)'""".toRegex(RegexOption.IGNORE_CASE)
        val singleMatch = singleQuotePattern.find(line)
        if (singleMatch != null && singleMatch.groupValues.size > 1) {
            val value = singleMatch.groupValues[1].trim()
            if (value.isNotEmpty()) return value
        }
        return null
    }

    private enum class ItemType { LIVE, VOD, SERIES }

    private fun determineItemType(groupName: String, url: String): ItemType {
        val groupLower = groupName.lowercase()
        val urlLower = url.lowercase()

        if (groupLower.contains("series") || groupLower.contains("tv show") || groupLower.contains("season") || groupLower.contains("episodes")) {
            return ItemType.SERIES
        }
        if (groupLower.contains("vod") || groupLower.contains("movie") || groupLower.contains("cinema") || groupLower.contains("films") || groupLower.contains("pelicula")) {
            return ItemType.VOD
        }

        if (urlLower.contains(".mp4") || urlLower.contains(".mkv") || urlLower.contains(".avi") || urlLower.contains(".mov")) {
            return ItemType.VOD
        }

        return ItemType.LIVE
    }

    private fun getFileExtension(url: String): String? {
        val cleanUrl = url.substringBefore('?').substringBefore('#')
        val lastDot = cleanUrl.lastIndexOf('.')
        if (lastDot != -1 && lastDot < cleanUrl.length - 1) {
            val ext = cleanUrl.substring(lastDot + 1).lowercase()
            if (ext.length in 2..5) return ext
        }
        return null
    }

    private fun slugify(text: String): String {
        return text.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifEmpty { "general" }
    }
}
