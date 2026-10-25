package com.andrespar2.parsound.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrespar2.parsound.data.local.PlaylistSummary
import com.andrespar2.parsound.data.local.SongEntity
import com.andrespar2.parsound.data.repo.MusicRepository
import com.andrespar2.parsound.player.PlayerManager
import com.andrespar2.parsound.util.songsLabel
import com.andrespar2.parsound.util.foldersLabel
import com.andrespar2.parsound.ui.theme.LocalGlass
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SongActionsViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playerManager: PlayerManager
) : ViewModel() {
    val playlists: StateFlow<List<PlaylistSummary>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteIds: StateFlow<Set<Long>> = repository.favoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggleFavorite(song: SongEntity, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val newValue = !favoriteIds.value.contains(song.id)
            repository.setFavorite(song.id, newValue)
            playerManager.refreshCurrentSong()
            onDone(newValue)
        }
    }

    fun playNext(song: SongEntity) = playerManager.playNext(song)

    fun addToQueue(song: SongEntity) = playerManager.addToQueue(song)

    fun addToPlaylist(playlistId: Long, song: SongEntity, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val added = repository.addToPlaylist(playlistId, song.id)
            onDone(added)
        }
    }

    fun createPlaylist(name: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            onCreated(repository.createPlaylist(name))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsSheet(
    song: SongEntity,
    onDismiss: () -> Unit,
    onGoToAlbum: () -> Unit,
    onGoToArtist: () -> Unit,
    onShowLyrics: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    viewModel: SongActionsViewModel = hiltViewModel()
) {
    val glass = LocalGlass.current
    val context = LocalContext.current
    val playlists by viewModel.playlists.collectAsState()
    val favorites by viewModel.favoriteIds.collectAsState()
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    val isFavorite = favorites.contains(song.id)

    if (showCreatePlaylist) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylist = false },
            onCreate = { name ->
                viewModel.createPlaylist(name) { id ->
                    viewModel.addToPlaylist(id, song) {
                        Toast.makeText(context, "Añadida a la lista", Toast.LENGTH_SHORT).show()
                    }
                }
                showCreatePlaylist = false
                showPlaylistPicker = false
                onDismiss()
            }
        )
    }

    if (showPlaylistPicker) {
        AddToPlaylistDialog(
            song = song,
            playlists = playlists,
            onDismiss = { showPlaylistPicker = false },
            onCreateNew = { showCreatePlaylist = true },
            onPick = { playlist ->
                viewModel.addToPlaylist(playlist.id, song) { added ->
                    Toast.makeText(
                        context,
                        if (added) "Añadida a ${playlist.name}" else "Ya estaba en ${playlist.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                showPlaylistPicker = false
                onDismiss()
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = glass.backgroundEnd,
        contentColor = glass.textPrimary,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(glass.textSecondary.copy(alpha = 0.35f), CircleShape)
            )
        }
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverArt(song.artworkUri, 52.dp, cornerRadius = 14.dp)
                Spacer(Modifier.width(12.dp))
                Column {
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
                        maxLines = 1
                    )
                }
            }
            SheetAction(Icons.Default.PlaylistAdd, "Reproducir a continuación") {
                viewModel.playNext(song)
                Toast.makeText(context, "Se reproducirá a continuación", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
            SheetAction(Icons.Default.QueueMusic, "Añadir a la cola") {
                viewModel.addToQueue(song)
                Toast.makeText(context, "Añadida a la cola", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
            SheetAction(Icons.Default.Add, "Añadir a una lista") {
                showPlaylistPicker = true
            }
            SheetAction(
                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                if (isFavorite) "Quitar de favoritos" else "Añadir a favoritos"
            ) {
                viewModel.toggleFavorite(song)
                onDismiss()
            }
            SheetAction(Icons.Default.Album, "Ir al álbum", onClick = {
                onDismiss()
                onGoToAlbum()
            })
            SheetAction(Icons.Default.Person, "Ir al artista", onClick = {
                onDismiss()
                onGoToArtist()
            })
            SheetAction(Icons.Default.Subject, "Ver letra", onClick = {
                onDismiss()
                onShowLyrics()
            })
            if (onRemoveFromPlaylist != null) {
                SheetAction(Icons.Default.Delete, "Quitar de esta lista", onClick = onRemoveFromPlaylist)
            }
        }
    }
}

@Composable
private fun SheetAction(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    val glass = LocalGlass.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = glass.textPrimary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = glass.textPrimary)
    }
}

@Composable
fun AddToPlaylistDialog(
    song: SongEntity,
    playlists: List<PlaylistSummary>,
    onDismiss: () -> Unit,
    onCreateNew: () -> Unit,
    onPick: (PlaylistSummary) -> Unit
) {
    val glass = LocalGlass.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = glass.backgroundEnd,
        title = { Text("Añadir a una lista", color = glass.textPrimary) },
        text = {
            Column(modifier = Modifier.heightIn(max = 380.dp)) {
                if (playlists.isEmpty()) {
                    Text(
                        "Todavía no tienes listas. Crea la primera.",
                        color = glass.textSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn {
                        items(playlists, key = { it.id }) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onPick(playlist) }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LibraryMusic,
                                    contentDescription = null,
                                    tint = glass.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(playlist.name, color = glass.textPrimary)
                                    Text(
                                        songsLabel(playlist.songCount),
                                        color = glass.textSecondary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onCreateNew() }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = glass.accent)
                    Spacer(Modifier.width(12.dp))
                    Text("Nueva lista", color = glass.accent, fontWeight = FontWeight.Medium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar", color = glass.accent) }
        }
    )
}

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    val glass = LocalGlass.current
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = glass.backgroundEnd,
        title = { Text("Nueva lista", color = glass.textPrimary) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre de la lista") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name) },
                enabled = name.isNotBlank()
            ) { Text("Crear", color = glass.accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = glass.textSecondary) }
        }
    )
}
