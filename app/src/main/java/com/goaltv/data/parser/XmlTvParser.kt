package com.goaltv.data.parser

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.xml.parsers.DocumentBuilderFactory

data class XmlTvChannel(
    val id: String,
    val displayNames: List<String> = emptyList(),
    val iconUrl: String? = null
)

data class XmlTvProgram(
    val channelId: String,
    val title: String,
    val description: String? = null,
    val category: String? = null,
    val startTimeSeconds: Long,
    val stopTimeSeconds: Long,
    val iconUrl: String? = null
)

data class XmlTvParseResult(
    val channels: Map<String, XmlTvChannel> = emptyMap(),
    val programs: List<XmlTvProgram> = emptyList()
)

class XmlTvParser {

    fun parse(
        inputStream: InputStream,
        minTimeSeconds: Long = 0L,
        maxTimeSeconds: Long = Long.MAX_VALUE,
        maxChannels: Int = Int.MAX_VALUE
    ): XmlTvParseResult {
        return try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(inputStream, "UTF-8")
            parseWithPullParser(parser, minTimeSeconds, maxTimeSeconds, maxChannels)
        } catch (_: Throwable) {
            // Fallback for JVM unit test environments where org.xmlpull is not mocked
            try {
                parseWithDom(inputStream, minTimeSeconds, maxTimeSeconds, maxChannels)
            } catch (e: Exception) {
                e.printStackTrace()
                XmlTvParseResult()
            }
        }
    }

    private fun parseWithPullParser(
        parser: XmlPullParser,
        minTimeSeconds: Long,
        maxTimeSeconds: Long,
        maxChannels: Int
    ): XmlTvParseResult {
        val channels = mutableMapOf<String, XmlTvChannel>()
        val programs = mutableListOf<XmlTvProgram>()

        var eventType = parser.eventType
        var currentTag = ""

        var channelId = ""
        val displayNames = mutableListOf<String>()
        var channelIcon: String? = null

        var progChannel = ""
        var progStart = 0L
        var progStop = 0L
        var progTitle = ""
        var progDesc: String? = null
        var progCategory: String? = null
        var progIcon: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    when (currentTag) {
                        "channel" -> {
                            channelId = parser.getAttributeValue(null, "id") ?: ""
                            displayNames.clear()
                            channelIcon = null
                        }
                        "programme" -> {
                            progChannel = parser.getAttributeValue(null, "channel") ?: ""
                            val startStr = parser.getAttributeValue(null, "start")
                            val stopStr = parser.getAttributeValue(null, "stop")
                            progStart = parseXmlTvDate(startStr)
                            progStop = parseXmlTvDate(stopStr)
                            progTitle = ""
                            progDesc = null
                            progCategory = null
                            progIcon = null
                        }
                        "icon" -> {
                            val src = parser.getAttributeValue(null, "src")
                            if (progChannel.isNotEmpty()) {
                                progIcon = src
                            } else if (channelId.isNotEmpty()) {
                                channelIcon = src
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim()
                    if (!text.isNullOrEmpty()) {
                        when (currentTag) {
                            "display-name" -> if (channelId.isNotEmpty()) displayNames.add(text)
                            "title" -> if (progChannel.isNotEmpty() && progTitle.isEmpty()) progTitle = text
                            "desc" -> if (progChannel.isNotEmpty()) progDesc = text
                            "category" -> if (progChannel.isNotEmpty()) {
                                progCategory = if (progCategory == null) text else "$progCategory / $text"
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val tag = parser.name
                    when (tag) {
                        "channel" -> {
                            if (channelId.isNotEmpty() && (channels.size < maxChannels || channels.containsKey(channelId))) {
                                channels[channelId] = XmlTvChannel(
                                    id = channelId,
                                    displayNames = displayNames.toList(),
                                    iconUrl = channelIcon
                                )
                            }
                            channelId = ""
                        }
                        "programme" -> {
                            if (progChannel.isNotEmpty() && progTitle.isNotEmpty()) {
                                val isWithinWindow = (progStop == 0L || progStop >= minTimeSeconds) &&
                                        (progStart == 0L || progStart <= maxTimeSeconds)
                                if (isWithinWindow) {
                                    programs.add(
                                        XmlTvProgram(
                                            channelId = progChannel,
                                            title = progTitle,
                                            description = progDesc,
                                            category = progCategory,
                                            startTimeSeconds = progStart,
                                            stopTimeSeconds = progStop,
                                            iconUrl = progIcon
                                        )
                                    )
                                }
                            }
                            progChannel = ""
                        }
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        return XmlTvParseResult(channels = channels, programs = programs)
    }

    private fun parseWithDom(
        inputStream: InputStream,
        minTimeSeconds: Long,
        maxTimeSeconds: Long,
        maxChannels: Int
    ): XmlTvParseResult {
        val channels = mutableMapOf<String, XmlTvChannel>()
        val programs = mutableListOf<XmlTvProgram>()

        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(inputStream)
        doc.documentElement.normalize()

        val channelNodes = doc.getElementsByTagName("channel")
        for (i in 0 until channelNodes.length) {
            if (channels.size >= maxChannels) break
            val node = channelNodes.item(i)
            if (node is org.w3c.dom.Element) {
                val id = node.getAttribute("id")
                if (id.isNotBlank()) {
                    val displayNames = mutableListOf<String>()
                    var channelIcon: String? = null

                    val children = node.childNodes
                    for (j in 0 until children.length) {
                        val child = children.item(j)
                        if (child is org.w3c.dom.Element) {
                            when (child.tagName) {
                                "display-name" -> displayNames.add(child.textContent.trim())
                                "icon" -> channelIcon = child.getAttribute("src")
                            }
                        }
                    }
                    channels[id] = XmlTvChannel(id = id, displayNames = displayNames, iconUrl = channelIcon)
                }
            }
        }

        val progNodes = doc.getElementsByTagName("programme")
        for (i in 0 until progNodes.length) {
            val node = progNodes.item(i)
            if (node is org.w3c.dom.Element) {
                val channelId = node.getAttribute("channel")
                val startStr = node.getAttribute("start")
                val stopStr = node.getAttribute("stop")
                val start = parseXmlTvDate(startStr)
                val stop = parseXmlTvDate(stopStr)

                val isWithinWindow = (stop == 0L || stop >= minTimeSeconds) &&
                        (start == 0L || start <= maxTimeSeconds)
                if (!isWithinWindow) continue

                var title = ""
                var desc: String? = null
                var category: String? = null
                var icon: String? = null

                val children = node.childNodes
                for (j in 0 until children.length) {
                    val child = children.item(j)
                    if (child is org.w3c.dom.Element) {
                        when (child.tagName) {
                            "title" -> if (title.isEmpty()) title = child.textContent.trim()
                            "desc" -> desc = child.textContent.trim()
                            "category" -> {
                                val catText = child.textContent.trim()
                                category = if (category == null) catText else "$category / $catText"
                            }
                            "icon" -> icon = child.getAttribute("src")
                        }
                    }
                }

                if (channelId.isNotBlank() && title.isNotBlank()) {
                    programs.add(
                        XmlTvProgram(
                            channelId = channelId,
                            title = title,
                            description = desc,
                            category = category,
                            startTimeSeconds = start,
                            stopTimeSeconds = stop,
                            iconUrl = icon
                        )
                    )
                }
            }
        }

        return XmlTvParseResult(channels = channels, programs = programs)
    }

    fun parseXmlTvDate(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return 0L
        return try {
            val cleanStr = dateStr.trim()
            val format = if (cleanStr.contains(" ")) {
                SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)
            } else if (cleanStr.length >= 14) {
                SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("Europe/Madrid")
                }
            } else {
                return 0L
            }
            val date = format.parse(cleanStr)
            (date?.time ?: 0L) / 1000L
        } catch (_: Exception) {
            0L
        }
    }
}
