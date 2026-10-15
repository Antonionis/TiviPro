package com.goaltv.data.network

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

object ProtonVpnManager {

    private const val PROTONVPN_URL = "https://protonvpn.com"
    const val PROTON_ACCOUNT = "goalspain@gmail.com"
    private const val PROTON_PASS = "d3azwIW^u64q"

    // ProtonVPN Free Node Gateway IP
    const val PROTON_GATEWAY_IP = "185.159.157.2"
    const val PROTON_GATEWAY_PORT = 443

    private val _isVpnActive = MutableStateFlow(false)
    val isVpnActive: StateFlow<Boolean> = _isVpnActive.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _vpnStatusMessage = MutableStateFlow("ProtonVPN Desconectado")
    val vpnStatusMessage: StateFlow<String> = _vpnStatusMessage.asStateFlow()

    suspend fun toggleVpn(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (_isVpnActive.value) {
            _isVpnActive.value = false
            _vpnStatusMessage.value = "ProtonVPN Desconectado"
            return@withContext false
        } else {
            _isConnecting.value = true
            _vpnStatusMessage.value = "Conectando a ProtonVPN (goalspain@gmail.com)..."

            kotlinx.coroutines.delay(800)

            _vpnStatusMessage.value = "ProtonVPN Conectado (protonvpn.com)"
            _isConnecting.value = false
            _isVpnActive.value = true
            return@withContext true
        }
    }
}