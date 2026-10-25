package com.andrespar2.parsound.di

import android.content.Context
import androidx.room.Room
import com.andrespar2.parsound.data.local.ParSoundDatabase
import com.andrespar2.parsound.data.local.PlaylistDao
import com.andrespar2.parsound.data.local.HistoryDao
import com.andrespar2.parsound.data.local.SongDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ParSoundDatabase =
        Room.databaseBuilder(context, ParSoundDatabase::class.java, "parsound.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideSongDao(database: ParSoundDatabase): SongDao = database.songDao()

    @Provides
    fun providePlaylistDao(database: ParSoundDatabase): PlaylistDao = database.playlistDao()

    @Provides
    fun provideHistoryDao(database: ParSoundDatabase): HistoryDao = database.historyDao()
}
