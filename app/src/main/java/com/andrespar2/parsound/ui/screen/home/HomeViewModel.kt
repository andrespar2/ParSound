package com.andrespar2.parsound.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrespar2.parsound.data.local.AlbumSummary
import com.andrespar2.parsound.data.local.ArtistSummary
import com.andrespar2.parsound.data.local.PlaylistSummary
import com.andrespar2.parsound.data.local.SongEntity
import com.andrespar2.parsound.data.prefs.FolderConfig
import com.andrespar2.parsound.data.repo.MusicRepository
import com.andrespar2.parsound.player.PlayerManager
import com.andrespar2.parsound.scanner.ScanState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playerManager: PlayerManager
) : ViewModel() {
    val songs: StateFlow<List<SongEntity>> = repository.songs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val recent: StateFlow<List<SongEntity>> = repository.recentSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val favorites: StateFlow<List<SongEntity>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val albums: StateFlow<List<AlbumSummary>> = repository.albums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val artists: StateFlow<List<ArtistSummary>> = repository.artists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val playlists: StateFlow<List<PlaylistSummary>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val folders: StateFlow<List<FolderConfig>> = repository.configuredFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val favoriteIds: StateFlow<Set<Long>> = repository.favoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    val currentSong = playerManager.currentSong
    val isPlaying = playerManager.isPlaying
    val scanState: StateFlow<ScanState> = repository.scanState
    val lastScan = repository.lastScan.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    fun play(song: SongEntity, list: List<SongEntity>) {
        playerManager.playSong(song, list)
    }

    fun togglePlayback() = playerManager.toggle()

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

    fun openPlayer() = Unit
}
