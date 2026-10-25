package com.andrespar2.parsound.ui.screen.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrespar2.parsound.data.local.PlaylistEntity
import com.andrespar2.parsound.data.local.SongEntity
import com.andrespar2.parsound.data.repo.MusicRepository
import com.andrespar2.parsound.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MusicRepository,
    private val playerManager: PlayerManager
) : ViewModel() {
    private val playlistId: Long = savedStateHandle["playlistId"] ?: 0L

    val playlist: StateFlow<PlaylistEntity?> = repository.playlist(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val songs: StateFlow<List<SongEntity>> = repository.playlistSongs(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allSongs: StateFlow<List<SongEntity>> = repository.songs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val favoriteIds: StateFlow<Set<Long>> = repository.favoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    val currentSong = playerManager.currentSong
    val isPlaying = playerManager.isPlaying

    fun play(song: SongEntity) = playerManager.playSong(song, songs.value)

    fun playAll() {
        if (songs.value.isNotEmpty()) playerManager.setQueue(songs.value, 0, autoPlay = true)
    }

    fun shuffleAll() {
        if (songs.value.isEmpty()) return
        if (!playerManager.shuffle.value) playerManager.toggleShuffle()
        playerManager.setQueue(songs.value, 0, autoPlay = true)
    }

    fun toggleFavorite(song: SongEntity) {
        viewModelScope.launch {
            repository.setFavorite(song.id, !favoriteIds.value.contains(song.id))
            playerManager.refreshCurrentSong()
        }
    }

    fun addSong(song: SongEntity, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val added = repository.addToPlaylist(playlistId, song.id)
            onResult(added)
        }
    }

    fun removeSong(song: SongEntity) {
        viewModelScope.launch { repository.removeFromPlaylist(playlistId, song.id) }
    }

    fun rename(name: String) {
        viewModelScope.launch { repository.renamePlaylist(playlistId, name) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            onDeleted()
        }
    }
}
