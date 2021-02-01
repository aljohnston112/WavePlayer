package com.example.waveplayer.random_playlist;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Song.class}, version = 1)
public abstract class SongDatabase extends RoomDatabase {

    public abstract SongDAO songDAO();

}
