package com.andrespar2.parsound.domain.model

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: String,
    val albumArtUri: String?,
    val trackNumber: Int = 0,
    val year: Int? = null,
    val genre: String? = null
)
