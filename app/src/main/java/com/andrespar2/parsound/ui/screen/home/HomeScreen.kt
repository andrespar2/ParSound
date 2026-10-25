package com.andrespar2.parsound.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.andrespar2.parsound.data.local.SongEntity
import com.andrespar2.parsound.ui.components.AmbientBackground
import com.andrespar2.parsound.ui.components.AlbumCard
import com.andrespar2.parsound.ui.components.ArtistCard
import com.andrespar2.parsound.ui.components.CoverArt
import com.andrespar2.parsound.ui.components.EmptyState
import com.andrespar2.parsound.ui.components.GlassCard
import com.andrespar2.parsound.ui.components.GlassIconButton
import com.andrespar2.parsound.ui.components.PillButton
import com.andrespar2.parsound.ui.components.PlaylistCard
import com.andrespar2.parsound.ui.components.SectionHeader
import com.andrespar2.parsound.ui.components.SongOptionsSheet
import com.andrespar2.parsound.ui.components.glassModifier
import com.andrespar2.parsound.ui.navigation.Routes
import com.andrespar2.parsound.util.songsLabel
import com.andrespar2.parsound.util.foldersLabel
import com.andrespar2.parsound.ui.theme.LocalGlass
import com.andrespar2.parsound.util.formatDate
import java.util.Calendar

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val songs by viewModel.songs.collectAsState()
    val recent by viewModel.recent.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val current by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val scanState by viewModel.scanState.collectAsState()
    val lastScan by viewModel.lastScan.collectAsState()
    var optionsSong by remember { mutableStateOf<SongEntity?>(null) }
    val glass = LocalGlass.current
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 6..11 -> "Buenos días"
        in 12..19 -> "Buenas tardes"
        else -> "Buenas noches"
    }

    AmbientBackground(artworkUri = current?.artworkUri) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 14.dp, top = 14.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            greeting,
                            style = MaterialTheme.typography.bodyMedium,
                            color = glass.textSecondary
                        )
                        Text(
                            "ParSound",
                            style = MaterialTheme.typography.displayMedium,
                            color = glass.textPrimary
                        )
                    }
                    GlassIconButton(Icons.Default.Search, "Buscar") {
                        navController.navigate(Routes.SEARCH)
                    }
                    Spacer(Modifier.width(6.dp))
                    GlassIconButton(Icons.Default.Settings, "Ajustes") {
                        navController.navigate(Routes.SETTINGS)
                    }
                }
            }

            if (scanState is com.andrespar2.parsound.scanner.ScanState.Scanning) {
                item {
                    val scanning = scanState as com.andrespar2.parsound.scanner.ScanState.Scanning
                    GlassCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = glass.accent
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Analizando ${scanning.folder}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = glass.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${scanning.filesFound} archivos encontrados",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            if (current != null) {
                item {
                    ContinueCard(
                        song = current!!,
                        isPlaying = isPlaying,
                        onOpen = { navController.navigate(Routes.PLAYER) },
                        onToggle = { viewModel.togglePlayback() }
                    )
                }
            }

            if (songs.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        strong = true,
                        padding = 20.dp
                    ) {
                        Text(
                            "Tu música, con tu propio cristal",
                            style = MaterialTheme.typography.headlineMedium,
                            color = glass.textPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Añade las carpetas donde guardas tu música. ParSound leerá títulos, portadas, artistas y letras sin subir nada a Internet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = glass.textSecondary
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PillButton("Añadir carpeta", onClick = { navController.navigate(Routes.FOLDERS) })
                            PillButton(
                                "Importar del dispositivo",
                                onClick = { navController.navigate(Routes.FOLDERS) },
                                primary = false
                            )
                        }
                        if (folders.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Carpetas configuradas: ${folders.size}. Último análisis: ${formatDate(lastScan)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary
                            )
                        }
                    }
                }
            }

            if (recent.isNotEmpty()) {
                item { SectionHeader("Escuchado recientemente") }
                item {
                    LazyRow(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recent.take(12), key = { "recent-${it.id}" }) { song ->
                            RecentCard(
                                song = song,
                                onClick = { viewModel.play(song, recent) },
                                onMore = { optionsSong = song }
                            )
                        }
                    }
                }
            }

            if (favorites.isNotEmpty()) {
                item { SectionHeader("Favoritas", actionLabel = "Ver todas", onAction = { navController.navigate(Routes.LIBRARY) }) }
                item {
                    LazyRow(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(favorites.take(12), key = { "fav-${it.id}" }) { song ->
                            RecentCard(
                                song = song,
                                onClick = { viewModel.play(song, favorites) },
                                onMore = { optionsSong = song }
                            )
                        }
                    }
                }
            }

            if (albums.isNotEmpty()) {
                item {
                    SectionHeader("Álbumes", actionLabel = "Biblioteca", onAction = {
                        navController.navigate(Routes.LIBRARY)
                    })
                }
                item {
                    LazyRow(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(albums.take(16), key = { "album-${it.albumKey}" }) { album ->
                            AlbumCard(album, onClick = { navController.navigate(Routes.album(album.albumKey)) })
                        }
                    }
                }
            }

            if (artists.isNotEmpty()) {
                item { SectionHeader("Artistas") }
                item {
                    LazyRow(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(artists.take(16), key = { "artist-${it.artist}" }) { artist ->
                            ArtistCard(artist, onClick = { navController.navigate(Routes.artist(artist.artist)) })
                        }
                    }
                }
            }

            if (playlists.isNotEmpty()) {
                item {
                    SectionHeader("Tus listas", actionLabel = "Gestionar", onAction = {
                        navController.navigate(Routes.PLAYLISTS)
                    })
                }
                items(playlists.take(5), key = { "playlist-${it.id}" }) { playlist ->
                    Box(modifier = Modifier.padding(horizontal = 10.dp)) {
                        PlaylistCard(playlist, onClick = { navController.navigate(Routes.playlist(playlist.id)) })
                    }
                }
            }

            if (songs.isNotEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        strong = false
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LibraryMusic,
                                contentDescription = null,
                                tint = glass.accent,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${songsLabel(songs.size)} · ${foldersLabel(folders.size)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = glass.textPrimary
                                )
                                Text(
                                    "Último análisis: ${formatDate(lastScan)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textSecondary
                                )
                            }
                            GlassIconButton(Icons.Default.ArrowForward, "Abrir biblioteca") {
                                navController.navigate(Routes.LIBRARY)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PillButton("Reproducir todo", onClick = { viewModel.playAll(songs) }, icon = Icons.Default.PlayArrow)
                            PillButton(
                                "Aleatorio",
                                onClick = { viewModel.shuffleAll(songs) },
                                icon = Icons.Default.Shuffle,
                                primary = false
                            )
                        }
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
                viewModel.play(song, songs.ifEmpty { listOf(song) })
                navController.navigate(Routes.PLAYER)
            }
        )
    }
}

