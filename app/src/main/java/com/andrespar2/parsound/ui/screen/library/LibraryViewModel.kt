package com.andrespar2.parsound.ui.screen.library

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrespar2.parsound.data.local.SongDao
import com.andrespar2.parsound.data.local.SongEntity
import com.andrespar2.parsound.player.PlayerManager
import com.andrespar2.parsound.scanner.MediaScanner
import com.andrespar2.parsound.service.PlaybackService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val songDao: SongDao,
    private val mediaScanner: MediaScanner,
    private val playerManager: PlayerManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val songs: StateFlow<List<SongEntity>> = songDao.getAllSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun scan() {
        viewModelScope.launch {
            mediaScanner.scan()
        }
    }

    fun play(song: SongEntity) {
        val intent = Intent(context, PlaybackService::class.java)
        ContextCompat.startForegroundService(context, intent)
        val allSongs = songs.value
        playerManager.playSong(song, if (allSongs.isNotEmpty()) allSongs else listOf(song))
    }

    fun toggleFavorite(song: SongEntity) {
        viewModelScope.launch {
            songDao.setFavorite(song.id, !song.isFavorite)
        }
    }
}
