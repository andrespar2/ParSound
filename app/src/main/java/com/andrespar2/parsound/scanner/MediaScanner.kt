package com.andrespar2.parsound.scanner

import android.content.Context
import android.net.Uri
import android.os.Build
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
        // MediaStore.Audio.Media.GENRE solo existe desde Android 11 (API 30).
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
            if (supportsGenre) add(MediaStore.Audio.Media.GENRE)
            add(MediaStore.Audio.Media.DATA)
        }.toTypedArray()
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
            val genreCol = if (supportsGenre) cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE) else -1
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
                        genre = if (genreCol >= 0 && !cursor.isNull(genreCol)) cursor.getString(genreCol) else null
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
