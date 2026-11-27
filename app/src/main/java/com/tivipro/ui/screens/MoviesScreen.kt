package com.tivipro.ui.screens

import com.tivipro.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.CardDefaults
import com.tivipro.ui.theme.StatusSuccess
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.Dp
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Surface
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tivipro.model.VodMovie
import com.tivipro.ui.components.FocusableCard
import com.tivipro.ui.components.TiviProLogo
import com.tivipro.ui.components.MovieDetailDialog
import com.tivipro.ui.theme.DarkBackground
import com.tivipro.ui.theme.GoldAccent
import com.tivipro.ui.theme.OrangeGlow
import com.tivipro.ui.theme.OrangePrimary
import com.tivipro.ui.theme.SurfaceDarkCard
import com.tivipro.ui.theme.TextMuted
import com.tivipro.ui.theme.TextSecondary
import com.tivipro.ui.viewmodel.VodViewModel

@Composable
fun MoviesScreen(
    viewModel: VodViewModel,
    onMoviePlay: (VodMovie) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var filteredMovies by remember { mutableStateOf(uiState.vodMovies) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            debouncedQuery = ""
        } else {
            kotlinx.coroutines.delay(200L)
            debouncedQuery = searchQuery
        }
    }

    LaunchedEffect(uiState.vodMovies, debouncedQuery) {
        val query = debouncedQuery.trim()
        if (query.isBlank()) {
            filteredMovies = uiState.vodMovies
        } else {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                val result = uiState.vodMovies.filter {
                    it.name.contains(query, ignoreCase = true)
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    filteredMovies = result
                }
            }
        }
    }

    val gridState = rememberLazyGridState()
    val firstMovieFocusRequester = remember { FocusRequester() }
    val selectedCategoryFocusRequester = remember { FocusRequester() }
    var hasFocusedForCategory by remember(uiState.selectedVodCategory, uiState.isMovieFavoritesSelected, uiState.searchQuery) { mutableStateOf(false) }
    var focusedMovieId by remember { mutableStateOf<String?>(null) }
    var selectedActorForDialog by remember { mutableStateOf<com.tivipro.model.CastMember?>(null) }

    LaunchedEffect(filteredMovies, uiState.selectedVodCategory, uiState.isMovieFavoritesSelected, uiState.searchQuery) {
        if (!hasFocusedForCategory && filteredMovies.isNotEmpty()) {
            try {
                firstMovieFocusRequester.requestFocus()
                hasFocusedForCategory = true
            } catch (_: Exception) {}
        }
    }

    // Detail Dialog State
    uiState.selectedMovie?.let { selectedMovie ->
        MovieDetailDialog(
            movie = selectedMovie,
            onDismiss = { viewModel.selectMovie(null) },
            onPlayClick = { movie -> onMoviePlay(movie) },
            onToggleFavorite = { id -> viewModel.toggleMovieFavorite(id) }
        )
    }

    val activeMovie = remember(focusedMovieId, filteredMovies) {
        if (focusedMovieId == null) null
        else filteredMovies.firstOrNull { it.id == focusedMovieId }
    }

    var activeMovieDescription by remember(activeMovie?.id) { mutableStateOf(activeMovie?.description) }
    var activeMovieBackdrop by remember(activeMovie?.id) { mutableStateOf(activeMovie?.backdropUrl) }
    var activeMovieLogo by remember(activeMovie?.id) { mutableStateOf(activeMovie?.logoUrl) }

    LaunchedEffect(activeMovie?.id) {
        val current = activeMovie ?: return@LaunchedEffect
        activeMovieDescription = current.description
        activeMovieBackdrop = current.backdropUrl
        activeMovieLogo = current.logoUrl

        if (current.description.isNullOrBlank() || current.backdropUrl.isNullOrBlank() || current.logoUrl.isNullOrBlank()) {
            try {
                val enriched = com.tivipro.data.network.TmdbHelper.enrichMovie(current)
                if (!enriched.description.isNullOrBlank()) {
                    activeMovieDescription = enriched.description
                }
                if (!enriched.backdropUrl.isNullOrBlank()) {
                    activeMovieBackdrop = enriched.backdropUrl
                }
                if (!enriched.logoUrl.isNullOrBlank()) {
                    activeMovieLogo = enriched.logoUrl
                }
            } catch (_: Exception) {}
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        val screenWidth = maxWidth
        val isCompactWidth = screenWidth < 600.dp
        val isMobile = maxHeight < 450.dp || screenWidth < 600.dp
        val horizontalPadding = if (isCompactWidth) 12.dp else 24.dp
        val verticalPadding = if (maxHeight < 500.dp) 6.dp else 12.dp

        Box(modifier = Modifier.fillMaxSize()) {
            // Background Hero Poster / Backdrop Banner for Active Movie (Image 2 Style)
            val heroImageUrl = activeMovieBackdrop?.ifBlank { activeMovie?.posterUrl } ?: activeMovie?.posterUrl
            if (activeMovie != null && !heroImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = heroImageUrl,
                    contentDescription = activeMovie.name,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isMobile) 160.dp else 280.dp)
                )

                // Dark gradient overlay for readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isMobile) 180.dp else 300.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x66030816),
                                    Color(0xCC030816),
                                    DarkBackground
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding)
            ) {
                // Header Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBackClick, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Volver",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.Movie,
                            contentDescription = "CINE",
                            tint = OrangePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CINE",
                            fontSize = if (isCompactWidth) 15.sp else 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${filteredMovies.size} películas",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Real-time Search Box for CINE
                        Box(
                            modifier = Modifier
                                .width(if (isCompactWidth) 120.dp else 160.dp)
                                .height(32.dp)
                                .background(Color(0xFF1B1B2C), RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0xFF33334A), RoundedCornerShape(16.dp))
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = "Buscar película",
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(14.dp)
                                )

                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Buscar película...",
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
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        cursorBrush = SolidColor(OrangePrimary),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Limpiar",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        TiviProLogo(sizeScale = if (isCompactWidth) 0.65f else 0.75f, showGlow = false)
                    }
                }

                // Active Movie Hero Information Banner (Image 2 style)
                if (activeMovie != null) {
                    val releaseYear = formatReleaseYear(activeMovie.releaseDate)
                    val cleanTitle = cleanMovieTitle(activeMovie.name, releaseYear)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 2.dp)
                    ) {
                        // Transparent PNG Title Logo from TMDb in upper open space
                        if (!activeMovieLogo.isNullOrBlank()) {
                            AsyncImage(
                                model = activeMovieLogo,
                                contentDescription = cleanTitle,
                                contentScale = ContentScale.Fit,
                                alignment = Alignment.CenterStart,
                                modifier = Modifier
                                    .height(if (isMobile) 40.dp else 60.dp)
                                    .padding(bottom = 4.dp)
                            )
                        } else {
                            Text(
                                text = cleanTitle,
                                fontSize = if (isMobile) 18.sp else 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // 1. Recuadro Año
                            if (!releaseYear.isNullOrBlank()) {
                                Surface(
                                    color = Color(0xFF1F2238),
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B3E5C))
                                ) {
                                    Text(
                                        text = releaseYear,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // 2. Recuadro Género
                            val genresStr = activeMovie.genres?.ifBlank { null }
                            if (!genresStr.isNullOrBlank()) {
                                Surface(
                                    color = Color(0xFF1F2238),
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B3E5C))
                                ) {
                                    Text(
                                        text = genresStr,
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // 3. Recuadro Clasificación por Edad (Mismo color que los demás)
                            val certAge = com.tivipro.data.network.TmdbHelper.formatCertificationDisplay(activeMovie.certification)
                            Surface(
                                color = Color(0xFF1F2238),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B3E5C))
                            ) {
                                Text(
                                    text = certAge,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            if (activeMovie.rating != null && activeMovie.rating > 0.0) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Icon(Icons.Rounded.Star, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(12.dp))
                                    Text(
                                        text = String.format(Locale.US, "%.1f", activeMovie.rating),
                                        color = GoldAccent,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = activeMovieDescription?.ifBlank { "Sinopsis no disponible." } ?: "Sinopsis no disponible.",
                            color = TextMuted,
                            fontSize = 11.5.sp,
                            maxLines = if (isMobile) 2 else 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(if (isMobile) 0.95f else 0.85f)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                LaunchedEffect(Unit) {
                    if (uiState.vodMovies.isEmpty()) {
                        viewModel.loadData()
                    }
                }

            Spacer(modifier = Modifier.height(4.dp))

            // Category Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                item {
                    val isAllSelected = uiState.selectedVodCategory == null && !uiState.isMovieFavoritesSelected
                    FocusableCard(
                        onClick = { viewModel.selectVodCategory(null) },
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
                                text = "Todas las Películas",
                                color = if (isAllSelected) Color.White else TextSecondary,
                                fontSize = 10.5.sp,
                                fontWeight = if (isAllSelected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        }
                    }
                }

                item {
                    val isFavSelected = uiState.isMovieFavoritesSelected
                    FocusableCard(
                        onClick = { viewModel.selectMovieFavorites() },
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

                items(uiState.vodCategories, key = { it.id }) { cat ->
                    val isSelected = uiState.selectedVodCategory?.id == cat.id
                    FocusableCard(
                        onClick = { viewModel.selectVodCategory(cat) },
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
                                text = cat.name,
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = OrangePrimary)
                }
            } else if (filteredMovies.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No se encontraron películas.", color = TextMuted, fontSize = 13.sp)
                }
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = if (isMobile) 60.dp else 80.dp),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isMobile) 190.dp else 250.dp)
                ) {
                    itemsIndexed(filteredMovies, key = { _, m -> m.id }) { index, movie ->
                        val watchStatus = remember(movie.id) { viewModel.getMovieWatchStatus(movie.id) }
                        MoviePosterCard(
                            movie = movie,
                            watchStatus = watchStatus,
                            onClick = {
                                if (focusedMovieId == movie.id) {
                                    viewModel.selectMovie(movie)
                                } else {
                                    focusedMovieId = movie.id
                                }
                            },
                            isRecentlyClosedDetail = (uiState.selectedMovie == null && movie.id == focusedMovieId),
                            modifier = Modifier
                                .onFocusChanged { state ->
                                    if (state.isFocused) {
                                        focusedMovieId = movie.id
                                    }
                                }
                                .focusProperties {
                                    if (index == 0) {
                                        up = selectedCategoryFocusRequester
                                    }
                                }
                                .then(if (index == 0) Modifier.focusRequester(firstMovieFocusRequester) else Modifier)
                        )
                    }
                }
            }
        }

        val context = androidx.compose.ui.platform.LocalContext.current

        // Actor Detail Dialog Popup
        selectedActorForDialog?.let { actor ->
            com.tivipro.ui.components.ActorDetailDialog(
                castMember = actor,
                onDismiss = { selectedActorForDialog = null },
                onMediaClick = { title ->
                    val foundMovie = filteredMovies.firstOrNull { cleanMovieTitle(it.name).equals(cleanMovieTitle(title), ignoreCase = true) }
                    if (foundMovie != null) {
                        selectedActorForDialog = null
                        focusedMovieId = foundMovie.id
                        viewModel.selectMovie(foundMovie)
                    } else {
                        android.widget.Toast.makeText(context, "La película '$title' no está disponible en tu catálogo actual.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}
}

private fun cleanMovieTitle(rawName: String, releaseYear: String? = null): String {
    var title = rawName.trim()
    title = title.replace(Regex("""(?i)\s*[-–—]\s*[({\[]?\d{4}[)}\]]?\s*$"""), "")
    title = title.replace(Regex("""(?i)\s*[({\[]\d{4}[)}\]]\s*$"""), "")
    title = title.replace(Regex("""(?i)\s*[({\[]?\s*(?:4k|fhd|hd|sd|uhd|hdr|1080p|720p|castellano|español|latino|dual|multi|subtitulado|sub)[)}\]]?\s*$"""), "")

    if (!releaseYear.isNullOrBlank() && title.endsWith(releaseYear)) {
        title = title.substring(0, title.length - releaseYear.length).trim()
    }
    title = title.replace(Regex("""\s*[-–—:]\s*$"""), "")
    return title.trim()
}

fun formatReleaseYear(dateStr: String?): String? {
    if (dateStr.isNullOrBlank()) return null
    val trimmed = dateStr.trim()
    if (trimmed.length == 4 && trimmed.all { it.isDigit() }) {
        val yearNum = trimmed.toIntOrNull()
        if (yearNum != null && yearNum in 1900..2099) return trimmed
    }
    if (trimmed.length >= 4 && trimmed.take(4).all { it.isDigit() }) {
        val yearNum = trimmed.take(4).toIntOrNull()
        if (yearNum != null && yearNum in 1900..2099) return yearNum.toString()
    }
    return null
}

@Composable
fun StarRatingBar(
    rating: Double?,
    modifier: Modifier = Modifier,
    starSize: Dp = 10.dp,
    starColor: Color = GoldAccent,
    showNumericRating: Boolean = true
) {
    if (rating == null || rating <= 0.0) return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = null,
            tint = starColor,
            modifier = Modifier.size(starSize)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = String.format(Locale.US, "%.1f", rating),
            color = starColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun MoviePosterCard(
    movie: VodMovie,
    watchStatus: com.tivipro.data.repository.WatchStatus = com.tivipro.data.repository.WatchStatus.NONE,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isRecentlyClosedDetail: Boolean = false
) {
    val cardFocusRequester = remember(movie.id) { FocusRequester() }

    var posterUrlState by remember(movie.id, movie.posterUrl) { mutableStateOf(movie.posterUrl) }
    var isImageError by remember(movie.id, movie.posterUrl) { mutableStateOf(false) }

    LaunchedEffect(movie.id, posterUrlState, isImageError) {
        if (posterUrlState.isNullOrBlank() || isImageError) {
            try {
                val tmdbDetail = com.tivipro.data.network.TmdbHelper.fetchTmdbMovieDetail(movie.name)
                if (!tmdbDetail?.posterPath.isNullOrBlank()) {
                    posterUrlState = tmdbDetail?.posterPath
                    isImageError = false
                }
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(isRecentlyClosedDetail) {
        if (isRecentlyClosedDetail) {
            try {
                cardFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    FocusableCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.68f)
            .focusRequester(cardFocusRequester),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!posterUrlState.isNullOrBlank() && !isImageError) {
                AsyncImage(
                    model = posterUrlState,
                    contentDescription = movie.name,
                    contentScale = ContentScale.Crop,
                    onError = {
                        isImageError = true
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceDarkCard)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceDarkCard)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Movie,
                            contentDescription = movie.name,
                            tint = OrangePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = movie.name,
                            color = Color.White,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Watched Checkmark Badge ("Stick" on top-left: ✓ VISTO only if completed, ▶ VIENDO if in progress)
            if (watchStatus == com.tivipro.data.repository.WatchStatus.COMPLETED) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(StatusSuccess)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓ VISTO",
                        color = Color.White,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            } else if (watchStatus == com.tivipro.data.repository.WatchStatus.IN_PROGRESS) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(OrangePrimary)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "▶ VIENDO",
                        color = Color.White,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
