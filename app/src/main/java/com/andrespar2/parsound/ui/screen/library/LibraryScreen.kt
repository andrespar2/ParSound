package com.andrespar2.parsound.ui.screen.library

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.andrespar2.parsound.data.local.PlaylistSummary
import com.andrespar2.parsound.data.local.SongEntity
import com.andrespar2.parsound.data.prefs.FolderConfig
import com.andrespar2.parsound.scanner.ScanState
import com.andrespar2.parsound.ui.components.AlbumCard
import com.andrespar2.parsound.ui.components.AmbientBackground
import com.andrespar2.parsound.ui.components.CoverArt
import com.andrespar2.parsound.ui.components.CreatePlaylistDialog
import com.andrespar2.parsound.ui.components.EmptyState
import com.andrespar2.parsound.ui.components.GlassCard
import com.andrespar2.parsound.ui.components.PillButton
import com.andrespar2.parsound.ui.components.PlaylistCard
import com.andrespar2.parsound.ui.components.SongOptionsSheet
import com.andrespar2.parsound.ui.components.SongRow
import com.andrespar2.parsound.ui.components.ParSoundTopBar
import com.andrespar2.parsound.ui.components.glassModifier
import com.andrespar2.parsound.ui.navigation.Routes
import com.andrespar2.parsound.util.songsLabel
import com.andrespar2.parsound.util.foldersLabel
import com.andrespar2.parsound.ui.theme.LocalGlass
import com.andrespar2.parsound.util.formatDate

private enum class LibraryTab(val label: String) {
    SONGS("Canciones"),
    ALBUMS("Álbumes"),
    ARTISTS("Artistas"),
    GENRES("Géneros"),
    FOLDERS("Carpetas"),
    PLAYLISTS("Listas"),
    FAVORITES("Favoritas")
}

