package com.andrespar2.parsound.ui.screen.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrespar2.parsound.data.local.AlbumSummary
import com.andrespar2.parsound.data.local.ArtistSummary
import com.andrespar2.parsound.data.local.FolderSummary
import com.andrespar2.parsound.data.local.GenreSummary
import com.andrespar2.parsound.data.local.PlaylistSummary
import com.andrespar2.parsound.data.local.SongEntity
import com.andrespar2.parsound.data.prefs.FolderConfig
import com.andrespar2.parsound.data.repo.MusicRepository
import com.andrespar2.parsound.player.PlayerManager
import com.andrespar2.parsound.scanner.ScanState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SongSort(val label: String) {
    TITLE("Título"),
    ARTIST("Artista"),
    ALBUM("Álbum"),
    DURATION("Duración"),
    ADDED("Fecha de añadido")
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playerManager: PlayerManager
) : ViewModel() {
    val songs: StateFlow<List<SongEntity>> = repository.songs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val favorites: StateFlow<List<SongEntity>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val favoriteIds: StateFlow<Set<Long>> = repository.favoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    val albums: StateFlow<List<AlbumSummary>> = repository.albums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val artists: StateFlow<List<ArtistSummary>> = repository.artists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val genres: StateFlow<List<GenreSummary>> = repository.genres
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val folders: StateFlow<List<FolderSummary>> = repository.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val playlists: StateFlow<List<PlaylistSummary>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val configuredFolders: StateFlow<List<FolderConfig>> = repository.configuredFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val scanState: StateFlow<ScanState> = repository.scanState
    val lastScan = repository.lastScan.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)
    val currentSong = playerManager.currentSong
    val isPlaying = playerManager.isPlaying
    val sortOrder = MutableStateFlow(SongSort.TITLE)

    val sortedSongs: StateFlow<List<SongEntity>> = combine(songs, sortOrder) { list, sort ->
        when (sort) {
            SongSort.TITLE -> list.sortedBy { it.title.lowercase() }
            SongSort.ARTIST -> list.sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
            SongSort.ALBUM -> list.sortedWith(compareBy({ it.album.lowercase() }, { it.trackNumber }))
            SongSort.DURATION -> list.sortedByDescending { it.durationMs }
            SongSort.ADDED -> list.sortedByDescending { it.addedAt }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSort(sort: SongSort) { sortOrder.value = sort }

    fun play(song: SongEntity, list: List<SongEntity>) {
        playerManager.playSong(song, list)
    }

    fun playAll(list: List<SongEntity>) {
        if (list.isNotEmpty()) playerManager.setQueue(list, 0, autoPlay = true)
    }

    fun shuffleAll(list: List<SongEntity>) {
        if (list.isEmpty()) return
        if (!playerManager.shuffle.value) playerManager.toggleShuffle()
        playerManager.setQueue(list, 0, autoPlay = true)
    }

    fun toggleFavorite(song: SongEntity) {
        viewModelScope.launch {
            repository.setFavorite(song.id, !favoriteIds.value.contains(song.id))
            playerManager.refreshCurrentSong()
        }
    }

    fun scan() = repository.scanNow()

    fun cancelScan() = repository.cancelScan()

    fun addFolder(folder: FolderConfig) {
        viewModelScope.launch { repository.addFolder(folder) }
    }

    fun removeFolder(folder: FolderConfig) {
        viewModelScope.launch { repository.removeFolder(folder) }
    }

    fun importMediaStore(onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val count = runCatching { repository.importMediaStore() }.getOrDefault(0)
            onResult(count)
        }
    }

    fun createPlaylist(name: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            onCreated(repository.createPlaylist(name))
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch { repository.deletePlaylist(id) }
    }

    fun addToQueue(song: SongEntity) = playerManager.addToQueue(song)
}
