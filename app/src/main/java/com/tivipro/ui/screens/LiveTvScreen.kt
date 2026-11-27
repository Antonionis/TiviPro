package com.tivipro.ui.screens

import androidx.annotation.OptIn
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.tivipro.data.repository.DataRepository
import com.tivipro.model.EpgProgram
import com.tivipro.model.LiveChannel
import com.tivipro.ui.components.FocusableCard
import com.tivipro.ui.components.TiviProLogo
import com.tivipro.ui.theme.DarkBackground
import com.tivipro.ui.theme.OrangeGlow
import com.tivipro.ui.theme.OrangePrimary
import com.tivipro.ui.theme.StatusSuccess
import com.tivipro.ui.theme.SurfaceDark
import com.tivipro.ui.theme.SurfaceDarkCard
import com.tivipro.ui.theme.TextMuted
import com.tivipro.ui.theme.TextSecondary
import com.tivipro.ui.viewmodel.LiveTvViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun LiveTvScreen(
    viewModel: LiveTvViewModel,
    onChannelPlay: (LiveChannel) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val selectedCategoryFocusRequester = remember { FocusRequester() }
    val selectedChannelFocusRequester = remember { FocusRequester() }
    val epgFocusRequester = remember { FocusRequester() }

    var showChannelSelectorForMultiview by remember { mutableStateOf(false) }
    var showMultiviewDialog by remember { mutableStateOf(false) }
    var multiviewCh1 by remember { mutableStateOf<LiveChannel?>(null) }
    var multiviewCh2 by remember { mutableStateOf<LiveChannel?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredChannels = remember(uiState.channels, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.channels
        } else {
            uiState.channels.filter { ch ->
                ch.name.contains(searchQuery, ignoreCase = true) ||
                ch.channelNumber.toString().contains(searchQuery)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    if (showChannelSelectorForMultiview) {
        SelectMultiviewChannelsDialog(
            channels = uiState.channels,
            initialCh1 = uiState.selectedChannel,
            initialCh2 = uiState.channels.getOrNull(1) ?: uiState.selectedChannel,
            onConfirm = { ch1, ch2 ->
                multiviewCh1 = ch1
                multiviewCh2 = ch2
                showChannelSelectorForMultiview = false
                showMultiviewDialog = true
            },
            onDismiss = { showChannelSelectorForMultiview = false }
        )
    }

    if (showMultiviewDialog) {
        MultiviewDialog(
            channels = uiState.channels,
            initialChannel1 = multiviewCh1 ?: uiState.selectedChannel,
            initialChannel2 = multiviewCh2 ?: uiState.channels.getOrNull(1) ?: uiState.selectedChannel,
            repository = viewModel.repository,
            onDismiss = { showMultiviewDialog = false }
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        val screenWidth = maxWidth
        val isCompactWidth = screenWidth < 600.dp
        val isCompactHeight = maxHeight < 500.dp
        val horizontalPadding = if (isCompactWidth) 8.dp else 20.dp
        val verticalPadding = if (isCompactHeight) 2.dp else 8.dp
        val computedChannelRowHeight = if (isCompactHeight) 32.dp else 42.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
        ) {
            // Top Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(if (isCompactHeight) 30.dp else 36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceDarkCard)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White,
                            modifier = Modifier.size(if (isCompactHeight) 16.dp else 20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        imageVector = Icons.Rounded.Tv,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(if (isCompactHeight) 20.dp else 24.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Column {
                        Text(
                            text = "TELEVISIÓN EN DIRECTO",
                            color = Color.White,
                            fontSize = if (isCompactHeight) 13.sp else 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Search Bar for Live Channels (Custom BasicTextField to prevent vertical text clipping)
                    Box(
                        modifier = Modifier
                            .width(if (isCompactWidth) 110.dp else 150.dp)
                            .height(32.dp)
                            .background(Color(0xFF1B1B2C), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFF33334A), RoundedCornerShape(16.dp))
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Buscar canal",
                                tint = OrangePrimary,
                                modifier = Modifier.size(15.dp)
                            )

                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Buscar canal...",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        maxLines = 1
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 15.sp
                                    ),
                                    cursorBrush = SolidColor(OrangePrimary),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Limpiar",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }

                    FocusableCard(
                        onClick = { showChannelSelectorForMultiview = true },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard),
                        elevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AspectRatio,
                                contentDescription = "Modo Multiview",
                                tint = OrangePrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Multiview",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    TiviProLogo(sizeScale = if (isCompactWidth) 0.6f else 0.75f, showGlow = false)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val categoryListState = rememberLazyListState()
            val selectedCategoryIndex = remember(uiState.selectedCategory, uiState.isFavoritesSelected, uiState.categories) {
                if (uiState.selectedCategory == null && !uiState.isFavoritesSelected) 0
                else if (uiState.isFavoritesSelected) 1
                else {
                    val idx = uiState.categories.indexOfFirst { it.id == uiState.selectedCategory?.id }
                    if (idx >= 0) idx + 2 else 0
                }
            }

            LaunchedEffect(selectedCategoryIndex) {
                try {
                    categoryListState.animateScrollToItem(selectedCategoryIndex)
                } catch (_: Exception) {}
            }

            // Categories Filter Bar
            LazyRow(
                state = categoryListState,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    val isAllSelected = uiState.selectedCategory == null && !uiState.isFavoritesSelected
                    FocusableCard(
                        onClick = { viewModel.selectCategory(null) },
                        shape = RoundedCornerShape(18.dp),
                        modifier = if (isAllSelected) Modifier.focusRequester(selectedCategoryFocusRequester) else Modifier
                    ) {
                        Box(
                            modifier = Modifier
                                .background(if (isAllSelected) OrangePrimary else Color(0xFF222232))
                                .border(
                                    width = if (isAllSelected) 2.dp else 1.dp,
                                    color = if (isAllSelected) OrangeGlow else Color(0xFF33334A),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Todos",
                                color = if (isAllSelected) Color.White else TextSecondary,
                                fontSize = 10.5.sp,
                                fontWeight = if (isAllSelected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        }
                    }
                }

                item {
                    val isFavSelected = uiState.isFavoritesSelected
                    FocusableCard(
                        onClick = { viewModel.selectFavorites() },
                        shape = RoundedCornerShape(18.dp),
                        modifier = if (isFavSelected) Modifier.focusRequester(selectedCategoryFocusRequester) else Modifier
                    ) {
                        Box(
                            modifier = Modifier
                                .background(if (isFavSelected) OrangePrimary else Color(0xFF222232))
                                .border(
                                    width = if (isFavSelected) 2.dp else 1.dp,
                                    color = if (isFavSelected) OrangeGlow else Color(0xFF33334A),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⭐ Favoritos",
                                color = if (isFavSelected) Color.White else TextSecondary,
                                fontSize = 10.5.sp,
                                fontWeight = if (isFavSelected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        }
                    }
                }

                items(uiState.categories, key = { it.id }) { category ->
                    val isSelected = uiState.selectedCategory?.id == category.id
                    FocusableCard(
                        onClick = { viewModel.selectCategory(category) },
                        shape = RoundedCornerShape(18.dp),
                        modifier = if (isSelected) Modifier.focusRequester(selectedCategoryFocusRequester) else Modifier
                    ) {
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) OrangePrimary else Color(0xFF222232))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) OrangeGlow else Color(0xFF33334A),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category.name,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = OrangePrimary)
                }
            } else {
                // Main Content: Channels List on Left, EPG Guide on Right
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Channels Column (45% width)
                    Column(
                        modifier = Modifier
                            .weight(if (isCompactWidth) 0.5f else 0.45f)
                            .fillMaxHeight()
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "CANALES (${uiState.channels.size})" else "CANALES (${filteredChannels.size})",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        val listState = rememberLazyListState()
                        val selectedChannelIndex = remember(filteredChannels, uiState.selectedChannel) {
                            filteredChannels.indexOfFirst { it.id == uiState.selectedChannel?.id }
                        }

                        LaunchedEffect(uiState.selectedChannel?.id) {
                            if (selectedChannelIndex >= 0) {
                                try {
                                    listState.animateScrollToItem(selectedChannelIndex)
                                    selectedChannelFocusRequester.requestFocus()
                                } catch (_: Exception) {}
                            }
                        }

                        var lastUpPressTime by remember { mutableStateOf(0L) }
                        var lastDownPressTime by remember { mutableStateOf(0L) }

                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(filteredChannels, key = { index, ch -> "${ch.id}_$index" }) { index, channel ->
                                val isSelected = uiState.selectedChannel?.id == channel.id
                                ChannelListItem(
                                    channel = channel,
                                    isSelected = isSelected,
                                    onSelect = { viewModel.selectChannel(channel) },
                                    onPlay = { onChannelPlay(channel) },
                                    onToggleFavorite = { viewModel.toggleFavorite(channel.id) },
                                    itemHeight = computedChannelRowHeight,
                                    selectedChannelFocusRequester = selectedChannelFocusRequester,
                                    epgFocusRequester = epgFocusRequester,
                                    isFirstItem = (index == 0),
                                    selectedCategoryFocusRequester = selectedCategoryFocusRequester,
                                    modifier = Modifier.onKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown) {
                                            val now = System.currentTimeMillis()
                                            if (event.key == Key.DirectionUp || event.nativeKeyEvent.keyCode == 19) {
                                                if (now - lastUpPressTime < 300) {
                                                    try {
                                                        scope.launch {
                                                            if (filteredChannels.isNotEmpty()) {
                                                                listState.animateScrollToItem(0)
                                                                viewModel.selectChannel(filteredChannels.first())
                                                            }
                                                        }
                                                    } catch (_: Exception) {}
                                                    true
                                                } else {
                                                    lastUpPressTime = now
                                                    false
                                                }
                                            } else if (event.key == Key.DirectionDown || event.nativeKeyEvent.keyCode == 20) {
                                                if (now - lastDownPressTime < 300) {
                                                    try {
                                                        scope.launch {
                                                            if (filteredChannels.isNotEmpty()) {
                                                                val lastIndex = filteredChannels.size - 1
                                                                listState.animateScrollToItem(lastIndex)
                                                                viewModel.selectChannel(filteredChannels.last())
                                                            }
                                                        }
                                                    } catch (_: Exception) {}
                                                    true
                                                } else {
                                                    lastDownPressTime = now
                                                    false
                                                }
                                            } else false
                                        } else false
                                    }
                                )
                            }
                        }
                    }

                    // EPG Timeline Column (55% width)
                    Column(
                        modifier = Modifier
                            .weight(if (isCompactWidth) 0.5f else 0.55f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceDark)
                            .padding(10.dp)
                            .focusProperties {
                                left = selectedChannelFocusRequester
                            }
                    ) {
                        val channel = uiState.selectedChannel

                        if (channel != null) {
                            // Channel Header inside EPG panel
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = if (isCompactHeight) 4.dp else 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = channel.iconUrl,
                                        contentDescription = channel.name,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .size(if (isCompactHeight) 26.dp else 34.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SurfaceDarkCard)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = channel.name,
                                            fontSize = if (isCompactHeight) 13.sp else 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Canal ${channel.channelNumber ?: ""}",
                                            fontSize = 10.sp,
                                            color = OrangeGlow
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onChannelPlay(channel) },
                                    modifier = Modifier
                                        .size(if (isCompactHeight) 30.dp else 36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(OrangePrimary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "Reproducción Rápida",
                                        tint = Color.White,
                                        modifier = Modifier.size(if (isCompactHeight) 18.dp else 22.dp)
                                    )
                                }
                            }

                            // Filter EPG to ONLY current live event + next 4 events (max 5 total)
                            val filteredEpgPrograms = remember(uiState.selectedChannel?.id, uiState.epgPrograms) {
                                val programs = uiState.epgPrograms
                                if (programs.isEmpty()) return@remember emptyList()

                                val nowSec = System.currentTimeMillis() / 1000L
                                val liveIdx = programs.indexOfFirst { it.isCurrentlyLive || (it.startTimestamp <= nowSec && it.endTimestamp > nowSec) }
                                if (liveIdx >= 0) {
                                    programs.subList(liveIdx, (liveIdx + 5).coerceAtMost(programs.size))
                                } else {
                                    val upcomingIdx = programs.indexOfFirst { it.endTimestamp > nowSec }
                                    if (upcomingIdx >= 0) {
                                        programs.subList(upcomingIdx, (upcomingIdx + 5).coerceAtMost(programs.size))
                                    } else {
                                        programs.takeLast(5)
                                    }
                                }
                            }

                            if (filteredEpgPrograms.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No hay datos EPG disponibles para este canal.",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                val epgListState = rememberLazyListState()

                                LazyColumn(
                                    state = epgListState,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    itemsIndexed(filteredEpgPrograms, key = { index, program -> "${program.id}_$index" }) { _, program ->
                                        EpgProgramItem(
                                            program = program,
                                            onPlayCatchup = { url ->
                                                onChannelPlay(channel.copy(streamUrl = url, name = "🔴 CATCH-UP: ${program.title}"))
                                            },
                                            modifier = Modifier
                                                .focusRequester(epgFocusRequester)
                                                .focusProperties {
                                                    left = selectedChannelFocusRequester
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
                                Text(
                                    text = "Selecciona un canal para ver la guía EPG.",
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectMultiviewChannelsDialog(
    channels: List<LiveChannel>,
    initialCh1: LiveChannel?,
    initialCh2: LiveChannel?,
    onConfirm: (LiveChannel, LiveChannel) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCh1 by remember { mutableStateOf(initialCh1 ?: channels.firstOrNull()) }
    var selectedCh2 by remember { mutableStateOf(initialCh2 ?: channels.getOrNull(1) ?: channels.firstOrNull()) }

    var isSelectingForCh1 by remember { mutableStateOf(false) }
    var isSelectingForCh2 by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints {
            val isCompactHeight = maxHeight < 480.dp
            val dialogWidth = if (isCompactHeight) 0.88f else 0.70f

            Surface(
                modifier = Modifier
                    .fillMaxWidth(dialogWidth)
                    .clip(RoundedCornerShape(18.dp)),
                color = SurfaceDark
            ) {
                Column(
                    modifier = Modifier
                        .padding(if (isCompactHeight) 10.dp else 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 6.dp else 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📺 MODO MULTIVIEW - SELECCIONAR CANALES",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isCompactHeight) 13.sp else 15.sp
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }

                    Text(
                        text = "Elige los 2 canales que deseas ver simultáneamente en la pantalla dividida:",
                        color = TextMuted,
                        fontSize = if (isCompactHeight) 10.5.sp else 12.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Left Screen Channel Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard)
                        ) {
                            Column(
                                modifier = Modifier.padding(if (isCompactHeight) 8.dp else 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("PANTALLA 1 (IZQUIERDA)", color = OrangePrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = selectedCh1?.name ?: "Sin canal",
                                    color = Color.White,
                                    fontSize = if (isCompactHeight) 12.sp else 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                FocusableCard(
                                    onClick = {
                                        isSelectingForCh1 = !isSelectingForCh1
                                        isSelectingForCh2 = false
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelectingForCh1) OrangePrimary else Color(0xFF222238)
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("ELEGIR CANAL 1", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }

                        // Right Screen Channel Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard)
                        ) {
                            Column(
                                modifier = Modifier.padding(if (isCompactHeight) 8.dp else 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("PANTALLA 2 (DERECHA)", color = OrangePrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = selectedCh2?.name ?: "Sin canal",
                                    color = Color.White,
                                    fontSize = if (isCompactHeight) 12.sp else 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                FocusableCard(
                                    onClick = {
                                        isSelectingForCh2 = !isSelectingForCh2
                                        isSelectingForCh1 = false
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelectingForCh2) OrangePrimary else Color(0xFF222238)
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("ELEGIR CANAL 2", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    // Channel Picker List if selecting
                    if (isSelectingForCh1 || isSelectingForCh2) {
                        val targetTitle = if (isSelectingForCh1) "Seleccionando para Pantalla 1:" else "Seleccionando para Pantalla 2:"
                        Text(text = targetTitle, color = OrangeGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = if (isCompactHeight) 120.dp else 180.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            channels.forEach { ch ->
                                FocusableCard(
                                    onClick = {
                                        if (isSelectingForCh1) {
                                            selectedCh1 = ch
                                            isSelectingForCh1 = false
                                        } else {
                                            selectedCh2 = ch
                                            isSelectingForCh2 = false
                                        }
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C))
                                ) {
                                    Text(
                                        text = ch.name,
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    FocusableCard(
                        onClick = {
                            val c1 = selectedCh1 ?: channels.firstOrNull()
                            val c2 = selectedCh2 ?: channels.getOrNull(1) ?: c1
                            if (c1 != null && c2 != null) {
                                onConfirm(c1, c2)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isCompactHeight) 38.dp else 44.dp),
                        colors = CardDefaults.cardColors(containerColor = OrangePrimary)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("▶ INICIAR MULTIVIEW (2 CANALES)", fontWeight = FontWeight.Bold, fontSize = if (isCompactHeight) 11.5.sp else 13.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelListItem(
    channel: LiveChannel,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    itemHeight: Dp = 44.dp,
    selectedChannelFocusRequester: FocusRequester? = null,
    epgFocusRequester: FocusRequester? = null,
    isFirstItem: Boolean = false,
    selectedCategoryFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(itemHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Main Channel Play Area (Logo + Name + Number)
        FocusableCard(
            onClick = {
                if (isSelected) {
                    onPlay()
                } else {
                    onSelect()
                }
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .focusProperties {
                    if (isFirstItem && selectedCategoryFocusRequester != null) {
                        up = selectedCategoryFocusRequester
                    }
                }
                .then(if (isSelected && selectedChannelFocusRequester != null) Modifier.focusRequester(selectedChannelFocusRequester) else Modifier)
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

                Spacer(modifier = Modifier.width(6.dp))

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

        // Dedicated Focusable Heart / Favorite Button
        FocusableCard(
            onClick = onToggleFavorite,
            modifier = Modifier
                .size(34.dp)
                .focusProperties {
                    if (isFirstItem && selectedCategoryFocusRequester != null) {
                        up = selectedCategoryFocusRequester
                    }
                    if (epgFocusRequester != null) {
                        right = epgFocusRequester
                    }
                },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (channel.isFavorite) Color(0xFF3B1A24) else Color(0xFF1E1E2C)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (channel.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorito",
                    tint = if (channel.isFavorite) Color(0xFFFF3366) else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun EpgProgramItem(
    program: EpgProgram,
    onPlayCatchup: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isLive = program.isCurrentlyLive || program.isLive
    val hasCatchup = !program.catchupUrl.isNullOrBlank()

    val timeFormatter = remember {
        SimpleDateFormat("HH:mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Europe/Madrid")
        }
    }
    val startTime = timeFormatter.format(Date(program.startTimestamp * 1000))
    val endTime = timeFormatter.format(Date(program.endTimestamp * 1000))

    FocusableCard(
        onClick = {},
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isLive) Modifier.border(1.dp, OrangePrimary, RoundedCornerShape(6.dp)) else Modifier
            ),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLive) Color(0xFF13283E) else SurfaceDarkCard
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
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
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$startTime - $endTime",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLive) OrangePrimary else TextSecondary
                    )
                }

                if (isLive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(StatusSuccess)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "EN DIRECTO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = DarkBackground
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = program.title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!program.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = program.description,
                    fontSize = 9.5.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isLive) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { program.progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = OrangePrimary,
                    trackColor = Color(0xFF1E3A5F)
                )
            }

            if (hasCatchup && !isLive) {
                Spacer(modifier = Modifier.height(4.dp))
                FocusableCard(
                    onClick = { program.catchupUrl?.let { url -> onPlayCatchup?.invoke(url) } },
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFC62828))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Rounded.Schedule, contentDescription = "Catchup", tint = Color.White, modifier = Modifier.size(10.dp))
                        Text("▶ EN DIFERIDO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun MultiviewDialog(
    channels: List<LiveChannel>,
    initialChannel1: LiveChannel?,
    initialChannel2: LiveChannel?,
    repository: DataRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var channel1 by remember { mutableStateOf(initialChannel1 ?: channels.firstOrNull()) }
    var channel2 by remember { mutableStateOf(initialChannel2 ?: channels.getOrNull(1) ?: channels.firstOrNull()) }
    var activeAudio by remember { mutableIntStateOf(1) }

    val okHttpClient = remember { repository.getPlayerOkHttpClient() }
    val dataSourceFactory = remember { OkHttpDataSource.Factory(okHttpClient) }
    val mediaSourceFactory = remember { DefaultMediaSourceFactory(dataSourceFactory) }

    val player1 = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    val player2 = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    LaunchedEffect(activeAudio) {
        player1.volume = if (activeAudio == 1) 1f else 0f
        player2.volume = if (activeAudio == 2) 1f else 0f
    }

    LaunchedEffect(channel1?.streamUrl) {
        channel1?.streamUrl?.let { url ->
            if (url.isNotBlank()) {
                player1.setMediaItem(MediaItem.fromUri(url))
                player1.prepare()
            }
        }
    }

    LaunchedEffect(channel2?.streamUrl) {
        channel2?.streamUrl?.let { url ->
            if (url.isNotBlank()) {
                player2.setMediaItem(MediaItem.fromUri(url))
                player2.prepare()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player1.release()
            player2.release()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📺 MODO MULTIVIEW (2 CANALES)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Surface(
                            color = OrangePrimary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("Audio: Pantalla $activeAudio", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }

                // Dual Video Split Layout
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    // Left Screen (Canal 1)
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().border(2.dp, if (activeAudio == 1) OrangePrimary else Color.Transparent)) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = player1
                                    useController = false
                                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "1. ${channel1?.name ?: "Canal 1"}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FocusableCard(
                                    onClick = { activeAudio = 1 },
                                    shape = RoundedCornerShape(4.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (activeAudio == 1) OrangePrimary else Color(0xFF2A2A40))
                                ) {
                                    Box(modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)) {
                                        Text("🔊 AUDIO 1", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    // Right Screen (Canal 2)
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().border(2.dp, if (activeAudio == 2) OrangePrimary else Color.Transparent)) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = player2
                                    useController = false
                                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "2. ${channel2?.name ?: "Canal 2"}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FocusableCard(
                                    onClick = { activeAudio = 2 },
                                    shape = RoundedCornerShape(4.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (activeAudio == 2) OrangePrimary else Color(0xFF2A2A40))
                                ) {
                                    Box(modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)) {
                                        Text("🔊 AUDIO 2", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
