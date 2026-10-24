package com.andrespar2.parsound.ui.screen.search

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val songDao: SongDao,
    private val playerManager: PlayerManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val results = _query.flatMapLatest { q ->
        if (q.isBlank()) flowOf(emptyList()) else songDao.search(q)
    }.stateInScope(emptyList())

    private fun <T> kotlinx.coroutines.flow.Flow<T>.stateInScope(initial: T) =
        this.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)

    fun setQuery(q: String) {
        _query.value = q
    }

    fun play(song: SongEntity, queueList: List<SongEntity>) {
        val intent = Intent(context, PlaybackService::class.java)
        ContextCompat.startForegroundService(context, intent)
        playerManager.playSong(song, queueList)
    }
}
