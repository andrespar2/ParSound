package com.andrespar2.parsound.player

data class PaletteColors(
    val backgroundStart: Int,
    val backgroundEnd: Int,
    val accent: Int,
    val accentSoft: Int,
    val onBackground: Int
) {
    companion object {
        val Default = PaletteColors(
            backgroundStart = 0xFF0A0C12.toInt(),
            backgroundEnd = 0xFF141824.toInt(),
            accent = 0xFF8AB4F8.toInt(),
            accentSoft = 0xFF5B8DEF.toInt(),
            onBackground = 0xFFF2F4F8.toInt()
        )
    }
}
