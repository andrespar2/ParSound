package com.andrespar2.parsound.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "songs",
    indices = [Index(value = ["uri"], unique = true), Index("title"), Index("artist"), Index("albumKey")]
)
data class SongEntity(
    @PrimaryKey val id: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumKey: String,
    val durationMs: Long,
    val artworkUri: String?,
    val trackNumber: Int = 0,
    val year: Int? = null,
    val genre: String? = null,
    val folder: String = "",
    val folderUri: String = "",
    val fileName: String = "",
    val lrc: String? = null,
    val isFavorite: Boolean = false,
    val addedAt: Long = 0L
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongEntity(
    val playlistId: Long,
    val songId: Long,
    val position: Int
)

@Entity(tableName = "history", indices = [Index("playedAt")])
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val playedAt: Long = System.currentTimeMillis()
)

data class AlbumSummary(
    val albumKey: String,
    val album: String,
    val artist: String,
    val artworkUri: String?,
    val songCount: Int,
    val durationMs: Long
)

data class ArtistSummary(
    val artist: String,
    val artworkUri: String?,
    val songCount: Int,
    val durationMs: Long
)

data class GenreSummary(
    val genre: String,
    val songCount: Int
)

data class FolderSummary(
    val folder: String,
    val folderUri: String,
    val songCount: Int
)

data class PlaylistSummary(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val songCount: Int
)
