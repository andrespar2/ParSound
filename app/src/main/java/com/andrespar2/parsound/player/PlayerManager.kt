package com.andrespar2.parsound.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.andrespar2.parsound.data.local.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exoPlayer: ExoPlayer
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _queue = MutableStateFlow<List<SongEntity>>(emptyList())
    val queue: StateFlow<List<SongEntity>> = _queue.asStateFlow()

    private val _currentSong = MutableStateFlow<SongEntity?>(null)
    val currentSong: StateFlow<SongEntity?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                val uri = mediaItem?.localConfiguration?.uri?.toString() ?: return
                // try to match
                val match = _queue.value.firstOrNull { it.uri == uri }
                if (match != null) _currentSong.value = match
            }
        })
    }

    fun setQueue(songs: List<SongEntity>, startIndex: Int = 0) {
        _queue.value = songs
        if (songs.isEmpty()) return
        val items = songs.map { MediaItem.fromUri(Uri.parse(it.uri)) }
        exoPlayer.setMediaItems(items, startIndex, 0L)
        exoPlayer.prepare()
        _currentSong.value = songs.getOrNull(startIndex)
        exoPlayer.play()
    }

    fun playSong(song: SongEntity, queueList: List<SongEntity> = listOf(song)) {
        val index = queueList.indexOfFirst { it.id == song.id }.takeIf { it >= 0 } ?: 0
        setQueue(queueList, index)
    }

    fun play() { exoPlayer.play() }
    fun pause() { exoPlayer.pause() }
    fun toggle() { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() }
    fun next() { if (exoPlayer.hasNextMediaItem()) exoPlayer.seekToNextMediaItem() else exoPlayer.seekTo(0, 0) }
    fun previous() { if (exoPlayer.currentPosition > 3000 && exoPlayer.hasPreviousMediaItem()) exoPlayer.seekToPreviousMediaItem() else if (exoPlayer.hasPreviousMediaItem()) exoPlayer.seekToPreviousMediaItem() else exoPlayer.seekTo(0,0) }
    fun seekTo(positionMs: Long) { exoPlayer.seekTo(positionMs) }
    fun getExoPlayer(): ExoPlayer = exoPlayer
}
