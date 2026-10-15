package com.goaltv.data.network

import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * Provides a Cloudflare 1.1.1.1 DNS-over-HTTPS (DoH) Dns instance
 * to prevent ISP domain blocking on IPTV streams and APIs.
 * Catches any lookup exceptions or null/empty results and falls back instantly to Dns.SYSTEM
 * so DNS resolution never blocks stream playback.
 */
object CloudflareDns {

    fun create(bootstrapClient: OkHttpClient = createDefaultBootstrapClient()): Dns {
        return try {
            val dohUrl = "https://1.1.1.1/dns-query".toHttpUrl()
            val bootstrapAddresses = listOf(
                InetAddress.getByAddress(byteArrayOf(1, 1, 1, 1)),
                InetAddress.getByAddress(byteArrayOf(1, 0, 0, 1))
            )

            val doh = DnsOverHttps.Builder()
                .client(bootstrapClient)
                .url(dohUrl)
                .bootstrapDnsHosts(bootstrapAddresses)
                .build()

            object : Dns {
                override fun lookup(hostname: String): List<InetAddress> {
                    return try {
                        val result = doh.lookup(hostname)
                        if (result.isEmpty()) {
                            Dns.SYSTEM.lookup(hostname)
                        } else {
                            result
                        }
                    } catch (_: Exception) {
                        Dns.SYSTEM.lookup(hostname)
                    }
                }
            }
        } catch (_: Exception) {
            Dns.SYSTEM
        }
    }

    private fun createDefaultBootstrapClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
}
