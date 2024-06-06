package io.fourthFinger.pinkyPlayer.random_playlist

import androidx.room.Database
import androidx.room.RoomDatabase
import io.fourthFinger.playlistDataSource.Song

/**
 * A database for caching songs.
 */
@Database(entities = [Song::class], version = 1)
abstract class SongDatabase : RoomDatabase() {

    abstract fun songDAO(): SongDAO

}