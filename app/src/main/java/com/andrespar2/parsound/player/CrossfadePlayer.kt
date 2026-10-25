package com.andrespar2.parsound.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.SimpleBasePlayer.MediaItemData
import androidx.media3.common.SimpleBasePlayer.State
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlin.math.cos
import kotlin.math.sin

/**
 * Reproductor con dos motores ExoPlayer. El motor activo reproduce la pista actual
 * mientras el segundo prepara la siguiente para poder solapar audio de verdad
 * (crossfade) sin cortes ni duplicaciones.
 */
@UnstableApi
class CrossfadePlayer(context: Context) : SimpleBasePlayer(Looper.getMainLooper()) {

    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val engines = arrayOf(createEngine(), createEngine())

    private var activeEngine = 0
    private var incomingEngine = -1
    private var items: List<MediaItem> = emptyList()
    private var itemData: List<MediaItemData> = emptyList()
    private var currentIndex = 0
    private var playWhenReadyState = false
    private var repeatModeState = Player.REPEAT_MODE_OFF
    private var shuffleState = false
    private var playbackParametersState = PlaybackParameters.DEFAULT
    private var volumeState = 1f
    private var crossfadeMs = 0
    private var crossfading = false
    private var prepared = false
    private var released = false
    private var lastError: PlaybackException? = null
    private var fadeRunnable: Runnable? = null
    private var fadeStartTime = 0L
    private var fadeDurationMs = 0L
    private var fadeOutIndex = 0
    private var fadeInIndex = -1
    private var preShuffleItems: List<MediaItem>? = null
    private var pendingCrossfadeIndex = -1
    private var latestAudioSessionId = 0
    private var audioSessionListener: ((Int) -> Unit)? = null
    private var stateListener: (() -> Unit)? = null

    private val ticker = object : Runnable {
        override fun run() {
            if (released) return
            if (playWhenReadyState && !crossfading) {
                maybeStartCrossfade()
                invalidateState()
            }
            handler.postDelayed(this, 180L)
        }
    }

