package io.fourth_finger.pinky_player.random_playlist

import androidx.room.*
import io.fourth_finger.playlist_data_source.Song

/**
 * The data access object for the SongDatabase.
 */
@Dao
interface SongDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg songs: Song)

    @Delete
    fun delete(user: Song)

    @Query("SELECT * FROM songs WHERE id = :songId")
    fun getSong(songId: Long): Song?

    @Query("SELECT * FROM songs")
    fun getAll(): MutableList<Song>

}