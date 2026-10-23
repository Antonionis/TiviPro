package com.tivipro.ui.components

import android.content.Intent
import android.net.Uri
import java.net.URLEncoder
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.tivipro.model.VodMovie
import com.tivipro.ui.screens.StarRatingBar
import com.tivipro.ui.screens.formatReleaseYear
import com.tivipro.ui.theme.OrangeGlow
import com.tivipro.ui.theme.OrangePrimary
import com.tivipro.ui.theme.SurfaceDark
import com.tivipro.ui.theme.SurfaceDarkCard
import com.tivipro.ui.theme.TextMuted

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

private fun formatDurationDisplay(mins: Int?): String? {
    if (mins == null || mins <= 0) return null
    val h = mins / 60
    val m = mins % 60
    return if (h > 0) {
        if (m > 0) "${h}h ${m}m" else "${h}h"
    } else {
        "${m}m"
    }
}

@Composable
fun MovieDetailDialog(
    movie: VodMovie,
    onDismiss: () -> Unit,
    onPlayClick: (VodMovie) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playButtonFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            playButtonFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    fun openTrailer() {
        val trailerUrl = movie.trailerUrl
        val intent = if (!trailerUrl.isNullOrBlank()) {
            Intent(Intent.ACTION_VIEW, Uri.parse(trailerUrl))
        } else {
            val query = URLEncoder.encode("${cleanMovieTitle(movie.name)} trailer", "UTF-8")
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$query"))
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(20.dp)),
            color = SurfaceDark
        ) {
            BoxWithConstraints {
                val isCompact = maxHeight < 500.dp
                val posterWidth = if (isCompact) 140.dp else 190.dp
                val posterHeight = if (isCompact) 210.dp else 285.dp
                val dialogPadding = if (isCompact) 14.dp else 20.dp

                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(dialogPadding)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Top Section: Poster on Left, Details & Overview & Buttons on Right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            // Big Poster Image on Left
                            AsyncImage(
                                model = movie.posterUrl,
                                contentDescription = movie.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(posterWidth)
                                    .height(posterHeight)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SurfaceDarkCard)
                            )

                            // Details Column on Right
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                // Title with Single Release Year
                                val cleanTitle = cleanMovieTitle(movie.name)
                                val releaseYear = formatReleaseYear(movie.releaseDate)
                                val titleWithYear = if (!releaseYear.isNullOrBlank()) "$cleanTitle ($releaseYear)" else cleanTitle

                                Text(
                                    text = titleWithYear,
                                    fontSize = if (isCompact) 18.sp else 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Subtitle: Genero y Duracion
                                val durationStr = formatDurationDisplay(movie.durationMinutes)
                                val genreAndDuration = listOfNotNull(
                                    movie.genres?.ifBlank { null },
                                    durationStr
                                ).joinToString(" • ")

                                if (genreAndDuration.isNotBlank()) {
                                    Text(
                                        text = genreAndDuration,
                                        color = OrangeGlow,
                                        fontSize = if (isCompact) 12.sp else 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }

                                // Star Rating Bar
                                StarRatingBar(rating = movie.rating, starSize = 14.dp, showNumericRating = true)

                                Spacer(modifier = Modifier.height(12.dp))

                                // Action Buttons Row: Reproducir + Ver Tráiler + Favorito
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Play Button
                                    Button(
                                        onClick = {
                                            onPlayClick(movie)
                                            onDismiss()
                                        },
                                        modifier = Modifier
                                            .height(if (isCompact) 40.dp else 46.dp)
                                            .focusRequester(playButtonFocusRequester),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.PlayArrow,
                                                contentDescription = "Reproducir",
                                                tint = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "REPRODUCIR",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = if (isCompact) 12.sp else 14.sp
                                            )
                                        }
                                    }

                                    // Trailer Button (Opens YouTube)
                                    OutlinedButton(
                                        onClick = { openTrailer() },
                                        modifier = Modifier.height(if (isCompact) 40.dp else 46.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Movie,
                                                contentDescription = "Tráiler",
                                                tint = OrangeGlow
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "VER TRÁILER",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = if (isCompact) 12.sp else 14.sp
                                            )
                                        }
                                    }

                                    // Favorite Button
                                    IconButton(
                                        onClick = { onToggleFavorite(movie.id) },
                                        modifier = Modifier
                                            .size(if (isCompact) 40.dp else 46.dp)
                                            .background(SurfaceDarkCard, RoundedCornerShape(12.dp))
                                    ) {
                                        Icon(
                                            imageVector = if (movie.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                            contentDescription = "Favorito",
                                            tint = if (movie.isFavorite) OrangePrimary else Color.White
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Description / Overview in Right Column
                                Text(
                                    text = "Vista general",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                var showFullOverviewDialog by remember { mutableStateOf(false) }

                                if (showFullOverviewDialog) {
                                    FullOverviewDialog(
                                        title = cleanMovieTitle(movie.name),
                                        overview = movie.description,
                                        onDismiss = { showFullOverviewDialog = false }
                                    )
                                }

                                Text(
                                    text = movie.description ?: "No hay descripción disponible para esta película.",
                                    color = TextMuted,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable { showFullOverviewDialog = true }
                                )
                            }
                        }

                        // Bottom Section: Reparto Principal (Navigable / Focusable)
                        if (movie.cast.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = "Reparto principal",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(movie.cast) { member ->
                                    FocusableCard(
                                        onClick = {},
                                        modifier = Modifier.width(140.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp)
                                        ) {
                                            Text(
                                                text = member.name,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            member.role?.let { role ->
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = role,
                                                    color = TextMuted,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Close Button Top Right
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FullOverviewDialog(
    title: String,
    overview: String?,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp)),
            color = SurfaceDark
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .heightIn(max = 350.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = overview ?: "No hay descripción disponible para esta película.",
                        color = TextMuted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("CERRAR", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}