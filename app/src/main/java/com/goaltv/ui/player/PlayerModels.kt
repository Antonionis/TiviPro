package com.goaltv.ui.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout

@OptIn(UnstableApi::class)
enum class AspectRatioMode(val label: String, val resizeMode: Int) {
    FIT("Ajustar", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("Rellenar", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    ZOOM("Ampliar", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    SIXTEEN_NINE("16:9", AspectRatioFrameLayout.RESIZE_MODE_FILL)
}

data class TrackOption(
    val id: String,
    val name: String,
    val language: String? = null,
    val groupIndex: Int,
    val trackIndex: Int,
    val trackType: Int, // C.TRACK_TYPE_AUDIO or C.TRACK_TYPE_TEXT
    val isSelected: Boolean = false
)

data class QualityOption(
    val id: String,
    val label: String,
    val width: Int,
    val height: Int,
    val bitrate: Int,
    val groupIndex: Int,
    val trackIndex: Int,
    val isSelected: Boolean = false
)
