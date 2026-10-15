package com.poltivi

import com.poltivi.data.parser.XmlTvParser
import com.poltivi.data.repository.SportsRepositoryImpl
import com.poltivi.model.LiveChannel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class XmlTvParserTest {

    @Test
    fun testParseXmlTvFeed() {
        val xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv generator-info-name="EPG_dobleM">
              <channel id="La1.es">
                <display-name>La 1 HD</display-name>
                <display-name>La 1</display-name>
                <icon src="http://example.com/la1.png"/>
              </channel>
              <channel id="DAZN1.es">
                <display-name>DAZN 1 HD</display-name>
                <display-name>DAZN 1</display-name>
                <icon src="http://example.com/dazn1.png"/>
              </channel>

              <programme start="20250520210000 +0200" stop="20250520230000 +0200" channel="DAZN1.es">
                <title lang="es">Real Madrid vs Barcelona - LaLiga EA Sports</title>
                <desc lang="es">El Clásico de LaLiga EA Sports en directo desde el Santiago Bernabéu.</desc>
                <category lang="es">Deportes</category>
                <category lang="es">Fútbol</category>
              </programme>
              <programme start="20250520180000 +0200" stop="20250520200000 +0200" channel="La1.es">
                <title lang="es">Telediario 2</title>
                <desc lang="es">Noticias del día.</desc>
                <category lang="es">Informativos</category>
              </programme>
            </tv>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(xmlContent.toByteArray(Charsets.UTF_8))
        val parser = XmlTvParser()
        val result = parser.parse(inputStream)

        assertNotNull(result)
        assertEquals(2, result.channels.size)
        assertTrue(result.channels.containsKey("La1.es"))
        assertTrue(result.channels.containsKey("DAZN1.es"))
        assertEquals("La 1 HD", result.channels["La1.es"]?.displayNames?.first())

        assertEquals(2, result.programs.size)
        val sportsProg = result.programs.first { it.channelId == "DAZN1.es" }
        assertEquals("Real Madrid vs Barcelona - LaLiga EA Sports", sportsProg.title)
        assertTrue(sportsProg.category?.contains("Fútbol") == true)
        assertTrue(sportsProg.startTimeSeconds > 0L)
        assertTrue(sportsProg.stopTimeSeconds > sportsProg.startTimeSeconds)
    }

    @Test
    fun testParseXmlTvDate() {
        val parser = XmlTvParser()
        val timestamp = parser.parseXmlTvDate("20250520210000 +0200")
        assertTrue("Parsed timestamp should be greater than 0", timestamp > 0L)
    }

    @Test
    fun testSportsRepositoryFallbackAndMatching() = runBlocking {
        val sportsRepository = SportsRepositoryImpl()
        val liveChannels = listOf(
            LiveChannel(
                id = "ch_dazn1",
                name = "DAZN 1 HD",
                iconUrl = "http://example.com/logo.png",
                streamUrl = "http://example.com/dazn1.m3u8",
                categoryId = "cat_sports",
                epgId = "DAZN1.es"
            )
        )

        val events = sportsRepository.getSportsEvents(liveChannels)
        assertNotNull(events)
        assertTrue("Sports events list should not be empty", events.isNotEmpty())
    }
}
