package com.tivipro.data.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Dns
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * Custom DNS provider that filters for IPv4 addresses to prevent IPv6 connection timeouts.
 */
object Ipv4OnlyDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val addresses = try {
            Dns.SYSTEM.lookup(hostname)
        } catch (e: Exception) {
            emptyList()
        }
        val ipv4Addresses = addresses.filterIsInstance<Inet4Address>()
        return if (ipv4Addresses.isNotEmpty()) {
            ipv4Addresses
        } else {
            addresses
        }
    }
}

/**
 * Centralized network module providing standard OkHttpClient instances bypassing IPv6.
 */
object NetworkModule {

    private val _isDohEnabled = MutableStateFlow(false)
    val isDohEnabled: StateFlow<Boolean> = _isDohEnabled.asStateFlow()

    fun setDohEnabled(enabled: Boolean) {
        _isDohEnabled.value = enabled
    }

    fun getOkHttpClient(dohEnabled: Boolean = false): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .dns(Ipv4OnlyDns)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "IPTVSmartersPro")
                    .header("Accept", "*/*")
                    .header("Connection", "keep-alive")
                    .build()
                chain.proceed(request)
            }

        return builder.build()
    }

    fun getPlayerOkHttpClient(dohEnabled: Boolean = false): OkHttpClient {
        return getOkHttpClient(false)
    }

    fun createXtreamCodesService(dohEnabled: Boolean = false): XtreamCodesService {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val okHttpClient = getOkHttpClient(false)

        val retrofit = Retrofit.Builder()
            .baseUrl("http://localhost/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(XtreamCodesService::class.java)
    }
}
