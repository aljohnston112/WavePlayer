package com.example.waveplayer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.core.content.res.ResourcesCompat;

public class RunnableSongPaneArtUpdater implements Runnable {

    ServiceMain serviceMain;

    ImageView imageViewSongPaneSongArt;

    public RunnableSongPaneArtUpdater(ServiceMain serviceMain, ImageView imageViewSongPaneSongArt) {
        this.serviceMain = serviceMain;
        this.imageViewSongPaneSongArt = imageViewSongPaneSongArt;
    }

    @Override
    public void run() {
        int songArtHeight;
        int songArtWidth = getSongArtWidth();
        songArtHeight = songArtWidth;
        Bitmap bitmapSongArt = ActivityMain.getThumbnail(
                serviceMain.getCurrentSong(), songArtWidth, songArtHeight, serviceMain.getApplicationContext());
        if (bitmapSongArt != null) {
            imageViewSongPaneSongArt.setImageBitmap(bitmapSongArt);
        } else {
            Bitmap defaultBitmap = getDefaultBitmap(songArtWidth, songArtHeight);
            if (defaultBitmap != null) {
                imageViewSongPaneSongArt.setImageBitmap(defaultBitmap);
            }
        }
    }

    private int getSongArtHeight() {
        if (imageViewSongPaneSongArt != null && serviceMain != null) {
            int songArtHeight = imageViewSongPaneSongArt.getHeight();
            if (songArtHeight > 0) {
                serviceMain.setSongPaneArtHeight(songArtHeight);
            } else {
                songArtHeight = serviceMain.getSongPaneArtHeight();
            }
            return songArtHeight;
        }
        return -1;
    }

    private int getSongArtWidth() {
        if (imageViewSongPaneSongArt != null && serviceMain != null) {
            int songArtWidth = imageViewSongPaneSongArt.getWidth();
            if (songArtWidth > 0) {
                serviceMain.setSongPaneArtWidth(songArtWidth);
            } else {
                songArtWidth = serviceMain.getSongPaneArtWidth();
            }
            return songArtWidth;
        }
        return -1;
    }

    private Bitmap getDefaultBitmap(int songArtWidth, int songArtHeight) {
        Drawable drawableSongArt = ResourcesCompat.getDrawable(
                imageViewSongPaneSongArt.getResources(), R.drawable.music_note_black_48dp, null);
        if (drawableSongArt != null) {
            Bitmap bitmapSongArt;
            drawableSongArt.setBounds(0, 0, songArtWidth, songArtHeight);
            bitmapSongArt = Bitmap.createBitmap(
                    songArtWidth, songArtHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmapSongArt);
            drawableSongArt.draw(canvas);
            Bitmap bitmapSongArtResized = FragmentPaneSong.getResizedBitmap(
                    bitmapSongArt, songArtWidth, songArtHeight);
            bitmapSongArt.recycle();
            return bitmapSongArtResized;
        }
        return null;
    }


}
