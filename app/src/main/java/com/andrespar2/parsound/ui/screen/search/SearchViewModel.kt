package com.andrespar2.parsound.ui.screen.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrespar2.parsound.data.local.AlbumSummary
import com.andrespar2.parsound.data.local.ArtistSummary
import com.andrespar2.parsound.data.local.SongEntity
import com.andrespar2.parsound.data.repo.MusicRepository
import com.andrespar2.parsound.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playerManager: PlayerManager
) : ViewModel() {
    val query = MutableStateFlow("")

    val results: StateFlow<List<SongEntity>> = query
        .debounce(180)
        .distinctUntilChanged()
        .flatMapLatest { text ->
            if (text.isBlank()) flowOf(emptyList()) else repository.search(text.trim())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val albumResults: StateFlow<List<AlbumSummary>> = combine(repository.albums, query) { albums, text ->
        if (text.isBlank()) emptyList()
        else albums.filter {
            it.album.contains(text, true) || it.artist.contains(text, true)
        }.take(8)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val artistResults: StateFlow<List<ArtistSummary>> = combine(repository.artists, query) { artists, text ->
        if (text.isBlank()) emptyList()
        else artists.filter { it.artist.contains(text, true) }.take(8)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteIds: StateFlow<Set<Long>> = repository.favoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    val currentSong = playerManager.currentSong
    val isPlaying = playerManager.isPlaying

    fun setQuery(value: String) { query.value = value }

    fun play(song: SongEntity, list: List<SongEntity>) = playerManager.playSong(song, list)

    fun toggleFavorite(song: SongEntity) {
        viewModelScope.launch {
            repository.setFavorite(song.id, !favoriteIds.value.contains(song.id))
            playerManager.refreshCurrentSong()
        }
    }
}
