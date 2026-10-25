package com.andrespar2.parsound.ui.screen.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrespar2.parsound.data.local.PlaylistSummary
import com.andrespar2.parsound.data.repo.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {
    val playlists: StateFlow<List<PlaylistSummary>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            onCreated(repository.createPlaylist(name))
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.deletePlaylist(id) }
    }
}
