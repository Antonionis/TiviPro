package com.tivipro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.tivipro.model.Episode
import com.tivipro.model.Series
import com.tivipro.ui.screens.StarRatingBar
import com.tivipro.ui.screens.formatReleaseYear
import com.tivipro.ui.theme.DarkBackground
import com.tivipro.ui.theme.GoldAccent
import com.tivipro.ui.theme.OrangeGlow
import com.tivipro.ui.theme.OrangePrimary
import com.tivipro.ui.theme.StatusSuccess
import com.tivipro.ui.theme.SurfaceDark
import com.tivipro.ui.theme.SurfaceDarkCard
import com.tivipro.ui.theme.TextMuted
import com.tivipro.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun SeriesDetailDialog(
    series: Series,
    selectedSeasonNum: Int,
    episodes: List<Episode>,
    isLoading: Boolean = false,
    onSeasonSelect: (Int) -> Unit,
    onEpisodePlay: (Episode) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val seasonsList = remember(series.seasons, episodes) {
        val fromEpisodes = episodes.map { it.seasonNum }.distinct().sorted()
        if (fromEpisodes.isNotEmpty()) {
            fromEpisodes
        } else if (series.seasons.isNotEmpty()) {
            series.seasons.map { it.seasonNumber }
        } else {
            listOf(1)
        }
    }

    var activeSeasonNum by remember(selectedSeasonNum, seasonsList) {
        androidx.compose.runtime.mutableIntStateOf(if (seasonsList.contains(selectedSeasonNum)) selectedSeasonNum else seasonsList.firstOrNull() ?: 1)
    }

    val playButtonFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            playButtonFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    fun openSeriesTrailer() {
        val query = java.net.URLEncoder.encode("${series.name} trailer oficial", "UTF-8")
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.youtube.com/results?search_query=$query"))
        try {
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    var isGridView by remember { mutableStateOf(true) }
    var isSortOldestFirst by remember { mutableStateOf(true) }

    val filteredEpisodes = remember(episodes, activeSeasonNum, isSortOldestFirst) {
        val seasonEps = episodes.filter { it.seasonNum == activeSeasonNum }
            .ifEmpty { episodes }
        if (isSortOldestFirst) {
            seasonEps.sortedBy { it.episodeNum }
        } else {
            seasonEps.sortedByDescending { it.episodeNum }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF070A14))
        ) {
            val screenWidth = maxWidth
            val isCompactWidth = screenWidth < 600.dp
            val isCompactHeight = maxHeight < 500.dp
            val horizontalPadding = if (isCompactWidth) 10.dp else 24.dp
            val verticalPadding = if (isCompactHeight) 4.dp else 12.dp

            Box(modifier = Modifier.fillMaxSize()) {
                // Background Hero Poster / Backdrop Banner with Gradient Overlay
                val heroImageUrl = series.backdropUrl?.ifBlank { series.posterUrl } ?: series.posterUrl
                AsyncImage(
                    model = heroImageUrl,
                    contentDescription = series.name,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isCompactHeight) 120.dp else if (isCompactWidth) 220.dp else 320.dp)
                )

                // Dark gradient overlay for readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isCompactHeight) 130.dp else if (isCompactWidth) 240.dp else 340.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x99070A14),
                                    Color(0xE6070A14),
                                    Color(0xFF070A14)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                ) {
                    // Top Navigation Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (isCompactHeight) 2.dp else 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FocusableCard(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0x66000000))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Volver",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text("Series", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(if (isCompactHeight) 28.dp else 34.dp)
                                .clip(CircleShape)
                                .background(Color(0x66000000))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Cerrar",
                                tint = Color.White,
                                modifier = Modifier.size(if (isCompactHeight) 15.dp else 18.dp)
                            )
                        }
                    }

                    // Series Title & Info Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (isCompactHeight) 4.dp else 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            // Display Logo INSTEAD OF Text Title if available (Matching Movies)
                            if (!series.logoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = series.logoUrl,
                                    contentDescription = series.name,
                                    contentScale = ContentScale.Fit,
                                    alignment = Alignment.CenterStart,
                                    modifier = Modifier
                                        .height(if (isCompactHeight) 38.dp else 56.dp)
                                        .padding(bottom = 4.dp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // 1. Temporadas
                                Text(
                                    text = "${seasonsList.size} Temp.",
                                    fontSize = 10.5.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )

                                // 2. Recuadro Género
                                val genresStr = series.genres?.ifBlank { null }
                                if (!genresStr.isNullOrBlank()) {
                                    Surface(
                                        color = Color(0xFF1F2238),
                                        shape = RoundedCornerShape(4.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B3E5C))
                                    ) {
                                        Text(
                                            text = genresStr,
                                            color = TextSecondary,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // 3. Recuadro Año (Entre Género y Clasificación)
                                val releaseYear = formatReleaseYear(series.releaseDate)
                                if (!releaseYear.isNullOrBlank()) {
                                    Surface(
                                        color = Color(0xFF1F2238),
                                        shape = RoundedCornerShape(4.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B3E5C))
                                    ) {
                                        Text(
                                            text = releaseYear,
                                            color = Color.White,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // 4. Recuadro Clasificación por Edad (Mismo color que los demás)
                                val certAge = com.tivipro.data.network.TmdbHelper.formatCertificationDisplay(series.certification)
                                Surface(
                                    color = Color(0xFF1F2238),
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B3E5C))
                                ) {
                                    Text(
                                        text = certAge,
                                        color = Color.White,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }

                                // 5. Puntuación
                                if (series.rating != null && series.rating > 0.0) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Icon(Icons.Rounded.Star, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(11.dp))
                                        Text(
                                            text = String.format(Locale.US, "%.1f", series.rating),
                                            color = GoldAccent,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (!isCompactHeight) {
                                Spacer(modifier = Modifier.height(4.dp))

                                // Overview Description (Hidden or 1 line on compact landscape screens)
                                Text(
                                    text = series.overview?.ifBlank { "Sin descripción disponible." } ?: "Sin descripción disponible.",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(if (isCompactHeight) 4.dp else 8.dp))

                            // Primary Play, Trailer & Favorite Buttons
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val firstEp = filteredEpisodes.firstOrNull()
                                FocusableCard(
                                    onClick = {
                                        if (firstEp != null) {
                                            onEpisodePlay(firstEp)
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = StatusSuccess),
                                    modifier = Modifier.focusRequester(playButtonFocusRequester)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = if (isCompactHeight) 10.dp else 14.dp,
                                            vertical = if (isCompactHeight) 4.dp else 7.dp
                                        ),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(if (isCompactHeight) 14.dp else 16.dp))
                                        Text(
                                            text = "Reproducir T$selectedSeasonNum · E${firstEp?.episodeNum ?: 1}",
                                            color = Color.White,
                                            fontSize = if (isCompactHeight) 10.5.sp else 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                FocusableCard(
                                    onClick = { openSeriesTrailer() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2B3C))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = if (isCompactHeight) 8.dp else 12.dp,
                                            vertical = if (isCompactHeight) 4.dp else 7.dp
                                        ),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Rounded.Movie,
                                            contentDescription = "Tráiler",
                                            tint = OrangeGlow,
                                            modifier = Modifier.size(if (isCompactHeight) 13.dp else 15.dp)
                                        )
                                        Text(
                                            text = "VER TRÁILER",
                                            color = Color.White,
                                            fontSize = if (isCompactHeight) 10.5.sp else 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                FocusableCard(
                                    onClick = { onToggleFavorite(series.id) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2030))
                                ) {
                                    Box(
                                        modifier = Modifier.padding(
                                            horizontal = if (isCompactHeight) 8.dp else 12.dp,
                                            vertical = if (isCompactHeight) 4.dp else 7.dp
                                        ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (series.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                            contentDescription = "Favorito",
                                            tint = if (series.isFavorite) OrangePrimary else Color.White,
                                            modifier = Modifier.size(if (isCompactHeight) 14.dp else 16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Seasons Tabs & View Controls Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Season Tabs Row
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(seasonsList) { seasonNum ->
                                val isSelected = seasonNum == activeSeasonNum
                                val seasonEpCount = episodes.count { it.seasonNum == seasonNum }

                                FocusableCard(
                                    onClick = {
                                        activeSeasonNum = seasonNum
                                        onSeasonSelect(seasonNum)
                                    },
                                    modifier = Modifier.onFocusChanged { state ->
                                        if (state.isFocused) {
                                            activeSeasonNum = seasonNum
                                            onSeasonSelect(seasonNum)
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) OrangePrimary else Color(0xFF161726)
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) OrangeGlow else Color(0xFF282A3E),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Temporada $seasonNum" + if (seasonEpCount > 0) " ($seasonEpCount)" else "",
                                            color = if (isSelected) Color.White else TextSecondary,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Controls: Grid/List Toggle
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { isGridView = !isGridView },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF161726))
                            ) {
                                Icon(
                                    imageVector = if (isGridView) Icons.Rounded.GridView else Icons.Rounded.FormatListNumbered,
                                    contentDescription = "Modo de Vista",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Episodes Display Area
                    if (isLoading || (filteredEpisodes.isEmpty() && episodes.isEmpty())) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(color = OrangePrimary)
                        }
                    } else if (filteredEpisodes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay episodios disponibles para la Temporada $activeSeasonNum.",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    } else if (isGridView) {
                        // EPISODES 16:9 GRID (EXACTLY LIKE IMAGE 3)
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 130.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredEpisodes, key = { it.id }) { episode ->
                                EpisodeGridCard(
                                    episode = episode,
                                    seriesPosterUrl = series.posterUrl,
                                    onPlayClick = { onEpisodePlay(episode) }
                                )
                            }
                        }
                    } else {
                        // EPISODES LIST VIEW
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredEpisodes, key = { it.id }) { episode ->
                                EpisodeRowItem(
                                    episode = episode,
                                    onPlayClick = { onEpisodePlay(episode) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 16:9 Episode Card component matching Image 3 design
 */
@Composable
fun EpisodeGridCard(
    episode: Episode,
    seriesPosterUrl: String?,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    FocusableCard(
        onClick = onPlayClick,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard),
        elevation = if (isFocused) 6.dp else 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) OrangeGlow else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 16:9 Thumbnail Image Box with Episode Number Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                    .background(Color(0xFF121422))
            ) {
                AsyncImage(
                    model = episode.iconUrl?.ifBlank { seriesPosterUrl } ?: seriesPosterUrl,
                    contentDescription = episode.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Dark gradient overlay on bottom of thumbnail
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xAA000000))
                            )
                        )
                )

                // Top-Left Episode Number Badge (Matching Image 3)
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${episode.episodeNum}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Center Play Icon overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isFocused) OrangePrimary else Color(0x88000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Reproducir",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Episode Info Below Thumbnail
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = "E${episode.episodeNum}. ${episode.title}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = episode.duration ?: "Episodio ${episode.episodeNum}",
                    fontSize = 10.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EpisodeRowItem(
    episode: Episode,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onPlayClick() },
        color = SurfaceDarkCard
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(OrangePrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Reproducir Episodio",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "E${episode.episodeNum}. ${episode.title}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                episode.duration?.let { dur ->
                    Text(
                        text = dur,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
