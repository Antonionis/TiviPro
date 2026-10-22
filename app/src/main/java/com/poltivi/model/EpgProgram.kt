package com.poltivi.model

data class EpgProgram(
    val id: String,
    val channelId: String,
    val title: String,
    val description: String? = null,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val isLive: Boolean = false,
    val catchupUrl: String? = null
) {
    val isCurrentlyLive: Boolean
        get() {
            if (isLive) return true
            val now = System.currentTimeMillis() / 1000
            return now in startTimestamp..endTimestamp
        }

    val isPastProgram: Boolean
        get() {
            val now = System.currentTimeMillis() / 1000
            return endTimestamp < now
        }

    val progressPercent: Float
        get() {
            val now = System.currentTimeMillis() / 1000
            if (now <= startTimestamp) return 0f
            if (now >= endTimestamp) return 1f
            val total = endTimestamp - startTimestamp
            if (total <= 0) return 0f
            val res = (now - startTimestamp).toFloat() / total.toFloat()
            if (res.isNaN() || res.isInfinite()) return 0f
            return res.coerceIn(0f, 1f)
        }
}
