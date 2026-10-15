package com.poltivi.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.poltivi.R

@Composable
fun PolTiviLogo(
    modifier: Modifier = Modifier,
    sizeScale: Float = 1.0f,
    showGlow: Boolean = true
) {
    val logoHeight = (38 * sizeScale).dp

    Image(
        painter = painterResource(id = R.drawable.logo),
        contentDescription = "PolTivi Logo",
        contentScale = ContentScale.Fit,
        modifier = modifier.height(logoHeight)
    )
}
