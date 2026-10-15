package com.goaltv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goaltv.data.repository.DataRepository
import com.goaltv.model.LiveChannel
import com.goaltv.model.Series
import com.goaltv.model.VodMovie
import com.goaltv.ui.theme.DarkBackground
import com.goaltv.ui.theme.OrangePrimary
import com.goaltv.ui.theme.SurfaceDarkCard
import com.goaltv.ui.theme.TextMuted
import com.goaltv.ui.theme.TextSecondary

@Composable
fun FavoritesScreen(
    repository: DataRepository,
    onChannelPlay: (LiveChannel) -> Unit,
    onMoviePlay: (VodMovie) -> Unit,
    onSeriesSelect: (Series) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Channels, 1 = Movies, 2 = Series

    val favChannels = repository.getFavoriteChannels()
    val favMovies = repository.getFavoriteMovies()
    val favSeries = repository.getFavoriteSeries()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(12.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = "Mis Favoritos",
                tint = OrangePrimary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Mis Favoritos",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Category Filter Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            FilterChip(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                label = { Text("Canales (${favChannels.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OrangePrimary,
                    selectedLabelColor = Color.White,
                    containerColor = SurfaceDarkCard,
                    labelColor = TextSecondary
                )
            )

            FilterChip(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                label = { Text("Películas (${favMovies.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OrangePrimary,
                    selectedLabelColor = Color.White,
                    containerColor = SurfaceDarkCard,
                    labelColor = TextSecondary
                )
            )

            FilterChip(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                label = { Text("Series (${favSeries.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OrangePrimary,
                    selectedLabelColor = Color.White,
                    containerColor = SurfaceDarkCard,
                    labelColor = TextSecondary
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
            0 -> {
                if (favChannels.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Aún no hay canales favoritos guardados.", color = TextMuted)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(favChannels, key = { it.id }) { channel ->
                            ChannelListItem(
                                channel = channel,
                                isSelected = false,
                                onSelect = { onChannelPlay(channel) },
                                onPlay = { onChannelPlay(channel) },
                                onToggleFavorite = { repository.toggleFavoriteChannel(channel.id) }
                            )
                        }
                    }
                }
            }
            1 -> {
                if (favMovies.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Aún no hay películas favoritas guardadas.", color = TextMuted)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(favMovies, key = { it.id }) { movie ->
                            MoviePosterCard(
                                movie = movie,
                                onClick = { onMoviePlay(movie) }
                            )
                        }
                    }
                }
            }
            2 -> {
                if (favSeries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Aún no hay series favoritas guardadas.", color = TextMuted)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(favSeries, key = { it.id }) { series ->
                            SeriesCard(
                                series = series,
                                onClick = { onSeriesSelect(series) }
                            )
                        }
                    }
                }
            }
        }
    }
}
