package com.andrespar2.parsound.ui.theme

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette

data class DynamicGlassColors(
    val backgroundStart: Color,
    val backgroundEnd: Color,
    val glass: Color,
    val glassBorder: Color,
    val accent: Color,
    val onBackground: Color,
    val onGlass: Color
)

object PaletteExtractor {
    fun extract(bitmap: Bitmap?): DynamicGlassColors {
        if (bitmap == null) {
            return defaultColors()
        }
        val palette = Palette.from(bitmap).generate()
        val dominant = palette.dominantSwatch
        val vibrant = palette.vibrantSwatch
        val muted = palette.mutedSwatch
        val darkVibrant = palette.darkVibrantSwatch
        val lightVibrant = palette.lightVibrantSwatch

        val start = Color(dominant?.rgb ?: 0xFF0B0B0F.toInt())
        val end = Color(darkVibrant?.rgb ?: (muted?.rgb ?: 0xFF101114.toInt()))
        val accentColor = Color(vibrant?.rgb ?: lightVibrant?.rgb ?: 0xFF8AB4F8.toInt())
        val textColor = Color(dominant?.bodyTextColor ?: 0xFFE8EAED.toInt())
        val glassColor = Color(0x66101114)
        val borderColor = Color(0x40FFFFFF)

        return DynamicGlassColors(
            backgroundStart = start,
            backgroundEnd = end,
            glass = glassColor,
            glassBorder = borderColor,
            accent = accentColor,
            onBackground = textColor,
            onGlass = Color(0xFFE8EAED)
        )
    }

    private fun defaultColors(): DynamicGlassColors {
        return DynamicGlassColors(
            backgroundStart = Color(0xFF0B0B0F),
            backgroundEnd = Color(0xFF101114),
            glass = Color(0x66101114),
            glassBorder = Color(0x40FFFFFF),
            accent = Color(0xFF8AB4F8),
            onBackground = Color(0xFFE8EAED),
            onGlass = Color(0xFFE8EAED)
        )
    }
}
