package com.andrespar2.parsound.ui.screen.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.andrespar2.parsound.data.local.SongEntity
import com.andrespar2.parsound.data.lyrics.LrcParser
import com.andrespar2.parsound.data.lyrics.Lyrics
import com.andrespar2.parsound.data.prefs.ParSoundPreferences
import com.andrespar2.parsound.data.repo.MusicRepository
import com.andrespar2.parsound.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playerManager: PlayerManager,
    private val preferences: ParSoundPreferences
) : ViewModel() {
    val currentSong = playerManager.currentSong
    val isPlaying = playerManager.isPlaying
    val position = playerManager.positionMs
    val duration = playerManager.durationMs
    val queue = playerManager.queue
    val shuffle = playerManager.shuffle
    val repeatMode = playerManager.repeatMode
    val sleepRemaining = playerManager.sleepRemaining
    val sleepEndsAt = playerManager.sleepEndsAt
    val volume = playerManager.volumeLevel
    val maxVolume = playerManager.maxVolume
    val playbackState = playerManager.playbackState
    val error = playerManager.error

    val favoriteIds: StateFlow<Set<Long>> = repository.favoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val lyrics: StateFlow<Lyrics> = currentSong
        .map { LrcParser.parse(it?.lrc) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Lyrics(emptyList(), false))

    fun toggle() = playerManager.toggle()
    fun next() = playerManager.next()
    fun previous() = playerManager.previous()
    fun seekTo(positionMs: Long) = playerManager.seekTo(positionMs)
    fun toggleShuffle() = playerManager.toggleShuffle()
    fun cycleRepeat() = playerManager.cycleRepeat()
    fun setVolume(value: Int) = playerManager.setVolumeLevel(value)

    fun toggleFavorite() {
        val song = currentSong.value ?: return
        viewModelScope.launch {
            repository.setFavorite(song.id, !favoriteIds.value.contains(song.id))
            playerManager.refreshCurrentSong()
        }
    }

    fun startSleep(minutes: Int) = playerManager.startSleepTimer(minutes)
    fun cancelSleep() = playerManager.cancelSleepTimer()
    fun dismissError() = playerManager.dismissError()
    fun skipError() = playerManager.skipError()

    fun seekToQueueIndex(index: Int) = playerManager.seekToQueueIndex(index)

    fun repeatLabel(): String = when (repeatMode.value) {
        Player.REPEAT_MODE_ONE -> "Repetir canción"
        Player.REPEAT_MODE_ALL -> "Repetir lista"
        else -> "Repetir desactivado"
    }

    val playbackSpeed = preferences.playbackSpeed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1f)
}
