package com.example.waveplayer.random_playlist

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Song::class], version = 1)
abstract class SongDatabase : RoomDatabase() {
    abstract fun songDAO(): SongDAO
}