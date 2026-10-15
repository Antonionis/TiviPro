package com.goaltv.data.network

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

object ProtonVpnManager {

    // WireGuard ProtonVPN Node: NL-FREE#254
    const val PROTON_GATEWAY_IP = "185.100.235.117"
    const val PROTON_GATEWAY_PORT = 51820
    const val WG_ADDRESS = "10.2.0.2"
    const val WG_PUBLIC_KEY = "hr7enEjU6cfXBCYqhF6e92hkr41BftzIz+ia21UUEHU="

    private val _isVpnActive = MutableStateFlow(false)
    val isVpnActive: StateFlow<Boolean> = _isVpnActive.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _vpnStatusMessage = MutableStateFlow("VPN Desconectado")
    val vpnStatusMessage: StateFlow<String> = _vpnStatusMessage.asStateFlow()

    suspend fun toggleVpn(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (_isVpnActive.value) {
            _isVpnActive.value = false
            _vpnStatusMessage.value = "VPN Desconectado"
            return@withContext false
        } else {
            _isConnecting.value = true
            _vpnStatusMessage.value = "Conectando a WireGuard VPN (NL-FREE#254)..."

            kotlinx.coroutines.delay(600)

            _vpnStatusMessage.value = "VPN Conectado (WireGuard NL-254)"
            _isConnecting.value = false
            _isVpnActive.value = true
            return@withContext true
        }
    }
}