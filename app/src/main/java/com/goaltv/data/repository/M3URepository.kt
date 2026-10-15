package com.goaltv.data.repository

import com.goaltv.data.parser.M3UParseResult
import com.goaltv.data.parser.M3UParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

interface M3URepository {
    suspend fun fetchAndParseFromUrl(url: String): Result<M3UParseResult>
    suspend fun parseFromText(content: String): Result<M3UParseResult>
    suspend fun parseFromFile(file: File): Result<M3UParseResult>
}

class M3URepositoryImpl(
    private val okHttpClientProvider: (Boolean) -> OkHttpClient
) : M3URepository {

    constructor(okHttpClient: OkHttpClient) : this({ okHttpClient })
    constructor() : this({ doh -> com.goaltv.data.network.NetworkModule.getOkHttpClient(doh) })

    private val okHttpClient: OkHttpClient
        get() = okHttpClientProvider(com.goaltv.data.network.NetworkModule.isDohEnabled.value)

    companion object {
        fun createDefaultOkHttpClient(): OkHttpClient {
            return com.goaltv.data.network.NetworkModule.getOkHttpClient()
        }
    }

    override suspend fun fetchAndParseFromUrl(url: String): Result<M3UParseResult> = withContext(Dispatchers.IO) {
        runCatching {
            var formattedUrl = url.trim()
            if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                formattedUrl = "http://$formattedUrl"
            }

            val request = Request.Builder()
                .url(formattedUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) GoalTV/1.0")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to fetch M3U playlist. HTTP Code: ${response.code}")
            }

            val inputStream = response.body.byteStream()

            M3UParser.parseInputStream(inputStream)
        }
    }

    override suspend fun parseFromText(content: String): Result<M3UParseResult> = withContext(Dispatchers.Default) {
        runCatching {
            M3UParser.parse(content)
        }
    }

    override suspend fun parseFromFile(file: File): Result<M3UParseResult> = withContext(Dispatchers.IO) {
        runCatching {
            file.inputStream().use { inputStream ->
                M3UParser.parseInputStream(inputStream)
            }
        }
    }
}
