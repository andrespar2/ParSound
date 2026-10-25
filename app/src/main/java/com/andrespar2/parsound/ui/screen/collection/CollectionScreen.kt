package com.andrespar2.parsound.ui.screen.collection

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.andrespar2.parsound.data.local.SongEntity
import com.andrespar2.parsound.ui.components.AmbientBackground
import com.andrespar2.parsound.ui.components.CoverArt
import com.andrespar2.parsound.ui.components.EmptyState
import com.andrespar2.parsound.ui.components.ParSoundTopBar
import com.andrespar2.parsound.ui.components.PillButton
import com.andrespar2.parsound.ui.components.SongOptionsSheet
import com.andrespar2.parsound.ui.components.SongRow
import com.andrespar2.parsound.ui.navigation.Routes
import com.andrespar2.parsound.ui.theme.LocalGlass

@Composable
fun CollectionScreen(
    navController: NavController,
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val songs by viewModel.songs.collectAsState()
    val title by viewModel.title.collectAsState()
    val subtitle by viewModel.subtitle.collectAsState()
    val artwork by viewModel.artworkUri.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val current by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    var optionsSong by remember { mutableStateOf<SongEntity?>(null) }
    val glass = LocalGlass.current
    val isArtist = viewModel.kind == "artist"

    AmbientBackground(artworkUri = artwork) {
        Column(modifier = Modifier.fillMaxSize()) {
            ParSoundTopBar(
                title = when (viewModel.kind) {
                    "album" -> "Álbum"
                    "artist" -> "Artista"
                    "genre" -> "Género"
                    else -> "Carpeta"
                },
                onBack = { navController.navigateUp() }
            )
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    HeaderBlock(
                        title = title,
                        subtitle = subtitle,
                        artwork = artwork,
                        circular = isArtist,
                        onPlay = viewModel::playAll,
                        onShuffle = viewModel::shuffleAll
                    )
                }
                if (songs.isEmpty()) {
                    item {
                        if (title.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = glass.accent)
                            }
                        } else {
                            EmptyState(
                                icon = Icons.Default.PlayArrow,
                                title = "Sin canciones",
                                message = "Esta colección no tiene pistas disponibles. Actualiza la biblioteca si has añadido archivos."
                            )
                        }
                    }
                } else {
                    items(songs, key = { "collect-${it.id}" }) { song ->
                        SongRow(
                            song = song,
                            isFavorite = favoriteIds.contains(song.id),
                            isCurrent = current?.id == song.id,
                            isPlaying = isPlaying,
                            onClick = { viewModel.play(song) },
                            onToggleFavorite = { viewModel.toggleFavorite(song) },
                            onMore = { optionsSong = song }
                        )
                    }
                }
            }
        }
    }

    optionsSong?.let { song ->
        SongOptionsSheet(
            song = song,
            onDismiss = { optionsSong = null },
            onGoToAlbum = { navController.navigate(Routes.album(song.albumKey)) },
            onGoToArtist = { navController.navigate(Routes.artist(song.artist)) },
            onShowLyrics = {
                viewModel.play(song)
                navController.navigate(Routes.PLAYER)
            }
        )
    }
}

@Composable
private fun HeaderBlock(
    title: String,
    subtitle: String,
    artwork: String?,
    circular: Boolean,
    onPlay: () -> Unit,
    onShuffle: () -> Unit
) {
    val glass = LocalGlass.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CoverArt(
            artworkUri = artwork,
            size = 190.dp,
            cornerRadius = 28.dp,
            circular = circular,
            contentDescription = title
        )
        Spacer(Modifier.height(18.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge,
            color = glass.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = glass.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton("Reproducir", onClick = onPlay, icon = Icons.Default.PlayArrow)
            PillButton(
                "Aleatorio",
                onClick = onShuffle,
                icon = Icons.Default.Shuffle,
                primary = false
            )
        }
        Spacer(Modifier.height(10.dp))
    }
}
