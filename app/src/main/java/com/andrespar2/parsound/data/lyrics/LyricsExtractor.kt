package com.andrespar2.parsound.data.lyrics

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Extrae letras incrustadas en los metadatos de los archivos locales.
 *
 * Soporta las etiquetas habituales:
 * - FLAC/OGG: LY RICS, UNSYNCEDLYRICS, SYNCEDLYRICS (Vorbis comments).
 * - MP3: USLT, SYLT y TXXX con descripción de letras (ID3v2.3/2.4).
 * - M4A/MP4: átomo ©lyr.
 * - WAV: bloque ID3.
 */
object LyricsExtractor {
    private const val MAX_BYTES = 12 * 1024 * 1024
    private const val MAX_TEXT = 512 * 1024
    private val LYRICS_KEYS = listOf(
        "SYNCEDLYRICS",
        "UNSYNCEDLYRICS",
        "LYRICS",
        "LYRIC",
        "LYRICS_ENG",
        "UNSYNCED LYRICS"
    )

    fun extract(context: Context, uri: Uri, fileName: String?): String? {
        val bytes = readPrefix(context, uri) ?: return null
        if (bytes.size < 16) return null
        val extension = fileName?.substringAfterLast('.', "")?.lowercase().orEmpty()
        val result = when {
            bytes.hasMagic("fLaC") -> parseFlac(bytes)
            bytes.hasMagic("ID3") -> parseId3(bytes)
            bytes.hasMagic("OggS") -> scanVorbisComments(bytes)
            bytes.hasMagic("RIFF") -> parseWav(bytes) ?: scanVorbisComments(bytes)
            isMp4(bytes) || extension in setOf("m4a", "m4b", "mp4", "aac") ->
                parseMp4(bytes) ?: scanVorbisComments(bytes)
            else -> parseId3(bytes) ?: scanVorbisComments(bytes)
        }
        return result?.cleanLyrics()?.takeIf { it.isNotBlank() }
    }

