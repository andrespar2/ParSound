package com.andrespar2.parsound.ui.screen.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import androidx.navigation.NavController
import com.andrespar2.parsound.data.lyrics.Lyrics
import com.andrespar2.parsound.ui.components.AmbientBackground
import com.andrespar2.parsound.ui.components.CoverArt
import com.andrespar2.parsound.ui.components.EmptyState
import com.andrespar2.parsound.ui.components.glassModifier
import com.andrespar2.parsound.ui.components.GlassIconButton
import com.andrespar2.parsound.ui.components.SongOptionsSheet
import com.andrespar2.parsound.ui.navigation.Routes
import com.andrespar2.parsound.ui.theme.LocalGlass
import com.andrespar2.parsound.util.formatDuration
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    navController: NavController,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val song by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.position.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val shuffle by viewModel.shuffle.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val sleepRemaining by viewModel.sleepRemaining.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val lyrics by viewModel.lyrics.collectAsState()
    val playbackError by viewModel.error.collectAsState()
    val glass = LocalGlass.current
    var showOptions by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showTimer by remember { mutableStateOf(false) }
    var showVolume by remember { mutableStateOf(false) }
    val isFavorite = song?.let { favoriteIds.contains(it.id) } == true
    val coverScale by animateFloatAsState(
        targetValue = when {
            !glass.animations -> 1f
            isPlaying -> 1f
            else -> 0.945f
        },
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 220f),
        label = "coverScale"
    )
    val effectiveDuration = duration.takeIf { it > 0 } ?: song?.durationMs ?: 0L

    if (showTimer) {
        SleepTimerDialog(
            remainingMs = sleepRemaining,
            onStart = {
                viewModel.startSleep(it)
                showTimer = false
            },
            onCancel = {
                viewModel.cancelSleep()
                showTimer = false
            },
            onDismiss = { showTimer = false }
        )
    }

    AmbientBackground(artworkUri = song?.artworkUri) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compact = maxHeight < 640.dp
            val veryCompact = maxHeight < 520.dp
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassIconButton(Icons.Default.KeyboardArrowDown, "Cerrar reproductor") {
                        navController.navigateUp()
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "REPRODUCIENDO",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textSecondary
                        )
                        Text(
                            "Biblioteca",
                            style = MaterialTheme.typography.labelLarge,
                            color = glass.textPrimary
                        )
                    }
                    GlassIconButton(Icons.Default.MoreVert, "Más opciones") {
                        showOptions = true
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val canShowLyrics = lyrics.lines.isNotEmpty() && maxHeight >= 260.dp
                        val expandedLyrics = canShowLyrics && maxHeight >= 520.dp
                        val coverFraction = if (canShowLyrics) {
                            if (expandedLyrics) 0.62f else 0.68f
                        } else {
                            0.88f
                        }
                        val coverSize = minOf(
                            maxWidth * 0.80f,
                            maxHeight * coverFraction
                        )
                            .coerceAtLeast(72.dp)
                            .coerceAtMost(420.dp)
                        val lyricsCardHeight = if (expandedLyrics) 124.dp else 82.dp
                        val showLyricsPreview =
                            canShowLyrics && maxHeight - coverSize >= lyricsCardHeight

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier.graphicsLayer {
                                    scaleX = coverScale
                                    scaleY = coverScale
                                }
                            ) {
                                CoverArt(
                                    artworkUri = song?.artworkUri,
                                    size = coverSize,
                                    cornerRadius = if (coverSize > 200.dp) 30.dp else 22.dp,
                                    contentDescription = song?.title
                                )
                            }
                            if (showLyricsPreview) {
                                Spacer(Modifier.height(12.dp))
                                LyricsPreview(
                                    lyrics = lyrics,
                                    positionMs = position,
                                    expanded = expandedLyrics,
                                    onOpen = { showLyrics = true }
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    glass.backgroundStart.copy(alpha = 0.78f),
                                    glass.backgroundStart.copy(alpha = 0.95f)
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(
                                start = if (veryCompact) 16.dp else 24.dp,
                                end = if (veryCompact) 16.dp else 24.dp,
                                top = 8.dp,
                                bottom = if (veryCompact) 4.dp else 8.dp
                            )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    song?.title ?: "Sin reproducción",
                                    style = if (compact) MaterialTheme.typography.titleLarge
                                    else MaterialTheme.typography.headlineMedium,
                                    color = glass.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    song?.artist ?: "Añade música para empezar",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = glass.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            GlassIconButton(
                                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                if (isFavorite) "Quitar de favoritos" else "Añadir a favoritos",
                                onClick = viewModel::toggleFavorite,
                                tint = if (isFavorite) glass.accent else glass.textSecondary,
                                size = if (veryCompact) 38.dp else 44.dp
                            )
                        }

                        Spacer(Modifier.height(if (veryCompact) 2.dp else 6.dp))
                        PlaybackSeekBar(
                            positionMs = position,
                            durationMs = effectiveDuration,
                            enabled = effectiveDuration > 0,
                            onSeek = viewModel::seekTo
                        )

                        Spacer(Modifier.height(if (veryCompact) 2.dp else 6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlayerControl(
                                icon = Icons.Default.Shuffle,
                                description = "Aleatorio",
                                active = shuffle,
                                onClick = viewModel::toggleShuffle
                            )
                            PlayerControl(
                                icon = Icons.Default.SkipPrevious,
                                description = "Anterior",
                                size = if (compact) 30.dp else 34.dp,
                                onClick = viewModel::previous
                            )
                            Box(
                                modifier = Modifier
                                    .size(if (veryCompact) 60.dp else if (compact) 68.dp else 76.dp)
                                    .clip(CircleShape)
                                    .background(glass.accent)
                                    .clickable(onClick = viewModel::toggle),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                    tint = if (glass.isDark) Color(0xFF061019) else Color.White,
                                    modifier = Modifier.size(if (veryCompact) 32.dp else 40.dp)
                                )
                            }
                            PlayerControl(
                                icon = Icons.Default.SkipNext,
                                description = "Siguiente",
                                size = if (compact) 30.dp else 34.dp,
                                onClick = viewModel::next
                            )
                            PlayerControl(
                                icon = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                description = "Repetir",
                                active = repeatMode != Player.REPEAT_MODE_OFF,
                                onClick = viewModel::cycleRepeat
                            )
                        }

                        Spacer(Modifier.height(if (veryCompact) 0.dp else 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SecondaryAction(
                                Icons.Default.Subject,
                                "Letra",
                                showLabel = !veryCompact
                            ) { showLyrics = true }
                            SecondaryAction(
                                Icons.Default.QueueMusic,
                                "Cola",
                                showLabel = !veryCompact
                            ) { navController.navigate(Routes.QUEUE) }
                            SecondaryAction(
                                Icons.Default.Equalizer,
                                "Efectos",
                                showLabel = !veryCompact
                            ) { navController.navigate(Routes.EFFECTS) }
                            SecondaryAction(
                                Icons.Default.Timer,
                                if (sleepRemaining > 0) formatDuration(sleepRemaining) else "Temporizador",
                                showLabel = !veryCompact
                            ) { showTimer = true }
                            SecondaryAction(
                                Icons.AutoMirrored.Filled.VolumeUp,
                                "Volumen",
                                showLabel = !veryCompact
                            ) { showVolume = true }
                        }
                    }
                }
            }
        }
    }

    if (playbackError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            containerColor = glass.backgroundEnd,
            title = { Text("No se pudo reproducir", color = glass.textPrimary) },
            text = {
                Text(
                    playbackError ?: "",
                    color = glass.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.skipError() }) {
                    Text("Siguiente", color = glass.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissError() }) {
                    Text("Cerrar", color = glass.textSecondary)
                }
            }
        )
    }

    if (showVolume) {
        VolumeDialog(
            volume = volume,
            maxVolume = viewModel.maxVolume,
            onChange = viewModel::setVolume,
            onDismiss = { showVolume = false }
        )
    }

    if (showOptions && song != null) {
        SongOptionsSheet(
            song = song!!,
            onDismiss = { showOptions = false },
            onGoToAlbum = { navController.navigate(Routes.album(song!!.albumKey)) },
            onGoToArtist = { navController.navigate(Routes.artist(song!!.artist)) },
            onShowLyrics = { showLyrics = true }
        )
    }

    if (showLyrics) {
        LyricsSheet(
            title = song?.title.orEmpty(),
            artist = song?.artist.orEmpty(),
            lyrics = lyrics,
            positionMs = position,
            onSeek = viewModel::seekTo,
            onDismiss = { showLyrics = false }
        )
    }
}

