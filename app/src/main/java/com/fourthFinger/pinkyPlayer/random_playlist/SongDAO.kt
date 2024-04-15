package com.fourthFinger.pinkyPlayer.random_playlist

import androidx.room.*

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
    @Query("DELETE FROM songs")
    fun deleteAll()
}