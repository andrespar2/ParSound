package com.andrespar2.parsound.scanner

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.andrespar2.parsound.data.local.SongDao
import com.andrespar2.parsound.data.lyrics.LyricsExtractor
import com.andrespar2.parsound.data.local.SongEntity
import com.andrespar2.parsound.data.prefs.FolderConfig
import com.andrespar2.parsound.data.prefs.ParSoundPreferences
import com.andrespar2.parsound.util.Ids
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ScanState {
    data object Idle : ScanState
    data object NoFolders : ScanState
    data class Scanning(
        val folder: String = "",
        val filesFound: Int = 0,
        val filesProcessed: Int = 0,
        val errors: Int = 0
    ) : ScanState
    data class Completed(
        val songs: Int,
        val errors: Int,
        val durationMs: Long,
        val cancelled: Boolean = false
    ) : ScanState
}

private val AUDIO_EXTENSIONS = setOf(
    "mp3", "flac", "m4a", "aac", "ogg", "opus", "wav", "mp4", "m4b", "wma", "aiff", "aif"
)

private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

@Singleton
class LibraryScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao,
    private val preferences: ParSoundPreferences
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    private var scanJob: Job? = null

    fun scanAllFolders() {
        if (scanJob?.isActive == true) return
        scanJob = scope.launch {
            scanInternal()
        }
    }

    fun cancel() {
        scanJob?.cancel()
        scanJob = null
        _state.value = ScanState.Completed(0, 0, 0, cancelled = true)
    }

    private suspend fun scanInternal() {
        val start = System.currentTimeMillis()
        val folders = preferences.foldersOnce()
        if (folders.isEmpty()) {
            _state.value = ScanState.NoFolders
            return
        }

        _state.value = ScanState.Scanning()
        val existingFavorites = songDao.getFavoritesOnce().associateBy { it.uri }
        var totalErrors = 0
        var processed = 0
        var found = 0
        val seenFingerprints = mutableSetOf<String>()

        try {
            for (folder in folders) {
                val folderStartFound = found
                val baseErrors = totalErrors
                try {
                    val scanned = withContext(Dispatchers.IO) {
                        scanFolder(folder, existingFavorites, seenFingerprints) { folderName, count, errors ->
                            processed = count
                            totalErrors = baseErrors + errors
                            _state.value = ScanState.Scanning(
                                folder = folderName,
                                filesFound = folderStartFound + count,
                                filesProcessed = processed,
                                errors = totalErrors
                            )
                        }
                    }
                    found = folderStartFound + scanned.size
                    if (scanned.isNotEmpty()) {
                        songDao.insertAll(scanned)
                    }
                    songDao.deleteMissingInFolder(folder.uri, scanned.map { it.id })
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    // Permiso revocado, carpeta eliminada o proveedor no disponible:
                    // se continúa con las demás carpetas.
                    totalErrors = baseErrors + 1
                }
            }

            songDao.prunePlaylistSongs()
            songDao.pruneHistory()
            preferences.setLastScan(System.currentTimeMillis())
            _state.value = ScanState.Completed(
                songs = songDao.count(),
                errors = totalErrors,
                durationMs = System.currentTimeMillis() - start
            )
        } catch (cancelled: CancellationException) {
            _state.value = ScanState.Completed(
                songs = songDao.count(),
                errors = totalErrors,
                durationMs = System.currentTimeMillis() - start,
                cancelled = true
            )
            throw cancelled
        } catch (error: Exception) {
            _state.value = ScanState.Completed(
                songs = songDao.count(),
                errors = totalErrors + 1,
                durationMs = System.currentTimeMillis() - start
            )
        }
    }

    private suspend fun scanFolder(
        folder: FolderConfig,
        favorites: Map<String, SongEntity>,
        seenFingerprints: MutableSet<String>,
        onProgress: (String, Int, Int) -> Unit
    ): List<SongEntity> = withContext(Dispatchers.IO) {
        val treeUri = Uri.parse(folder.uri)
        val canRead = context.contentResolver.persistedUriPermissions.any {
            it.uri == treeUri && it.isReadPermission
        }
        if (!canRead) {
            throw SecurityException("Permiso revocado para ${folder.name}")
        }
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IllegalStateException("No se pudo abrir ${folder.name}")
        val rootDocId = runCatching {
            android.provider.DocumentsContract.getTreeDocumentId(treeUri)
        }.getOrDefault("")

        val result = mutableListOf<SongEntity>()
        val queue = ArrayDeque<Pair<DocumentFile, Int>>()
        queue.add(root to 0)
        var visited = 0
        var errors = 0
        var processed = 0

        while (queue.isNotEmpty()) {
            val (dir, depth) = queue.removeFirst()
            if (depth > 16) continue
            val children = runCatching { dir.listFiles() }.getOrElse {
                errors++
                emptyArray()
            }
            val audioFiles = children.filter { it.isFile && isAudio(it.name, it.type) }
            val lrcFiles = children.filter { it.isFile && it.name?.endsWith(".lrc", true) == true }
            val cover = children.firstOrNull {
                it.isFile && isImage(it.name, it.type) &&
                    it.name?.substringBeforeLast('.')?.lowercase() in setOf("cover", "folder", "albumart", "front")
            }

            if (audioFiles.isNotEmpty()) {
                val folderRelative = relativeFolder(dir, rootDocId, folder.name)
                val lrcByBase = lrcFiles.associateBy {
                    it.name?.substringBeforeLast('.')?.lowercase().orEmpty()
                }
                for (file in audioFiles) {
                    visited++
                    if (visited > 30_000) break
                    try {
                        val name = file.name ?: continue
                        val base = name.substringBeforeLast('.').lowercase()
                        val lrc = lrcByBase[base]?.let { readText(it.uri) }
                        val song = readSong(
                            file = file,
                            folderName = folderRelative,
                            folderUri = folder.uri,
                            lrc = lrc,
                            folderCover = cover,
                            favorites = favorites
                        )
                        if (song != null) {
                            val fingerprint = buildString {
                                append(song.title.lowercase().trim())
                                append('|').append(song.artist.lowercase().trim())
                                append('|').append(song.album.lowercase().trim())
                                append('|').append(song.durationMs / 1000)
                            }
                            if (seenFingerprints.add(fingerprint)) {
                                result.add(song)
                            }
                        }
                    } catch (error: Exception) {
                        errors++
                    }
                    processed++
                    if (processed % 5 == 0 || processed == 1) {
                        onProgress(folder.name, processed, errors)
                    }
                }
            }

            children.filter { it.isDirectory && it.name?.startsWith(".") != true }
                .forEach { queue.add(it to depth + 1) }
        }
        onProgress(folder.name, processed, errors)
        result
    }

    private fun relativeFolder(dir: DocumentFile, rootDocId: String, rootName: String): String {
        val docId = runCatching {
            android.provider.DocumentsContract.getDocumentId(dir.uri)
        }.getOrNull().orEmpty()
        val relative = if (rootDocId.isNotEmpty() && docId.startsWith(rootDocId)) {
            docId.removePrefix(rootDocId).trim('/', '\\', ':')
        } else {
            ""
        }
        if (relative.isBlank()) return rootName
        return "$rootName/$relative"
    }

    private fun readSong(
        file: DocumentFile,
        folderName: String,
        folderUri: String,
        lrc: String?,
        folderCover: DocumentFile?,
        favorites: Map<String, SongEntity>
    ): SongEntity? {
        val uri = file.uri.toString()
        val fileName = file.name ?: return null
        val fallbackTitle = fileName.substringBeforeLast('.').trim().ifBlank { "Sin título" }
        val id = Ids.forUri(uri)
        val retriever = MediaMetadataRetriever()
        var title = fallbackTitle
        var artist = "Artista desconocido"
        var album = "Álbum desconocido"
        var duration = 0L
        var track = 0
        var year: Int? = null
        var genre: String? = null
        var artwork: String? = null

        try {
            retriever.setDataSource(context, file.uri)
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.trim()?.takeIf { it.isNotBlank() } ?: fallbackTitle
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.trim()?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Artista desconocido"
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.trim()?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Álbum desconocido"
            duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            track = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                ?.substringBefore('/')?.toIntOrNull() ?: 0
            year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                ?.take(4)?.toIntOrNull()
            genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                ?.replace("\u0000", "")?.trim()?.takeIf { it.isNotBlank() }

            val embedded = retriever.embeddedPicture
            if (embedded != null && embedded.isNotEmpty()) {
                artwork = saveArtwork(id, embedded)
            }
        } catch (_: Exception) {
            // Archivo dañado o formato no legible: se conservan los datos del nombre.
        } finally {
            runCatching { retriever.release() }
        }

        if (duration <= 0) {
            duration = runCatching { file.length() }.getOrDefault(0L) / 32
        }

        if (artwork == null && folderCover != null) {
            artwork = folderCover.uri.toString()
        }

        val embeddedLyrics = if (lrc.isNullOrBlank()) {
            runCatching { LyricsExtractor.extract(context, file.uri, fileName) }.getOrNull()
        } else null

        return SongEntity(
            id = id,
            uri = uri,
            title = title,
            artist = artist,
            album = album,
            albumKey = "$album|$artist",
            durationMs = duration,
            artworkUri = artwork,
            trackNumber = track,
            year = year,
            genre = genre,
            folder = folderName,
            folderUri = folderUri,
            fileName = fileName,
            lrc = lrc ?: embeddedLyrics,
            isFavorite = favorites[uri]?.isFavorite ?: false,
            addedAt = file.lastModified()
        )
    }

    private fun saveArtwork(id: Long, bytes: ByteArray): String? = runCatching {
        val dir = File(context.cacheDir, "artwork").apply { mkdirs() }
        val file = File(dir, "$id.jpg")
        if (!file.exists() || file.length() != bytes.size.toLong()) {
            file.writeBytes(bytes)
        }
        Uri.fromFile(file).toString()
    }.getOrNull()

    private fun readText(uri: Uri): String? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            var total = 0
            while (total < 512 * 1024) {
                val read = input.read(buffer)
                if (read <= 0) break
                output.write(buffer, 0, read)
                total += read
            }
            String(output.toByteArray(), Charsets.UTF_8)
        }
    }.getOrNull()

    private fun isAudio(name: String?, mime: String?): Boolean {
        if (mime?.startsWith("audio/") == true) return true
        val ext = name?.substringAfterLast('.', "")?.lowercase() ?: return false
        if (ext in AUDIO_EXTENSIONS) return true
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)?.startsWith("audio/") == true
    }

    private fun isImage(name: String?, mime: String?): Boolean {
        if (mime?.startsWith("image/") == true) return true
        val ext = name?.substringAfterLast('.', "")?.lowercase() ?: return false
        return ext in IMAGE_EXTENSIONS
    }

    suspend fun importFromMediaStore(): Int = withContext(Dispatchers.IO) {
        val supportsGenre = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.TRACK)
            add(MediaStore.Audio.Media.YEAR)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            if (supportsGenre) add(MediaStore.Audio.Media.GENRE)
        }.toTypedArray()
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val existingFavorites = songDao.getFavoritesOnce().associateBy { it.uri }
        val result = mutableListOf<SongEntity>()
        val fingerprints = mutableSetOf<String>()

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            fun column(name: String) = cursor.getColumnIndex(name)
            val idCol = column(MediaStore.Audio.Media._ID)
            val titleCol = column(MediaStore.Audio.Media.TITLE)
            val artistCol = column(MediaStore.Audio.Media.ARTIST)
            val albumCol = column(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = column(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = column(MediaStore.Audio.Media.DURATION)
            val trackCol = column(MediaStore.Audio.Media.TRACK)
            val yearCol = column(MediaStore.Audio.Media.YEAR)
            val nameCol = column(MediaStore.Audio.Media.DISPLAY_NAME)
            val genreCol = if (supportsGenre) column(MediaStore.Audio.Media.GENRE) else -1

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString()
                )
                val uri = contentUri.toString()
                val albumId = cursor.getLong(albumIdCol)
                val artwork = Uri.withAppendedPath(
                    Uri.parse("content://media/external/audio/albumart"), albumId.toString()
                ).toString()
                val title = cursor.getString(titleCol) ?: "Sin título"
                val artist = cursor.getString(artistCol)?.takeIf { it != "<unknown>" } ?: "Artista desconocido"
                val album = cursor.getString(albumCol)?.takeIf { it != "<unknown>" } ?: "Álbum desconocido"
                val duration = cursor.getLong(durationCol)
                val fingerprint = "${title.lowercase()}|${artist.lowercase()}|${album.lowercase()}|${duration / 1000}"
                if (!fingerprints.add(fingerprint)) continue
                val fileName = if (nameCol >= 0) cursor.getString(nameCol).orEmpty() else ""
                result.add(
                    SongEntity(
                        id = Ids.forUri(uri),
                        uri = uri,
                        title = title,
                        artist = artist,
                        album = album,
                        albumKey = "$album|$artist",
                        durationMs = duration,
                        artworkUri = artwork,
                        trackNumber = cursor.getInt(trackCol) % 1000,
                        year = if (yearCol >= 0 && !cursor.isNull(yearCol)) cursor.getInt(yearCol) else null,
                        genre = if (genreCol >= 0 && !cursor.isNull(genreCol)) cursor.getString(genreCol) else null,
                        folder = "Almacenamiento del dispositivo",
                        folderUri = MEDIASTORE_FOLDER,
                        fileName = fileName,
                        isFavorite = existingFavorites[uri]?.isFavorite ?: false,
                        addedAt = System.currentTimeMillis()
                    )
                )
            }
        }
        if (result.isNotEmpty()) {
            songDao.insertAll(result)
        }
        songDao.deleteMissingInFolder(MEDIASTORE_FOLDER, result.map { it.id })
        songDao.prunePlaylistSongs()
        songDao.pruneHistory()
        _state.value = ScanState.Completed(
            songs = songDao.count(),
            errors = 0,
            durationMs = 0
        )
        result.size
    }

    companion object {
        const val MEDIASTORE_FOLDER = "mediastore://all"
    }
}
