package com.andrespar2.parsound.ui.screen.queue

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.andrespar2.parsound.ui.components.AmbientBackground
import com.andrespar2.parsound.ui.components.CoverArt
import com.andrespar2.parsound.ui.components.EmptyState
import com.andrespar2.parsound.ui.components.ParSoundTopBar
import com.andrespar2.parsound.ui.components.PillButton
import com.andrespar2.parsound.util.songsLabel
import com.andrespar2.parsound.util.foldersLabel
import com.andrespar2.parsound.ui.theme.LocalGlass

@Composable
fun QueueScreen(
    navController: NavController,
    viewModel: QueueViewModel = hiltViewModel()
) {
    val queue by viewModel.queue.collectAsState()
    val current by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val glass = LocalGlass.current
    var confirmClear by remember { mutableStateOf(false) }

    AmbientBackground(showArtwork = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            ParSoundTopBar(
                title = "Cola de reproducción",
                subtitle = songsLabel(queue.size),
                onBack = { navController.navigateUp() },
                actions = {
                    if (queue.isNotEmpty()) {
                        TextButton(onClick = { confirmClear = true }) {
                            Text("Vaciar", color = glass.accent)
                        }
                    }
                }
            )
            if (queue.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.QueueMusic,
                    title = "La cola está vacía",
                    message = "Reproduce una canción o añade temas desde el menú de opciones."
                )
                return@Column
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PillButton(
                    if (isPlaying) "Pausar" else "Reproducir",
                    onClick = viewModel::toggle,
                    icon = Icons.Default.PlayArrow
                )
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 20.dp)) {
                itemsIndexed(queue) { index, song ->
                    val isCurrent = current?.id == song.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 3.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isCurrent) glass.accent.copy(alpha = 0.10f) else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable { viewModel.playAt(index) }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCurrent) glass.accent else glass.textSecondary,
                            modifier = Modifier.width(24.dp)
                        )
                        CoverArt(song.artworkUri, 44.dp, cornerRadius = 12.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                song.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isCurrent) glass.accent else glass.textPrimary,
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
                        IconButton(
                            onClick = { if (index > 0) viewModel.move(index, index - 1) },
                            enabled = index > 0,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                "Subir",
                                tint = glass.textSecondary.copy(alpha = if (index > 0) 1f else 0.3f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { if (index < queue.lastIndex) viewModel.move(index, index + 1) },
                            enabled = index < queue.lastIndex,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                "Bajar",
                                tint = glass.textSecondary.copy(alpha = if (index < queue.lastIndex) 1f else 0.3f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.remove(index) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                "Quitar de la cola",
                                tint = glass.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = glass.backgroundEnd,
            title = { Text("Vaciar la cola", color = glass.textPrimary) },
            text = {
                Text(
                    "Se quitarán todas las canciones de la cola y se detendrá la reproducción.",
                    color = glass.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    viewModel.clear()
                }) { Text("Vaciar", color = glass.accent) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("Cancelar", color = glass.textSecondary)
                }
            }
        )
    }
}
