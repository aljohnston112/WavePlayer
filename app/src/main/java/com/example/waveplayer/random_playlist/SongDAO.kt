package com.example.waveplayer.random_playlist

import androidx.room.*

@Dao
interface SongDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    open fun insertAll(vararg songs: Song?)
    @Delete
    open fun delete(user: Song?)
    @Query("SELECT * FROM songs WHERE id = :song_id")
    open fun getSong(song_id: Long?): Song?
    @Query("SELECT * FROM songs")
    open fun getAll(): MutableList<Song?>?
    @Query("DELETE FROM songs")
    open fun deleteAll()
}