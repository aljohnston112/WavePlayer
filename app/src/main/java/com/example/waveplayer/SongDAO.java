package com.example.waveplayer;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SongDAO {

    @Insert
    void insertAll(Song... songs);

    @Delete
    void delete(Song user);

    @Query("SELECT * FROM songs WHERE id = :song_id")
    Song getSong(Long song_id);

    @Query("SELECT * FROM songs")
    List<Song> getAll();

}
