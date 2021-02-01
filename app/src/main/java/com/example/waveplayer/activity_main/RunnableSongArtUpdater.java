package com.example.waveplayer.activity_main;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.core.content.res.ResourcesCompat;

import com.example.waveplayer.R;
import com.example.waveplayer.media_controller.MediaData;

public class RunnableSongArtUpdater implements Runnable {

    private final SongArtUpdateCallback songArtUpdateCallback;

    public interface SongArtUpdateCallback {
        void updateSongArtRun();
    }

    public RunnableSongArtUpdater(SongArtUpdateCallback songArtUpdateCallback) {
        this.songArtUpdateCallback = songArtUpdateCallback;
    }

    @Override
    public void run() {
        songArtUpdateCallback.updateSongArtRun();
    }

}