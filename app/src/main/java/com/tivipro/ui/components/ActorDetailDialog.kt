package com.tivipro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.tivipro.data.network.TmdbHelper
import com.tivipro.model.CastMember
import com.tivipro.ui.theme.OrangeGlow
import com.tivipro.ui.theme.OrangePrimary
import com.tivipro.ui.theme.SurfaceDark
import com.tivipro.ui.theme.SurfaceDarkCard
import com.tivipro.ui.theme.TextMuted
import com.tivipro.ui.theme.TextSecondary

private fun formatDateDdMmYyyy(rawDate: String?): String? {
    if (rawDate.isNullOrBlank()) return null
    val parts = rawDate.trim().split("-")
    if (parts.size == 3 && parts[0].length == 4) {
        val year = parts[0]
        val month = parts[1]
        val day = parts[2]
        return "$day/$month/$year"
    }
    return rawDate
}

@Composable
fun ActorDetailDialog(
    castMember: CastMember,
    onDismiss: () -> Unit,
    onMediaClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var personDetail by remember { mutableStateOf<TmdbHelper.TmdbPersonDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val firstKnownForFocusRequester = remember { FocusRequester() }

    LaunchedEffect(castMember.name) {
        isLoading = true
        personDetail = TmdbHelper.fetchTmdbPersonDetail(castMember.name)
        isLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.88f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(20.dp)),
            color = SurfaceDark
        ) {
            BoxWithConstraints {
                val isCompactHeight = maxHeight < 500.dp
                val dialogPadding = if (isCompactHeight) 12.dp else 20.dp

                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(dialogPadding)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 8.dp else 12.dp)
                    ) {
                        // Header Info: Photo + Name + Birth & Department
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val photoUrl = personDetail?.profilePath ?: castMember.profileUrl
                            if (!photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = castMember.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .width(if (isCompactHeight) 64.dp else 85.dp)
                                        .height(if (isCompactHeight) 88.dp else 115.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceDarkCard)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .width(if (isCompactHeight) 64.dp else 85.dp)
                                        .height(if (isCompactHeight) 88.dp else 115.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF28283C)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = personDetail?.name ?: castMember.name,
                                    fontSize = if (isCompactHeight) 17.sp else 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                castMember.role?.let { role ->
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Papel / Personaje: $role",
                                        color = OrangeGlow,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                val formattedBirth = formatDateDdMmYyyy(personDetail?.birthday)
                                val place = personDetail?.placeOfBirth
                                val birthInfo = listOfNotNull(
                                    if (!formattedBirth.isNullOrBlank()) "Nacimiento: $formattedBirth" else null,
                                    place?.ifBlank { null }
                                ).joinToString(" • ")

                                if (birthInfo.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = birthInfo,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Biography Section
                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = OrangePrimary)
                            }
                        } else {
                            val bioText = personDetail?.biography
                            if (!bioText.isNullOrBlank()) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Biografía",
                                        color = Color.White,
                                        fontSize = if (isCompactHeight) 13.sp else 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = bioText,
                                        color = TextMuted,
                                        fontSize = 11.5.sp,
                                        lineHeight = 16.sp,
                                        maxLines = if (isCompactHeight) 4 else 8,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.focusProperties { down = firstKnownForFocusRequester }
                                    )
                                }
                            }

                            // Known For Section (Conocido por - Image 2 style)
                            val movies = personDetail?.knownForMovies ?: emptyList()
                            if (movies.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Conocido por",
                                        color = Color.White,
                                        fontSize = if (isCompactHeight) 14.sp else 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        itemsIndexed(movies) { index, m ->
                                            FocusableCard(
                                                onClick = {
                                                    onMediaClick?.invoke(m.title)
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF161726)),
                                                modifier = Modifier
                                                    .width(95.dp)
                                                    .then(if (index == 0) Modifier.focusRequester(firstKnownForFocusRequester) else Modifier)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(4.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    if (!m.posterPath.isNullOrBlank()) {
                                                        AsyncImage(
                                                            model = m.posterPath,
                                                            contentDescription = m.title,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .aspectRatio(0.68f)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(SurfaceDarkCard)
                                                        )
                                                    } else {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .aspectRatio(0.68f)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(SurfaceDarkCard),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = m.title.take(1),
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = m.title,
                                                        color = Color.White,
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                        maxLines = 2,
                                                        lineHeight = 12.sp,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Close Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
