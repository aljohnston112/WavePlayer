package com.example.waveplayer.random_playlist

import androidx.room.*

@Dao
interface SongDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg songs: Song)
    @Delete
    fun delete(user: Song)
    @Query("SELECT * FROM songs WHERE id = :song_id")
    fun getSong(song_id: Long): Song
    @Query("SELECT * FROM songs")
    fun getAll(): MutableList<Song>
    @Query("DELETE FROM songs")
    fun deleteAll()
}