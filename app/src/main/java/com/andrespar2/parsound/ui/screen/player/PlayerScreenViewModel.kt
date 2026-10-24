package com.andrespar2.parsound.ui.screen.player

import androidx.lifecycle.ViewModel
import com.andrespar2.parsound.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlayerScreenViewModel @Inject constructor(
    private val playerManager: PlayerManager
) : ViewModel() {
    val currentSong = playerManager.currentSong
    val isPlaying = playerManager.isPlaying
    val queue = playerManager.queue

    fun play() = playerManager.play()
    fun pause() = playerManager.pause()
    fun toggle() = playerManager.toggle()
    fun next() = playerManager.next()
    fun previous() = playerManager.previous()
    fun seekTo(positionMs: Long) = playerManager.seekTo(positionMs)
}
