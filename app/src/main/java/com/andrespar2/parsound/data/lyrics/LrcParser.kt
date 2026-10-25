package com.andrespar2.parsound.data.lyrics

data class LyricLine(val timeMs: Long, val text: String)

data class Lyrics(
    val lines: List<LyricLine>,
    val synced: Boolean
) {
    fun currentIndex(positionMs: Long): Int {
        if (lines.isEmpty() || !synced) return 0
        var result = -1
        for (index in lines.indices) {
            if (lines[index].timeMs <= positionMs) result = index else break
        }
        return result.coerceAtLeast(0)
    }
}

object LrcParser {
    private val timeTag = Regex("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?]")
    private val offsetTag = Regex("\\[offset:([+-]?\\d+)]", RegexOption.IGNORE_CASE)

    fun parse(raw: String?): Lyrics {
        if (raw.isNullOrBlank()) return Lyrics(emptyList(), false)
        val offset = offsetTag.find(raw)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
        val parsed = mutableListOf<LyricLine>()
        var hasTimestamps = false

        raw.lineSequence().forEach { line ->
            val matches = timeTag.findAll(line).toList()
            if (matches.isEmpty()) {
                val metadata = line.trim()
                if (metadata.isNotEmpty() && !metadata.startsWith("[")) {
                    parsed.add(LyricLine(0L, metadata))
                }
            } else {
                hasTimestamps = true
                val text = timeTag.replace(line, "").trim()
                if (text.isNotEmpty()) {
                    matches.forEach { match ->
                        val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                        val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                        val fractionRaw = match.groupValues[3]
                        val fraction = when (fractionRaw.length) {
                            0 -> 0L
                            1 -> fractionRaw.toLongOrNull()?.times(100) ?: 0L
                            2 -> fractionRaw.toLongOrNull()?.times(10) ?: 0L
                            else -> fractionRaw.take(3).toLongOrNull() ?: 0L
                        }
                        parsed.add(
                            LyricLine(
                                timeMs = minutes * 60_000 + seconds * 1_000 + fraction - offset,
                                text = text
                            )
                        )
                    }
                }
            }
        }

        val lines = if (hasTimestamps) {
            parsed.sortedBy { it.timeMs }
        } else {
            parsed.mapIndexed { index, line -> line.copy(timeMs = index.toLong()) }
        }
        return Lyrics(lines, hasTimestamps)
    }
}
