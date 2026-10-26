package com.andrespar2.parsound.data.repo

import android.content.Context
import android.net.Uri
import com.andrespar2.parsound.data.local.AlbumSummary
import com.andrespar2.parsound.data.local.ArtistSummary
import com.andrespar2.parsound.data.local.FolderSummary
import com.andrespar2.parsound.data.local.GenreSummary
import com.andrespar2.parsound.data.local.HistoryDao
import com.andrespar2.parsound.data.local.HistoryEntity
import com.andrespar2.parsound.data.local.PlaylistDao
import com.andrespar2.parsound.data.local.PlaylistEntity
import com.andrespar2.parsound.data.local.PlaylistSongEntity
import com.andrespar2.parsound.data.local.PlaylistSummary
import com.andrespar2.parsound.data.local.SongDao
import com.andrespar2.parsound.data.local.SongEntity
import com.andrespar2.parsound.data.lyrics.LyricsExtractor
import com.andrespar2.parsound.data.prefs.FolderConfig
import com.andrespar2.parsound.data.prefs.ParSoundPreferences
import com.andrespar2.parsound.scanner.LibraryScanner
import com.andrespar2.parsound.scanner.ScanState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class MusicRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val historyDao: HistoryDao,
    private val preferences: ParSoundPreferences,
    private val scanner: LibraryScanner
) {
    val songs: Flow<List<SongEntity>> = songDao.getAllSongs()
    val favorites: Flow<List<SongEntity>> = songDao.getFavorites()
    val favoriteIds: Flow<Set<Long>> = songDao.getFavorites().map { list -> list.map { it.id }.toSet() }
    val albums: Flow<List<AlbumSummary>> = songDao.getAlbums()
    val artists: Flow<List<ArtistSummary>> = songDao.getArtists()
    val genres: Flow<List<GenreSummary>> = songDao.getGenres()
    val folders: Flow<List<FolderSummary>> = songDao.getFolders()
    val songCount: Flow<Int> = songDao.countFlow()
    val playlists: Flow<List<PlaylistSummary>> = playlistDao.observePlaylistsWithCount()
    val configuredFolders: Flow<List<FolderConfig>> = preferences.folders
    val scanState: StateFlow<ScanState> = scanner.state
    val lastScan: Flow<Long> = preferences.lastScan

    val recentSongs: Flow<List<SongEntity>> = historyDao.observeRecent(120).mapLatest { entries ->
        val ids = entries.map { it.songId }.distinct().take(24)
        val byId = songDao.getByIds(ids).associateBy { it.id }
        ids.mapNotNull { byId[it] }
    }

    fun search(query: String): Flow<List<SongEntity>> = songDao.search(query)

    fun albumSongs(albumKey: String): Flow<List<SongEntity>> = songDao.getSongsByAlbum(albumKey)

    fun artistSongs(artist: String): Flow<List<SongEntity>> = songDao.getSongsByArtist(artist)

    fun genreSongs(genre: String): Flow<List<SongEntity>> = songDao.getSongsByGenre(genre)

    fun folderSongs(folder: String, folderUri: String): Flow<List<SongEntity>> =
        songDao.getSongsByFolder(folder, folderUri)

    fun playlist(id: Long): Flow<PlaylistEntity?> = playlistDao.observePlaylist(id)

    fun playlistSongs(playlistId: Long): Flow<List<SongEntity>> =
        playlistDao.observeSongIds(playlistId).mapLatest { ids ->
            val byId = songDao.getByIds(ids).associateBy { it.id }
            ids.mapNotNull { byId[it] }
        }

    suspend fun allSongsOnce(): List<SongEntity> = songDao.getAllOnce()

    suspend fun songById(id: Long): SongEntity? = songDao.getById(id)

    suspend fun setFavorite(id: Long, favorite: Boolean) = songDao.setFavorite(id, favorite)

    suspend fun updateLyrics(id: Long, lyrics: String) = songDao.updateLyrics(id, lyrics)

    suspend fun enrichMissingLyrics(onProgress: (found: Int, total: Int) -> Unit = { _, _ -> }): Int =
        withContext(Dispatchers.IO) {
            val songs = songDao.getSongsMissingLyrics()
            var found = 0
            songs.forEachIndexed { index, song ->
                val lyrics = runCatching {
                    LyricsExtractor.extract(context, android.net.Uri.parse(song.uri), song.fileName)
                }.getOrNull()
                if (!lyrics.isNullOrBlank()) {
                    songDao.updateLyrics(song.id, lyrics)
                    found++
                }
                onProgress(found, index + 1)
            }
            found
        }

    suspend fun addFolder(folder: FolderConfig) {
        preferences.addFolder(folder)
        scanner.scanAllFolders()
    }

    suspend fun removeFolder(folder: FolderConfig) {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                Uri.parse(folder.uri),
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        preferences.removeFolder(folder.uri)
        songDao.deleteByFolder(folder.uri)
    }

    fun scanNow() = scanner.scanAllFolders()

    fun cancelScan() = scanner.cancel()

    suspend fun importMediaStore(): Int = scanner.importFromMediaStore()

    suspend fun createPlaylist(name: String): Long =
        playlistDao.insertPlaylist(PlaylistEntity(name = name.trim().ifBlank { "Lista sin nombre" }))

    suspend fun renamePlaylist(id: Long, name: String) =
        playlistDao.rename(id, name.trim().ifBlank { "Lista sin nombre" })

    suspend fun deletePlaylist(id: Long) {
        playlistDao.clearPlaylist(id)
        playlistDao.delete(id)
    }

    suspend fun addToPlaylist(playlistId: Long, songId: Long): Boolean {
        val exists = playlistDao.getSongIds(playlistId).contains(songId)
        if (exists) return false
        val position = playlistDao.maxPosition(playlistId) + 1
        playlistDao.addSong(PlaylistSongEntity(playlistId, songId, position))
        return true
    }

    suspend fun removeFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.removeSong(playlistId, songId)
    }

    suspend fun movePlaylistSong(playlistId: Long, songId: Long, newPosition: Int) {
        playlistDao.updatePosition(playlistId, songId, newPosition)
    }

    suspend fun playlistsForSong(songId: Long): List<PlaylistEntity> =
        playlistDao.playlistsForSong(songId).mapNotNull { playlistDao.getPlaylist(it) }

    suspend fun addHistory(songId: Long) {
        historyDao.insert(HistoryEntity(songId = songId))
    }

    suspend fun clearHistory() = historyDao.clear()

    suspend fun release() = Unit

    val preferencesRef: ParSoundPreferences get() = preferences
    val scannerRef: LibraryScanner get() = scanner
}
