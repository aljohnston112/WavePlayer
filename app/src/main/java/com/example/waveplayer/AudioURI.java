package com.example.waveplayer;

import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.Nullable;

public final class AudioURI implements Comparable<AudioURI> {

    public final Uri uri;

    public final Bitmap thumbnail;

    public final String displayName;

    public final String artist;

    public final String title;

    private boolean isSelected = false;

    public AudioURI(Uri uri, Bitmap thumbnail, String displayName, String artist, String title){
        this.uri = uri;
        this.thumbnail = thumbnail;
        this.displayName = displayName;
        this.artist = artist;
        this.title = title;
    }

    @Override
    public int compareTo(AudioURI o) {
        int h = uri.compareTo(o.uri);
        if(h != 0) {
            return h;
        }
        h = artist.compareTo(o.artist);
        if(h != 0) {
            return h;
        }
        h = title.compareTo(o.title);
        return h;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj instanceof AudioURI && uri.equals(((AudioURI) obj).uri)){
            return true;
        }
        return false;
    }
}
