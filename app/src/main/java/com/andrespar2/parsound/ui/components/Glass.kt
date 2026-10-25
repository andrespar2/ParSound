package com.andrespar2.parsound.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.andrespar2.parsound.ui.theme.LocalGlass
import com.andrespar2.parsound.ui.theme.darken
import com.andrespar2.parsound.ui.theme.lighten
import java.io.File

@Composable
fun Modifier.glassModifier(
    cornerRadius: Dp = 24.dp,
    strong: Boolean = false,
    bordered: Boolean = true,
    shape: Shape = RoundedCornerShape(cornerRadius)
): Modifier {
    val glass = LocalGlass.current
    val fill = if (strong) glass.glassStrong else glass.glassFill
    var result = this
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(
                    fill,
                    fill.copy(alpha = fill.alpha * 0.55f)
                )
            )
        )
    if (bordered) {
        result = result.border(
            0.8.dp,
            Brush.verticalGradient(
                listOf(
                    glass.glassBorder,
                    glass.glassBorder.copy(alpha = glass.glassBorder.alpha * 0.25f)
                )
            ),
            shape
        )
    }
    return result
}

@Composable
fun AmbientBackground(
    artworkUri: String? = null,
    showArtwork: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalGlass.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(glass.backgroundStart, glass.backgroundEnd, glass.backgroundStart)
                )
            )
    ) {
        if (showArtwork && glass.coverBackground && !artworkUri.isNullOrBlank() &&
            !glass.performance && !glass.amoled
        ) {
            val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artworkUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (supportsBlur) 0.55f else 0.20f)
                    .blur(glass.blur)
                    .graphicsLayer {
                        scaleX = 1.12f
                        scaleY = 1.12f
                    }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = if (supportsBlur) {
                                listOf(
                                    glass.backgroundStart.copy(alpha = 0.25f),
                                    glass.backgroundStart.copy(alpha = 0.72f),
                                    glass.backgroundStart.copy(alpha = 0.94f)
                                )
                            } else {
                                listOf(
                                    glass.backgroundStart.copy(alpha = 0.58f),
                                    glass.backgroundStart.copy(alpha = 0.86f),
                                    glass.backgroundStart.copy(alpha = 0.97f)
                                )
                            }
                        )
                    )
            )
        }
        if (!glass.performance && !glass.amoled) {
            Box(
                modifier = Modifier
                    .size(360.dp)
                    .align(Alignment.TopEnd)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                glass.accent.copy(alpha = (0.10f + glass.glow * 0.20f).coerceIn(0f, 0.30f)),
                                Color.Transparent
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .align(Alignment.BottomStart)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                glass.accentSoft.copy(alpha = (0.08f + glass.glow * 0.14f).coerceIn(0f, 0.22f)),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        content()
    }
}

@Composable
fun CoverArt(
    artworkUri: String?,
    size: Dp,
    cornerRadius: Dp = size / 7,
    circular: Boolean = false,
    contentDescription: String? = null
) {
    val glass = LocalGlass.current
    val shape = if (circular) CircleShape else RoundedCornerShape(cornerRadius)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        glass.accentSoft.copy(alpha = 0.42f),
                        darken(glass.backgroundEnd, 0.85f)
                    )
                )
            )
            .border(0.8.dp, glass.glassBorder.copy(alpha = 0.6f), shape),
        contentAlignment = Alignment.Center
    ) {
        if (artworkUri.isNullOrBlank()) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                tint = glass.textPrimary.copy(alpha = 0.55f),
                modifier = Modifier.size(size * 0.36f)
            )
        }
        if (!artworkUri.isNullOrBlank()) {
            val model = remember(artworkUri) {
                if (artworkUri.startsWith("file://")) {
                    runCatching { File(android.net.Uri.parse(artworkUri).path ?: "") }.getOrNull()
                } else {
                    artworkUri
                }
            }
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(model)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape),
                onError = {
                    android.util.Log.e(
                        "ParSoundCover",
                        "No se pudo cargar $artworkUri: ${it.result.throwable.message}"
                    )
                },
                onSuccess = {
                }
            )
        }
    }
}

@Composable
fun ParSoundTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    val glass = LocalGlass.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            GlassIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Atrás",
                onClick = onBack
            )
            Spacer(Modifier.width(10.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = glass.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            actions()
        }
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    onClick: () -> Unit
) {
    val glass = LocalGlass.current
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(glass.glassFill)
            .border(0.8.dp, glass.glassBorder.copy(alpha = 0.55f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = tint ?: glass.textPrimary,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primary: Boolean = true,
    enabled: Boolean = true
) {
    val glass = LocalGlass.current
    if (primary) {
        Button(
            onClick = onClick,
            modifier = modifier.height(46.dp),
            enabled = enabled,
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 22.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = glass.accent,
                contentColor = if (glass.isDark) Color(0xFF07111C) else Color.White,
                disabledContainerColor = glass.accent.copy(alpha = 0.35f)
            )
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    } else {
        Row(
            modifier = modifier
                .height(46.dp)
                .glassModifier(23.dp)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = glass.textPrimary, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = glass.textPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val glass = LocalGlass.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = glass.textPrimary,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = glass.accent,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val glass = LocalGlass.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(glass.glassFill)
                .border(0.8.dp, glass.glassBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = glass.accent, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = glass.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = glass.textSecondary,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            PillButton(actionLabel, onAction)
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    strong: Boolean = false,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassModifier(24.dp, strong = strong)
            .padding(padding),
        content = content
    )
}

@Composable
fun RowDivider() {
    val glass = LocalGlass.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(0.6.dp)
            .background(glass.glassBorder.copy(alpha = 0.35f))
    )
}

@Composable
fun AccentGlowBackground() {
    val glass = LocalGlass.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(
                Brush.verticalGradient(
                    listOf(glass.accent.copy(alpha = 0.10f), Color.Transparent)
                )
            )
    )
}
