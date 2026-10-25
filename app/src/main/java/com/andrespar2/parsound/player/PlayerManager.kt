package com.andrespar2.parsound.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.net.Uri
import android.util.LruCache
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.palette.graphics.Palette
import com.andrespar2.parsound.data.local.SongEntity
import com.andrespar2.parsound.data.prefs.ParSoundPreferences
import com.andrespar2.parsound.data.repo.MusicRepository
import com.andrespar2.parsound.service.PlaybackService
import com.andrespar2.parsound.widget.ParSoundWidget
import com.andrespar2.parsound.widget.WidgetSnapshot
import com.andrespar2.parsound.widget.WidgetStateStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MusicRepository,
    private val preferences: ParSoundPreferences,
    private val equalizer: EqualizerController,
    val player: CrossfadePlayer
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val paletteCache = LruCache<Long, PaletteColors>(24)
    private val restoreMutex = Mutex()

    private val _queue = MutableStateFlow<List<SongEntity>>(emptyList())
    val queue: StateFlow<List<SongEntity>> = _queue.asStateFlow()

    private val _currentSong = MutableStateFlow<SongEntity?>(null)
    val currentSong: StateFlow<SongEntity?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _palette = MutableStateFlow(PaletteColors.Default)
    val palette: StateFlow<PaletteColors> = _palette.asStateFlow()

    private val _sleepEndsAt = MutableStateFlow(0L)
    val sleepEndsAt: StateFlow<Long> = _sleepEndsAt.asStateFlow()

    private val _sleepRemaining = MutableStateFlow(0L)
    val sleepRemaining: StateFlow<Long> = _sleepRemaining.asStateFlow()

    private val _volumeLevel = MutableStateFlow(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    val volumeLevel: StateFlow<Int> = _volumeLevel.asStateFlow()
    val maxVolume: Int get() = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    private var songsById: Map<Long, SongEntity> = emptyMap()
    private var lastHistorySongId = -1L
    private var lastPersistAt = 0L
    private var lastWidgetSongId = -1L
    private var lastWidgetPlaying: Boolean? = null
    private var shufflePreference = false
    private var sleepTimerJob: Job? = null
    private var restoreAttempted = false
    private var paletteJob: Job? = null
    private var mediaController: MediaController? = null
    private var mediaControllerFuture: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null

    init {
        scope.launch {
            preferences.crossfadeMs.collect { player.setCrossfadeMillis(it) }
        }
        scope.launch {
            preferences.pauseOnDisconnect.collect { player.setPauseOnDisconnect(it) }
        }
        scope.launch {
            preferences.shuffleEnabled.collect { enabled ->
                shufflePreference = enabled
                if (enabled != player.shuffleModeEnabled) {
                    player.shuffleModeEnabled = enabled
                    _shuffle.value = enabled
                }
            }
        }
        scope.launch {
            preferences.repeatMode.collect { mode ->
                if (mode != player.repeatMode) player.repeatMode = mode
                _repeatMode.value = mode
            }
        }
        scope.launch {
            kotlinx.coroutines.flow.combine(
                preferences.playbackSpeed,
                preferences.pitch
            ) { speed, pitch -> speed to pitch }.collect { (speed, pitch) ->
                val parameters = PlaybackParameters(speed, pitch)
                if (parameters != player.playbackParameters) player.playbackParameters = parameters
            }
        }

        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                syncFromPlayer()
            }
        })
        player.setAudioSessionListener { session ->
            equalizer.attachSession(session)
        }
        player.setStateListener {
            syncFromPlayer()
        }
        scope.launch { tickerLoop() }
    }

    private suspend fun tickerLoop() {
        while (scope.isActive) {
            if (player.playWhenReady || player.isPlaying || player.playbackState == Player.STATE_READY) {
                syncPosition()
                persistProgressIfNeeded()
            }
            updateVolume()
            updateSleepRemaining()
            delay(200)
        }
    }

    private fun syncFromPlayer() {
        _isPlaying.value = player.isPlaying
        _playbackState.value = player.playbackState
        _repeatMode.value = player.repeatMode
        _shuffle.value = player.shuffleModeEnabled
        _error.value = player.playerError?.let { playbackError ->
            "No se pudo reproducir esta canción (${playbackError.errorCodeName}). " +
                "Comprueba que el archivo siga disponible y vuelve a analizar la biblioteca."
        }
        syncPosition()
        syncQueue()
        val mediaId = player.currentMediaItem?.mediaId?.toLongOrNull()
        val song = mediaId?.let { songsById[it] }
        if (song != null && song.id != _currentSong.value?.id) {
            _currentSong.value = song
            onSongChanged(song)
        }
        if (player.isPlaying && song != null && song.id != lastHistorySongId) {
            lastHistorySongId = song.id
            scope.launch { repository.addHistory(song.id) }
        }
        updateWidgetIfNeeded()
    }

    private fun syncQueue() {
        if (songsById.isEmpty()) {
            _queue.value = emptyList()
            return
        }
        val synced = buildList {
            for (i in 0 until player.mediaItemCount) {
                val id = player.getMediaItemAt(i).mediaId.toLongOrNull() ?: continue
                songsById[id]?.let { add(it) }
            }
        }
        if (synced.map { it.id } != _queue.value.map { it.id }) {
            _queue.value = synced
        }
    }

    private fun syncPosition() {
        val position = player.currentPosition.coerceAtLeast(0L)
        _positionMs.value = position
        val playerDuration = player.duration
        val songDuration = _currentSong.value?.durationMs ?: 0L
        _durationMs.value = when {
            playerDuration > 0 && playerDuration != androidx.media3.common.C.TIME_UNSET -> playerDuration
            songDuration > 0 -> songDuration
            else -> 0L
        }
    }

    private fun onSongChanged(song: SongEntity) {
        scope.launch {
            preferences.setLastSongUri(song.uri)
            preferences.setLastPosition(0)
        }
        paletteJob?.cancel()
        paletteJob = scope.launch {
            val cached = paletteCache.get(song.id)
            val colors = cached ?: withContext(Dispatchers.IO) { extractPalette(song) }
            paletteCache.put(song.id, colors)
            _palette.value = colors
            updateWidget(force = true)
        }
        lastHistorySongId = -1L
    }

    private fun extractPalette(song: SongEntity): PaletteColors {
        val bitmap = loadArtwork(song.artworkUri)
            ?: return PaletteColors.Default
        return runCatching {
            val palette = Palette.from(bitmap).maximumColorCount(20).generate()
            val dominant = palette.dominantSwatch?.rgb
            val vibrant = palette.vibrantSwatch?.rgb ?: palette.lightVibrantSwatch?.rgb
            val dark = palette.darkMutedSwatch?.rgb ?: palette.darkVibrantSwatch?.rgb
            val muted = palette.mutedSwatch?.rgb
            val start = darken(dark ?: dominant ?: 0xFF10151F.toInt(), 0.55f)
            val end = darken(muted ?: dominant ?: 0xFF1B2230.toInt(), 0.30f)
            val accent = vibrant ?: palette.darkVibrantSwatch?.rgb ?: 0xFF8AB4F8.toInt()
            val accentSoft = blend(accent, 0xFF0B0E15.toInt(), 0.45f)
            PaletteColors(
                backgroundStart = start,
                backgroundEnd = end,
                accent = accent,
                accentSoft = accentSoft,
                onBackground = 0xFFF5F7FB.toInt()
            )
        }.getOrDefault(PaletteColors.Default).also { bitmap.recycle() }
    }

    private fun loadArtwork(uriString: String?): Bitmap? {
        if (uriString.isNullOrBlank()) return null
        return runCatching {
            val uri = Uri.parse(uriString)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while (bounds.outWidth / sample > 320 || bounds.outHeight / sample > 320) sample *= 2
            val options = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            openStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        }.getOrNull()
    }

    private fun openStream(uri: Uri): java.io.InputStream? = when (uri.scheme) {
        "content" -> context.contentResolver.openInputStream(uri)
        "file" -> runCatching { java.io.File(uri.path ?: return null).inputStream() }.getOrNull()
        else -> runCatching { java.io.File(uri.toString()).inputStream() }.getOrNull()
    }

    private fun updateWidgetIfNeeded() {
        val song = _currentSong.value ?: return
        val playing = player.isPlaying
        if (song.id == lastWidgetSongId && playing == lastWidgetPlaying) return
        lastWidgetSongId = song.id
        lastWidgetPlaying = playing
        updateWidget(force = false)
    }

    fun updateWidget(force: Boolean = false) {
        val song = _currentSong.value
        val colors = _palette.value
        val snapshot = WidgetSnapshot(
            title = song?.title ?: "ParSound",
            artist = song?.artist ?: "Añade música a tu biblioteca",
            isPlaying = player.isPlaying,
            artworkPath = song?.artworkUri,
            accent = colors.accent,
            background = colors.backgroundStart,
            songId = song?.id ?: 0L
        )
        WidgetStateStore.save(context, snapshot)
        ParSoundWidget.updateAll(context)
    }

    fun setQueue(songs: List<SongEntity>, startIndex: Int = 0, autoPlay: Boolean = true) {
        if (songs.isEmpty()) return
        _error.value = null
        songsById = songs.associateBy { it.id }
        _queue.value = songs
        val index = startIndex.coerceIn(songs.indices)
        val items = songs.map { it.toMediaItem() }
        player.setMediaItems(items, index, 0L)
        if (player.shuffleModeEnabled) player.shuffleModeEnabled = false
        if (shufflePreference) player.shuffleModeEnabled = true
        if (autoPlay) {
            ensureService()
            player.play()
        } else {
            player.pause()
        }
        syncFromPlayer()
    }

    fun playSong(song: SongEntity, list: List<SongEntity>) {
        _error.value = null
        val index = list.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        setQueue(list, index, autoPlay = true)
    }

    fun play() {
        if (player.mediaItemCount == 0) return
        _error.value = null
        ensureService()
        player.play()
    }

    fun dismissError() {
        _error.value = null
    }

    fun skipError() {
        _error.value = null
        next()
        if (player.mediaItemCount > 0) play()
    }

    fun pause() = player.pause()

    fun toggle() {
        if (player.isPlaying || player.playWhenReady) player.pause() else play()
    }

    fun next() {
        if (player.hasNextMediaItem()) player.seekToNextMediaItem()
        else if (player.repeatMode == Player.REPEAT_MODE_ALL && player.mediaItemCount > 0) {
            player.seekToDefaultPosition(0)
        }
    }

    fun previous() {
        if (player.currentPosition > 3_000) {
            player.seekTo(0)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(0)
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
        _positionMs.value = positionMs.coerceAtLeast(0L)
    }
    fun seekToQueueIndex(index: Int) {
        if (index in 0 until player.mediaItemCount) {
            player.seekTo(index, 0L)
            player.play()
        }
    }

    fun addToQueue(song: SongEntity) {
        songsById = songsById + (song.id to song)
        player.addMediaItem(song.toMediaItem())
        _queue.value = _queue.value + song
    }

    fun playNext(song: SongEntity) {
        songsById = songsById + (song.id to song)
        val index = (player.currentMediaItemIndex + 1).coerceIn(0, player.mediaItemCount)
        player.addMediaItem(index, song.toMediaItem())
        syncQueue()
    }

    fun moveQueueItem(from: Int, to: Int) {
        if (from in 0 until player.mediaItemCount && to in 0 until player.mediaItemCount) {
            player.moveMediaItem(from, to)
            syncQueue()
        }
    }

    fun removeQueueItem(index: Int) {
        if (index in 0 until player.mediaItemCount) {
            player.removeMediaItem(index)
            syncQueue()
        }
    }

    fun clearQueue() {
        player.pause()
        player.clearMediaItems()
        _queue.value = emptyList()
        _currentSong.value = null
        _positionMs.value = 0
        _durationMs.value = 0
    }

    fun toggleShuffle() {
        val enabled = !player.shuffleModeEnabled
        player.shuffleModeEnabled = enabled
        _shuffle.value = enabled
        scope.launch { preferences.setShuffle(enabled) }
    }

    fun cycleRepeat() {
        val next = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        player.repeatMode = next
        _repeatMode.value = next
        scope.launch { preferences.setRepeatMode(next) }
    }

    fun setSpeed(value: Float) {
        val speed = value.coerceIn(0.5f, 2f)
        player.playbackParameters = PlaybackParameters(speed, player.playbackParameters.pitch)
        scope.launch { preferences.setPlaybackSpeed(speed) }
    }

    fun setPitch(value: Float) {
        val pitch = value.coerceIn(0.5f, 2f)
        player.playbackParameters = PlaybackParameters(player.playbackParameters.speed, pitch)
        scope.launch { preferences.setPitch(pitch) }
    }

    fun setVolumeLevel(level: Int) {
        val clamped = level.coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, clamped, 0)
        _volumeLevel.value = clamped
    }

    fun setPauseOnDisconnect(enabled: Boolean) {
        player.setPauseOnDisconnect(enabled)
    }

    private fun updateVolume() {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (current != _volumeLevel.value) _volumeLevel.value = current
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        val millis = minutes.coerceAtLeast(1) * 60_000L
        _sleepEndsAt.value = System.currentTimeMillis() + millis
        _sleepRemaining.value = millis
        sleepTimerJob = scope.launch {
            while (_sleepEndsAt.value > System.currentTimeMillis()) {
                _sleepRemaining.value = _sleepEndsAt.value - System.currentTimeMillis()
                delay(500)
            }
            player.pause()
            _sleepEndsAt.value = 0
            _sleepRemaining.value = 0
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepEndsAt.value = 0
        _sleepRemaining.value = 0
    }

    private fun updateSleepRemaining() {
        if (_sleepEndsAt.value > 0) {
            _sleepRemaining.value = (_sleepEndsAt.value - System.currentTimeMillis()).coerceAtLeast(0)
        }
    }

    suspend fun restoreSessionOnce() = restoreMutex.withLock {
        if (restoreAttempted) return
        if (player.mediaItemCount > 0 || _currentSong.value != null) {
            restoreAttempted = true
            return
        }
        restoreAttempted = true
        if (!preferences.resumeSession.first()) return
        val songs = repository.allSongsOnce()
        if (songs.isEmpty()) return
        val lastUri = preferences.lastSongUri.first()
        if (lastUri.isBlank()) return
        val index = songs.indexOfFirst { it.uri == lastUri }
        if (index < 0) return
        val position = preferences.lastPosition.first()
        setQueue(songs, index, autoPlay = false)
        if (position > 0) player.seekTo(position)
        _positionMs.value = position
    }

    private fun persistProgressIfNeeded() {
        val now = System.currentTimeMillis()
        if (now - lastPersistAt < 10_000) return
        lastPersistAt = now
        val song = _currentSong.value ?: return
        val position = player.currentPosition
        scope.launch {
            preferences.setLastSongUri(song.uri)
            preferences.setLastPosition(position)
        }
    }

    fun pauseAndPersist() {
        player.pause()
        val song = _currentSong.value ?: return
        scope.launch {
            preferences.setLastSongUri(song.uri)
            preferences.setLastPosition(player.currentPosition)
        }
    }

    private fun ensureService() {
        runCatching {
            context.startService(Intent(context, PlaybackService::class.java))
        }
        ensureController()
    }

    /**
     * MediaSessionService solo publica la notificación multimedia cuando hay un
     * MediaController conectado. La app controla el reproductor directamente, pero
     * mantiene esta conexión para que el sistema muestre los controles y mantenga
     * el servicio en primer plano.
     */
    private fun ensureController() {
        if (mediaController?.isConnected == true || mediaControllerFuture != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        mediaControllerFuture = future
        future.addListener({
            runCatching {
                mediaController = future.get()
            }.onFailure {
                mediaControllerFuture = null
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun refreshCurrentSong() {
        val id = _currentSong.value?.id ?: return
        scope.launch {
            repository.songById(id)?.let { refreshed ->
                songsById = songsById + (refreshed.id to refreshed)
                _currentSong.value = refreshed
                _queue.value = _queue.value.map { if (it.id == refreshed.id) refreshed else it }
            }
        }
    }

    private fun SongEntity.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setAlbumArtist(artist)
            .setTrackNumber(if (trackNumber > 0) trackNumber else null)
            .setGenre(genre)
            .setDurationMs(durationMs.takeIf { it > 0 })
            .setArtworkUri(artworkUri?.let { Uri.parse(it) })
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build()
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun darken(color: Int, factor: Float): Int {
        val r = ((color shr 16 and 0xFF) * factor).roundToInt().coerceIn(0, 255)
        val g = ((color shr 8 and 0xFF) * factor).roundToInt().coerceIn(0, 255)
        val b = ((color and 0xFF) * factor).roundToInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun blend(color: Int, other: Int, ratio: Float): Int {
        val r = ((color shr 16 and 0xFF) * (1 - ratio) + (other shr 16 and 0xFF) * ratio).roundToInt()
        val g = ((color shr 8 and 0xFF) * (1 - ratio) + (other shr 8 and 0xFF) * ratio).roundToInt()
        val b = ((color and 0xFF) * (1 - ratio) + (other and 0xFF) * ratio).roundToInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    fun audioSessionId(): Int = player.currentAudioSessionId()
}
