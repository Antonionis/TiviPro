package com.tivipro.model

data class XtreamUserInfo(
    val username: String = "",
    val password: String = "",
    val message: String? = null,
    val auth: Int? = null,
    val status: String? = null,
    val expDate: String? = null,
    val isTrial: String? = null,
    val activeCons: String? = null,
    val createdAt: String? = null,
    val maxConnections: String? = null,
    val allowedOutputFormats: List<String>? = emptyList()
)
