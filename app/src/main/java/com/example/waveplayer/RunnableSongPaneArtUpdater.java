package com.example.waveplayer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.core.content.res.ResourcesCompat;

public class RunnableSongPaneArtUpdater implements Runnable {

    ActivityMain activityMain;

    public RunnableSongPaneArtUpdater(ActivityMain activityMain) {
        this.activityMain = activityMain;
    }

    @Override
    public void run() {
        int songArtHeight;
        int songArtWidth = getSongArtWidth();
        songArtHeight = songArtWidth;
        Bitmap bitmapSongArt = ActivityMain.getThumbnail(
                activityMain.getCurrentSong(), songArtWidth, songArtHeight, activityMain.getApplicationContext());
        ImageView imageViewSongPaneSongArt = activityMain.findViewById(R.id.imageViewSongPaneSongArt);
        if(imageViewSongPaneSongArt != null) {
            if (bitmapSongArt != null) {
                imageViewSongPaneSongArt.setImageBitmap(bitmapSongArt);
            } else {
                Bitmap defaultBitmap = getDefaultBitmap(songArtWidth, songArtHeight);
                if (defaultBitmap != null) {
                    imageViewSongPaneSongArt.setImageBitmap(defaultBitmap);
                }
            }
        }
    }

    private int getSongArtHeight() {
        ImageView imageViewSongPaneSongArt = activityMain.findViewById(R.id.imageViewSongPaneSongArt);
        if (imageViewSongPaneSongArt != null) {
            int songArtHeight = imageViewSongPaneSongArt.getHeight();
            if (songArtHeight > 0) {
                activityMain.setSongPaneArtHeight(songArtHeight);
            } else {
                songArtHeight = activityMain.getSongPaneArtHeight();
            }
            return songArtHeight;
        }
        return -1;
    }

    private int getSongArtWidth() {
        ImageView imageViewSongPaneSongArt = activityMain.findViewById(R.id.imageViewSongPaneSongArt);
        if (imageViewSongPaneSongArt != null) {
            int songArtWidth = imageViewSongPaneSongArt.getWidth();
            if (songArtWidth > 0) {
                activityMain.setSongPaneArtWidth(songArtWidth);
            } else {
                songArtWidth = activityMain.getSongPaneArtWidth();
            }
            return songArtWidth;
        }
        return -1;
    }

    private Bitmap getDefaultBitmap(int songArtWidth, int songArtHeight) {
        ImageView imageViewSongPaneSongArt = activityMain.findViewById(R.id.imageViewSongPaneSongArt);
        if(imageViewSongPaneSongArt != null) {
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
        }
        return null;
    }

}
