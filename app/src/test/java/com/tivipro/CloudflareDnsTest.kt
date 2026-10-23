package com.tivipro

import com.tivipro.data.network.CloudflareDns
import com.tivipro.data.network.NetworkModule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudflareDnsTest {

    @Test
    fun testCloudflareDnsCreation() {
        val dns = CloudflareDns.create()
        assertNotNull(dns)
    }

    @Test
    fun testNetworkModuleOkHttpClientWithDoh() {
        val clientWithDoh = NetworkModule.getOkHttpClient(dohEnabled = true)
        assertNotNull(clientWithDoh)

        val clientWithoutDoh = NetworkModule.getOkHttpClient(dohEnabled = false)
        assertNotNull(clientWithoutDoh)
    }

    @Test
    fun testNetworkModuleDohToggle() {
        NetworkModule.setDohEnabled(true)
        assertTrue(NetworkModule.isDohEnabled.value)

        NetworkModule.setDohEnabled(false)
        assertFalse(NetworkModule.isDohEnabled.value)

        // Restore default enabled state
        NetworkModule.setDohEnabled(true)
    }
}