@Composable
fun LibraryScreen(
    navController: NavController,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val songs by viewModel.sortedSongs.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val genres by viewModel.genres.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val configuredFolders by viewModel.configuredFolders.collectAsState()
    val scanState by viewModel.scanState.collectAsState()
    val lastScan by viewModel.lastScan.collectAsState()
    val current by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val glass = LocalGlass.current
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val tab = LibraryTab.entries[selectedTab]
    var optionsSong by remember { mutableStateOf<SongEntity?>(null) }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var showOverflow by remember { mutableStateOf(false) }
    var folderToDelete by remember { mutableStateOf<FolderConfig?>(null) }
    var playlistToDelete by remember { mutableStateOf<PlaylistSummary?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    val sort by viewModel.sortOrder.collectAsState()

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val name = runCatching { DocumentFile.fromTreeUri(context, uri)?.name }.getOrNull()
                ?: "Carpeta de música"
            viewModel.addFolder(FolderConfig(uri.toString(), name))
            Toast.makeText(context, "Analizando $name…", Toast.LENGTH_SHORT).show()
        }
    }

    val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.importMediaStore { count ->
                Toast.makeText(
                    context,
                    if (count > 0) "$count canciones importadas" else "No se encontró música nueva",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(context, "Permiso de audio denegado", Toast.LENGTH_SHORT).show()
        }
    }

    if (folderToDelete != null) {
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            containerColor = glass.backgroundEnd,
            title = { Text("Quitar carpeta", color = glass.textPrimary) },
            text = {
                Text(
                    "Se quitarán de ParSound las canciones de ${folderToDelete?.name}. Los archivos originales no se borrarán.",
                    color = glass.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    folderToDelete?.let { viewModel.removeFolder(it) }
                    folderToDelete = null
                }) { Text("Quitar", color = glass.accent) }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text("Cancelar", color = glass.textSecondary)
                }
            }
        )
    }

    if (playlistToDelete != null) {
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            containerColor = glass.backgroundEnd,
            title = { Text("Eliminar lista", color = glass.textPrimary) },
            text = {
                Text(
                    "¿Eliminar «${playlistToDelete?.name}»? Las canciones seguirán en tu biblioteca.",
                    color = glass.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    playlistToDelete?.let { viewModel.deletePlaylist(it.id) }
                    playlistToDelete = null
                }) { Text("Eliminar", color = glass.accent) }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text("Cancelar", color = glass.textSecondary)
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = glass.backgroundEnd,
            title = { Text("Importar del dispositivo", color = glass.textPrimary) },
            text = {
                Text(
                    "ParSound puede leer la música registrada por Android. Se pedirá permiso de acceso a audio. También puedes usar carpetas con acceso limitado, que es la opción recomendada.",
                    color = glass.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showImportDialog = false
                    storagePermissionLauncher.launch(mediaPermission)
                }) { Text("Continuar", color = glass.accent) }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancelar", color = glass.textSecondary)
                }
            }
        )
    }

    if (showCreatePlaylist) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylist = false },
            onCreate = { name ->
                viewModel.createPlaylist(name) { id ->
                    showCreatePlaylist = false
                    navController.navigate(Routes.playlist(id))
                }
            }
        )
    }

    AmbientBackground(showArtwork = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            ParSoundTopBar(
                title = "Biblioteca",
                subtitle = buildString {
                    append(songsLabel(songs.size))
                    if (configuredFolders.isNotEmpty()) append(" · ${foldersLabel(configuredFolders.size)}")
                },
                actions = {
                    Box {
                        IconButton(onClick = { showOverflow = true }) {
                            Icon(Icons.Default.MoreVert, "Más opciones", tint = glass.textPrimary)
                        }
                        DropdownMenu(
                            expanded = showOverflow,
                            onDismissRequest = { showOverflow = false },
                            containerColor = glass.backgroundEnd
                        ) {
                            DropdownMenuItem(
                                text = { Text("Añadir carpeta", color = glass.textPrimary) },
                                leadingIcon = { Icon(Icons.Default.Add, null, tint = glass.accent) },
                                onClick = {
                                    showOverflow = false
                                    folderPicker.launch(null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Importar del dispositivo", color = glass.textPrimary) },
                                leadingIcon = { Icon(Icons.Default.FolderOpen, null, tint = glass.textPrimary) },
                                onClick = {
                                    showOverflow = false
                                    showImportDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Actualizar biblioteca", color = glass.textPrimary) },
                                leadingIcon = { Icon(Icons.Default.Refresh, null, tint = glass.textPrimary) },
                                onClick = {
                                    showOverflow = false
                                    viewModel.scan()
                                }
                            )
                        }
                    }
                    IconButton(onClick = { showSort = true }) {
                        Icon(Icons.Default.Sort, "Ordenar", tint = glass.textPrimary)
                    }
                    IconButton(onClick = { navController.navigate(Routes.SEARCH) }) {
                        Icon(Icons.Default.Search, "Buscar", tint = glass.textPrimary)
                    }
                    IconButton(onClick = { viewModel.scan() }) {
                        Icon(Icons.Default.Refresh, "Actualizar", tint = glass.textPrimary)
                    }
                }
            )

            if (showSort) {
                SortDialog(
                    current = sort,
                    onSelect = {
                        viewModel.setSort(it)
                        showSort = false
                    },
                    onDismiss = { showSort = false }
                )
            }

            if (scanState is ScanState.Scanning) {
                val scanning = scanState as ScanState.Scanning
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .glassModifier(16.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = glass.accent
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Analizando ${scanning.folder}",
                            style = MaterialTheme.typography.labelLarge,
                            color = glass.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${scanning.filesProcessed} de ${scanning.filesFound} archivos · ${scanning.errors} avisos",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                    }
                    TextButton(onClick = { viewModel.cancelScan() }) {
                        Text("Cancelar", color = glass.accent)
                    }
                }
            }

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 12.dp,
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentColor = glass.accent,
                divider = {}
            ) {
                LibraryTab.entries.forEachIndexed { index, item ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                item.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selectedTab == index) glass.accent else glass.textSecondary
                            )
                        }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (tab) {
                    LibraryTab.SONGS -> SongsTab(
                        songs = songs,
                        favoriteIds = favoriteIds,
                        currentId = current?.id,
                        isPlaying = isPlaying,
                        onPlay = { viewModel.play(it, songs) },
                        onPlayAll = { viewModel.playAll(songs) },
                        onShuffle = { viewModel.shuffleAll(songs) },
                        onFavorite = viewModel::toggleFavorite,
                        onMore = { optionsSong = it },
                        onAddFolder = { folderPicker.launch(null) }
                    )
                    LibraryTab.ALBUMS -> AlbumsTab(
                        albums = albums,
                        onOpen = { navController.navigate(Routes.album(it.albumKey)) },
                        onAddFolder = { folderPicker.launch(null) }
                    )
                    LibraryTab.ARTISTS -> ArtistsTab(
                        artists = artists,
                        onOpen = { navController.navigate(Routes.artist(it.artist)) },
                        onAddFolder = { folderPicker.launch(null) }
                    )
                    LibraryTab.GENRES -> GenresTab(
                        genres = genres,
                        onOpen = { navController.navigate(Routes.genre(it.genre)) },
                        onAddFolder = { folderPicker.launch(null) }
                    )
                    LibraryTab.FOLDERS -> FoldersTab(
                        folders = folders,
                        configuredFolders = configuredFolders,
                        lastScan = lastScan,
                        onOpen = { folder -> navController.navigate(Routes.folder(folder.folderUri, folder.folder)) },
                        onAdd = { folderPicker.launch(null) },
                        onImport = { showImportDialog = true },
                        onDelete = { folderToDelete = it }
                    )
                    LibraryTab.PLAYLISTS -> PlaylistsTab(
                        playlists = playlists,
                        onOpen = { navController.navigate(Routes.playlist(it.id)) },
                        onCreate = { showCreatePlaylist = true },
                        onDelete = { playlistToDelete = it },
                        onAddFolder = { folderPicker.launch(null) }
                    )
                    LibraryTab.FAVORITES -> FavoritesTab(
                        favorites = favorites,
                        favoriteIds = favoriteIds,
                        currentId = current?.id,
                        isPlaying = isPlaying,
                        onPlay = { viewModel.play(it, favorites) },
                        onPlayAll = { viewModel.playAll(favorites) },
                        onShuffle = { viewModel.shuffleAll(favorites) },
                        onFavorite = viewModel::toggleFavorite,
                        onMore = { optionsSong = it }
                    )
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
                viewModel.play(song, songs)
                navController.navigate(Routes.PLAYER)
            }
        )
    }
}

