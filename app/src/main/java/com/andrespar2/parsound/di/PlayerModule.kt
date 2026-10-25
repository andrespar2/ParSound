package com.andrespar2.parsound.di

import android.content.Context
import com.andrespar2.parsound.player.CrossfadePlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {
    @Provides
    @Singleton
    fun provideCrossfadePlayer(@ApplicationContext context: Context): CrossfadePlayer =
        CrossfadePlayer(context)
}