    private fun readPrefix(context: Context, uri: Uri): ByteArray? = runCatching {
        val input = when (uri.scheme) {
            "content" -> context.contentResolver.openInputStream(uri)
            "file" -> File(uri.path ?: return null).inputStream()
            else -> context.contentResolver.openInputStream(uri)
        } ?: return null
        input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(64 * 1024)
            var total = 0
            while (total < MAX_BYTES) {
                val read = stream.read(buffer)
                if (read <= 0) break
                output.write(buffer, 0, read)
                total += read
            }
            output.toByteArray()
        }
    }.getOrNull()

    private fun parseFlac(bytes: ByteArray): String? {
        var offset = 4
        var blocks = 0
        while (offset + 4 <= bytes.size && blocks < 256) {
            val header = bytes[offset].toInt() and 0xFF
            val last = header and 0x80 != 0
            val type = header and 0x7F
            val length = readBE24(bytes, offset + 1)
            val start = offset + 4
            val end = (start + length).coerceAtMost(bytes.size)
            if (end <= start) break
            if (type == 4) {
                parseVorbisCommentBlock(bytes, start, end)?.let { return it }
            }
            offset = end
            blocks++
            if (last) break
        }
        return null
    }

    private fun parseVorbisCommentBlock(bytes: ByteArray, start: Int, end: Int): String? {
        var offset = start
        if (offset + 4 > end) return null
        val vendorLength = readLE32(bytes, offset)
        offset += 4 + vendorLength
        if (offset + 4 > end) return null
        val count = readLE32(bytes, offset)
        offset += 4
        if (count !in 0..10_000) return scanVorbisComments(bytes)
        var best: String? = null
        repeat(count) {
            if (offset + 4 > end) return@repeat
            val length = readLE32(bytes, offset)
            offset += 4
            if (length <= 0 || offset + length > end) return@repeat
            val comment = String(bytes, offset, length, StandardCharsets.UTF_8)
            offset += length
            val separator = comment.indexOf('=')
            if (separator > 0) {
                val key = comment.substring(0, separator).trim().uppercase()
                val value = comment.substring(separator + 1)
                if (key in LYRICS_KEYS && value.isNotBlank()) {
                    best = chooseBetter(best, value)
                }
            }
        }
        return best
    }

    private fun scanVorbisComments(bytes: ByteArray): String? {
        var best: String? = null
        LYRICS_KEYS.forEach { key ->
            val pattern = "$key=".toByteArray(StandardCharsets.US_ASCII)
            var from = 0
            while (from < bytes.size) {
                val index = indexOfIgnoreCase(bytes, pattern, from)
                if (index < 0) break
                from = index + pattern.size
                if (index < 4) continue
                val length = readLE32(bytes, index - 4)
                if (length <= 0 || length > MAX_TEXT) continue
                val end = (index + length).coerceAtMost(bytes.size)
                if (end <= index) continue
                val value = String(bytes, index + pattern.size, end - index - pattern.size, StandardCharsets.UTF_8)
                if (value.isNotBlank()) best = chooseBetter(best, value)
            }
        }
        return best
    }

    private fun parseId3(bytes: ByteArray): String? {
        if (!bytes.hasMagic("ID3") || bytes.size < 10) return null
        val major = bytes[3].toInt() and 0xFF
        val flags = bytes[5].toInt() and 0xFF
        val tagSize = readSyncsafe(bytes, 6)
        var offset = 10
        val tagEnd = (tagSize + 10).coerceAtMost(bytes.size)

        if (flags and 0x40 != 0 && offset + 4 <= tagEnd) {
            val extended = if (major >= 4) readSyncsafe(bytes, offset) else readBE32(bytes, offset) + 4
            offset += if (major >= 4) extended else extended
        }

        var candidates: String? = null
        while (offset + 10 <= tagEnd && major >= 3) {
            val id = String(bytes, offset, 4, StandardCharsets.US_ASCII)
            if (id.all { it == '\u0000' }) break
            val frameSize = if (major >= 4) readSyncsafe(bytes, offset + 4) else readBE32(bytes, offset + 4)
            val contentStart = offset + 10
            val contentEnd = (contentStart + frameSize).coerceAtMost(tagEnd)
            if (frameSize <= 0 || contentEnd <= contentStart) break
            when (id) {
                "USLT" -> parseUslt(bytes, contentStart, contentEnd)?.let {
                    candidates = chooseBetter(candidates, it)
                }
                "SYLT" -> parseSylt(bytes, contentStart, contentEnd)?.let {
                    candidates = chooseBetter(candidates, it)
                }
                "TXXX" -> parseTxxx(bytes, contentStart, contentEnd)?.let {
                    candidates = chooseBetter(candidates, it)
                }
            }
            offset = contentEnd
        }
        return candidates
    }

    private fun parseUslt(bytes: ByteArray, start: Int, end: Int): String? {
        if (start + 4 > end) return null
        val encoding = bytes[start].toInt() and 0xFF
        var offset = start + 4
        offset = skipTerminated(bytes, offset, end, encoding)
        if (offset >= end) return null
        return decodeText(bytes, offset, end, encoding)
    }

    private fun parseTxxx(bytes: ByteArray, start: Int, end: Int): String? {
        if (start + 1 > end) return null
        val encoding = bytes[start].toInt() and 0xFF
        var offset = start + 1
        val descriptionStart = offset
        offset = skipTerminated(bytes, offset, end, encoding)
        if (offset <= descriptionStart || offset >= end) return null
        val description = decodeText(bytes, descriptionStart, offset - terminatorSize(encoding), encoding)
        if (!description.contains("LYRIC", ignoreCase = true)) return null
        return decodeText(bytes, offset, end, encoding)
    }

    private fun parseSylt(bytes: ByteArray, start: Int, end: Int): String? {
        if (start + 6 > end) return null
        val encoding = bytes[start].toInt() and 0xFF
        val timestampFormat = bytes[start + 4].toInt() and 0xFF
        var offset = start + 6
        offset = skipTerminated(bytes, offset, end, encoding)
        if (offset >= end) return null
        val lines = mutableListOf<String>()
        while (offset < end - 4) {
            val textStart = offset
            offset = skipTerminated(bytes, offset, end, encoding)
            if (offset >= end || offset - terminatorSize(encoding) <= textStart) break
            val text = decodeText(bytes, textStart, offset - terminatorSize(encoding), encoding)
            if (offset + 4 > end) break
            val stamp = readBE32(bytes, offset)
            offset += 4
            if (text.isNotBlank()) {
                val ms = if (timestampFormat == 2) stamp.toLong() else stamp.toLong() * 32L
                lines.add(formatLrcTime(ms) + text)
            }
        }
        return lines.takeIf { it.isNotEmpty() }?.joinToString("\n")
    }

    private fun parseMp4(bytes: ByteArray): String? {
        val markers = listOf(
            byteArrayOf(0xA9.toByte(), 0x6C, 0x79, 0x72),
            "lyr ".toByteArray(StandardCharsets.US_ASCII)
        )
        markers.forEach { marker ->
            var from = 0
            while (from < bytes.size) {
                val index = indexOf(bytes, marker, from)
                if (index < 0) break
                from = index + marker.size
                val atomStart = index - 4
                if (atomStart < 0) continue
                val atomSize = readBE32(bytes, atomStart)
                val atomEnd = if (atomSize in 16..MAX_TEXT) {
                    (atomStart + atomSize).coerceAtMost(bytes.size)
                } else {
                    (index + 4096).coerceAtMost(bytes.size)
                }
                val dataIndex = indexOf(bytes, "data".toByteArray(StandardCharsets.US_ASCII), index + 4)
                if (dataIndex in (index + 4) until atomEnd) {
                    val payloadStart = dataIndex + 12
                    if (payloadStart in 0 until atomEnd) {
                        val text = String(bytes, payloadStart, atomEnd - payloadStart, StandardCharsets.UTF_8)
                            .trim('\u0000', ' ', '\n', '\r', '\t')
                        if (text.isNotBlank()) return text
                    }
                }
            }
        }
        return null
    }

    private fun parseWav(bytes: ByteArray): String? {
        var offset = 12
        while (offset + 8 <= bytes.size) {
            val id = String(bytes, offset, 4, StandardCharsets.US_ASCII)
            var size = readLE32(bytes, offset + 4)
            val dataStart = offset + 8
            if (size < 0 || dataStart + size > bytes.size) size = bytes.size - dataStart
            if (id == "id3 " || id == "ID3 ") {
                val tag = bytes.copyOfRange(dataStart, (dataStart + size).coerceAtMost(bytes.size))
                parseId3(tag)?.let { return it }
            }
            offset = dataStart + size + (size and 1)
        }
        return null
    }

    private fun skipTerminated(bytes: ByteArray, start: Int, end: Int, encoding: Int): Int {
        val twoBytes = encoding == 1 || encoding == 2
        var offset = start
        if (twoBytes) {
            while (offset + 1 < end) {
                if (bytes[offset].toInt() == 0 && bytes[offset + 1].toInt() == 0) {
                    return offset + 2
                }
                offset += 2
            }
        } else {
            while (offset < end) {
                if (bytes[offset].toInt() == 0) return offset + 1
                offset++
            }
        }
        return end
    }

    private fun terminatorSize(encoding: Int): Int =
        if (encoding == 1 || encoding == 2) 2 else 1

    private fun decodeText(bytes: ByteArray, start: Int, end: Int, encoding: Int): String {
        if (end <= start) return ""
        val charset = when (encoding) {
            0 -> Charset.forName("ISO-8859-1")
            1 -> Charset.forName("UTF-16")
            2 -> Charset.forName("UTF-16BE")
            else -> StandardCharsets.UTF_8
        }
        return String(bytes, start, end - start, charset)
            .trim('\u0000', '\uFEFF', ' ', '\n', '\r', '\t')
    }

    private fun chooseBetter(current: String?, candidate: String): String {
        if (current == null) return candidate
        val currentSynced = containsTimestamp(current)
        val candidateSynced = containsTimestamp(candidate)
        return when {
            candidateSynced && !currentSynced -> candidate
            !candidateSynced && currentSynced -> current
            candidate.length > current.length -> candidate
            else -> current
        }
    }

    private fun containsTimestamp(text: String): Boolean =
        Regex("\\[\\d{1,2}:\\d{2}(?:[.:]\\d{1,3})?]").containsMatchIn(text)

    private fun String.cleanLyrics(): String = trim()
        .replace("\u0000", "")
        .lines()
        .joinToString("\n") { it.trimEnd() }
        .trim()

    private fun formatLrcTime(ms: Long): String {
        val minutes = ms / 60_000
        val seconds = (ms % 60_000) / 1000
        val hundredths = (ms % 1000) / 10
        return "[%02d:%02d.%02d]".format(minutes, seconds, hundredths)
    }

    private fun ByteArray.hasMagic(magic: String): Boolean {
        val value = magic.toByteArray(StandardCharsets.US_ASCII)
        if (size < value.size) return false
        return value.indices.all { this[it] == value[it] }
    }

    private fun isMp4(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        return String(bytes, 4, 4, StandardCharsets.US_ASCII) == "ftyp"
    }

    private fun indexOf(haystack: ByteArray, needle: ByteArray, from: Int): Int {
        if (needle.isEmpty() || haystack.size < needle.size) return -1
        var i = from.coerceAtLeast(0)
        val last = haystack.size - needle.size
        while (i <= last) {
            var match = true
            for (j in needle.indices) {
                if (haystack[i + j] != needle[j]) {
                    match = false
                    break
                }
            }
            if (match) return i
            i++
        }
        return -1
    }

    private fun indexOfIgnoreCase(haystack: ByteArray, needle: ByteArray, from: Int): Int {
        if (needle.isEmpty() || haystack.size < needle.size) return -1
        var i = from.coerceAtLeast(0)
        val last = haystack.size - needle.size
        while (i <= last) {
            var match = true
            for (j in needle.indices) {
                val a = haystack[i + j].toInt() and 0xFF
                val b = needle[j].toInt() and 0xFF
                if (a != b && a.toChar().uppercaseChar() != b.toChar().uppercaseChar()) {
                    match = false
                    break
                }
            }
            if (match) return i
            i++
        }
        return -1
    }

    private fun readBE24(bytes: ByteArray, offset: Int): Int {
        if (offset + 3 > bytes.size) return 0
        return ((bytes[offset].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            (bytes[offset + 2].toInt() and 0xFF)
    }

    private fun readBE32(bytes: ByteArray, offset: Int): Int {
        if (offset + 4 > bytes.size) return 0
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)
    }

    private fun readLE32(bytes: ByteArray, offset: Int): Int {
        if (offset + 4 > bytes.size) return 0
        return (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun readSyncsafe(bytes: ByteArray, offset: Int): Int {
        if (offset + 4 > bytes.size) return 0
        return ((bytes[offset].toInt() and 0x7F) shl 21) or
            ((bytes[offset + 1].toInt() and 0x7F) shl 14) or
            ((bytes[offset + 2].toInt() and 0x7F) shl 7) or
            (bytes[offset + 3].toInt() and 0x7F)
    }
}
