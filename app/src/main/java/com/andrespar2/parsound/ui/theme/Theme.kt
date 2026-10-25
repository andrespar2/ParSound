package com.andrespar2.parsound.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.andrespar2.parsound.player.PaletteColors

data class GlassTokens(
    val isDark: Boolean,
    val backgroundStart: Color,
    val backgroundEnd: Color,
    val accent: Color,
    val accentSoft: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val glassFill: Color,
    val glassStrong: Color,
    val glassBorder: Color,
    val glassHighlight: Color,
    val glassAlpha: Float,
    val glow: Float,
    val blur: Dp,
    val animations: Boolean,
    val performance: Boolean,
    val amoled: Boolean,
    val coverBackground: Boolean
)

val LocalGlass = staticCompositionLocalOf {
    GlassTokens(
        isDark = true,
        backgroundStart = Color(0xFF0A0C12),
        backgroundEnd = Color(0xFF141824),
        accent = Color(0xFF8AB4F8),
        accentSoft = Color(0xFF5B8DEF),
        textPrimary = Color(0xFFF2F4F8),
        textSecondary = Color(0xFFB8C0D0),
        glassFill = Color(0x14FFFFFF),
        glassStrong = Color(0x24FFFFFF),
        glassBorder = Color(0x33FFFFFF),
        glassHighlight = Color(0x22FFFFFF),
        glassAlpha = 0.55f,
        glow = 0.45f,
        blur = 30.dp,
        animations = true,
        performance = false,
        amoled = false,
        coverBackground = true
    )
}

@Composable
fun ParSoundTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoledBlack: Boolean = false,
    palette: PaletteColors = PaletteColors.Default,
    glassAlpha: Float = 0.55f,
    glow: Float = 0.45f,
    blur: Float = 0.65f,
    animations: Boolean = true,
    performanceMode: Boolean = false,
    coverBackground: Boolean = true,
    content: @Composable () -> Unit
) {
    val animated = animations && !performanceMode
    val spec = tween<Color>(durationMillis = if (animated) 850 else 0)
    val backgroundStart by animateColorAsState(
        targetValue = if (darkTheme) Color(palette.backgroundStart)
        else lighten(Color(palette.accent), 0.90f),
        animationSpec = spec,
        label = "backgroundStart"
    )
    val backgroundEnd by animateColorAsState(
        targetValue = if (darkTheme) Color(palette.backgroundEnd)
        else lighten(Color(palette.accentSoft), 0.94f),
        animationSpec = spec,
        label = "backgroundEnd"
    )
    val accent by animateColorAsState(
        targetValue = Color(palette.accent),
        animationSpec = tween(if (animated) 600 else 0),
        label = "accent"
    )
    val textPrimary = if (darkTheme) Color(0xFFF5F7FB) else Color(0xFF14161C)
    val textSecondary = if (darkTheme) Color(0xB8FFFFFF) else Color(0x9914161C)
    val glassFill = if (darkTheme) {
        Color.White.copy(alpha = (0.045f + glassAlpha * 0.10f).coerceIn(0.04f, 0.18f))
    } else {
        Color.White.copy(alpha = (0.35f + glassAlpha * 0.32f).coerceIn(0.35f, 0.72f))
    }
    val glassStrong = if (darkTheme) {
        Color.White.copy(alpha = (0.07f + glassAlpha * 0.14f).coerceIn(0.06f, 0.24f))
    } else {
        Color.White.copy(alpha = (0.55f + glassAlpha * 0.25f).coerceIn(0.55f, 0.86f))
    }
    val glassBorder = if (darkTheme) {
        Color.White.copy(alpha = (0.10f + glow * 0.22f).coerceIn(0.08f, 0.34f))
    } else {
        Color.White.copy(alpha = 0.72f)
    }
    val tokens = GlassTokens(
        isDark = darkTheme,
        backgroundStart = if (amoledBlack && darkTheme) Color.Black else backgroundStart,
        backgroundEnd = if (amoledBlack && darkTheme) Color.Black else backgroundEnd,
        accent = accent,
        accentSoft = Color(palette.accentSoft),
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        glassFill = glassFill,
        glassStrong = glassStrong,
        glassBorder = glassBorder,
        glassHighlight = Color.White.copy(alpha = (0.06f + glow * 0.10f).coerceIn(0.02f, 0.18f)),
        glassAlpha = glassAlpha,
        glow = glow,
        blur = (blur * 44f).dp,
        animations = animated,
        performance = performanceMode,
        amoled = amoledBlack && darkTheme,
        coverBackground = coverBackground
    )

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = accent,
            secondary = Color(palette.accentSoft),
            background = tokens.backgroundStart,
            surface = tokens.backgroundEnd,
            surfaceVariant = Color(0x1AFFFFFF),
            onPrimary = Color(0xFF081018),
            onBackground = textPrimary,
            onSurface = textPrimary,
            onSurfaceVariant = textSecondary,
            outline = glassBorder
        )
    } else {
        lightColorScheme(
            primary = darken(Color(palette.accent), 0.75f),
            secondary = darken(Color(palette.accentSoft), 0.7f),
            background = tokens.backgroundStart,
            surface = tokens.backgroundEnd,
            surfaceVariant = Color(0x33FFFFFF),
            onPrimary = Color.White,
            onBackground = textPrimary,
            onSurface = textPrimary,
            onSurfaceVariant = textSecondary,
            outline = Color(0x3314161C)
        )
    }

    CompositionLocalProvider(LocalGlass provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ParSoundTypography,
            content = content
        )
    }
}

fun lighten(color: Color, amount: Float): Color = Color(
    red = color.red + (1f - color.red) * amount,
    green = color.green + (1f - color.green) * amount,
    blue = color.blue + (1f - color.blue) * amount,
    alpha = 1f
)

fun darken(color: Color, factor: Float): Color = Color(
    red = color.red * factor,
    green = color.green * factor,
    blue = color.blue * factor,
    alpha = 1f
)