@Composable
private fun LyricsPreview(
    lyrics: com.andrespar2.parsound.data.lyrics.Lyrics,
    positionMs: Long,
    expanded: Boolean,
    onOpen: () -> Unit
) {
    val glass = LocalGlass.current
    val index = lyrics.currentIndex(positionMs)
    val current = lyrics.lines.getOrNull(index) ?: return
    val previous = lyrics.lines.getOrNull(index - 1)
    val next = lyrics.lines.getOrNull(index + 1)
    val next2 = lyrics.lines.getOrNull(index + 2)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .glassModifier(20.dp)
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "LETRA",
                style = MaterialTheme.typography.labelSmall,
                color = glass.accent,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            Text(
                "Toca para ver completa",
                style = MaterialTheme.typography.labelSmall,
                color = glass.textSecondary
            )
        }
        Spacer(Modifier.height(4.dp))
        if (expanded && previous != null) {
            Text(
                previous.text,
                style = MaterialTheme.typography.bodySmall,
                color = glass.textSecondary.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
        }
        Text(
            current.text,
            style = MaterialTheme.typography.titleMedium,
            color = glass.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (next != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                next.text,
                style = MaterialTheme.typography.bodySmall,
                color = glass.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (expanded && next2 != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                next2.text,
                style = MaterialTheme.typography.bodySmall,
                color = glass.textSecondary.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PlaybackSeekBar(
    positionMs: Long,
    durationMs: Long,
    enabled: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlass.current
    var widthPx by remember { mutableIntStateOf(1) }
    var dragging by remember { mutableStateOf(false) }
    var previewMs by remember { mutableLongStateOf(positionMs) }
    LaunchedEffect(positionMs, dragging) {
        if (!dragging) previewMs = positionMs
    }
    val safeDuration = durationMs.coerceAtLeast(1L)
    val fraction = (previewMs.toFloat() / safeDuration).coerceIn(0f, 1f)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                }
                .pointerInput(enabled, durationMs) {
                    if (!enabled || durationMs <= 0L) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        dragging = true
                        fun update(x: Float) {
                            val clamped = (x / widthPx).coerceIn(0f, 1f)
                            previewMs = (clamped * durationMs).toLong().coerceIn(0L, durationMs)
                        }
                        update(down.position.x)
                        down.consume()
                        drag(down.id) { change ->
                            update(change.position.x)
                            change.consume()
                        }
                        dragging = false
                        onSeek(previewMs)
                    }
                }
        ) {
            val trackHeight = 4.dp.toPx()
            val centerY = size.height / 2f
            val corner = CornerRadius(trackHeight / 2f)
            drawRoundRect(
                color = glass.textSecondary.copy(alpha = 0.22f),
                topLeft = Offset(0f, centerY - trackHeight / 2f),
                size = Size(size.width, trackHeight),
                cornerRadius = corner
            )
            if (fraction > 0f) {
                drawRoundRect(
                    color = glass.accent,
                    topLeft = Offset(0f, centerY - trackHeight / 2f),
                    size = Size(size.width * fraction, trackHeight),
                    cornerRadius = corner
                )
            }
            val thumbX = size.width * fraction
            drawCircle(
                color = glass.accent.copy(alpha = 0.18f),
                radius = 10.dp.toPx(),
                center = Offset(thumbX, centerY)
            )
            drawCircle(
                color = glass.accent,
                radius = 6.dp.toPx(),
                center = Offset(thumbX, centerY)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = 6.dp.toPx(),
                center = Offset(thumbX, centerY),
                style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                formatDuration(previewMs),
                style = MaterialTheme.typography.labelMedium,
                color = glass.textPrimary
            )
            Text(
                formatDuration(durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = glass.textSecondary
            )
        }
    }
}

@Composable
private fun PlayerControl(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    active: Boolean = false,
    size: Dp = 26.dp
) {
    val glass = LocalGlass.current
    val tint by animateColorAsState(
        targetValue = if (active) glass.accent else glass.textPrimary,
        label = "controlTint"
    )
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(size))
        if (active) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(glass.accent)
            )
        }
    }
}

