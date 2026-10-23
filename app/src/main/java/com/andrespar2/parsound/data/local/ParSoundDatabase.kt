package com.andrespar2.parsound.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SongEntity::class], version = 1, exportSchema = false)
abstract class ParSoundDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
}
