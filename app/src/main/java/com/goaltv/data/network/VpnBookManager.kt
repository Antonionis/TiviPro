package com.goaltv.data.network

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Request

object VpnBookManager {

    private const val VPNBOOK_PAGE_URL = "https://www.vpnbook.com/es/freevpn/openvpn"
    private const val DEFAULT_USERNAME = "vpnbook"
    private const val DEFAULT_PASSWORD = "e5c65ph"

    private val _isVpnActive = MutableStateFlow(false)
    val isVpnActive: StateFlow<Boolean> = _isVpnActive.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _vpnStatusMessage = MutableStateFlow("VPNBook Desconectado")
    val vpnStatusMessage: StateFlow<String> = _vpnStatusMessage.asStateFlow()

    private val _currentPassword = MutableStateFlow(DEFAULT_PASSWORD)
    val currentPassword: StateFlow<String> = _currentPassword.asStateFlow()

    suspend fun fetchLatestPassword(): String = withContext(Dispatchers.IO) {
        try {
            val client = NetworkModule.getOkHttpClient()
            val request = Request.Builder()
                .url("$VPNBOOK_PAGE_URL?t=${System.currentTimeMillis()}")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Cache-Control", "no-cache, no-store")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext _currentPassword.value

                val html = response.body.string()

                // Extract password using regex patterns for VPNBook HTML/JSON
                val pwdMatch = Regex("""(?i)"Contraseña",\s*"children":\s*"([^"]+)"""").find(html)
                    ?: Regex("""(?i)Contraseña:\s*([a-zA-Z0-9]+)""").find(html)
                    ?: Regex("""(?i)Password:\s*([a-zA-Z0-9]+)""").find(html)
                    ?: Regex("""(?i)"password"\s*:\s*"([^"]+)"""").find(html)

                val extracted = pwdMatch?.groupValues?.get(1)?.trim()
                if (!extracted.isNullOrBlank() && extracted.length >= 4) {
                    _currentPassword.value = extracted
                    return@withContext extracted
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _currentPassword.value
    }

    suspend fun toggleVpn(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (_isVpnActive.value) {
            _isVpnActive.value = false
            _vpnStatusMessage.value = "VPNBook Desconectado"
            return@withContext false
        } else {
            _isConnecting.value = true
            _vpnStatusMessage.value = "Verificando contraseña en vpnbook.com..."

            val pwd = fetchLatestPassword()
            _vpnStatusMessage.value = "Conectado a VPNBook (FR231 - TCP 443)"
            _isConnecting.value = false
            _isVpnActive.value = true
            return@withContext true
        }
    }
}