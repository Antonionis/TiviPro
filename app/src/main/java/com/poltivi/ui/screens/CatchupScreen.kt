package com.poltivi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.poltivi.data.repository.DataRepository
import com.poltivi.data.repository.EpgSourceMode
import com.poltivi.model.EpgProgram
import com.poltivi.model.LiveChannel
import com.poltivi.ui.components.FocusableCard
import com.poltivi.ui.components.PolTiviLogo
import com.poltivi.ui.theme.DarkBackground
import com.poltivi.ui.theme.OrangeGlow
import com.poltivi.ui.theme.OrangePrimary
import com.poltivi.ui.theme.StatusSuccess
import com.poltivi.ui.theme.SurfaceDark
import com.poltivi.ui.theme.SurfaceDarkCard
import com.poltivi.ui.theme.TextMuted
import com.poltivi.ui.theme.TextSecondary
import com.poltivi.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun CatchupScreen(
    viewModel: MainViewModel,
    onProgramPlay: (LiveChannel, EpgProgram, String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val repository = viewModel.repository
    val scope = rememberCoroutineScope()

    var channels by remember { mutableStateOf<List<LiveChannel>>(emptyList()) }
    var selectedChannel by remember { mutableStateOf<LiveChannel?>(null) }
    var epgPrograms by remember { mutableStateOf<List<EpgProgram>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val channelFocusRequester = remember { FocusRequester() }
    val epgFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val catchupChs = repository.getCatchupChannels()
            channels = catchupChs
            val firstCh = catchupChs.firstOrNull()
            selectedChannel = firstCh
            if (firstCh != null) {
                epgPrograms = repository.getEpgForChannel(firstCh.id, firstCh.epgId, forceProvider = true)
            }
        } catch (_: Exception) {
        } finally {
            isLoading = false
        }
    }

    fun loadEpgForChannel(ch: LiveChannel) {
        selectedChannel = ch
        scope.launch {
            try {
                epgPrograms = repository.getEpgForChannel(ch.id, ch.epgId, forceProvider = true)
            } catch (_: Exception) {}
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        val isCompactWidth = maxWidth < 600.dp
        val isCompactHeight = maxHeight < 500.dp
        val horizontalPadding = if (isCompactWidth) 12.dp else 24.dp
        val verticalPadding = if (isCompactHeight) 6.dp else 12.dp
        val channelRowHeight = if (isCompactHeight) 38.dp else 46.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
        ) {
            // Header Bar (NO Categories Row!)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
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
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(26.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "GRABACIONES CATCH-UP",
                            color = Color.White,
                            fontSize = if (isCompactWidth) 15.sp else 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Emisiones en diferido del proveedor IPTV",
                            color = TextMuted,
                            fontSize = if (isCompactWidth) 10.sp else 11.sp
                        )
                    }
                }

                PolTiviLogo(sizeScale = if (isCompactWidth) 0.6f else 0.75f, showGlow = false)
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = OrangePrimary)
                }
            } else if (channels.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "El servidor actual no dispone de canales con servicio Catch-Up.",
                            color = TextMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left Column: Catch-Up Channels List
                    Column(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight()
                    ) {
                        Text(
                            text = "CANALES CATCH-UP (${channels.size})",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        val listState = rememberLazyListState()

                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(channels, key = { index, ch -> "${ch.id}_$index" }) { index, channel ->
                                val isSelected = selectedChannel?.id == channel.id
                                CatchupChannelListItem(
                                    channel = channel,
                                    isSelected = isSelected,
                                    onSelect = { loadEpgForChannel(channel) },
                                    itemHeight = channelRowHeight,
                                    modifier = Modifier
                                        .focusProperties {
                                            right = epgFocusRequester
                                        }
                                        .then(if (isSelected) Modifier.focusRequester(channelFocusRequester) else Modifier)
                                )
                            }
                        }
                    }

                    // Right Column: EPG Programs Guide for Catchup Channel
                    Column(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceDark)
                            .padding(12.dp)
                            .focusProperties {
                                left = channelFocusRequester
                            }
                    ) {
                        val activeCh = selectedChannel

                        if (activeCh != null) {
                            // Channel Header
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                            ) {
                                AsyncImage(
                                    model = activeCh.iconUrl,
                                    contentDescription = activeCh.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceDarkCard)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = activeCh.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Servicio Catch-Up • ${activeCh.tvArchiveDuration} días de archivo",
                                        fontSize = 11.sp,
                                        color = OrangeGlow
                                    )
                                }
                            }

                            Text(
                                text = "EMISIONES DISPONIBLES EN DIFERIDO",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            if (epgPrograms.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Cargando guía de emisiones o no hay datos disponible.",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                val epgListState = rememberLazyListState()

                                LazyColumn(
                                    state = epgListState,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    itemsIndexed(epgPrograms, key = { index, p -> "${p.id}_$index" }) { _, program ->
                                        CatchupEpgProgramItem(
                                            program = program,
                                            channel = activeCh,
                                            repository = repository,
                                            onPlay = { url ->
                                                onProgramPlay(activeCh, program, url)
                                            },
                                            modifier = Modifier
                                                .focusRequester(epgFocusRequester)
                                                .focusProperties {
                                                    left = channelFocusRequester
                                                }
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Selecciona un canal para explorar emisiones en diferido.", color = TextMuted, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun decodeBase64IfNeeded(text: String?): String? {
    if (text.isNullOrBlank()) return text
    if (text.contains(" ") || text.contains("\n") || text.contains("\r")) return text
    return try {
        val bytes = android.util.Base64.decode(text, android.util.Base64.DEFAULT)
        val decoded = String(bytes, Charsets.UTF_8)
        if (decoded.none { it.code in 0..8 || it.code in 11..12 || it.code in 14..31 }) {
            decoded
        } else {
            text
        }
    } catch (_: Exception) {
        text
    }
}

@Composable
fun CatchupChannelListItem(
    channel: LiveChannel,
    isSelected: Boolean,
    onSelect: () -> Unit,
    itemHeight: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    FocusableCard(
        onClick = onSelect,
        modifier = modifier
            .fillMaxWidth()
            .height(itemHeight)
            .onFocusChanged { state ->
                if (state.isFocused) {
                    onSelect()
                }
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF2B2B40) else SurfaceDarkCard
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = channel.iconUrl,
                contentDescription = channel.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF16161E))
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) OrangeGlow else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CatchupEpgProgramItem(
    program: EpgProgram,
    channel: LiveChannel,
    repository: DataRepository,
    onPlay: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLive = program.isCurrentlyLive || program.isLive

    val displayTitle = remember(program.title) {
        decodeBase64IfNeeded(program.title) ?: program.title
    }
    val displayDesc = remember(program.description) {
        decodeBase64IfNeeded(program.description)
    }

    val timeFormatter = remember {
        SimpleDateFormat("HH:mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Europe/Madrid")
        }
    }
    val startTime = timeFormatter.format(Date(program.startTimestamp * 1000))
    val endTime = timeFormatter.format(Date(program.endTimestamp * 1000))

    val catchupStreamUrl = remember(program, channel) {
        program.catchupUrl ?: run {
            val durationMins = ((program.endTimestamp - program.startTimestamp) / 60L).coerceAtLeast(1L)
            val url = repository.buildCatchupStreamUrl(channel.id, program.startTimestamp, durationMins)
            if (url.isNotBlank()) url else channel.streamUrl
        }
    }

    FocusableCard(
        onClick = { onPlay(catchupStreamUrl) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLive) Color(0xFF13283E) else SurfaceDarkCard
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = "Hora",
                        tint = if (isLive) OrangePrimary else TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "$startTime - $endTime",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLive) OrangePrimary else TextSecondary
                    )
                }

                if (isLive) {
                    Surface(
                        color = StatusSuccess,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "🟢 EN DIRECTO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = displayTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (!displayDesc.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = displayDesc,
                    fontSize = 11.sp,
                    color = TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    color = OrangePrimary,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Reproducir", tint = Color.White, modifier = Modifier.size(12.dp))
                        Text("REPRODUCIR EMISIÓN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
