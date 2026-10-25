package com.andrespar2.parsound.ui.screen.search

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.andrespar2.parsound.data.local.SongEntity
import com.andrespar2.parsound.ui.components.AmbientBackground
import com.andrespar2.parsound.ui.components.CoverArt
import com.andrespar2.parsound.ui.components.EmptyState
import com.andrespar2.parsound.ui.components.GlassIconButton
import com.andrespar2.parsound.ui.components.SongOptionsSheet
import com.andrespar2.parsound.ui.components.SongRow
import com.andrespar2.parsound.ui.components.glassModifier
import com.andrespar2.parsound.ui.navigation.Routes
import com.andrespar2.parsound.util.songsLabel
import com.andrespar2.parsound.util.foldersLabel
import com.andrespar2.parsound.ui.theme.LocalGlass

@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val albums by viewModel.albumResults.collectAsState()
    val artists by viewModel.artistResults.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val current by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val glass = LocalGlass.current
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var optionsSong by remember { mutableStateOf<SongEntity?>(null) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AmbientBackground(showArtwork = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") {
                    navController.navigateUp()
                }
                Spacer(Modifier.width(10.dp))
                TextField(
                    value = query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text("Canciones, artistas, álbumes…", color = glass.textSecondary)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, tint = glass.textSecondary)
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setQuery("") }) {
                                Icon(Icons.Default.Close, "Borrar", tint = glass.textSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    shape = CircleShape,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = glass.glassStrong,
                        unfocusedContainerColor = glass.glassFill,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = glass.textPrimary,
                        unfocusedTextColor = glass.textPrimary,
                        cursorColor = glass.accent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() })
                )
            }

            if (query.isBlank()) {
                EmptyState(
                    icon = Icons.Default.Search,
                    title = "Busca en tu música",
                    message = "Encuentra canciones, artistas y álbumes al instante. Todo se busca en tu dispositivo."
                )
                return@Column
            }

            if (results.isEmpty() && albums.isEmpty() && artists.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Search,
                    title = "Sin resultados",
                    message = "No encontramos nada para «$query». Prueba con otro título o artista."
                )
                return@Column
            }

            LazyColumn(contentPadding = PaddingValues(bottom = 20.dp)) {
                if (albums.isNotEmpty()) {
                    item {
                        Text(
                            "ÁLBUMES",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textSecondary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                    items(albums, key = { "sa-${it.albumKey}" }) { album ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate(Routes.album(album.albumKey))
                                }
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CoverArt(album.artworkUri, 46.dp, cornerRadius = 12.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    album.album,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = glass.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    album.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textSecondary,
                                    maxLines = 1
                                )
                            }
                            Icon(Icons.Default.Album, null, tint = glass.textSecondary)
                        }
                    }
                }
                if (artists.isNotEmpty()) {
                    item {
                        Text(
                            "ARTISTAS",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textSecondary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                    items(artists, key = { "sr-${it.artist}" }) { artist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate(Routes.artist(artist.artist)) }
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CoverArt(artist.artworkUri, 46.dp, circular = true)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    artist.artist,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = glass.textPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    songsLabel(artist.songCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textSecondary
                                )
                            }
                            Icon(Icons.Default.Person, null, tint = glass.textSecondary)
                        }
                    }
                }
                if (results.isNotEmpty()) {
                    item {
                        Text(
                            "CANCIONES",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textSecondary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                    items(results, key = { "ss-${it.id}" }) { song ->
                        SongRow(
                            song = song,
                            isFavorite = favoriteIds.contains(song.id),
                            isCurrent = current?.id == song.id,
                            isPlaying = isPlaying,
                            onClick = { viewModel.play(song, results) },
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
                viewModel.play(song, results)
                navController.navigate(Routes.PLAYER)
            }
        )
    }
}


