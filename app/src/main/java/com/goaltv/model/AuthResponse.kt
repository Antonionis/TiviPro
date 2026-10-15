package com.goaltv.model

data class AuthResponse(
    val userInfo: XtreamUserInfo? = null,
    val serverInfo: ServerInfo? = null
) {
    val isAuthenticated: Boolean
        get() = userInfo?.auth == 1 || userInfo?.status?.equals("Active", ignoreCase = true) == true
}
