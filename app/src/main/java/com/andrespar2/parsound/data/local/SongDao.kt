package com.andrespar2.parsound.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun getFavorites(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE albumKey = :albumKey ORDER BY trackNumber ASC, title COLLATE NOCASE ASC")
    fun getSongsByAlbum(albumKey: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE artist = :artist ORDER BY album COLLATE NOCASE ASC, trackNumber ASC")
    fun getSongsByArtist(artist: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE genre = :genre ORDER BY title COLLATE NOCASE ASC")
    fun getSongsByGenre(genre: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE folder = :folder AND folderUri = :folderUri ORDER BY title COLLATE NOCASE ASC")
    fun getSongsByFolder(folder: String, folderUri: String): Flow<List<SongEntity>>

    @Query(
        "SELECT albumKey, album, artist, MAX(artworkUri) AS artworkUri, COUNT(*) AS songCount, " +
            "SUM(durationMs) AS durationMs FROM songs GROUP BY albumKey, album, artist " +
            "ORDER BY album COLLATE NOCASE ASC"
    )
    fun getAlbums(): Flow<List<AlbumSummary>>

    @Query(
        "SELECT artist, MAX(artworkUri) AS artworkUri, COUNT(*) AS songCount, SUM(durationMs) AS durationMs " +
            "FROM songs GROUP BY artist ORDER BY artist COLLATE NOCASE ASC"
    )
    fun getArtists(): Flow<List<ArtistSummary>>

    @Query(
        "SELECT genre, COUNT(*) AS songCount FROM songs WHERE genre IS NOT NULL AND genre != '' " +
            "GROUP BY genre ORDER BY genre COLLATE NOCASE ASC"
    )
    fun getGenres(): Flow<List<GenreSummary>>

    @Query(
        "SELECT folder, folderUri, COUNT(*) AS songCount FROM songs " +
            "GROUP BY folder, folderUri ORDER BY folder COLLATE NOCASE ASC"
    )
    fun getFolders(): Flow<List<FolderSummary>>

    @Query(
        "SELECT * FROM songs WHERE title LIKE '%' || :query || '%' COLLATE NOCASE " +
            "OR artist LIKE '%' || :query || '%' COLLATE NOCASE " +
            "OR album LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY title COLLATE NOCASE ASC LIMIT 200"
    )
    fun search(query: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE lrc IS NULL OR TRIM(lrc) = ''")
    suspend fun getSongsMissingLyrics(): List<SongEntity>

    @Query("UPDATE songs SET lrc = :lyrics WHERE id = :id")
    suspend fun updateLyrics(id: Long, lyrics: String)

    @Query("SELECT * FROM songs WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<SongEntity>

    @Query("SELECT * FROM songs")
    suspend fun getAllOnce(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE isFavorite = 1")
    suspend fun getFavoritesOnce(): List<SongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE id NOT IN (:keepIds)")
    suspend fun deleteMissing(keepIds: List<Long>)

    @Query("DELETE FROM songs WHERE folderUri = :folderUri AND id NOT IN (:keepIds)")
    suspend fun deleteMissingInFolder(folderUri: String, keepIds: List<Long>)

    @Query("DELETE FROM songs WHERE folderUri = :folderUri")
    suspend fun deleteByFolder(folderUri: String)

    @Query("DELETE FROM songs")
    suspend fun clearAll()

    @Query("UPDATE songs SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE songs SET uri = :uri WHERE id = :id")
    suspend fun updateUri(id: Long, uri: String)

    @Query("DELETE FROM playlist_songs WHERE songId NOT IN (SELECT id FROM songs)")
    suspend fun prunePlaylistSongs()

    @Query("DELETE FROM history WHERE songId NOT IN (SELECT id FROM songs)")
    suspend fun pruneHistory()

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM songs")
    fun countFlow(): Flow<Int>

    @Transaction
    suspend fun replaceLibrary(songs: List<SongEntity>) {
        if (songs.isEmpty()) {
            deleteMissing(listOf(-1L))
            return
        }
        insertAll(songs)
        deleteMissing(songs.map { it.id })
    }
}
