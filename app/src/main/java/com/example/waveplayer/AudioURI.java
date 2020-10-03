package com.example.waveplayer;

import android.content.ContentUris;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.Nullable;

import java.io.Serializable;

public final class AudioURI implements Comparable<AudioURI>, Serializable {

    transient private Uri uri;

    public final Bitmap thumbnail;

    public final String displayName;

    public final String artist;

    public final String title;

    final long id;

    private boolean isSelected = false;

    public AudioURI(Uri uri, Bitmap thumbnail, String displayName, String artist, String title, long id){
        if(uri != null) {
            this.uri = uri;
        } else{
            this.uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        }
        this.thumbnail = thumbnail;
        this.displayName = displayName;
        this.artist = artist;
        this.title = title;
        this.id = id;
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
        if(uri == null){
            this.uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        }
        return obj instanceof AudioURI && uri.equals(((AudioURI) obj).getUri());
    }

    @Override
    public int hashCode() {
        if(uri == null) {
            this.uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        }
        return uri.toString().hashCode();
    }

    public Uri getUri() {
        if(uri == null){
            this.uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        }
        return uri;
    }

}
