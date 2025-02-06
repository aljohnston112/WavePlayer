package io.fourth_finger.pinky_player.random_playlist

import androidx.room.Database
import androidx.room.RoomDatabase
import io.fourth_finger.playlist_data_source.Song

/**
 * A database for caching songs.
 */
@Database(entities = [Song::class], version = 1)
abstract class SongDatabase : RoomDatabase() {

    abstract fun songDAO(): SongDAO

}