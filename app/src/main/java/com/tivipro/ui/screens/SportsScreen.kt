package com.tivipro.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tivipro.data.network.DeviceManager
import com.tivipro.model.SportsEvent
import com.tivipro.ui.components.FocusableCard
import com.tivipro.ui.components.TiviProLogo
import com.tivipro.ui.theme.DarkBackground
import com.tivipro.ui.theme.OrangeGlow
import com.tivipro.ui.theme.OrangePrimary
import com.tivipro.ui.theme.StatusSuccess
import com.tivipro.ui.theme.SurfaceDarkCard
import com.tivipro.ui.theme.TextMuted
import com.tivipro.ui.viewmodel.SportsViewModel

@Composable
fun SportsScreen(
    viewModel: SportsViewModel,
    onEventPlay: (SportsEvent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isPremium by viewModel.repository.isPremium.collectAsState()

    var alertedEventIds by remember { mutableStateOf(com.tivipro.data.network.TelegramAlertManager.getAlertedEventIds(context)) }
    var showTelegramSetupDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val status = DeviceManager.verificarDispositivo(context)
        viewModel.repository.setPremiumStatus(status.isPremium)
        viewModel.loadSportsEvents()
    }

    if (showTelegramSetupDialog) {
        TelegramSetupDialog(
            onDismiss = { showTelegramSetupDialog = false }
        )
    }

    LaunchedEffect(uiState.filteredEvents) {
        if (uiState.filteredEvents.isNotEmpty()) {
            com.tivipro.data.network.TelegramAlertManager.checkAndTriggerPendingAlerts(context, uiState.filteredEvents)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    val keyCode = event.nativeKeyEvent.keyCode
                    val isLeft = event.key == Key.DirectionLeft || keyCode == 21
                    val isRight = event.key == Key.DirectionRight || keyCode == 22
                    if (isLeft && isPremium) {
                        when (uiState.selectedDateTab) {
                            "Mañana" -> viewModel.selectDateTab("Hoy")
                            "Pasado Mañana" -> viewModel.selectDateTab("Mañana")
                        }
                        true
                    } else if (isRight && isPremium) {
                        when (uiState.selectedDateTab) {
                            "Hoy" -> viewModel.selectDateTab("Mañana")
                            "Mañana" -> viewModel.selectDateTab("Pasado Mañana")
                        }
                        true
                    } else false
                } else false
            }
    ) {
        val screenWidth = maxWidth
        val isCompactWidth = screenWidth < 600.dp
        val horizontalPadding = if (isCompactWidth) 14.dp else 28.dp
        val verticalPadding = if (maxHeight < 500.dp) 8.dp else 16.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
        ) {
            // TOP HEADER BAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SurfaceDarkCard)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(26.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "PROGRAMACIÓN DEPORTIVA",
                            color = Color.White,
                            fontSize = if (isCompactWidth) 15.sp else 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Guía de eventos deportivos en vivo y transmisiones",
                            color = TextMuted,
                            fontSize = if (isCompactWidth) 10.sp else 11.sp
                        )
                    }
                }

                TiviProLogo(sizeScale = if (isCompactWidth) 0.6f else 0.75f, showGlow = false)
            }

            Spacer(modifier = Modifier.height(10.dp))

            val firstDateStr = remember(uiState.allEvents) {
                uiState.allEvents.firstOrNull { it.dateTab == "Hoy" }?.id?.split("_")?.getOrNull(1)
            }
            val secondDateStr = remember(uiState.allEvents) {
                uiState.allEvents.firstOrNull { it.dateTab == "Mañana" }?.id?.split("_")?.getOrNull(1)
            }
            val thirdDateStr = remember(uiState.allEvents) {
                uiState.allEvents.firstOrNull { it.dateTab == "Pasado Mañana" }?.id?.split("_")?.getOrNull(1)
            }

            val todayDateTitle = remember(firstDateStr) { getTabFormattedTitleForDate(firstDateStr, 0) }
            val tomorrowDateTitle = remember(secondDateStr) { getTabFormattedTitleForDate(secondDateStr, 1) }
            val pasadoDateTitle = remember(thirdDateStr) { getTabFormattedTitleForDate(thirdDateStr, 2) }

            val dateTabs = remember(todayDateTitle, tomorrowDateTitle, pasadoDateTitle, isPremium) {
                if (isPremium) {
                    listOf(
                        "Hoy" to todayDateTitle,
                        "Mañana" to tomorrowDateTitle,
                        "Pasado Mañana" to pasadoDateTitle
                    )
                } else {
                    listOf(
                        "Hoy" to todayDateTitle
                    )
                }
            }

            // DATE TABS: HOY FECHA, DÍA_SEMANA FECHA 1, DÍA_SEMANA FECHA 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dateTabs.forEach { (tabKey, tabLabel) ->
                    val isSelected = uiState.selectedDateTab.equals(tabKey, ignoreCase = true)
                    FocusableCard(
                        onClick = { viewModel.selectDateTab(tabKey) },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(if (isSelected) OrangePrimary else SurfaceDarkCard),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabLabel,
                                color = if (isSelected) Color.White else TextMuted,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                fontSize = if (isCompactWidth) 11.sp else 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // EVENTS LIST (With Auto-Scroll to Live Event)
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = OrangePrimary)
                }
            } else if (uiState.filteredEvents.isEmpty()) {
                val currentTabFormattedTitle = when (uiState.selectedDateTab) {
                    "Hoy" -> todayDateTitle
                    "Mañana" -> tomorrowDateTitle
                    else -> pasadoDateTitle
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No hay eventos deportivos para $currentTabFormattedTitle",
                            color = TextMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                val listState = rememberLazyListState()
                val liveFocusRequester = remember { FocusRequester() }

                // Find index of first live event or closest upcoming event for "Hoy"
                val liveIndex = remember(uiState.selectedDateTab, uiState.filteredEvents) {
                    val liveIdx = uiState.filteredEvents.indexOfFirst { it.isLive }
                    if (liveIdx >= 0) liveIdx else 0
                }

                LaunchedEffect(uiState.selectedDateTab, uiState.filteredEvents) {
                    if (liveIndex >= 0 && uiState.filteredEvents.isNotEmpty()) {
                        try {
                            listState.animateScrollToItem(liveIndex)
                            liveFocusRequester.requestFocus()
                        } catch (_: Exception) {}
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(uiState.filteredEvents, key = { index, e -> "${e.id}_$index" }) { index, event ->
                        val isAlerted = alertedEventIds.contains(event.id)
                        SportsEventCard(
                            event = event,
                            isAlerted = isAlerted,
                            onEventCardClick = {
                                if (event.isLive && event.streamUrl.isNotBlank()) {
                                    onEventPlay(event)
                                } else {
                                    if (!com.tivipro.data.network.TelegramAlertManager.isConfigured(context)) {
                                        showTelegramSetupDialog = true
                                    } else {
                                        val newAlertState = com.tivipro.data.network.TelegramAlertManager.toggleEventAlert(context, event)
                                        alertedEventIds = com.tivipro.data.network.TelegramAlertManager.getAlertedEventIds(context)
                                        val msg = if (newAlertState) "🔔 Alerta programada en Telegram (10 min antes) para: ${event.title}" else "🔕 Alerta de Telegram cancelada para: ${event.title}"
                                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onToggleAlert = {
                                if (!com.tivipro.data.network.TelegramAlertManager.isConfigured(context)) {
                                    showTelegramSetupDialog = true
                                } else {
                                    val newAlertState = com.tivipro.data.network.TelegramAlertManager.toggleEventAlert(context, event)
                                    alertedEventIds = com.tivipro.data.network.TelegramAlertManager.getAlertedEventIds(context)
                                    val msg = if (newAlertState) "🔔 Alerta programada en Telegram (10 min antes) para: ${event.title}" else "🔕 Alerta de Telegram cancelada para: ${event.title}"
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = if (index == liveIndex) Modifier.focusRequester(liveFocusRequester) else Modifier
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SportsEventCard(
    event: SportsEvent,
    isAlerted: Boolean = false,
    onEventCardClick: () -> Unit = {},
    onToggleAlert: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val greenHighlight = Color(0xFF00E676)
    val darkGreenBg = Color(0xFF0B2519)

    FocusableCard(
        onClick = onEventCardClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (event.isLive) darkGreenBg else SurfaceDarkCard
        ),
        focusedBorderColor = if (event.isLive) greenHighlight else null,
        unfocusedBorder = if (event.isLive) BorderStroke(1.5.dp, greenHighlight) else null
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            // Top Row: Sport Category + Live Badge (Left) | Competition Badge & Alarm Bell (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Badges: Sport + Live Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Sport Category Badge
                    Surface(
                        color = OrangePrimary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(5.dp),
                        border = BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = event.sportCategory,
                            color = OrangeGlow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (event.isLive) {
                        Surface(
                            color = Color(0xFF00C853),
                            shape = RoundedCornerShape(5.dp)
                        ) {
                            Text(
                                text = "🟢 EN DIRECTO",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Right Badges: Competition Badge + Focusable Alarm Bell Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = Color(0xFF2A1B4E),
                        shape = RoundedCornerShape(5.dp),
                        border = BorderStroke(1.dp, Color(0xFF673AB7))
                    ) {
                        Text(
                            text = event.competition,
                            color = Color(0xFFD1C4E9),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Focusable Alarm Bell Button (10 min Telegram alert)
                    FocusableCard(
                        onClick = onToggleAlert,
                        shape = RoundedCornerShape(5.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAlerted) Color(0xFF3B2810) else Color(0xFF1E1E2C)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = if (isAlerted) Icons.Rounded.NotificationsActive else Icons.Rounded.Notifications,
                                contentDescription = "Alerta Telegram",
                                tint = if (isAlerted) OrangeGlow else TextMuted,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (isAlerted) "🔔 ACTIVA" else "🔔 ALERTA",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAlerted) OrangeGlow else TextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Teams / Event Title
            Text(
                text = event.title,
                color = Color.White,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Bottom Row: Hour & Channel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF131922), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = if (event.isLive) greenHighlight else StatusSuccess,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = event.dateTimeDisplay,
                        color = if (event.isLive) greenHighlight else StatusSuccess,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tv,
                        contentDescription = "Emisión",
                        tint = OrangePrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = event.channelName,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun getTabFormattedTitleForDate(dateStr: String?, fallbackOffset: Int): String {
    val localeEs = java.util.Locale("es", "ES")
    val tz = java.util.TimeZone.getTimeZone("Europe/Madrid")
    val cal = java.util.Calendar.getInstance(tz)

    if (!dateStr.isNullOrBlank()) {
        try {
            val sdfIn = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply { timeZone = tz }
            val parsed = sdfIn.parse(dateStr)
            if (parsed != null) {
                cal.time = parsed
            } else {
                cal.add(java.util.Calendar.DAY_OF_YEAR, fallbackOffset)
            }
        } catch (_: Exception) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, fallbackOffset)
        }
    } else {
        cal.add(java.util.Calendar.DAY_OF_YEAR, fallbackOffset)
    }

    return if (fallbackOffset == 0) {
        val sdf = java.text.SimpleDateFormat("d 'de' MMMM", localeEs)
        "HOY " + sdf.format(cal.time)
    } else {
        val sdf = java.text.SimpleDateFormat("EEEE d 'de' MMMM", localeEs)
        val raw = sdf.format(cal.time)
        raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(localeEs) else it.toString() }
    }
}

@Composable
fun TelegramSetupDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val deviceId = remember { DeviceManager.getDeviceId(context) }
    val initialUser = remember { com.tivipro.data.network.TelegramAlertManager.getTelegramUser(context) }
    var telegramUser by remember { mutableStateOf(initialUser) }
    var statusMsg by remember { mutableStateOf<String?>(null) }

    val webTelegramUrl = remember(deviceId) { "https://tivipro.duckdns.org/tivipro/telegram.php?id=$deviceId" }
    val qrCodeUrl = remember(webTelegramUrl) { com.tivipro.ui.components.QrCodeGenerator.getQrCodeUrl(webTelegramUrl) }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(2000)
            try {
                val polledUser = DeviceManager.pollTelegramCredentials(context)
                if (!polledUser.isNullOrBlank() && polledUser != initialUser) {
                    telegramUser = polledUser
                    statusMsg = "✅ ¡Nuevos datos recibidos desde la web! Conectando..."
                    val (ok, details) = com.tivipro.data.network.TelegramAlertManager.sendTestMessageDetailed(context, polledUser)
                    if (ok) {
                        statusMsg = "✅ ¡Vinculado con éxito desde la web a $polledUser!"
                        delay(1200)
                        onDismiss()
                        break
                    } else {
                        statusMsg = "❌ $details"
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun openWebPage() {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(webTelegramUrl))
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("http://84.235.239.152/tivipro/telegram.php?id=$deviceId"))
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {}
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints {
            val isCompact = maxHeight < 480.dp

            Surface(
                modifier = Modifier
                    .fillMaxWidth(if (isCompact) 0.88f else 0.85f)
                    .clip(RoundedCornerShape(18.dp)),
                color = com.tivipro.ui.theme.SurfaceDark
            ) {
                Column(
                    modifier = Modifier
                        .padding(if (isCompact) 14.dp else 20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                            Text("CONFIGURAR ALERTAS DE EVENTOS EN TELEGRAM", color = Color.White, fontWeight = FontWeight.Bold, fontSize = if (isCompact) 13.sp else 15.sp)
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }

                    Text(
                        text = "Escanea el código QR o pulsa el enlace para vincular tus alertas de Telegram automáticamente:",
                        color = TextMuted,
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    // QR Code (Clickable on touch)
                    FocusableCard(
                        onClick = { openWebPage() },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.size(if (isCompact) 120.dp else 160.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            coil.compose.AsyncImage(
                                model = qrCodeUrl,
                                contentDescription = "QR Configurar Alertas Telegram",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Clickable web link button for touch devices
                    FocusableCard(
                        onClick = { openWebPage() },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2B3C))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = webTelegramUrl,
                                color = Color(0xFF29B6F6),
                                fontSize = if (isCompact) 11.sp else 12.sp,
                                fontWeight = FontWeight.Bold,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                            )
                        }
                    }

                    // Notice banner for automatic closure on configuration
                    Surface(
                        color = Color(0xFF2B1A0E),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, OrangePrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "⏳", fontSize = 14.sp)
                            Text(
                                text = "Esta ventana se cerrará automáticamente al configurar el usuario desde la web",
                                color = OrangeGlow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    statusMsg?.let { msg ->
                        Text(
                            text = msg,
                            color = if (msg.startsWith("✅")) StatusSuccess else OrangeGlow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Cancel button
                    FocusableCard(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A40))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("CANCELAR", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
