package com.andrespar2.parsound.ui.screen.queue

import androidx.lifecycle.ViewModel
import com.andrespar2.parsound.data.local.SongEntity
import com.andrespar2.parsound.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val playerManager: PlayerManager
) : ViewModel() {
    val queue: StateFlow<List<SongEntity>> = playerManager.queue
    val currentSong = playerManager.currentSong
    val isPlaying = playerManager.isPlaying

    fun playAt(index: Int) = playerManager.seekToQueueIndex(index)
    fun move(from: Int, to: Int) = playerManager.moveQueueItem(from, to)
    fun remove(index: Int) = playerManager.removeQueueItem(index)
    fun clear() = playerManager.clearQueue()
    fun toggle() = playerManager.toggle()
}
