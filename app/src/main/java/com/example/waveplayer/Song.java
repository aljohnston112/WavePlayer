package com.example.waveplayer;

import androidx.annotation.Nullable;

import com.example.waveplayer.random_playlist.AudioUri;

import java.io.Serializable;

public class Song implements Comparable<Song>, Serializable {

    public final Long id;

    public final String title;

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

    public static Song getSong(AudioUri audioUri){
        return new Song(audioUri.id, audioUri.title);
    }

}
