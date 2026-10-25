package com.andrespar2.parsound.ui.screen.playlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.andrespar2.parsound.data.local.PlaylistSummary
import com.andrespar2.parsound.ui.components.AmbientBackground
import com.andrespar2.parsound.ui.components.CreatePlaylistDialog
import com.andrespar2.parsound.ui.components.EmptyState
import com.andrespar2.parsound.ui.components.ParSoundTopBar
import com.andrespar2.parsound.ui.components.PillButton
import com.andrespar2.parsound.ui.components.PlaylistCard
import com.andrespar2.parsound.ui.navigation.Routes
import com.andrespar2.parsound.ui.theme.LocalGlass

@Composable
fun PlaylistsScreen(
    navController: NavController,
    viewModel: PlaylistsViewModel = hiltViewModel()
) {
    val playlists by viewModel.playlists.collectAsState()
    val glass = LocalGlass.current
    var showCreate by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<PlaylistSummary?>(null) }

    if (showCreate) {
        CreatePlaylistDialog(
            onDismiss = { showCreate = false },
            onCreate = { name ->
                viewModel.create(name) { id ->
                    showCreate = false
                    navController.navigate(Routes.playlist(id))
                }
            }
        )
    }
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            containerColor = glass.backgroundEnd,
            title = { Text("Eliminar lista", color = glass.textPrimary) },
            text = {
                Text(
                    "¿Eliminar «${toDelete?.name}»? Las canciones seguirán en tu biblioteca.",
                    color = glass.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    toDelete?.let { viewModel.delete(it.id) }
                    toDelete = null
                }) { Text("Eliminar", color = glass.accent) }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) {
                    Text("Cancelar", color = glass.textSecondary)
                }
            }
        )
    }

    AmbientBackground(showArtwork = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            ParSoundTopBar(
                title = "Listas de reproducción",
                subtitle = "${playlists.size} listas",
                onBack = { navController.navigateUp() },
                actions = {
                    IconButton(onClick = { showCreate = true }) {
                        Icon(Icons.Default.Add, "Nueva lista", tint = glass.textPrimary)
                    }
                }
            )
            if (playlists.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.LibraryMusic,
                    title = "Crea tu primera lista",
                    message = "Organiza tus canciones por momentos, géneros o lo que prefieras.",
                    actionLabel = "Nueva lista",
                    onAction = { showCreate = true }
                )
                return@Column
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 20.dp)) {
                item {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        PillButton("Nueva lista", onClick = { showCreate = true }, icon = Icons.Default.Add)
                    }
                }
                items(playlists, key = { it.id }) { playlist ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            PlaylistCard(playlist, onClick = { navController.navigate(Routes.playlist(playlist.id)) })
                        }
                        IconButton(onClick = { toDelete = playlist }) {
                            Icon(Icons.Default.Delete, "Eliminar", tint = glass.textSecondary)
                        }
                    }
                }
            }
        }
    }
}
