package com.tivipro.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tivipro.ui.components.FocusableCard
import com.tivipro.ui.theme.DarkBackground
import com.tivipro.ui.theme.OrangeGlow
import com.tivipro.ui.theme.OrangePrimary
import com.tivipro.ui.theme.StatusSuccess
import com.tivipro.ui.theme.SurfaceDark
import com.tivipro.ui.theme.SurfaceDarkCard
import com.tivipro.ui.theme.TextMuted
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlayerControls(
    isVisible: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    title: String,
    subTitle: String?,
    logoUrl: String?,
    isLive: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    aspectRatioMode: AspectRatioMode,
    audioTracks: List<TrackOption>,
    subtitleTracks: List<TrackOption>,
    qualityOptions: List<QualityOption>,
    toastMessage: String?,
    audioDelayMs: Int = 0,
    isPremium: Boolean = false,
    onAudioDelayChange: (Int) -> Unit = {},
    onPlayPauseToggle: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onRewind10s: () -> Unit,
    onForward10s: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onToggleAspectRatio: () -> Unit,
    onSelectAudioTrack: (TrackOption) -> Unit,
    onSelectSubtitleTrack: (TrackOption?) -> Unit,
    onSelectQuality: (QualityOption) -> Unit,
    onToggleDrawer: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAudioSubtitleDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var currentTimeString by remember { mutableStateOf("") }

    val playPauseFocusRequester = remember { FocusRequester() }
    val forwardFocusRequester = remember { FocusRequester() }
    val rewindFocusRequester = remember { FocusRequester() }
    val aspectRatioFocusRequester = remember { FocusRequester() }
    val subtitlesFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            try {
                delay(100)
                playPauseFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Update current clock
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            currentTimeString = sdf.format(Date())
            delay(1000)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Buffering Indicator
        if (isBuffering) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = OrangePrimary,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Cargando Transmisión...",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Toast / OSD Indicator Banner
        if (!toastMessage.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .safeDrawingPadding()
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceDarkCard.copy(alpha = 0.9f))
                    .border(1.dp, OrangePrimary, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = toastMessage,
                    color = OrangeGlow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Main Controls Overlay
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .safeDrawingPadding()
                    .padding(horizontal = 28.dp, vertical = 16.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
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

                        if (!logoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = logoUrl,
                                contentDescription = title,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceDark)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Column {
                            Text(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                            subTitle?.let { sub ->
                                Text(
                                    text = sub,
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Track info pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceDarkCard)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isLive) "HLS / En Vivo" else "1080p • AAC",
                                fontSize = 10.sp,
                                color = OrangeGlow,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Clock and Quality below
                        val activeQualityLabel = qualityOptions.firstOrNull { it.isSelected }?.label
                            ?: qualityOptions.firstOrNull()?.label
                            ?: "1080P"

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = currentTimeString,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Calidad: $activeQualityLabel",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMuted
                            )
                        }
                    }
                }

                // Center Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous
                    FocusableCard(
                        onClick = onPreviousClick,
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = "Anterior",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Rewind 10s
                    FocusableCard(
                        onClick = onRewind10s,
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard),
                        modifier = Modifier
                            .focusRequester(rewindFocusRequester)
                            .focusProperties {
                                right = playPauseFocusRequester
                            }
                            .size(44.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Replay10,
                                contentDescription = "Retroceder 10s",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Large Play / Pause Button
                    FocusableCard(
                        onClick = onPlayPauseToggle,
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(containerColor = OrangePrimary),
                        modifier = Modifier
                            .focusRequester(playPauseFocusRequester)
                            .focusProperties {
                                left = rewindFocusRequester
                                right = forwardFocusRequester
                            }
                            .size(64.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    // Fast Forward 10s
                    FocusableCard(
                        onClick = onForward10s,
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard),
                        modifier = Modifier
                            .focusRequester(forwardFocusRequester)
                            .focusProperties {
                                left = playPauseFocusRequester
                                right = aspectRatioFocusRequester
                            }
                            .size(44.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Forward10,
                                contentDescription = "Adelantar 10s",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Next
                    FocusableCard(
                        onClick = onNextClick,
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "Siguiente",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Bottom Bar Controls & Seek Slider
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                ) {
                    // Seek Bar for VOD or Live Badge
                    if (!isLive) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(currentPositionMs),
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            Slider(
                                value = currentPositionMs.toFloat().coerceIn(0f, durationMs.coerceAtLeast(1L).toFloat()),
                                onValueChange = { onSeekTo(it.toLong()) },
                                valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 10.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = OrangePrimary,
                                    activeTrackColor = OrangePrimary,
                                    inactiveTrackColor = Color(0xFF3E3E50)
                                )
                            )

                            Text(
                                text = formatTime(durationMs),
                                fontSize = 11.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(StatusSuccess)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "TRANSMISIÓN EN VIVO",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkBackground
                                    )
                                }
                            }
                        }
                    }

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left actions: EPG Drawer toggle
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isLive) {
                                FocusableCard(
                                    onClick = onToggleDrawer,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Tv,
                                            contentDescription = "Guía de Canales",
                                            tint = OrangePrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("CANALES", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Right actions: Aspect Ratio, Audio/Subtitles, Quality
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Aspect Ratio Button
                            FocusableCard(
                                onClick = onToggleAspectRatio,
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard),
                                modifier = Modifier
                                    .focusRequester(aspectRatioFocusRequester)
                                    .focusProperties {
                                        left = forwardFocusRequester
                                        right = subtitlesFocusRequester
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AspectRatio,
                                        contentDescription = "Relación de Aspecto",
                                        tint = OrangeGlow,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(aspectRatioMode.label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Audio & Subtitles
                            FocusableCard(
                                onClick = { showAudioSubtitleDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard),
                                modifier = Modifier
                                    .size(36.dp)
                                    .focusRequester(subtitlesFocusRequester)
                                    .focusProperties {
                                        left = aspectRatioFocusRequester
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Subtitles,
                                        contentDescription = "Audio y Subtítulos",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dialog for Audio & Subtitles
        if (showAudioSubtitleDialog) {
            AudioSubtitleSelectionDialog(
                audioTracks = audioTracks,
                subtitleTracks = subtitleTracks,
                audioDelayMs = audioDelayMs,
                isPremium = isPremium,
                onAudioDelayChange = onAudioDelayChange,
                onSelectAudio = {
                    onSelectAudioTrack(it)
                    showAudioSubtitleDialog = false
                },
                onSelectSubtitle = {
                    onSelectSubtitleTrack(it)
                    showAudioSubtitleDialog = false
                },
                onDismiss = { showAudioSubtitleDialog = false }
            )
        }

        // Dialog for Stream Quality
        if (showQualityDialog) {
            QualitySelectionDialog(
                qualityOptions = qualityOptions,
                onSelectQuality = {
                    onSelectQuality(it)
                    showQualityDialog = false
                },
                onDismiss = { showQualityDialog = false }
            )
        }
    }
}

@Composable
fun AudioSubtitleSelectionDialog(
    audioTracks: List<TrackOption>,
    subtitleTracks: List<TrackOption>,
    audioDelayMs: Int = 0,
    isPremium: Boolean = false,
    onAudioDelayChange: (Int) -> Unit = {},
    onSelectAudio: (TrackOption) -> Unit,
    onSelectSubtitle: (TrackOption?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val zeroFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            delay(100)
            zeroFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    fun handleAudioSyncChange(newDelay: Int) {
        if (!isPremium) {
            android.widget.Toast.makeText(
                context,
                "Sincronización de audio disponible solo en la versión Premium",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } else {
            onAudioDelayChange(newDelay)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text("Audio, Subtítulos y Sincronización", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // AUDIO SYNC CONTROL
                Text("SINCRONIZACIÓN DE AUDIO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OrangePrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Desfase actual: ${if (audioDelayMs > 0) "+$audioDelayMs ms" else "$audioDelayMs ms"}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FocusableCard(
                            onClick = { handleAudioSyncChange(audioDelayMs - 250) },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A40))
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text("-250ms", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    item {
                        FocusableCard(
                            onClick = { handleAudioSyncChange(audioDelayMs - 100) },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A40))
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text("-100ms", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    item {
                        FocusableCard(
                            onClick = { handleAudioSyncChange(0) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.focusRequester(zeroFocusRequester),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF33334A))
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text("0ms (Reset)", fontSize = 11.sp, color = OrangeGlow, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    item {
                        FocusableCard(
                            onClick = { handleAudioSyncChange(audioDelayMs + 100) },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A40))
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text("+100ms", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    item {
                        FocusableCard(
                            onClick = { handleAudioSyncChange(audioDelayMs + 250) },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A40))
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text("+250ms", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("PISTAS DE AUDIO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OrangePrimary)
                Spacer(modifier = Modifier.height(4.dp))
                if (audioTracks.isEmpty()) {
                    Text("Pista de Audio Predeterminada", fontSize = 12.sp, color = TextMuted)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        audioTracks.forEach { track ->
                            FocusableCard(
                                onClick = { onSelectAudio(track) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (track.isSelected) OrangePrimary else SurfaceDarkCard
                                )
                            ) {
                                Text(
                                    text = track.name,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("PISTAS DE SUBTÍTULOS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OrangePrimary)
                Spacer(modifier = Modifier.height(4.dp))

                FocusableCard(
                    onClick = { onSelectSubtitle(null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard)
                ) {
                    Text("Desactivado (Sin Subtítulos)", fontSize = 12.sp, color = Color.White, modifier = Modifier.padding(8.dp))
                }

                if (subtitleTracks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        subtitleTracks.forEach { track ->
                            FocusableCard(
                                onClick = { onSelectSubtitle(track) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (track.isSelected) OrangePrimary else SurfaceDarkCard
                                )
                            ) {
                                Text(
                                    text = track.name,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = OrangeGlow, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun QualitySelectionDialog(
    qualityOptions: List<QualityOption>,
    onSelectQuality: (QualityOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text("Calidad de Transmisión", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                if (qualityOptions.isEmpty()) {
                    Text("Calidad Automática (Tasa Adaptativa)", fontSize = 14.sp, color = Color.White)
                } else {
                    LazyColumn {
                        items(qualityOptions) { quality ->
                            FocusableCard(
                                onClick = { onSelectQuality(quality) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = androidx.compose.material3.CardDefaults.cardColors(
                                    containerColor = if (quality.isSelected) OrangePrimary else SurfaceDarkCard
                                )
                            ) {
                                Text(
                                    text = quality.label,
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextMuted)
            }
        }
    )
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).coerceAtLeast(0)
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