@Composable
private fun ContinueCard(
    song: SongEntity,
    isPlaying: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit
) {
    val glass = LocalGlass.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .glassModifier(24.dp, strong = true)
            .clickable(onClick = onOpen)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverArt(song.artworkUri, 68.dp, cornerRadius = 18.dp)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "CONTINUAR ESCUCHANDO",
                style = MaterialTheme.typography.labelSmall,
                color = glass.accent,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(3.dp))
            Text(
                song.title,
                style = MaterialTheme.typography.titleMedium,
                color = glass.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = glass.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        GlassIconButton(
            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            if (isPlaying) "Pausar" else "Reproducir",
            onClick = onToggle,
            tint = glass.accent,
            size = 48.dp,
            iconSize = 26.dp
        )
    }
}

@Composable
fun RecentCard(
    song: SongEntity,
    onClick: () -> Unit,
    onMore: () -> Unit
) {
    val glass = LocalGlass.current
    Column(
        modifier = Modifier
            .width(124.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Box {
            CoverArt(song.artworkUri, 124.dp, cornerRadius = 20.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(30.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    .clickable(onClick = onMore),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = "Opciones",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            song.title,
            style = MaterialTheme.typography.titleSmall,
            color = glass.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            song.artist,
            style = MaterialTheme.typography.bodySmall,
            color = glass.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
