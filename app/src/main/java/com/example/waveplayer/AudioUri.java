package com.example.waveplayer;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;

import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

public final class AudioUri implements Comparable<AudioUri>, Serializable {

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

    // Used for determining if the user picked a song when making a playlist.
    private boolean isSelected = false;

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public AudioUri(Uri uri, String data, String displayName, String artist, String title, long id) {
        //Log.v(TAG, "AudioURI constructing");
        this.uri = uri;
        this.displayName = displayName;
        this.artist = artist;
        this.title = title;
        this.id = id;
        this.data = data;
       // Log.v(TAG, "AudioURI constructed");
    }

    public static Bitmap getThumbnail(AudioUri audioURI, Context context){
        Bitmap bitmap = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                bitmap = context.getContentResolver().loadThumbnail(
                        audioURI.getUri(), new Size(128, 128), null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            try {
                mmr.setDataSource(context.getContentResolver().openFileDescriptor(
                        audioURI.getUri(), "r").getFileDescriptor());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            InputStream inputStream = null;
            if (mmr.getEmbeddedPicture() != null) {
                inputStream = new ByteArrayInputStream(mmr.getEmbeddedPicture());
            }
            mmr.release();
            bitmap = BitmapFactory.decodeStream(inputStream);
        }
        return bitmap;
    }

    @Override
    public int compareTo(AudioUri o) {
       // Log.v(TAG, "compareTo start");
        int h = title.compareTo(o.title);
       // Log.v(TAG, "compareTo end");
        return h;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        //Log.v(TAG, "equals start");
        if (uri == null) {
            this.uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        }
       // Log.v(TAG, "equals end");
        return obj instanceof AudioUri && uri.equals(((AudioUri) obj).getUri());
    }

    @Override
    public int hashCode() {
        //Log.v(TAG, "hashCode start");
        if (uri == null) {
            this.uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        }
        //Log.v(TAG, "hashCode end");
        return uri.toString().hashCode();
    }

}
