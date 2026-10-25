package com.andrespar2.parsound.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.andrespar2.parsound.data.local.AlbumSummary
import com.andrespar2.parsound.data.local.ArtistSummary
import com.andrespar2.parsound.data.local.PlaylistSummary
import com.andrespar2.parsound.data.local.SongEntity
import com.andrespar2.parsound.util.songsLabel
import com.andrespar2.parsound.util.foldersLabel
import com.andrespar2.parsound.ui.theme.LocalGlass
import com.andrespar2.parsound.util.formatDuration

@Composable
fun SongRow(
    song: SongEntity,
    isFavorite: Boolean,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    showNumber: Int? = null
) {
    val glass = LocalGlass.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showNumber != null) {
            Box(modifier = Modifier.width(26.dp), contentAlignment = Alignment.Center) {
                if (isCurrent) {
                    Icon(
                        Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = glass.accent,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        "$showNumber",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
        }
        CoverArt(song.artworkUri, 46.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isCurrent) glass.accent else glass.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                buildString {
                    append(song.artist)
                    if (song.album.isNotBlank() && song.album != "Álbum desconocido") {
                        append(" · ")
                        append(song.album)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = glass.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            formatDuration(song.durationMs),
            style = MaterialTheme.typography.labelSmall,
            color = glass.textSecondary
        )
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(34.dp)) {
            Icon(
                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "Quitar de favoritos" else "Añadir a favoritos",
                tint = if (isFavorite) glass.accent else glass.textSecondary.copy(alpha = 0.75f),
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = onMore, modifier = Modifier.size(34.dp)) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Más opciones",
                tint = glass.textSecondary,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
fun MiniPlayer(
    song: SongEntity,
    isPlaying: Boolean,
    positionMs: Long,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlass.current
    val progress = if (song.durationMs > 0) (positionMs.toFloat() / song.durationMs).coerceIn(0f, 1f) else 0f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .glassModifier(22.dp, strong = true, shape = RoundedCornerShape(22.dp))
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoverArt(song.artworkUri, 44.dp, cornerRadius = 12.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = glass.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onToggle, modifier = Modifier.size(40.dp)) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    tint = glass.textPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Siguiente",
                    tint = glass.textPrimary,
                    modifier = Modifier.size(23.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(progress)
                .height(2.dp)
                .background(glass.accent, RoundedCornerShape(bottomStart = 22.dp))
        )
    }
}

data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun GlassBottomBar(
    destinations: List<BottomDestination>,
    selectedRoute: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlass.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .glassModifier(26.dp, strong = true),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        destinations.forEach { destination ->
            val selected = destination.route == selectedRoute
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onSelect(destination.route) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (selected) glass.accent.copy(alpha = 0.17f) else Color.Transparent)
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        destination.icon,
                        contentDescription = destination.label,
                        tint = if (selected) glass.accent else glass.textSecondary,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    destination.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) glass.accent else glass.textSecondary,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun AlbumCard(
    album: AlbumSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 138.dp
) {
    val glass = LocalGlass.current
    Column(
        modifier = modifier
            .width(size)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(bottom = 4.dp)
    ) {
        CoverArt(album.artworkUri, size, cornerRadius = 20.dp)
        Spacer(Modifier.height(8.dp))
        Text(
            album.album,
            style = MaterialTheme.typography.titleSmall,
            color = glass.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = glass.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ArtistCard(
    artist: ArtistSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp
) {
    val glass = LocalGlass.current
    Column(
        modifier = modifier
            .width(size)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CoverArt(artist.artworkUri, size, circular = true)
        Spacer(Modifier.height(8.dp))
        Text(
            artist.artist,
            style = MaterialTheme.typography.titleSmall,
            color = glass.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            songsLabel(artist.songCount),
            style = MaterialTheme.typography.bodySmall,
            color = glass.textSecondary,
            maxLines = 1
        )
    }
}

@Composable
fun PlaylistCard(
    playlist: PlaylistSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlass.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(glass.accent.copy(alpha = 0.15f))
                .border(0.7.dp, glass.glassBorder, RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.QueueMusic,
                contentDescription = null,
                tint = glass.accent,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                playlist.name,
                style = MaterialTheme.typography.titleSmall,
                color = glass.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                songsLabel(playlist.songCount),
                style = MaterialTheme.typography.bodySmall,
                color = glass.textSecondary
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = glass.textSecondary
        )
    }
}
