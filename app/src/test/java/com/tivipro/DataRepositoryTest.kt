package com.tivipro

import com.tivipro.data.repository.DataRepositoryImpl
import com.tivipro.data.repository.DataSourceMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DataRepositoryTest {

    private lateinit var repository: DataRepositoryImpl

    @Before
    fun setUp() {
        repository = DataRepositoryImpl()
    }

    @Test
    fun testInitialUnauthenticatedState() = runBlocking {
        assertFalse("Initial session should not be logged in", repository.isLoggedIn.value)
        assertNull("Active Xtream account should be null before login", repository.activeXtreamAccount.value)

        val liveCategories = repository.getLiveCategories()
        assertTrue("Live categories should be empty before login", liveCategories.isEmpty())

        val liveChannels = repository.getLiveChannels()
        assertTrue("Live channels should be empty before login", liveChannels.isEmpty())

        val vodMovies = repository.getVodMovies()
        assertTrue("VOD movies should be empty before login", vodMovies.isEmpty())

        val seriesList = repository.getSeriesList()
        assertTrue("Series list should be empty before login", seriesList.isEmpty())

        val epg = repository.getEpgForChannel("ch_1")
        assertTrue("EPG items should be empty before login", epg.isEmpty())
    }

    @Test
    fun testSessionSaveAndClear() = runBlocking {
        val serverUrl = "http://zeta.proxy-pass.xyz:80"
        val username = "testuser"
        val password = "testpassword"

        repository.saveSession(serverUrl, username, password)

        assertTrue("Session should be logged in after saving", repository.isLoggedIn.value)
        assertEquals(DataSourceMode.XTREAM, repository.activeMode.value)
        assertNotNull(repository.activeXtreamAccount.value)
        assertEquals(username, repository.activeXtreamAccount.value?.username)

        repository.clearSession()

        assertFalse("Session should not be logged in after clearing", repository.isLoggedIn.value)
        assertNull("Active account should be null after logout", repository.activeXtreamAccount.value)
    }

    @Test
    fun testToggleFavorites() = runBlocking {
        val channelId = "ch_123"
        val favoritesBefore = repository.getFavoriteChannels()
        assertFalse(favoritesBefore.any { it.id == channelId })

        repository.toggleFavoriteChannel(channelId)
        repository.toggleFavoriteChannel(channelId)
    }
}
