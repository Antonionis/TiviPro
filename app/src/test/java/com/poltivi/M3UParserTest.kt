package com.poltivi

import com.poltivi.data.parser.M3UParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M3UParserTest {

    @Test
    fun testParseM3uPlaylist() {
        val m3uContent = """
            #EXTM3U
            #EXTINF:-1 tvg-id="espn.us" tvg-name="ESPN HD" tvg-logo="http://example.com/logo.png" group-title="Sports", ESPN HD 1080p
            http://example.com/stream/espn.m3u8
            #EXTINF:-1 tvg-id="movie123" tvg-name="Big Buck Bunny" tvg-logo="http://example.com/bbb.jpg" group-title="Movies", Big Buck Bunny
            http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4
            #EXTINF:-1 tvg-id="ser456" tvg-name="Nature Earth S01E01" tvg-logo="http://example.com/nature.jpg" group-title="Series", Nature Earth Episode 1
            http://example.com/series/nature_s1e1.mp4
        """.trimIndent()

        val result = M3UParser.parse(m3uContent)

        assertNotNull(result)
        assertEquals(1, result.liveChannels.size)
        assertEquals(1, result.vodMovies.size)
        assertEquals(1, result.seriesList.size)

        val channel = result.liveChannels.first()
        assertEquals("ESPN HD 1080p", channel.name)
        assertEquals("http://example.com/logo.png", channel.iconUrl)
        assertEquals("espn.us", channel.epgId)
        assertEquals("http://example.com/stream/espn.m3u8", channel.streamUrl)

        val movie = result.vodMovies.first()
        assertEquals("Big Buck Bunny", movie.name)
        assertEquals("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", movie.streamUrl)

        val series = result.seriesList.first()
        assertEquals("Nature Earth Episode 1", series.name)

        assertTrue(result.categories.isNotEmpty())
    }
}
