package com.andrespar2.parsound.ui.screen.queue

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.andrespar2.parsound.player.PlayerManager
import com.andrespar2.parsound.service.PlaybackService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val playerManager: PlayerManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val queue = playerManager.queue
    val currentSong = playerManager.currentSong

    fun playAt(index: Int) {
        val list = playerManager.queue.value
        if (index < 0 || index >= list.size) return
        val intent = Intent(context, PlaybackService::class.java)
        ContextCompat.startForegroundService(context, intent)
        playerManager.playSong(list[index], list)
    }
}
