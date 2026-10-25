package com.andrespar2.parsound.ui.screen.playlist

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.andrespar2.parsound.data.local.SongEntity
import com.andrespar2.parsound.ui.components.AmbientBackground
import com.andrespar2.parsound.ui.components.CoverArt
import com.andrespar2.parsound.ui.components.EmptyState
import com.andrespar2.parsound.ui.components.GlassIconButton
import com.andrespar2.parsound.ui.components.ParSoundTopBar
import com.andrespar2.parsound.ui.components.PillButton
import com.andrespar2.parsound.ui.components.SongOptionsSheet
import com.andrespar2.parsound.ui.components.SongRow
import com.andrespar2.parsound.ui.navigation.Routes
import com.andrespar2.parsound.util.songsLabel
import com.andrespar2.parsound.util.foldersLabel
import com.andrespar2.parsound.ui.theme.LocalGlass

@Composable
fun PlaylistDetailScreen(
    navController: NavController,
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val allSongs by viewModel.allSongs.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val current by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val glass = LocalGlass.current
    val context = LocalContext.current
    var optionsSong by remember { mutableStateOf<SongEntity?>(null) }
    var showAddSongs by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val cover = songs.firstOrNull { !it.artworkUri.isNullOrBlank() }?.artworkUri

    AmbientBackground(artworkUri = cover) {
        Column(modifier = Modifier.fillMaxSize()) {
            ParSoundTopBar(
                title = "Lista",
                onBack = { navController.navigateUp() },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "Opciones", tint = glass.textPrimary)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = glass.backgroundEnd
                        ) {
                            DropdownMenuItem(
                                text = { Text("Renombrar", color = glass.textPrimary) },
                                leadingIcon = { Icon(Icons.Default.Edit, null, tint = glass.textPrimary) },
                                onClick = {
                                    showMenu = false
                                    showRename = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Eliminar lista", color = glass.textPrimary) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = glass.textPrimary) },
                                onClick = {
                                    showMenu = false
                                    showDelete = true
                                }
                            )
                        }
                    }
                }
            )
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CoverArt(cover, 190.dp, cornerRadius = 28.dp)
                        Spacer(Modifier.height(18.dp))
                        Text(
                            playlist?.name ?: "Lista",
                            style = MaterialTheme.typography.headlineLarge,
                            color = glass.textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            songsLabel(songs.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = glass.textSecondary
                        )
                        Spacer(Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PillButton(
                                "Reproducir",
                                onClick = viewModel::playAll,
                                icon = Icons.Default.PlayArrow,
                                enabled = songs.isNotEmpty()
                            )
                            PillButton(
                                "Aleatorio",
                                onClick = viewModel::shuffleAll,
                                icon = Icons.Default.Shuffle,
                                primary = false,
                                enabled = songs.isNotEmpty()
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        PillButton(
                            "Añadir canciones",
                            onClick = { showAddSongs = true },
                            icon = Icons.Default.Add,
                            primary = false
                        )
                    }
                }
                if (songs.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.LibraryMusic,
                            title = "Lista vacía",
                            message = "Añade canciones de tu biblioteca para empezar esta lista.",
                            actionLabel = "Añadir canciones",
                            onAction = { showAddSongs = true }
                        )
                    }
                } else {
                    items(songs, key = { "pls-${it.id}" }) { song ->
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
            },
            onRemoveFromPlaylist = {
                viewModel.removeSong(song)
                optionsSong = null
            }
        )
    }

    if (showAddSongs) {
        AddSongsDialog(
            allSongs = allSongs,
            existingIds = songs.map { it.id }.toSet(),
            onDismiss = { showAddSongs = false },
            onAdd = { song ->
                viewModel.addSong(song) { added ->
                    Toast.makeText(
                        context,
                        if (added) "Añadida" else "Ya está en la lista",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    if (showRename) {
        var name by remember { mutableStateOf(playlist?.name.orEmpty()) }
        AlertDialog(
            onDismissRequest = { showRename = false },
            containerColor = glass.backgroundEnd,
            title = { Text("Renombrar lista", color = glass.textPrimary) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) viewModel.rename(name)
                    showRename = false
                }) { Text("Guardar", color = glass.accent) }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) {
                    Text("Cancelar", color = glass.textSecondary)
                }
            }
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            containerColor = glass.backgroundEnd,
            title = { Text("Eliminar lista", color = glass.textPrimary) },
            text = {
                Text(
                    "La lista se eliminará, pero las canciones seguirán en tu biblioteca.",
                    color = glass.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    viewModel.delete { navController.navigateUp() }
                }) { Text("Eliminar", color = glass.accent) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text("Cancelar", color = glass.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun AddSongsDialog(
    allSongs: List<SongEntity>,
    existingIds: Set<Long>,
    onDismiss: () -> Unit,
    onAdd: (SongEntity) -> Unit
) {
    val glass = LocalGlass.current
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, allSongs) {
        if (query.isBlank()) allSongs
        else allSongs.filter {
            it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = glass.backgroundEnd,
        title = { Text("Añadir canciones", color = glass.textPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Buscar canción", color = glass.textSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                    items(filtered, key = { "add-${it.id}" }) { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !existingIds.contains(song.id)) { onAdd(song) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CoverArt(song.artworkUri, 38.dp, cornerRadius = 10.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    song.title,
                                    color = glass.textPrimary,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    song.artist,
                                    color = glass.textSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                            }
                            Icon(
                                if (existingIds.contains(song.id)) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = null,
                                tint = if (existingIds.contains(song.id)) glass.accent else glass.textSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Listo", color = glass.accent) }
        }
    )
}
