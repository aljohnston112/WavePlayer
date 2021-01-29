package com.example.waveplayer.random_playlist;

import android.content.ContentUris;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.Random;

public final class AudioUri implements Comparable<AudioUri>, Serializable {

    transient static final String TAG = "AudioURI";

    transient private Uri uri;

    private final NestedProbMap nestProbMap = new NestedProbMap();

    public final String displayName;

    public final String artist;

    public final String title;

    public final long id;

    public Uri getUri() {
        Log.v(TAG, "getUri started");
        if (uri == null) {
            this.uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        }
        Log.v(TAG, "getUri ended");
        return uri;
    }

    public static Uri getUri(Long songID) {
        return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songID);
    }

    private int duration = -1;

    public int getDuration(Context context) {
        Log.v(TAG, "getDuration started");
        if (duration == -1) {
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(context, getUri());
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

    public AudioUri(String displayName, String artist, String title, long id) {
        //Log.v(TAG, "AudioURI constructing");
        this.displayName = displayName;
        this.artist = artist;
        this.title = title;
        this.id = id;

        // Log.v(TAG, "AudioURI constructed");
    }

    public boolean shouldPlay(Random random) {
        return nestProbMap.outcome(random);
    }

    public boolean bad(double percent) {
        return nestProbMap.bad(percent);
    }

    public boolean good(double percent) {
        return nestProbMap.good(percent);
    }

    public void clearProbabilities() {
        nestProbMap.clearProbabilities();
    }

        @Override
    public int compareTo(AudioUri o) {
            return title.compareTo(o.title);
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