@Composable
private fun SecondaryAction(
    icon: ImageVector,
    label: String,
    showLabel: Boolean = true,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val glass = LocalGlass.current
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = if (showLabel) 4.dp else 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (enabled) glass.textSecondary else glass.textSecondary.copy(alpha = 0.35f),
            modifier = Modifier.size(21.dp)
        )
        if (showLabel) {
            Spacer(Modifier.height(3.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) glass.textSecondary else glass.textSecondary.copy(alpha = 0.35f),
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsSheet(
    title: String,
    artist: String,
    lyrics: Lyrics,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalGlass.current
    val listState = rememberLazyListState()
    val currentIndex = lyrics.currentIndex(positionMs)
    val dragged by listState.interactionSource.collectIsDraggedAsState()
    var autoScroll by remember { mutableStateOf(true) }

    LaunchedEffect(dragged) {
        if (dragged) autoScroll = false
    }
    LaunchedEffect(currentIndex, autoScroll) {
        if (autoScroll && lyrics.synced && lyrics.lines.isNotEmpty()) {
            listState.animateScrollToItem((currentIndex - 2).coerceAtLeast(0))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = glass.backgroundEnd,
        contentColor = glass.textPrimary,
        dragHandle = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.92f)
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "LETRA",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.accent
                    )
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        color = glass.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary,
                        maxLines = 1
                    )
                }
                if (!autoScroll) {
                    Text(
                        "Volver a la actual",
                        style = MaterialTheme.typography.labelLarge,
                        color = glass.accent,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                autoScroll = true
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            if (lyrics.lines.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Subject,
                    title = "Sin letra disponible",
                    message = "ParSound busca letras LRC junto al archivo y letras incrustadas en los metadatos (FLAC, MP3, OGG, M4A o WAV). Si esta canción no las incluye, puedes añadir un .lrc con el mismo nombre."
                )
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 680.dp),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
                    ) {
                        itemsIndexed(lyrics.lines) { index, line ->
                            val active = lyrics.synced && index == currentIndex
                            Text(
                                line.text,
                                style = if (active) MaterialTheme.typography.titleLarge
                                else MaterialTheme.typography.bodyLarge,
                                color = if (active) glass.accent
                                else glass.textPrimary.copy(alpha = 0.68f),
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Start,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = lyrics.synced) {
                                        autoScroll = true
                                        onSeek(line.timeMs.coerceAtLeast(0))
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VolumeDialog(
    volume: Int,
    maxVolume: Int,
    onChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalGlass.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = glass.backgroundEnd,
        title = { Text("Volumen", color = glass.textPrimary) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = glass.accent
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "${(volume * 100 / maxVolume.coerceAtLeast(1))} %",
                        color = glass.textPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Slider(
                    value = volume.toFloat(),
                    onValueChange = { onChange(it.roundToInt()) },
                    valueRange = 0f..maxVolume.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = glass.accent,
                        activeTrackColor = glass.accent,
                        inactiveTrackColor = glass.textSecondary.copy(alpha = 0.2f)
                    )
                )
                Text(
                    "Controla el volumen multimedia del dispositivo. Los botones físicos siguen funcionando.",
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar", color = glass.accent) }
        }
    )
}

@Composable
private fun SleepTimerDialog(
    remainingMs: Long,
    onStart: (Int) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalGlass.current
    val presets = listOf(5, 10, 15, 30, 45, 60, 90, 120)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = glass.backgroundEnd,
        title = { Text("Temporizador de apagado", color = glass.textPrimary) },
        text = {
            Column {
                Text(
                    if (remainingMs > 0) "Tiempo restante: ${formatDuration(remainingMs)}"
                    else "La reproducción se detendrá cuando termine el tiempo.",
                    color = glass.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                presets.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { minutes ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(glass.glassFill)
                                    .clickable { onStart(minutes) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$minutes min",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = glass.textPrimary
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            if (remainingMs > 0) {
                TextButton(onClick = onCancel) { Text("Cancelar temporizador", color = glass.accent) }
            } else {
                TextButton(onClick = onDismiss) { Text("Cerrar", color = glass.textSecondary) }
            }
        }
    )
}
