package com.andrespar2.parsound.scanner

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.andrespar2.parsound.data.local.SongDao
import com.andrespar2.parsound.data.local.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao
) {
    suspend fun scan() = withContext(Dispatchers.IO) {
        val songs = mutableListOf<SongEntity>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.GENRE,
            MediaStore.Audio.Media.DATA
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val genreCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString())
                val albumArtUri = Uri.withAppendedPath(Uri.parse("content://media/external/audio/albumart"), cursor.getLong(albumIdCol).toString()).toString()
                songs.add(
                    SongEntity(
                        id = id,
                        title = cursor.getString(titleCol) ?: "Desconocido",
                        artist = cursor.getString(artistCol) ?: "Desconocido",
                        album = cursor.getString(albumCol) ?: "Desconocido",
                        albumId = cursor.getLong(albumIdCol),
                        durationMs = cursor.getLong(durationCol),
                        uri = contentUri.toString(),
                        albumArtUri = albumArtUri,
                        trackNumber = cursor.getInt(trackCol) % 1000,
                        year = if (cursor.isNull(yearCol)) null else cursor.getInt(yearCol),
                        genre = if (cursor.isNull(genreCol)) null else cursor.getString(genreCol)
                    )
                )
            }
        }
        if (songs.isNotEmpty()) {
            songDao.clearAll()
            songDao.insertAll(songs)
        }
    }
}
