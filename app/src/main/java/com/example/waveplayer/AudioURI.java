package com.example.waveplayer;

import android.graphics.Bitmap;
import android.net.Uri;

public final class AudioURI implements Comparable<AudioURI> {

    public final Uri uri;

    public final String displayName;

    public final String title;

    public final String artist;

    public final Bitmap thumbnail;

    private boolean isChecked = false;

    public AudioURI(Uri uri, Bitmap thumbnail, String displayName, String artist, String title){
        this.uri = uri;
        this.displayName = displayName;
        this.title = title;
        this.artist = artist;
        this.thumbnail = thumbnail;
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

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

}
