package com.example.waveplayer.media_controller;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "songs")
public class Song implements Comparable<Song>, Serializable {

    @PrimaryKey
    public final Long id;

    @ColumnInfo(name = "title")
    public final String title;

    @Ignore
    private boolean selected;

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public Song(Long id, String title){
        this.id = id;
        this.title = title;
    }

    @Override
    public int compareTo(Song o) {
        return title.compareTo(o.title);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof Song && id.equals(((Song) obj).id);
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

}
