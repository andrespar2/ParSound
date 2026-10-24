package com.andrespar2.parsound.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: String,
    val albumArtUri: String?,
    val trackNumber: Int = 0,
    val year: Int? = null,
    val genre: String? = null,
    val folderUri: String = "",
    val isFavorite: Boolean = false
)