@Composable
private fun SongsTab(
    songs: List<SongEntity>,
    favoriteIds: Set<Long>,
    currentId: Long?,
    isPlaying: Boolean,
    onPlay: (SongEntity) -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onFavorite: (SongEntity) -> Unit,
    onMore: (SongEntity) -> Unit,
    onAddFolder: () -> Unit
) {
    if (songs.isEmpty()) {
        EmptyState(
            icon = Icons.Default.LibraryMusic,
            title = "Aún no hay canciones",
            message = "Añade una carpeta con música o importa la biblioteca del dispositivo para empezar.",
            actionLabel = "Añadir carpeta",
            onAction = onAddFolder
        )
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PillButton("Reproducir", onClick = onPlayAll, icon = Icons.Default.PlayArrow)
                PillButton("Aleatorio", onClick = onShuffle, icon = Icons.Default.Shuffle, primary = false)
            }
        }
        items(songs, key = { "song-${it.id}" }) { song ->
            SongRow(
                song = song,
                isFavorite = favoriteIds.contains(song.id),
                isCurrent = currentId == song.id,
                isPlaying = isPlaying,
                onClick = { onPlay(song) },
                onToggleFavorite = { onFavorite(song) },
                onMore = { onMore(song) }
            )
        }
    }
}

@Composable
private fun AlbumsTab(
    albums: List<com.andrespar2.parsound.data.local.AlbumSummary>,
    onOpen: (com.andrespar2.parsound.data.local.AlbumSummary) -> Unit,
    onAddFolder: () -> Unit
) {
    if (albums.isEmpty()) {
        EmptyState(
            icon = Icons.Default.LibraryMusic,
            title = "Sin álbumes",
            message = "Cuando tu música tenga metadatos de álbum aparecerá aquí.",
            actionLabel = "Añadir carpeta",
            onAction = onAddFolder
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(142.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        items(albums, key = { it.albumKey }) { album ->
            AlbumCard(album, onClick = { onOpen(album) }, size = 142.dp)
        }
    }
}

@Composable
private fun ArtistsTab(
    artists: List<com.andrespar2.parsound.data.local.ArtistSummary>,
    onOpen: (com.andrespar2.parsound.data.local.ArtistSummary) -> Unit,
    onAddFolder: () -> Unit
) {
    if (artists.isEmpty()) {
        EmptyState(
            icon = Icons.Default.LibraryMusic,
            title = "Sin artistas",
            message = "Los artistas se obtienen de los metadatos de tus archivos.",
            actionLabel = "Añadir carpeta",
            onAction = onAddFolder
        )
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        items(artists, key = { it.artist }) { artist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(artist) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverArt(artist.artworkUri, 54.dp, circular = true)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        artist.artist,
                        style = MaterialTheme.typography.titleSmall,
                        color = LocalGlass.current.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        songsLabel(artist.songCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalGlass.current.textSecondary
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = LocalGlass.current.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun GenresTab(
    genres: List<com.andrespar2.parsound.data.local.GenreSummary>,
    onOpen: (com.andrespar2.parsound.data.local.GenreSummary) -> Unit,
    onAddFolder: () -> Unit
) {
    if (genres.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Tag,
            title = "Sin géneros",
            message = "Los géneros aparecen cuando tus archivos los incluyen en sus metadatos.",
            actionLabel = "Añadir carpeta",
            onAction = onAddFolder
        )
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        items(genres, key = { it.genre }) { genre ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(genre) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(LocalGlass.current.glassFill),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Tag, null, tint = LocalGlass.current.accent)
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    genre.genre,
                    style = MaterialTheme.typography.titleSmall,
                    color = LocalGlass.current.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${genre.songCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalGlass.current.textSecondary
                )
            }
        }
    }
}

@Composable
private fun FoldersTab(
    folders: List<com.andrespar2.parsound.data.local.FolderSummary>,
    configuredFolders: List<FolderConfig>,
    lastScan: Long,
    onOpen: (com.andrespar2.parsound.data.local.FolderSummary) -> Unit,
    onAdd: () -> Unit,
    onImport: () -> Unit,
    onDelete: (FolderConfig) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            GlassCard(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                strong = true
            ) {
                Text(
                    "Carpetas de música",
                    style = MaterialTheme.typography.titleMedium,
                    color = LocalGlass.current.textPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Último análisis: ${formatDate(lastScan)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalGlass.current.textSecondary
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton("Añadir carpeta", onClick = onAdd, icon = Icons.Default.Add)
                    PillButton("Importar", onClick = onImport, primary = false)
                }
            }
        }
        if (folders.isEmpty() && configuredFolders.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.Folder,
                    title = "Sin carpetas configuradas",
                    message = "Selecciona la carpeta donde guardas tu música. ParSound solo accederá a lo que autorices.",
                    actionLabel = "Elegir carpeta",
                    onAction = onAdd
                )
            }
        }
        items(configuredFolders, key = { it.uri }) { config ->
            val summary = folders.firstOrNull { it.folderUri == config.uri }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (summary != null) onOpen(summary)
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LocalGlass.current.glassFill),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Folder, null, tint = LocalGlass.current.accent)
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        config.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = LocalGlass.current.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        songsLabel(summary?.songCount ?: 0),
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalGlass.current.textSecondary
                    )
                }
                IconButton(onClick = { onDelete(config) }) {
                    Icon(Icons.Default.Delete, "Quitar carpeta", tint = LocalGlass.current.textSecondary)
                }
            }
        }
        val mediastore = folders.filter { it.folderUri == "mediastore://all" }
        if (mediastore.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.FolderOpen, null, tint = LocalGlass.current.accent)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "Almacenamiento del dispositivo",
                            style = MaterialTheme.typography.titleSmall,
                            color = LocalGlass.current.textPrimary
                        )
                        Text(
                            songsLabel(mediastore.sumOf { it.songCount }) + " importadas",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalGlass.current.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistsTab(
    playlists: List<PlaylistSummary>,
    onOpen: (PlaylistSummary) -> Unit,
    onCreate: () -> Unit,
    onDelete: (PlaylistSummary) -> Unit,
    onAddFolder: () -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                PillButton("Nueva lista", onClick = onCreate, icon = Icons.Default.Add)
            }
        }
        if (playlists.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.LibraryMusic,
                    title = "Sin listas propias",
                    message = "Crea listas para organizar tus canciones favoritas.",
                    actionLabel = "Crear lista",
                    onAction = onCreate
                )
            }
        }
        items(playlists, key = { "pl-${it.id}" }) { playlist ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    PlaylistCard(playlist, onClick = { onOpen(playlist) })
                }
                IconButton(onClick = { onDelete(playlist) }) {
                    Icon(Icons.Default.Delete, "Eliminar lista", tint = LocalGlass.current.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun FavoritesTab(
    favorites: List<SongEntity>,
    favoriteIds: Set<Long>,
    currentId: Long?,
    isPlaying: Boolean,
    onPlay: (SongEntity) -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onFavorite: (SongEntity) -> Unit,
    onMore: (SongEntity) -> Unit
) {
    if (favorites.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Favorite,
            title = "Sin favoritas",
            message = "Marca el corazón de una canción para encontrarla aquí.",
        )
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PillButton("Reproducir", onClick = onPlayAll, icon = Icons.Default.PlayArrow)
                PillButton("Aleatorio", onClick = onShuffle, icon = Icons.Default.Shuffle, primary = false)
            }
        }
        items(favorites, key = { "fav-${it.id}" }) { song ->
            SongRow(
                song = song,
                isFavorite = favoriteIds.contains(song.id),
                isCurrent = currentId == song.id,
                isPlaying = isPlaying,
                onClick = { onPlay(song) },
                onToggleFavorite = { onFavorite(song) },
                onMore = { onMore(song) }
            )
        }
    }
}

@Composable
private fun SortDialog(
    current: SongSort,
    onSelect: (SongSort) -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalGlass.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = glass.backgroundEnd,
        title = { Text("Ordenar canciones", color = glass.textPrimary) },
        text = {
            Column {
                SongSort.entries.forEach { sort ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(sort) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = sort == current, onClick = { onSelect(sort) })
                        Spacer(Modifier.width(8.dp))
                        Text(sort.label, color = glass.textPrimary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar", color = glass.accent) }
        }
    )
}