    private val engineListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val session = currentEngine().audioSessionId
            if (session != 0 && session != latestAudioSessionId) {
                latestAudioSessionId = session
                audioSessionListener?.invoke(session)
            }
            if (playbackState == Player.STATE_ENDED) {
                if (crossfading) return
                onTrackEnded()
            } else if (playbackState == Player.STATE_READY) {
                if (crossfading && fadeRunnable == null &&
                    incomingEngine in engines.indices &&
                    engines[incomingEngine].playbackState == Player.STATE_READY
                ) {
                    // El motor entrante ya está listo: se inicia el fundido.
                    startFadeAnimation()
                }
            }
            stateListener?.invoke()
            invalidateState()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            stateListener?.invoke()
            invalidateState()
        }

        override fun onPlayerError(error: PlaybackException) {
            if (crossfading && incomingEngine >= 0 && currentEngine() === engines[incomingEngine]) {
                cancelCrossfadeKeepActive()
                stateListener?.invoke()
                invalidateState()
                return
            }
            lastError = error
            playWhenReadyState = false
            stateListener?.invoke()
            invalidateState()
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            playbackParametersState = playbackParameters
            invalidateState()
        }
    }

    init {
        engines.forEach { engine ->
            engine.addListener(engineListener)
            engine.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            engine.setHandleAudioBecomingNoisy(true)
            engine.volume = 0f
        }
        engines[0].volume = volumeState
        handler.post(ticker)
    }

    fun setAudioSessionListener(listener: (Int) -> Unit) {
        audioSessionListener = listener
        val session = currentEngine().audioSessionId
        if (session != 0) listener(session)
    }

    fun setStateListener(listener: () -> Unit) {
        stateListener = listener
    }

    fun setCrossfadeMillis(value: Int) {
        crossfadeMs = value.coerceIn(0, 12_000)
    }

    fun setPauseOnDisconnect(enabled: Boolean) {
        engines.forEach { it.setHandleAudioBecomingNoisy(enabled) }
    }

    fun setDeviceVolume(value: Float) {
        volumeState = value.coerceIn(0f, 1f)
        applyVolumes()
        invalidateState()
    }

    fun currentAudioSessionId(): Int = currentEngine().audioSessionId

    fun currentEngineIsPlaying(): Boolean = currentEngine().isPlaying

    fun playNextWithFade(forceCrossfade: Boolean = false) {
        val next = nextIndex()
        if (next < 0) return
        goToIndex(next, 0, if (forceCrossfade) crossfadeMs else 0)
    }

    fun restartCurrentWithFade() {
        goToIndex(currentIndex, 0, crossfadeMs)
    }

    private fun createEngine(): ExoPlayer =
        ExoPlayer.Builder(appContext).setWakeMode(C.WAKE_MODE_LOCAL).build()

    private fun currentEngine(): ExoPlayer = engines[activeEngine]

    private fun applyVolumes() {
        if (crossfading) return
        engines.forEachIndexed { index, engine ->
            engine.volume = if (index == activeEngine) volumeState else 0f
        }
    }

    private fun buildItemData(list: List<MediaItem>): List<MediaItemData> =
        list.map { item ->
            val durationMs = item.mediaMetadata.durationMs ?: 0L
            MediaItemData.Builder(item.mediaId.ifBlank { item.toString() })
                .setMediaItem(item)
                .setMediaMetadata(item.mediaMetadata)
                .setDurationUs(if (durationMs > 0) durationMs * 1000 else 0L)
                .setIsSeekable(true)
                .setIsDynamic(false)
                .setDefaultPositionUs(0)
                .setPositionInFirstPeriodUs(0)
                .build()
        }

    override fun getState(): State {
        val engine = if (crossfading && incomingEngine in engines.indices) {
            engines[incomingEngine]
        } else {
            currentEngine()
        }
        val playbackState = when {
            items.isEmpty() -> Player.STATE_IDLE
            released -> Player.STATE_IDLE
            engine.playbackState == Player.STATE_IDLE && playWhenReadyState -> Player.STATE_BUFFERING
            else -> engine.playbackState
        }
        val builder = State.Builder()
            .setAvailableCommands(AVAILABLE_COMMANDS)
            .setPlayWhenReady(playWhenReadyState, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(playbackState)
            .setPlaybackSuppressionReason(Player.PLAYBACK_SUPPRESSION_REASON_NONE)
            .setPlayerError(lastError ?: engine.playerError)
            .setRepeatMode(repeatModeState)
            .setShuffleModeEnabled(shuffleState)
            .setIsLoading(engine.isLoading)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .setMaxSeekToPreviousPositionMs(3_000)
            .setPlaybackParameters(playbackParametersState)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setVolume(volumeState)
            .setPlaylist(itemData)
            .setCurrentMediaItemIndex(currentIndex.coerceAtLeast(0))
            .setContentPositionMs { engine.currentPosition.coerceAtLeast(0L) }
            .setContentBufferedPositionMs { engine.bufferedPosition.coerceAtLeast(0L) }
            .setTotalBufferedDurationMs { engine.totalBufferedDuration.coerceAtLeast(0L) }
        return builder.build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        if (released) return Futures.immediateVoidFuture()
        if (crossfading && !playWhenReady) finishCrossfade()
        playWhenReadyState = playWhenReady
        if (playWhenReady) {
            if (items.isEmpty()) {
                playWhenReadyState = false
                return Futures.immediateVoidFuture()
            }
            lastError = null
            if (!prepared) prepareActive()
            if (currentEngine().playbackState == Player.STATE_ENDED) {
                restartCurrent()
            }
            currentEngine().play()
            startTicker()
        } else {
            engines.forEach { it.pause() }
            persistPosition()
        }
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handlePrepare(): ListenableFuture<*> {
        if (items.isNotEmpty() && !prepared) prepareActive()
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        playWhenReadyState = false
        crossfading = false
        incomingEngine = -1
        cancelFadeCallbacks()
        engines.forEach {
            it.pause()
            it.volume = 0f
        }
        currentEngine().volume = volumeState
        prepared = false
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleRelease(): ListenableFuture<*> {
        released = true
        handler.removeCallbacks(ticker)
        cancelFadeCallbacks()
        engines.forEach {
            runCatching { it.release() }
        }
        stateListener?.invoke()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetRepeatMode(repeatMode: Int): ListenableFuture<*> {
        repeatModeState = repeatMode
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean): ListenableFuture<*> {
        if (shuffleState == shuffleModeEnabled) return Futures.immediateVoidFuture()
        val currentItem = items.getOrNull(currentIndex)
        if (shuffleModeEnabled) {
            preShuffleItems = items
            val rest = items.filterIndexed { index, _ -> index != currentIndex }.shuffled()
            items = listOfNotNull(currentItem) + rest
            currentIndex = 0
        } else {
            val base = preShuffleItems
            if (base != null && base.size == items.size) {
                val currentId = currentItem?.mediaId
                items = base
                currentIndex = items.indexOfFirst { it.mediaId == currentId }.coerceAtLeast(0)
            }
        }
        itemData = buildItemData(items)
        shuffleState = shuffleModeEnabled
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetPlaybackParameters(playbackParameters: PlaybackParameters): ListenableFuture<*> {
        playbackParametersState = playbackParameters
        engines.forEach { it.playbackParameters = playbackParameters }
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetVolume(volume: Float): ListenableFuture<*> {
        volumeState = volume.coerceIn(0f, 1f)
        applyVolumes()
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetMediaItems(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): ListenableFuture<*> {
        cancelFadeCallbacks()
        crossfading = false
        incomingEngine = -1
        preShuffleItems = null
        items = mediaItems
        itemData = buildItemData(items)
        currentIndex = if (items.isEmpty()) 0 else startIndex.coerceIn(items.indices)
        lastError = null
        prepared = false
        engines.forEach { it.pause() }
        if (items.isEmpty()) {
            playWhenReadyState = false
            invalidateState()
            return Futures.immediateVoidFuture()
        }
        prepareActive(startPositionMs)
        invalidateState()
        if (playWhenReadyState) {
            currentEngine().play()
            startTicker()
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleAddMediaItems(index: Int, mediaItems: List<MediaItem>): ListenableFuture<*> {
        if (mediaItems.isEmpty()) return Futures.immediateVoidFuture()
        val insertAt = index.coerceIn(0, items.size)
        items = items.toMutableList().apply { addAll(insertAt, mediaItems) }
        if (insertAt <= currentIndex) currentIndex += mediaItems.size
        itemData = buildItemData(items)
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleMoveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int): ListenableFuture<*> {
        if (items.isEmpty()) return Futures.immediateVoidFuture()
        val currentId = items.getOrNull(currentIndex)?.mediaId
        val mutable = items.toMutableList()
        val moved = mutable.subList(fromIndex, toIndex).toList()
        repeat(toIndex - fromIndex) { mutable.removeAt(fromIndex) }
        mutable.addAll(newIndex.coerceIn(0, mutable.size), moved)
        items = mutable
        currentIndex = items.indexOfFirst { it.mediaId == currentId }.coerceAtLeast(0)
        itemData = buildItemData(items)
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleRemoveMediaItems(fromIndex: Int, toIndex: Int): ListenableFuture<*> {
        if (items.isEmpty()) return Futures.immediateVoidFuture()
        val removingCurrent = currentIndex in fromIndex until toIndex
        val currentId = items.getOrNull(currentIndex)?.mediaId
        items = items.toMutableList().apply { subList(fromIndex, toIndex).clear() }
        itemData = buildItemData(items)
        if (items.isEmpty()) {
            playWhenReadyState = false
            engines.forEach { it.pause() }
            currentIndex = 0
        } else if (removingCurrent) {
            currentIndex = fromIndex.coerceIn(0, items.lastIndex)
            prepareActive()
            if (playWhenReadyState) currentEngine().play()
        } else {
            currentIndex = items.indexOfFirst { it.mediaId == currentId }.coerceAtLeast(0)
        }
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleReplaceMediaItems(
        fromIndex: Int,
        toIndex: Int,
        mediaItems: List<MediaItem>
    ): ListenableFuture<*> {
        val currentId = items.getOrNull(currentIndex)?.mediaId
        val mutable = items.toMutableList()
        mutable.subList(fromIndex, toIndex).clear()
        mutable.addAll(fromIndex, mediaItems)
        items = mutable
        itemData = buildItemData(items)
        currentIndex = items.indexOfFirst { it.mediaId == currentId }.coerceAtLeast(0)
        if (items.isEmpty()) playWhenReadyState = false
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(
        mediaItemIndex: Int,
        positionMs: Long,
        seekCommand: Int
    ): ListenableFuture<*> {
        if (items.isEmpty()) return Futures.immediateVoidFuture()
        if (crossfading) finishCrossfade()
        val target = mediaItemIndex.coerceIn(items.indices)
        lastError = null
        if (target != currentIndex) {
            val fade = if (playWhenReadyState && crossfadeMs > 0) crossfadeMs else 0
            goToIndex(target, positionMs, fade)
        } else {
            currentEngine().seekTo(positionMs.coerceAtLeast(0))
            if (!prepared) prepareActive(positionMs)
            invalidateState()
        }
        return Futures.immediateVoidFuture()
    }

    private fun prepareActive(startPositionMs: Long = 0) {
        val item = items.getOrNull(currentIndex) ?: return
        val engine = currentEngine()
        engine.setMediaItem(item, startPositionMs.coerceAtLeast(0))
        engine.playbackParameters = playbackParametersState
        engine.volume = volumeState
        engine.prepare()
        prepared = true
    }

    private fun startTicker() {
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    private fun restartCurrent() {
        if (items.isEmpty()) return
        cancelCrossfadeKeepActive()
        currentEngine().seekTo(0)
        if (!prepared) prepareActive()
        currentEngine().play()
        playWhenReadyState = true
        invalidateState()
    }

    private fun maybeStartCrossfade() {
        if (crossfadeMs <= 0 || crossfading || items.isEmpty() || !prepared) return
        val engine = currentEngine()
        if (engine.playbackState != Player.STATE_READY) return
        val duration = engine.duration
        if (duration <= 0 || duration == C.TIME_UNSET) return
        if (duration < crossfadeMs + 3_000L) return
        val remaining = duration - engine.currentPosition
        if (remaining > crossfadeMs || remaining <= 0) return
        val next = nextIndex()
        if (next < 0) return
        beginCrossfade(next)
    }

    private fun nextIndex(): Int {
        if (items.isEmpty()) return -1
        if (repeatModeState == Player.REPEAT_MODE_ONE) return currentIndex
        if (currentIndex + 1 < items.size) return currentIndex + 1
        if (repeatModeState == Player.REPEAT_MODE_ALL) return 0
        return -1
    }

    private fun beginCrossfade(targetIndex: Int) {
        if (crossfading || targetIndex !in items.indices) return
        val target = items[targetIndex]
        val engine = engines[1 - activeEngine]
        pendingCrossfadeIndex = targetIndex
        incomingEngine = 1 - activeEngine
        crossfading = true
        engine.setMediaItem(target, 0)
        engine.playbackParameters = playbackParametersState
        engine.volume = 0f
        engine.prepare()
        engine.play()
        handler.postDelayed({
            if (crossfading && engine.playbackState == Player.STATE_READY && fadeRunnable == null) {
                startFadeAnimation()
            }
        }, 120)
        // Si el motor entrante no está listo en 4 s se cancela de forma segura.
        handler.postDelayed({
            if (crossfading && fadeRunnable == null && engine.playbackState != Player.STATE_READY) {
                cancelCrossfadeKeepActive()
            }
        }, 4_000)
    }

    private fun startFadeAnimation() {
        if (!crossfading || fadeRunnable != null) return
        val incomingIdx = incomingEngine
        if (incomingIdx < 0) return
        val incoming = engines[incomingIdx]
        if (incoming.playbackState != Player.STATE_READY) return
        currentIndex = pendingCrossfadeIndex.coerceIn(items.indices)
        fadeOutIndex = activeEngine
        fadeInIndex = incomingIdx
        fadeStartTime = SystemClock.elapsedRealtime()
        fadeDurationMs = crossfadeMs.toLong().coerceAtLeast(400L)
        incoming.volume = 0f
        incoming.play()
        val step = object : Runnable {
            override fun run() {
                if (!crossfading || fadeInIndex < 0) return
                val elapsed = SystemClock.elapsedRealtime() - fadeStartTime
                val t = (elapsed.toFloat() / fadeDurationMs).coerceIn(0f, 1f)
                val old = engines[fadeOutIndex]
                val next = engines[fadeInIndex]
                old.volume = (volumeState * cos(t * Math.PI / 2)).toFloat().coerceIn(0f, 1f)
                next.volume = (volumeState * sin(t * Math.PI / 2)).toFloat().coerceIn(0f, 1f)
                if (t < 1f) {
                    handler.postDelayed(this, 16L)
                } else {
                    fadeRunnable = null
                    finishCrossfade()
                }
            }
        }
        fadeRunnable = step
        handler.post(step)
        invalidateState()
    }

    private fun cancelFadeCallbacks() {
        fadeRunnable?.let { handler.removeCallbacks(it) }
        fadeRunnable = null
        fadeInIndex = -1
    }

    private fun finishCrossfade() {
        if (!crossfading) return
        val incomingIdx = incomingEngine
        cancelFadeCallbacks()
        val old = engines[activeEngine]
        old.pause()
        old.volume = 0f
        if (incomingIdx in engines.indices) {
            activeEngine = incomingIdx
            engines[activeEngine].volume = volumeState
            if (playWhenReadyState) engines[activeEngine].play()
        }
        incomingEngine = -1
        crossfading = false
        prepared = true
        stateListener?.invoke()
        invalidateState()
    }

    private fun cancelCrossfadeKeepActive() {
        val incomingIdx = incomingEngine
        cancelFadeCallbacks()
        if (incomingIdx in engines.indices) {
            engines[incomingIdx].pause()
            engines[incomingIdx].volume = 0f
        }
        incomingEngine = -1
        crossfading = false
        currentEngine().volume = volumeState
        invalidateState()
    }

    private fun onTrackEnded() {
        if (!playWhenReadyState) {
            stateListener?.invoke()
            invalidateState()
            return
        }
        if (repeatModeState == Player.REPEAT_MODE_ONE) {
            goToIndex(currentIndex, 0, crossfadeMs)
            return
        }
        val next = nextIndex()
        if (next >= 0) {
            goToIndex(next, 0, crossfadeMs)
        } else {
            playWhenReadyState = false
            stateListener?.invoke()
            invalidateState()
        }
    }

    private fun goToIndex(index: Int, positionMs: Long, fadeMs: Int) {
        if (index !in items.indices) return
        if (fadeMs > 0 && playWhenReadyState && currentEngine().isPlaying) {
            finishCrossfade()
            pendingCrossfadeIndex = index
            currentIndex = index
            beginCrossfade(index)
            return
        }
        cancelCrossfadeKeepActive()
        cancelFadeCallbacks()
        val engine = currentEngine()
        engines.forEach { it.pause() }
        currentIndex = index
        engine.setMediaItem(items[index], positionMs.coerceAtLeast(0))
        engine.playbackParameters = playbackParametersState
        engine.volume = volumeState
        engine.prepare()
        prepared = true
        if (playWhenReadyState) engine.play()
        invalidateState()
    }

    private fun persistPosition() {
        stateListener?.invoke()
    }

    private val AVAILABLE_COMMANDS: Player.Commands
        get() = Player.Commands.Builder()
            .addAll(
                Player.COMMAND_PLAY_PAUSE,
                Player.COMMAND_PREPARE,
                Player.COMMAND_STOP,
                Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
                Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_MEDIA_ITEM,
                Player.COMMAND_SEEK_BACK,
                Player.COMMAND_SEEK_FORWARD,
                Player.COMMAND_SET_SPEED_AND_PITCH,
                Player.COMMAND_SET_SHUFFLE_MODE,
                Player.COMMAND_SET_REPEAT_MODE,
                Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                Player.COMMAND_GET_TIMELINE,
                Player.COMMAND_GET_METADATA,
                Player.COMMAND_SET_MEDIA_ITEM,
                Player.COMMAND_CHANGE_MEDIA_ITEMS,
                Player.COMMAND_GET_AUDIO_ATTRIBUTES,
                Player.COMMAND_GET_VOLUME,
                Player.COMMAND_SET_VOLUME,
                Player.COMMAND_RELEASE
            )
            .build()
}
