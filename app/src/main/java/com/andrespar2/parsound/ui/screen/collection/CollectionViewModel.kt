package com.andrespar2.parsound.ui.screen.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrespar2.parsound.data.local.SongEntity
import com.andrespar2.parsound.data.repo.MusicRepository
import com.andrespar2.parsound.player.PlayerManager
import com.andrespar2.parsound.util.formatTotalDuration
import com.andrespar2.parsound.util.songsLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MusicRepository,
    private val playerManager: PlayerManager
) : ViewModel() {
    private val albumKey: String? = savedStateHandle["albumKey"]
    private val artist: String? = savedStateHandle["artist"]
    private val genre: String? = savedStateHandle["genre"]
    private val folder: String? = savedStateHandle["folder"]
    private val folderUri: String? = savedStateHandle["folderUri"]

    val kind: String = when {
        albumKey != null -> "album"
        artist != null -> "artist"
        genre != null -> "genre"
        else -> "folder"
    }

    private val source: Flow<List<SongEntity>> = when {
        albumKey != null -> repository.albumSongs(albumKey)
        artist != null -> repository.artistSongs(artist)
        genre != null -> repository.genreSongs(genre)
        folder != null && folderUri != null -> repository.folderSongs(folder, folderUri)
        else -> flowOf(emptyList())
    }

    val songs: StateFlow<List<SongEntity>> = source
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val title: StateFlow<String> = source.map { list ->
        when {
            albumKey != null -> list.firstOrNull()?.album ?: albumKey.substringBeforeLast("|")
            artist != null -> artist
            genre != null -> genre
            else -> folder?.substringAfterLast('/') ?: "Carpeta"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val subtitle: StateFlow<String> = source.map { list ->
        when {
            list.isEmpty() -> "Sin canciones"
            albumKey != null -> buildString {
                append(list.first().artist)
                append(" · ")
                append(songsLabel(list.size) + " · ")
                append(formatTotalDuration(list.sumOf { it.durationMs }))
            }
            else -> "${songsLabel(list.size)} · ${formatTotalDuration(list.sumOf { it.durationMs })}"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val artworkUri: StateFlow<String?> = source.map { list ->
        list.firstOrNull { !it.artworkUri.isNullOrBlank() }?.artworkUri
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
}
