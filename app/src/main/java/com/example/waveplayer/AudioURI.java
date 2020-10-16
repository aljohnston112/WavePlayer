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
import android.util.Log;
import android.util.Size;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.Serializable;

public final class AudioURI implements Comparable<AudioURI>, Serializable {

    transient static final String TAG = "AudioURI";

    transient private Uri uri;

    public final String displayName;

    public final String artist;

    public final String title;

    final long id;

    final String data;

    public Uri getUri() {
        Log.v(TAG, "getUri started");
        if (uri == null) {
            this.uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        }
        Log.v(TAG, "getUri ended");
        return uri;
    }

    private int duration = -1;

    public int getDuration(Context context) {
        Log.v(TAG, "getDuration started");
        if (duration == -1) {
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(context, uri);
            String time = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (time == null) {
                time = "00:00:00";
            }
            this.duration = Integer.parseInt(time);
            mediaMetadataRetriever.release();
        }
        Log.v(TAG, "getDuration ended");
        return duration;
    }

    // TODO find a more efficient way to find songs the user picked
    // Used for determining if the user picked a song when making a playlist.
    private boolean isSelected = false;

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public AudioURI(Uri uri, String data, String displayName, String artist, String title, long id) {
        Log.v(TAG, "AudioURI constructing");
        this.uri = uri;
        this.displayName = displayName;
        this.artist = artist;
        this.title = title;
        this.id = id;
        this.data = data;
        Log.v(TAG, "AudioURI constructed");
    }

    public static Bitmap getThumbnail(AudioURI audioURI, Context context) {
        Log.v(TAG, "getThumbnail start");
        Bitmap thumbnail = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                thumbnail = context.getContentResolver().loadThumbnail(
                        audioURI.getUri(), new Size(640, 480), null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            final int thumbNailWidthAndHeight = 128;
            thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(audioURI.data),
                    thumbNailWidthAndHeight, thumbNailWidthAndHeight);
        }
        Log.v(TAG, "getThumbnail end");
        return thumbnail;
    }

    @Override
    public int compareTo(AudioURI o) {
        Log.v(TAG, "compareTo start");
        int h = uri.compareTo(o.uri);
        if (h != 0) {
            return h;
        }
        h = artist.compareTo(o.artist);
        if (h != 0) {
            return h;
        }
        h = title.compareTo(o.title);
        Log.v(TAG, "compareTo end");
        return h;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        Log.v(TAG, "equals start");
        if (uri == null) {
            this.uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        }
        Log.v(TAG, "equals end");
        return obj instanceof AudioURI && uri.equals(((AudioURI) obj).getUri());
    }

    @Override
    public int hashCode() {
        Log.v(TAG, "hashCode start");
        if (uri == null) {
            this.uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        }
        Log.v(TAG, "hashCode end");
        return uri.toString().hashCode();
    }

}
