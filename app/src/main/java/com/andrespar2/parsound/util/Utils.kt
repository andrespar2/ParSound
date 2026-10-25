package com.andrespar2.parsound.util

import android.content.Context
import android.net.Uri
import java.security.MessageDigest

object Ids {
    fun forUri(uri: String): Long {
        val digest = MessageDigest.getInstance("SHA-1").digest(uri.toByteArray())
        var value = 0L
        for (i in 0 until 8) value = (value shl 8) or (digest[i].toLong() and 0xFF)
        return value and Long.MAX_VALUE
    }
}

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val total = ms / 1000
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
    else String.format("%d:%02d", minutes, seconds)
}

fun songsLabel(count: Int): String = if (count == 1) "1 canción" else "$count canciones"

fun foldersLabel(count: Int): String = if (count == 1) "1 carpeta" else "$count carpetas"

fun formatTotalDuration(ms: Long): String {
    if (ms <= 0) return "0 min"
    if (ms < 60_000) return "${(ms / 1000).coerceAtLeast(1)} s"
    val minutes = ms / 60_000
    if (minutes < 60) return "$minutes min"
    return "${minutes / 60} h ${minutes % 60} min"
}

fun formatDate(timestamp: Long): String {
    if (timestamp <= 0) return "Nunca"
    val format = java.text.SimpleDateFormat("d MMM yyyy, HH:mm", java.util.Locale.getDefault())
    return format.format(java.util.Date(timestamp))
}

fun Context.openUri(url: String) {
    runCatching {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}
