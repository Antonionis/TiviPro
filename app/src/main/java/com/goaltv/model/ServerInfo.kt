package com.goaltv.model

data class ServerInfo(
    val url: String? = null,
    val port: String? = null,
    val httpsPort: String? = null,
    val serverProtocol: String? = "http",
    val rtmpPort: String? = null,
    val timezone: String? = null,
    val now: Long? = null
) {
    fun buildBaseUrl(): String {
        if (!url.isNullOrBlank()) {
            val protocol = serverProtocol ?: if (httpsPort != null && httpsPort != "0") "https" else "http"
            val portString = when {
                port != null && port.isNotBlank() && port != "80" && port != "443" -> ":$port"
                else -> ""
            }
            var cleanUrl = url
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                cleanUrl = "$protocol://$cleanUrl"
            }
            if (!cleanUrl.contains(":") || cleanUrl.substringAfter("://").contains(":").not()) {
                cleanUrl += portString
            }
            return cleanUrl.removeSuffix("/")
        }
        return ""
    }
}
