package com.andrespar2.parsound.ui.screen.favorites

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrespar2.parsound.data.local.SongDao
import com.andrespar2.parsound.data.local.SongEntity
import com.andrespar2.parsound.player.PlayerManager
import com.andrespar2.parsound.service.PlaybackService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val songDao: SongDao,
    private val playerManager: PlayerManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val favorites: StateFlow<List<SongEntity>> = songDao.getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun play(song: SongEntity, queueList: List<SongEntity>) {
        val intent = Intent(context, PlaybackService::class.java)
        ContextCompat.startForegroundService(context, intent)
        playerManager.playSong(song, queueList)
    }
}
