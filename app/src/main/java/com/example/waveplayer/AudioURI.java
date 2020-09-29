package com.example.waveplayer;

import android.graphics.Bitmap;
import android.net.Uri;

public final class AudioURI implements Comparable<AudioURI> {

    public final Uri uri;

    public final String title;

    public final String artist;

    final Bitmap thumbnail;

    final int duration;

    private boolean isChecked = false;

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public AudioURI(Uri uri, Bitmap thumbnail, String artist, String title, int duration){
        this.uri = uri;
        this.title = title;
        this.artist = artist;
        this.thumbnail = thumbnail;
        this.duration = duration;
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

}
