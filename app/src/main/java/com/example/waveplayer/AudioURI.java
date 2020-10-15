package com.example.waveplayer;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Size;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.Serializable;

public final class AudioURI implements Comparable<AudioURI>, Serializable {

    transient private Uri uri;

    private Bitmap thumbnail;

    public final String displayName;

    public final String artist;

    public final String title;

    private int duration = -1;

    final long id;

    final String data;

    private boolean isSelected = false;

    public AudioURI(Uri uri, String data, String displayName, String artist, String title, long id) {
        this.uri = uri;
        this.displayName = displayName;
        this.artist = artist;
        this.title = title;
        this.id = id;
        this.data = data;
    }

    public Bitmap getThumbnail(Context context){
        if(thumbnail == null){
            Bitmap thumbnail = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    thumbnail = context.getContentResolver().loadThumbnail(
                            uri, new Size(640, 480), null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                final int thumbNailWidthAndHeight = 128;
                thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(data),
                        thumbNailWidthAndHeight, thumbNailWidthAndHeight);
            }
            this.thumbnail = thumbnail;
        }
        return thumbnail;
    }

    public int getDuration(Context context) {
        if(duration == -1){
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(context,uri);
            String time = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (time == null) {
                time = "00:00:00";
            }
            this.duration = Integer.parseInt(time);
            mediaMetadataRetriever.release();
        }
        return duration;
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
