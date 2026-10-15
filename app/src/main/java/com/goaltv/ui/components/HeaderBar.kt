package com.goaltv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goaltv.ui.theme.OrangePrimary
import com.goaltv.ui.theme.StatusSuccess
import com.goaltv.ui.theme.SurfaceDarkCard
import com.goaltv.ui.theme.TextMuted
import com.goaltv.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HeaderBar(
    currentTime: String = "",
    currentDate: String = "",
    currentDateTime: String = "",
    isReloading: Boolean = false,
    isDohEnabled: Boolean = true,
    onSearchClick: () -> Unit = {},
    onReloadClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onToggleDoh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isReloading) 360f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "refresh_rotation"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: GOALTV Logo
        GoalTvLogo(
            sizeScale = 0.8f,
            modifier = Modifier.clickable { onAccountClick() }
        )

        // Right Controls & Header Info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Time and Date
            if (currentTime.isNotBlank() || currentDate.isNotBlank()) {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    if (currentTime.isNotBlank()) {
                        Text(
                            text = currentTime,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (currentDate.isNotBlank()) {
                        Text(
                            text = currentDate,
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else if (currentDateTime.isNotBlank()) {
                Text(
                    text = currentDateTime,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                HeaderClock()
            }

            // Search Button: "Búsqueda maestra"
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceDarkCard)
                    .border(
                        width = 1.dp,
                        color = OrangePrimary.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { onSearchClick() }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Búsqueda maestra",
                        tint = OrangePrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Búsqueda maestra",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // VPNBook Badge
            VpnBookBadge()

            // Account Icon Button
            IconButton(
                onClick = onAccountClick,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(SurfaceDarkCard)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AccountCircle,
                    contentDescription = "Cuenta / Iniciar Sesión",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun VpnBookBadge(
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val isVpnActive by com.goaltv.data.network.VpnBookManager.isVpnActive.collectAsState()
    val isConnecting by com.goaltv.data.network.VpnBookManager.isConnecting.collectAsState()

    val badgeBg = if (isVpnActive) Color(0xFF0F2E1E) else SurfaceDarkCard
    val badgeContentColor = if (isVpnActive) StatusSuccess else TextSecondary
    val statusText = when {
        isConnecting -> "VPNBook ⏳"
        isVpnActive -> "VPNBook 🛡️ Activo"
        else -> "VPNBook"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(badgeBg)
            .border(
                width = 1.dp,
                color = if (isVpnActive) StatusSuccess.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable {
                scope.launch {
                    com.goaltv.data.network.VpnBookManager.toggleVpn(context)
                }
            }
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = if (isVpnActive) Icons.Rounded.Security else Icons.Rounded.Lock,
                contentDescription = statusText,
                tint = badgeContentColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = statusText,
                color = badgeContentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun HeaderClock(
    modifier: Modifier = Modifier
) {
    var timeStr by remember { mutableStateOf("") }
    var dateStr by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormatter = SimpleDateFormat("HH:mm", Locale.US)
        val dateFormatter = SimpleDateFormat("MMMM d, yyyy", Locale("es", "ES"))
        while (isActive) {
            val now = Date()
            timeStr = timeFormatter.format(now).uppercase()
            dateStr = dateFormatter.format(now)
            delay(10000L)
        }
    }

    if (timeStr.isNotBlank() || dateStr.isNotBlank()) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = modifier
        ) {
            if (timeStr.isNotBlank()) {
                Text(
                    text = timeStr,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (dateStr.isNotBlank()) {
                Text(
                    text = dateStr,
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
