package com.andrespar2.parsound.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

data class WidgetSnapshot(
    val title: String = "ParSound",
    val artist: String = "Añade música a tu biblioteca",
    val isPlaying: Boolean = false,
    val artworkPath: String? = null,
    val accent: Int = 0xFF8AB4F8.toInt(),
    val background: Int = 0xFF101114.toInt(),
    val songId: Long = 0L
)

object WidgetStateStore {
    private const val PREFS = "parsound_widget_state"

    fun save(context: Context, snapshot: WidgetSnapshot) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("title", snapshot.title)
            .putString("artist", snapshot.artist)
            .putBoolean("playing", snapshot.isPlaying)
            .putString("artwork", snapshot.artworkPath)
            .putInt("accent", snapshot.accent)
            .putInt("background", snapshot.background)
            .putLong("songId", snapshot.songId)
            .apply()
    }

    fun load(context: Context): WidgetSnapshot {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return WidgetSnapshot(
            title = prefs.getString("title", "ParSound") ?: "ParSound",
            artist = prefs.getString("artist", "Añade música a tu biblioteca")
                ?: "Añade música a tu biblioteca",
            isPlaying = prefs.getBoolean("playing", false),
            artworkPath = prefs.getString("artwork", null),
            accent = prefs.getInt("accent", 0xFF8AB4F8.toInt()),
            background = prefs.getInt("background", 0xFF101114.toInt()),
            songId = prefs.getLong("songId", 0L)
        )
    }

    fun artworkBitmap(context: Context, path: String?, maxSize: Int = 512): Bitmap? {
        if (path.isNullOrBlank()) return null
        return runCatching {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            when {
                path.startsWith("file://") -> BitmapFactory.decodeFile(path.removePrefix("file://"), options)
                path.startsWith("content://") -> context.contentResolver.openInputStream(android.net.Uri.parse(path))?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
            }
            if (options.outWidth <= 0 || options.outHeight <= 0) return null
            var sample = 1
            while (options.outWidth / sample > maxSize || options.outHeight / sample > maxSize) sample *= 2
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            when {
                path.startsWith("file://") ->
                    BitmapFactory.decodeFile(path.removePrefix("file://"), decodeOptions)
                path.startsWith("content://") ->
                    context.contentResolver.openInputStream(android.net.Uri.parse(path))?.use {
                        BitmapFactory.decodeStream(it, null, decodeOptions)
                    }
                else -> null
            }
        }.getOrNull()
    }

    fun artworkFile(path: String?): File? {
        if (path.isNullOrBlank() || !path.startsWith("file://")) return null
        val file = File(path.removePrefix("file://"))
        return if (file.exists()) file else null
    }
}
